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

package org.os890.cdi.uml.dynamic.flow.renderer.render;

import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;

import java.util.List;

/**
 * Removes frames which are nothing but a proxy handing the very same call on to the very same
 * bean-class.
 * <p>
 * The primary defence against duplicated entries is that container-generated classes never get the
 * interceptor-binding in the first place (see {@code Instrumentability}). This collapser is the
 * safety-net for setups where a proxy still ends up inside the recorded chain.
 * <p>
 * It must not swallow real recursion, which looks deceptively similar. The decisive difference is
 * the target: a proxy hop means <em>two different objects</em> for the same logical call, while
 * recursion re-enters the <em>same</em> instance. On top of that a pass-through does no work of its
 * own, so the child has to account for practically the whole duration of the parent.
 */
public final class ProxyFrameCollapser {

    /** a frame doing less than this much work of its own is a pass-through, not a real call */
    static final long MAX_PASS_THROUGH_NANOS = 1_000_000L;

    private ProxyFrameCollapser() {
    }

    public static CallNode collapse(CallNode root) {
        collapseRecursively(root);
        return root;
    }

    private static void collapseRecursively(CallNode node) {
        //a chain of two proxies (client-proxy plus interceptor-subclass) collapses in two rounds
        while (isProxyPassThrough(node)) {
            CallNode duplicate = node.children().get(0);
            node.replaceChildren(List.copyOf(duplicate.children()));
            node.collapsedProxyHop();
        }

        for (CallNode child : node.children()) {
            collapseRecursively(child);
        }
    }

    static boolean isProxyPassThrough(CallNode node) {
        List<CallNode> children = node.children();
        if (children.size() != 1) {
            return false;
        }

        CallNode child = children.get(0);
        return node.targetIdentity() != child.targetIdentity()
                && node.beanClassName().equals(child.beanClassName())
                && node.signature().equals(child.signature())
                && node.hasFailed() == child.hasFailed()
                && child.startedAtNanos() >= node.startedAtNanos()
                && child.finishedAtNanos() <= node.finishedAtNanos()
                && (node.durationNanos() - child.durationNanos()) < MAX_PASS_THROUGH_NANOS;
    }
}
