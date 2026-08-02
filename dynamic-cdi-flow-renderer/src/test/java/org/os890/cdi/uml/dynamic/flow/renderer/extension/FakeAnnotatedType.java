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

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Minimal {@link AnnotatedType} over a plain class, so the instrumentability-rules can be tested
 * without booting a container. Only what the rules actually read is implemented.
 */
final class FakeAnnotatedType<X> implements AnnotatedType<X> {

    private final Class<X> javaClass;

    private FakeAnnotatedType(Class<X> javaClass) {
        this.javaClass = javaClass;
    }

    static <X> AnnotatedType<X> of(Class<X> javaClass) {
        return new FakeAnnotatedType<>(javaClass);
    }

    @Override
    public Class<X> getJavaClass() {
        return javaClass;
    }

    @Override
    public Type getBaseType() {
        return javaClass;
    }

    @Override
    public Set<Type> getTypeClosure() {
        return Set.of(javaClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return javaClass.getAnnotation(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return Set.of(javaClass.getAnnotations());
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return javaClass.isAnnotationPresent(annotationType);
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return Set.of();
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return Set.of();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        return Set.of();
    }
}
