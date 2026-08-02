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
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Small read-helpers so the assertions can talk about the shape of a recorded call-tree.
 */
public final class Flows {

    private Flows() {
    }

    /** pre-order list of {@code Bean#method(ParamTypes)} entries */
    public static List<String> flatten(CallFlow flow) {
        return flatten(flow.root());
    }

    public static List<String> flatten(CallNode root) {
        List<String> result = new ArrayList<>();
        collect(root, result);
        return result;
    }

    private static void collect(CallNode node, List<String> target) {
        target.add(node.beanSimpleName() + "#" + node.signature());
        node.children().forEach(child -> collect(child, target));
    }

    /** number of participating beans in a single straight line - a leaf has depth 1 */
    public static int depth(CallNode node) {
        return node.children().stream().mapToInt(Flows::depth).max().orElse(0) + 1;
    }

    public static int totalCallCount(CallNode node) {
        return 1 + node.children().stream().mapToInt(Flows::totalCallCount).sum();
    }

    /** follows the single-child chain as far as it goes */
    public static List<CallNode> straightChain(CallNode root) {
        List<CallNode> chain = new ArrayList<>();
        for (CallNode current = root; current != null; ) {
            chain.add(current);
            current = current.children().size() == 1 ? current.children().get(0) : null;
        }
        return chain;
    }

    public static List<String> childBeanNames(CallNode node) {
        return node.children().stream().map(CallNode::beanSimpleName).toList();
    }
}
