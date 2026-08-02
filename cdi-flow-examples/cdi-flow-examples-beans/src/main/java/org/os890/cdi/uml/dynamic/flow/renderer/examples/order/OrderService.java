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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * Entry-point of the order example: the bean which triggers the first interceptor-call and
 * therefore names the diagram file.
 */
@ApplicationScoped
public class OrderService {

    @Inject
    private PricingService pricingService;

    @Inject
    private InventoryService inventoryService;

    @Inject
    private AuditService auditService;

    public Order placeOrder(String sku, int quantity) {
        BigDecimal grossPrice = pricingService.priceOf(sku);
        boolean reserved = inventoryService.reserve(sku, quantity);
        auditService.log("order placed: " + sku);
        return new Order(sku, quantity, grossPrice, reserved);
    }
}
