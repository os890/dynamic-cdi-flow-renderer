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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Immutable snapshot of the {@code cdi-flow.*} configuration, read once while the container boots.
 */
public final class FlowConfig {

    public static final String PREFIX = "cdi-flow.";

    public static final String KEY_ENABLED = PREFIX + "enabled";
    public static final String KEY_INCLUDE_PATTERN = PREFIX + "include-pattern";
    public static final String KEY_EXCLUDE_PATTERN = PREFIX + "exclude-pattern";
    public static final String KEY_INCLUDE_STEREOTYPES = PREFIX + "include-stereotypes";
    public static final String KEY_OUTPUT_DIRECTORY = PREFIX + "output-directory";
    public static final String KEY_OUTPUT_FORMAT = PREFIX + "output-format";
    public static final String KEY_FOLD_LOOPS = PREFIX + "fold-loops";
    public static final String KEY_COLLAPSE_PROXY_FRAMES = PREFIX + "collapse-proxy-frames";
    public static final String KEY_WRITE_FILES = PREFIX + "write-files";
    public static final String KEY_HOTSPOT_THRESHOLD_MS = PREFIX + "hotspot-threshold-ms";
    public static final String KEY_GROUP_BY_LABEL = PREFIX + "group-by-label";
    public static final String KEY_REPORT = PREFIX + "report";
    public static final String KEY_MAX_COMBINED_REQUESTS = PREFIX + "max-combined-requests";
    public static final String KEY_COMBINED_EXCLUDE_PATTERN = PREFIX + "combined-exclude-pattern";
    public static final String KEY_LABEL_HEADER = PREFIX + "label-header";
    public static final String KEY_DESCRIPTION_HEADER = PREFIX + "description-header";
    public static final String KEY_FILE_HEADER = PREFIX + "file-header";

    public static final DiagramFormat DEFAULT_OUTPUT_FORMAT = DiagramFormat.MERMAID;
    public static final String DEFAULT_LABEL_HEADER = "X-Flow-Label";
    public static final String DEFAULT_DESCRIPTION_HEADER = "X-Flow-Description";
    public static final int DEFAULT_MAX_COMBINED_REQUESTS = 25;

    private static final Logger LOGGER = Logger.getLogger(FlowConfig.class.getName());

    private final boolean enabled;
    private final Pattern includePattern;
    private final Pattern excludePattern;
    private final Set<String> includeStereotypes;
    private final Path outputDirectory;
    private final DiagramFormat outputFormat;
    private final boolean foldLoops;
    private final boolean collapseProxyFrames;
    private final boolean writeFiles;
    private final long hotspotThresholdMillis;
    private final boolean groupByLabel;
    private final boolean report;
    private final int maxCombinedRequests;
    private final Pattern combinedExcludePattern;
    private final String labelHeader;
    private final String descriptionHeader;
    private final String fileHeader;

