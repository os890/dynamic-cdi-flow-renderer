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
 * Structural validator for the generated diagrams.
 * <p>
 * Shared with the examples-module via the test-jar so that every diagram produced by the
 * integration-tests - on Weld and on OpenWebBeans - is checked with the same rules.
 */
public final class MermaidAssertions {

    private static final Pattern PARTICIPANT =
            Pattern.compile("^participant\\s+(\\w+)(?:\\s+as\\s+(.+))?$");
    private static final Pattern MESSAGE =
            Pattern.compile("^(\\w+)\\s*(-->>|->>|--x|-x|-\\)|->)\\s*(\\w+)\\s*:\\s*(.*)$");
    private static final Pattern ACTIVATION = Pattern.compile("^(activate|deactivate)\\s+(\\w+)$");
    private static final Pattern LOOP_START = Pattern.compile("^loop\\s+(\\d+)\\s+times$");
    /** one request of a combined use-case diagram */
    private static final Pattern RECT_START = Pattern.compile("^rect\\s+rgb\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\)$");
    private static final Pattern NOTE = Pattern.compile("^Note\\s+over\\s+([\\w,\\s]+):\\s*(.*)$");

    /** anything a CDI implementation adds to a generated class-name */
    private static final String[] PROXY_MARKERS = {
            "$$", "$Proxy", "_Subclass", "_ClientProxy", "Weld", "Owb", "OpenWebBeans", "Proxy$"
    };

    private MermaidAssertions() {
    }

    public static void assertWellFormed(String diagram) {
        String[] lines = diagram.split("\n", -1);
        //a configured file-header sits in front of the diagram, as a comment Mermaid does not draw
        int start = 0;
        while (start < lines.length && lines[start].strip().startsWith("%%")) {
            start++;
        }
        assertThat(lines[start]).as("first line of the diagram").isEqualTo("sequenceDiagram");

        Set<String> declaredParticipants = new HashSet<>();
        Map<String, Integer> activationDepth = new HashMap<>();
        Deque<String> blocks = new ArrayDeque<>();
        int messageCount = 0;

        for (int i = start + 1; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty() || "autonumber".equals(line) || line.startsWith("%%")) {
                continue;
            }

            Matcher participant = PARTICIPANT.matcher(line);
            if (participant.matches()) {
                declaredParticipants.add(participant.group(1));
                continue;
            }
            if (NOTE.matcher(line).matches()) {
                continue;
            }
            if (LOOP_START.matcher(line).matches()) {
                blocks.push("loop");
                continue;
            }
            if (RECT_START.matcher(line).matches()) {
                blocks.push("rect");
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
                        .as("'%s' activates the undeclared participant '%s'", line, id)
                        .contains(id);
                int delta = "activate".equals(activation.group(1)) ? 1 : -1;
                int depth = activationDepth.merge(id, delta, Integer::sum);
                assertThat(depth).as("activation depth of '%s' after line %d", id, i + 1)
                        .isGreaterThanOrEqualTo(0);
                continue;
            }

            Matcher message = MESSAGE.matcher(line);
            if (message.matches()) {
                messageCount++;
                assertThat(declaredParticipants)
                        .as("sender of '%s'", line).contains(message.group(1));
                assertThat(declaredParticipants)
                        .as("receiver of '%s'", line).contains(message.group(3));
                assertThat(message.group(4))
                        .as("label of '%s' must not contain Mermaid control characters", line)
                        .doesNotContain(";").doesNotContain("#");
                continue;
            }

            fail("unrecognized Mermaid line %d: '%s'%n%s", i + 1, line, diagram);
        }

        assertThat(blocks).as("unclosed blocks in:%n%s", diagram).isEmpty();
        assertThat(messageCount).as("the diagram contains no messages:%n%s", diagram).isPositive();
        activationDepth.forEach((id, depth) ->
                assertThat(depth).as("'%s' is still activated at the end of:%n%s", id, diagram).isZero());
    }

    /**
     * Verifies that no CDI-implementation artifact leaked into the diagram.
     */
    public static void assertFreeOfProxyNames(String diagram) {
        for (String marker : PROXY_MARKERS) {
            assertThat(diagram).as("diagram must not expose the proxy-marker '%s'", marker)
                    .doesNotContain(marker);
        }
    }

    /**
     * Counts whole statements rather than substrings - {@code activate X} would otherwise also
     * match inside {@code deactivate X}.
     */
    public static int countStatements(String diagram, String statement) {
        return (int) diagram.lines().map(String::strip).filter(statement::equals).count();
    }

    /**
     * @return how often the given text occurs in the diagram
     */
    public static int countOccurrences(String diagram, String text) {
        int count = 0;
        for (int index = diagram.indexOf(text); index >= 0; index = diagram.indexOf(text, index + 1)) {
            count++;
        }
        return count;
    }
}
