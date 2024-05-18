package org.blacksmith.blacksmith.commands;

import org.blacksmith.blacksmith.Blacksmith;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.*;
import java.util.*;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.configuration.file.FileConfiguration;

import static org.apache.commons.lang.StringUtils.*;

public class editConfigCompletion implements TabCompleter {
    private final Blacksmith plugin;

    public editConfigCompletion(Blacksmith plugin) {
        this.plugin = plugin;
    }
    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[] args){
        List<String> list = new ArrayList<String>();
        Player player = (Player) sender;
        String dir = String.join(".", Arrays.copyOfRange(args, 0, args.length - 1));
        try {
            for (String key : plugin.config.getConfigurationSection(dir).getKeys(false)) {
                list.add(key);
            }
        }
        catch (Exception e){}
        if (list.isEmpty()){
            try{list.add(plugin.config.getString(dir));}
            catch (Exception a){
            }
        }
        if (plugin.config.getConfigurationSection(dir) == null){
            return list;
        }
        try {
            if (args.length == 4 && !args[args.length-2].equals("permissions")) {
                List<String> enchantmentList = new ArrayList<String>();
                for (Enchantment enchantment : Enchantment.values()) {
                    enchantmentList.add(enchantment.getKey().getKey());
                }
                list.addAll(enchantmentList);
            }
        }
        catch (Exception e){}
        return list;
    }
}
