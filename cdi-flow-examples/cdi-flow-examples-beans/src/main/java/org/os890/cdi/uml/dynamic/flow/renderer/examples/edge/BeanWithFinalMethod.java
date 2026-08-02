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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.edge;

import jakarta.enterprise.context.Dependent;

/**
 * A final business-method is rejected by the container as soon as the class carries an
 * interceptor-binding, so this bean must not be instrumented either.
 */
@Dependent
public class BeanWithFinalMethod {

    public final String stamp() {
        return "stamped";
    }
}
