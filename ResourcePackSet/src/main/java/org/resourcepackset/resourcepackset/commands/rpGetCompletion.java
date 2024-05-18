package org.resourcepackset.resourcepackset.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import org.jetbrains.annotations.Nullable;
import org.resourcepackset.resourcepackset.ResourcePackSet;

import java.util.ArrayList;
import java.util.List;

public class rpGetCompletion implements TabCompleter{
    private final ResourcePackSet plugin;

    public rpGetCompletion(ResourcePackSet plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> list = new ArrayList<String>();
        if (args.length == 1){
            try {
            for (String key : plugin.config.getConfigurationSection("").getKeys(false)) {
                list.add(key);
                }
            }
            catch (Exception e){}
        }
        list.add("reset");
        return list;
    }
}
