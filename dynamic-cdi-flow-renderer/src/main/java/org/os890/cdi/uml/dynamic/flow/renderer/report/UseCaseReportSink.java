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
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowLabel;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.sink.DiagramWriter;
import org.os890.cdi.uml.dynamic.flow.renderer.sink.FileFlowSink;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sorts the recorded flows into the use-case each of them was labelled with, and keeps the files of
 * every use-case up to date as it goes on: one diagram per distinct chain, the combined diagram of
 * the use-case, an index of both, and one document listing all of them.
 *
 * <p>An unlabelled flow is handed to the plain {@link FileFlowSink} instead, which is what an
 * application that never sets a label sees: one file per recorded chain, as before.
 *
 * <p>The files are rewritten while the recording is still going on rather than at the end, because
 * there is no reliable "end": a suite may be interrupted, and a dev-mode process may run for hours.
 */
public final class UseCaseReportSink implements FlowSink {

    private static final Logger LOGGER = Logger.getLogger(UseCaseReportSink.class.getName());

    private final FlowConfig config;
    private final FileFlowSink unlabelledSink;

    /** in the order the use-cases first appeared, which is the order they were driven in */
    private final Map<String, UseCaseReport> reports = new LinkedHashMap<>();

    public UseCaseReportSink(FlowConfig config) {
        this.config = config;
        this.unlabelledSink = new FileFlowSink(config);
    }

    @Override
    public void onFlowRecorded(CallFlow flow) {
        FlowLabel label = flow.label();
        if (label == null || !config.isGroupByLabel() || !config.isReport()) {
            unlabelledSink.onFlowRecorded(flow);
            return;
        }
        try {
            synchronized (this) {
                UseCaseReport report = reports.computeIfAbsent(label.directoryName(),
                        directoryName -> new UseCaseReport(config, label, reports.size() + 1));
                report.add(flow);
                report.write();
                if (config.isWriteFiles()) {
                    DiagramWriter.replace(config.outputDirectory(), UseCaseDocument.FILE_NAME,
                            UseCaseDocument.of(reports.values(), config));
                }
            }
        } catch (IOException | RuntimeException e) {
            //reporting must never break the business-call which produced the flow
            LOGGER.log(Level.WARNING, e, () -> "could not report the recorded flow " + flow);
        }
    }
}
