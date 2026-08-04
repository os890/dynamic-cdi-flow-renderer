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

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Where the REST endpoints live - and nothing else.
 *
 * <p>The server finds the resources, the exception-mapper and the providers on its own, cdi-flow's
 * request-filter among them; and because they are CDI beans, they are recorded. That is the whole
 * difference to wiring a REST layer by hand: there is nothing to list here.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
