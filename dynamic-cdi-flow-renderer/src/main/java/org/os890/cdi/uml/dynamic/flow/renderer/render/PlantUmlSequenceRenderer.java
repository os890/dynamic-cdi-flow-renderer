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

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders a recorded call-flow as a PlantUML sequence-diagram.
 * <p>
 * The structure is identical to the Mermaid output - only the notation differs:
 * {@code A -> B : msg} instead of {@code A->>B: msg}, {@code "label" as Id} instead of
 * {@code Id as label}, and the whole diagram is wrapped in {@code @startuml} / {@code @enduml}.
 */
public final class PlantUmlSequenceRenderer extends AbstractSequenceDiagramRenderer {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter END_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    public PlantUmlSequenceRenderer(FlowConfig config) {
        super(config);
    }

    /** PlantUML has a title-directive of its own; one line, no quoting needed. */
    @Override
    protected void appendTitle(StringBuilder out, CallFlow flow) {
        if (flow.label() == null || !config.isTitleDiagrams()) {
            return;
        }
        out.append("title ").append(escape(flow.label().name())).append('\n');
    }

    @Override
    protected int baseDepth() {
        return 0;
    }

    @Override
    protected void appendPrologue(StringBuilder out, CallFlow flow, ParticipantNamer namer) {
        out.append("@startuml\n");
        appendTitle(out, flow);
        out.append("autonumber\n");
        out.append("hide footbox\n");
        out.append("participant \"").append(ParticipantNamer.CALLER_DISPLAY_NAME)
                .append("\" as ").append(ParticipantNamer.CALLER_ID).append('\n');

        namer.participants().forEach((id, displayName) -> {
            if (id.equals(displayName)) {
                out.append("participant ").append(id).append('\n');
            } else {
                out.append("participant \"").append(escape(displayName)).append("\" as ")
                        .append(id).append('\n');
            }
        });

        //the block form keeps the two lines readable and needs no escaping of line-breaks
        out.append("note over ").append(ParticipantNamer.CALLER_ID)
                .append(", ").append(lastParticipantId(namer)).append('\n');
        out.append(INDENT)
                .append(TIME_FORMAT.format(Instant.ofEpochMilli(flow.startedAtEpochMillis())))
                .append(" - ").append(END_TIME_FORMAT.format(Instant.ofEpochMilli(flow.finishedAtEpochMillis())))
                .append('\n');
        out.append(INDENT).append(formatDuration(flow.durationNanos()))
                .append(" | thread ").append(escape(flow.threadName())).append('\n');
        out.append("end note\n");
    }

    @Override
    protected void appendEpilogue(StringBuilder out) {
        out.append("@enduml\n");
    }

    @Override
    protected void appendMessage(StringBuilder out, int depth, String from, String to,
                                 String arrow, String label) {
        indent(out, depth);
        out.append(from).append(' ').append(arrow).append(' ').append(to)
                .append(" : ").append(label).append('\n');
    }

    @Override
    protected void appendActivate(StringBuilder out, int depth, String participantId) {
        indent(out, depth);
        out.append("activate ").append(participantId).append('\n');
    }

    @Override
    protected void appendDeactivate(StringBuilder out, int depth, String participantId) {
        indent(out, depth);
        out.append("deactivate ").append(participantId).append('\n');
    }

    @Override
    protected void appendHotspotNote(StringBuilder out, int depth, String participantId, String text) {
        indent(out, depth);
        out.append("note over ").append(participantId).append(" : ").append(text).append('\n');
    }

    @Override
    protected void appendLoopStart(StringBuilder out, int depth, int count) {
        indent(out, depth);
        out.append("loop ").append(count).append(" times\n");
    }

    @Override
    protected void appendLoopEnd(StringBuilder out, int depth) {
        indent(out, depth);
        out.append("end\n");
    }

    @Override
    protected String callArrow(boolean observerMethod) {
        //->> is PlantUML's asynchronous message, which is what an observer-notification is
        return observerMethod ? "->>" : "->";
    }

    @Override
    protected String returnArrow() {
        return "-->";
    }

    @Override
    protected String failureArrow() {
        return "-->x";
    }

    @Override
    protected String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r", " ")
                .replace("\n", " ")
                .replace("\"", "'");
    }
}
