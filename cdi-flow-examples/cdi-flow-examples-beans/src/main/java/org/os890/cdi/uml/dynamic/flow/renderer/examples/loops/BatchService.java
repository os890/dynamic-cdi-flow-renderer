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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.loops;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.AuditService;

import java.util.List;

/**
 * Calls a public method of another CDI bean inside a loop - the case where an unfolded diagram
 * would repeat the same two lines once per iteration.
 */
@ApplicationScoped
public class BatchService {

    @Inject
    private ItemValidator itemValidator;

    @Inject
    private AuditService auditService;

    public int processAll(List<String> items) {
        int validItems = 0;
        for (String item : items) {
            if (itemValidator.validate(item)) {
                validItems++;
            }
        }
        auditService.log("batch processed: " + validItems + "/" + items.size());
        return validItems;
    }
}
