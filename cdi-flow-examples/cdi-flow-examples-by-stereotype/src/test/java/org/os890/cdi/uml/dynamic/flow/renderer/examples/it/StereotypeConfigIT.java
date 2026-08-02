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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.stereotypes.AuditedEntryService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.stereotypes.AuditedService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.stereotypes.InternalService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code cdi-flow.include-stereotypes} on its own - this example configures no pattern, so the
 * stereotypes are the only selector.
 * <p>
 * The first tests boot without any override and therefore run on what this example's
 * {@code META-INF/microprofile-config.properties} declares. The later ones explore variations and
 * override it through a system-property, which MicroProfile-Config ranks higher.
 * <p>
 * Every test needs its own container because the configuration is read while the container boots.
 */
class StereotypeConfigIT {

    /** boots on the configuration this example ships */
    private static CdiFlowTestContainer startFromConfigFile() {
        return CdiFlowTestContainer.startFor(StereotypeConfigIT.class);
    }

    private static CdiFlowTestContainer startWith(String stereotypes) {
        return CdiFlowTestContainer.configured()
                .with(FlowConfig.KEY_INCLUDE_STEREOTYPES, stereotypes)
                .startFor(StereotypeConfigIT.class);
    }

    @Test
    @DisplayName("only beans carrying the configured stereotype are recorded")
    void recordsOnlyBeansWithTheConfiguredStereotype() {
        try (CdiFlowTestContainer container = startFromConfigFile()) {
            container.get(AuditedEntryService.class).handle("x");

            assertThat(Flows.flatten(container.flows().singleEnteredAt(AuditedEntryService.class)))
                    .containsExactly(
                            "AuditedEntryService#handle(String)",
                            "AuditedHelper#audited(String)",
                            //selected through StackedAuditedService, which carries @AuditedService
                            "StackedHelper#stacked(String)")
                    .doesNotContain("InternalHelper#internal(String)")
                    .doesNotContain("PlainHelper#plain(String)");
        }
    }

    @Test
    @DisplayName("a bean without any of the configured stereotypes is not instrumented at all")
    void ignoresBeansWithoutTheStereotype() {
        try (CdiFlowTestContainer container = startFromConfigFile()) {
            assertThat(container.get(OrderService.class).placeOrder("SKU-1", 2).reserved())
                    .as("the bean still works").isTrue();

            assertThat(container.flows().all()).isEmpty();
            assertThat(container.writtenDiagrams()).isEmpty();
        }
    }

    @Test
    @DisplayName("several stereotypes can be configured comma-separated")
    void acceptsSeveralStereotypes() {
        try (CdiFlowTestContainer container = startWith(
                AuditedService.class.getName() + "," + InternalService.class.getName())) {
            container.get(AuditedEntryService.class).handle("x");

            assertThat(Flows.flatten(container.flows().singleEnteredAt(AuditedEntryService.class)))
                    .containsExactly(
                            "AuditedEntryService#handle(String)",
                            "AuditedHelper#audited(String)",
                            "StackedHelper#stacked(String)",
                            "InternalHelper#internal(String)")
                    .doesNotContain("PlainHelper#plain(String)");
        }
    }

    @Test
    @DisplayName("a stereotype may also be given by its simple name")
    void acceptsASimpleStereotypeName() {
        try (CdiFlowTestContainer container = startWith("AuditedService")) {
            container.get(AuditedEntryService.class).handle("x");

            assertThat(Flows.flatten(container.flows().singleEnteredAt(AuditedEntryService.class)))
                    .containsExactly(
                            "AuditedEntryService#handle(String)",
                            "AuditedHelper#audited(String)",
                            "StackedHelper#stacked(String)");
        }
    }

    @Test
    @DisplayName("a mistyped stereotype records nothing, but the deployment still comes up")
    void survivesAnUnknownStereotype() {
        try (CdiFlowTestContainer container = startWith("com.acme.DoesNotExist")) {
            container.get(AuditedEntryService.class).handle("x");
            container.get(OrderService.class).placeOrder("SKU-1", 2);

            assertThat(container.flows().all()).isEmpty();
        }
    }

}
