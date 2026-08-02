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

package org.os890.cdi.uml.dynamic.flow.renderer.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.PlantUmlAssertions;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class PlantUmlSequenceRendererTest {

    private static String lines(String... values) {
        return String.join("\n", values) + "\n";
    }

    @Test
    @DisplayName("a nested call renders as an indented, activated sub-sequence")
    void rendersNestedCall() {
        CallFlow flow = call("OrderService", "placeOrder").params("String", "int").returning("Order")
                .nanos(0, 12_100_000)
                .calling(call("PricingService", "priceOf").params("String").returning("BigDecimal")
                        .nanos(1_000_000, 2_000_000))
                .buildFlow();

        String diagram = flow.toPlantUml();

        assertThat(diagram).startsWith(lines(
                "@startuml",
                "autonumber",
                "hide footbox",
                "participant \"caller\" as Caller",
                "participant OrderService",
                "participant PricingService",
                "note over Caller, PricingService"));

        assertThat(diagram).endsWith(lines(
                "Caller -> OrderService : placeOrder(String, int)",
                "activate OrderService",
                "    OrderService -> PricingService : priceOf(String)",
                "    activate PricingService",
                "    PricingService --> OrderService : BigDecimal [1.00 ms]",
                "    deactivate PricingService",
                "OrderService --> Caller : Order [12.1 ms]",
                "deactivate OrderService",
                "@enduml"));

        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("the header note is a block with both timestamps, the duration and the thread")
    void rendersHeaderNote() {
        String diagram = call("OrderService", "placeOrder").nanos(0, 56_300_000).buildFlow().toPlantUml();

        assertThat(diagram)
                .contains("note over Caller, OrderService\n")
                .contains("    56.3 ms | thread main\n")
                .contains("end note\n")
                .containsPattern(
                        "    \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} - \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\n");
    }

    @Test
    @DisplayName("a failed call renders as a cross-arrow and still closes its activation")
    void rendersFailure() {
        CallFlow flow = call("OrderService", "placeOrder").returning("Order").nanos(0, 3_000_000)
                .calling(call("PricingService", "priceOf").returning("BigDecimal")
                        .nanos(1_000_000, 2_000_000)
                        .throwing(new IllegalStateException("boom")))
                .throwing(new IllegalStateException("boom"))
                .buildFlow();

        String diagram = flow.toPlantUml();

        assertThat(diagram)
                .contains("PricingService -->x OrderService : throws IllegalStateException [1.00 ms]")
                .contains("OrderService -->x Caller : throws IllegalStateException [3.00 ms]");
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("an observer-method renders as an asynchronous message")
    void rendersObserverMethodAsEvent() {
        CallFlow flow = call("CheckoutService", "checkout")
                .calling(call("StockObserver", "onCheckout").params("CheckoutEvent").asObserver())
                .buildFlow();

        String diagram = flow.toPlantUml();

        assertThat(diagram)
                .contains("CheckoutService ->> StockObserver : [event] onCheckout(CheckoutEvent)");
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("repeated identical sibling-calls fold into a loop-block")
    void foldsLoops() {
        CallFlow flow = call("BatchService", "process")
                .calling(validation(), validation(), validation())
                .buildFlow(FlowConfig.builder().outputFormat(DiagramFormat.PLANTUML).build());

        String diagram = flow.toDiagram();

        assertThat(diagram).contains("loop 3 times");
        assertThat(PlantUmlAssertions.countOccurrences(diagram, "ItemValidator : validate(Item)")).isOne();
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("loop-folding can be switched off, then every single call shows up")
    void rendersEveryCallWhenFoldingIsDisabled() {
        CallFlow flow = call("BatchService", "process")
                .calling(validation(), validation(), validation())
                .buildFlow(FlowConfig.builder()
                        .outputFormat(DiagramFormat.PLANTUML).foldLoops(false).build());

        String diagram = flow.toDiagram();

        assertThat(diagram).doesNotContain("loop ");
        assertThat(PlantUmlAssertions.countOccurrences(diagram, "ItemValidator : validate(Item)")).isEqualTo(3);
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("beans with the same simple name in different packages stay distinguishable")
    void disambiguatesEquallyNamedBeans() {
        CallFlow flow = call("Service", "run").inPackage("com.acme.order")
                .calling(call("Service", "run").inPackage("com.acme.billing"))
                .buildFlow();

        String diagram = flow.toPlantUml();

        assertThat(diagram)
                .contains("participant \"com.acme.order.Service\" as Service")
                .contains("participant \"com.acme.billing.Service\" as Service_2")
                .contains("Service -> Service_2 : run()");
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("both notations describe exactly the same call-structure")
    void staysStructurallyIdenticalToTheMermaidOutput() {
        CallFlow flow = call("OrderService", "placeOrder").params("String").returning("Order")
                .calling(call("PricingService", "priceOf").returning("BigDecimal"),
                        call("PricingService", "priceOf").returning("BigDecimal"),
                        call("StockObserver", "onStock").params("Event").asObserver()
                                .calling(call("AuditService", "log").params("String")))
                .buildFlow();

        assertThat(structureOf(flow.toPlantUml())).isEqualTo(structureOf(flow.toMermaid()));
    }

    /**
     * Reduces a diagram to what it says about the call-structure, so the two notations become
     * comparable: participants, activations, loop-blocks and the kind, caller and callee of every
     * message.
     * <p>
     * The arrow-tokens have to be translated per format - {@code ->>} is a plain call in PlantUML
     * but an event in Mermaid. Longest token first, otherwise {@code -->>} would be eaten by the
     * rule for {@code ->>}.
     */
    private static String structureOf(String diagram) {
        boolean plantUml = diagram.startsWith("@startuml");
        String[][] arrows = plantUml
                ? new String[][]{{"-->x", "FAIL"}, {"-->", "RETURN"}, {"->>", "EVENT"}, {"->", "CALL"}}
                : new String[][]{{"-->>", "RETURN"}, {"--x", "FAIL"}, {"-)", "EVENT"}, {"->>", "CALL"}};

        return diagram.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("@") && !line.equals("hide footbox")
                        && !line.equals("sequenceDiagram")
                        && !line.startsWith("Note over") && !line.startsWith("note over")
                        && !line.equals("end note"))
                //the note body is a separate line only in the PlantUML block form
                .filter(line -> !line.contains("thread ") && !line.matches(".*\\d{4}-\\d{2}-\\d{2}.*"))
                .map(line -> line
                        .replaceAll("^participant \"([^\"]+)\" as (\\w+)$", "participant $2=$1")
                        .replaceAll("^participant (\\w+) as (.+)$", "participant $1=$2")
                        .replaceAll("^participant (\\w+)$", "participant $1=$1"))
                .map(line -> translateArrows(line, arrows))
                .map(line -> line.replaceAll("\\s*:\\s*", " : "))
                .reduce("", (a, b) -> a + b + "\n");
    }

    private static String translateArrows(String line, String[][] arrows) {
        for (String[] arrow : arrows) {
            String translated = line.replaceAll("\\s*" + Pattern.quote(arrow[0]) + "\\s*",
                    " " + arrow[1] + " ");
            if (!translated.equals(line)) {
                return translated;
            }
        }
        return line;
    }

    @Test
    @DisplayName("a hotspot is marked with a note on the slow participant")
    void rendersHotspotNote() {
        CallFlow flow = call("OrderService", "placeOrder").nanos(0, 100_000_000)
                .calling(call("PricingService", "priceOf").returning("BigDecimal")
                        .nanos(1_000_000, 91_000_000))
                .buildFlow(FlowConfig.builder().hotspotThresholdMillis(50).build());
        HotspotDetector.markHotspots(flow.root(), 50);

        String diagram = flow.toPlantUml();

        assertThat(diagram).contains(
                "note over PricingService : HOTSPOT PricingService.priceOf took 90.0 ms (over 50 ms)");
        assertThat(PlantUmlAssertions.countOccurrences(diagram, "HOTSPOT")).isOne();
        PlantUmlAssertions.assertWellFormed(diagram);
    }

    private static CallNodeBuilder validation() {
        return call("ItemValidator", "validate").params("Item").returning("boolean");
    }
}
