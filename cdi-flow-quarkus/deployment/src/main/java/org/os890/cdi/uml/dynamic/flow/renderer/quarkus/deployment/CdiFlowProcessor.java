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

package org.os890.cdi.uml.dynamic.flow.renderer.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.LaunchMode;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Declaration;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.os890.cdi.uml.dynamic.flow.renderer.api.FlowRecorded;
import org.os890.cdi.uml.dynamic.flow.renderer.config.FlowConfig;
import org.os890.cdi.uml.dynamic.flow.renderer.extension.InstrumentabilityRules;
import org.os890.cdi.uml.dynamic.flow.renderer.jaxrs.FlowLabelFilter;
import org.os890.cdi.uml.dynamic.flow.renderer.quarkus.CdiFlowRecorder;
import org.os890.cdi.uml.dynamic.flow.renderer.runtime.FlowRecordingInterceptor;
import org.os890.cdi.uml.dynamic.flow.renderer.runtime.FlowRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Makes cdi-flow a dependency and nothing else.
 *
 * <p>Quarkus resolves beans, interceptors and bindings while it builds the application and never runs
 * a portable CDI extension, so the one cdi-flow ships does nothing here. This does the three things
 * that were missing instead: it attaches the recording binding to the beans of <em>the application
 * archive</em>, it registers the interceptor and the label-filter as beans, and it arms the recorder
 * at startup.
 *
 * <p>Restricting the binding to the application archive is the part only a Quarkus extension can do.
 * Every bean the index holds includes the hundreds a framework brings along, and recording those
 * produces diagrams nobody asked about - so an integration without this knowledge needs an
 * include-pattern, and this one does not.
 *
 * <h2>When it records</h2>
 *
 * In dev-mode and in tests, whenever the extension is on the class-path: that is where a recording
 * tool belongs, and no configuration should be needed to use it. In a production build it stays out
 * of the way unless {@code cdi-flow.enabled=true} says otherwise - packaging an application which
 * instruments every bean of itself should take a deliberate act.
 */
public class CdiFlowProcessor {

    private static final String FEATURE = "cdi-flow";

    private static final Logger LOGGER = Logger.getLogger(CdiFlowProcessor.class.getName());

    private static final List<DotName> OBSERVER_ANNOTATIONS = List.of(
            DotName.createSimple("jakarta.enterprise.event.Observes"),
            DotName.createSimple("jakarta.enterprise.event.ObservesAsync"));

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * The interceptor and the filter live in a dependency that carries no bean-defining annotation
     * ArC would find on its own, so they are registered explicitly - and unremovable, because
     * nothing injects them.
     */
    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans, LaunchModeBuildItem launchMode) {
        if (!isRecording(launchMode)) {
            return;
        }
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(FlowRecordingInterceptor.class, FlowLabelFilter.class)
                .setUnremovable()
                .build());
    }

    @BuildStep
    void attachRecorder(BuildProducer<AnnotationsTransformerBuildItem> transformers,
                        ApplicationIndexBuildItem applicationIndex,
                        LaunchModeBuildItem launchMode) {
        if (!isRecording(launchMode)) {
            return;
        }
        FlowConfig config = FlowConfig.load();
        Index index = applicationIndex.getIndex();

        transformers.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forClasses()
                .when(context -> isRecorded(context.declaration(), index, config))
                .transform(context -> context.add(FlowRecorded.class))));

        LOGGER.info("cdi-flow records the beans of this application"
                + (config.includePattern() == null ? "" : ", narrowed to " + config.includePattern()));
    }

    /**
     * Arms the recorder at runtime-init, so the configuration of the machine it runs on decides -
     * not the configuration of the machine it was built on.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem armRecorder(CdiFlowRecorder recorder, ShutdownContextBuildItem shutdownContext,
                                      BeanArchiveIndexBuildItem beanArchiveIndex,
                                      LaunchModeBuildItem launchMode) {
        if (isRecording(launchMode)) {
            recorder.arm(shutdownContext, observerMethodsOf(beanArchiveIndex.getIndex()));
        }
        return new ServiceStartBuildItem(FEATURE);
    }

    /**
     * The observer-methods of the application, named rather than reflected on.
     *
     * <p>A synchronous event is delivered on the firing thread and therefore belongs to the same
     * call-chain, and cdi-flow draws it with an event-arrow instead of a call-arrow. Its portable
     * extension learns which methods those are from {@code ProcessObserverMethod}, which does not
     * exist here - so they are collected from the index and handed to the runtime.
     */
    private static List<String> observerMethodsOf(IndexView index) {
        List<String> keys = new ArrayList<>();
        for (DotName observesAnnotation : OBSERVER_ANNOTATIONS) {
            for (AnnotationInstance observes : index.getAnnotations(observesAnnotation)) {
                if (observes.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER) {
                    continue;
                }
                MethodInfo method = observes.target().asMethodParameter().method();
                keys.add(FlowRuntime.observerKey(method.declaringClass().name().toString(), method.name(),
                        method.parameterTypes().stream().map(type -> type.name().toString()).toList()));
            }
        }
        return keys;
    }

    private static boolean isRecording(LaunchModeBuildItem launchMode) {
        if (!FlowConfig.load().isEnabled()) {
            return false;
        }
        if (launchMode.getLaunchMode() != LaunchMode.NORMAL) {
            //dev-mode and tests: on, because that is what a recording tool is for
            return true;
        }
        //a production build instruments nothing unless somebody wrote the property down
        if (!isEnabledExplicitly()) {
            return false;
        }
        LOGGER.warning("cdi-flow is recording in a production build: an interceptor on every bean of"
                + " this application, a growing call-tree per thread and a file per call-chain. That"
                + " is a development tool - leave cdi-flow.enabled unset for anything else.");
        return true;
    }

    /**
     * {@code enabled} defaults to {@code true}, which is right for a container that only ever sees
     * this addon when somebody put it there. A production build is the other way round, so it has to
     * find the property actually set.
     */
    private static boolean isEnabledExplicitly() {
        try {
            return ConfigProvider.getConfig()
                    .getOptionalValue(FlowConfig.KEY_ENABLED, Boolean.class)
                    .orElse(false);
        } catch (RuntimeException e) {
            //no MicroProfile-Config while building: the system-property is the remaining way to say it
            return Boolean.parseBoolean(System.getProperty(FlowConfig.KEY_ENABLED, "false"));
        }
    }

    /**
     * A class is recorded when it is part of the application, may be instrumented at all, and is not
     * narrowed away by {@code cdi-flow.include-pattern} / {@code cdi-flow.exclude-pattern}.
     */
    private static boolean isRecorded(Declaration declaration, Index index, FlowConfig config) {
        if (declaration.kind() != AnnotationTarget.Kind.CLASS) {
            return false;
        }
        ClassInfo type = declaration.asClass();
        if (index.getClassByName(type.name()) == null) {
            //not part of the application archive: a framework bean, or one of cdi-flow's own
            return false;
        }
        Optional<String> rejectionReason =
                InstrumentabilityRules.rejectionReason(new JandexTypeFacts(type));
        if (rejectionReason.isPresent()) {
            LOGGER.finest(() -> "cdi-flow skips " + type.name() + ": " + rejectionReason.get());
            return false;
        }
        return config.matches(type.name().toString(), annotationNamesOf(type));
    }

    private static Set<String> annotationNamesOf(ClassInfo type) {
        return type.declaredAnnotations().stream()
                .map(annotation -> annotation.name())
                .map(DotName::toString)
                .collect(Collectors.toSet());
    }
}
