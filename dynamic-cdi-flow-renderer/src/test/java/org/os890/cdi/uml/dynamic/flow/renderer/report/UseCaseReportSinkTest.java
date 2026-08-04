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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowLabel;
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.PlantUmlAssertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class UseCaseReportSinkTest {

    private static final FlowLabel DELETING = FlowLabel.of("a contact is deleted",
            "Deleting a contact asks first, and the row is gone once that is confirmed.");

    private static CallFlow listFlow(FlowConfig config, FlowLabel label) {
        return labelled(call("ContactResource", "list")
                .calling(call("ContactService", "list").returning("List")), config, label);
    }

    private static CallFlow deleteFlow(FlowConfig config, FlowLabel label) {
        return labelled(call("ContactResource", "delete").params("Long")
                .calling(call("ContactService", "delete").params("Long")), config, label);
    }

    private static CallFlow labelled(
            org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder builder,
            FlowConfig config, FlowLabel label) {
        return new CallFlow(builder.build(), "executor-thread-1", config, label);
    }

    private static FlowConfig configIn(Path outputDirectory) {
        return FlowConfig.builder().outputDirectory(outputDirectory).build();
    }

    @Test
    @DisplayName("a labelled use-case gets a directory, a combined diagram and an index")
    void writesTheUseCase(@TempDir Path tempDir) throws IOException {
        FlowConfig config = configIn(tempDir);
        UseCaseReportSink sink = new UseCaseReportSink(config);

        sink.onFlowRecorded(listFlow(config, DELETING));
        sink.onFlowRecorded(deleteFlow(config, DELETING));

        Path useCase = tempDir.resolve("a-contact-is-deleted");
        assertThat(useCase).isDirectory();
        assertThat(Files.list(useCase).map(path -> path.getFileName().toString()))
                .contains("use-case.mmd", "README.md");

        String combined = Files.readString(useCase.resolve("use-case.mmd"));
        MermaidAssertions.assertWellFormed(combined);
        //the use-case is on the diagram itself, not only in the directory-name
        assertThat(combined).startsWith("---\ntitle: \"a contact is deleted\"\n---\nsequenceDiagram");
        assertThat(combined).contains("ContactResource.list", "ContactResource.delete");
        //one block per request, and every lane declared once
        assertThat(combined.lines().filter(line -> line.strip().startsWith("rect rgb")).count()).isEqualTo(2);
        assertThat(combined.lines().filter(line -> line.contains("participant ContactService")).count())
                .isEqualTo(1);

        assertThat(Files.readString(useCase.resolve("README.md")))
                .contains("a contact is deleted", "Deleting a contact asks first",
                        "`ContactResource.list`", "`ContactResource.delete`");
    }

    @Test
    @DisplayName("the same chain twice is one file, counted twice, and two blocks in the diagram")
    void collapsesIdenticalChains(@TempDir Path tempDir) throws IOException {
        FlowConfig config = configIn(tempDir);
        UseCaseReportSink sink = new UseCaseReportSink(config);

        sink.onFlowRecorded(listFlow(config, DELETING));
        sink.onFlowRecorded(listFlow(config, DELETING));

        Path useCase = tempDir.resolve("a-contact-is-deleted");
        assertThat(Files.list(useCase).filter(path -> path.getFileName().toString().endsWith(".mmd")))
                .hasSize(2); //the one chain, plus the combined diagram
        assertThat(Files.readString(useCase.resolve("README.md"))).contains("2×");
        assertThat(Files.readString(useCase.resolve("use-case.mmd")).lines()
                .filter(line -> line.strip().startsWith("rect rgb")).count()).isEqualTo(2);
    }

    @Test
    @DisplayName("the document lists every use-case, with its diagram inline")
    void writesTheDocument(@TempDir Path tempDir) throws IOException {
        FlowConfig config = configIn(tempDir);
        UseCaseReportSink sink = new UseCaseReportSink(config);

        sink.onFlowRecorded(listFlow(config, DELETING));
        sink.onFlowRecorded(deleteFlow(config, FlowLabel.of("a contact is created", null)));

        String document = Files.readString(tempDir.resolve("use-cases.md"));
        assertThat(document)
                .contains("## 1. a contact is deleted", "Deleting a contact asks first")
                .contains("## 2. a contact is created")
                .contains("```mermaid");
    }

    @Test
    @DisplayName("a use-case of more requests than configured is linked, not inlined")
    void linksLargeUseCases(@TempDir Path tempDir) throws IOException {
        FlowConfig config = FlowConfig.builder()
                .outputDirectory(tempDir)
                .maxCombinedRequests(1)
                .build();
        UseCaseReportSink sink = new UseCaseReportSink(config);

        sink.onFlowRecorded(listFlow(config, DELETING));
        sink.onFlowRecorded(deleteFlow(config, DELETING));

        assertThat(Files.readString(tempDir.resolve("use-cases.md")))
                .contains("too many to inline")
                .doesNotContain("```mermaid");
    }

    @Test
    @DisplayName("an entry-point the application calls noise stays out of the combined diagram only")
    void combinedExcludePattern(@TempDir Path tempDir) throws IOException {
        FlowConfig config = FlowConfig.builder()
                .outputDirectory(tempDir)
                .combinedExcludePattern("ContactResource\\.list")
                .build();
        UseCaseReportSink sink = new UseCaseReportSink(config);

        sink.onFlowRecorded(listFlow(config, DELETING));
        sink.onFlowRecorded(deleteFlow(config, DELETING));

        Path useCase = tempDir.resolve("a-contact-is-deleted");
        String combined = Files.readString(useCase.resolve("use-case.mmd"));
        assertThat(combined).doesNotContain("ContactResource.list").contains("ContactResource.delete");
        //recorded and kept regardless
        assertThat(Files.readString(useCase.resolve("README.md"))).contains("`ContactResource.list`");
    }

    @Test
    @DisplayName("the other notation gets the same report, in its own syntax")
    void plantUml(@TempDir Path tempDir) throws IOException {
        FlowConfig config = FlowConfig.builder()
                .outputDirectory(tempDir)
                .outputFormat(DiagramFormat.PLANTUML)
                .build();
        UseCaseReportSink sink = new UseCaseReportSink(config);

        sink.onFlowRecorded(listFlow(config, DELETING));
        sink.onFlowRecorded(deleteFlow(config, DELETING));

        Path useCase = tempDir.resolve("a-contact-is-deleted");
        String combined = Files.readString(useCase.resolve("use-case.puml"));
        PlantUmlAssertions.assertWellFormed(combined);
        assertThat(combined).startsWith("@startuml\ntitle a contact is deleted\n");
        //one group per request, the caller declared once, and no Mermaid left anywhere
        assertThat(combined.lines().filter(line -> line.strip().startsWith("group ")).count()).isEqualTo(2);
        assertThat(combined.lines().filter(line -> line.contains("as Caller")).count()).isEqualTo(1);
        assertThat(combined).doesNotContain("sequenceDiagram", "rect rgb", "->>");

        assertThat(Files.readString(useCase.resolve("README.md"))).contains("`use-case.puml`");
        assertThat(Files.readString(tempDir.resolve("use-cases.md")))
                .contains("```plantuml")
                .doesNotContain("```mermaid");
    }

    @Test
    @DisplayName("the single chains of a use-case are titled with it as well")
    void singleChainsAreTitled(@TempDir Path tempDir) throws IOException {
        FlowConfig config = configIn(tempDir);

        new UseCaseReportSink(config).onFlowRecorded(listFlow(config, DELETING));

        Path chain = Files.list(tempDir.resolve("a-contact-is-deleted"))
                .filter(path -> path.getFileName().toString().startsWith("ContactResource_list_"))
                .findFirst()
                .orElseThrow();
        String diagram = Files.readString(chain);
        MermaidAssertions.assertWellFormed(diagram);
        assertThat(diagram).contains("title: \"a contact is deleted\"");
    }

    @Test
    @DisplayName("title-diagrams=false leaves the use-case off the diagrams, and nothing else")
    void titlesCanBeSwitchedOff(@TempDir Path tempDir) throws IOException {
        FlowConfig config = FlowConfig.builder()
                .outputDirectory(tempDir)
                .titleDiagrams(false)
                .build();
        UseCaseReportSink sink = new UseCaseReportSink(config);

        sink.onFlowRecorded(listFlow(config, DELETING));
        sink.onFlowRecorded(deleteFlow(config, DELETING));

        Path useCase = tempDir.resolve("a-contact-is-deleted");
        String combined = Files.readString(useCase.resolve("use-case.mmd"));
        MermaidAssertions.assertWellFormed(combined);
        assertThat(combined).startsWith("sequenceDiagram").doesNotContain("title:");

        Path chain = Files.list(useCase)
                .filter(path -> path.getFileName().toString().startsWith("ContactResource_list_"))
                .findFirst()
                .orElseThrow();
        assertThat(Files.readString(chain)).startsWith("sequenceDiagram").doesNotContain("title:");

        //the use-case is still a use-case: its own directory, its index and its entry in the document
        assertThat(Files.readString(useCase.resolve("README.md"))).contains("a contact is deleted");
        assertThat(Files.readString(tempDir.resolve("use-cases.md"))).contains("## 1. a contact is deleted");
    }

    @Test
    @DisplayName("an unlabelled flow is written flat, exactly as before, and reports nothing")
    void unlabelledFlowsAreWrittenFlat(@TempDir Path tempDir) throws IOException {
        FlowConfig config = configIn(tempDir);

        new UseCaseReportSink(config).onFlowRecorded(listFlow(config, null));

        assertThat(Files.list(tempDir).map(path -> path.getFileName().toString()))
                .singleElement()
                .satisfies(name -> assertThat(name).startsWith("ContactResource_list_").endsWith(".mmd"));
        //and carries no title at all, so an application which records no use-cases sees what it did before
        assertThat(Files.readString(Files.list(tempDir).findFirst().orElseThrow()))
                .startsWith("sequenceDiagram");
    }

    @Test
    @DisplayName("group-by-label=false puts a labelled flow back into the flat directory")
    void groupingCanBeSwitchedOff(@TempDir Path tempDir) throws IOException {
        FlowConfig config = FlowConfig.builder().outputDirectory(tempDir).groupByLabel(false).build();

        new UseCaseReportSink(config).onFlowRecorded(listFlow(config, DELETING));

        assertThat(Files.list(tempDir).filter(Files::isDirectory)).isEmpty();
        assertThat(Files.list(tempDir)).hasSize(1);
    }
}
