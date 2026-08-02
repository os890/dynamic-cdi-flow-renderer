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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each test needs its own container, because the output-directory is read while the container boots.
 */
class OutputDirectoryConfigIT {

    @Test
    @DisplayName("diagrams are written into the configured directory, which is created on demand")
    void writesIntoTheConfiguredDirectory() {
        Path configured = Paths.get(System.getProperty("cdi-flow.test.output-directory",
                "target/flow-diagrams"), "OutputDirectoryConfigIT", "custom", "nested");

        try (CdiFlowTestContainer container = CdiFlowTestContainer.configured()
                .with(FlowConfig.KEY_OUTPUT_DIRECTORY, configured.toString())
                .startFor(OutputDirectoryConfigIT.class)) {

            container.get(OrderService.class).placeOrder("SKU-1", 2);

            assertThat(container.outputDirectory()).isEqualTo(configured);
            assertThat(container.writtenDiagrams()).hasSize(1);
            assertThat(container.writtenDiagrams().get(0)).hasParent(configured);
        }
    }

    @Test
    @DisplayName("without configuration the tmp-directory is used")
    void fallsBackToTheTmpDirectory() {
        //file-writing is switched off so the real tmp-directory stays untouched
        try (CdiFlowTestContainer container = CdiFlowTestContainer.configured()
                .usingDefaultOutputDirectory()
                .writingFiles(false)
                .startFor(OutputDirectoryConfigIT.class)) {

            container.get(OrderService.class).placeOrder("SKU-1", 2);

            CallFlow flow = container.flows().singleEnteredAt(OrderService.class);
            assertThat(flow.config().outputDirectory())
                    .isEqualTo(Paths.get(System.getProperty("java.io.tmpdir")));
        }
    }

    @Test
    @DisplayName("recording without writing files keeps the in-memory sinks working")
    void canRecordWithoutWritingFiles() {
        try (CdiFlowTestContainer container = CdiFlowTestContainer.configured()
                .writingFiles(false)
                .startFor(OutputDirectoryConfigIT.class)) {

            container.get(OrderService.class).placeOrder("SKU-1", 2);

            assertThat(container.flows().all()).hasSize(1);
            assertThat(container.writtenDiagrams()).isEmpty();
        }
    }
}
