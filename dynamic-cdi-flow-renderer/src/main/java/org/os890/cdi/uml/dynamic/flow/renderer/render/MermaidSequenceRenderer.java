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
 * Renders a recorded call-flow as a Mermaid sequence-diagram.
 */
public final class MermaidSequenceRenderer extends AbstractSequenceDiagramRenderer {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    /** the end of a flow is almost always on the same day - repeating the date just costs width */
    private static final DateTimeFormatter END_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    public MermaidSequenceRenderer(FlowConfig config) {
        super(config);
    }

    @Override
    protected int baseDepth() {
        return 1;
    }

    @Override
    protected void appendPrologue(StringBuilder out, CallFlow flow, ParticipantNamer namer) {
        out.append("sequenceDiagram\n");
        out.append(INDENT).append("autonumber\n");
        out.append(INDENT).append("participant ").append(ParticipantNamer.CALLER_ID)
                .append(" as ").append(ParticipantNamer.CALLER_DISPLAY_NAME).append('\n');

        namer.participants().forEach((id, displayName) -> {
            out.append(INDENT).append("participant ").append(id);
            if (!id.equals(displayName)) {
                out.append(" as ").append(displayName);
            }
            out.append('\n');
        });

        out.append(INDENT).append("Note over ").append(ParticipantNamer.CALLER_ID)
                .append(',').append(lastParticipantId(namer)).append(": ")
                .append(TIME_FORMAT.format(Instant.ofEpochMilli(flow.startedAtEpochMillis())))
                .append(" - ").append(END_TIME_FORMAT.format(Instant.ofEpochMilli(flow.finishedAtEpochMillis())))
                .append("<br/>").append(formatDuration(flow.durationNanos()))
                .append(" | thread ").append(escape(flow.threadName()))
                .append('\n');
    }

    @Override
    protected void appendEpilogue(StringBuilder out) {
        //nothing - a Mermaid diagram has no closing token
    }

    @Override
    protected void appendMessage(StringBuilder out, int depth, String from, String to,
                                 String arrow, String label) {
        indent(out, depth);
        out.append(from).append(arrow).append(to).append(": ").append(label).append('\n');
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
        out.append("Note over ").append(participantId).append(": ").append(text).append('\n');
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
        return observerMethod ? "-)" : "->>";
    }

    @Override
    protected String returnArrow() {
        return "-->>";
    }

    @Override
    protected String failureArrow() {
        return "--x";
    }

    @Override
    protected String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r", " ")
                .replace("\n", " ")
                .replace(";", ",")
                .replace("#", "no.");
    }
}
