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

public class setCount implements CommandExecutor {
    private final Blacksmith plugin;
    public setCount(Blacksmith plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2){
            try {
                plugin.simpleSqlDatabase.setValue(args[0], Integer.valueOf(args[1]));
                sender.sendMessage("Успешно!");
            } catch (SQLException e) {
                sender.sendMessage("Ошибка!");
            }
            return true;
        }
        else {
            sender.sendMessage("Укажите ник и число");
            return false;
        }
    }
}
