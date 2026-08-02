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

package org.os890.cdi.uml.dynamic.flow.renderer.testsupport;

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds recorded call-trees without booting a CDI container, so the renderer and the normalizers
 * can be tested in isolation.
 */
public final class CallNodeBuilder {

    private static final AtomicInteger NEXT_TARGET_IDENTITY = new AtomicInteger(1);

    private String packageName = "org.example";
    private final String simpleName;
    private final String methodName;
    private List<String> parameterTypeNames = List.of();
    private String returnTypeName = "void";
    private int targetIdentity = NEXT_TARGET_IDENTITY.incrementAndGet();
    private boolean observerMethod;
    private long startNanos;
    private long endNanos = 5_000_000L;
    private long startEpochMillis = 1_754_120_832_345L;
    private long endEpochMillis = 1_754_120_832_350L;
    private Throwable thrown;
    private final List<CallNodeBuilder> children = new ArrayList<>();

    private CallNodeBuilder(String simpleName, String methodName) {
        this.simpleName = simpleName;
        this.methodName = methodName;
    }

    public static CallNodeBuilder call(String simpleName, String methodName) {
        return new CallNodeBuilder(simpleName, methodName);
    }

    public CallNodeBuilder inPackage(String value) {
        this.packageName = value;
        return this;
    }

    public CallNodeBuilder params(String... types) {
        this.parameterTypeNames = List.of(types);
        return this;
    }

    public CallNodeBuilder returning(String type) {
        this.returnTypeName = type;
        return this;
    }

    public CallNodeBuilder asObserver() {
        this.observerMethod = true;
        return this;
    }

    public CallNodeBuilder onTarget(int identity) {
        this.targetIdentity = identity;
        return this;
    }

    public CallNodeBuilder nanos(long start, long end) {
        this.startNanos = start;
        this.endNanos = end;
        return this;
    }

    public CallNodeBuilder epochMillis(long start, long end) {
        this.startEpochMillis = start;
        this.endEpochMillis = end;
        return this;
    }

    public CallNodeBuilder throwing(Throwable value) {
        this.thrown = value;
        return this;
    }

    public CallNodeBuilder calling(CallNodeBuilder... nested) {
        this.children.addAll(Arrays.asList(nested));
        return this;
    }

    public CallNode build() {
        CallNode node = new CallNode(packageName + "." + simpleName, simpleName, methodName,
                parameterTypeNames, returnTypeName, targetIdentity, observerMethod);
        node.started(startEpochMillis, startNanos);
        node.finished(endEpochMillis, endNanos);
        if (thrown != null) {
            node.failedWith(thrown);
        }
        children.forEach(child -> node.addChild(child.build()));
        return node;
    }

    public CallFlow buildFlow(FlowConfig config) {
        return new CallFlow(build(), "main", config);
    }

    public CallFlow buildFlow() {
        return buildFlow(FlowConfig.builder().build());
    }
}
