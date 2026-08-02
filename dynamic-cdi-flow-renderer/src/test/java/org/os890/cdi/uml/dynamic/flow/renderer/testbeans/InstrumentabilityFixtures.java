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

package org.os890.cdi.uml.dynamic.flow.renderer.testbeans;

import jakarta.decorator.Decorator;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.interceptor.Interceptor;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;

/**
 * Bean-shapes for the instrumentability-rules.
 * <p>
 * Deliberately outside of {@code org.os890.cdi.uml.dynamic.flow.renderer.extension}: the addon excludes its own
 * packages, so fixtures living there would all be rejected for the wrong reason.
 */
public final class InstrumentabilityFixtures {

    private InstrumentabilityFixtures() {
    }

    public static class PlainBean {
        public void run() {
        }
    }

    public static final class FinalBean {
        public void run() {
        }
    }

    public static class BeanWithFinalMethod {
        public final void run() {
        }
    }

    public static class BeanWithFinalMethodInSuperclass extends BeanWithFinalMethod {
    }

    public static class BeanWithFinalStaticMethod {
        public static void run() {
        }
    }

    public static class BeanWithPrivateConstructorOnly {
        private BeanWithPrivateConstructorOnly() {
        }

        public void run() {
        }
    }

    public abstract static class AbstractBean {
        public abstract void run();
    }

    public interface SomeInterface {
    }

    public enum SomeEnum {
        VALUE
    }

    public record SomeRecord(String value) {
    }

    public static class Outer {
        public class NonStaticInnerBean {
            public void run() {
            }
        }
    }

    @Interceptor
    public static class SomeInterceptor {
    }

    @Decorator
    public abstract static class SomeDecorator {
    }

    public static class SomeExtension implements Extension {
    }

    public static class SomeSink implements FlowSink {
        @Override
        public void onFlowRecorded(CallFlow flow) {
        }
    }
}
