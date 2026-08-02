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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.loops.BatchService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same loop with {@code cdi-flow.fold-loops=false} - every iteration is drawn.
 */
class UnfoldedLoopFlowIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.configured()
                .foldingLoops(false)
                .startFor(UnfoldedLoopFlowIT.class);
    }

    @AfterAll
    static void stopContainer() {
        container.close();
    }

    @Test
    @DisplayName("with folding switched off every iteration is drawn separately")
    void rendersEveryIteration() {
        container.get(BatchService.class).processAll(List.of("a", "b", "c", "d", "e"));

        String diagram = container.flows().singleEnteredAt(BatchService.class).toMermaid();

        assertThat(diagram).doesNotContain("loop ");
        assertThat(MermaidAssertions.countOccurrences(diagram, "ItemValidator: validate(String)"))
                .isEqualTo(5);
        assertThat(MermaidAssertions.countOccurrences(diagram, "ItemNormalizer: normalize(String)"))
                .isEqualTo(5);
        MermaidAssertions.assertWellFormed(diagram);
    }
}
