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

package org.os890.cdi.uml.examples.jakartacrud;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.ws.rs.core.Application;
import org.os890.cdi.uml.dynamic.flow.renderer.jaxrs.FlowLabelFilter;

import java.util.Set;

/**
 * The JAX-RS side of the application.
 *
 * <p>The resource and the exception-mapper are taken <em>from the container</em> rather than
 * constructed here. That is what makes them intercepted beans instead of plain objects - and
 * therefore what makes them appear in a diagram at all. It is the one thing to get right when a
 * REST layer is wired by hand.
 *
 * <p>{@link FlowLabelFilter} comes from {@code cdi-flow-jaxrs} and is the same filter the Quarkus
 * extension registers by itself: it reads the use-case a caller names in a header.
 */
public class CrudRestApplication extends Application {

    private final Set<Object> singletons;

    public CrudRestApplication(SeContainer container) {
        this.singletons = Set.of(
                container.select(CustomerResource.class).get(),
                container.select(BusinessRuleMapper.class).get(),
                new FlowLabelFilter(),
                new JsonProvider());
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
