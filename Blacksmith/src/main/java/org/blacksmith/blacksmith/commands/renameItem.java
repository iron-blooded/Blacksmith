package org.blacksmith.blacksmith.commands;
import org.blacksmith.blacksmith.Blacksmith;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

public class renameItem implements CommandExecutor{
    private final Blacksmith plugin;
    public renameItem(Blacksmith plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        ItemStack item_in_main_hand = player.getInventory().getItemInMainHand();
        if (item_in_main_hand.getType() == Material.AIR){
            return false;
        }
//        ItemStack item_in_off_hand = player.getInventory().getItemInOffHand();
        Map<Enchantment, Integer> matchingEnchants = plugin.getMatchingEnchants(item_in_main_hand, item_in_main_hand);
        String name = item_in_main_hand.getItemMeta().getDisplayName();
        for (Enchantment ench : matchingEnchants.keySet()) {
             name = ench.getName() + " " + matchingEnchants.get(ench);
        }
        ItemMeta itemMeta = item_in_main_hand.getItemMeta();
        itemMeta.setDisplayName(name);
        item_in_main_hand.setItemMeta(itemMeta);
        return true;
    }
}
