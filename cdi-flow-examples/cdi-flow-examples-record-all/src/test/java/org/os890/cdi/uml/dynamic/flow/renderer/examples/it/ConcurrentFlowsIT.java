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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.nesting.Level1Service;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A flow is recorded per thread, so concurrent calls must not bleed into each other.
 */
class ConcurrentFlowsIT {

    private static final int THREADS = 8;

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(ConcurrentFlowsIT.class);
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

    private static void runConcurrently(Runnable task) throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            for (int i = 0; i < THREADS; i++) {
                executor.execute(() -> {
                    try {
                        startSignal.await();
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }
            startSignal.countDown();
            assertThat(finished.await(30, TimeUnit.SECONDS)).as("all threads finished").isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("each thread produces its own complete, uncontaminated flow")
    void keepsConcurrentFlowsApart() throws InterruptedException {
        runConcurrently(() -> container.get(OrderService.class).placeOrder("SKU-1", 2));

        List<CallFlow> flows = container.flows().all();

        assertThat(flows).hasSize(THREADS);
        assertThat(flows).allSatisfy(flow -> assertThat(Flows.flatten(flow)).containsExactly(
                "OrderService#placeOrder(String, int)",
                "PricingService#priceOf(String)",
                "TaxService#taxFor(BigDecimal)",
                "RoundingService#round(BigDecimal)",
                "InventoryService#reserve(String, int)",
                "AuditService#log(String)"));
        assertThat(flows).extracting(CallFlow::threadName).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("every concurrent flow gets its own diagram file")
    void writesOneDiagramPerThread() throws InterruptedException {
        runConcurrently(() -> container.get(OrderService.class).placeOrder("SKU-1", 2));

        assertThat(container.writtenDiagrams()).hasSize(THREADS);
        container.writtenDiagrams().forEach(file -> {
            String diagram = container.readDiagram(file);
            MermaidAssertions.assertWellFormed(diagram);
            MermaidAssertions.assertFreeOfProxyNames(diagram);
        });
    }

    @Test
    @DisplayName("different entry-points running at the same time stay separate")
    void keepsDifferentEntryPointsApart() throws InterruptedException {
        runConcurrently(() -> {
            if (Thread.currentThread().getName().hashCode() % 2 == 0) {
                container.get(OrderService.class).placeOrder("SKU-1", 2);
            } else {
                container.get(Level1Service.class).descend("start");
            }
        });

        assertThat(container.flows().all()).hasSize(THREADS)
                .allSatisfy(flow -> assertThat(flow.entryTypeSimpleName())
                        .isIn("OrderService", "Level1Service"));
        assertThat(container.flows().enteredAt(Level1Service.class))
                .allSatisfy(flow -> assertThat(Flows.totalCallCount(flow.root())).isEqualTo(6));
        assertThat(container.flows().enteredAt(OrderService.class))
                .allSatisfy(flow -> assertThat(Flows.totalCallCount(flow.root())).isEqualTo(6));
    }
}
