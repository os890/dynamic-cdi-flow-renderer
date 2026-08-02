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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * There is no MicroProfile-Config implementation on the addon's class-path, so these tests also
 * cover the documented fallback: the addon has to stay fully usable without MicroProfile-Config.
 */
class FlowConfigTest {

    private final List<String> touchedProperties = new ArrayList<>();

    @AfterEach
    void restoreSystemProperties() {
        touchedProperties.forEach(System::clearProperty);
        touchedProperties.clear();
    }

    private void set(String key, String value) {
        touchedProperties.add(key);
        System.setProperty(key, value);
    }

    @Test
    @DisplayName("without any configuration every bean is recorded into the tmp-directory")
    void appliesTheDocumentedDefaults() {
        FlowConfig config = FlowConfig.load();

        assertThat(config.isEnabled()).isTrue();
        assertThat(config.includePattern()).as("unset means no name-restriction").isNull();
        assertThat(config.excludePattern()).isNull();
        assertThat(config.outputDirectory())
                .isEqualTo(Paths.get(System.getProperty("java.io.tmpdir")));
        assertThat(config.outputFormat()).as("mermaid is the documented default")
                .isEqualTo(DiagramFormat.MERMAID);
        assertThat(config.isFoldLoops()).isTrue();
        assertThat(config.isCollapseProxyFrames()).isTrue();
        assertThat(config.isWriteFiles()).isTrue();

        assertThat(config.includeStereotypes()).isEmpty();
        assertThat(config.matches("com.acme.OrderService", Set.of())).isTrue();
        assertThat(config.matches("anything.at.all.Bean", Set.of())).isTrue();
    }

    @Test
    @DisplayName("the include-pattern narrows the recorded beans")
    void appliesTheIncludePattern() {
        set(FlowConfig.KEY_INCLUDE_PATTERN, "com\\.acme\\.order\\..*");

        FlowConfig config = FlowConfig.load();

        assertThat(config.matches("com.acme.order.OrderService", Set.of())).isTrue();
        assertThat(config.matches("com.acme.billing.InvoiceService", Set.of())).isFalse();
    }

    @Test
    @DisplayName("the exclude-pattern wins over the include-pattern")
    void appliesTheExcludePattern() {
        set(FlowConfig.KEY_INCLUDE_PATTERN, "com\\.acme\\..*");
        set(FlowConfig.KEY_EXCLUDE_PATTERN, ".*\\.internal\\..*");

        FlowConfig config = FlowConfig.load();

        assertThat(config.matches("com.acme.OrderService", Set.of())).isTrue();
        assertThat(config.matches("com.acme.internal.Cache", Set.of())).isFalse();
    }

    @Test
    @DisplayName("without a configured stereotype the stereotypes play no role at all")
    void appliesNoStereotypeRestrictionByDefault() {
        FlowConfig config = FlowConfig.load();

        assertThat(config.includeStereotypes()).isEmpty();
        assertThat(config.hasStereotypeRestriction()).isFalse();
        assertThat(config.carriesConfiguredStereotype(Set.of("com.acme.Whatever"))).isFalse();
        assertThat(config.matches("any.Bean", Set.of())).as("everything is recorded").isTrue();
    }

    @Test
    @DisplayName("a configured stereotype selects the recorded beans on its own")
    void appliesTheStereotypeFilterOnItsOwn() {
        set(FlowConfig.KEY_INCLUDE_STEREOTYPES, "com.acme.Audited");

        FlowConfig config = FlowConfig.load();

        assertThat(config.hasStereotypeRestriction()).isTrue();
        assertThat(config.matches("com.acme.order.OrderService", Set.of("com.acme.Audited"))).isTrue();
        assertThat(config.matches("com.acme.order.OrderService", Set.of("com.acme.Internal"))).isFalse();
        assertThat(config.matches("com.acme.order.OrderService", Set.of())).isFalse();
    }

