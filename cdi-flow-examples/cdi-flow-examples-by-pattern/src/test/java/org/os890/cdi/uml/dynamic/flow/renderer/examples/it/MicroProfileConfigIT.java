/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.os890.cdi.uml.dynamic.flow.renderer.examples.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.nesting.Level1Service;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the configuration really travels through MicroProfile-Config: the patterns asserted
 * here are declared in this example's {@code META-INF/microprofile-config.properties} and nowhere
 * else.
 * <p>
 * Every test boots its own container - only one CDI container can run per JVM at a time.
 */
class MicroProfileConfigIT {

    private static final String INCLUDE_FROM_CONFIG_FILE =
            "org\\.os890\\.cdi\\.uml\\.dynamic\\.flow\\.renderer\\.examples\\.order\\..*";
    private static final String EXCLUDE_FROM_CONFIG_FILE = ".*\\.RoundingService";

    @Test
    @DisplayName("a MicroProfile-Config implementation is present in this setup")
    void detectsMicroProfileConfig() {
        assertThat(FlowConfig.isMicroProfileConfigAvailable()).isTrue();
    }

    @Test
    @DisplayName("both patterns are read from META-INF/microprofile-config.properties")
    void readsThePatternsFromTheConfigFile() {
        assertThat(System.getProperty(FlowConfig.KEY_INCLUDE_PATTERN))
                .as("the value must not come from a system-property").isNull();

        try (CdiFlowTestContainer container =
                     CdiFlowTestContainer.startFor(MicroProfileConfigIT.class)) {

            container.get(OrderService.class).placeOrder("SKU-1", 2);

            FlowConfig config = container.flows().singleEnteredAt(OrderService.class).config();
            assertThat(config.includePattern()).isEqualTo(INCLUDE_FROM_CONFIG_FILE);
            assertThat(config.excludePattern()).isEqualTo(EXCLUDE_FROM_CONFIG_FILE);
        }
    }

    @Test
    @DisplayName("a system-property wins over the config-file, as the MicroProfile ordinals demand")
    void letsSystemPropertiesWin() {
        String overridden =
                "org\\.os890\\.cdi\\.uml\\.dynamic\\.flow\\.renderer\\.examples\\.nesting\\..*";

        try (CdiFlowTestContainer container = CdiFlowTestContainer.configured()
                .recordingOnly(overridden)
                .startFor(MicroProfileConfigIT.class)) {

            container.get(Level1Service.class).descend("start");
            container.get(OrderService.class).placeOrder("SKU-1", 2);

            assertThat(container.flows().singleEnteredAt(Level1Service.class).config().includePattern())
                    .isEqualTo(overridden);
            assertThat(container.flows().enteredAt(OrderService.class))
                    .as("the package the config-file selected is no longer recorded").isEmpty();
        }
    }
}
