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
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import static org.assertj.core.api.Assertions.assertThat;

class DiagramWriterTest {

    private static FlowConfig withFileHeader(DiagramFormat format) {
        return FlowConfig.builder()
                .outputFormat(format)
                .fileHeader("Licensed under the Apache License, Version 2.0")
                .build();
    }

    @Test
    @DisplayName("without a configured header the diagram is written as it was rendered")
    void noHeader() {
        FlowConfig config = FlowConfig.builder().build();

        assertThat(DiagramWriter.withHeader(config, "sequenceDiagram\n")).isEqualTo("sequenceDiagram\n");
    }

    @Test
    @DisplayName("the header goes on top, as a comment of the notation in use")
    void headerOnTop() {
        assertThat(DiagramWriter.withHeader(withFileHeader(DiagramFormat.MERMAID), "sequenceDiagram\n"))
                .isEqualTo("%% Licensed under the Apache License, Version 2.0\nsequenceDiagram\n");
        assertThat(DiagramWriter.withHeader(withFileHeader(DiagramFormat.PLANTUML), "@startuml\n"))
                .isEqualTo("' Licensed under the Apache License, Version 2.0\n@startuml\n");
    }

    @Test
    @DisplayName("a front-matter block keeps the first line, because Mermaid only reads it there")
    void headerFollowsFrontMatter() {
        String diagram = "---\ntitle: \"a contact is deleted\"\n---\nsequenceDiagram\n";

        String written = DiagramWriter.withHeader(withFileHeader(DiagramFormat.MERMAID), diagram);

        assertThat(written).isEqualTo("---\ntitle: \"a contact is deleted\"\n---\n"
                + "%% Licensed under the Apache License, Version 2.0\nsequenceDiagram\n");
    }

    @Test
    @DisplayName("a multi-line header becomes one comment-line per line")
    void multiLineHeader() {
        FlowConfig config = FlowConfig.builder().fileHeader("first\nsecond").build();

        assertThat(DiagramWriter.withHeader(config, "sequenceDiagram\n"))
                .isEqualTo("%% first\n%% second\nsequenceDiagram\n");
    }
}
