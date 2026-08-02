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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.support;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSinks;
import org.os890.cdi.uml.dynamic.flow.renderer.config.DiagramFormat;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Boots a CDI container for a single test-class.
 * <p>
 * Only the standard {@link SeContainerInitializer} is used, so the very same tests run on Weld
 * ({@code -Pweld}) and on OpenWebBeans ({@code -Powb}).
 * <p>
 * Each test-class gets its own container because the addon reads its configuration once while the
 * container boots - changing {@code cdi-flow.*} at runtime would have no effect.
 */
public final class CdiFlowTestContainer implements AutoCloseable {

    /** set by surefire, so weld and owb runs do not write into the same directory */
    private static final String OUTPUT_DIRECTORY_BASE_KEY = "cdi-flow.test.output-directory";

    private final SeContainer container;
    private final CapturingFlowSink flowSink;
    private final Path outputDirectory;
    private final List<String> systemPropertyKeys;

    private CdiFlowTestContainer(SeContainer container, CapturingFlowSink flowSink,
                                  Path outputDirectory, List<String> systemPropertyKeys) {
        this.container = container;
        this.flowSink = flowSink;
        this.outputDirectory = outputDirectory;
        this.systemPropertyKeys = systemPropertyKeys;
    }

    public static Builder configured() {
        return new Builder();
    }

    /**
     * Boots with the default configuration - i.e. with every example-bean being recorded.
     */
    public static CdiFlowTestContainer startFor(Class<?> testClass) {
        return configured().startFor(testClass);
    }

    public <T> T get(Class<T> beanType) {
        return container.select(beanType).get();
    }

    public SeContainer container() {
        return container;
    }

    public CapturingFlowSink flows() {
        return flowSink;
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    /** every extension a diagram can be written with */
    private static final List<String> DIAGRAM_EXTENSIONS =
            Arrays.stream(DiagramFormat.values()).map(DiagramFormat::fileExtension).toList();

    /**
     * @return the diagram-files written so far - in any format - sorted by name
     */
    public List<Path> writtenDiagrams() {
        return writtenDiagrams(DIAGRAM_EXTENSIONS);
    }

    public List<Path> writtenDiagrams(String fileExtension) {
        return writtenDiagrams(List.of(fileExtension));
    }

    private List<Path> writtenDiagrams(List<String> fileExtensions) {
        if (!Files.isDirectory(outputDirectory)) {
            return List.of();
        }
        try (var files = Files.list(outputDirectory)) {
            return files.filter(path -> fileExtensions.stream()
                            .anyMatch(extension -> path.getFileName().toString().endsWith(extension)))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Lets a single test start from an empty output-directory even though the container - and with
     * it the configured directory - is shared by the whole test-class.
     */
    public void clearWrittenDiagrams() {
        writtenDiagrams().forEach(file -> {
            try {
                Files.delete(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public String readDiagram(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            container.close();
        } finally {
            FlowSinks.clear();
            systemPropertyKeys.forEach(System::clearProperty);
            releaseConfig();
        }
    }

    /**
     * MicroProfile-Config caches one {@code Config} per class-loader. Releasing it makes the next
     * container in the same JVM read the system-properties the next test-class sets.
     */
    private static void releaseConfig() {
        try {
            ConfigProviderResolver resolver = ConfigProviderResolver.instance();
            resolver.releaseConfig(resolver.getConfig());
        } catch (Throwable ignored) {
            //no MicroProfile-Config implementation, or nothing cached - both are fine
        }
    }

    private static void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static final class Builder {

        private final Map<String, String> properties = new LinkedHashMap<>();
        private boolean useDefaultOutputDirectory;

        /**
         * Leaves {@code cdi-flow.output-directory} unset so the addon falls back to the
         * tmp-directory. Only useful together with {@link #writingFiles(boolean)} {@code false} -
         * otherwise the test litters the real tmp-directory.
         */
        public Builder usingDefaultOutputDirectory() {
            this.useDefaultOutputDirectory = true;
            return this;
        }

        public Builder with(String key, String value) {
            properties.put(key, value);
            return this;
        }

        public Builder recordingOnly(String includePattern) {
            return with(FlowConfig.KEY_INCLUDE_PATTERN, includePattern);
        }

        public Builder excluding(String excludePattern) {
            return with(FlowConfig.KEY_EXCLUDE_PATTERN, excludePattern);
        }

        public Builder disabled() {
            return with(FlowConfig.KEY_ENABLED, "false");
        }

        public Builder foldingLoops(boolean value) {
            return with(FlowConfig.KEY_FOLD_LOOPS, Boolean.toString(value));
        }

        public Builder collapsingProxyFrames(boolean value) {
            return with(FlowConfig.KEY_COLLAPSE_PROXY_FRAMES, Boolean.toString(value));
        }

        public Builder writingFiles(boolean value) {
            return with(FlowConfig.KEY_WRITE_FILES, Boolean.toString(value));
        }

        public Builder outputFormat(DiagramFormat value) {
            return with(FlowConfig.KEY_OUTPUT_FORMAT, value.name());
        }

        public CdiFlowTestContainer startFor(Class<?> testClass) {
            Path outputDirectory;
            if (useDefaultOutputDirectory) {
                outputDirectory = FlowConfig.defaultOutputDirectory();
            } else {
                outputDirectory = properties.containsKey(FlowConfig.KEY_OUTPUT_DIRECTORY)
                        ? Path.of(properties.get(FlowConfig.KEY_OUTPUT_DIRECTORY))
                        : defaultOutputDirectoryFor(testClass);
                properties.putIfAbsent(FlowConfig.KEY_OUTPUT_DIRECTORY, outputDirectory.toString());
                deleteRecursively(outputDirectory);
            }

            //the addon reads its configuration during BeforeBeanDiscovery - so set it up first
            releaseConfig();
            List<String> systemPropertyKeys = new ArrayList<>(properties.keySet());
            properties.forEach(System::setProperty);

            CapturingFlowSink flowSink = new CapturingFlowSink();
            FlowSinks.clear();
            FlowSinks.register(flowSink);

            try {
                SeContainer container = SeContainerInitializer.newInstance().initialize();
                return new CdiFlowTestContainer(container, flowSink, outputDirectory, systemPropertyKeys);
            } catch (RuntimeException | Error bootFailure) {
                FlowSinks.clear();
                systemPropertyKeys.forEach(System::clearProperty);
                throw bootFailure;
            }
        }

        private static Path defaultOutputDirectoryFor(Class<?> testClass) {
            String base = System.getProperty(OUTPUT_DIRECTORY_BASE_KEY, "target/flow-diagrams");
            return Path.of(base, testClass.getSimpleName());
        }
    }
}
