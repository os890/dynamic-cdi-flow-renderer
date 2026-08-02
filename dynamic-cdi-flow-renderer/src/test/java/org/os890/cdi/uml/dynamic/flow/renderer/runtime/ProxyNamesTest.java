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

package org.os890.cdi.uml.dynamic.flow.renderer.runtime;

import org.jboss.weld.fake.WeldConstructMarker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyNamesTest {

    static class OrderService {
        public String placeOrder() {
            return "ok";
        }
    }

    /** looks like an OpenWebBeans normal-scope proxy */
    static class OrderService$$OwbNormalScopeProxy extends OrderService {
    }

    /** looks like a Weld interceptor-subclass */
    static class OrderService$Proxy$_$$_WeldSubclass extends OrderService {
    }

    /** a proxy which is only recognizable by the marker-interface it implements */
    static class OrderServiceMarkedProxy extends OrderService implements WeldConstructMarker {
    }

    /** a proxy stacked on top of another proxy, as normal-scoped intercepted beans produce */
    static class OrderService$$Stacked extends OrderService$$OwbNormalScopeProxy {
    }

    /** an ordinary subclass an application-developer could have written */
    static class PremiumOrderService extends OrderService {
    }

    private static Method placeOrder() throws NoSuchMethodException {
        return OrderService.class.getMethod("placeOrder");
    }

    @Test
    @DisplayName("an OpenWebBeans proxy resolves to the bean-class")
    void unwrapsOpenWebBeansProxy() throws Exception {
        assertThat(ProxyNames.unproxy(new OrderService$$OwbNormalScopeProxy(), placeOrder()))
                .isEqualTo(OrderService.class);
    }

    @Test
    @DisplayName("a Weld subclass resolves to the bean-class")
    void unwrapsWeldSubclass() throws Exception {
        assertThat(ProxyNames.unproxy(new OrderService$Proxy$_$$_WeldSubclass(), placeOrder()))
                .isEqualTo(OrderService.class);
    }

    @Test
    @DisplayName("a proxy is also recognized by an implementation-specific marker-interface")
    void unwrapsProxyRecognizedByMarkerInterface() throws Exception {
        assertThat(ProxyNames.unproxy(new OrderServiceMarkedProxy(), placeOrder()))
                .isEqualTo(OrderService.class);
    }

    @Test
    @DisplayName("stacked proxies are unwrapped in one go")
    void unwrapsStackedProxies() throws Exception {
        assertThat(ProxyNames.unproxy(new OrderService$$Stacked(), placeOrder()))
                .isEqualTo(OrderService.class);
    }

    @Test
    @DisplayName("a hand-written subclass is kept - it is the bean the developer deployed")
    void keepsRegularSubclass() throws Exception {
        assertThat(ProxyNames.unproxy(new PremiumOrderService(), placeOrder()))
                .isEqualTo(PremiumOrderService.class);
    }

    @Test
    @DisplayName("without a target the declaring class of the method is used")
    void fallsBackToTheDeclaringClass() throws Exception {
        assertThat(ProxyNames.unproxy(null, placeOrder())).isEqualTo(OrderService.class);
    }

    @Test
    @DisplayName("a JDK proxy which only implements interfaces falls back to the declaring class")
    void fallsBackForInterfaceOnlyProxies() throws Exception {
        Object jdkProxy = java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{Runnable.class}, (p, m, a) -> null);

        assertThat(ProxyNames.unproxy(jdkProxy, placeOrder())).isEqualTo(OrderService.class);
    }

    @Test
    @DisplayName("a lambda is recognized as generated")
    void detectsGeneratedClasses() {
        Runnable lambda = () -> {
        };

        assertThat(ProxyNames.isGenerated(lambda.getClass())).isTrue();
        assertThat(ProxyNames.isGenerated(OrderService.class)).isFalse();
        assertThat(ProxyNames.isGenerated(PremiumOrderService.class)).isFalse();
    }
}
