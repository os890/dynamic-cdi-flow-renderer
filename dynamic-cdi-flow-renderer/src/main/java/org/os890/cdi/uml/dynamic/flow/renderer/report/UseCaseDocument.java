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

import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import java.util.Collection;

/**
 * One document holding every use-case recorded so far: what it is, and its diagram inline, so the
 * file reads on its own wherever Markdown with Mermaid in it is rendered.
 */
final class UseCaseDocument {

    static final String FILE_NAME = "use-cases.md";

    private UseCaseDocument() {
    }

    static String of(Collection<UseCaseReport> reports, FlowConfig config) {
        String fence = config.outputFormat() == DiagramFormat.PLANTUML ? "plantuml" : "mermaid";
        StringBuilder document = new StringBuilder()
                .append("# What each use-case does\n\n")
                .append("Recorded by cdi-flow while the use-cases below were driven through the")
                .append(" application. One block per request, in the order the application handled")
                .append(" them; the blocks are the recorded chains, unchanged.\n\n")
                .append("A flow ends when its outermost call returns, and every request is an")
                .append(" outermost call on a thread of its own, so no single recording can span a")
                .append(" use-case. What ties them together is the label each request carried.\n\n");

        for (UseCaseReport report : reports) {
            document.append("## ").append(report.number()).append(". ")
                    .append(report.label().name()).append("\n\n");
            if (report.label().description() != null) {
                document.append(report.label().description()).append("\n\n");
            }
            document.append(report.combinedRequestCount()).append(" request(s), ")
                    .append(report.distinctChainCount()).append(" distinct chain(s) of ")
                    .append(report.recordedChainCount()).append(" recorded — [all of them](")
                    .append(report.label().directoryName()).append("/")
                    .append(UseCaseReport.INDEX_FILE_NAME).append(").\n\n");

            if (report.combinedRequestCount() == 0) {
                document.append("Nothing was recorded for this use-case.\n\n");
            } else if (report.combinedRequestCount() > config.maxCombinedRequests()) {
                document.append("Its ").append(report.combinedRequestCount())
                        .append(" requests are too many to inline here: [`")
                        .append(report.combinedFileName()).append("`](")
                        .append(report.label().directoryName()).append("/")
                        .append(report.combinedFileName()).append(").\n\n");
            } else {
                document.append("```").append(fence).append("\n")
                        .append(report.combinedDiagram().stripTrailing()).append("\n```\n\n");
            }
        }
        return document.toString();
    }
}
