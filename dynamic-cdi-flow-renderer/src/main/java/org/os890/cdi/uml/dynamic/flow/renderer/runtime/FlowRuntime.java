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
import java.util.List;
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

    /**
     * Set as soon as a container-managed lifecycle takes charge of arming and disarming - the
     * portable extension, or the Quarkus extension. Self-arming then stays out of the way, so
     * "the container shut the recorder down" keeps meaning what it says.
     */
    private static volatile boolean containerManaged;

    /** so a disabled or unusable configuration is read once, not on every intercepted call */
    private static volatile boolean selfArmingRuledOut;

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

    /**
     * Arms the recorder with the sink the configuration asks for, which is what nearly every
     * caller wants - only a test replacing the built-in sink needs the two-argument variant.
     */
    public static FlowRuntime activate(FlowConfig config) {
        return activate(config, BuiltInSink.of(config));
    }

    /**
     * Announced by whoever arms the recorder from a container lifecycle, before it arms it.
     */
    public static void markContainerManaged() {
        containerManaged = true;
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
        FlowRuntime runtime = active;
        if (runtime != null || containerManaged || selfArmingRuledOut) {
            return runtime;
        }
        return selfArm();
    }

    /**
     * Arms the recorder on the first intercepted call when nothing else did.
     *
     * <p>The portable extension arms it from {@code AfterDeploymentValidation}, which a container
     * that does not run portable extensions - ArC, above all - never reaches. The interceptor would
     * then silently do nothing at all: it is on the beans, the binding is right, and no diagram is
     * ever written. Arming from the first call instead means the binding is the only thing an
     * integration has to arrange.
     */
    private static synchronized FlowRuntime selfArm() {
        if (active != null || containerManaged || selfArmingRuledOut) {
            return active;
        }
        FlowConfig config;
        try {
            config = FlowConfig.load();
        } catch (RuntimeException e) {
            //an unreadable configuration must not break the call which happened to be first
            selfArmingRuledOut = true;
            LOGGER.log(Level.WARNING, e, () -> "cdi-flow could not read its configuration");
            return null;
        }
        if (!config.isEnabled()) {
            selfArmingRuledOut = true;
            return null;
        }
        FlowRuntime runtime = activate(config);
        LOGGER.info(() -> "cdi-flow armed itself on the first recorded call; "
                + config.outputFormat().name().toLowerCase(java.util.Locale.ROOT)
                + " diagrams are written to " + config.outputDirectory().toAbsolutePath());
        return runtime;
    }

    /**
     * Forgets that a configuration was once found unusable - for a test-suite booting several
     * containers in one JVM, where a later container may well be configured differently.
     */
    public static void reset() {
        deactivate();
        containerManaged = false;
        selfArmingRuledOut = false;
    }

    public FlowConfig config() {
        return config;
    }

    public void registerObserverMethod(Class<?> declaringClass, String methodName, Class<?>[] parameterTypes) {
        observerMethods.add(observerKey(declaringClass.getName(), methodName, parameterTypes));
    }

    /**
     * Registers an observer-method known by name rather than by class.
     *
     * <p>For a container which resolves its observers while the application is built there is no
     * {@code ProcessObserverMethod} to listen to and no loaded class to reflect on - only the names
     * an index holds. Without this, an event would be drawn as an ordinary call.
     *
     * @param key as built by {@link #observerKey(String, String, List)}
     */
    public void registerObserverMethod(String key) {
        observerMethods.add(key);
    }

    /**
     * The identity of an observer-method, for an integration which has to name one at build-time.
     */
    public static String observerKey(String declaringClassName, String methodName,
                                     List<String> parameterTypeNames) {
        StringBuilder key = new StringBuilder(declaringClassName).append('#').append(methodName).append('(');
        for (String parameterTypeName : parameterTypeNames) {
            key.append(parameterTypeName).append(',');
        }
        return key.append(')').toString();
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
