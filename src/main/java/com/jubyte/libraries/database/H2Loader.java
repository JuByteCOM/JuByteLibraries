package com.jubyte.libraries.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Lädt den passenden H2-Treiber nur auf MC-Versionen < 1.17.
 * Ab 1.17 sollte 'plugin.yml -> libraries:' genutzt werden.
 */
public final class H2Loader {

    private static final String MAVEN_REPO = "https://repo1.maven.org/maven2/com/h2database/h2/";
    private static final String H2_DRIVER_CLASS = "org.h2.Driver";

    private H2Loader() {
    }

    public static void load(JavaPlugin plugin) {
        // Guard: Nur auf Minecraft < 1.17 ausführen
        if (!isUnder117()) {
            plugin.getLogger().fine("H2Loader übersprungen: Server-Version ist >= 1.17 (nutze plugin.yml:libraries).");
            return;
        }

        try {
            if (isDriverAvailable()) {
                plugin.getLogger().fine("H2Loader übersprungen: H2-Treiber ist bereits verfügbar.");
                return;
            }

            int major = getJavaMajor();
            String version = (major >= 11) ? "2.3.232" : "1.4.200";

            Path libsDir = plugin.getDataFolder().toPath().resolve("libs");
            Files.createDirectories(libsDir);
            Path jarPath = libsDir.resolve("h2-" + version + ".jar");

            if (Files.notExists(jarPath)) {
                String url = MAVEN_REPO + version + "/h2-" + version + ".jar";
                try (InputStream in = new URL(url).openStream()) {
                    Files.copy(in, jarPath);
                }
            }

            registerDriver(jarPath);
            plugin.getLogger().info("H2 database driver " + version + " loaded.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load H2 database driver", e);
        }
    }

    private static boolean isDriverAvailable() {
        try {
            Class.forName(H2_DRIVER_CLASS);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static void registerDriver(Path jarPath) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, H2Loader.class.getClassLoader());
        Class<?> driverClass = Class.forName(H2_DRIVER_CLASS, true, loader);
        Driver driver = (Driver) driverClass.newInstance();
        DriverManager.registerDriver(new DriverShim(driver));
    }

    /** Prüft, ob die laufende MC-Version < 1.17 ist (z. B. 1.8–1.16.5). */
    private static boolean isUnder117() {
        // Beispiele: "1.16.5-R0.1-SNAPSHOT", "1.20.4-R0.1-SNAPSHOT", "26.1.2-R0.1-SNAPSHOT"
        String base = Bukkit.getBukkitVersion().split("-")[0];
        String[] parts = base.split("\\.");
        if (parts.length < 2) return true; // sehr alte/unklare Angabe -> konservativ behandeln

        int major;
        int minor;
        try {
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return true; // im Zweifel als < 1.17 behandeln
        }

        if (major > 1) return false;
        if (major < 1) return true;
        return minor < 17;
    }

    private static int getJavaMajor() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) version = version.substring(2); // "1.8" -> "8"
        int dot = version.indexOf('.');
        if (dot != -1) version = version.substring(0, dot);
        return Integer.parseInt(version);
    }

    private static final class DriverShim implements Driver {

        private final Driver driver;

        private DriverShim(Driver driver) {
            this.driver = driver;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
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
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }
}
