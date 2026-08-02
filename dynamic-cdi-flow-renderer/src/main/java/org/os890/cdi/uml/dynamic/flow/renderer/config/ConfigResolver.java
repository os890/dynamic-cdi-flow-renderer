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

package org.os890.cdi.uml.dynamic.flow.renderer.config;

import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads configuration through MicroProfile-Config when it is available and falls back to
 * system-properties / environment-variables otherwise.
 * <p>
 * The fallback is what makes the MicroProfile-Config dependency genuinely optional: the addon
 * has to keep working in a plain CDI container which has no MP-Config implementation on the
 * class-path at all.
 */
final class ConfigResolver {

    private static final Logger LOGGER = Logger.getLogger(ConfigResolver.class.getName());

    private static final String MP_CONFIG_PROVIDER = "org.eclipse.microprofile.config.ConfigProvider";

    private ConfigResolver() {
    }

    static Optional<String> lookup(String key) {
        Optional<String> viaMpConfig = viaMicroProfileConfig(key);
        if (viaMpConfig.isPresent()) {
            return viaMpConfig;
        }
        return viaSystemProperties(key);
    }

    static boolean isMicroProfileConfigAvailable() {
        return MpConfigHolder.AVAILABLE;
    }

    private static Optional<String> viaMicroProfileConfig(String key) {
        if (!MpConfigHolder.AVAILABLE) {
            return Optional.empty();
        }
        try {
            return org.eclipse.microprofile.config.ConfigProvider.getConfig()
                    .getOptionalValue(key, String.class);
        } catch (Throwable t) {
            //e.g. a broken/half-initialized MP-Config implementation - never let it break the boot
            LOGGER.log(Level.FINE, t, () -> "could not read '" + key + "' via MicroProfile-Config");
            return Optional.empty();
        }
    }

    private static Optional<String> viaSystemProperties(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(toEnvironmentName(key));
        }
        return Optional.ofNullable(value).map(String::trim).filter(v -> !v.isEmpty());
    }

    /**
     * {@code cdi-flow.output-directory} -> {@code CDI_FLOW_OUTPUT_DIRECTORY}
     */
    static String toEnvironmentName(String key) {
        StringBuilder result = new StringBuilder(key.length());
        for (char c : key.toCharArray()) {
            result.append(Character.isLetterOrDigit(c) ? Character.toUpperCase(c) : '_');
        }
        return result.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * Resolved once - {@code Class.forName} on every lookup would be needless overhead and the
     * class-path cannot change while the container is booting.
     */
    private static final class MpConfigHolder {
        private static final boolean AVAILABLE = detect();

        private static boolean detect() {
            try {
                Class.forName(MP_CONFIG_PROVIDER, false, resolveClassLoader());
                return true;
            } catch (Throwable t) {
                LOGGER.fine("MicroProfile-Config is not available - falling back to system-properties");
                return false;
            }
        }

        private static ClassLoader resolveClassLoader() {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            return contextClassLoader != null ? contextClassLoader : ConfigResolver.class.getClassLoader();
        }
    }
}
