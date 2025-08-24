package com.jubyte.libraries.command;

import com.jubyte.developerapi.commands.AbstractCommand;
import com.jubyte.libraries.JuByteLibraries;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Justin_SGD
 * @since 02.02.2023
 */

public class PasteCommand extends AbstractCommand {

    private final JuByteLibraries juByteLibraries;

    public PasteCommand(JuByteLibraries juByteLibraries) {
        super("jubyte");
        this.juByteLibraries = juByteLibraries;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length != 1) return false;
        if (!strings[0].equalsIgnoreCase("paste")) return false;
        try {
            System.out.println("===================================[Server Information]===================================");
            System.out.println("Server-Version: " + Bukkit.getBukkitVersion());
            System.out.println("Server-IP : " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Plugins: ");
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                System.out.println("- Plugin-Name: " + plugin.getName() + ", Plugin-Version: " + plugin.getDescription().getVersion()
                        + ", Plugins-Authors: " + plugin.getDescription().getAuthors());
            }
            System.out.println("===================================[Server Information]===================================");
        } catch (UnknownHostException e) {
            Bukkit.getLogger().severe("[JuByteLibraries] IPv4 not found from this server.");
            throw new RuntimeException(e);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                final File file = new File("logs/latest.log");
                String content = juByteLibraries.getHasteServer().readFileReversed(file);
                if (content == null) return;
                String url = juByteLibraries.getHasteServer().publishLogAndGetUrl(content);
                if (url == null) return;
                Bukkit.getLogger().info("See your log on " + url);
                commandSender.sendMessage("The latest log file url: " + url);
            }
        }.runTaskLaterAsynchronously(this.juByteLibraries, 60L);
        return false;
    }
}