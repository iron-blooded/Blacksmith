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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

public class editConfig implements CommandExecutor {
    private final Blacksmith plugin;
    public editConfig(Blacksmith plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        String dir = String.join(".", Arrays.copyOfRange(args, 0, args.length - 1));
        try {
            float ar = Float.valueOf(args[args.length - 1]);
            plugin.config.set(dir, ar);
        }
        catch (Exception e){
            sender.sendMessage("Хуйню ты какую то понаписал");
            return false;
        }
        try {
            plugin.saveDefaultConfig();
            plugin.saveConfig();
            plugin.config = plugin.getConfig();
        } catch (Exception e) {
            sender.sendMessage("конфиг сохранить не удалось ;(");
            return false;
        }
        sender.sendMessage("конфиг успешно сохранен");
        return true;
    }
}
