package com.jubyte.libraries.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
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

            URLClassLoader loader = (URLClassLoader) plugin.getClass().getClassLoader();
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(loader, jarPath.toUri().toURL());
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
}
