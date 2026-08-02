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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.scopes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Touches every scope in one call-chain. The scopes differ in how much proxying the container puts
 * between the caller and the bean, which is exactly what the recorder has to hide again.
 */
@ApplicationScoped
public class ScopeMixingService {

    @Inject
    private ApplicationScopedGreeter applicationScopedGreeter;

    @Inject
    private DependentGreeter dependentGreeter;

    @Inject
    private RequestScopedGreeter requestScopedGreeter;

    @Inject
    private SingletonGreeter singletonGreeter;

    public String greetAll(String name) {
        return String.join(",",
                applicationScopedGreeter.greet(name),
                dependentGreeter.greet(name),
                requestScopedGreeter.greet(name),
                singletonGreeter.greet(name));
    }
}
