package com.qa.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central place for environment configuration. Values come from
 * {@code config.properties} on the classpath (defaults for the self-hosted
 * toolshop-clone-api target); any key can be overridden on the Maven/Surefire
 * command line with {@code -Dkey=value} without touching the file - that's how
 * Jenkins (see Jenkinsfile's API_BASE_URL parameter) and ad-hoc runs against a
 * different environment work.
 *
 * Resolution order for every key: system property (-D) -> config.properties -> error.
 * There is no silent third fallback - a missing key fails fast at startup rather
 * than sending a request to an unresolved or wrong URL.
 */
public final class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = load();

    private ConfigManager() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(CONFIG_FILE + " not found on the classpath "
                        + "(expected at src/test/resources/" + CONFIG_FILE + ")");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + CONFIG_FILE, e);
        }
        return properties;
    }

    /** -D system property wins if present; otherwise the value from config.properties. */
    private static String resolve(String key) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override;
        }
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("No value for '" + key + "' in -D or " + CONFIG_FILE);
        }
        return value;
    }

    public static String baseUrl() {
        return resolve("api.baseUrl");
    }

    public static String url(String path) {
        return baseUrl() + path;
    }

    public static String customerEmail() {
        return resolve("customer.email");
    }

    public static String customerPassword() {
        return resolve("customer.password");
    }

    public static String adminEmail() {
        return resolve("admin.email");
    }

    public static String adminPassword() {
        return resolve("admin.password");
    }
}
