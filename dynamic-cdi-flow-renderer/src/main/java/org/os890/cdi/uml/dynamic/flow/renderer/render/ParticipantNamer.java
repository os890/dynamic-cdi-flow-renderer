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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Turns bean-class-names into stable, Mermaid-safe participant-ids.
 * <p>
 * Two beans with the same simple name in different packages would otherwise silently become one
 * participant, so the ambiguous ones are shown with their fully qualified name.
 */
public final class ParticipantNamer {

    public static final String CALLER_ID = "Caller";
    public static final String CALLER_DISPLAY_NAME = "caller";

    /** identifiers which would be parsed as Mermaid syntax instead of as a participant */
    private static final Set<String> RESERVED = Set.of(
            "end", "loop", "alt", "else", "opt", "par", "and", "rect", "note", "over",
            "activate", "deactivate", "participant", "actor", "autonumber", "critical",
            "break", "link", "title");

    private final Map<String, String> idByClassName = new LinkedHashMap<>();
    private final Map<String, String> displayNameById = new LinkedHashMap<>();

    /**
     * @param simpleNameByClassName bean-classes in the order they appear in the flow
     */
    public ParticipantNamer(Map<String, String> simpleNameByClassName) {
        Map<String, Integer> occurrencesPerSimpleName = new HashMap<>();
        simpleNameByClassName.values()
                .forEach(simpleName -> occurrencesPerSimpleName.merge(simpleName, 1, Integer::sum));

        Set<String> usedIds = new HashSet<>();
        usedIds.add(CALLER_ID);

        simpleNameByClassName.forEach((className, simpleName) -> {
            boolean ambiguous = occurrencesPerSimpleName.getOrDefault(simpleName, 0) > 1;
            String displayName = ambiguous ? className : simpleName;

            String id = uniqueId(sanitize(simpleName), usedIds);
            usedIds.add(id);
            idByClassName.put(className, id);
            displayNameById.put(id, displayName);
        });
    }

    public String idFor(String className) {
        String id = idByClassName.get(className);
        return id != null ? id : sanitize(className);
    }

    /** participant-id -> label, in first-appearance order */
    public Map<String, String> participants() {
        return new LinkedHashMap<>(displayNameById);
    }

    private static String uniqueId(String preferredId, Set<String> usedIds) {
        if (!usedIds.contains(preferredId)) {
            return preferredId;
        }
        for (int suffix = 2; ; suffix++) {
            String candidate = preferredId + "_" + suffix;
            if (!usedIds.contains(candidate)) {
                return candidate;
            }
        }
    }

    static String sanitize(String name) {
        StringBuilder sanitized = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            sanitized.append(Character.isLetterOrDigit(c) ? c : '_');
        }
        if (sanitized.isEmpty()) {
            sanitized.append("Bean");
        }
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized.insert(0, '_');
        }
        if (RESERVED.contains(sanitized.toString().toLowerCase(java.util.Locale.ROOT))) {
            sanitized.append('_');
        }
        return sanitized.toString();
    }
}
