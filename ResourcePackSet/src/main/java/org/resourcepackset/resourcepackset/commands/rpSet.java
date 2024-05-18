package org.resourcepackset.resourcepackset.commands;
import org.jetbrains.annotations.NotNull;
import org.resourcepackset.resourcepackset.ResourcePackSet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class rpSet implements CommandExecutor{
    private final ResourcePackSet plugin;

    public rpSet(ResourcePackSet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2){
            plugin.config.set(args[0], args[1]);
            sender.sendMessage("Ресурспак успешно добавлен!");
            plugin.saveConfig();
            plugin.reloadConfig();
            plugin.config = plugin.getConfig();
        }
        else {
            return false;
        }
        return true;
    }
}
