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
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.OrderService;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.support.CdiFlowTestContainer;
import org.os890.cdi.uml.dynamic.flow.renderer.sink.DiagramFileNamer;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code <entry class>_<entry method>_<start>_<end>.mmd}
 */
class DiagramFileNamingIT {

    private static final Pattern FILE_NAME =
            Pattern.compile("(\\w+)_(\\w+)_(\\d{8}-\\d{9})_(\\d{8}-\\d{9})(-\\d+)?\\.mmd");

    private static CdiFlowTestContainer container;

    @BeforeAll
    static void startContainer() {
        container = CdiFlowTestContainer.startFor(DiagramFileNamingIT.class);
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

    @Test
    @DisplayName("the name is built from the entry-bean, the entry-method and both timestamps")
    void followsTheNamingScheme() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);
        CallFlow flow = container.flows().singleEnteredAt(OrderService.class);

        Path file = container.writtenDiagrams().get(0);
        Matcher name = FILE_NAME.matcher(file.getFileName().toString());

        assertThat(name.matches()).as("file-name '%s'", file.getFileName()).isTrue();
        assertThat(name.group(1)).as("simple class-name of the entry-point").isEqualTo("OrderService");
        assertThat(name.group(2)).as("first public method, without parameters").isEqualTo("placeOrder");
        assertThat(name.group(3)).isEqualTo(DiagramFileNamer.timestamp(flow.startedAtEpochMillis()));
        assertThat(name.group(4)).isEqualTo(DiagramFileNamer.timestamp(flow.finishedAtEpochMillis()));
    }

    @Test
    @DisplayName("the end-timestamp is never before the start-timestamp")
    void keepsTheTimestampOrder() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);

        Matcher name = FILE_NAME.matcher(container.writtenDiagrams().get(0).getFileName().toString());

        assertThat(name.matches()).isTrue();
        assertThat(name.group(4)).isGreaterThanOrEqualTo(name.group(3));
    }

    @Test
    @DisplayName("the nested beans do not appear in the file-name - only the entry-point does")
    void namesTheEntryPointOnly() {
        container.get(OrderService.class).placeOrder("SKU-1", 2);

        assertThat(container.writtenDiagrams().get(0).getFileName().toString())
                .doesNotContain("PricingService")
                .doesNotContain("AuditService");
    }

    @Test
    @DisplayName("flows finishing in the same millisecond each get their own file")
    void doesNotOverwriteFastRepeatedFlows() {
        for (int i = 0; i < 5; i++) {
            container.get(OrderService.class).placeOrder("SKU-" + i, 1);
        }

        List<Path> diagrams = container.writtenDiagrams();

        assertThat(diagrams).hasSize(5);
        assertThat(diagrams).extracting(path -> path.getFileName().toString())
                .doesNotHaveDuplicates()
                .allMatch(fileName -> FILE_NAME.matcher(fileName).matches());
    }
}
