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

package org.os890.cdi.uml.dynamic.flow.renderer.report;

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Combines a list of recorded flows into <strong>one</strong> sequence-diagram - one block per
 * flow, in the order they are handed over, sharing the participant-lanes.
 *
 * <p>The same rendering {@link UseCaseReport} writes as {@code use-case.mmd} respectively
 * {@code use-case.puml}, reachable without a file and without a label: a caller which holds the
 * flows it is interested in - a test asserting what a scenario did, for instance - renders them
 * here in whichever notation it wants.
 *
 * <p>What it deliberately does <em>not</em> do is the part {@link UseCaseReport} adds on top:
 * identical chains are <strong>not</strong> collapsed and the list is <strong>not</strong> capped
 * at {@code cdi-flow.max-combined-requests}. A caller passing the same chain twice gets two
 * blocks, which is what an assertion needs - collapsing would hide a repetition and a cap would
 * silently truncate.
 */
public final class CombinedFlowDiagram {

    private CombinedFlowDiagram() {
    }

    /**
     * @param flows  the flows to combine, in the order they should appear; must not be
     *               {@code null}, may be empty - which yields the bare diagram-header
     * @param format the notation to render in; each flow is rendered in it as well, so the
     *               notation configured via {@code cdi-flow.output-format} does not leak in
     * @param title  the title of the combined diagram, or {@code null} for none
     * @return the combined diagram, one block per flow
     */
    public static String of(List<CallFlow> flows, DiagramFormat format, String title) {
        Objects.requireNonNull(flows, "flows");
        Objects.requireNonNull(format, "format");

        List<RecordedChain> chains = new ArrayList<>(flows.size());
        for (CallFlow flow : flows) {
            chains.add(new RecordedChain(flow, "", flow.toDiagram(format)));
        }
        return CombinedDiagram.of(format, title, chains);
    }
}
