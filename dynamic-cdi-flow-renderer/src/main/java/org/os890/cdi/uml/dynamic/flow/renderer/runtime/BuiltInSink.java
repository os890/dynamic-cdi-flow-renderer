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

import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.report.UseCaseReportSink;

/**
 * The sink the recorder arms itself with, so no integration has to decide this.
 *
 * <p>It is always the use-case report: for a labelled flow that writes the use-case's diagrams and
 * its index, and for an unlabelled one it does exactly what the plain file-sink did - one file per
 * recorded chain in the output-directory.
 */
public final class BuiltInSink {

    private BuiltInSink() {
    }

    public static FlowSink of(FlowConfig config) {
        return new UseCaseReportSink(config);
    }
}
