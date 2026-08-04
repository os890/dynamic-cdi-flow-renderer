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

package org.os890.cdi.uml.dynamic.flow.renderer.extension;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowRecorded;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSink;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowSinks;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.runtime.FlowRuntime;
import org.os890.cdi.uml.dynamic.flow.renderer.sink.FileFlowSink;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Portable CDI extension which applies the flow-recorder to the beans of the application during
 * the boot of the container.
 * <p>
 * It is implemented against the CDI API/SPI only - there is no reference to Weld, to
 * OpenWebBeans or to any other implementation anywhere in the addon.
 */
public class FlowRecorderExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(FlowRecorderExtension.class.getName());

    private FlowConfig config;
    private FlowRuntime runtime;
    private int instrumentedTypeCount;

    /**
     * Reads the configuration and starts the runtime. From here on the interceptor - which is
     * globally enabled via {@code @Priority} - has something to record into.
     */
    void startRecorder(@Observes BeforeBeanDiscovery beforeBeanDiscovery) {
        //a previous container in the same JVM may still be registered
        FlowRuntime.reset();
        //this container arms and disarms the recorder, so it must not arm itself on the first call
        FlowRuntime.markContainerManaged();

        config = FlowConfig.load();
        if (!config.isEnabled()) {
            LOGGER.info("cdi-flow is disabled via '" + FlowConfig.KEY_ENABLED + "'");
            return;
        }

        beforeBeanDiscovery.addInterceptorBinding(FlowRecorded.class);
        runtime = FlowRuntime.activate(config);

        LOGGER.info(() -> "cdi-flow is recording call-flows into " + config.outputDirectory()
                + " (" + config + ", MicroProfile-Config "
                + (FlowConfig.isMicroProfileConfigAvailable() ? "available" : "not available") + ")");
    }

    /**
     * Adds the interceptor-binding to every eligible bean-class. This is what makes the recorder
     * apply automatically - application-beans stay free of any cdi-flow annotation.
     */
    <T> void enableRecordingFor(@Observes ProcessAnnotatedType<T> processAnnotatedType) {
        if (runtime == null) {
            return;
        }

        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();
        Class<?> javaClass = annotatedType.getJavaClass();

        var rejectionReason = Instrumentability.rejectionReason(annotatedType);
        if (rejectionReason.isPresent()) {
            LOGGER.log(Level.FINE,
                    () -> "not recording " + javaClass.getName() + ": " + rejectionReason.get());
            return;
        }

        //the annotation-closure is only built when a stereotype is actually configured
        Set<String> annotationTypeNames = config.hasStereotypeRestriction()
                ? Stereotypes.annotationNamesOf(annotatedType)
                : Set.of();

        if (!config.matches(javaClass.getName(), annotationTypeNames)) {
            LOGGER.log(Level.FINE, () -> "not recording " + javaClass.getName()
                    + ": neither the configured pattern nor a configured stereotype selects it");
            return;
        }

        registerObserverMethods(annotatedType);
        processAnnotatedType.configureAnnotatedType().add(FlowRecorded.Literal.INSTANCE);
        instrumentedTypeCount++;
        LOGGER.log(Level.FINE, () -> "recording " + javaClass.getName());
    }

    /**
     * Observer-methods are invoked by the container rather than by the calling bean, so they are
     * drawn as event-arrows instead of as plain calls.
     */
    private void registerObserverMethods(AnnotatedType<?> annotatedType) {
        for (AnnotatedMethod<?> annotatedMethod : annotatedType.getMethods()) {
            if (!isObserverMethod(annotatedMethod)) {
                continue;
            }
            Method javaMethod = annotatedMethod.getJavaMember();
            runtime.registerObserverMethod(javaMethod.getDeclaringClass(), javaMethod.getName(),
                    javaMethod.getParameterTypes());
        }
    }

    private static boolean isObserverMethod(AnnotatedMethod<?> annotatedMethod) {
        for (AnnotatedParameter<?> parameter : annotatedMethod.getParameters()) {
            if (parameter.isAnnotationPresent(Observes.class)
                    || parameter.isAnnotationPresent(ObservesAsync.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks up sinks which are CDI beans. Sinks registered statically via
     * {@link FlowSinks#register(FlowSink)} are already active at this point.
     */
    void collectFlowSinks(@Observes AfterDeploymentValidation afterDeploymentValidation,
                          BeanManager beanManager) {
        if (runtime == null) {
            return;
        }

        try {
            beanManager.createInstance().select(FlowSink.class).forEach(FlowSinks::register);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, t, () -> "could not look up flow-sinks as CDI beans");
        }

        warnAboutUnknownStereotypes(beanManager);

        LOGGER.info(() -> "cdi-flow is recording " + instrumentedTypeCount + " bean-class(es)");
    }

    /**
     * A mistyped stereotype-name would silently record nothing at all, which is a confusing way to
     * find out about a typo. The names are therefore checked once the container knows its
     * stereotypes - including the ones other extensions registered.
     */
    @SuppressWarnings("unchecked")
    private void warnAboutUnknownStereotypes(BeanManager beanManager) {
        for (String configuredStereotype : config.includeStereotypes()) {
            if (!configuredStereotype.contains(".")) {
                //a simple name cannot be resolved to a class - it is matched by name at runtime
                continue;
            }
            try {
                Class<?> annotationType = Class.forName(configuredStereotype, false, classLoader());
                if (!annotationType.isAnnotation()) {
                    LOGGER.warning(() -> "'" + configuredStereotype + "' configured via "
                            + FlowConfig.KEY_INCLUDE_STEREOTYPES + " is not an annotation");
                } else if (!beanManager.isStereotype((Class<? extends Annotation>) annotationType)) {
                    LOGGER.warning(() -> "'" + configuredStereotype + "' configured via "
                            + FlowConfig.KEY_INCLUDE_STEREOTYPES + " is not a CDI stereotype - it is"
                            + " still matched as a plain annotation");
                }
            } catch (ClassNotFoundException | LinkageError notResolvable) {
                LOGGER.warning(() -> "'" + configuredStereotype + "' configured via "
                        + FlowConfig.KEY_INCLUDE_STEREOTYPES + " is not on the class-path");
            }
        }
    }

    private static ClassLoader classLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader != null ? contextClassLoader : FlowRecorderExtension.class.getClassLoader();
    }

    void stopRecorder(@Observes BeforeShutdown beforeShutdown) {
        FlowRuntime.deactivate();
        runtime = null;
        instrumentedTypeCount = 0;
    }
}
