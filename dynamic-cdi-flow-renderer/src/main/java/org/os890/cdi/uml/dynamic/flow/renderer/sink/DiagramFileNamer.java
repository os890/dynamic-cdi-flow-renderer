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

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Builds the diagram file-name:
 * <p>
 * {@code <simple class-name of the entry-point>_<first public method-name>_<start>_<end><extension>}
 * <p>
 * for example {@code OrderService_placeOrder_20260802-094712345_20260802-094712401.mmd} - the
 * extension follows the configured output-format ({@code .mmd} or {@code .puml}).
 */
public final class DiagramFileNamer {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS").withZone(ZoneId.systemDefault());

    private DiagramFileNamer() {
    }

    public static String fileNameFor(CallFlow flow) {
        return baseNameFor(flow) + flow.config().outputFormat().fileExtension();
    }

    /**
     * The file-name without its extension - used to build a non-clashing alternative.
     */
    public static String baseNameFor(CallFlow flow) {
        return sanitize(flow.entryTypeSimpleName())
                + "_" + sanitize(flow.entryMethodName())
                + "_" + timestamp(flow.startedAtEpochMillis())
                + "_" + timestamp(flow.finishedAtEpochMillis());
    }

    public static String timestamp(long epochMillis) {
        return TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }

    private static String sanitize(String namePart) {
        StringBuilder sanitized = new StringBuilder(namePart.length());
        for (char c : namePart.toCharArray()) {
            sanitized.append(Character.isLetterOrDigit(c) ? c : '_');
        }
        return sanitized.isEmpty() ? "unknown" : sanitized.toString();
    }
}
