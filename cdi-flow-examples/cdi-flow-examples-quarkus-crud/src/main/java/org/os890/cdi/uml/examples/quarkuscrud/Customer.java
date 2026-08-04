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

package org.os890.cdi.uml.examples.quarkuscrud;

import java.util.List;

/**
 * A customer as the API hands it out and takes it in.
 *
 * <p>A record, so never a bean and never recorded - and deliberately without helper-methods: to a
 * JSON binding, a method on a record looks like one more property to write out.
 */
public record Customer(Long id, String name, String email, List<String> tags) {

    public Customer withId(Long assignedId) {
        return new Customer(assignedId, name, email, tags);
    }
}
