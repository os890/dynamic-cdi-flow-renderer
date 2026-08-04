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

import java.util.Locale;
import java.util.Objects;

/**
 * The use-case a recorded flow belongs to.
 *
 * <p>A flow ends when its outermost call returns, so one browser-driven use-case produces a whole
 * series of them - one per request. A label ties that series together: whatever sets it before the
 * work starts (the JAX-RS filter of {@code cdi-flow-jaxrs} reading a header, or a test calling
 * {@link #set(String, String)} directly) decides which use-case the flows are recorded under, and
 * every flow started on that thread carries it.
 *
 * <p>It is held per thread and read when a flow <em>starts</em>, not when it is published: a flow
 * that outlives the label - an asynchronous observer, say - keeps the label it began with.
 */
public final class FlowLabel {

    private static final int MAX_DIRECTORY_NAME_LENGTH = 70;

    private static final ThreadLocal<FlowLabel> CURRENT = new ThreadLocal<>();

    private final String name;
    private final String description;

    private FlowLabel(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Labels every flow started on this thread from now on. A blank name clears the label again,
     * so a caller does not have to special-case "no label".
     */
    public static void set(String name, String description) {
        FlowLabel label = of(name, description);
        if (label == null) {
            clear();
            return;
        }
        CURRENT.set(label);
    }

    /**
     * @return a label to hand to a sink or a test directly, or {@code null} for a blank name
     */
    public static FlowLabel of(String name, String description) {
        return name == null || name.isBlank() ? null : new FlowLabel(name.strip(), blankToNull(description));
    }

    public static void set(String name) {
        set(name, null);
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * @return the label of the current thread, or {@code null} when nothing labelled it - which is
     * the normal case for an application nobody is recording use-cases of
     */
    public static FlowLabel current() {
        return CURRENT.get();
    }

    public String name() {
        return name;
    }

    /**
     * @return a sentence about the use-case, to be carried into the generated document, or
     * {@code null} when whoever set the label had nothing to say about it
     */
    public String description() {
        return description;
    }

    /**
     * The label as a directory-name: lower-case, one hyphen per run of anything else. Two labels
     * differing only in punctuation therefore share a directory, which is the lesser evil compared
     * to a name the file-system may refuse.
     */
    public String directoryName() {
        StringBuilder result = new StringBuilder(name.length());
        boolean pendingSeparator = false;
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (pendingSeparator && !result.isEmpty()) {
                    result.append('-');
                }
                result.append(c);
                pendingSeparator = false;
            } else {
                pendingSeparator = true;
            }
        }
        String directoryName = result.length() > MAX_DIRECTORY_NAME_LENGTH
                ? result.substring(0, MAX_DIRECTORY_NAME_LENGTH)
                : result.toString();
        return directoryName.isEmpty() ? "unlabelled" : directoryName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FlowLabel otherLabel)) {
            return false;
        }
        return name.equals(otherLabel.name) && Objects.equals(description, otherLabel.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }

    @Override
    public String toString() {
        return "FlowLabel[" + name + "]";
    }
}