    @Test
    @DisplayName("several stereotypes are configured comma-separated, blanks are ignored")
    void acceptsSeveralStereotypes() {
        set(FlowConfig.KEY_INCLUDE_STEREOTYPES, " com.acme.Audited , ,com.acme.Internal , ");

        FlowConfig config = FlowConfig.load();

        assertThat(config.includeStereotypes()).containsExactlyInAnyOrder(
                "com.acme.Audited", "com.acme.Internal");
        assertThat(config.carriesConfiguredStereotype(Set.of("com.acme.Internal"))).isTrue();
        assertThat(config.carriesConfiguredStereotype(Set.of("com.acme.Other"))).isFalse();
    }

    @Test
    @DisplayName("a stereotype may also be given by its simple name")
    void acceptsSimpleStereotypeNames() {
        set(FlowConfig.KEY_INCLUDE_STEREOTYPES, "Audited");

        FlowConfig config = FlowConfig.load();

        assertThat(config.carriesConfiguredStereotype(Set.of("com.acme.Audited"))).isTrue();
        assertThat(config.carriesConfiguredStereotype(Set.of("com.acme.Outer$Audited"))).isTrue();
        assertThat(config.carriesConfiguredStereotype(Set.of("com.acme.NotAudited"))).isFalse();
    }

    @Test
    @DisplayName("pattern and stereotype select alternatively - either one is enough")
    void unitesThePatternWithTheStereotypes() {
        set(FlowConfig.KEY_INCLUDE_PATTERN, "com\\.acme\\.order\\..*");
        set(FlowConfig.KEY_INCLUDE_STEREOTYPES, "com.acme.Audited");

        FlowConfig config = FlowConfig.load();

        assertThat(config.matches("com.acme.order.OrderService", Set.of()))
                .as("selected by the pattern alone").isTrue();
        assertThat(config.matches("com.acme.billing.InvoiceService", Set.of("com.acme.Audited")))
                .as("selected by the stereotype alone").isTrue();
        assertThat(config.matches("com.acme.order.OrderService", Set.of("com.acme.Audited")))
                .as("selected by both").isTrue();
        assertThat(config.matches("com.acme.billing.InvoiceService", Set.of("com.acme.Internal")))
                .as("selected by neither").isFalse();
    }

    @Test
    @DisplayName("an include-pattern of '.*' swallows the stereotype-filter - by definition of a union")
    void letsAnAllMatchingPatternSwallowTheStereotypes() {
        set(FlowConfig.KEY_INCLUDE_PATTERN, ".*");
        set(FlowConfig.KEY_INCLUDE_STEREOTYPES, "com.acme.Audited");

        FlowConfig config = FlowConfig.load();

        assertThat(config.matches("com.acme.billing.InvoiceService", Set.of())).isTrue();
    }

    @Test
    @DisplayName("the exclude-pattern is a veto and also removes a bean a stereotype selected")
    void letsTheExcludePatternWinOverAStereotype() {
        set(FlowConfig.KEY_EXCLUDE_PATTERN, ".*\\.internal\\..*");
        set(FlowConfig.KEY_INCLUDE_STEREOTYPES, "com.acme.Audited");

        FlowConfig config = FlowConfig.load();

        assertThat(config.matches("com.acme.OrderService", Set.of("com.acme.Audited"))).isTrue();
        assertThat(config.matches("com.acme.internal.Cache", Set.of("com.acme.Audited"))).isFalse();
    }

    @Test
    @DisplayName("all flags and the output-directory are read from the configuration")
    void readsEveryProperty() {
        set(FlowConfig.KEY_ENABLED, "false");
        set(FlowConfig.KEY_OUTPUT_DIRECTORY, "/tmp/cdi-flow-test");
        set(FlowConfig.KEY_OUTPUT_FORMAT, "plantuml");
        set(FlowConfig.KEY_FOLD_LOOPS, "false");
        set(FlowConfig.KEY_COLLAPSE_PROXY_FRAMES, "false");
        set(FlowConfig.KEY_WRITE_FILES, "false");

        FlowConfig config = FlowConfig.load();

        assertThat(config.isEnabled()).isFalse();
        assertThat(config.outputDirectory()).isEqualTo(Path.of("/tmp/cdi-flow-test"));
        assertThat(config.outputFormat()).isEqualTo(DiagramFormat.PLANTUML);
        assertThat(config.isFoldLoops()).isFalse();
        assertThat(config.isCollapseProxyFrames()).isFalse();
        assertThat(config.isWriteFiles()).isFalse();
    }

