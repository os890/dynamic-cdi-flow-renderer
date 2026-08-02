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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.os890.cdi.uml.dynamic.flow.renderer.testsupport.CallNodeBuilder.call;

class HotspotDetectorTest {

    private static final long THRESHOLD_MS = 50;

    private static CallNodeBuilder lasting(String bean, String method, long millis) {
        return call(bean, method).nanos(0, millis * 1_000_000L);
    }

    private static List<String> hotspotsOf(CallNode root) {
        HotspotDetector.markHotspots(root, THRESHOLD_MS);
        List<String> marked = new ArrayList<>();
        collect(root, marked);
        return marked;
    }

    private static void collect(CallNode node, List<String> target) {
        if (node.isHotspot()) {
            target.add(node.beanSimpleName() + "#" + node.methodName());
        }
        node.children().forEach(child -> collect(child, target));
    }

    @Test
    @DisplayName("only the innermost slow call of the branch is marked, never its slow callers")
    void marksTheInnermostSlowCall() {
        CallNode root = lasting("OrderService", "placeOrder", 100)
                .calling(lasting("PricingService", "priceOf", 95)
                                .calling(lasting("TaxService", "taxFor", 90)
                                        .calling(lasting("RoundingService", "round", 88))),
                        lasting("InventoryService", "reserve", 2),
                        lasting("AuditService", "log", 1))
                .build();

        assertThat(hotspotsOf(root)).containsExactly("RoundingService#round");
    }

    @Test
    @DisplayName("the outermost call is never marked - it is slow by construction")
    void neverMarksTheOutermostCall() {
        CallNode root = lasting("OrderService", "placeOrder", 100)
                .calling(lasting("PricingService", "priceOf", 90))
                .build();

        assertThat(hotspotsOf(root)).containsExactly("PricingService#priceOf");
        assertThat(root.isHotspot()).isFalse();
    }

    @Test
    @DisplayName("two independently slow branches each get their own marker")
    void marksEveryBranch() {
        CallNode root = lasting("Facade", "run", 200)
                .calling(lasting("SlowA", "a", 90).calling(lasting("FastA", "inner", 1)),
                        lasting("SlowB", "b", 95),
                        lasting("Fast", "quick", 3))
                .build();

        assertThat(hotspotsOf(root)).containsExactlyInAnyOrder("SlowA#a", "SlowB#b");
    }

    @Test
    @DisplayName("a slow frame whose child is slow too hands the marker down")
    void handsTheMarkerDown() {
        CallNode root = lasting("Facade", "run", 200)
                .calling(lasting("Middle", "m", 150)
                        .calling(lasting("Leaf", "l", 120)))
                .build();

        assertThat(hotspotsOf(root)).containsExactly("Leaf#l");
    }

    @Test
    @DisplayName("nothing is marked when every call is fast enough")
    void marksNothingBelowTheThreshold() {
        CallNode root = lasting("Facade", "run", 40)
                .calling(lasting("Helper", "help", 20))
                .build();

        assertThat(hotspotsOf(root)).isEmpty();
    }

    @Test
    @DisplayName("a call has to be strictly longer than the threshold")
    void appliesTheThresholdStrictly() {
        CallNode exactly = lasting("Facade", "run", 500)
                .calling(lasting("Helper", "help", THRESHOLD_MS))
                .build();
        CallNode justOver = lasting("Facade", "run", 500)
                .calling(lasting("Helper", "help", THRESHOLD_MS + 1))
                .build();

        assertThat(hotspotsOf(exactly)).isEmpty();
        assertThat(hotspotsOf(justOver)).containsExactly("Helper#help");
    }

    @Test
    @DisplayName("a threshold of zero or less switches the detection off")
    void staysOffWithoutAThreshold() {
        CallNode root = lasting("Facade", "run", 500)
                .calling(lasting("Helper", "help", 400))
                .build();

        HotspotDetector.markHotspots(root, 0);
        assertThat(root.children().get(0).isHotspot()).isFalse();

        HotspotDetector.markHotspots(root, -1);
        assertThat(root.children().get(0).isHotspot()).isFalse();
    }

    @Test
    @DisplayName("an outermost call which is slow all by itself yields no marker at all")
    void reportsNothingWhenOnlyTheOutermostCallIsSlow() {
        CallNode root = lasting("SelfSlowService", "workOnItsOwn", 300)
                .calling(lasting("Helper", "quick", 2))
                .build();

        //a documented consequence of never marking the outermost call: there is no inner
        //frame to point at, so the diagram stays unannotated
        assertThat(hotspotsOf(root)).isEmpty();
    }
}
