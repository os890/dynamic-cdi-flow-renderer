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
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Walks the recorded call-tree and leaves the notation to the subclass.
 * <p>
 * Both supported formats describe the same thing with slightly different tokens, so sharing the
 * traversal is what keeps them from drifting apart: participant order, loop-folding, activation
 * nesting and the outcome of a call are decided here, exactly once.
 */
abstract class AbstractSequenceDiagramRenderer implements SequenceDiagramRenderer {

    protected static final String INDENT = "    ";

    protected final FlowConfig config;

    protected AbstractSequenceDiagramRenderer(FlowConfig config) {
        this.config = config;
    }

    @Override
    public final String render(CallFlow flow) {
        ParticipantNamer namer = new ParticipantNamer(collectParticipants(flow.root()));

        StringBuilder out = new StringBuilder(512);
        appendPrologue(out, flow, namer);
        emitNode(out, ParticipantNamer.CALLER_ID, flow.root(), namer, baseDepth());
        appendEpilogue(out);
        return out.toString();
    }

    private void emitNode(StringBuilder out, String callerId, CallNode node,
                          ParticipantNamer namer, int depth) {
        String id = namer.idFor(node.beanClassName());

        appendMessage(out, depth, callerId, id, callArrow(node.isObserverMethod()), label(node));
        appendActivate(out, depth, id);

        emitChildren(out, id, node, namer, depth + 1);

        String outcome = node.hasFailed()
                ? "throws " + escape(node.thrownTypeName())
                : escape(node.returnTypeName());
        appendMessage(out, depth, id, callerId,
                node.hasFailed() ? failureArrow() : returnArrow(),
                outcome + " [" + formatDuration(node.durationNanos()) + "]");

        if (node.isHotspot()) {
            appendHotspotNote(out, depth, id, hotspotText(node));
        }

        appendDeactivate(out, depth, id);
    }

    private void emitChildren(StringBuilder out, String parentId, CallNode parent,
                              ParticipantNamer namer, int depth) {
        for (LoopFolder.Repetition repetition : LoopFolder.fold(parent.children(), config.isFoldLoops())) {
            if (repetition.count() > 1) {
                appendLoopStart(out, depth, repetition.count());
                emitNode(out, parentId, repetition.node(), namer, depth + 1);
                appendLoopEnd(out, depth);
            } else {
                emitNode(out, parentId, repetition.node(), namer, depth);
            }
        }
    }

    protected String hotspotText(CallNode node) {
        return "HOTSPOT " + escape(node.beanSimpleName()) + "." + escape(node.methodName())
                + " took " + formatDuration(node.durationNanos())
                + " (over " + config.hotspotThresholdMillis() + " ms)";
    }

    protected String label(CallNode node) {
        String signature = escape(node.signature());
        return node.isObserverMethod() ? "[event] " + signature : signature;
    }

    /** indentation-depth of the outermost call - the notations differ in how much they wrap */
    protected abstract int baseDepth();

    protected abstract void appendPrologue(StringBuilder out, CallFlow flow, ParticipantNamer namer);

    protected abstract void appendEpilogue(StringBuilder out);

    protected abstract void appendMessage(StringBuilder out, int depth, String from, String to,
                                          String arrow, String label);

    protected abstract void appendActivate(StringBuilder out, int depth, String participantId);

    protected abstract void appendDeactivate(StringBuilder out, int depth, String participantId);

    /** the marker of the innermost slow call of a branch - see {@link HotspotDetector} */
    protected abstract void appendHotspotNote(StringBuilder out, int depth, String participantId,
                                              String text);

    protected abstract void appendLoopStart(StringBuilder out, int depth, int count);

    protected abstract void appendLoopEnd(StringBuilder out, int depth);

    protected abstract String callArrow(boolean observerMethod);

    protected abstract String returnArrow();

    protected abstract String failureArrow();

    /** keeps characters which would terminate or re-interpret a statement out of a label */
    protected abstract String escape(String text);

    /** bean-classes in the order they first show up, so the participant-lanes match the flow */
    protected static Map<String, String> collectParticipants(CallNode root) {
        Map<String, String> simpleNameByClassName = new LinkedHashMap<>();
        collectParticipants(root, simpleNameByClassName);
        return simpleNameByClassName;
    }

    private static void collectParticipants(CallNode node, Map<String, String> target) {
        target.putIfAbsent(node.beanClassName(), node.beanSimpleName());
        for (CallNode child : node.children()) {
            collectParticipants(child, target);
        }
    }

    protected static void indent(StringBuilder out, int depth) {
        out.append(INDENT.repeat(depth));
    }

    /**
     * Puts the use-case a flow was recorded under on the diagram, as its title.
     *
     * <p>Which is where a reader looks for it: the label is otherwise only in the directory-name and
     * in the generated index, and a diagram copied out of either says nothing about what it belongs
     * to. A flow without a label gets no title, so an application which records no use-cases sees
     * exactly the diagrams it saw before.
     */
    protected abstract void appendTitle(StringBuilder out, CallFlow flow);

    protected static String formatDuration(long nanos) {
        double millis = nanos / 1_000_000d;
        if (millis < 10) {
            return String.format(Locale.ROOT, "%.2f ms", millis);
        }
        return String.format(Locale.ROOT, "%.1f ms", millis);
    }

    /**
     * The lane the header-note is spanned to. A note which is wider than the lanes it covers is
     * drawn outside of the diagram bounds and gets cut off when the diagram is exported to an image.
     */
    protected static String lastParticipantId(ParticipantNamer namer) {
        return namer.participants().keySet().stream()
                .reduce((first, second) -> second)
                .orElse(ParticipantNamer.CALLER_ID);
    }
}
