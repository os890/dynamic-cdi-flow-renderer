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
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.util.concurrent.CompletionStage;

/**
 * Fires CDI events. A synchronous event is delivered on the calling thread, so the observers show
 * up nested inside {@code checkout(..)}. An asynchronous event is delivered on another thread and
 * therefore produces its own diagram.
 */
@ApplicationScoped
public class CheckoutService {

    @Inject
    private Event<CheckoutEvent> checkoutEvent;

    public void checkout(String sku, int quantity) {
        checkoutEvent.fire(new CheckoutEvent(sku, quantity));
    }

    public CompletionStage<CheckoutEvent> checkoutAsync(String sku, int quantity) {
        return checkoutEvent.fireAsync(new CheckoutEvent(sku, quantity));
    }
}
