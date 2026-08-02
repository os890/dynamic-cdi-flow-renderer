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

import java.util.Locale;
import java.util.logging.Logger;

/**
 * The notation a recorded flow is written in.
 */
public enum DiagramFormat {

    MERMAID(".mmd"),
    PLANTUML(".puml");

    private static final Logger LOGGER = Logger.getLogger(DiagramFormat.class.getName());

    private final String fileExtension;

    DiagramFormat(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String fileExtension() {
        return fileExtension;
    }

    /**
     * Accepts the enum-name as well as the usual short forms, case-insensitively. An unknown value
     * falls back to the given default instead of breaking the boot.
     */
    public static DiagramFormat parse(String value, DiagramFormat fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "mermaid", "mmd", "mermaidjs":
                return MERMAID;
            case "plantuml", "puml", "uml":
                return PLANTUML;
            default:
                LOGGER.warning(() -> "'" + value + "' is not a known output-format - using "
                        + fallback.name().toLowerCase(Locale.ROOT) + ". Supported: mermaid, plantuml");
                return fallback;
        }
    }
}