    private FlowConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.includePattern = builder.includePattern;
        this.excludePattern = builder.excludePattern;
        this.includeStereotypes = Set.copyOf(builder.includeStereotypes);
        this.outputDirectory = builder.outputDirectory;
        this.outputFormat = builder.outputFormat;
        this.foldLoops = builder.foldLoops;
        this.collapseProxyFrames = builder.collapseProxyFrames;
        this.writeFiles = builder.writeFiles;
        this.hotspotThresholdMillis = builder.hotspotThresholdMillis;
        this.groupByLabel = builder.groupByLabel;
        this.report = builder.report;
        this.maxCombinedRequests = builder.maxCombinedRequests;
        this.combinedExcludePattern = builder.combinedExcludePattern;
        this.labelHeader = builder.labelHeader;
        this.descriptionHeader = builder.descriptionHeader;
        this.fileHeader = builder.fileHeader;
    }

    public static FlowConfig load() {
        Builder builder = builder();
        builder.enabled = booleanValue(KEY_ENABLED, true);
        builder.includePattern = patternValue(KEY_INCLUDE_PATTERN);
        builder.excludePattern = patternValue(KEY_EXCLUDE_PATTERN);
        builder.includeStereotypes = listValue(KEY_INCLUDE_STEREOTYPES);
        builder.outputDirectory = ConfigResolver.lookup(KEY_OUTPUT_DIRECTORY)
                .map(String::trim)
                .map(Paths::get)
                .orElseGet(FlowConfig::defaultOutputDirectory);
        builder.outputFormat = ConfigResolver.lookup(KEY_OUTPUT_FORMAT)
                .map(value -> DiagramFormat.parse(value, DEFAULT_OUTPUT_FORMAT))
                .orElse(DEFAULT_OUTPUT_FORMAT);
        builder.foldLoops = booleanValue(KEY_FOLD_LOOPS, true);
        builder.collapseProxyFrames = booleanValue(KEY_COLLAPSE_PROXY_FRAMES, true);
        builder.writeFiles = booleanValue(KEY_WRITE_FILES, true);
        builder.hotspotThresholdMillis = millisecondValue(KEY_HOTSPOT_THRESHOLD_MS);
        builder.groupByLabel = booleanValue(KEY_GROUP_BY_LABEL, true);
        builder.report = booleanValue(KEY_REPORT, true);
        builder.maxCombinedRequests = countValue(KEY_MAX_COMBINED_REQUESTS, DEFAULT_MAX_COMBINED_REQUESTS);
        builder.combinedExcludePattern = patternValue(KEY_COMBINED_EXCLUDE_PATTERN);
        builder.labelHeader = stringValue(KEY_LABEL_HEADER, DEFAULT_LABEL_HEADER);
        builder.descriptionHeader = stringValue(KEY_DESCRIPTION_HEADER, DEFAULT_DESCRIPTION_HEADER);
        builder.fileHeader = stringValue(KEY_FILE_HEADER, null);
        return builder.build();
    }

    /** the tmp-directory is the documented default */
    public static Path defaultOutputDirectory() {
        return Paths.get(System.getProperty("java.io.tmpdir", "."));
    }

    public static boolean isMicroProfileConfigAvailable() {
        return ConfigResolver.isMicroProfileConfigAvailable();
    }

    private static boolean booleanValue(String key, boolean fallback) {
        return ConfigResolver.lookup(key).map(String::trim).map(Boolean::parseBoolean).orElse(fallback);
    }

    private static String stringValue(String key, String fallback) {
        return ConfigResolver.lookup(key).map(String::trim).filter(v -> !v.isEmpty()).orElse(fallback);
    }

    /**
     * @return the configured count, or the fallback when it is unset, not a number or not positive
     */
    private static int countValue(String key, int fallback) {
        Optional<String> configured = ConfigResolver.lookup(key).map(String::trim).filter(v -> !v.isEmpty());
        if (configured.isEmpty()) {
            return fallback;
        }
        try {
            int count = Integer.parseInt(configured.get());
            if (count < 1) {
                LOGGER.warning(() -> "'" + key + "' must be positive - ignoring it: " + count);
                return fallback;
            }
            return count;
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, e,
                    () -> "'" + key + "' is not a number - ignoring it: " + configured.get());
            return fallback;
        }
    }

    /**
     * @return the configured number of milliseconds, or {@code 0} when unset or unusable
     */
    private static long millisecondValue(String key) {
        Optional<String> configured = ConfigResolver.lookup(key).map(String::trim).filter(v -> !v.isEmpty());
        if (configured.isEmpty()) {
            return 0;
        }
        try {
            long millis = Long.parseLong(configured.get());
            if (millis < 0) {
                LOGGER.warning(() -> "'" + key + "' must not be negative - ignoring it: " + millis);
                return 0;
            }
            return millis;
        } catch (NumberFormatException e) {
            //an unusable value must not prevent the application from starting
            LOGGER.log(Level.WARNING, e,
                    () -> "'" + key + "' is not a number of milliseconds - ignoring it: " + configured.get());
            return 0;
        }
    }

    /** comma-separated, blanks and empty entries ignored */
    private static Set<String> listValue(String key) {
        return ConfigResolver.lookup(key)
                .map(value -> Arrays.stream(value.split(","))
                        .map(String::strip)
                        .filter(entry -> !entry.isEmpty())
                        .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
    }

    /**
     * @return the configured pattern, or {@code null} when it is unset, blank or unusable
     */
    private static Pattern patternValue(String key) {
        Optional<String> configured = ConfigResolver.lookup(key).map(String::trim).filter(v -> !v.isEmpty());
        if (configured.isEmpty()) {
            return null;
        }
        String value = configured.get();
        try {
            return Pattern.compile(value);
        } catch (PatternSyntaxException e) {
            //an unusable regex must not prevent the application from starting
            LOGGER.log(Level.WARNING, e,
                    () -> "'" + key + "' is not a valid regular expression - ignoring it: " + value);
            return null;
        }
    }

    /**
     * Decides whether the given bean should be recorded.
     * <p>
     * The include-pattern and the stereotypes are two <em>alternative</em> ways of selecting a
     * bean, so a bean qualifies as soon as one of them says yes:
     * <ul>
     *     <li>neither configured - every bean is recorded, which is the default</li>
     *     <li>only the pattern configured - the bean-name decides</li>
     *     <li>only stereotypes configured - the stereotypes decide</li>
     *     <li>both configured - the union: the name <em>or</em> a stereotype is enough</li>
     * </ul>
     * The exclude-pattern is not part of that union. It is a veto and removes a bean again, no
     * matter which of the two selected it.
     * <p>
     * Note that an include-pattern of {@code .*} together with a stereotype therefore records
     * everything - the pattern alone already selects every bean. Leave the pattern unset to let
     * the stereotypes decide on their own.
     *
     * @param annotationTypeNames the transitive closure of the annotations on the bean-class
     */
    public boolean matches(String className, Set<String> annotationTypeNames) {
        if (excludePattern != null && excludePattern.matcher(className).matches()) {
            return false;
        }
        if (includePattern == null && includeStereotypes.isEmpty()) {
            return true;
        }
        return selectedByIncludePattern(className) || carriesConfiguredStereotype(annotationTypeNames);
    }

    /**
     * @return {@code true} when an include-pattern is configured and the name matches it. An unset
     * pattern selects <em>nothing</em> here - "record everything" is decided in
     * {@link #matches(String, Set)} before the two selectors are consulted, so that an unset
     * pattern cannot silently win the union over a configured stereotype.
     */
    public boolean selectedByIncludePattern(String className) {
        return includePattern != null && includePattern.matcher(className).matches();
    }

    /**
     * @return {@code true} when the bean carries one of the configured stereotypes - directly or
     * through another stereotype. Always {@code false} when no stereotype is configured.
     */
    public boolean carriesConfiguredStereotype(Set<String> annotationTypeNames) {
        for (String annotationTypeName : annotationTypeNames) {
            if (includeStereotypes.contains(annotationTypeName)
                    || includeStereotypes.contains(simpleNameOf(annotationTypeName))) {
                return true;
            }
        }
        return false;
    }

    /** lets the caller skip building the annotation-closure when no stereotype is configured */
    public boolean hasStereotypeRestriction() {
        return !includeStereotypes.isEmpty();
    }

    public Set<String> includeStereotypes() {
        return includeStereotypes;
    }

    private static String simpleNameOf(String annotationTypeName) {
        int lastSeparator = Math.max(annotationTypeName.lastIndexOf('.'), annotationTypeName.lastIndexOf('$'));
        return lastSeparator < 0 ? annotationTypeName : annotationTypeName.substring(lastSeparator + 1);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String includePattern() {
        return includePattern == null ? null : includePattern.pattern();
    }

    public String excludePattern() {
        return excludePattern == null ? null : excludePattern.pattern();
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public DiagramFormat outputFormat() {
        return outputFormat;
    }

    public boolean isFoldLoops() {
        return foldLoops;
    }

    public boolean isCollapseProxyFrames() {
        return collapseProxyFrames;
    }

    public boolean isWriteFiles() {
        return writeFiles;
    }

    /**
     * @return the number of milliseconds a call has to exceed to count as a hotspot, or {@code 0}
     * when hotspot-detection is switched off
     */
    public long hotspotThresholdMillis() {
        return hotspotThresholdMillis;
    }

    public boolean isHotspotDetectionEnabled() {
        return hotspotThresholdMillis > 0;
    }

    /**
     * @return whether a labelled flow is written into a sub-directory of its own; an unlabelled
     * flow is written straight into the output-directory either way
     */
    public boolean isGroupByLabel() {
        return groupByLabel;
    }

    /**
     * @return whether a labelled use-case also gets its combined diagram, its index and an entry in
     * the generated document - the whole point of labelling, so on by default
     */
    public boolean isReport() {
        return report;
    }

    /**
     * @return how many requests a combined diagram may hold before the document links it instead of
     * inlining it; a use-case of a hundred requests is a strip nobody can read
     */
    public int maxCombinedRequests() {
        return maxCombinedRequests;
    }

    /**
     * @return the entry-points ({@code Type.method}) an application considers noise in the story of a
     * use-case - recorded and kept, but left out of the combined diagram; or {@code null}
     */
    public Pattern combinedExcludePattern() {
        return combinedExcludePattern;
    }

    public String labelHeader() {
        return labelHeader;
    }

    public String descriptionHeader() {
        return descriptionHeader;
    }

    /**
     * @return a line written as a comment at the top of every generated diagram - a licence header,
     * typically, so a build which insists on one does not need an exclusion; or {@code null}
     */
    public String fileHeader() {
        return fileHeader;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "FlowConfig[enabled=" + enabled
                + ", include=" + includePattern()
                + ", exclude=" + excludePattern()
                + ", includeStereotypes=" + includeStereotypes
                + ", outputDirectory=" + outputDirectory
                + ", outputFormat=" + outputFormat
                + ", foldLoops=" + foldLoops
                + ", collapseProxyFrames=" + collapseProxyFrames
                + ", writeFiles=" + writeFiles
                + ", hotspotThresholdMillis=" + hotspotThresholdMillis + "]";
    }

    public static final class Builder {
        private boolean enabled = true;
        private Pattern includePattern;
        private Pattern excludePattern;
        private Set<String> includeStereotypes = Set.of();
        private Path outputDirectory = defaultOutputDirectory();
        private DiagramFormat outputFormat = DEFAULT_OUTPUT_FORMAT;
        private boolean foldLoops = true;
        private boolean collapseProxyFrames = true;
        private boolean writeFiles = true;
        private long hotspotThresholdMillis;
        private boolean groupByLabel = true;
        private boolean report = true;
        private int maxCombinedRequests = DEFAULT_MAX_COMBINED_REQUESTS;
        private Pattern combinedExcludePattern;
        private String labelHeader = DEFAULT_LABEL_HEADER;
        private String descriptionHeader = DEFAULT_DESCRIPTION_HEADER;
        private String fileHeader;

        public Builder groupByLabel(boolean value) {
            this.groupByLabel = value;
            return this;
        }

        public Builder report(boolean value) {
            this.report = value;
            return this;
        }

        public Builder maxCombinedRequests(int value) {
            this.maxCombinedRequests = value;
            return this;
        }

        public Builder combinedExcludePattern(String value) {
            this.combinedExcludePattern = value == null ? null : Pattern.compile(value);
            return this;
        }

        public Builder labelHeader(String value) {
            this.labelHeader = value;
            return this;
        }

        public Builder descriptionHeader(String value) {
            this.descriptionHeader = value;
            return this;
        }

        public Builder fileHeader(String value) {
            this.fileHeader = value;
            return this;
        }

        public Builder enabled(boolean value) {
            this.enabled = value;
            return this;
        }

        public Builder includePattern(String value) {
            this.includePattern = value == null ? null : Pattern.compile(value);
            return this;
        }

        public Builder excludePattern(String value) {
            this.excludePattern = value == null ? null : Pattern.compile(value);
            return this;
        }

        public Builder includeStereotypes(String... values) {
            this.includeStereotypes = Set.of(values);
            return this;
        }

        public Builder includeStereotypes(Class<?>... values) {
            this.includeStereotypes = Arrays.stream(values)
                    .map(Class::getName)
                    .collect(Collectors.toUnmodifiableSet());
            return this;
        }

        public Builder outputDirectory(Path value) {
            this.outputDirectory = value;
            return this;
        }

        public Builder outputFormat(DiagramFormat value) {
            this.outputFormat = value;
            return this;
        }

        public Builder foldLoops(boolean value) {
            this.foldLoops = value;
            return this;
        }

        public Builder collapseProxyFrames(boolean value) {
            this.collapseProxyFrames = value;
            return this;
        }

        public Builder writeFiles(boolean value) {
            this.writeFiles = value;
            return this;
        }

        public Builder hotspotThresholdMillis(long value) {
            this.hotspotThresholdMillis = value;
            return this;
        }

        public FlowConfig build() {
            return new FlowConfig(this);
        }
    }
}
