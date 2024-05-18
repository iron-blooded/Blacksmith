package org.fishingspots.fishingspots;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class AddItemCommand implements CommandExecutor {

    private final FishingSpots plugin;

    public AddItemCommand(FishingSpots plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        sender.sendMessage(label);
        return true;
    }
}