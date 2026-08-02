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

import jakarta.decorator.Decorator;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.interceptor.Interceptor;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether the recorder may be added to a bean-class.
 * <p>
 * Two kinds of types are rejected. The first kind <em>cannot</em> be intercepted - the container
 * builds a subclass for an intercepted bean, so a final class, a final business-method or a
 * class without an accessible constructor would turn into a deployment-error rather than into a
 * diagram. The second kind <em>must not</em> be intercepted - the addon's own classes and the
 * infrastructure of the container itself, because recording the recorder recurses.
 */
final class Instrumentability {

    /** the addon's own packages - explicitly listed so that {@code ...renderer.examples} still matches */
    private static final Set<String> ADDON_PACKAGES = Set.of(
            "org.os890.cdi.uml.dynamic.flow.renderer.api",
            "org.os890.cdi.uml.dynamic.flow.renderer.config",
            "org.os890.cdi.uml.dynamic.flow.renderer.extension",
            "org.os890.cdi.uml.dynamic.flow.renderer.render",
            "org.os890.cdi.uml.dynamic.flow.renderer.runtime",
            "org.os890.cdi.uml.dynamic.flow.renderer.sink");

    private static final String[] INFRASTRUCTURE_PACKAGE_PREFIXES = {
            "java.", "javax.", "jakarta.", "jdk.", "sun.", "com.sun.",
            "org.jboss.", "org.apache.webbeans.", "org.apache.geronimo.",
            "io.smallrye.", "org.eclipse.microprofile.",
            "org.junit.", "org.assertj.", "org.opentest4j."
    };

    private Instrumentability() {
    }

    static boolean isInstrumentable(AnnotatedType<?> type) {
        return rejectionReason(type).isEmpty();
    }

    /**
     * @return the reason why the type is not instrumentable, or empty when it is
     */
    static Optional<String> rejectionReason(AnnotatedType<?> type) {
        Class<?> javaClass = type.getJavaClass();

        if (ADDON_PACKAGES.contains(javaClass.getPackageName())) {
            return Optional.of("belongs to cdi-flow itself");
        }
        for (String prefix : INFRASTRUCTURE_PACKAGE_PREFIXES) {
            if (javaClass.getName().startsWith(prefix)) {
                return Optional.of("infrastructure package");
            }
        }

        if (javaClass.isInterface() || javaClass.isAnnotation() || javaClass.isEnum()
                || javaClass.isRecord() || javaClass.isPrimitive() || javaClass.isArray()) {
            return Optional.of("not a managed bean-class");
        }
        if (javaClass.isSynthetic() || javaClass.isAnonymousClass() || javaClass.isLocalClass()
                || javaClass.isHidden()) {
            return Optional.of("generated or non-top-level class");
        }
        if (javaClass.getEnclosingClass() != null && !Modifier.isStatic(javaClass.getModifiers())) {
            return Optional.of("non-static inner class");
        }

        int modifiers = javaClass.getModifiers();
        if (Modifier.isFinal(modifiers)) {
            return Optional.of("final class - the container cannot subclass it for interception");
        }
        if (Modifier.isAbstract(modifiers)) {
            return Optional.of("abstract class");
        }
        if (!hasAccessibleConstructor(javaClass)) {
            return Optional.of("no non-private constructor");
        }

        Method finalBusinessMethod = findFinalBusinessMethod(javaClass);
        if (finalBusinessMethod != null) {
            return Optional.of("final business-method '" + finalBusinessMethod.getName() + "'");
        }

        if (type.isAnnotationPresent(Interceptor.class) || type.isAnnotationPresent(Decorator.class)) {
            return Optional.of("interceptor or decorator");
        }
        if (Extension.class.isAssignableFrom(javaClass)
                || BuildCompatibleExtension.class.isAssignableFrom(javaClass)) {
            return Optional.of("CDI extension");
        }
        if (FlowSink.class.isAssignableFrom(javaClass)) {
            return Optional.of("flow-sink");
        }

        return Optional.empty();
    }

    private static boolean hasAccessibleConstructor(Class<?> javaClass) {
        for (var constructor : javaClass.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A class-level interceptor-binding turns every non-static, non-private method into an
     * intercepted business-method - and a final one of those is rejected by the container.
     */
    private static Method findFinalBusinessMethod(Class<?> javaClass) {
        for (Class<?> current = javaClass; current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isFinal(modifiers) && !Modifier.isStatic(modifiers)
                        && !Modifier.isPrivate(modifiers) && !method.isBridge() && !method.isSynthetic()) {
                    return method;
                }
            }
        }
        return null;
    }
}
