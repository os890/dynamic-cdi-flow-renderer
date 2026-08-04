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

package org.os890.cdi.uml.dynamic.flow.renderer.jaxrs;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowLabel;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;

/**
 * Labels everything recorded during a request with the use-case the caller names in a header.
 *
 * <p>This is what lets one running application serve a whole browser-driven suite and still keep the
 * recordings apart: a test sets {@code X-Flow-Label} (and optionally {@code X-Flow-Description}) on
 * its requests, and every flow started while handling them is filed under that use-case. Without the
 * header nothing changes - the flows are written as they always were.
 *
 * <p>Runs as early as a request filter can, so a flow started by an authentication mechanism or
 * another filter is labelled as well, and clears the label again on the way out: the thread goes back
 * into a pool, and a label left behind would end up on somebody else's request.
 */
@Provider
@Priority(1)
public class FlowLabelFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final String labelHeader;
    private final String descriptionHeader;

    public FlowLabelFilter() {
        FlowConfig config = FlowConfig.load();
        this.labelHeader = config.labelHeader();
        this.descriptionHeader = config.descriptionHeader();
    }

    @Override
    public void filter(ContainerRequestContext request) {
        //set unconditionally: a request without the header must not inherit the previous label
        FlowLabel.set(request.getHeaderString(labelHeader), request.getHeaderString(descriptionHeader));
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        FlowLabel.clear();
    }
}
