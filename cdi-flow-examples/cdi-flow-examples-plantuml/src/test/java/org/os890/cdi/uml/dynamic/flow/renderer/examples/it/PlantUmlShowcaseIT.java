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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.ExampleScenarios;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.PlantUmlAssertions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same scenarios as {@link DiagramShowcaseIT}, written as PlantUML into
 * {@code target/flow-diagrams/<container>/showcase} of this example.
 */
class PlantUmlShowcaseIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        Path showcase = Paths.get(System.getProperty("cdi-flow.test.output-directory",
                "target/flow-diagrams"), "showcase");
        container = CdiFlowTestContainer.configured()
                .with(FlowConfig.KEY_OUTPUT_DIRECTORY, showcase.toString())
                .startFor(PlantUmlShowcaseIT.class);
    }

    @AfterAll
    static void stopContainer() {
        container.close();
    }

    @Test
    @DisplayName("one PlantUML diagram per scenario is produced and every one of them is well-formed")
    void producesAShowcaseOfEveryScenario() {
        ExampleScenarios.runAll(container);

        List<Path> diagrams = container.writtenDiagrams();

        assertThat(diagrams).hasSize(ExampleScenarios.ENTRY_POINTS.size());
        assertThat(diagrams).extracting(path -> path.getFileName().toString())
                .allMatch(name -> name.endsWith(".puml"));
        ExampleScenarios.ENTRY_POINTS.forEach(entryPoint ->
                assertThat(diagrams).extracting(path -> path.getFileName().toString())
                        .anySatisfy(name -> assertThat(name).startsWith(entryPoint)));

        diagrams.forEach(file -> {
            String diagram = container.readDiagram(file);
            PlantUmlAssertions.assertWellFormed(diagram);
            PlantUmlAssertions.assertFreeOfProxyNames(diagram);
        });
    }
}
