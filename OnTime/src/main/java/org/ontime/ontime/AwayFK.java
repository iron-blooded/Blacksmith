package org.ontime.ontime;

import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import me.edge209.OnSign.OnSignHandler;
import me.edge209.OnTime.Rewards.RewardData;
import me.edge209.OnTime.Rewards.Rewards;
import me.edge209.afkTerminator.AfkTerminatorAPI;
import org.bukkit.entity.Player;

public class AwayFK {
   private static OnTime _plugin;

   public AwayFK(OnTime plugin) {
      _plugin = plugin;
   }

   public void notAFK(PlayerData playerData, Player player) {
      if (playerData == null) {
         playerData = Players.getData(player);
      }

      if (playerData != null) {
         if (playerData.afkData.firstActionTime == 0L) {
            playerData.afkData.firstActionTime = Calendar.getInstance().getTimeInMillis();
         }

         playerData.afkData.lastActionTime = Calendar.getInstance().getTimeInMillis();
         if (playerData.afkData.currentlyAFK) {
            this.backFromAFK(playerData);
         }
      }

   }

   public void checkAFK() {
      long now = Calendar.getInstance().getTimeInMillis();
      long shortestTime = TimeUnit.MINUTES.toMillis((long)OnTime.afkTime);
      Iterator var6 = _plugin.getServer().getOnlinePlayers().iterator();

      while(var6.hasNext()) {
         Player player = (Player)var6.next();
         LogFile.write(0, "{checkAFK} checking on " + OnTime.getPlayerName(player));
         if (_plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
            PlayerData playerData = Players.getData(player.getUniqueId());
            if (playerData != null && playerData.onLine && !this.checkPlayerAFK(playerData) && playerData.onLine && playerData.afkData.lastActionTime > 0L && playerData.afkData.lastActionTime + TimeUnit.MINUTES.toMillis((long)OnTime.afkTime) - now < shortestTime) {
               shortestTime = playerData.afkData.lastActionTime + TimeUnit.MINUTES.toMillis((long)OnTime.afkTime) - now;
            }
         }
      }

      if (TimeUnit.MILLISECONDS.toSeconds(shortestTime) < 5L) {
         shortestTime = TimeUnit.SECONDS.toMillis(5L);
      }

      _plugin.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new Runnable() {
         public void run() {
            AwayFK._plugin.get_awayfk().checkAFK();
         }
      }, shortestTime / 50L);
   }

   public boolean checkPlayerAFK(PlayerData playerData) {
      if (playerData == null) {
         return false;
      } else {
         String playerName = playerData.playerName;
         if (playerData.afkData.lastActionTime > 0L) {
            Long AFKStart = Calendar.getInstance().getTimeInMillis() - TimeUnit.MINUTES.toMillis((long)OnTime.afkTime);
            if (playerData.afkData.lastActionTime < AFKStart) {
               if (!playerData.afkData.currentlyAFK) {
                  this.playerNowAFK(playerData);
               } else if (OnTime.AfkTerminator && AfkTerminatorAPI.isAFKMachineDetected(playerName)) {
                  LogFile.console(3, "AFK Machine detected for " + playerName);
                  playerData.afkData.lastActionTime = AfkTerminatorAPI.getAFKMachineStartTime(playerName);
               }

               return true;
            } else if (OnTime.AfkTerminator && AfkTerminatorAPI.isAFKMachineDetected(playerName)) {
               playerData.afkData.lastActionTime = AfkTerminatorAPI.getAFKMachineStartTime(playerName);
               this.playerNowAFK(playerData);
               LogFile.console(3, "AFK Machine detected for " + playerName);
               return true;
            } else {
               return this.backFromAFK(playerData);
            }
         } else {
            return false;
         }
      }
   }

   public boolean backFromAFK(PlayerData playerData) {
      if (playerData == null) {
         return false;
      } else {
         String playerName = playerData.playerName;
         long laskAFKduration = this.updateAFKTime(playerData);
         if (laskAFKduration != 0L) {
            LogFile.write(2, playerName + " is no longer AFK.  AFK interval = " + Output.getTimeBreakdown(laskAFKduration, Output.TIMEDETAIL.SHORT) + " Total AFK (this login) = " + Output.getTimeBreakdown(playerData.afkData.totalAFKTime, Output.TIMEDETAIL.SHORT));
            LogFile.console(2, playerName + " is no longer AFK.  AFK interval = " + Output.getTimeBreakdown(laskAFKduration, Output.TIMEDETAIL.SHORT) + " Total AFK (this login) = " + Output.getTimeBreakdown(playerData.afkData.totalAFKTime, Output.TIMEDETAIL.SHORT));
            Output.broadcast(Output.OnTimeOutput, "output.broadcast.playerNotAFK", "ontime.afk.notify", playerName, (RewardData)null, 0);
            if (_plugin.isEnabled() && _plugin.getServer().getPlayer(playerData.uuid) != null && OnTime.rewardsEnable && _plugin.get_permissionsHandler().playerHas(Players.getOnlinePlayer(playerName), "ontime.track")) {
               _plugin.get_rewards().scheduleNextReward(playerName, (RewardData.timeScope)null);
               _plugin.get_rewards().scheduleRepeatingReward(playerData, -1);
               _plugin.get_rewards().scheduleIndiRewards(playerName, Rewards.indiScheduleSource.AFK);
            }

            if (OnTime.onlineTrackingEnable && Output.getOnlineFields().contains("afk")) {
               _plugin.get_dataio().updateOnlineReport("ontime-online", playerData);
            }

            if (OnTime.OnSign) {
               OnSignHandler.playerNOTAFK(Output.getMixedName(playerName));
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public void forceAllFromAFK() {
      Iterator var2 = _plugin.getServer().getOnlinePlayers().iterator();

      while(var2.hasNext()) {
         Player player = (Player)var2.next();
         this.notAFK((PlayerData)null, player);
      }

   }

   private void playerNowAFK(PlayerData playerData) {
      playerData.afkData.currentlyAFK = true;
      if (playerData.afkData.lastActionTime > playerData.afkData.lastRewardTime) {
         playerData.afkData.AFKStartTime = playerData.afkData.lastActionTime;
      } else {
         playerData.afkData.AFKStartTime = playerData.afkData.lastRewardTime + TimeUnit.SECONDS.toMillis(1L);
      }

      playerData.afkData.firstActionTime = 0L;
      _plugin.get_rewards().cancelPlayerRewardTasks(playerData.playerName, "all");
      LogFile.console(2, playerData.playerName + " is now AFK");
      LogFile.write(1, playerData.playerName + " is now AFK");
      if (OnTime.onlineTrackingEnable && Output.getOnlineFields().contains("afk")) {
         _plugin.get_dataio().updateOnlineReport("ontime-online", playerData);
      }

      if (OnTime.OnSign) {
         OnSignHandler.playerAFK(Output.getMixedName(playerData.playerName));
      }

      Output.broadcast(Output.OnTimeOutput, "output.broadcast.playerIsAFK", "ontime.afk.notify", playerData.playerName, (RewardData)null, 0);
   }

   public void updateLastReward(Player player, long time) {
      if (OnTime.afkCheckEnable) {
         Players.getData(player).afkData.lastRewardTime = time;
      }

   }

   public long updateAFKTime(PlayerData playerData) {
      if (playerData.afkData.currentlyAFK) {
         long lastDuration = playerData.afkData.firstActionTime - playerData.afkData.AFKStartTime;
         AwayFKData var10000 = playerData.afkData;
         var10000.totalAFKTime += lastDuration;
         playerData.afkData.currentlyAFK = false;
         return lastDuration;
      } else {
         return 0L;
      }
   }

   public long getAFKTime(PlayerData playerData) {
      long currentAFKTime = 0L;
      if (OnTime.afkCheckEnable && playerData != null) {
         if (playerData.afkData.currentlyAFK) {
            currentAFKTime = playerData.afkData.totalAFKTime + Calendar.getInstance().getTimeInMillis() - playerData.afkData.AFKStartTime;
         } else {
            currentAFKTime = playerData.afkData.totalAFKTime;
         }
      }

      return currentAFKTime;
   }

   public void resetAFKTime(PlayerData playerData) {
      if (OnTime.afkCheckEnable && playerData != null) {
         playerData.afkData.totalAFKTime = 0L;
         playerData.afkData.currentlyAFK = false;
         playerData.afkData.firstActionTime = Calendar.getInstance().getTimeInMillis();
         playerData.afkData.lastActionTime = Calendar.getInstance().getTimeInMillis();
      }

   }

   public void update(PlayerData playerData) {
      if (playerData != null && OnTime.afkCheckEnable) {
         try {
            if (_plugin.get_permissionsHandler().playerHas(playerData.playerName, "ontime.afk.collect")) {
               AwayFKData var10000 = playerData.afkData;
               var10000.todayAFKTime += playerData.afkData.totalAFKTime;
               var10000 = playerData.afkData;
               var10000.weekAFKTime += playerData.afkData.totalAFKTime;
               var10000 = playerData.afkData;
               var10000.monthAFKTime += playerData.afkData.totalAFKTime;
            }
         } catch (Exception var3) {
            LogFile.write(11, "{AwayFK.update} Update failed for " + playerData.playerName + " Error:" + var3.getMessage());
         }
      }

   }
}
