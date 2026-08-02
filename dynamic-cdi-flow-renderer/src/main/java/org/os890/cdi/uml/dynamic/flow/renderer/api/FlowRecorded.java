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

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Interceptor-binding of the flow-recorder.
 * <p>
 * It usually isn't needed in application-code: the portable extension adds it to every eligible
 * bean-class during {@code ProcessAnnotatedType}. It can still be used explicitly, e.g. to record
 * a single bean while {@code cdi-flow.include-pattern} excludes everything else.
 */
@InterceptorBinding
@Inherited
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface FlowRecorded {

    final class Literal extends AnnotationLiteral<FlowRecorded> implements FlowRecorded {
        private static final long serialVersionUID = 1L;

        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }
}
