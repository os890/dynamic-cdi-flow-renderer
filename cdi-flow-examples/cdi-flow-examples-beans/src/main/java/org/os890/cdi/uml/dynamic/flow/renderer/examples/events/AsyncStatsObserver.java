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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.AuditService;

/**
 * Runs on a container-managed worker-thread. A recorded flow belongs to exactly one thread, so
 * this observer produces a diagram of its own instead of being nested into the firing method.
 */
@ApplicationScoped
public class AsyncStatsObserver {

    @Inject
    private AuditService auditService;

    public void onCheckoutAsync(@ObservesAsync CheckoutEvent event) {
        auditService.log("statistics updated for " + event.sku());
    }
}
