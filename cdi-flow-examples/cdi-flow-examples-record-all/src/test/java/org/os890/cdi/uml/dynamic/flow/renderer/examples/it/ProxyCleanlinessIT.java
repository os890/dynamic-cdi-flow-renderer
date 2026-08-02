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

import jakarta.enterprise.context.control.RequestContextController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.scopes.ScopeMixingService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The scopes differ in how much the container proxies: a normal-scoped bean gets a client-proxy
 * on top of the interceptor-subclass, a pseudo-scoped one only the subclass. None of that may
 * reach the diagram - neither as a mangled name nor as a duplicated frame.
 */
class ProxyCleanlinessIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(ProxyCleanlinessIT.class);
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

    private CallFlow greetAcrossAllScopes() {
        RequestContextController requestContext = container.get(RequestContextController.class);
        requestContext.activate();
        try {
            container.get(ScopeMixingService.class).greetAll("cdi");
        } finally {
            requestContext.deactivate();
        }
        return container.flows().singleEnteredAt(ScopeMixingService.class);
    }

    @Test
    @DisplayName("every scope shows up exactly once, in call-order")
    void recordsEveryScopeExactlyOnce() {
        CallFlow flow = greetAcrossAllScopes();

        assertThat(Flows.flatten(flow)).containsExactly(
                "ScopeMixingService#greetAll(String)",
                "ApplicationScopedGreeter#greet(String)",
                "DependentGreeter#greet(String)",
                "RequestScopedGreeter#greet(String)",
                "SingletonGreeter#greet(String)");
    }

    @Test
    @DisplayName("no bean-name carries a proxy-suffix of the CDI implementation")
    void keepsBeanNamesClean() {
        CallFlow flow = greetAcrossAllScopes();

        assertThat(Flows.flatten(flow)).allSatisfy(entry -> assertThat(entry)
                .doesNotContain("$").doesNotContain("Proxy")
                .doesNotContain("Subclass").doesNotContain("Weld").doesNotContain("Owb"));
        MermaidAssertions.assertFreeOfProxyNames(flow.toMermaid());
        MermaidAssertions.assertWellFormed(flow.toMermaid());
    }

    @Test
    @DisplayName("the recorded class-names are the ones the developer wrote")
    void reportsTheDeclaredBeanClasses() {
        CallFlow flow = greetAcrossAllScopes();

        assertThat(flow.root().beanClassName())
                .isEqualTo(ScopeMixingService.class.getName());
        assertThat(flow.root().children()).extracting(CallNode::beanClassName)
                .allSatisfy(className -> assertThat(className)
                        .startsWith("org.os890.cdi.uml.dynamic.flow.renderer.examples.scopes.")
                        .doesNotContain("$"));
    }

    @Test
    @DisplayName("a proxy in the chain does not turn into a duplicated frame")
    void producesNoDuplicatedFrames() {
        CallFlow flow = greetAcrossAllScopes();

        assertThat(Flows.totalCallCount(flow.root())).isEqualTo(5);
        assertThat(flow.root().children()).hasSize(4);
        assertThat(flow.root().children()).allSatisfy(child -> {
            assertThat(child.children()).as("%s must not nest a copy of itself", child).isEmpty();
            assertThat(child.collapsedProxyHops())
                    .as("proxy-hops collapsed below %s", child).isZero();
        });
    }

    @Test
    @DisplayName("each participant lane is declared exactly once")
    void declaresEachParticipantOnce() {
        String diagram = greetAcrossAllScopes().toMermaid();

        for (String bean : new String[]{"ScopeMixingService", "ApplicationScopedGreeter",
                "DependentGreeter", "RequestScopedGreeter", "SingletonGreeter"}) {
            assertThat(MermaidAssertions.countStatements(diagram, "participant " + bean))
                    .as("participant declarations of %s", bean).isOne();
        }
    }
}
