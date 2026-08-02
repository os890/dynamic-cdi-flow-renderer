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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.edge.BeanWithFinalMethod;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.edge.FinalBean;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.edge.GreetingProducer;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A bean the container could not subclass must be skipped silently. Adding the binding to it would
 * turn a working deployment into a {@code DeploymentException} - which would make the addon
 * impossible to just drop onto the class-path.
 */
class NonInstrumentableBeansIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(NonInstrumentableBeansIT.class);
    }

    @AfterAll
    static void stopContainer() {
        container.close();
    }

    @BeforeEach
    void forgetPreviousFlows() {
        container.flows().clear();
        container.clearWrittenDiagrams();
    }

    @Test
    @DisplayName("the deployment succeeds although the archive contains non-proxyable beans")
    void deploysDespiteNonProxyableBeans() {
        assertThat(container.get(FinalBean.class).describe()).isEqualTo("final bean");
        assertThat(container.get(BeanWithFinalMethod.class).stamp()).isEqualTo("stamped");
    }

    @Test
    @DisplayName("a final bean-class is not recorded")
    void skipsFinalBeanClasses() {
        container.get(FinalBean.class).describe();

        assertThat(container.flows().all()).isEmpty();
    }

    @Test
    @DisplayName("a bean with a final business-method is not recorded")
    void skipsBeansWithFinalBusinessMethods() {
        container.get(BeanWithFinalMethod.class).stamp();

        assertThat(container.flows().all()).isEmpty();
    }

    @Test
    @DisplayName("a produced bean is no managed bean and therefore not recorded")
    void skipsProducedBeans() {
        //resolving it runs the producer-method, which is recorded - that is a separate call
        GreetingProducer.GreetingTemplate template = container.get(GreetingProducer.GreetingTemplate.class);
        container.flows().clear();

        assertThat(template.format("cdi")).isEqualTo("hello cdi");

        assertThat(container.flows().all()).isEmpty();
    }

    @Test
    @DisplayName("the producer-method itself belongs to a managed bean and is recorded")
    void recordsTheProducerBean() {
        container.get(GreetingProducer.class).greetingTemplate();

        assertThat(Flows.flatten(container.flows().singleEnteredAt(GreetingProducer.class)))
                .containsExactly("GreetingProducer#greetingTemplate()");
    }
}
