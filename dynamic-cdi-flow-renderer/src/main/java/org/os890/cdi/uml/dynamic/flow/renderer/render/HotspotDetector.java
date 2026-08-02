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

/**
 * Marks the calls which took longer than the configured threshold - but only the ones worth
 * looking at.
 * <p>
 * Marking every slow frame would mark the whole path down to the slow call, because a caller is
 * slow whenever its callee is. Two rules cut that down to the interesting frames:
 * <ul>
 *     <li>the outermost call is never marked - it contains everything, so it is slow by
 *     construction and says nothing about where the time went</li>
 *     <li>a slow frame is only marked when nothing below it is slow as well - the marker therefore
 *     sits on the innermost still-slow call of each branch, which is the one to look at</li>
 * </ul>
 * A branch which is slow for two independent reasons gets a marker per reason, since each of them
 * is the innermost slow call of its own branch.
 */
public final class HotspotDetector {

    private HotspotDetector() {
    }

    /**
     * @param thresholdMillis a call has to take strictly longer than this to count as slow
     */
    public static CallNode markHotspots(CallNode root, long thresholdMillis) {
        if (thresholdMillis <= 0) {
            return root;
        }
        mark(root, thresholdMillis * 1_000_000L, true);
        return root;
    }

    /**
     * @return {@code true} when this frame or anything below it is slow
     */
    private static boolean mark(CallNode node, long thresholdNanos, boolean outermost) {
        boolean slowBelow = false;
        for (CallNode child : node.children()) {
            //no short-circuiting - every branch has to be visited to get its own marker
            slowBelow |= mark(child, thresholdNanos, false);
        }

        boolean slow = node.durationNanos() > thresholdNanos;
        if (slow && !slowBelow && !outermost) {
            node.markAsHotspot();
        }
        return slow || slowBelow;
    }
}
