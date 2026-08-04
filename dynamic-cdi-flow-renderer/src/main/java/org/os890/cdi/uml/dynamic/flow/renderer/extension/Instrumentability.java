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
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.interceptor.Interceptor;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Answers {@link InstrumentabilityRules} about a type the portable extension was handed, with the
 * reflection an already-loaded class allows.
 *
 * <p>The rules themselves - and the reasons they give - are shared with the build compatible
 * extension, which has to answer the same questions from a build-time model instead.
 */
final class Instrumentability {

    private Instrumentability() {
    }

    static boolean isInstrumentable(AnnotatedType<?> type) {
        return rejectionReason(type).isEmpty();
    }

    /**
     * @return the reason why the type is not instrumentable, or empty when it is
     */
    static Optional<String> rejectionReason(AnnotatedType<?> type) {
        return InstrumentabilityRules.rejectionReason(new ReflectiveTypeFacts(type));
    }

    private static final class ReflectiveTypeFacts implements TypeFacts {

        private final AnnotatedType<?> type;
        private final Class<?> javaClass;

        private ReflectiveTypeFacts(AnnotatedType<?> type) {
            this.type = type;
            this.javaClass = type.getJavaClass();
        }

        @Override
        public String className() {
            return javaClass.getName();
        }

        @Override
        public String packageName() {
            return javaClass.getPackageName();
        }

        @Override
        public Kind kind() {
            if (javaClass.isAnnotation()) {
                return Kind.ANNOTATION;
            }
            if (javaClass.isInterface()) {
                return Kind.INTERFACE;
            }
            if (javaClass.isEnum()) {
                return Kind.ENUM;
            }
            if (javaClass.isRecord()) {
                return Kind.RECORD;
            }
            if (javaClass.isPrimitive() || javaClass.isArray()) {
                return Kind.OTHER;
            }
            return Kind.CLASS;
        }

        @Override
        public boolean isAbstract() {
            return Modifier.isAbstract(javaClass.getModifiers());
        }

        @Override
        public boolean isFinal() {
            return Modifier.isFinal(javaClass.getModifiers());
        }

        @Override
        public boolean isNested() {
            return javaClass.getEnclosingClass() != null && !Modifier.isStatic(javaClass.getModifiers());
        }

        @Override
        public boolean isGenerated() {
            return javaClass.isSynthetic() || javaClass.isAnonymousClass()
                    || javaClass.isLocalClass() || javaClass.isHidden();
        }

        @Override
        public boolean hasUsableConstructor() {
            for (var constructor : javaClass.getDeclaredConstructors()) {
                if (!Modifier.isPrivate(constructor.getModifiers())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Optional<String> finalBusinessMethodName() {
            for (Class<?> current = javaClass; current != null && current != Object.class;
                 current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    int modifiers = method.getModifiers();
                    if (Modifier.isFinal(modifiers) && !Modifier.isStatic(modifiers)
                            && !Modifier.isPrivate(modifiers) && !method.isBridge()
                            && !method.isSynthetic()) {
                        return Optional.of(method.getName());
                    }
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean isInterceptorOrDecorator() {
            return type.isAnnotationPresent(Interceptor.class) || type.isAnnotationPresent(Decorator.class);
        }

        @Override
        public boolean isCdiExtension() {
            return Extension.class.isAssignableFrom(javaClass)
                    || BuildCompatibleExtension.class.isAssignableFrom(javaClass);
        }

        @Override
        public boolean isFlowSink() {
            return FlowSink.class.isAssignableFrom(javaClass);
        }
    }
}
