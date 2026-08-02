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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.events.CheckoutService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.nesting.Level1Service;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.runtime.FlowRuntime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With {@code cdi-flow.enabled=false} the extension must stay completely out of the way: no
 * interceptor-binding is added anywhere and the application behaves exactly as without the addon.
 */
class DisabledRecorderIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.configured().disabled().startFor(DisabledRecorderIT.class);
    }

    @AfterAll
    static void stopContainer() {
        container.close();
    }

    @Test
    @DisplayName("the application still works")
    void keepsTheApplicationWorking() {
        assertThat(container.get(OrderService.class).placeOrder("SKU-1", 2).reserved()).isTrue();
        assertThat(container.get(Level1Service.class).descend("start"))
                .isEqualTo("start/level1/level2/level3/level4/level5/level6");
        container.get(CheckoutService.class).checkout("SKU-1", 2);
    }

    @Test
    @DisplayName("nothing is recorded and nothing is written")
    void recordsNothing() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);
        container.get(Level1Service.class).descend("start");

        assertThat(container.flows().all()).isEmpty();
        assertThat(container.writtenDiagrams()).isEmpty();
    }

    @Test
    @DisplayName("the runtime is not even started")
    void doesNotStartTheRuntime() {
        assertThat(FlowRuntime.active()).isNull();
    }
}
