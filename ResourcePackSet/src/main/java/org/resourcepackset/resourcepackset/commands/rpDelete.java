package org.resourcepackset.resourcepackset.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.resourcepackset.resourcepackset.ResourcePackSet;

public class rpDelete implements CommandExecutor {
    private final ResourcePackSet plugin;

    public rpDelete(ResourcePackSet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (args.length >= 1){
            plugin.config.set(args[0], null);
            plugin.saveConfig();
            plugin.reloadConfig();
            plugin.config = plugin.getConfig();
            player.sendMessage("Ресурспак успешно удален!");
            return true;
        }
        return false;
    }
}
