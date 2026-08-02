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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.recursion.SelfCallService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterizes the one limitation of an interceptor-based recorder: a call a bean makes on itself
 * never passes the container, so no interceptor can see it. This test exists to pin that behaviour
 * rather than to claim it is desirable.
 */
class SelfInvocationIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(SelfInvocationIT.class);
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
    @DisplayName("a self-call is not recorded, but what it calls further on still is")
    void doesNotRecordSelfCalls() {
        assertThat(container.get(SelfCallService.class).outer()).isEqualTo("outer:inner");

        CallFlow flow = container.flows().singleEnteredAt(SelfCallService.class);

        assertThat(Flows.flatten(flow)).containsExactly(
                "SelfCallService#outer()",
                //inner() is missing - it never left the instance
                "AuditService#log(String)");
    }

    @Test
    @DisplayName("calling the same method from outside is recorded normally")
    void recordsTheSameMethodWhenCalledFromOutside() {
        container.get(SelfCallService.class).inner();

        CallFlow flow = container.flows().singleEnteredAt(SelfCallService.class);

        assertThat(Flows.flatten(flow)).containsExactly(
                "SelfCallService#inner()",
                "AuditService#log(String)");
    }
}
