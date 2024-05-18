package org.ontime.ontime;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityEventListener implements Listener {
   private static OnTime _plugin;

   public EntityEventListener(OnTime plugin) {
      _plugin = plugin;
   }

   @EventHandler
   public void onEntityDeath(EntityDeathEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         LogFile.console(0, player.getName() + " has died.");
         if (player != null) {
            _plugin.get_rewards().processDeathRewards(OnTime.getPlayerName(player));
         }
      }

   }
}
