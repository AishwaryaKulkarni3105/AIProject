package com.ediqa.api.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads environment-specific properties from {@code config/<environment>.properties}
 * on the test classpath.  The active environment is controlled by the
 * {@code -Denvironment} JVM system property (default: {@code qa}).
 *
 * <p>Usage: {@code ConfigManager.getInstance().getBaseUrl()}
 *
 * <p>To run against prod: {@code mvn test -Denvironment=prod}
 */
public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static final ConfigManager INSTANCE = new ConfigManager();

    private final Properties props = new Properties();
    private final String environment;

    private ConfigManager() {
        environment = System.getProperty("environment", "qa");
        String path = "config/" + environment + ".properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Properties file not found on classpath: " + path);
            }
            props.load(is);
            log.info("Loaded configuration from classpath:{}", path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties file: " + path, e);
        }
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the value of {@code key}, throwing if it is absent.
     */
    public String getProperty(String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new RuntimeException(
                "Property '" + key + "' not found in config/" + environment + ".properties");
        }
        return value;
    }

    /** Convenience accessor for {@code base.url}. */
    public String getBaseUrl() {
        return getProperty("base.url");
    }

    /** Returns the active environment name (e.g. "qa", "prod"). */
    public String getEnvironment() {
        return environment;
    }
}
