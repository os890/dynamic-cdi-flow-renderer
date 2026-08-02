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
import jakarta.inject.Inject;
import org.os890.cdi.uml.dynamic.flow.renderer.examples.order.AuditService;

/**
 * Demonstrates the one thing an interceptor-based recorder cannot see: a call a bean makes on
 * itself never leaves the instance, so no interceptor runs for it. Whatever {@code inner()} calls
 * still shows up - just one level too high.
 */
@ApplicationScoped
public class SelfCallService {

    @Inject
    private AuditService auditService;

    public String outer() {
        return "outer:" + inner();
    }

    public String inner() {
        auditService.log("inner was called");
        return "inner";
    }
}
