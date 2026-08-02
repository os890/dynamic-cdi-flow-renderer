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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallFlow;
import org.os890.cdi.uml.dynamic.flow.renderer.api.CallNode;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.hotspots.ReportService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.hotspots.SelfSlowService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.hotspots.TwoBranchService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.testsupport.MermaidAssertions;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The container boots on this example's own {@code META-INF/microprofile-config.properties}, which
 * sets {@code cdi-flow.hotspot-threshold-ms=50}.
 */
class HotspotConfigIT {

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(HotspotConfigIT.class);
    }

    @AfterAll
    static void stopContainer() {
        container.close();
    }

    @BeforeEach
    void forgetPreviousFlows() {
        container.flows().clear();
        container.clearWrittenDiagrams();
    }

    private static List<String> hotspotsOf(CallFlow flow) {
        List<String> marked = new ArrayList<>();
        collect(flow.root(), marked);
        return marked;
    }

    private static void collect(CallNode node, List<String> target) {
        if (node.isHotspot()) {
            target.add(node.beanSimpleName() + "#" + node.methodName());
        }
        node.children().forEach(child -> collect(child, target));
    }

    @Test
    @DisplayName("the threshold is read from the example's config-file")
    void readsTheThresholdFromTheConfigFile() {
        container.get(ReportService.class).buildReport("sales");

        assertThat(container.flows().singleEnteredAt(ReportService.class).config())
                .satisfies(config -> {
                    assertThat(config.hotspotThresholdMillis()).isEqualTo(50);
                    assertThat(config.isHotspotDetectionEnabled()).isTrue();
                });
    }

    @Test
    @DisplayName("the marker sits on the innermost slow call, not on its slow callers")
    void marksTheInnermostSlowCall() {
        container.get(ReportService.class).buildReport("sales");

        CallFlow flow = container.flows().singleEnteredAt(ReportService.class);

        //ReportService and ReportAssemblyService are slow too - but only because of the repository
        assertThat(hotspotsOf(flow)).containsExactly("ReportRepository#loadRows");
        assertThat(flow.root().isHotspot()).as("the outermost call is never marked").isFalse();
    }

    @Test
    @DisplayName("the diagram carries the hint as a note on the slow participant")
    void rendersTheHintIntoTheDiagram() {
        container.get(ReportService.class).buildReport("sales");

        String diagram = container.flows().singleEnteredAt(ReportService.class).toDiagram();

        assertThat(diagram).containsPattern(
                "Note over ReportRepository: HOTSPOT ReportRepository\\.loadRows took [\\d.]+ ms \\(over 50 ms\\)");
        assertThat(MermaidAssertions.countOccurrences(diagram, "HOTSPOT")).isOne();
        MermaidAssertions.assertWellFormed(diagram);
        MermaidAssertions.assertFreeOfProxyNames(diagram);
    }

    @Test
    @DisplayName("two independently slow branches each get their own marker")
    void marksEveryBranch() {
        container.get(TwoBranchService.class).runBothBranches();

        CallFlow flow = container.flows().singleEnteredAt(TwoBranchService.class);

        assertThat(hotspotsOf(flow))
                .containsExactlyInAnyOrder("ReportRepository#loadRows", "SlowExportService#export");
        assertThat(MermaidAssertions.countOccurrences(flow.toDiagram(), "HOTSPOT")).isEqualTo(2);
    }

    @Test
    @DisplayName("a flow which is fast enough is not annotated at all")
    void leavesFastFlowsAlone() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);

        CallFlow flow = container.flows().singleEnteredAt(OrderService.class);

        assertThat(hotspotsOf(flow)).isEmpty();
        assertThat(flow.toDiagram()).doesNotContain("HOTSPOT");
    }

    @Test
    @DisplayName("a call slow all by itself yields no marker - the outermost call is never marked")
    void reportsNothingWhenOnlyTheOutermostCallIsSlow() {
        container.get(SelfSlowService.class).workOnItsOwn();

        CallFlow flow = container.flows().singleEnteredAt(SelfSlowService.class);

        //documented consequence: there is no inner frame to point at
        assertThat(hotspotsOf(flow)).isEmpty();
        assertThat(flow.toDiagram()).doesNotContain("HOTSPOT");
    }
}
