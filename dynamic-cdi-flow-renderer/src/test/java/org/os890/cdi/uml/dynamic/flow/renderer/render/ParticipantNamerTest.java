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

package org.os890.cdi.uml.dynamic.flow.renderer.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantNamerTest {

    private static Map<String, String> beans(String... classNameAndSimpleNamePairs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < classNameAndSimpleNamePairs.length; i += 2) {
            result.put(classNameAndSimpleNamePairs[i], classNameAndSimpleNamePairs[i + 1]);
        }
        return result;
    }

    @Test
    @DisplayName("an unambiguous bean keeps its simple name as id and label")
    void usesTheSimpleName() {
        ParticipantNamer namer = new ParticipantNamer(beans("com.acme.OrderService", "OrderService"));

        assertThat(namer.idFor("com.acme.OrderService")).isEqualTo("OrderService");
        assertThat(namer.participants()).containsExactly(Map.entry("OrderService", "OrderService"));
    }

    @Test
    @DisplayName("equally named beans get distinct ids and fully qualified labels")
    void disambiguatesEqualSimpleNames() {
        ParticipantNamer namer = new ParticipantNamer(
                beans("com.acme.a.Service", "Service", "com.acme.b.Service", "Service"));

        assertThat(namer.idFor("com.acme.a.Service")).isEqualTo("Service");
        assertThat(namer.idFor("com.acme.b.Service")).isEqualTo("Service_2");
        assertThat(namer.participants()).containsExactly(
                Map.entry("Service", "com.acme.a.Service"),
                Map.entry("Service_2", "com.acme.b.Service"));
    }

    @Test
    @DisplayName("the reserved caller-lane is never taken over by a bean")
    void neverCollidesWithTheCallerLane() {
        ParticipantNamer namer = new ParticipantNamer(beans("com.acme.Caller", "Caller"));

        assertThat(namer.idFor("com.acme.Caller")).isEqualTo("Caller_2");
    }

    @Test
    @DisplayName("nested bean-classes produce a valid id")
    void sanitizesNestedClassNames() {
        ParticipantNamer namer = new ParticipantNamer(
                beans("com.acme.Outer$Inner", "Outer.Inner"));

        assertThat(namer.idFor("com.acme.Outer$Inner")).isEqualTo("Outer_Inner");
    }

    @Test
    @DisplayName("Mermaid keywords are escaped so they cannot be parsed as syntax")
    void escapesMermaidKeywords() {
        assertThat(ParticipantNamer.sanitize("end")).isEqualTo("end_");
        assertThat(ParticipantNamer.sanitize("Loop")).isEqualTo("Loop_");
        assertThat(ParticipantNamer.sanitize("Note")).isEqualTo("Note_");
        assertThat(ParticipantNamer.sanitize("OrderService")).isEqualTo("OrderService");
    }

    @Test
    @DisplayName("an id never starts with a digit and is never empty")
    void producesValidIdentifiers() {
        assertThat(ParticipantNamer.sanitize("1Service")).isEqualTo("_1Service");
        assertThat(ParticipantNamer.sanitize("...")).isEqualTo("___");
        assertThat(ParticipantNamer.sanitize("")).isEqualTo("Bean");
    }
}
