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
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowLabel;
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Where a recorded diagram goes and how it is written - shared by the plain file-sink and the
 * use-case report, which need the same directory, the same clash-handling and the same header.
 */
public final class DiagramWriter {

    private DiagramWriter() {
    }

    /**
     * The directory a flow belongs in: a sub-directory per use-case for a labelled flow, the
     * configured output-directory itself for an unlabelled one.
     */
    public static Path directoryFor(FlowConfig config, CallFlow flow) {
        FlowLabel label = flow.label();
        if (label == null || !config.isGroupByLabel()) {
            return config.outputDirectory();
        }
        return config.outputDirectory().resolve(label.directoryName());
    }

    /**
     * Writes a file, and puts a counter in front of an extension rather than overwriting: two flows
     * of the same method can well finish inside the same millisecond.
     */
    public static Path writeNew(Path directory, String baseName, String extension, String content)
            throws IOException {
        Files.createDirectories(directory);
        for (int attempt = 0; ; attempt++) {
            String candidateName = attempt == 0
                    ? baseName + extension
                    : baseName + "-" + attempt + extension;
            Path candidate = directory.resolve(candidateName);
            try {
                Files.writeString(candidate, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return candidate;
            } catch (FileAlreadyExistsException alreadyExists) {
                if (attempt > 1_000) {
                    throw alreadyExists;
                }
            }
        }
    }

    /** Writes a file which is meant to be replaced as more is recorded - an index, or a combined diagram. */
    public static Path replace(Path directory, String fileName, String content) throws IOException {
        Files.createDirectories(directory);
        Path target = directory.resolve(fileName);
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return target;
    }

    /**
     * Puts the configured header line in front of a diagram, as a comment of the notation in use, so
     * a build which insists on a licence header in every file is satisfied without an exclusion.
     */
    public static String withHeader(FlowConfig config, String diagram) {
        String header = config.fileHeader();
        if (header == null || header.isBlank()) {
            return diagram;
        }
        String commentPrefix = config.outputFormat() == DiagramFormat.PLANTUML ? "' " : "%% ";
        StringBuilder comment = new StringBuilder();
        for (String line : header.split("\\R")) {
            comment.append(commentPrefix).append(line.strip()).append('\n');
        }
        int insertAt = afterFrontMatter(diagram);
        return diagram.substring(0, insertAt) + comment + diagram.substring(insertAt);
    }

    /**
     * Where the header may go: after a front-matter block, and at the very top otherwise.
     *
     * <p>Mermaid takes the title of a diagram from front-matter, and only recognizes it when the
     * document begins with it - a comment in front turns the whole diagram into a parse-error. The
     * header therefore follows the front-matter rather than preceding it.
     */
    private static int afterFrontMatter(String diagram) {
        if (!diagram.startsWith("---\n")) {
            return 0;
        }
        int closing = diagram.indexOf("\n---\n", "---".length());
        return closing < 0 ? 0 : closing + "\n---\n".length();
    }
}
