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
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.Flows;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The exclude-pattern of this example vetoes {@code RoundingService}, which the include-pattern
 * had selected.
 */
class ExcludePatternConfigIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(ExcludePatternConfigIT.class);
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
    @DisplayName("the exclude-pattern removes a bean the include-pattern had selected")
    void appliesTheExcludePatternOnTopOfTheIncludePattern() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);

        assertThat(Flows.flatten(container.flows().singleEnteredAt(OrderService.class)))
                .doesNotContain("RoundingService#round(BigDecimal)")
                .as("its caller is in the very same package and stays")
                .contains("TaxService#taxFor(BigDecimal)");
    }

    @Test
    @DisplayName("an excluded bean in the middle of a chain does not break the nesting")
    void keepsTheChainIntactAroundAnExcludedBean() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);

        CallNode root = container.flows().singleEnteredAt(OrderService.class).root();
        CallNode pricing = root.children().get(0);
        CallNode tax = pricing.children().get(0);

        assertThat(pricing.beanSimpleName()).isEqualTo("PricingService");
        assertThat(tax.beanSimpleName()).isEqualTo("TaxService");
        assertThat(tax.children()).as("the excluded callee simply is not there").isEmpty();
    }
}
