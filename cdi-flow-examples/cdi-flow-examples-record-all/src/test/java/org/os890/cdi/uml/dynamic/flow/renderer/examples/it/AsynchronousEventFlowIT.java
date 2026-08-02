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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.events.AsyncStatsObserver;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.events.CheckoutService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A recorded flow belongs to exactly one thread. An asynchronous event is delivered on a
 * container-managed worker-thread, so it produces a diagram of its own instead of being nested
 * into the firing method - that is a deliberate property, not a gap.
 */
class AsynchronousEventFlowIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(AsynchronousEventFlowIT.class);
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
    @DisplayName("the asynchronous observer gets its own flow on its own thread")
    void recordsTheAsyncObserverSeparately() {
        container.get(CheckoutService.class).checkoutAsync("SKU-9", 4).toCompletableFuture().join();

        CallFlow observerFlow = container.flows()
                .awaitSingleEnteredAt(AsyncStatsObserver.class, Duration.ofSeconds(5));
        CallFlow firingFlow = container.flows().singleEnteredAt(CheckoutService.class);

        assertThat(Flows.flatten(observerFlow)).containsExactly(
                "AsyncStatsObserver#onCheckoutAsync(CheckoutEvent)",
                "AuditService#log(String)");
        assertThat(Flows.flatten(firingFlow))
                .containsExactly("CheckoutService#checkoutAsync(String, int)");
        assertThat(observerFlow.threadName()).isNotEqualTo(firingFlow.threadName());
    }

    @Test
    @DisplayName("each thread writes its own diagram file")
    void writesOneDiagramPerThread() {
        container.get(CheckoutService.class).checkoutAsync("SKU-9", 4).toCompletableFuture().join();
        container.flows().awaitSingleEnteredAt(AsyncStatsObserver.class, Duration.ofSeconds(5));

        assertThat(container.writtenDiagrams())
                .extracting(path -> path.getFileName().toString())
                .anySatisfy(name -> assertThat(name).startsWith("CheckoutService_checkoutAsync_"))
                .anySatisfy(name -> assertThat(name).startsWith("AsyncStatsObserver_onCheckoutAsync_"));
        container.writtenDiagrams()
                .forEach(file -> MermaidAssertions.assertWellFormed(container.readDiagram(file)));
    }

    @Test
    @DisplayName("the asynchronous observer is still marked as an event")
    void marksTheAsyncObserverAsEvent() {
        container.get(CheckoutService.class).checkoutAsync("SKU-9", 4).toCompletableFuture().join();

        CallFlow observerFlow = container.flows()
                .awaitSingleEnteredAt(AsyncStatsObserver.class, Duration.ofSeconds(5));

        assertThat(observerFlow.root().isObserverMethod()).isTrue();
        assertThat(observerFlow.toMermaid())
                .contains("Caller-)AsyncStatsObserver: [event] onCheckoutAsync(CheckoutEvent)");
    }
}
