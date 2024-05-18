package org.gromoverzhec.gromoverzhec;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class Gromoverzhec extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // Создаем новое зачарование
        Enchantment thunderEnchant = new EnchantmentWrapper("gromoverzhec") {
            @Override
            public boolean canEnchantItem(ItemStack item) {
                return item.getType() == Material.TRIDENT;
            }

            @Override
            public boolean conflictsWith(Enchantment other) {
                return false;
            }

            @Override
            public EnchantmentTarget getItemTarget() {
                return EnchantmentTarget.TRIDENT;
            }

            @Override
            public int getMaxLevel() {
                return 1;
            }

            @Override
            public String getName() {
                return "thunder_enchant";
            }

            @Override
            public int getStartLevel() {
                return 1;
            }

            @Override
            public boolean isTreasure() {
                return true;
            }

            @Override
            public boolean isCursed() {
                return false;
            }
        };

        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
        } catch (Exception e) {
//            e.printStackTrace();
        }
        // Регистрируем новое зачарование
        try {
            Enchantment.registerEnchantment(thunderEnchant);
        }catch (Exception e) {
//            e.printStackTrace();
        }


        // Настраиваем обработку событий
        getServer().getPluginManager().registerEvents(new ThunderEnchantListener(this), this);
//        getCommand("gromoverzhec_enchant").setExecutor(new command_enchant(thunderEnchant));
    }
    
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause() == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            event.setCancelled(true);
        }
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            Enchantment.stopAcceptingRegistrations();
        }catch (Exception e){}
    }

}
