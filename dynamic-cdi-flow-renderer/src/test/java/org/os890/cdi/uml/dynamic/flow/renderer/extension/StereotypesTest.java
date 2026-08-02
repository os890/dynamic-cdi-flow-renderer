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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.testbeans.StereotypeFixtures;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StereotypesTest {

    private static Set<String> annotationNamesOf(Class<?> beanClass) {
        return Stereotypes.annotationNamesOf(FakeAnnotatedType.of(beanClass));
    }

    @Test
    @DisplayName("a stereotype written on the class is found")
    void findsDirectStereotypes() {
        assertThat(annotationNamesOf(StereotypeFixtures.AuditedBean.class))
                .contains(StereotypeFixtures.Audited.class.getName());
    }

    @Test
    @DisplayName("a stereotype reached through another stereotype is found too")
    void findsStackedStereotypes() {
        assertThat(annotationNamesOf(StereotypeFixtures.StackedBean.class))
                .contains(StereotypeFixtures.Stacked.class.getName(),
                        StereotypeFixtures.Audited.class.getName());
    }

    @Test
    @DisplayName("the scope a stereotype declares is part of the closure as well")
    void includesWhatTheStereotypeDeclares() {
        assertThat(annotationNamesOf(StereotypeFixtures.AuditedBean.class))
                .contains("jakarta.enterprise.context.ApplicationScoped",
                        "jakarta.enterprise.inject.Stereotype");
    }

    @Test
    @DisplayName("an inherited stereotype is found on the subclass")
    void findsInheritedStereotypes() {
        assertThat(annotationNamesOf(StereotypeFixtures.InheritingBean.class))
                .contains(StereotypeFixtures.InheritedStereotype.class.getName());
    }

    @Test
    @DisplayName("unrelated stereotypes are not reported")
    void doesNotInventStereotypes() {
        assertThat(annotationNamesOf(StereotypeFixtures.InternalBean.class))
                .contains(StereotypeFixtures.Internal.class.getName())
                .doesNotContain(StereotypeFixtures.Audited.class.getName());

        assertThat(annotationNamesOf(StereotypeFixtures.PlainBean.class))
                .doesNotContain(StereotypeFixtures.Audited.class.getName(),
                        StereotypeFixtures.Internal.class.getName());
    }

    @Test
    @DisplayName("the meta-annotations every annotation carries are left out")
    void skipsJavaMetaAnnotations() {
        assertThat(annotationNamesOf(StereotypeFixtures.AuditedBean.class))
                .noneMatch(name -> name.startsWith("java.lang.annotation."));
    }

    @Test
    @DisplayName("the cyclic meta-annotations of the JDK do not cause an endless walk")
    void terminatesOnAnnotationCycles() {
        assertThat(annotationNamesOf(StereotypeFixtures.MarkedBean.class))
                .contains(StereotypeFixtures.PlainMarker.class.getName());
    }
}
