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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.support;

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Keeps every recorded flow in memory so the tests can assert on the model as well as on the
 * rendered diagram, without having to read files back.
 */
public final class CapturingFlowSink implements FlowSink {

    private final List<CallFlow> flows = new CopyOnWriteArrayList<>();

    @Override
    public void onFlowRecorded(CallFlow flow) {
        flows.add(flow);
    }

    public void clear() {
        flows.clear();
    }

    public List<CallFlow> all() {
        return List.copyOf(flows);
    }

    public List<CallFlow> enteredAt(Class<?> entryBean) {
        return flows.stream()
                .filter(flow -> flow.entryTypeSimpleName().equals(entryBean.getSimpleName()))
                .toList();
    }

    public CallFlow single() {
        assertThat(flows).as("exactly one recorded flow was expected").hasSize(1);
        return flows.get(0);
    }

    public CallFlow singleEnteredAt(Class<?> entryBean) {
        List<CallFlow> matching = enteredAt(entryBean);
        assertThat(matching).as("exactly one flow entered at %s was expected, recorded: %s",
                entryBean.getSimpleName(), flows).hasSize(1);
        return matching.get(0);
    }

    public String singleDiagram() {
        return single().toMermaid();
    }

    /**
     * Asynchronous events are delivered on a container-managed thread, so the flow they produce
     * arrives after the firing method already returned.
     */
    public CallFlow awaitSingleEnteredAt(Class<?> entryBean, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            List<CallFlow> matching = enteredAt(entryBean);
            if (!matching.isEmpty()) {
                return matching.get(0);
            }
            Thread.onSpinWait();
        }
        return fail("no flow entered at %s arrived within %s, recorded: %s",
                entryBean.getSimpleName(), timeout, flows);
    }
}
