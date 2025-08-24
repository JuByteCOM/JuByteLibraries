package com.jubyte.libraries.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Loads the appropriate H2 database driver based on the server's Java version.
 */
public final class H2Loader {

    private static final String MAVEN_REPO = "https://repo1.maven.org/maven2/com/h2database/h2/";

    private H2Loader() {
    }

    public static void load(JavaPlugin plugin) {
        try {
            int major = getJavaMajor();
            String version = major >= 11 ? "2.3.232" : "1.4.200";

            Path libsDir = plugin.getDataFolder().toPath().resolve("libs");
            Files.createDirectories(libsDir);
            Path jarPath = libsDir.resolve("h2-" + version + ".jar");

            if (Files.notExists(jarPath)) {
                String url = MAVEN_REPO + version + "/h2-" + version + ".jar";
                try (InputStream in = new URL(url).openStream()) {
                    Files.copy(in, jarPath);
                }
            }

            // Create a dedicated class loader for the H2 driver instead of
            // manipulating the server's class loader which isn't allowed on
            // newer Java versions.
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{jarPath.toUri().toURL()},
                    plugin.getClass().getClassLoader());
            Class<?> driverClass = Class.forName("org.h2.Driver", true, loader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load H2 database driver", e);
        }
    }

    private static int getJavaMajor() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dot = version.indexOf('.');
        if (dot != -1) {
            version = version.substring(0, dot);
        }
        return Integer.parseInt(version);
    }
    private static class DriverShim implements Driver {
        private final Driver driver;

        private DriverShim(Driver driver) {
            this.driver = driver;
        }

        @Override
        public java.sql.Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }
}
