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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Beans created by a producer-method are no managed beans, so the container never intercepts them.
 * The producer-method itself belongs to a managed bean and is recorded.
 */
@ApplicationScoped
public class GreetingProducer {

    /**
     * {@code @Dependent} on purpose - a normal-scoped producer would need a proxyable return-type,
     * and the point of this example is that the produced final class is <em>not</em> proxied.
     */
    @Produces
    public GreetingTemplate greetingTemplate() {
        return new GreetingTemplate("hello %s");
    }

    /** deliberately not a bean - it is produced, not managed */
    public static final class GreetingTemplate {
        private final String pattern;

        public GreetingTemplate(String pattern) {
            this.pattern = pattern;
        }

        public String format(String name) {
            return pattern.formatted(name);
        }
    }
}
