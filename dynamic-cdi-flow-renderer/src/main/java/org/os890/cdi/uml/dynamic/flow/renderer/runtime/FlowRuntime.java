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

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSinks;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds everything the interceptor needs at runtime: the effective configuration, the set of
 * observer-methods (so they can be drawn as event-arrows) and the built-in sink.
 * <p>
 * Static, because the interceptor must stay cheap and must not depend on injection - and because
 * a test-suite boots and shuts down several containers inside a single JVM.
 */
public final class FlowRuntime {

    private static final Logger LOGGER = Logger.getLogger(FlowRuntime.class.getName());

    private static volatile FlowRuntime active;

    private final FlowConfig config;
    private final Set<String> observerMethods = ConcurrentHashMap.newKeySet();
    private final FlowSink builtInSink;

    private FlowRuntime(FlowConfig config, FlowSink builtInSink) {
        this.config = config;
        this.builtInSink = builtInSink;
    }

    public static FlowRuntime activate(FlowConfig config, FlowSink builtInSink) {
        FlowRuntime runtime = new FlowRuntime(config, builtInSink);
        active = runtime;
        if (builtInSink != null) {
            FlowSinks.register(builtInSink);
        }
        return runtime;
    }

    public static void deactivate() {
        FlowRuntime runtime = active;
        active = null;
        if (runtime != null && runtime.builtInSink != null) {
            FlowSinks.unregister(runtime.builtInSink);
        }
        FlowContext.clear();
    }

    /**
     * @return the active runtime or {@code null} when the extension is disabled or no container
     * is running - the interceptor then degrades to a pass-through
     */
    public static FlowRuntime active() {
        return active;
    }

    public FlowConfig config() {
        return config;
    }

    public void registerObserverMethod(Class<?> declaringClass, String methodName, Class<?>[] parameterTypes) {
        observerMethods.add(observerKey(declaringClass.getName(), methodName, parameterTypes));
    }

    public boolean isObserverMethod(Method method) {
        return observerMethods.contains(observerKey(method.getDeclaringClass().getName(),
                method.getName(), method.getParameterTypes()));
    }

    private static String observerKey(String declaringClassName, String methodName, Class<?>[] parameterTypes) {
        StringBuilder key = new StringBuilder(declaringClassName).append('#').append(methodName).append('(');
        for (Class<?> parameterType : parameterTypes) {
            key.append(parameterType.getName()).append(',');
        }
        return key.append(')').toString();
    }

    void publish(CallFlow flow) {
        for (FlowSink sink : FlowSinks.all()) {
            try {
                sink.onFlowRecorded(flow);
            } catch (Throwable t) {
                //a broken sink must never break the business-call which triggered the recording
                LOGGER.log(Level.WARNING, t, () -> "flow-sink " + sink.getClass().getName() + " failed");
            }
        }
    }
}
