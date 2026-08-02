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

package org.os890.cdi.uml.dynamic.flow.renderer.examples.scopes;

import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Singleton;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A stereotype declaring {@link Singleton} as its default scope.
 * <p>
 * {@code jakarta.inject.Singleton} on its own is not discovered by every container in
 * {@code bean-discovery-mode="annotated"}, while a stereotype always is. It also proves that the
 * recorder is applied to stereotyped beans just as it is to directly scoped ones.
 */
@Stereotype
@Singleton
@Target(TYPE)
@Retention(RUNTIME)
public @interface SingletonBean {
}
