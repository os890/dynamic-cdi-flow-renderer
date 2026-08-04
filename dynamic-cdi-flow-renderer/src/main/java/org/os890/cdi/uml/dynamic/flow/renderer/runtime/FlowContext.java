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

package org.os890.cdi.uml.dynamic.flow.renderer.runtime;

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowLabel;
import org.os890.cdi.uml.dynamic.flow.renderer.render.HotspotDetector;
import org.os890.cdi.uml.dynamic.flow.renderer.render.ProxyFrameCollapser;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The call-chain currently being recorded on this thread.
 * <p>
 * Nesting is a plain stack: every intercepted call pushes a node below its caller, and the flow is
 * complete once the stack runs empty again - which is exactly the outermost interceptor-call.
 */
final class FlowContext {

    private static final ThreadLocal<FlowContext> CURRENT = new ThreadLocal<>();

    private final Deque<CallNode> stack = new ArrayDeque<>();
    private CallNode root;
    private FlowLabel label;
    private boolean suspended;

    private FlowContext() {
    }

    static FlowContext current() {
        FlowContext context = CURRENT.get();
        if (context == null) {
            context = new FlowContext();
            CURRENT.set(context);
        }
        return context;
    }

    static void clear() {
        CURRENT.remove();
    }

    boolean isSuspended() {
        return suspended;
    }

    void push(CallNode node) {
        CallNode parent = stack.peek();
        if (parent == null) {
            root = node;
            //read when the flow starts: a flow which outlives the label keeps the one it began with
            label = FlowLabel.current();
        } else {
            parent.addChild(node);
        }
        stack.push(node);
    }

    /**
     * @return {@code true} when the outermost recorded call just returned, i.e. the flow is complete
     */
    boolean pop() {
        stack.pop();
        return stack.isEmpty();
    }

    /**
     * Normalizes the recorded tree and hands it to the sinks.
     * <p>
     * The context is suspended while publishing: sinks may well be CDI beans themselves, and
     * recording the recorder would recurse forever.
     */
    void publish(FlowRuntime runtime) {
        CallNode completedRoot = root;
        FlowLabel completedLabel = label;
        root = null;
        label = null;
        if (completedRoot == null) {
            clear();
            return;
        }

        suspended = true;
        try {
            CallNode normalizedRoot = runtime.config().isCollapseProxyFrames()
                    ? ProxyFrameCollapser.collapse(completedRoot)
                    : completedRoot;
            //after collapsing - a collapsed proxy-frame must not be reported as the hotspot
            HotspotDetector.markHotspots(normalizedRoot, runtime.config().hotspotThresholdMillis());
            runtime.publish(new CallFlow(normalizedRoot, Thread.currentThread().getName(),
                    runtime.config(), completedLabel));
        } finally {
            suspended = false;
            clear();
        }
    }
}
