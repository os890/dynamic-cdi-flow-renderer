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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowLabelTest {

    @AfterEach
    void clearLabel() {
        FlowLabel.clear();
    }

    @Test
    @DisplayName("a label is held per thread and read back with its description")
    void heldPerThread() throws InterruptedException {
        FlowLabel.set("a contact is deleted", "Deleting a contact asks first.");

        assertThat(FlowLabel.current().name()).isEqualTo("a contact is deleted");
        assertThat(FlowLabel.current().description()).isEqualTo("Deleting a contact asks first.");

        Thread other = new Thread(() -> assertThat(FlowLabel.current()).isNull());
        other.start();
        other.join();
    }

    @Test
    @DisplayName("a blank name clears the label, so a caller need not special-case 'no label'")
    void blankClears() {
        FlowLabel.set("something");
        FlowLabel.set("   ");

        assertThat(FlowLabel.current()).isNull();
        assertThat(FlowLabel.of(null, "described")).isNull();
    }

    @Test
    @DisplayName("a blank description is no description at all")
    void blankDescription() {
        FlowLabel.set("a use-case", "  ");

        assertThat(FlowLabel.current().description()).isNull();
    }

    @Test
    @DisplayName("the directory-name is the label, lower-case, one hyphen per run of anything else")
    void directoryName() {
        assertThat(FlowLabel.of("A Contact Is Deleted", null).directoryName())
                .isEqualTo("a-contact-is-deleted");
        assertThat(FlowLabel.of("deals-and-tasks.spec.ts › a deal is won!", null).directoryName())
                .isEqualTo("deals-and-tasks-spec-ts-a-deal-is-won");
        assertThat(FlowLabel.of("...", null).directoryName()).isEqualTo("unlabelled");
    }

    @Test
    @DisplayName("a very long label is cut, because a file-system may refuse it")
    void longNamesAreCut() {
        String name = "a use-case with a title which goes on and on and on and simply does not stop";

        assertThat(FlowLabel.of(name, null).directoryName()).hasSize(70);
    }
}
