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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.loops.BatchService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.nesting.Level1Service;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.AuditService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The container boots without a single override - everything comes from this example's own
 * {@code META-INF/microprofile-config.properties}, which selects the beans of the order-package.
 */
class IncludePatternConfigIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(IncludePatternConfigIT.class);
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
    @DisplayName("beans matching the pattern are recorded")
    void recordsMatchingBeans() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);

        assertThat(Flows.flatten(container.flows().singleEnteredAt(OrderService.class)))
                .containsExactly(
                        "OrderService#placeOrder(String, int)",
                        "PricingService#priceOf(String)",
                        "TaxService#taxFor(BigDecimal)",
                        //RoundingService is in the package too, but vetoed by the exclude-pattern
                        "InventoryService#reserve(String, int)",
                        "AuditService#log(String)");
    }

    @Test
    @DisplayName("beans outside the pattern are not instrumented at all")
    void ignoresNonMatchingBeans() {
        String result = container.get(Level1Service.class).descend("start");

        assertThat(result).as("the bean itself still works").isNotBlank();
        assertThat(container.flows().all()).isEmpty();
        assertThat(container.writtenDiagrams()).isEmpty();
    }

    @Test
    @DisplayName("a non-matching bean in the middle of a chain drops out of the diagram")
    void skipsNonMatchingBeansInsideAChain() {
        container.get(BatchService.class).processAll(List.of("a", "b"));

        //BatchService and ItemValidator are outside the pattern, the audit-call is inside it
        assertThat(container.flows().enteredAt(AuditService.class)).hasSize(1);
        assertThat(container.flows().all()).hasSize(1);
    }
}
