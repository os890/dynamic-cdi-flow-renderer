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

package org.os890.cdi.uml.dynamic.flow.renderer.report;

/**
 * What two recordings of the same call-chain have in common once the clock is taken out of them.
 *
 * <p>A use-case reads a list four times and checks the session before every single request, so most
 * of what is recorded repeats: same participants, same calls, different microseconds. Comparing the
 * diagrams with the timestamps, the durations and the hotspot-notes removed identifies those, which
 * is what lets a use-case keep one file per distinct chain and count the rest.
 */
final class ChainShape {

    private ChainShape() {
    }

    static String of(String diagram) {
        StringBuilder shape = new StringBuilder(diagram.length());
        boolean insidePlantUmlNote = false;
        for (String line : diagram.split("\\R")) {
            String trimmed = line.strip();
            if (insidePlantUmlNote) {
                insidePlantUmlNote = !trimmed.equals("end note");
                continue;
            }
            if (trimmed.startsWith("Note over Caller,") || trimmed.startsWith("note over Caller,")) {
                //the note carries nothing but the wall-clock of this one recording
                insidePlantUmlNote = !trimmed.contains("<br/>");
                continue;
            }
            shape.append(withoutDurations(trimmed)).append('\n');
        }
        return shape.toString();
    }

    /**
     * Removes {@code [12.34 ms]} from a return-arrow and the measured value from a hotspot-note,
     * both of which differ between two recordings of the very same chain.
     */
    private static String withoutDurations(String line) {
        return line
                .replaceAll("\\[\\d+([.,]\\d+)? ms]", "[]")
                .replaceAll("took \\d+([.,]\\d+)? ms", "took")
                .replaceAll("over \\d+([.,]\\d+)? ms", "over");
    }
}
