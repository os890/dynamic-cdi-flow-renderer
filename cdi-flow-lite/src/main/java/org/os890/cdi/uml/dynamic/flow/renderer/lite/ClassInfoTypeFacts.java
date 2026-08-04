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

import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import org.os890.cdi.uml.dynamic.flow.renderer.extension.TypeFacts;

import java.util.Optional;
import java.util.Set;

/**
 * Answers the shared instrumentability rules from the build-time model, where there is no loaded
 * class to reflect on - only what the index knows.
 */
final class ClassInfoTypeFacts implements TypeFacts {

    private static final Set<String> INTERCEPTOR_ANNOTATIONS =
            Set.of("jakarta.interceptor.Interceptor", "jakarta.decorator.Decorator");

    private static final Set<String> EXTENSION_TYPES = Set.of(
            "jakarta.enterprise.inject.spi.Extension",
            "jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension");

    private static final String FLOW_SINK = "org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink";

    private final ClassInfo type;

    ClassInfoTypeFacts(ClassInfo type) {
        this.type = type;
    }

    @Override
    public String className() {
        return type.name();
    }

    @Override
    public String packageName() {
        return type.packageInfo() == null ? "" : type.packageInfo().name();
    }

    @Override
    public Kind kind() {
        if (type.isAnnotation()) {
            return Kind.ANNOTATION;
        }
        if (type.isInterface()) {
            return Kind.INTERFACE;
        }
        if (type.isEnum()) {
            return Kind.ENUM;
        }
        if (type.isRecord()) {
            return Kind.RECORD;
        }
        return type.isPlainClass() ? Kind.CLASS : Kind.OTHER;
    }

    @Override
    public boolean isAbstract() {
        return type.isAbstract();
    }

    @Override
    public boolean isFinal() {
        return type.isFinal();
    }

    /**
     * A nested class carries the enclosing class in its name, and a static one is as instantiable as
     * a top-level class. Whether it is static is not part of the build-time model, so a nested class
     * is left alone either way - a bean is normally not one.
     */
    @Override
    public boolean isNested() {
        return type.name().indexOf('$') >= 0;
    }

    @Override
    public boolean isGenerated() {
        return type.name().contains("$$") || type.simpleName().isEmpty();
    }

    /**
     * The interceptor subclass calls a constructor without arguments, so the bean needs one: either
     * declared, or implicit because it declares none at all.
     */
    @Override
    public boolean hasUsableConstructor() {
        return type.constructors().isEmpty()
                || type.constructors().stream().anyMatch(constructor -> constructor.parameters().isEmpty());
    }

    @Override
    public Optional<String> finalBusinessMethodName() {
        return type.methods().stream()
                .filter(method -> method.isFinal() && !method.isStatic() && !method.isConstructor())
                .map(MethodInfo::name)
                .findFirst();
    }

    @Override
    public boolean isInterceptorOrDecorator() {
        return INTERCEPTOR_ANNOTATIONS.stream().anyMatch(this::hasAnnotation);
    }

    @Override
    public boolean isCdiExtension() {
        return type.superInterfacesDeclarations().stream()
                .anyMatch(superInterface -> EXTENSION_TYPES.contains(superInterface.name()));
    }

    @Override
    public boolean isFlowSink() {
        return type.superInterfacesDeclarations().stream()
                .anyMatch(superInterface -> FLOW_SINK.equals(superInterface.name()));
    }

    private boolean hasAnnotation(String annotationName) {
        return type.annotations().stream()
                .anyMatch(annotation -> annotation.declaration().name().equals(annotationName));
    }
}
