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

public class applyingEnchantmentCompletion implements TabCompleter{
    private final Blacksmith plugin;

    public applyingEnchantmentCompletion(Blacksmith plugin) {
        this.plugin = plugin;
    }
    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[] args){
        List<String> list = new ArrayList<String>();
        Player player = (Player) sender;
        ItemStack item_in_off_hand = player.getInventory().getItemInOffHand();
        if (args.length == 1) {
            Map<Enchantment, Integer> matchingEnchants = plugin.getMatchingEnchants(item_in_off_hand, item_in_off_hand);
            for (Enchantment ench : matchingEnchants.keySet()) {
                list.add(ench.getKey().getKey());
            }
            if (list.size() == 0) {
                list.add("нет_одинаковых_зачарований");
            }
        }
        return list;
    }
}
