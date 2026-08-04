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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** In memory, so the example needs no database - and its diagrams show calls, not SQL. */
@ApplicationScoped
public class CustomerRepository {

    private final Map<Long, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public List<Customer> findAll() {
        List<Customer> all = new ArrayList<>(customers.values());
        all.sort((left, right) -> left.name().compareToIgnoreCase(right.name()));
        return all;
    }

    public Optional<Customer> find(long id) {
        return Optional.ofNullable(customers.get(id));
    }

    public Customer save(Customer customer) {
        Customer stored = customer.id() == null
                ? customer.withId(nextId.incrementAndGet())
                : customer;
        customers.put(stored.id(), stored);
        return stored;
    }

    public boolean delete(long id) {
        return customers.remove(id) != null;
    }
}
