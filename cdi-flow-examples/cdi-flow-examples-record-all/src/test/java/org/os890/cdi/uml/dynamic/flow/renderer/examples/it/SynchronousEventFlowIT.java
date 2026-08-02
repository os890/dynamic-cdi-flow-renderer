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
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.events.CheckoutService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A synchronous CDI event is delivered on the firing thread, so the observers belong into the same
 * call-chain as the method which fired the event.
 */
class SynchronousEventFlowIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(SynchronousEventFlowIT.class);
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
    @DisplayName("both observers are recorded inside the firing method, in priority order")
    void recordsObserversInsideTheFiringMethod() {
        container.get(CheckoutService.class).checkout("SKU-7", 2);

        CallFlow flow = container.flows().singleEnteredAt(CheckoutService.class);

        assertThat(Flows.flatten(flow)).containsExactly(
                "CheckoutService#checkout(String, int)",
                "StockObserver#onCheckout(CheckoutEvent)",
                "InventoryService#reserve(String, int)",
                "NotificationObserver#onCheckout(CheckoutEvent)",
                "AuditService#log(String)");
    }

    @Test
    @DisplayName("what an observer calls itself is nested below the observer")
    void nestsWhatTheObserverCalls() {
        container.get(CheckoutService.class).checkout("SKU-7", 2);

        CallNode root = container.flows().singleEnteredAt(CheckoutService.class).root();
        CallNode stockObserver = root.children().get(0);

        assertThat(stockObserver.beanSimpleName()).isEqualTo("StockObserver");
        assertThat(Flows.childBeanNames(stockObserver)).containsExactly("InventoryService");
    }

    @Test
    @DisplayName("observer-methods are drawn as event-arrows, plain calls are not")
    void drawsObserversAsEvents() {
        container.get(CheckoutService.class).checkout("SKU-7", 2);

        CallFlow flow = container.flows().singleEnteredAt(CheckoutService.class);
        String diagram = flow.toMermaid();

        assertThat(flow.root().children()).allMatch(CallNode::isObserverMethod);
        assertThat(diagram)
                .contains("CheckoutService-)StockObserver: [event] onCheckout(CheckoutEvent)")
                .contains("CheckoutService-)NotificationObserver: [event] onCheckout(CheckoutEvent)")
                //the call the observer makes is an ordinary call again
                .contains("StockObserver->>InventoryService: reserve(String, int)");
        MermaidAssertions.assertWellFormed(diagram);
        MermaidAssertions.assertFreeOfProxyNames(diagram);
    }

    @Test
    @DisplayName("firing an event still produces exactly one diagram")
    void writesOneDiagram() {
        container.get(CheckoutService.class).checkout("SKU-7", 2);

        assertThat(container.writtenDiagrams()).hasSize(1);
        assertThat(container.writtenDiagrams().get(0).getFileName().toString())
                .startsWith("CheckoutService_checkout_");
    }
}
