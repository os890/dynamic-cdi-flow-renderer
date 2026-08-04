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

package org.os890.cdi.uml.dynamic.flow.renderer.sink;

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes every recorded flow as a {@code .mmd} file into the configured output-directory
 * (the tmp-directory by default), or into the sub-directory of the use-case it was labelled with.
 */
public final class FileFlowSink implements FlowSink {

    private static final Logger LOGGER = Logger.getLogger(FileFlowSink.class.getName());

    private final FlowConfig config;

    public FileFlowSink(FlowConfig config) {
        this.config = config;
    }

    @Override
    public void onFlowRecorded(CallFlow flow) {
        if (!config.isWriteFiles()) {
            return;
        }

        try {
            Path target = DiagramWriter.writeNew(DiagramWriter.directoryFor(config, flow),
                    DiagramFileNamer.baseNameFor(flow), config.outputFormat().fileExtension(),
                    DiagramWriter.withHeader(config, flow.toDiagram()));
            LOGGER.log(Level.FINE, () -> "recorded call-flow written to " + target);
        } catch (IOException | RuntimeException e) {
            //writing a diagram must never break the business-call which produced it
            LOGGER.log(Level.WARNING, e, () -> "could not write the call-flow diagram for " + flow);
        }
    }
}
