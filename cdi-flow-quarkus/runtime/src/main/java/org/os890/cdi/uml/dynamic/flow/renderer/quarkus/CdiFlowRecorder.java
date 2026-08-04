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

package org.os890.cdi.uml.dynamic.flow.renderer.quarkus;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.runtime.FlowRuntime;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Arms the recorder while the application starts, and disarms it when it stops.
 *
 * <p>The recorder can arm itself on the first recorded call, but doing it here means the
 * output-directory is in the log before the first request arrives - and that work done during
 * startup is recorded as well.
 */
@Recorder
public class CdiFlowRecorder {

    private static final Logger LOGGER = Logger.getLogger(CdiFlowRecorder.class.getName());

    /**
     * @param observerMethodKeys the observer-methods found while the application was built, so an
     *                           event is drawn as an event and not as an ordinary call
     */
    public void arm(ShutdownContext shutdownContext, List<String> observerMethodKeys) {
        FlowRuntime.reset();
        FlowRuntime.markContainerManaged();

        FlowConfig config = FlowConfig.load();
        if (!config.isEnabled()) {
            LOGGER.info("cdi-flow is disabled via '" + FlowConfig.KEY_ENABLED + "'");
            return;
        }
        FlowRuntime runtime = FlowRuntime.activate(config);
        observerMethodKeys.forEach(runtime::registerObserverMethod);
        shutdownContext.addShutdownTask(FlowRuntime::deactivate);

        LOGGER.info(() -> "cdi-flow is recording; "
                + config.outputFormat().name().toLowerCase(Locale.ROOT) + " diagrams are written to "
                + config.outputDirectory().toAbsolutePath());
    }
}
