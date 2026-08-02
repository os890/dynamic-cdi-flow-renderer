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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleFlowIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(SimpleFlowIT.class);
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
    @DisplayName("a call into a bean is recorded without any annotation on the bean")
    void recordsACallWithoutAnyBeanAnnotation() {
        container.get(OrderService.class).placeOrder("SKU-1", 3);

        CallFlow flow = container.flows().singleEnteredAt(OrderService.class);
        CallNode root = flow.root();

        assertThat(root.beanSimpleName()).isEqualTo("OrderService");
        assertThat(root.methodName()).isEqualTo("placeOrder");
        assertThat(root.parameterTypeNames()).containsExactly("String", "int");
        assertThat(root.returnTypeName()).isEqualTo("Order");
        assertThat(root.hasFailed()).isFalse();
        assertThat(root.children()).extracting(CallNode::beanSimpleName)
                .containsExactly("PricingService", "InventoryService", "AuditService");
    }

    @Test
    @DisplayName("the diagram is well-formed and free of proxy-names")
    void rendersAWellFormedDiagram() {
        container.get(OrderService.class).placeOrder("SKU-1", 3);

        String diagram = container.flows().singleEnteredAt(OrderService.class).toMermaid();

        MermaidAssertions.assertWellFormed(diagram);
        MermaidAssertions.assertFreeOfProxyNames(diagram);
        assertThat(diagram).contains("Caller->>OrderService: placeOrder(String, int)");
    }

    @Test
    @DisplayName("a diagram file is written for the outermost call only")
    void writesOneFilePerOutermostCall() {
        container.get(OrderService.class).placeOrder("SKU-1", 3);

        List<Path> diagrams = container.writtenDiagrams();

        assertThat(diagrams).hasSize(1);
        assertThat(diagrams.get(0).getFileName().toString())
                .matches("OrderService_placeOrder_\\d{8}-\\d{9}_\\d{8}-\\d{9}\\.mmd");
        MermaidAssertions.assertWellFormed(container.readDiagram(diagrams.get(0)));
    }
}
