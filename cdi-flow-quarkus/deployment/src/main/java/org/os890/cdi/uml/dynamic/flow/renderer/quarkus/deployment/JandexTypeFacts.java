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

package org.os890.cdi.uml.dynamic.flow.renderer.quarkus.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.os890.cdi.uml.dynamic.flow.renderer.extension.TypeFacts;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

/**
 * Answers the shared instrumentability rules from the Jandex index Quarkus hands its build steps.
 */
final class JandexTypeFacts implements TypeFacts {

    private static final Set<DotName> INTERCEPTOR_ANNOTATIONS = Set.of(
            DotName.createSimple("jakarta.interceptor.Interceptor"),
            DotName.createSimple("jakarta.decorator.Decorator"));

    private static final Set<DotName> EXTENSION_TYPES = Set.of(
            DotName.createSimple("jakarta.enterprise.inject.spi.Extension"),
            DotName.createSimple("jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension"));

    private static final DotName FLOW_SINK =
            DotName.createSimple("org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink");

    private final ClassInfo type;

    JandexTypeFacts(ClassInfo type) {
        this.type = type;
    }

    @Override
    public String className() {
        return type.name().toString();
    }

    @Override
    public String packageName() {
        String name = className();
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? "" : name.substring(0, lastDot);
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
        return Kind.CLASS;
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(type.flags());
    }

    @Override
    public boolean isFinal() {
        return Modifier.isFinal(type.flags());
    }

    @Override
    public boolean isNested() {
        return type.nestingType() != ClassInfo.NestingType.TOP_LEVEL
                && !Modifier.isStatic(type.flags());
    }

    @Override
    public boolean isGenerated() {
        return type.isSynthetic() || className().contains("$$")
                || type.nestingType() == ClassInfo.NestingType.ANONYMOUS
                || type.nestingType() == ClassInfo.NestingType.LOCAL;
    }

    @Override
    public boolean hasUsableConstructor() {
        //ArC needs one it can call from the generated subclass
        return type.constructors().isEmpty()
                || type.constructors().stream()
                        .anyMatch(constructor -> constructor.parametersCount() == 0
                                && !Modifier.isPrivate(constructor.flags()));
    }

    @Override
    public Optional<String> finalBusinessMethodName() {
        return type.methods().stream()
                .filter(method -> Modifier.isFinal(method.flags())
                        && !Modifier.isStatic(method.flags())
                        && !Modifier.isPrivate(method.flags())
                        && !method.isSynthetic()
                        && !method.name().startsWith("<"))
                .map(MethodInfo::name)
                .findFirst();
    }

    @Override
    public boolean isInterceptorOrDecorator() {
        return INTERCEPTOR_ANNOTATIONS.stream().anyMatch(type::hasDeclaredAnnotation);
    }

    @Override
    public boolean isCdiExtension() {
        return type.interfaceNames().stream().anyMatch(EXTENSION_TYPES::contains);
    }

    @Override
    public boolean isFlowSink() {
        return type.interfaceNames().contains(FLOW_SINK);
    }
}
