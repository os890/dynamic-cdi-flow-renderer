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

package org.os890.cdi.uml.dynamic.flow.renderer.testbeans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Stereotype shapes for the annotation-closure: direct, stacked and inherited.
 */
public final class StereotypeFixtures {

    private StereotypeFixtures() {
    }

    @Stereotype
    @ApplicationScoped
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface Audited {
    }

    @Stereotype
    @ApplicationScoped
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface Internal {
    }

    /** a stereotype which itself carries another stereotype */
    @Stereotype
    @Audited
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface Stacked {
    }

    @Stereotype
    @ApplicationScoped
    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface InheritedStereotype {
    }

    /** not a stereotype - just a marker annotation */
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface PlainMarker {
    }

    @Audited
    public static class AuditedBean {
    }

    @Stacked
    public static class StackedBean {
    }

    @Internal
    public static class InternalBean {
    }

    @PlainMarker
    public static class MarkedBean {
    }

    @ApplicationScoped
    public static class PlainBean {
    }

    @InheritedStereotype
    public static class InheritingBaseBean {
    }

    public static class InheritingBean extends InheritingBaseBean {
    }
}
