package org.gromoverzhec.gromoverzhec;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class command_enchant implements CommandExecutor {
    private final Enchantment enchantment;

    public command_enchant(Enchantment enchantment) {
        this.enchantment = enchantment;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        ItemStack item = player.getInventory().getItemInMainHand();
        item.addUnsafeEnchantment(enchantment, 1);
        return true;
    }
}
