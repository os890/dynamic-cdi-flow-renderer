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

package org.jboss.weld.fake;

/**
 * Stand-in for the marker-interfaces Weld puts on its generated classes
 * (e.g. {@code org.jboss.weld.bean.proxy.WeldConstruct}).
 * <p>
 * The addon is compiled against the CDI API only, so it recognizes such markers by the package
 * their name starts with. This fake lets that rule be tested without pulling Weld onto the
 * addon's class-path.
 */
public interface WeldConstructMarker {
}
