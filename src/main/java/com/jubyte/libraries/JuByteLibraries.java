package com.jubyte.libraries;

import com.jubyte.developerapi.commands.AbstractCommand;
import com.jubyte.developerapi.web.HasteServer;
import com.jubyte.libraries.command.PasteCommand;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Justin_SGD
 * @since 22.09.2022
 */

public class JuByteLibraries extends JavaPlugin {

    @Getter
    private JuByteLibraries juByteLibraries;
    @Getter
    private HasteServer hasteServer;

    public void onEnable() {
        this.juByteLibraries = this;

        this.hasteServer = new HasteServer();

        this.loadCommands();
    }

    private void loadCommands() {
        AbstractCommand pasteCommand = new PasteCommand(this);
        pasteCommand.register();
    }
}