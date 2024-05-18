package org.resourcepackset.resourcepackset.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.resourcepackset.resourcepackset.ResourcePackSet;

public class rpGet implements CommandExecutor {
    private final ResourcePackSet plugin;

    public rpGet(ResourcePackSet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (args.length >= 1){
            String link = "";
            if (args[0].contains("reset")){
                link = "http://google.com";
            }
            else {
                if (plugin.config.contains(args[0])){
                    link = plugin.config.getString(args[0]);
                }
                else {
                    player.sendMessage("Такого ресурспака не существует.");
                    return true;
                }
            }
            player.setTexturePack(link);
            player.sendMessage("Ресурспак установлен!");
            return true;
        }
        return false;
    }
}
