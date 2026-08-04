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

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The application: a CDI container, a JAX-RS layer in front of its beans, and the front-end.
 *
 * <p>This is the whole difference to the Quarkus example. There, one dependency arranges the
 * container, the REST layer, the front-end and the recorder; here they are wired by hand, as a
 * plain Jakarta application does it. The beans, the front-end and the test-suite are the same, and
 * so is what cdi-flow produces from them.
 *
 * <p>About the recorder there is nothing to arrange at all: the portable extension in the addon jar
 * is picked up while the container boots, attaches the recorder to the beans and arms it. The only
 * cdi-flow line anywhere in this application is the label-filter registered in
 * {@link CrudRestApplication}, and only because a JAX-RS provider has to be named somewhere.
 */
public final class CrudApplication {

    private static final int DEFAULT_PORT = 8092;

    private CrudApplication() {
    }

    public static void main(String[] args) {
        int port = port();
        //not closed on purpose: the container lives as long as the process, and its shutdown-hook
        //flushes the recorder
        SeContainer container = SeContainerInitializer.newInstance().initialize();

        UndertowJaxrsServer server = new UndertowJaxrsServer();
        server.deploy(new CrudRestApplication(container), "/api");
        frontEnd().ifPresent(directory -> server.addResourcePrefixPath("/",
                new ResourceHandler(new PathResourceManager(directory))
                        .setWelcomeFiles("index.html")));

        server.start(Undertow.builder().addHttpListener(port, "0.0.0.0"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            container.close();
        }));

        System.out.printf("cdi-flow CRUD example (Jakarta) listening on http://localhost:%d%n", port);
    }

    private static int port() {
        String configured = System.getenv("CRUD_PORT");
        return configured == null || configured.isBlank()
                ? Integer.getInteger("crud.port", DEFAULT_PORT)
                : Integer.parseInt(configured);
    }

    /**
     * The built Angular bundle, if it is there - the build produces it, and a run without it still
     * serves the API so the failure is obvious rather than mysterious.
     */
    private static java.util.Optional<Path> frontEnd() {
        Path bundle = Paths.get(System.getProperty("crud.webui", "target/webui"));
        if (Files.isDirectory(bundle)) {
            return java.util.Optional.of(bundle);
        }
        System.err.println("the front-end bundle is not at " + bundle.toAbsolutePath()
                + " - run `mvn package` first");
        return java.util.Optional.empty();
    }
}
