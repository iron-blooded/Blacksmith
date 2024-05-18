package org.blacksmith.blacksmith.commands;
//import org.blacksmith.blacksmith.Blacksmith
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.blacksmith.blacksmith.*;

public class reload implements CommandExecutor{
//    private final org.blacksmith.blacksmith.Blacksmith plugin;
    private final Blacksmith plugin;
    public reload(Blacksmith plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.config = plugin.getConfig();
        sender.sendMessage("Конфиги успешно перезагружены!");
        return true;
    }
}
