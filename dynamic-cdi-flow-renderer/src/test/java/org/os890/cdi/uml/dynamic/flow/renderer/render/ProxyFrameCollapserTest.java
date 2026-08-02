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

package org.os890.cdi.uml.dynamic.flow.renderer.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class ProxyFrameCollapserTest {

    @Test
    @DisplayName("a proxy handing the same call to the same bean-class is removed")
    void collapsesProxyPassThrough() {
        CallNode root = call("OrderService", "placeOrder").params("String").onTarget(1).nanos(0, 5_000_000)
                .calling(call("OrderService", "placeOrder").params("String").onTarget(2).nanos(1_000, 4_999_000)
                        .calling(call("PricingService", "priceOf").onTarget(3).nanos(2_000, 4_000_000)))
                .build();

        CallNode collapsed = ProxyFrameCollapser.collapse(root);

        assertThat(collapsed.collapsedProxyHops()).isOne();
        assertThat(collapsed.children()).extracting(CallNode::beanSimpleName)
                .containsExactly("PricingService");
    }

    @Test
    @DisplayName("a client-proxy stacked on an interceptor-subclass collapses in one pass")
    void collapsesStackedProxies() {
        CallNode root = call("OrderService", "placeOrder").onTarget(1).nanos(0, 5_000_000)
                .calling(call("OrderService", "placeOrder").onTarget(2).nanos(500, 4_999_500)
                        .calling(call("OrderService", "placeOrder").onTarget(3).nanos(1_000, 4_999_000)
                                .calling(call("AuditService", "log").onTarget(4).nanos(2_000, 3_000_000))))
                .build();

        CallNode collapsed = ProxyFrameCollapser.collapse(root);

        assertThat(collapsed.collapsedProxyHops()).isEqualTo(2);
        assertThat(collapsed.children()).extracting(CallNode::beanSimpleName).containsExactly("AuditService");
    }

    @Test
    @DisplayName("real recursion is kept - it re-enters the same instance")
    void keepsRecursionOnTheSameInstance() {
        CallNode root = call("RecursiveService", "countdown").params("int").onTarget(7).nanos(0, 5_000_000)
                .calling(call("RecursiveService", "countdown").params("int").onTarget(7).nanos(100, 4_999_000))
                .build();

        CallNode collapsed = ProxyFrameCollapser.collapse(root);

        assertThat(collapsed.collapsedProxyHops()).isZero();
        assertThat(collapsed.children()).hasSize(1);
    }

    @Test
    @DisplayName("a frame which does real work of its own is kept even if the signature matches")
    void keepsFramesDoingRealWork() {
        CallNode root = call("OrderService", "placeOrder").onTarget(1).nanos(0, 50_000_000)
                .calling(call("OrderService", "placeOrder").onTarget(2).nanos(1_000, 10_000_000))
                .build();

        assertThat(ProxyFrameCollapser.collapse(root).collapsedProxyHops()).isZero();
    }

    @Test
    @DisplayName("delegation to a different bean is never collapsed")
    void keepsDelegationToAnotherBean() {
        CallNode root = call("OrderService", "placeOrder").onTarget(1).nanos(0, 5_000_000)
                .calling(call("OrderServiceImpl", "placeOrder").onTarget(2).nanos(100, 4_999_000))
                .build();

        assertThat(ProxyFrameCollapser.collapse(root).collapsedProxyHops()).isZero();
    }

    @Test
    @DisplayName("a frame with more than one child is never collapsed")
    void keepsFramesWithSeveralChildren() {
        CallNode root = call("OrderService", "placeOrder").onTarget(1).nanos(0, 5_000_000)
                .calling(call("OrderService", "placeOrder").onTarget(2).nanos(100, 2_000_000),
                        call("OrderService", "placeOrder").onTarget(3).nanos(2_000_100, 4_999_000))
                .build();

        assertThat(ProxyFrameCollapser.collapse(root).collapsedProxyHops()).isZero();
    }

    @Test
    @DisplayName("proxies deeper in the chain are collapsed too")
    void collapsesNestedProxyFrames() {
        CallNode root = call("OrderService", "placeOrder").onTarget(1).nanos(0, 9_000_000)
                .calling(call("PricingService", "priceOf").onTarget(2).nanos(1_000, 5_000_000)
                        .calling(call("PricingService", "priceOf").onTarget(3).nanos(2_000, 4_999_000)))
                .build();

        CallNode collapsed = ProxyFrameCollapser.collapse(root);

        assertThat(collapsed.collapsedProxyHops()).isZero();
        assertThat(collapsed.children()).singleElement()
                .satisfies(child -> {
                    assertThat(child.collapsedProxyHops()).isOne();
                    assertThat(child.children()).isEmpty();
                });
    }
}
