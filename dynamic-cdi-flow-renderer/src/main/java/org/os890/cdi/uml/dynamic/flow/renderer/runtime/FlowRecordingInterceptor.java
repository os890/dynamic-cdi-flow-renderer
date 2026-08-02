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

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowRecorded;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Records every public method-call of every instrumented bean.
 * <p>
 * The extension registers this interceptor programmatically and {@link Priority} enables it
 * globally, so no {@code beans.xml} entry is needed - on Weld and on OpenWebBeans alike.
 */
@Interceptor
@FlowRecorded
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class FlowRecordingInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object record(InvocationContext invocationContext) throws Exception {
        FlowRuntime runtime = FlowRuntime.active();
        Method method = invocationContext.getMethod();

        if (runtime == null || !isRecordable(method)) {
            return invocationContext.proceed();
        }

        FlowContext flowContext = FlowContext.current();
        if (flowContext.isSuspended()) {
            return invocationContext.proceed();
        }

        Object target = invocationContext.getTarget();
        CallNode node = new CallNode(
                ProxyNames.unproxy(target, method),
                method,
                System.identityHashCode(target),
                runtime.isObserverMethod(method));

        flowContext.push(node);
        node.started(System.currentTimeMillis(), System.nanoTime());
        try {
            return invocationContext.proceed();
        } catch (Throwable throwable) {
            node.failedWith(throwable);
            throw throwable;
        } finally {
            node.finished(System.currentTimeMillis(), System.nanoTime());
            if (flowContext.pop()) {
                flowContext.publish(runtime);
            }
        }
    }

    /**
     * Only public business-methods are recorded. A class-level interceptor-binding would also cover
     * protected and package-private methods, and the methods inherited from {@link Object} are just
     * noise in a sequence-diagram.
     */
    private static boolean isRecordable(Method method) {
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)) {
            return false;
        }
        if (method.getDeclaringClass() == Object.class) {
            return false;
        }

        String name = method.getName();
        int parameterCount = method.getParameterCount();
        if (parameterCount == 0 && ("toString".equals(name) || "hashCode".equals(name))) {
            return false;
        }
        return !(parameterCount == 1 && "equals".equals(name) && method.getParameterTypes()[0] == Object.class);
    }
}
