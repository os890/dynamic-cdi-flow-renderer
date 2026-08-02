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

/**
 * Receives a call-flow once the outermost recorded method returned.
 * <p>
 * Implementations are picked up in two ways:
 * <ul>
 *     <li>statically via {@link FlowSinks#register(FlowSink)} - also works before the container
 *     is booted, which is what the tests use</li>
 *     <li>as a CDI bean - the extension looks them up during {@code AfterDeploymentValidation}</li>
 * </ul>
 * Sinks are never themselves recorded: the recorder suspends itself while publishing.
 */
@FunctionalInterface
public interface FlowSink {

    void onFlowRecorded(CallFlow flow);
}
