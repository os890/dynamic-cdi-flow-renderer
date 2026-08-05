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

import org.apache.tomee.embedded.Configuration;
import org.apache.tomee.embedded.Container;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Starts the server and deploys this application into it.
 *
 * <p>The server is TomEE - Tomcat, OpenWebBeans for CDI, CXF for Jakarta REST, Johnzon for JSON -
 * embedded so that the example needs no installation. What is deployed is an ordinary web
 * application: the classpath as its classes, the built Angular bundle as its document-root.
 *
 * <p>There is nothing about recording in here. The portable extension in the addon jar is picked up
 * by the CDI container of the web application, attaches the recorder to the beans and arms it; the
 * server finds cdi-flow's request-filter the same way it finds any other provider. The only cdi-flow
 * lines in this whole application are the two defaults below, and only because a diagram has to be
 * written somewhere.
 */
public final class CrudApplication {

    private static final int DEFAULT_PORT = 8092;

    private CrudApplication() {
    }

    public static void main(String[] args) throws Exception {
        configureRecorderDefaults();

        int port = port();
        Configuration configuration = new Configuration()
                .http(port)
                .dir(Files.createTempDirectory("cdi-flow-crud-").toString());

        try (Container container = new Container(configuration)) {
            container.deployClasspathAsWebApp("", frontEnd());
            System.out.printf("cdi-flow CRUD example (Jakarta EE, TomEE %s) on http://localhost:%d%n",
                    tomeeVersion(), port);
            container.await();
        }
    }

    /**
     * Where the diagrams go, and the licence header every generated file carries.
     *
     * <p>The include-pattern is the one thing a server needs that Quarkus does not.
     *
     * <p>As system-properties rather than in {@code META-INF/microprofile-config.properties}, because
     * this server ships no MicroProfile-Config implementation - the addon then reads system-properties
     * and environment-variables, which is exactly what it falls back to. Set only when nothing else
     * says otherwise, so `run.sh --format plantuml` and `--no-title` keep working through the
     * environment.
     */
    private static void configureRecorderDefaults() {
        setUnlessConfigured("cdi-flow.output-directory", "target/flow-diagrams");
        //a server has beans of its own - MyFaces, the CDI implementation, the REST layer - and
        //recording those says nothing about this application. Quarkus knows which beans belong to the
        //application archive and needs no pattern; a full server does.
        setUnlessConfigured("cdi-flow.include-pattern", "org\\.os890\\.cdi\\.uml\\.examples\\..*");
        setUnlessConfigured("cdi-flow.file-header", "Licensed under the Apache License, Version 2.0");
    }

    private static void setUnlessConfigured(String key, String value) {
        String environmentName = key.toUpperCase().replace('.', '_').replace('-', '_');
        if (System.getProperty(key) == null && System.getenv(environmentName) == null) {
            System.setProperty(key, value);
        }
    }

    private static int port() {
        String configured = System.getenv("CRUD_PORT");
        return configured == null || configured.isBlank()
                ? Integer.getInteger("crud.port", DEFAULT_PORT)
                : Integer.parseInt(configured);
    }

    /**
     * The document-root: the built Angular bundle. Without it the API still answers, so a run before
     * the front-end was built fails visibly rather than mysteriously.
     */
    private static File frontEnd() {
        Path bundle = Paths.get(System.getProperty("crud.webui", "target/webui"));
        if (!Files.isDirectory(bundle)) {
            System.err.println("the front-end bundle is not at " + bundle.toAbsolutePath()
                    + " - run `mvn package` first");
        }
        return bundle.toFile();
    }

    private static String tomeeVersion() {
        String version = Container.class.getPackage().getImplementationVersion();
        return version == null ? "embedded" : version;
    }
}
