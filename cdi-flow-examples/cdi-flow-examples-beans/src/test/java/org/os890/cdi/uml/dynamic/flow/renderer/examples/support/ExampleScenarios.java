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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.support;

import jakarta.enterprise.context.control.RequestContextController;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.edge.FailingOrchestrator;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.events.CheckoutService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.loops.BatchService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.nesting.Level1Service;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.recursion.RecursiveService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.scopes.ScopeMixingService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs one call per feature the recorder has to cope with, so a showcase can be produced in any
 * output-format without duplicating the scenarios.
 */
public final class ExampleScenarios {

    /** the entry-bean of every scenario, in the order they are executed */
    public static final List<String> ENTRY_POINTS = List.of(
            "OrderService_placeOrder_",
            "Level1Service_descend_",
            "BatchService_processAll_",
            "CheckoutService_checkout_",
            "RecursiveService_countdown_",
            "ScopeMixingService_greetAll_",
            "FailingOrchestrator_run_");

    private ExampleScenarios() {
    }

    public static void runAll(CdiFlowTestContainer container) {
        container.get(OrderService.class).placeOrder("SKU-1", 3);
        container.get(Level1Service.class).descend("start");
        container.get(BatchService.class).processAll(List.of("a", "b", "c", "d", "e"));
        container.get(CheckoutService.class).checkout("SKU-7", 2);
        container.get(RecursiveService.class).countdown(3);

        RequestContextController requestContext = container.get(RequestContextController.class);
        requestContext.activate();
        try {
            container.get(ScopeMixingService.class).greetAll("cdi");
        } finally {
            requestContext.deactivate();
        }

        assertThatThrownBy(() -> container.get(FailingOrchestrator.class).run())
                .isInstanceOf(IllegalStateException.class);
    }
}
