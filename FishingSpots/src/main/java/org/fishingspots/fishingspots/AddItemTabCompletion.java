package org.fishingspots.fishingspots;

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

public class AddItemTabCompletion implements TabCompleter {
    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[] args){
        List<String> l = new ArrayList<String>();
        switch (args.length){
            case 1: l.add("название_предмета");
            break;
            case 2: // материалы
                for(Material c : Material.values())
                    if (c.name().startsWith(args[1])) {
                        l.add(c.name());
                    }
                break;
            case 3: l.add("шанс_выпадения");
            break;
            case 4: l.add("подпись_в_лоре");
            break;
        }
        return l;
    }
}