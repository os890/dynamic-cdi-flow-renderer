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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.nesting.Level1Service;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import static org.assertj.core.api.Assertions.assertThat;

class DeeplyNestedFlowIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(DeeplyNestedFlowIT.class);
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
    @DisplayName("a six-level chain is recorded in full and in order")
    void recordsTheWholeChain() {
        String result = container.get(Level1Service.class).descend("start");

        assertThat(result).isEqualTo("start/level1/level2/level3/level4/level5/level6");

        CallFlow flow = container.flows().singleEnteredAt(Level1Service.class);
        assertThat(Flows.flatten(flow)).containsExactly(
                "Level1Service#descend(String)",
                "Level2Service#descend(String)",
                "Level3Service#descend(String)",
                "Level4Service#descend(String)",
                "Level5Service#descend(String)",
                "Level6Service#descend(String)");
        assertThat(Flows.depth(flow.root())).isEqualTo(6);
    }

    @Test
    @DisplayName("only the outermost call writes a diagram - the five nested ones do not")
    void writesExactlyOneDiagramForTheWholeChain() {
        container.get(Level1Service.class).descend("start");

        assertThat(container.flows().all()).hasSize(1);
        assertThat(container.writtenDiagrams()).hasSize(1);
        assertThat(container.writtenDiagrams().get(0).getFileName().toString())
                .startsWith("Level1Service_descend_");
    }

    @Test
    @DisplayName("every level is activated and deactivated exactly once")
    void rendersBalancedActivations() {
        container.get(Level1Service.class).descend("start");

        String diagram = container.flows().singleEnteredAt(Level1Service.class).toMermaid();

        MermaidAssertions.assertWellFormed(diagram);
        MermaidAssertions.assertFreeOfProxyNames(diagram);
        for (int level = 1; level <= 6; level++) {
            String participant = "Level" + level + "Service";
            assertThat(MermaidAssertions.countStatements(diagram, "activate " + participant))
                    .as("activations of %s", participant).isOne();
            assertThat(MermaidAssertions.countStatements(diagram, "deactivate " + participant))
                    .as("deactivations of %s", participant).isOne();
        }
    }

    @Test
    @DisplayName("starting further down the chain makes that bean the entry-point")
    void usesTheOutermostBeanAsEntryPoint() {
        container.get(org.os890.cdi.uml.dynamic.flow.renderer.examples.nesting.Level4Service.class).descend("start");

        CallFlow flow = container.flows().single();

        assertThat(flow.entryTypeSimpleName()).isEqualTo("Level4Service");
        assertThat(flow.entryMethodName()).isEqualTo("descend");
        assertThat(Flows.straightChain(flow.root())).hasSize(3);
    }

    @Test
    @DisplayName("the recorded frames are properly nested rather than flattened")
    void nestsRatherThanFlattens() {
        container.get(Level1Service.class).descend("start");

        CallNode root = container.flows().single().root();

        assertThat(root.children()).hasSize(1);
        assertThat(Flows.totalCallCount(root)).isEqualTo(6);
        assertThat(Flows.straightChain(root)).extracting(CallNode::beanSimpleName)
                .containsExactly("Level1Service", "Level2Service", "Level3Service",
                        "Level4Service", "Level5Service", "Level6Service");
    }
}
