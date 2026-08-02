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
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

/**
 * Turns a recorded call-flow into the text of a sequence-diagram.
 */
@FunctionalInterface
public interface SequenceDiagramRenderer {

    String render(CallFlow flow);

    static SequenceDiagramRenderer of(DiagramFormat format, FlowConfig config) {
        return switch (format) {
            case MERMAID -> new MermaidSequenceRenderer(config);
            case PLANTUML -> new PlantUmlSequenceRenderer(config);
        };
    }
}
