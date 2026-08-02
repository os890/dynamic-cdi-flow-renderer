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
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class LoopFolderTest {

    @Test
    @DisplayName("only consecutive identical calls fold together")
    void foldsConsecutiveRunsOnly() {
        List<CallNode> children = List.of(
                call("Validator", "check").build(),
                call("Validator", "check").build(),
                call("Auditor", "log").build(),
                call("Validator", "check").build());

        List<LoopFolder.Repetition> folded = LoopFolder.fold(children, true);

        assertThat(folded).extracting(r -> r.node().beanSimpleName() + "x" + r.count())
                .containsExactly("Validatorx2", "Auditorx1", "Validatorx1");
    }

    @Test
    @DisplayName("calls only fold when their whole sub-tree is identical")
    void doesNotFoldDifferentSubTrees() {
        List<CallNode> children = List.of(
                call("Validator", "check").calling(call("Rules", "load")).build(),
                call("Validator", "check").build());

        assertThat(LoopFolder.fold(children, true)).hasSize(2);
    }

    @Test
    @DisplayName("a call which failed never folds with one which succeeded")
    void doesNotFoldAcrossDifferentOutcomes() {
        List<CallNode> children = List.of(
                call("Validator", "check").build(),
                call("Validator", "check").throwing(new IllegalArgumentException()).build());

        assertThat(LoopFolder.fold(children, true)).hasSize(2);
    }

    @Test
    @DisplayName("a plain call and an event of the same signature stay separate")
    void doesNotFoldEventsWithPlainCalls() {
        List<CallNode> children = List.of(
                call("Listener", "onEvent").params("Payload").build(),
                call("Listener", "onEvent").params("Payload").asObserver().build());

        assertThat(LoopFolder.fold(children, true)).hasSize(2);
    }

    @Test
    @DisplayName("folding disabled keeps every call")
    void keepsEveryCallWhenDisabled() {
        List<CallNode> children = List.of(
                call("Validator", "check").build(),
                call("Validator", "check").build());

        assertThat(LoopFolder.fold(children, false))
                .hasSize(2)
                .allSatisfy(repetition -> assertThat(repetition.count()).isOne());
    }

    @Test
    @DisplayName("an empty child-list folds to nothing")
    void handlesNoChildren() {
        assertThat(LoopFolder.fold(List.of(), true)).isEmpty();
    }
}
