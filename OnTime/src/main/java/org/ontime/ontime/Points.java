package org.ontime.ontime;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Points {
   private static OnTime _plugin;

   public Points(OnTime plugin) {
      _plugin = plugin;
   }

   public boolean pointsEnabled(CommandSender sender) {
      if (!OnTime.pointsEnable) {
         sender.sendMessage(ChatColor.RED + "'Points' is not enabled on this server.");
         return false;
      } else {
         return true;
      }
   }

   public boolean setPoints(String playerName, int points) {
      PlayerData playerData = null;
      if ((playerData = Players.getData(playerName)) == null) {
         return false;
      } else {
         if (!OnTime.negativePointsEnable && points < 0) {
            playerData.points = 0;
         } else {
            playerData.points = points;
         }

         return _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "points", playerData.points, playerName);
      }
   }

   public int addPoints(String playerName, int points) {
      PlayerData playerData = null;
      if ((playerData = Players.getData(playerName)) != null) {
         playerData.points += points;
         if (!OnTime.negativePointsEnable && playerData.points < 0) {
            playerData.points = 0;
         }

         return _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "points", playerData.points, playerName) ? playerData.points : -1;
      } else {
         return -1;
      }
   }
}
