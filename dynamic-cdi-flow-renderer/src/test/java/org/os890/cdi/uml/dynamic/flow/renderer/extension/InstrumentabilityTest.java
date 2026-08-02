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
import org.os890.cdi.uml.dynamic.flow.renderer.runtime.FlowRecordingInterceptor;
import org.os890.cdi.uml.dynamic.flow.renderer.testbeans.InstrumentabilityFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class InstrumentabilityTest {

    private static String reasonFor(Class<?> type) {
        return Instrumentability.rejectionReason(FakeAnnotatedType.of(type)).orElse(null);
    }

    @Test
    @DisplayName("an ordinary bean-class is instrumented")
    void acceptsAPlainBean() {
        assertThat(Instrumentability.isInstrumentable(
                FakeAnnotatedType.of(InstrumentabilityFixtures.PlainBean.class))).isTrue();
        assertThat(reasonFor(InstrumentabilityFixtures.PlainBean.class)).isNull();
    }

    @Test
    @DisplayName("a static method being final does not disqualify the bean")
    void acceptsFinalStaticMethods() {
        assertThat(reasonFor(InstrumentabilityFixtures.BeanWithFinalStaticMethod.class)).isNull();
    }

    @Test
    @DisplayName("types the container could not subclass are rejected")
    void rejectsNonProxyableTypes() {
        assertThat(reasonFor(InstrumentabilityFixtures.FinalBean.class))
                .contains("final class");
        assertThat(reasonFor(InstrumentabilityFixtures.BeanWithFinalMethod.class))
                .contains("final business-method");
        assertThat(reasonFor(InstrumentabilityFixtures.BeanWithFinalMethodInSuperclass.class))
                .contains("final business-method");
        assertThat(reasonFor(InstrumentabilityFixtures.BeanWithPrivateConstructorOnly.class))
                .contains("no non-private constructor");
        assertThat(reasonFor(InstrumentabilityFixtures.AbstractBean.class))
                .contains("abstract class");
        assertThat(reasonFor(InstrumentabilityFixtures.Outer.NonStaticInnerBean.class))
                .contains("non-static inner class");
    }

    @Test
    @DisplayName("types which are no managed bean-classes are rejected")
    void rejectsNonBeanClasses() {
        assertThat(reasonFor(InstrumentabilityFixtures.SomeInterface.class))
                .contains("not a managed bean-class");
        assertThat(reasonFor(InstrumentabilityFixtures.SomeEnum.class))
                .contains("not a managed bean-class");
        assertThat(reasonFor(InstrumentabilityFixtures.SomeRecord.class))
                .contains("not a managed bean-class");
        assertThat(reasonFor(int.class)).contains("not a managed bean-class");
        assertThat(reasonFor(String[].class)).contains("not a managed bean-class");
    }

    @Test
    @DisplayName("CDI infrastructure is never recorded")
    void rejectsCdiInfrastructure() {
        assertThat(reasonFor(InstrumentabilityFixtures.SomeInterceptor.class))
                .isEqualTo("interceptor or decorator");
        assertThat(reasonFor(InstrumentabilityFixtures.SomeDecorator.class)).isNotNull();
        assertThat(reasonFor(InstrumentabilityFixtures.SomeExtension.class))
                .contains("CDI extension");
        assertThat(reasonFor(InstrumentabilityFixtures.SomeSink.class))
                .contains("flow-sink");
    }

    @Test
    @DisplayName("recording the recorder would recurse, so the addon excludes itself")
    void rejectsTheAddonItself() {
        assertThat(reasonFor(FlowRecordingInterceptor.class)).contains("cdi-flow itself");
        assertThat(reasonFor(FlowRecorderExtension.class)).contains("cdi-flow itself");
    }

    @Test
    @DisplayName("the container's own classes are excluded by package")
    void rejectsInfrastructurePackages() {
        assertThat(reasonFor(java.util.ArrayList.class)).contains("infrastructure package");
        assertThat(reasonFor(jakarta.enterprise.util.TypeLiteral.class))
                .contains("infrastructure package");
    }

    @Test
    @DisplayName("the exclusion of the addon must not swallow the examples-package")
    void doesNotRejectTheExamplesPackage() {
        assertThat(reasonFor(InstrumentabilityFixtures.PlainBean.class)).isNull();
        assertThat(InstrumentabilityFixtures.PlainBean.class.getPackageName())
                .startsWith("org.os890.cdi.uml.dynamic.flow.renderer.");
    }
}
