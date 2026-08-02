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
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.edge.FailingOrchestrator;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.events.CheckoutService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.loops.BatchService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.PlantUmlAssertions;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code cdi-flow.output-format=plantuml} - configured in this example's
 * {@code META-INF/microprofile-config.properties} - switches the notation. Everything else, which
 * calls are recorded, how they nest, how loops fold, has to stay exactly the same.
 */
class PlantUmlOutputIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        //no override - the format comes from this example's microprofile-config.properties
        container = CdiFlowTestContainer.startFor(PlantUmlOutputIT.class);
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
    @DisplayName("the configured format reaches the runtime")
    void appliesTheConfiguredFormat() {
        container.get(OrderService.class).placeOrder("SKU-1", 3);

        assertThat(container.flows().singleEnteredAt(OrderService.class).config().outputFormat())
                .isEqualTo(DiagramFormat.PLANTUML);
    }

    @Test
    @DisplayName("diagrams are written as .puml and wrapped in @startuml/@enduml")
    void writesPlantUmlFiles() {
        container.get(OrderService.class).placeOrder("SKU-1", 3);

        List<Path> diagrams = container.writtenDiagrams(".puml");

        assertThat(container.writtenDiagrams(".mmd")).isEmpty();
        assertThat(diagrams).hasSize(1);
        assertThat(diagrams.get(0).getFileName().toString())
                .matches("OrderService_placeOrder_\\d{8}-\\d{9}_\\d{8}-\\d{9}\\.puml");

        String diagram = container.readDiagram(diagrams.get(0));
        assertThat(diagram).startsWith("@startuml\n").endsWith("@enduml\n")
                .contains("Caller -> OrderService : placeOrder(String, int)")
                .contains("OrderService -> PricingService : priceOf(String)")
                .contains("PricingService --> OrderService : BigDecimal");
        PlantUmlAssertions.assertWellFormed(diagram);
        PlantUmlAssertions.assertFreeOfProxyNames(diagram);
    }

    @Test
    @DisplayName("loop-folding works the same way in PlantUML")
    void foldsLoops() {
        container.get(BatchService.class).processAll(List.of("a", "b", "c", "d", "e"));

        String diagram = container.flows().singleEnteredAt(BatchService.class).toDiagram();

        assertThat(diagram).contains("loop 5 times");
        assertThat(PlantUmlAssertions.countOccurrences(diagram, "ItemValidator : validate(String)")).isOne();
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("observers become asynchronous messages")
    void rendersEventsAsAsynchronousMessages() {
        container.get(CheckoutService.class).checkout("SKU-7", 2);

        String diagram = container.flows().singleEnteredAt(CheckoutService.class).toDiagram();

        assertThat(diagram)
                .contains("CheckoutService ->> StockObserver : [event] onCheckout(CheckoutEvent)")
                .contains("StockObserver -> InventoryService : reserve(String, int)");
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("exceptions become cross-arrows")
    void rendersFailuresAsCrossArrows() {
        assertThatThrownBy(() -> container.get(FailingOrchestrator.class).run())
                .isInstanceOf(IllegalStateException.class);

        String diagram = container.flows().singleEnteredAt(FailingOrchestrator.class).toDiagram();

        assertThat(diagram)
                .contains("FailingService -->x FailingOrchestrator : throws IllegalStateException")
                .contains("FailingOrchestrator -->x Caller : throws IllegalStateException");
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("switching the notation does not change what is recorded")
    void recordsExactlyTheSameCalls() {
        container.get(OrderService.class).placeOrder("SKU-1", 3);

        CallFlow flow = container.flows().singleEnteredAt(OrderService.class);

        assertThat(Flows.flatten(flow)).containsExactly(
                "OrderService#placeOrder(String, int)",
                "PricingService#priceOf(String)",
                "TaxService#taxFor(BigDecimal)",
                "RoundingService#round(BigDecimal)",
                "InventoryService#reserve(String, int)",
                "AuditService#log(String)");
    }

    @Test
    @DisplayName("both notations remain available on a recorded flow, whatever is configured")
    void canStillRenderTheOtherNotation() {
        container.get(OrderService.class).placeOrder("SKU-1", 3);

        CallFlow flow = container.flows().singleEnteredAt(OrderService.class);

        assertThat(flow.toDiagram()).isEqualTo(flow.toPlantUml());
        assertThat(flow.toMermaid()).startsWith("sequenceDiagram\n");
        assertThat(flow.toDiagram(DiagramFormat.MERMAID)).isEqualTo(flow.toMermaid());
    }
}