    @Test
    @DisplayName("an unusable regex is ignored instead of breaking the boot")
    void survivesAnInvalidPattern() {
        set(FlowConfig.KEY_INCLUDE_PATTERN, "com.acme.[");

        FlowConfig config = FlowConfig.load();

        assertThat(config.includePattern()).isNull();
        assertThat(config.matches("com.acme.OrderService", Set.of())).isTrue();
    }

    @Test
    @DisplayName("a blank value counts as unset")
    void ignoresBlankValues() {
        set(FlowConfig.KEY_INCLUDE_PATTERN, "   ");
        set(FlowConfig.KEY_INCLUDE_STEREOTYPES, " , ");

        FlowConfig config = FlowConfig.load();

        assertThat(config.includePattern()).isNull();
        assertThat(config.includeStereotypes()).isEmpty();
        assertThat(config.matches("com.acme.OrderService", Set.of())).isTrue();
    }

    @Test
    @DisplayName("an unknown output-format falls back to mermaid")
    void survivesAnUnknownOutputFormat() {
        set(FlowConfig.KEY_OUTPUT_FORMAT, "graphviz");

        assertThat(FlowConfig.load().outputFormat()).isEqualTo(DiagramFormat.MERMAID);
    }

    @Test
    @DisplayName("hotspot-detection is off unless a threshold is configured")
    void keepsHotspotDetectionOffByDefault() {
        FlowConfig config = FlowConfig.load();

        assertThat(config.hotspotThresholdMillis()).isZero();
        assertThat(config.isHotspotDetectionEnabled()).isFalse();
    }

    @Test
    @DisplayName("a configured threshold switches hotspot-detection on")
    void readsTheHotspotThreshold() {
        set(FlowConfig.KEY_HOTSPOT_THRESHOLD_MS, "250");

        FlowConfig config = FlowConfig.load();

        assertThat(config.hotspotThresholdMillis()).isEqualTo(250);
        assertThat(config.isHotspotDetectionEnabled()).isTrue();
    }

    @Test
    @DisplayName("an unusable or negative threshold is ignored instead of breaking the boot")
    void survivesAnInvalidHotspotThreshold() {
        set(FlowConfig.KEY_HOTSPOT_THRESHOLD_MS, "soon");
        assertThat(FlowConfig.load().isHotspotDetectionEnabled()).isFalse();

        set(FlowConfig.KEY_HOTSPOT_THRESHOLD_MS, "-5");
        assertThat(FlowConfig.load().isHotspotDetectionEnabled()).isFalse();

        set(FlowConfig.KEY_HOTSPOT_THRESHOLD_MS, "0");
        assertThat(FlowConfig.load().isHotspotDetectionEnabled()).isFalse();
    }

    @Test
    @DisplayName("configuration keys map onto environment-variable names")
    void mapsKeysToEnvironmentNames() {
        assertThat(ConfigResolver.toEnvironmentName(FlowConfig.KEY_OUTPUT_DIRECTORY))
                .isEqualTo("CDI_FLOW_OUTPUT_DIRECTORY");
        assertThat(ConfigResolver.toEnvironmentName(FlowConfig.KEY_ENABLED))
                .isEqualTo("CDI_FLOW_ENABLED");
        assertThat(ConfigResolver.toEnvironmentName(FlowConfig.KEY_OUTPUT_FORMAT))
                .isEqualTo("CDI_FLOW_OUTPUT_FORMAT");
        assertThat(ConfigResolver.toEnvironmentName(FlowConfig.KEY_HOTSPOT_THRESHOLD_MS))
                .isEqualTo("CDI_FLOW_HOTSPOT_THRESHOLD_MS");
    }
}
