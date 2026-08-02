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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.PlantUmlAssertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class FileFlowSinkTest {

    private static CallFlow flow(FlowConfig config) {
        return call("OrderService", "placeOrder").params("String")
                .epochMillis(1_754_120_832_345L, 1_754_120_832_401L)
                .calling(call("PricingService", "priceOf").returning("BigDecimal"))
                .buildFlow(config);
    }

    @Test
    @DisplayName("the diagram is written into the configured directory, which is created on demand")
    void writesTheDiagram(@TempDir Path tempDir) throws IOException {
        Path outputDirectory = tempDir.resolve("not").resolve("there").resolve("yet");
        FlowConfig config = FlowConfig.builder().outputDirectory(outputDirectory).build();

        new FileFlowSink(config).onFlowRecorded(flow(config));

        List<Path> written = Files.list(outputDirectory).toList();
        assertThat(written).singleElement()
                .satisfies(file -> assertThat(file.getFileName().toString())
                        .matches("OrderService_placeOrder_\\d{8}-\\d{9}_\\d{8}-\\d{9}\\.mmd"));
        MermaidAssertions.assertWellFormed(Files.readString(written.get(0)));
    }

    @Test
    @DisplayName("the plantuml format is written as .puml")
    void writesPlantUml(@TempDir Path tempDir) throws IOException {
        FlowConfig config = FlowConfig.builder()
                .outputDirectory(tempDir)
                .outputFormat(org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat.PLANTUML)
                .build();

        new FileFlowSink(config).onFlowRecorded(flow(config));

        List<Path> written = Files.list(tempDir).toList();
        assertThat(written).singleElement()
                .satisfies(file -> assertThat(file.getFileName().toString())
                        .matches("OrderService_placeOrder_\\d{8}-\\d{9}_\\d{8}-\\d{9}\\.puml"));
        PlantUmlAssertions.assertWellFormed(Files.readString(written.get(0)));
    }

    @Test
    @DisplayName("two flows finishing in the same millisecond do not overwrite each other")
    void doesNotOverwriteOnNameClashes(@TempDir Path tempDir) throws IOException {
        FlowConfig config = FlowConfig.builder().outputDirectory(tempDir).build();
        FileFlowSink sink = new FileFlowSink(config);

        sink.onFlowRecorded(flow(config));
        sink.onFlowRecorded(flow(config));

        assertThat(Files.list(tempDir).map(path -> path.getFileName().toString()).sorted().toList())
                .containsExactly(
                        "OrderService_placeOrder_" + DiagramFileNamer.timestamp(1_754_120_832_345L)
                                + "_" + DiagramFileNamer.timestamp(1_754_120_832_401L) + "-1.mmd",
                        "OrderService_placeOrder_" + DiagramFileNamer.timestamp(1_754_120_832_345L)
                                + "_" + DiagramFileNamer.timestamp(1_754_120_832_401L) + ".mmd");
    }

    @Test
    @DisplayName("file-writing can be switched off")
    void writesNothingWhenDisabled(@TempDir Path tempDir) throws IOException {
        FlowConfig config = FlowConfig.builder().outputDirectory(tempDir).writeFiles(false).build();

        new FileFlowSink(config).onFlowRecorded(flow(config));

        assertThat(Files.list(tempDir)).isEmpty();
    }

    @Test
    @DisplayName("an unwritable directory is logged, not thrown at the business-call")
    void swallowsWriteFailures(@TempDir Path tempDir) throws IOException {
        Path blockingFile = Files.createFile(tempDir.resolve("blocked"));
        FlowConfig config = FlowConfig.builder().outputDirectory(blockingFile).build();

        new FileFlowSink(config).onFlowRecorded(flow(config));

        assertThat(blockingFile).isEmptyFile();
    }
}
