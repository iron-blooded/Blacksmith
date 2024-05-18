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

public class splitEnchantments implements CommandExecutor{
    private final Blacksmith plugin;
    public splitEnchantments(Blacksmith plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        ItemStack item_in_main_hand = player.getInventory().getItemInMainHand();
        ItemStack item_in_off_hand = player.getInventory().getItemInOffHand();
        if (plugin.checkItemForEnchant(item_in_off_hand, item_in_main_hand, player)){
            return false;
        }
        if (args.length == 1){
            Map<Enchantment, Integer> matchingEnchants = plugin.getMatchingEnchants(item_in_off_hand, item_in_off_hand);
            for (Enchantment ench : matchingEnchants.keySet()) {
                if (ench.getKey().getKey().equals(args[0])){
                    if (item_in_main_hand.getEnchantments().containsKey(ench)){
                        sender.sendMessage("Предмет уже содержит данное зачарование.");
                        return false;
                    }
                    int level = matchingEnchants.get(ench);
                    double lvl_blacksmith = plugin.getChanceEnchant(player, ench, level);
                    if (lvl_blacksmith >= 0 || lvl_blacksmith == -2) {
                        plugin.transferEnchantment(ench, item_in_off_hand.clone(), item_in_main_hand, -1);
                        plugin.transferEnchantment(ench, item_in_off_hand.clone(), item_in_off_hand, -1);
                        sender.sendMessage("§aВы успешно разделили зачарования!");
                    }
                    else {
                        sender.sendMessage("Ваших навыков не хватает для этого.");
                    }
                    return true;
                }
            }
        }
        sender.sendMessage("Укажите корректное название зачарования");
        return false;
    }


}
