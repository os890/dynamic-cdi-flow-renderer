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

package org.os890.cdi.uml.dynamic.flow.renderer.extension;

import java.util.Optional;
import java.util.Set;

/**
 * Which classes the recording binding may be attached to, and why not when it may not.
 *
 * <p>Shared by every integration - the portable extension, the build compatible extension and the
 * Quarkus extension - so that all of them reject the same classes for the same stated reason.
 * Attaching the binding to a class the container cannot subclass does not degrade gracefully: it
 * turns a working deployment into one that refuses to start.
 */
public final class InstrumentabilityRules {

    /** the addon's own packages - listed, so that an application package below them still matches */
    private static final Set<String> ADDON_PACKAGES = Set.of(
            "org.os890.cdi.uml.dynamic.flow.renderer.api",
            "org.os890.cdi.uml.dynamic.flow.renderer.config",
            "org.os890.cdi.uml.dynamic.flow.renderer.extension",
            "org.os890.cdi.uml.dynamic.flow.renderer.lite",
            "org.os890.cdi.uml.dynamic.flow.renderer.jaxrs",
            "org.os890.cdi.uml.dynamic.flow.renderer.quarkus",
            "org.os890.cdi.uml.dynamic.flow.renderer.report",
            "org.os890.cdi.uml.dynamic.flow.renderer.render",
            "org.os890.cdi.uml.dynamic.flow.renderer.runtime",
            "org.os890.cdi.uml.dynamic.flow.renderer.sink");

    private static final String[] INFRASTRUCTURE_PACKAGE_PREFIXES = {
            "java.", "javax.", "jakarta.", "jdk.", "sun.", "com.sun.",
            "org.jboss.", "org.apache.webbeans.", "org.apache.geronimo.",
            "io.quarkus.", "io.vertx.", "io.netty.", "org.hibernate.", "org.flywaydb.",
            "io.smallrye.", "org.eclipse.microprofile.",
            "org.junit.", "org.assertj.", "org.opentest4j."
    };

    private InstrumentabilityRules() {
    }

    public static boolean isInstrumentable(TypeFacts type) {
        return rejectionReason(type).isEmpty();
    }

    /**
     * @return the reason why the type must not be instrumented, or empty when it may be
     */
    public static Optional<String> rejectionReason(TypeFacts type) {
        if (ADDON_PACKAGES.contains(type.packageName())) {
            return Optional.of("belongs to cdi-flow itself");
        }
        for (String prefix : INFRASTRUCTURE_PACKAGE_PREFIXES) {
            if (type.className().startsWith(prefix)) {
                return Optional.of("infrastructure package");
            }
        }
        if (type.kind() != TypeFacts.Kind.CLASS) {
            return Optional.of("not a managed bean-class");
        }
        if (type.isGenerated()) {
            return Optional.of("generated or non-top-level class");
        }
        if (type.isNested()) {
            return Optional.of("non-static inner class");
        }
        if (type.isFinal()) {
            return Optional.of("final class - the container cannot subclass it for interception");
        }
        if (type.isAbstract()) {
            return Optional.of("abstract class");
        }
        if (!type.hasUsableConstructor()) {
            return Optional.of("no non-private constructor");
        }
        Optional<String> finalBusinessMethod = type.finalBusinessMethodName();
        if (finalBusinessMethod.isPresent()) {
            return Optional.of("final business-method '" + finalBusinessMethod.get() + "'");
        }
        if (type.isInterceptorOrDecorator()) {
            return Optional.of("interceptor or decorator");
        }
        if (type.isCdiExtension()) {
            return Optional.of("CDI extension");
        }
        if (type.isFlowSink()) {
            return Optional.of("flow-sink");
        }
        return Optional.empty();
    }
}
