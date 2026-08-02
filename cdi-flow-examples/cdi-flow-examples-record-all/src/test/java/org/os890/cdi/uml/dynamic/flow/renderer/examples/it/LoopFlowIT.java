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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.loops.BatchService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A public method of another CDI bean, called inside a loop.
 */
class LoopFlowIT {

    private static final List<String> FIVE_ITEMS = List.of("a", "b", "c", "d", "e");

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(LoopFlowIT.class);
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
    @DisplayName("every single iteration is recorded in the model")
    void recordsEveryIteration() {
        container.get(BatchService.class).processAll(FIVE_ITEMS);

        CallFlow flow = container.flows().singleEnteredAt(BatchService.class);

        assertThat(Flows.childBeanNames(flow.root()))
                .containsExactly("ItemValidator", "ItemValidator", "ItemValidator",
                        "ItemValidator", "ItemValidator", "AuditService");
        //five iterations, each of them nesting a call into the normalizer
        assertThat(Flows.totalCallCount(flow.root())).isEqualTo(1 + 5 * 2 + 1);
    }

    @Test
    @DisplayName("the diagram folds the five iterations into one loop-block")
    void foldsTheIterationsInTheDiagram() {
        container.get(BatchService.class).processAll(FIVE_ITEMS);

        String diagram = container.flows().singleEnteredAt(BatchService.class).toMermaid();

        assertThat(diagram).contains("loop 5 times");
        assertThat(MermaidAssertions.countOccurrences(diagram, "ItemValidator: validate(String)")).isOne();
        assertThat(MermaidAssertions.countOccurrences(diagram, "ItemNormalizer: normalize(String)")).isOne();
        //the call after the loop must stay outside of it
        assertThat(diagram).contains("BatchService->>AuditService: log(String)");
        MermaidAssertions.assertWellFormed(diagram);
        MermaidAssertions.assertFreeOfProxyNames(diagram);
    }

    @Test
    @DisplayName("a single iteration produces no loop-block")
    void doesNotFoldASingleIteration() {
        container.get(BatchService.class).processAll(List.of("only-one"));

        String diagram = container.flows().singleEnteredAt(BatchService.class).toMermaid();

        assertThat(diagram).doesNotContain("loop ");
        MermaidAssertions.assertWellFormed(diagram);
    }

    @Test
    @DisplayName("an empty loop-body leaves only the call after the loop")
    void handlesAnEmptyLoop() {
        container.get(BatchService.class).processAll(List.of());

        CallFlow flow = container.flows().singleEnteredAt(BatchService.class);

        assertThat(Flows.childBeanNames(flow.root())).containsExactly("AuditService");
        MermaidAssertions.assertWellFormed(flow.toMermaid());
    }
}
