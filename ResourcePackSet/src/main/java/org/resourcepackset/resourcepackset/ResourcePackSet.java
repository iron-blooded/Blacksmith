package org.resourcepackset.resourcepackset;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.resourcepackset.resourcepackset.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

public final class ResourcePackSet extends JavaPlugin implements Listener{
    public FileConfiguration config = null;
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        config = getConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        getCommand("rp_get").setTabCompleter(new rpGetCompletion(this));
        getCommand("rp_get").setExecutor(new rpGet(this));

        getCommand("rp_delete").setTabCompleter(new rpGetCompletion(this));
        getCommand("rp_delete").setExecutor(new rpDelete(this));

        getCommand("rp_set").setExecutor(new rpSet(this));
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (config.contains("main")) {
            player.setTexturePack(config.getString("main"));
        }
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
