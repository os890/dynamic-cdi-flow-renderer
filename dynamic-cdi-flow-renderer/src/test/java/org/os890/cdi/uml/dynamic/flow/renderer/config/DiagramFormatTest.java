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

package org.os890.cdi.uml.dynamic.flow.renderer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DiagramFormatTest {

    @ParameterizedTest
    @ValueSource(strings = {"mermaid", "MERMAID", " Mermaid ", "mmd", "mermaidjs"})
    @DisplayName("the mermaid format is accepted under its usual spellings")
    void parsesMermaid(String value) {
        assertThat(DiagramFormat.parse(value, DiagramFormat.PLANTUML)).isEqualTo(DiagramFormat.MERMAID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"plantuml", "PlantUML", " PLANTUML ", "puml", "uml"})
    @DisplayName("the plantuml format is accepted under its usual spellings")
    void parsesPlantUml(String value) {
        assertThat(DiagramFormat.parse(value, DiagramFormat.MERMAID)).isEqualTo(DiagramFormat.PLANTUML);
    }

    @ParameterizedTest
    @ValueSource(strings = {"graphviz", "", "   ", "plant-uml"})
    @DisplayName("an unknown or blank value falls back instead of breaking the boot")
    void fallsBackForUnknownValues(String value) {
        assertThat(DiagramFormat.parse(value, DiagramFormat.MERMAID)).isEqualTo(DiagramFormat.MERMAID);
    }

    @Test
    @DisplayName("null falls back too")
    void fallsBackForNull() {
        assertThat(DiagramFormat.parse(null, DiagramFormat.MERMAID)).isEqualTo(DiagramFormat.MERMAID);
    }

    @Test
    @DisplayName("each format brings its own file-extension")
    void providesTheFileExtension() {
        assertThat(DiagramFormat.MERMAID.fileExtension()).isEqualTo(".mmd");
        assertThat(DiagramFormat.PLANTUML.fileExtension()).isEqualTo(".puml");
    }
}
