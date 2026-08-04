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

package org.os890.cdi.uml.dynamic.flow.renderer.extension;

import java.util.Optional;

/**
 * The questions {@link InstrumentabilityRules} needs answered about a bean-class, and nothing else.
 *
 * <p>A portable extension is handed {@code AnnotatedType} and can use reflection; a build compatible
 * extension is handed a build-time model and cannot. Both can answer these, so the rules - and the
 * reasons they give - live in one place instead of being re-derived per integration, which is how a
 * class that cannot be subclassed ends up instrumented and a working deployment stops deploying.
 */
public interface TypeFacts {

    enum Kind {
        CLASS, INTERFACE, ANNOTATION, ENUM, RECORD, OTHER
    }

    String className();

    String packageName();

    Kind kind();

    boolean isAbstract();

    boolean isFinal();

    /** an inner class the container cannot instantiate on its own, or anything else not top-level */
    boolean isNested();

    /** synthetic, anonymous, local or hidden - not something anybody wrote as a bean */
    boolean isGenerated();

    /** a constructor the container can call: non-private, and without arguments it cannot supply */
    boolean hasUsableConstructor();

    /**
     * A class-level binding makes every non-static, non-private method an intercepted
     * business-method, and a final one of those is rejected by the container.
     *
     * @return the name of the offending method, or empty when there is none
     */
    Optional<String> finalBusinessMethodName();

    boolean isInterceptorOrDecorator();

    boolean isCdiExtension();

    boolean isFlowSink();
}
