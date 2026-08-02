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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.recursion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Real recursion through a contextual reference. Every level re-enters the <em>same</em> bean
 * instance, which is how the proxy-frame collapser tells recursion apart from a proxy hop.
 */
@ApplicationScoped
public class RecursiveService {

    @Inject
    private Instance<RecursiveService> self;

    public int countdown(int value) {
        if (value <= 0) {
            return 0;
        }
        return 1 + self.get().countdown(value - 1);
    }
}
