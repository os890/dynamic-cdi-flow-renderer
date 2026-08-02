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

import jakarta.enterprise.inject.spi.AnnotatedType;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * Collects the annotations of a bean-class, transitively.
 * <p>
 * Stereotypes may be stacked - a stereotype can itself be annotated with further stereotypes - so
 * looking only at the annotations written on the class would miss exactly the case the feature is
 * most useful for. The closure is taken over the {@link AnnotatedType}, not over the raw class, so
 * annotations other extensions added are honoured as well.
 */
final class Stereotypes {

    /** meta-annotations every annotation carries - they can never be a stereotype */
    private static final String JAVA_META_ANNOTATION_PACKAGE = "java.lang.annotation.";

    private Stereotypes() {
    }

    static Set<String> annotationNamesOf(AnnotatedType<?> annotatedType) {
        Set<String> annotationNames = new HashSet<>();
        for (Annotation annotation : annotatedType.getAnnotations()) {
            collect(annotation.annotationType(), annotationNames);
        }
        return annotationNames;
    }

    private static void collect(Class<? extends Annotation> annotationType, Set<String> collected) {
        String name = annotationType.getName();
        if (name.startsWith(JAVA_META_ANNOTATION_PACKAGE)) {
            return;
        }
        //add() returning false also breaks the cycles the meta-annotations form
        if (!collected.add(name)) {
            return;
        }
        for (Annotation metaAnnotation : annotationType.getAnnotations()) {
            collect(metaAnnotation.annotationType(), collected);
        }
    }
}
