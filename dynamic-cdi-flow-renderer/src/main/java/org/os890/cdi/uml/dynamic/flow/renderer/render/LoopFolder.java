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

import java.util.ArrayList;
import java.util.List;

/**
 * Folds consecutive sibling-calls which describe the very same sub-tree into one repetition, so a
 * bean-method called inside a loop renders as a single Mermaid {@code loop N times} block instead
 * of N identical message-pairs.
 */
public final class LoopFolder {

    /**
     * @param node  the first call of the run - the one which gets rendered
     * @param count how often it occurred in a row
     */
    public record Repetition(CallNode node, int count) {
    }

    private LoopFolder() {
    }

    public static List<Repetition> fold(List<CallNode> children, boolean enabled) {
        List<Repetition> result = new ArrayList<>(children.size());
        if (children.isEmpty()) {
            return result;
        }
        if (!enabled) {
            children.forEach(child -> result.add(new Repetition(child, 1)));
            return result;
        }

        CallNode runStart = children.get(0);
        String runSignature = signatureOf(runStart);
        int runLength = 1;

        for (int i = 1; i < children.size(); i++) {
            CallNode candidate = children.get(i);
            String candidateSignature = signatureOf(candidate);
            if (candidateSignature.equals(runSignature)) {
                runLength++;
                continue;
            }
            result.add(new Repetition(runStart, runLength));
            runStart = candidate;
            runSignature = candidateSignature;
            runLength = 1;
        }
        result.add(new Repetition(runStart, runLength));
        return result;
    }

    /**
     * Structural fingerprint of a sub-tree: bean, method-signature, outcome and - recursively - the
     * same for everything it called. Two calls only fold together when they did exactly the same.
     */
    public static String signatureOf(CallNode node) {
        StringBuilder signature = new StringBuilder();
        appendSignature(node, signature);
        return signature.toString();
    }

    private static void appendSignature(CallNode node, StringBuilder target) {
        target.append(node.beanClassName()).append('#').append(node.signature());
        if (node.isObserverMethod()) {
            target.append("@event");
        }
        if (node.hasFailed()) {
            target.append("!").append(node.thrownTypeName());
        }
        target.append('{');
        for (CallNode child : node.children()) {
            appendSignature(child, target);
            target.append(';');
        }
        target.append('}');
    }
}
