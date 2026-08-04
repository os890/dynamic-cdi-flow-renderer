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

import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The whole use-case as one diagram: every chain it produced, in the order the application handled
 * them, each in a block of its own.
 *
 * <p>It is titled with the use-case it belongs to, which is where a reader looks for that - a diagram
 * copied out of a directory otherwise says nothing about what it is.
 *
 * <p>This is stitched, not recorded, and it has to be: a flow ends when its outermost call returns,
 * and a request is an outermost call on a thread of its own, so no recording can span two of them.
 * What is stitched are the recorded chains themselves - their arrows are copied across unchanged,
 * and only the note about the timing of a single chain is replaced by the block-label.
 */
final class CombinedDiagram {

    private static final String MERMAID_BLOCK_COLOUR = "rect rgb(244, 244, 244)";

    private CombinedDiagram() {
    }

    /**
     * @param useCase the title to carry, or {@code null} when titles are switched off
     */
    static String of(DiagramFormat format, String useCase, List<RecordedChain> chains) {
        return format == DiagramFormat.PLANTUML ? plantUml(useCase, chains) : mermaid(useCase, chains);
    }

    private static String mermaid(String useCase, List<RecordedChain> chains) {
        List<String> lines = new ArrayList<>();
        //front-matter is YAML, so the use-case is quoted and a quote inside it escaped
        if (useCase != null) {
            lines.add("---");
            lines.add("title: \"" + useCase.replace("\"", "\\\"") + "\"");
            lines.add("---");
        }
        lines.add("sequenceDiagram");
        lines.add("    autonumber");
        lines.add("    participant Caller as caller");
        lines.addAll(indent(participantsOf(chains)));
        for (RecordedChain chain : chains) {
            lines.add("    " + MERMAID_BLOCK_COLOUR);
            lines.add("        Note over Caller," + chain.lastParticipant(DiagramFormat.MERMAID)
                    + ": " + chain.title());
            for (String bodyLine : chain.body()) {
                lines.add("    " + bodyLine);
            }
            lines.add("    end");
        }
        return String.join("\n", lines) + "\n";
    }

    private static String plantUml(String useCase, List<RecordedChain> chains) {
        List<String> lines = new ArrayList<>();
        lines.add("@startuml");
        if (useCase != null) {
            lines.add("title " + useCase.replace("\n", " ").replace("\"", "'"));
        }
        lines.add("autonumber");
        lines.add("hide footbox");
        lines.add("participant \"caller\" as Caller");
        lines.addAll(participantsOf(chains));
        for (RecordedChain chain : chains) {
            lines.add("group " + chain.title());
            for (String bodyLine : chain.body()) {
                lines.add("    " + bodyLine);
            }
            lines.add("end");
        }
        lines.add("@enduml");
        return String.join("\n", lines) + "\n";
    }

    /** every lane any chain declares, in the order it was first seen */
    private static Set<String> participantsOf(List<RecordedChain> chains) {
        Set<String> participants = new LinkedHashSet<>();
        for (RecordedChain chain : chains) {
            participants.addAll(chain.participants());
        }
        return participants;
    }

    private static List<String> indent(Set<String> lines) {
        List<String> indented = new ArrayList<>(lines.size());
        for (String line : lines) {
            indented.add("    " + line);
        }
        return indented;
    }
}
