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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.stereotypes.AuditedEntryService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This example ships no {@code microprofile-config.properties} at all, which is the case a
 * developer gets by simply putting the addon onto the class-path: every eligible bean is recorded.
 * <p>
 * It is the counterpart of the {@code by-pattern} and {@code by-stereotype} examples - the same
 * beans, but nothing narrowing them down.
 */
class NoConfigurationIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(NoConfigurationIT.class);
    }

    @AfterAll
    static void stopContainer() {
        container.close();
    }

    @BeforeEach
    void forgetPreviousFlows() {
        container.flows().clear();
        container.clearWrittenDiagrams();
    }

    @Test
    @DisplayName("neither selector is configured")
    void configuresNoSelector() {
        container.get(AuditedEntryService.class).handle("x");

        FlowConfig config = container.flows().singleEnteredAt(AuditedEntryService.class).config();

        assertThat(config.includePattern()).isNull();
        assertThat(config.includeStereotypes()).isEmpty();
        assertThat(config.excludePattern()).isNull();
    }

    @Test
    @DisplayName("without any selector every bean is recorded, stereotyped or not")
    void recordsEveryBean() {
        container.get(AuditedEntryService.class).handle("x");

        assertThat(Flows.flatten(container.flows().singleEnteredAt(AuditedEntryService.class)))
                .containsExactly(
                        "AuditedEntryService#handle(String)",
                        "AuditedHelper#audited(String)",
                        "StackedHelper#stacked(String)",
                        "InternalHelper#internal(String)",
                        //the bean without any stereotype is recorded here as well
                        "PlainHelper#plain(String)");
    }
}
