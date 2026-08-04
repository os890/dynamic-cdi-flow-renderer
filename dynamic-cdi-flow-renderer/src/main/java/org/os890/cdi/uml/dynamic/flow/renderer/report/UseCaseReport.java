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
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.sink.DiagramFileNamer;
import org.os890.cdi.uml.dynamic.flow.renderer.sink.DiagramWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything recorded under one label: the chains in the order they happened, one file per distinct
 * chain, the combined diagram of the use-case and an index of both.
 */
final class UseCaseReport {

    static final String COMBINED_FILE_NAME = "use-case";
    static final String INDEX_FILE_NAME = "README.md";

    private final FlowConfig config;
    private final FlowLabel label;
    private final int number;
    private final Path directory;

    /** every chain, duplicates included: a request the use-case made twice happened twice */
    private final List<RecordedChain> chains = new ArrayList<>();

    /** one entry per distinct chain-shape, in the order first seen */
    private final Map<String, DistinctChain> distinctChains = new LinkedHashMap<>();

    UseCaseReport(FlowConfig config, FlowLabel label, int number) {
        this.config = config;
        this.label = label;
        this.number = number;
        this.directory = config.outputDirectory().resolve(label.directoryName());
    }

    /**
     * Takes in one recorded chain: written as a file when its shape is new, counted when it is not,
     * and part of the combined diagram either way.
     */
    void add(CallFlow flow) throws IOException {
        String diagram = flow.toDiagram();
        String shape = ChainShape.of(diagram);
        DistinctChain known = distinctChains.get(shape);
        String fileName;
        if (known == null) {
            fileName = config.isWriteFiles()
                    ? DiagramWriter.writeNew(directory, DiagramFileNamer.baseNameFor(flow),
                            config.outputFormat().fileExtension(),
                            DiagramWriter.withHeader(config, diagram)).getFileName().toString()
                    : DiagramFileNamer.fileNameFor(flow);
            distinctChains.put(shape, new DistinctChain(fileName,
                    flow.entryTypeSimpleName() + "." + flow.entryMethodName()));
        } else {
            known.occurred();
            fileName = known.fileName();
        }
        if (isPartOfCombinedDiagram(flow)) {
            chains.add(new RecordedChain(flow, fileName, diagram));
        }
    }

    /**
     * Whether a chain belongs in the story of the use-case. Everything is recorded; what an
     * application considers noise - a session-check running before every single request, say - is
     * named in {@code cdi-flow.combined-exclude-pattern} and left out of the combined diagram only.
     */
    private boolean isPartOfCombinedDiagram(CallFlow flow) {
        return config.combinedExcludePattern() == null
                || !config.combinedExcludePattern()
                        .matcher(flow.entryTypeSimpleName() + "." + flow.entryMethodName()).matches();
    }

    /** Rewritten as the use-case goes on, so the files are usable while the suite is still running. */
    void write() throws IOException {
        if (!config.isWriteFiles() || chains.isEmpty()) {
            return;
        }
        DiagramWriter.replace(directory, COMBINED_FILE_NAME + config.outputFormat().fileExtension(),
                DiagramWriter.withHeader(config, CombinedDiagram.of(config.outputFormat(), chains)));
        DiagramWriter.replace(directory, INDEX_FILE_NAME, index());
    }

    private String index() {
        StringBuilder index = new StringBuilder("# ").append(label.name()).append("\n\n");
        if (label.description() != null) {
            index.append(label.description()).append("\n\n");
        }
        index.append("**The use-case as one diagram: [`").append(combinedFileName()).append("`](")
                .append(combinedFileName()).append(")** — the ").append(chains.size())
                .append(" chain(s) below, in the order the application handled them, one block per")
                .append(" request.\n\n")
                .append(distinctChains.size()).append(" distinct call chain(s), out of ")
                .append(recordedChainCount()).append(" recorded:\n\n")
                .append("| Entry point | Diagram | Recorded |\n|---|---|---|\n");
        for (DistinctChain chain : distinctChains.values()) {
            index.append("| `").append(chain.entryPoint()).append("` | [`").append(chain.fileName())
                    .append("`](").append(chain.fileName()).append(") | ")
                    .append(chain.occurrences()).append("× |\n");
        }
        return index.toString();
    }

    String combinedFileName() {
        return COMBINED_FILE_NAME + config.outputFormat().fileExtension();
    }

    FlowLabel label() {
        return label;
    }

    int number() {
        return number;
    }

    Path directory() {
        return directory;
    }

    /** how many requests the combined diagram holds */
    int combinedRequestCount() {
        return chains.size();
    }

    int distinctChainCount() {
        return distinctChains.size();
    }

    int recordedChainCount() {
        int recorded = 0;
        for (DistinctChain chain : distinctChains.values()) {
            recorded += chain.occurrences();
        }
        return recorded;
    }

    String combinedDiagram() {
        return CombinedDiagram.of(config.outputFormat(), chains);
    }

    /** One chain kept as a file, and how often that same shape was recorded. */
    private static final class DistinctChain {

        private final String fileName;
        private final String entryPoint;
        private int occurrences = 1;

        private DistinctChain(String fileName, String entryPoint) {
            this.fileName = fileName;
            this.entryPoint = entryPoint;
        }

        private void occurred() {
            occurrences++;
        }

        private String fileName() {
            return fileName;
        }

        private String entryPoint() {
            return entryPoint;
        }

        private int occurrences() {
            return occurrences;
        }
    }
}
