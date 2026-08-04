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

package org.os890.cdi.uml.dynamic.flow.renderer.lite;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowRecorded;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.extension.InstrumentabilityRules;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Attaches the recording interceptor while the application is built.
 *
 * <p>A container which resolves beans, interceptors and bindings at build-time never runs a portable
 * extension, so the one shipped with cdi-flow does nothing there - the interceptor sits in the jar
 * and is never bound to anything. A build compatible extension is the moment such a container still
 * accepts an annotation, and attaching {@link FlowRecorded} is all that is missing: the recorder
 * arms itself on the first call it then sees.
 *
 * <p>The same {@link InstrumentabilityRules} the portable extension uses decide what may be
 * instrumented, so a class the container cannot subclass is left alone here as well - attaching the
 * binding to one turns a working application into a failed build.
 *
 * <p>What to record is narrowed with {@code cdi-flow.include-pattern} and
 * {@code cdi-flow.exclude-pattern}. Without a pattern every eligible bean the index holds is
 * recorded, which in a framework with hundreds of its own beans is rarely what anybody wants - the
 * infrastructure packages are skipped, but an application should still name its own.
 */
public class FlowRecordingBuildExtension implements BuildCompatibleExtension {

    private static final Logger LOGGER = Logger.getLogger(FlowRecordingBuildExtension.class.getName());

    private final FlowConfig config = FlowConfig.load();

    private int instrumented;

    @Enhancement(types = Object.class, withSubtypes = true)
    public void attachRecorder(ClassConfig candidate) {
        if (!config.isEnabled()) {
            return;
        }
        ClassInfo type = candidate.info();
        Optional<String> rejectionReason =
                InstrumentabilityRules.rejectionReason(new ClassInfoTypeFacts(type));
        if (rejectionReason.isPresent()) {
            LOGGER.finest(() -> "cdi-flow skips " + type.name() + ": " + rejectionReason.get());
            return;
        }
        if (!config.matches(type.name(), annotationNamesOf(type))) {
            return;
        }
        candidate.addAnnotation(FlowRecorded.class);
        instrumented++;
        LOGGER.fine(() -> "cdi-flow records " + type.name());
        if (instrumented == 1) {
            LOGGER.info("cdi-flow attaches its recorder while this application is built");
        }
    }

    /** the annotations of the class itself - what {@code cdi-flow.include-stereotypes} matches against */
    private static Set<String> annotationNamesOf(ClassInfo type) {
        return type.annotations().stream()
                .map(AnnotationInfo::declaration)
                .map(ClassInfo::name)
                .collect(Collectors.toSet());
    }
}
