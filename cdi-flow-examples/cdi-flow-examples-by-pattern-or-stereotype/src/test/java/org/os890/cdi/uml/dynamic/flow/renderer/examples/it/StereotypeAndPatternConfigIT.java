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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.nesting.Level1Service;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.stereotypes.AuditedEntryService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pattern and stereotype configured together in
 * {@code META-INF/microprofile-config.properties}. They are two <em>alternative</em> selectors, so
 * a bean is recorded as soon as one of them applies - the pattern selects a package the stereotype
 * does not reach, and the stereotype reaches beans the pattern does not cover.
 */
class StereotypeAndPatternConfigIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        //no override - both selectors come from this example's microprofile-config.properties
        container = CdiFlowTestContainer.startFor(StereotypeAndPatternConfigIT.class);
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
    @DisplayName("a bean the pattern selects is recorded although it has no stereotype")
    void recordsBeansSelectedByThePattern() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);

        assertThat(Flows.flatten(container.flows().singleEnteredAt(OrderService.class)))
                .containsExactly(
                        "OrderService#placeOrder(String, int)",
                        "PricingService#priceOf(String)",
                        "TaxService#taxFor(BigDecimal)",
                        "RoundingService#round(BigDecimal)",
                        "InventoryService#reserve(String, int)",
                        "AuditService#log(String)");
    }

    @Test
    @DisplayName("a bean the stereotype selects is recorded although it is outside the pattern")
    void recordsBeansSelectedByTheStereotype() {
        container.get(AuditedEntryService.class).handle("x");

        assertThat(Flows.flatten(container.flows().singleEnteredAt(AuditedEntryService.class)))
                .containsExactly(
                        "AuditedEntryService#handle(String)",
                        "AuditedHelper#audited(String)",
                        "StackedHelper#stacked(String)")
                .doesNotContain("PlainHelper#plain(String)");
    }

    @Test
    @DisplayName("a bean neither selector covers stays out")
    void ignoresBeansNeitherSelectorCovers() {
        container.get(Level1Service.class).descend("start");

        assertThat(container.flows().all()).isEmpty();
    }
}
