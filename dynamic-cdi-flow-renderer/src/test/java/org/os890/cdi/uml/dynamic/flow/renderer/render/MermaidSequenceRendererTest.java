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
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class MermaidSequenceRendererTest {

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

        String diagram = flow.toMermaid();

        assertThat(diagram).startsWith(lines(
                "sequenceDiagram",
                "    autonumber",
                "    participant Caller as caller",
                "    participant OrderService",
                "    participant PricingService").stripTrailing() + "\n    Note over Caller,PricingService: ");

        assertThat(diagram).endsWith(lines(
                "    Caller->>OrderService: placeOrder(String, int)",
                "    activate OrderService",
                "        OrderService->>PricingService: priceOf(String)",
                "        activate PricingService",
                "        PricingService-->>OrderService: BigDecimal [1.00 ms]",
                "        deactivate PricingService",
                "    OrderService-->>Caller: Order [12.1 ms]",
                "    deactivate OrderService"));

        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("the header note carries both timestamps, the duration and the thread")
    void rendersHeaderNote() {
        CallFlow flow = call("OrderService", "placeOrder").nanos(0, 56_300_000).buildFlow();

        assertThat(headerNoteOf(flow))
                .contains("Note over Caller,OrderService:")
                .contains("56.3 ms")
                .contains("thread main")
                .containsPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} - \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
    }

    @Test
    @DisplayName("the header note spans all lanes and wraps, so an exported image does not clip it")
    void spansTheHeaderNoteAcrossAllParticipants() {
        CallFlow flow = call("OrderService", "placeOrder")
                .calling(call("PricingService", "priceOf"), call("AuditService", "log"))
                .buildFlow();

        String note = headerNoteOf(flow);

        assertThat(note).startsWith("Note over Caller,AuditService: ").contains("<br/>");
        assertThat(note.substring(note.indexOf(':') + 1).split("<br/>"))
                .allSatisfy(line -> assertThat(line.strip().length())
                        .as("line '%s' of the header note", line).isLessThan(45));
    }

    private static String headerNoteOf(CallFlow flow) {
        return flow.toMermaid().lines()
                .map(String::strip)
                .filter(line -> line.startsWith("Note over"))
                .findFirst()
                .orElseThrow();
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

        String diagram = flow.toMermaid();

        assertThat(diagram)
                .contains("PricingService--xOrderService: throws IllegalStateException [1.00 ms]")
                .contains("OrderService--xCaller: throws IllegalStateException [3.00 ms]");
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("an observer-method renders as an event-arrow")
    void rendersObserverMethodAsEvent() {
        CallFlow flow = call("CheckoutService", "checkout")
                .calling(call("StockObserver", "onCheckout").params("CheckoutEvent").asObserver())
                .buildFlow();

        String diagram = flow.toMermaid();

        assertThat(diagram).contains("CheckoutService-)StockObserver: [event] onCheckout(CheckoutEvent)");
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("repeated identical sibling-calls fold into a loop-block")
    void foldsLoops() {
        CallFlow flow = call("BatchService", "process")
                .calling(repeatedValidation(), repeatedValidation(), repeatedValidation())
                .buildFlow(FlowConfig.builder().foldLoops(true).build());

        String diagram = flow.toMermaid();

        assertThat(diagram).contains("loop 3 times");
        assertThat(MermaidAssertions.countOccurrences(diagram, "ItemValidator: validate(Item)")).isOne();
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("loop-folding can be switched off, then every single call shows up")
    void rendersEveryCallWhenFoldingIsDisabled() {
        CallFlow flow = call("BatchService", "process")
                .calling(repeatedValidation(), repeatedValidation(), repeatedValidation())
                .buildFlow(FlowConfig.builder().foldLoops(false).build());

        String diagram = flow.toMermaid();

        assertThat(diagram).doesNotContain("loop ");
        assertThat(MermaidAssertions.countOccurrences(diagram, "ItemValidator: validate(Item)")).isEqualTo(3);
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("beans with the same simple name in different packages stay distinguishable")
    void disambiguatesEquallyNamedBeans() {
        CallFlow flow = call("Service", "run").inPackage("com.acme.order")
                .calling(call("Service", "run").inPackage("com.acme.billing"))
                .buildFlow();

        String diagram = flow.toMermaid();

        assertThat(diagram)
                .contains("participant Service as com.acme.order.Service")
                .contains("participant Service_2 as com.acme.billing.Service")
                .contains("Service->>Service_2: run()");
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("a bean named like a Mermaid keyword does not break the diagram")
    void escapesReservedParticipantNames() {
        CallFlow flow = call("End", "run").calling(call("Loop", "run")).buildFlow();

        String diagram = flow.toMermaid();

        assertThat(diagram).contains("participant End_ as End").contains("participant Loop_ as Loop");
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("a hotspot is marked with a note on the slow participant")
    void rendersHotspotNote() {
        CallFlow flow = call("OrderService", "placeOrder").nanos(0, 100_000_000)
                .calling(call("PricingService", "priceOf").returning("BigDecimal")
                        .nanos(1_000_000, 91_000_000))
                .buildFlow(FlowConfig.builder().hotspotThresholdMillis(50).build());
        HotspotDetector.markHotspots(flow.root(), 50);

        String diagram = flow.toMermaid();

        assertThat(diagram).contains(
                "Note over PricingService: HOTSPOT PricingService.priceOf took 90.0 ms (over 50 ms)");
        assertThat(MermaidAssertions.countOccurrences(diagram, "HOTSPOT")).isOne();
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("without a marked hotspot the diagram is unchanged")
    void rendersNoNoteWithoutAHotspot() {
        CallFlow flow = call("OrderService", "placeOrder").nanos(0, 100_000_000)
                .calling(call("PricingService", "priceOf").nanos(1_000_000, 91_000_000))
                .buildFlow();

        assertThat(flow.toMermaid()).doesNotContain("HOTSPOT");
    }

    private static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder repeatedValidation() {
        return call("ItemValidator", "validate").params("Item").returning("boolean");
    }
}
