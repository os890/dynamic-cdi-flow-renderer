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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static registry of {@link FlowSink}s.
 * <p>
 * Static on purpose: sinks have to be registrable before the container boots and have to survive
 * the several container life-cycles a test-suite goes through inside one JVM.
 */
public final class FlowSinks {

    private static final List<FlowSink> SINKS = new CopyOnWriteArrayList<>();

    private FlowSinks() {
    }

    public static void register(FlowSink sink) {
        if (sink != null && !SINKS.contains(sink)) {
            SINKS.add(sink);
        }
    }

    public static void unregister(FlowSink sink) {
        SINKS.remove(sink);
    }

    public static List<FlowSink> all() {
        return List.copyOf(SINKS);
    }

    public static void clear() {
        SINKS.clear();
    }
}
