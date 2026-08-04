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

package org.os890.cdi.uml.dynamic.flow.renderer.testsupport;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Structural validator for the PlantUML output - the counterpart of {@link MermaidAssertions},
 * applying the same rules to the other notation.
 */
public final class PlantUmlAssertions {

    private static final Pattern PARTICIPANT =
            Pattern.compile("^participant\\s+(?:\"([^\"]+)\"\\s+as\\s+)?(\\w+)$");
    private static final Pattern MESSAGE =
            Pattern.compile("^(\\w+)\\s+(-->x|-->|->>|->x|->)\\s+(\\w+)\\s+:\\s*(.*)$");
    private static final Pattern ACTIVATION = Pattern.compile("^(activate|deactivate)\\s+(\\w+)$");
    private static final Pattern LOOP_START = Pattern.compile("^loop\\s+(\\d+)\\s+times$");
    /** one request of a combined use-case diagram */
    private static final Pattern GROUP_START = Pattern.compile("^group\\s+.+$");
    private static final Pattern NOTE_START = Pattern.compile("^note over\\s+[\\w,\\s]+$");
    /** the single-line form used for the hotspot-marker */
    private static final Pattern INLINE_NOTE = Pattern.compile("^note over\\s+\\w+\\s+:\\s*(.*)$");

    private static final Set<String> DIRECTIVES = Set.of("autonumber", "hide footbox");
    /** the use-case a labelled flow belongs to */
    private static final Pattern TITLE = Pattern.compile("^title\\s+.+$");

    private PlantUmlAssertions() {
    }

    public static void assertWellFormed(String diagram) {
        String[] lines = diagram.split("\n", -1);
        //a configured file-header sits in front of the diagram, as a comment PlantUML does not draw
        int start = 0;
        while (start < lines.length && lines[start].strip().startsWith("'")) {
            start++;
        }
        assertThat(lines[start]).as("first line of the diagram").isEqualTo("@startuml");
        assertThat(diagram.stripTrailing()).as("last line of the diagram").endsWith("@enduml");

        Set<String> declaredParticipants = new HashSet<>();
        Map<String, Integer> activationDepth = new HashMap<>();
        Deque<String> blocks = new ArrayDeque<>();
        boolean insideNote = false;
        int messageCount = 0;

        for (int i = start + 1; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty() || "@enduml".equals(line) || DIRECTIVES.contains(line)
                    || line.startsWith("'")) {
                continue;
            }

            if (insideNote) {
                insideNote = !"end note".equals(line);
                continue;
            }
            if (INLINE_NOTE.matcher(line).matches()) {
                continue;
            }
            if (NOTE_START.matcher(line).matches()) {
                insideNote = true;
                continue;
            }

            Matcher participant = PARTICIPANT.matcher(line);
            if (participant.matches()) {
                declaredParticipants.add(participant.group(2));
                continue;
            }
            if (LOOP_START.matcher(line).matches()) {
                blocks.push("loop");
                continue;
            }
            if (TITLE.matcher(line).matches()) {
                continue;
            }
            if (GROUP_START.matcher(line).matches()) {
                blocks.push("group");
                continue;
            }
            if ("end".equals(line)) {
                if (blocks.isEmpty()) {
                    fail("unbalanced 'end' in line %d of:%n%s", i + 1, diagram);
                }
                blocks.pop();
                continue;
            }

            Matcher activation = ACTIVATION.matcher(line);
            if (activation.matches()) {
                String id = activation.group(2);
                assertThat(declaredParticipants)
                        .as("'%s' activates the undeclared participant '%s'", line, id).contains(id);
                int depth = activationDepth.merge(id,
                        "activate".equals(activation.group(1)) ? 1 : -1, Integer::sum);
                assertThat(depth).as("activation depth of '%s' after line %d", id, i + 1)
                        .isGreaterThanOrEqualTo(0);
                continue;
            }

            Matcher message = MESSAGE.matcher(line);
            if (message.matches()) {
                messageCount++;
                assertThat(declaredParticipants).as("sender of '%s'", line).contains(message.group(1));
                assertThat(declaredParticipants).as("receiver of '%s'", line).contains(message.group(3));
                assertThat(message.group(4))
                        .as("label of '%s' must not contain a quote or a line-break", line)
                        .doesNotContain("\"");
                continue;
            }

            fail("unrecognized PlantUML line %d: '%s'%n%s", i + 1, line, diagram);
        }

        assertThat(insideNote).as("unterminated note in:%n%s", diagram).isFalse();
        assertThat(blocks).as("unclosed blocks in:%n%s", diagram).isEmpty();
        assertThat(messageCount).as("the diagram contains no messages:%n%s", diagram).isPositive();
        activationDepth.forEach((id, depth) ->
                assertThat(depth).as("'%s' is still activated at the end of:%n%s", id, diagram).isZero());
    }

    public static void assertFreeOfProxyNames(String diagram) {
        MermaidAssertions.assertFreeOfProxyNames(diagram);
    }

    public static int countStatements(String diagram, String statement) {
        return MermaidAssertions.countStatements(diagram, statement);
    }

    public static int countOccurrences(String diagram, String text) {
        return MermaidAssertions.countOccurrences(diagram, text);
    }
}
