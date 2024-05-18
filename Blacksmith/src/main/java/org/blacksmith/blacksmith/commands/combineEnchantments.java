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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

public class combineEnchantments implements CommandExecutor{
    private final Blacksmith plugin;
    public combineEnchantments(Blacksmith plugin) {
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
            Map<Enchantment, Integer> matchingEnchants = plugin.getMatchingEnchants(item_in_main_hand, item_in_off_hand);
            for (Enchantment ench : matchingEnchants.keySet()) {
                if (ench.getKey().getKey().equals(args[0])){
                    int result = plugin.checkBlacksmithLVL(player, ench, matchingEnchants.get(ench));
                    Map<Enchantment, Integer> m = new HashMap<>();
                    m.put(ench, matchingEnchants.get(ench));
                    if (result >= 1) {
                        plugin.transferEnchantment(ench, item_in_off_hand, item_in_main_hand, 1);
                        sender.sendMessage("§aВы успешно объеденили зачарования!");
                        int lvl = 1;
                        if (plugin.rnd() <= 1/2400 && (//эф защ острота
                                ench == Enchantment.DIG_SPEED ||
                                ench == Enchantment.PROTECTION_ENVIRONMENTAL ||
                                ench == Enchantment.DAMAGE_ALL
                                )){
                          lvl += 1;
                        }
                        if (result == 1) {
                            try {
                                plugin.simpleSqlDatabase.sumNumInKey(player.getName(), lvl);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    else if (result == 0) {
                        item_in_off_hand.setAmount(0);
                        sender.sendMessage("§cВы допустили ошибку при слиянии!");
                    } else if (result == -1) {
                        sender.sendMessage("Шансы на успех слишком малы.");
                    }
                    return true;
                }
            }
        }
        sender.sendMessage("Укажите корректное название зачарования");
        return false;
    }
}

