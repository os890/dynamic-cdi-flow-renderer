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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.hotspots.ReportService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Writes the annotated diagram into {@code target/flow-diagrams/<container>/showcase} - it is the
 * hotspot sample shown in the README.
 */
class HotspotShowcaseIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        Path showcase = Paths.get(System.getProperty("cdi-flow.test.output-directory",
                "target/flow-diagrams"), "showcase");
        container = CdiFlowTestContainer.configured()
                .with(FlowConfig.KEY_OUTPUT_DIRECTORY, showcase.toString())
                .startFor(HotspotShowcaseIT.class);
    }

    @AfterAll
    static void stopContainer() {
        container.close();
    }

    @Test
    @DisplayName("the showcase diagram points at the innermost slow call")
    void producesTheAnnotatedShowcase() {
        container.get(ReportService.class).buildReport("sales");

        List<Path> diagrams = container.writtenDiagrams();

        assertThat(diagrams).singleElement().satisfies(file ->
                assertThat(file.getFileName().toString()).startsWith("ReportService_buildReport_"));

        String diagram = container.readDiagram(diagrams.get(0));
        assertThat(diagram).contains("Note over ReportRepository: HOTSPOT ReportRepository.loadRows");
        MermaidAssertions.assertWellFormed(diagram);
        MermaidAssertions.assertFreeOfProxyNames(diagram);
    }
}
