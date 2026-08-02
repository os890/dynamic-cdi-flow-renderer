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
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class DiagramFileNamerTest {

    @Test
    @DisplayName("the file-name is entry-class, entry-method, start-timestamp and end-timestamp")
    void buildsTheDocumentedFileName() {
        CallFlow flow = call("OrderService", "placeOrder").params("String", "int")
                .epochMillis(1_754_120_832_345L, 1_754_120_832_401L)
                .buildFlow();

        String fileName = DiagramFileNamer.fileNameFor(flow);

        assertThat(fileName)
                .matches("OrderService_placeOrder_\\d{8}-\\d{9}_\\d{8}-\\d{9}\\.mmd")
                .isEqualTo("OrderService_placeOrder_"
                        + DiagramFileNamer.timestamp(1_754_120_832_345L) + "_"
                        + DiagramFileNamer.timestamp(1_754_120_832_401L) + ".mmd");
    }

    @Test
    @DisplayName("the method-parameters are not part of the file-name")
    void omitsMethodParameters() {
        String withParameters = DiagramFileNamer.fileNameFor(
                call("OrderService", "placeOrder").params("String", "int")
                        .epochMillis(1_000L, 2_000L).buildFlow());
        String withoutParameters = DiagramFileNamer.fileNameFor(
                call("OrderService", "placeOrder")
                        .epochMillis(1_000L, 2_000L).buildFlow());

        assertThat(withParameters).isEqualTo(withoutParameters);
    }

    @Test
    @DisplayName("the end-timestamp is never before the start-timestamp")
    void keepsTheTimestampOrder() {
        String fileName = DiagramFileNamer.fileNameFor(
                call("OrderService", "placeOrder")
                        .epochMillis(1_754_120_832_345L, 1_754_120_899_999L).buildFlow());

        String[] parts = fileName.replace(".mmd", "").split("_");
        assertThat(parts[3]).isGreaterThan(parts[2]);
    }

    @Test
    @DisplayName("the extension follows the configured output-format")
    void usesTheExtensionOfTheOutputFormat() {
        CallFlow mermaid = call("OrderService", "placeOrder").epochMillis(1_000L, 2_000L)
                .buildFlow(FlowConfig.builder().outputFormat(DiagramFormat.MERMAID).build());
        CallFlow plantUml = call("OrderService", "placeOrder").epochMillis(1_000L, 2_000L)
                .buildFlow(FlowConfig.builder().outputFormat(DiagramFormat.PLANTUML).build());

        assertThat(DiagramFileNamer.fileNameFor(mermaid)).endsWith(".mmd");
        assertThat(DiagramFileNamer.fileNameFor(plantUml)).endsWith(".puml");
        assertThat(DiagramFileNamer.baseNameFor(mermaid))
                .as("only the extension differs")
                .isEqualTo(DiagramFileNamer.baseNameFor(plantUml))
                .doesNotContain(".");
    }

    @Test
    @DisplayName("a nested entry-bean still produces a file-system safe name")
    void sanitizesTheEntryTypeName() {
        CallFlow flow = call("Outer.Inner", "run").epochMillis(1_000L, 2_000L).buildFlow();

        assertThat(DiagramFileNamer.fileNameFor(flow))
                .startsWith("Outer_Inner_run_")
                .matches("[A-Za-z0-9_\\-]+\\.mmd");
    }
}
