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

package org.os890.cdi.uml.dynamic.flow.renderer.api;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One recorded invocation of a public bean-method, plus the invocations it triggered.
 * <p>
 * Nothing here keeps a reference to the bean-instance or to the arguments: the target is reduced
 * to its identity-hash and every type is reduced to its name. A recorded flow can therefore be
 * held on to indefinitely without retaining application state.
 */
public final class CallNode {

    private final String beanClassName;
    private final String beanSimpleName;
    private final String methodName;
    private final List<String> parameterTypeNames;
    private final String returnTypeName;
    private final int targetIdentity;
    private final boolean observerMethod;

    private final List<CallNode> children = new ArrayList<>(2);

    private long startedAtEpochMillis;
    private long finishedAtEpochMillis;
    private long startedAtNanos;
    private long finishedAtNanos;
    private String thrownTypeName;
    private int collapsedProxyHops;
    private boolean hotspot;

    public CallNode(Class<?> beanClass, Method method, int targetIdentity, boolean observerMethod) {
        this.beanClassName = beanClass.getName();
        this.beanSimpleName = simpleNameOf(beanClass);
        this.methodName = method.getName();
        this.returnTypeName = typeNameOf(method.getReturnType());
        this.targetIdentity = targetIdentity;
        this.observerMethod = observerMethod;

        List<String> parameters = new ArrayList<>(method.getParameterCount());
        for (Class<?> parameterType : method.getParameterTypes()) {
            parameters.add(typeNameOf(parameterType));
        }
        this.parameterTypeNames = Collections.unmodifiableList(parameters);
    }

    /**
     * Test-only constructor which allows building a tree without a real invocation.
     */
    public CallNode(String beanClassName, String beanSimpleName, String methodName,
                    List<String> parameterTypeNames, String returnTypeName,
                    int targetIdentity, boolean observerMethod) {
        this.beanClassName = beanClassName;
        this.beanSimpleName = beanSimpleName;
        this.methodName = methodName;
        this.parameterTypeNames = List.copyOf(parameterTypeNames);
        this.returnTypeName = returnTypeName;
        this.targetIdentity = targetIdentity;
        this.observerMethod = observerMethod;
    }

    private static String simpleNameOf(Class<?> beanClass) {
        Class<?> enclosingClass = beanClass.getEnclosingClass();
        if (enclosingClass != null) {
            return enclosingClass.getSimpleName() + "." + beanClass.getSimpleName();
        }
        return beanClass.getSimpleName();
    }

    private static String typeNameOf(Class<?> type) {
        if (type.isArray()) {
            return typeNameOf(type.getComponentType()) + "[]";
        }
        return simpleNameOf(type);
    }

    public void started(long epochMillis, long nanos) {
        this.startedAtEpochMillis = epochMillis;
        this.startedAtNanos = nanos;
    }

    public void finished(long epochMillis, long nanos) {
        this.finishedAtEpochMillis = epochMillis;
        this.finishedAtNanos = nanos;
    }

    public void failedWith(Throwable throwable) {
        this.thrownTypeName = throwable.getClass().getSimpleName();
    }

    public void addChild(CallNode child) {
        children.add(child);
    }

    public void replaceChildren(List<CallNode> replacement) {
        children.clear();
        children.addAll(replacement);
    }

    public void collapsedProxyHop() {
        collapsedProxyHops++;
    }

    public void markAsHotspot() {
        this.hotspot = true;
    }

    /**
     * @return {@code true} when this call took longer than the configured hotspot-threshold and is
     * the innermost such call of its branch - see {@code HotspotDetector}
     */
    public boolean isHotspot() {
        return hotspot;
    }

    public String beanClassName() {
        return beanClassName;
    }

    public String beanSimpleName() {
        return beanSimpleName;
    }

    public String methodName() {
        return methodName;
    }

    public List<String> parameterTypeNames() {
        return parameterTypeNames;
    }

    public String returnTypeName() {
        return returnTypeName;
    }

    public int targetIdentity() {
        return targetIdentity;
    }

    public boolean isObserverMethod() {
        return observerMethod;
    }

    public List<CallNode> children() {
        return Collections.unmodifiableList(children);
    }

    public long startedAtEpochMillis() {
        return startedAtEpochMillis;
    }

    public long finishedAtEpochMillis() {
        return finishedAtEpochMillis;
    }

    public long startedAtNanos() {
        return startedAtNanos;
    }

    public long finishedAtNanos() {
        return finishedAtNanos;
    }

    public long durationNanos() {
        return finishedAtNanos - startedAtNanos;
    }

    public boolean hasFailed() {
        return thrownTypeName != null;
    }

    public String thrownTypeName() {
        return thrownTypeName;
    }

    public int collapsedProxyHops() {
        return collapsedProxyHops;
    }

    /**
     * {@code methodName(ParamType, ParamType)} - the label used in the diagram and the key used
     * to decide whether two frames describe the same invocation.
     */
    public String signature() {
        return methodName + "(" + String.join(", ", parameterTypeNames) + ")";
    }

    @Override
    public String toString() {
        return beanSimpleName + "#" + signature();
    }
}
