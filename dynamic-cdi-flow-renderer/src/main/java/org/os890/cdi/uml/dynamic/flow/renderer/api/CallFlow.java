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

package org.os890.cdi.uml.dynamic.flow.renderer.api;

import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.render.MermaidSequenceRenderer;
import org.os890.cdi.uml.dynamic.flow.renderer.render.PlantUmlSequenceRenderer;
import org.os890.cdi.uml.dynamic.flow.renderer.render.SequenceDiagramRenderer;

/**
 * A complete call-chain: everything which happened on one thread between entering and leaving the
 * outermost recorded bean-method.
 */
public final class CallFlow {

    private final CallNode root;
    private final String threadName;
    private final FlowConfig config;
    private final FlowLabel label;

    public CallFlow(CallNode root, String threadName, FlowConfig config) {
        this(root, threadName, config, null);
    }

    public CallFlow(CallNode root, String threadName, FlowConfig config, FlowLabel label) {
        this.root = root;
        this.threadName = threadName;
        this.config = config;
        this.label = label;
    }

    public CallNode root() {
        return root;
    }

    public String threadName() {
        return threadName;
    }

    /**
     * @return the use-case this flow was recorded under, or {@code null} when nothing labelled the
     * thread it started on - see {@link FlowLabel}
     */
    public FlowLabel label() {
        return label;
    }

    public FlowConfig config() {
        return config;
    }

    /**
     * The bean which triggered the first interceptor-call - the entry-point of the flow.
     */
    public String entryTypeSimpleName() {
        return root.beanSimpleName();
    }

    public String entryMethodName() {
        return root.methodName();
    }

    public long startedAtEpochMillis() {
        return root.startedAtEpochMillis();
    }

    public long finishedAtEpochMillis() {
        return root.finishedAtEpochMillis();
    }

    public long durationNanos() {
        return root.durationNanos();
    }

    /**
     * Renders the flow in the configured notation - Mermaid unless
     * {@code cdi-flow.output-format} says otherwise.
     */
    public String toDiagram() {
        return toDiagram(config.outputFormat());
    }

    public String toDiagram(DiagramFormat format) {
        return SequenceDiagramRenderer.of(format, config).render(this);
    }

    public String toMermaid() {
        return new MermaidSequenceRenderer(config).render(this);
    }

    public String toPlantUml() {
        return new PlantUmlSequenceRenderer(config).render(this);
    }

    @Override
    public String toString() {
        return "CallFlow[" + root + " on " + threadName + "]";
    }
}
