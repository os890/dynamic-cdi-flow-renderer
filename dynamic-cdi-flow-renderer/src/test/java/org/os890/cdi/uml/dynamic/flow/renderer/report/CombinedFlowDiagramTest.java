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

package org.os890.cdi.uml.dynamic.flow.renderer.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.PlantUmlAssertions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class CombinedFlowDiagramTest {

    private static CallFlow placeOrderFlow() {
        return call("OrderService", "placeOrder").params("String", "int").returning("Order")
                .calling(call("PricingService", "priceOf").params("String").returning("BigDecimal"))
                .buildFlow();
    }

    private static CallFlow shipFlow() {
        return call("ShippingService", "ship").params("String").returning("void")
                .calling(call("AuditService", "log").params("String"))
                .buildFlow();
    }

    @Test
    @DisplayName("one flow becomes one block, in Mermaid")
    void singleFlowInMermaid() {
        String diagram = CombinedFlowDiagram.of(List.of(placeOrderFlow()), DiagramFormat.MERMAID, null);

        MermaidAssertions.assertWellFormed(diagram);
        assertThat(diagram).startsWith("sequenceDiagram");
        assertThat(MermaidAssertions.countStatements(diagram, "rect rgb(244, 244, 244)")).isEqualTo(1);
        assertThat(diagram)
                .contains("participant Caller as caller")
                .contains("participant OrderService")
                .contains("participant PricingService")
                .contains("OrderService.placeOrder")
                .contains("Caller->>OrderService: placeOrder(String, int)");
    }

    @Test
    @DisplayName("every flow gets its own block and the lanes are shared")
    void severalFlowsShareTheirParticipants() {
        String diagram = CombinedFlowDiagram.of(
                List.of(placeOrderFlow(), shipFlow()), DiagramFormat.MERMAID, null);

        MermaidAssertions.assertWellFormed(diagram);
        assertThat(MermaidAssertions.countStatements(diagram, "rect rgb(244, 244, 244)")).isEqualTo(2);
        assertThat(MermaidAssertions.countOccurrences(diagram, "participant Caller as caller")).isEqualTo(1);
        assertThat(diagram)
                .contains("OrderService.placeOrder")
                .contains("ShippingService.ship");
    }

    @Test
    @DisplayName("identical chains are kept, not collapsed - an assertion has to see the repetition")
    void identicalChainsAreNotCollapsed() {
        String diagram = CombinedFlowDiagram.of(
                List.of(placeOrderFlow(), placeOrderFlow(), placeOrderFlow()),
                DiagramFormat.MERMAID, null);

        MermaidAssertions.assertWellFormed(diagram);
        assertThat(MermaidAssertions.countStatements(diagram, "rect rgb(244, 244, 244)")).isEqualTo(3);
        assertThat(MermaidAssertions.countOccurrences(diagram, "participant OrderService")).isEqualTo(1);
    }

    @Test
    @DisplayName("the notation is the caller's, not the configured one")
    void plantUmlIsRenderedIndependentlyOfTheConfiguration() {
        FlowConfig mermaidConfig = FlowConfig.builder().outputFormat(DiagramFormat.MERMAID).build();
        CallFlow recordedAsMermaid = call("OrderService", "placeOrder").params("String", "int")
                .returning("Order")
                .calling(call("PricingService", "priceOf").params("String").returning("BigDecimal"))
                .buildFlow(mermaidConfig);

        String diagram = CombinedFlowDiagram.of(
                List.of(recordedAsMermaid), DiagramFormat.PLANTUML, null);

        PlantUmlAssertions.assertWellFormed(diagram);
        assertThat(diagram)
                .startsWith("@startuml")
                .endsWith("@enduml\n")
                .contains("group OrderService.placeOrder")
                .contains("Caller -> OrderService : placeOrder(String, int)")
                .doesNotContain("->>");
    }

    @Test
    @DisplayName("a title is carried into the diagram, and null leaves it off")
    void titleIsOptional() {
        String titled = CombinedFlowDiagram.of(
                List.of(placeOrderFlow()), DiagramFormat.MERMAID, "an order is placed");
        String untitled = CombinedFlowDiagram.of(
                List.of(placeOrderFlow()), DiagramFormat.MERMAID, null);

        assertThat(titled).startsWith("---\ntitle: \"an order is placed\"\n---\n");
        assertThat(untitled).doesNotContain("title");
    }

    @Test
    @DisplayName("no flow at all yields the bare header rather than an exception")
    void emptyListIsRendered() {
        String diagram = CombinedFlowDiagram.of(List.of(), DiagramFormat.MERMAID, null);

        assertThat(diagram).isEqualTo("sequenceDiagram\n    autonumber\n    participant Caller as caller\n");
    }

    @Test
    @DisplayName("neither the flows nor the format may be null")
    void argumentsAreChecked() {
        assertThatThrownBy(() -> CombinedFlowDiagram.of(null, DiagramFormat.MERMAID, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("flows");
        assertThatThrownBy(() -> CombinedFlowDiagram.of(List.of(), null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("format");
    }
}
