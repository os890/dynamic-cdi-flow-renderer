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
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The one bean that calls the others, which is what makes a diagram worth looking at. */
@ApplicationScoped
public class CustomerService {

    @Inject
    CustomerRepository repository;

    @Inject
    CustomerValidation validation;

    @Inject
    CustomerNumbers customerNumbers;

    @Inject
    TagNormalizer tagNormalizer;

    @Inject
    Event<CustomerCreated> customerCreated;

    public List<Customer> list() {
        return repository.findAll();
    }

    public Optional<Customer> find(long id) {
        return repository.find(id);
    }

    public Customer create(Customer customer) {
        validation.check(customer);
        Customer stored = repository.save(new Customer(null, customer.name().strip(),
                customer.email(), normalize(customer.tagsOrEmpty())));
        String customerNumber = customerNumbers.nextFor(stored);
        customerCreated.fire(new CustomerCreated(stored, customerNumber));
        return stored;
    }

    public Optional<Customer> update(long id, Customer customer) {
        return repository.find(id).map(existing -> {
            validation.check(customer);
            return repository.save(new Customer(id, customer.name().strip(), customer.email(),
                    normalize(customer.tagsOrEmpty())));
        });
    }

    public boolean delete(long id) {
        return repository.delete(id);
    }

    /** one call per tag - consecutive identical calls fold into a `loop` block */
    private List<String> normalize(List<String> tags) {
        List<String> normalized = new ArrayList<>(tags.size());
        for (String tag : tags) {
            normalized.add(tagNormalizer.normalize(tag));
        }
        return normalized;
    }
}
