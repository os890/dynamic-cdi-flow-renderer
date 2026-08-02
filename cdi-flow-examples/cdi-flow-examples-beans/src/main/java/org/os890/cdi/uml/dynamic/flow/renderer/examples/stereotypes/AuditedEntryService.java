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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.stereotypes;

import jakarta.inject.Inject;

/**
 * Entry-point of the stereotype example - it calls one bean per stereotype-flavour, so a single
 * call shows exactly which of them the configuration selected.
 */
@AuditedService
public class AuditedEntryService {

    @Inject
    private AuditedHelper auditedHelper;

    @Inject
    private StackedHelper stackedHelper;

    @Inject
    private InternalHelper internalHelper;

    @Inject
    private PlainHelper plainHelper;

    public String handle(String input) {
        return String.join(",",
                auditedHelper.audited(input),
                stackedHelper.stacked(input),
                internalHelper.internal(input),
                plainHelper.plain(input));
    }
}
