package org.gromoverzhec.gromoverzhec;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ThunderEnchantListener implements Listener {
    private final Gromoverzhec plugin;

    public ThunderEnchantListener(Gromoverzhec plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTridentHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Trident) {
            Trident trident = (Trident) event.getDamager();
            ItemStack tridentItem = trident.getItem();
            boolean hasEnchant = false;
            if (tridentItem != null && tridentItem.hasItemMeta()) {
                for (Enchantment enchantment : tridentItem.getItemMeta().getEnchants().keySet()){
                    if (enchantment.getName() == "thunder_enchant"){
                        hasEnchant = true;
                    }

                }
            }
            if (hasEnchant){
//            if (tridentItem.getEnchantmentLevel(Enchantment.getByKey(NamespacedKey.minecraft("thunder_enchant"))) > 0) {
                Location location = trident.getLocation();
                trident.getWorld().strikeLightning(location);
//                new BukkitRunnable() {
//                    @Override
//                    public void run() {
//                        for (Entity entity : event.getDamager().getNearbyEntities(5, 5, 5)) {
//                            Location playerLocation = event.getDamager().getLocation();
//                            int radius = 5;
//                            for (int x = -radius; x <= radius; x++) {
//                                for (int y = -radius; y <= radius; y++) {
//                                    for (int z = -radius; z <= radius; z++) {
//                                        Location location = playerLocation.clone().add(x, y, z);
//                                        if (location.getBlock().getType() == Material.FIRE) {
//                                            location.getBlock().setType(Material.AIR);
//                                        }
//                                    }
//                                }
//                            }
//
//                        }
//                        // Ваше действие, которое нужно выполнить через секунду
//                    }
//                }.runTaskLater(this.plugin, 20L); // 20 тиков = 1 секунда
//            }
            }
        }
    }
}

