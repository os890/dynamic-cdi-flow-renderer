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

/**
 * One recorded chain, taken apart far enough to be put next to the others: the lanes it declares,
 * the arrows in between, and the line the recorder wrote about its timing.
 *
 * <p>Working on the rendered text rather than on the model is deliberate. The renderers decide how a
 * name is cleaned, how a loop folds and where a hotspot-note goes, and a combined diagram which
 * re-derived any of that from the model would drift away from the single diagrams beside it.
 */
final class RecordedChain {

    private final String entryPoint;
    private final String fileName;
    private final String diagram;
    private final List<String> participants = new ArrayList<>();
    private final List<String> body = new ArrayList<>();
    private final String timing;

    RecordedChain(CallFlow flow, String fileName, String diagram) {
        this.entryPoint = flow.entryTypeSimpleName() + "." + flow.entryMethodName();
        this.fileName = fileName;
        this.diagram = diagram;

        String timingLine = "";
        boolean insideNote = false;
        for (String line : diagram.split("\\R")) {
            String trimmed = line.strip();
            if (insideNote) {
                if (trimmed.equals("end note")) {
                    insideNote = false;
                } else if (timingLine.isEmpty() && trimmed.contains(" ms ")) {
                    timingLine = trimmed;
                }
                continue;
            }
            if (isHeaderLine(trimmed)) {
                continue;
            }
            if (trimmed.startsWith("participant ")) {
                //the caller-lane is declared once for the whole combined diagram
                if (!trimmed.equals("participant Caller as caller")
                        && !trimmed.equals("participant \"caller\" as Caller")) {
                    participants.add(trimmed);
                }
                continue;
            }
            if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("note over caller,")) {
                int mermaidBreak = trimmed.indexOf("<br/>");
                if (mermaidBreak >= 0) {
                    timingLine = trimmed.substring(mermaidBreak + "<br/>".length()).strip();
                } else {
                    insideNote = true;
                }
                continue;
            }
            body.add(line);
        }
        this.timing = timingLine;
    }

    private static boolean isHeaderLine(String trimmed) {
        return trimmed.isEmpty()
                //the title of a single chain: the combined diagram carries the use-case's own
                || trimmed.equals("---")
                || trimmed.startsWith("title:")
                || trimmed.startsWith("title ")
                || trimmed.equals("sequenceDiagram")
                || trimmed.equals("autonumber")
                || trimmed.equals("@startuml")
                || trimmed.equals("@enduml")
                || trimmed.equals("hide footbox");
    }

    String entryPoint() {
        return entryPoint;
    }

    String fileName() {
        return fileName;
    }

    String diagram() {
        return diagram;
    }

    List<String> participants() {
        return participants;
    }

    List<String> body() {
        return body;
    }

    /** e.g. {@code 10.2 ms | thread executor-thread-2}, or empty when the note was not recognized */
    String timing() {
        return timing;
    }

    /** the lane the block-note may span to; the caller-lane on its own when nothing else was called */
    String lastParticipant(DiagramFormat format) {
        if (participants.isEmpty()) {
            return "Caller";
        }
        String declaration = participants.get(participants.size() - 1);
        if (format == DiagramFormat.PLANTUML) {
            int alias = declaration.lastIndexOf(" as ");
            return alias < 0 ? declaration.substring("participant ".length()) : declaration.substring(alias + 4);
        }
        String name = declaration.substring("participant ".length());
        int alias = name.indexOf(" as ");
        return alias < 0 ? name : name.substring(0, alias);
    }

    String title() {
        return timing.isEmpty() ? entryPoint : entryPoint + " — " + timing;
    }
}
