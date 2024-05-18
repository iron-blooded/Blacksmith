package org.blacksmith.blacksmith.commands;
import org.blacksmith.blacksmith.Blacksmith;
import org.bukkit.Bukkit;
import org.bukkit.attribute.AttributeInstance;
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
import org.bukkit.attribute.Attribute;

public class getCount implements CommandExecutor {
    private final Blacksmith plugin;
    public getCount(Blacksmith plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player= (Player) sender;
        if (args.length >= 1){
            try {
                sender.sendMessage(String.valueOf(plugin.simpleSqlDatabase.getValue(args[0])));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        else {
            player.sendMessage(String.valueOf(player.getInventory().getItemInMainHand().getEnchantments()));
            sender.sendMessage("Укажите ник");
            return false;
        }
    }
}
