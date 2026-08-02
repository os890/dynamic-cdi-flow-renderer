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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.recursion.RecursiveService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recursion looks structurally identical to a proxy handing a call on: same bean, same method,
 * one child, no work of its own. It must survive the de-duplication, and it does because it
 * re-enters the same instance.
 */
class RecursionFlowIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(RecursionFlowIT.class);
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
    @DisplayName("every recursion level is kept as its own frame")
    void keepsEveryRecursionLevel() {
        int result = container.get(RecursiveService.class).countdown(4);

        assertThat(result).isEqualTo(4);

        CallFlow flow = container.flows().singleEnteredAt(RecursiveService.class);
        assertThat(Flows.straightChain(flow.root())).hasSize(5);
        assertThat(Flows.flatten(flow))
                .containsOnly("RecursiveService#countdown(int)")
                .hasSize(5);
    }

    @Test
    @DisplayName("the de-duplication does not mistake recursion for a proxy hop")
    void doesNotCollapseRecursion() {
        container.get(RecursiveService.class).countdown(4);

        CallFlow flow = container.flows().singleEnteredAt(RecursiveService.class);

        assertThat(Flows.straightChain(flow.root()))
                .allSatisfy(node -> assertThat(node.collapsedProxyHops())
                        .as("collapsed hops at %s", node).isZero());
    }

    @Test
    @DisplayName("recursion re-enters the very same bean instance")
    void reEntersTheSameInstance() {
        container.get(RecursiveService.class).countdown(4);

        CallFlow flow = container.flows().singleEnteredAt(RecursiveService.class);

        assertThat(Flows.straightChain(flow.root())).extracting(CallNode::targetIdentity)
                .containsOnly(flow.root().targetIdentity());
    }

    @Test
    @DisplayName("the recursion renders as nested activations of one participant")
    void rendersNestedActivations() {
        container.get(RecursiveService.class).countdown(4);

        String diagram = container.flows().singleEnteredAt(RecursiveService.class).toMermaid();

        assertThat(MermaidAssertions.countStatements(diagram, "activate RecursiveService")).isEqualTo(5);
        assertThat(MermaidAssertions.countStatements(diagram, "deactivate RecursiveService")).isEqualTo(5);
        assertThat(MermaidAssertions.countStatements(diagram, "participant RecursiveService")).isOne();
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("a recursion which stops immediately is a single frame")
    void handlesTheBaseCase() {
        container.get(RecursiveService.class).countdown(0);

        CallFlow flow = container.flows().singleEnteredAt(RecursiveService.class);

        assertThat(Flows.totalCallCount(flow.root())).isOne();
        MermaidAssertions.assertWellFormed(flow.toMermaid());
    }
}
