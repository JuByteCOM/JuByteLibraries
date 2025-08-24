package com.jubyte.libraries.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Lädt den passenden H2-Treiber nur auf MC-Versionen < 1.17.
 * Ab 1.17 sollte 'plugin.yml -> libraries:' genutzt werden.
 */
public final class H2Loader {

    private static final String MAVEN_REPO = "https://repo1.maven.org/maven2/com/h2database/h2/";

    private H2Loader() {
    }

    public static void load(JavaPlugin plugin) {
        // Guard: Nur auf Minecraft < 1.17 ausführen
        if (!isUnder117()) {
            plugin.getLogger().fine("H2Loader übersprungen: Server-Version ist >= 1.17 (nutze plugin.yml:libraries).");
            return;
        }

        try {
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

            // Auf alten Servern ist der Plugin-Classloader i. d. R. ein URLClassLoader
            ClassLoader cl = plugin.getClass().getClassLoader();
            if (cl instanceof URLClassLoader) {
                URLClassLoader loader = (URLClassLoader) cl;
                Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addURL.setAccessible(true);
                addURL.invoke(loader, jarPath.toUri().toURL());
            } else {
                plugin.getLogger().warning("Unerwarteter ClassLoader-Typ: " + cl.getClass().getName() +
                        " — H2-Jar wird nicht injiziert.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load H2 database driver", e);
        }
    }

    /** Prüft, ob die laufende MC-Version < 1.17 ist (z. B. 1.8–1.16.5). */
    private static boolean isUnder117() {
        // Beispiele: "1.16.5-R0.1-SNAPSHOT", "1.12.2", "1.20.4-R0.1-SNAPSHOT"
        String base = Bukkit.getBukkitVersion().split("-")[0]; // "1.x.y"
        String[] parts = base.split("\\.");
        if (parts.length < 2) return true; // sehr alte/unklare Angabe -> konservativ behandeln
        // parts[0] ist "1", parts[1] ist die Minor (8, 12, 16, 17, 20, ...)
        int minor;
        try {
            minor = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return true; // im Zweifel als < 1.17 behandeln
        }
        return minor < 17;
    }

    private static int getJavaMajor() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) version = version.substring(2); // "1.8" -> "8"
        int dot = version.indexOf('.');
        if (dot != -1) version = version.substring(0, dot);
        return Integer.parseInt(version);
    }
}
