package org.gromoverzhec.gromoverzhec;

import io.papermc.paper.enchantments.EnchantmentRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CustomEnchantment extends Enchantment {
    public CustomEnchantment(JavaPlugin plugin) {
        super(new NamespacedKey(plugin, "gromoverzhec"));
    }
//    @Override
//    public void onEntityDamage(Entity entity, Player player, int level) {
//        if (entity instanceof LightningStrike && player.getInventory().getItemInMainHand().containsEnchantment(this)) {
//            LightningStrike lightning = (LightningStrike) entity;
//            lightning.getWorld().strikeLightning(lightning.getLocation());
//        }
//    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        return item.getType() == Material.TRIDENT;
    }

    @Override
    public @NotNull Component displayName(int i) {
        return null;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public @NotNull EnchantmentRarity getRarity() {
        return null;
    }

    @Override
    public float getDamageIncrease(int i, @NotNull EntityCategory entityCategory) {
        return 0;
    }

    @Override
    public @NotNull Set<EquipmentSlot> getActiveSlots() {
        return null;
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
        return "Gromoverzhec";
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


    @Override
    public @NotNull String translationKey() {
        return null;
    }
}
