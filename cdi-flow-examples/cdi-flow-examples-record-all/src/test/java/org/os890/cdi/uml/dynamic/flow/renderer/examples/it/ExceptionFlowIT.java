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
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.edge.FailingOrchestrator;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionFlowIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(ExceptionFlowIT.class);
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

    private CallFlow runFailingOrchestration() {
        assertThatThrownBy(() -> container.get(FailingOrchestrator.class).run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("inventory is unavailable");
        return container.flows().singleEnteredAt(FailingOrchestrator.class);
    }

    @Test
    @DisplayName("the exception is passed on unchanged - the recorder only observes")
    void doesNotSwallowTheException() {
        CallFlow flow = runFailingOrchestration();

        assertThat(flow).isNotNull();
    }

    @Test
    @DisplayName("everything before the failure is still recorded")
    void recordsTheWholeChainUpToTheFailure() {
        CallFlow flow = runFailingOrchestration();

        assertThat(Flows.flatten(flow)).containsExactly(
                "FailingOrchestrator#run()",
                "AuditService#log(String)",
                "FailingService#fail()");
    }

    @Test
    @DisplayName("every frame the exception travelled through is marked as failed")
    void marksTheFailingFrames() {
        CallFlow flow = runFailingOrchestration();
        CallNode root = flow.root();

        assertThat(root.hasFailed()).isTrue();
        assertThat(root.thrownTypeName()).isEqualTo("IllegalStateException");

        CallNode successfulLog = root.children().get(0);
        CallNode failingCall = root.children().get(1);
        assertThat(successfulLog.hasFailed()).as("the call before the failure").isFalse();
        assertThat(failingCall.hasFailed()).isTrue();
        assertThat(failingCall.thrownTypeName()).isEqualTo("IllegalStateException");
    }

    @Test
    @DisplayName("a failed flow is written and drawn with cross-arrows")
    void writesAndRendersTheFailedFlow() {
        CallFlow flow = runFailingOrchestration();
        String diagram = flow.toMermaid();

        assertThat(diagram)
                .contains("FailingService--xFailingOrchestrator: throws IllegalStateException")
                .contains("FailingOrchestrator--xCaller: throws IllegalStateException")
                .contains("AuditService-->>FailingOrchestrator: void");
        MermaidAssertions.assertWellFormed(diagram);

        assertThat(container.writtenDiagrams()).hasSize(1);
        assertThat(container.writtenDiagrams().get(0).getFileName().toString())
                .startsWith("FailingOrchestrator_run_");
    }

    @Test
    @DisplayName("the thread is left clean, so the next call starts a fresh flow")
    void resetsTheThreadStateAfterAFailure() {
        runFailingOrchestration();
        container.flows().clear();

        container.get(org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService.class).placeOrder("SKU-1", 1);

        CallFlow nextFlow = container.flows().single();
        assertThat(nextFlow.entryTypeSimpleName()).isEqualTo("OrderService");
        assertThat(nextFlow.root().hasFailed()).isFalse();
    }
}
