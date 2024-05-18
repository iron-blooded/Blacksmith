package org.ontime.ontime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ontime.ontime.OnTime;
import org.ontime.ontime.PlayerData;

public class LoginTime {
   private static OnTime _plugin;

   public LoginTime(OnTime plugin) {
      _plugin = plugin;
   }

   public long lastLogin(me.edge209.OnTime.PlayerData playerData, String worldName) {
      me.edge209.OnTime.PlayTimeData worldData = null;
      if (playerData != null) {
         if ((worldData = me.edge209.OnTime.Players.getWorldTime(playerData, worldName)) != null && worldData.lastLogin != 0L) {
            return worldData.lastLogin;
         }

         if (playerData.uuid != null && worldName.equalsIgnoreCase(OnTime.serverID) && _plugin.getServer().getOfflinePlayer(playerData.uuid).hasPlayedBefore()) {
            return _plugin.getServer().getOfflinePlayer(playerData.uuid).getLastPlayed();
         }
      }

      return 0L;
   }

   public long getFirstLogin(PlayerData playerData) {
      if (playerData != null && playerData.firstLogin != 0L) {
         return playerData.firstLogin;
      } else {
         return playerData.uuid != null && _plugin.getServer().getOfflinePlayer(playerData.uuid).hasPlayedBefore() ? _plugin.getServer().getOfflinePlayer(playerData.uuid).getFirstPlayed() : 0L;
      }
   }

   public boolean newDay(long oldDate) {
      if (oldDate == 0L) {
         return true;
      } else {
         long oldDay = TimeUnit.MILLISECONDS.toDays(oldDate);
         long today = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis());
         return oldDay != today;
      }
   }

   public long setLogin(PlayerData playerData, long timeOffset) {
      if (playerData == null) {
         return 0L;
      } else {
         long currentTime = Calendar.getInstance().getTimeInMillis() - timeOffset;
         long secs = TimeUnit.MILLISECONDS.toSeconds(currentTime);
         long millisec = currentTime - TimeUnit.SECONDS.toMillis(secs);
         long loginTime = currentTime - millisec;
         long daysOn = 0L;
         long lastLogin = 0L;
         daysOn = (long)playerData.daysOn;
         lastLogin = Players.getWorldTime(playerData, OnTime.serverID).lastLogin;
         LogFile.console(0, "Last Login set for " + playerData.playerName + " from playerData at " + (new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss:SSSS] ")).format(lastLogin));
         boolean dayAdded = false;
         if (this.newDay(lastLogin)) {
            long daysAgo = (long)this.getDaysAgo(playerData);
            if (daysOn <= daysAgo) {
               ++daysOn;
               LogFile.write(1, "Incrementing days on for " + playerData.playerName + " Now set to : " + daysOn);
               dayAdded = true;
               _plugin.get_rewards().checkSpecialRewards(playerData.playerName, (int)daysOn);
            } else if (this.getDaysAgo(playerData) > 0) {
               LogFile.write(10, "{setLogin} OnTime is attempting to increment DaysOn (" + daysOn + ") greater than DaysAgo (" + this.getDaysAgo(playerData) + ") for " + playerData.playerName);
            }
         }

         Players.getWorldTime(playerData, OnTime.serverID).lastLogin = loginTime;
         playerData.daysOn = (int)daysOn;
         Player player;
         if (OnTime.perWorldEnable) {
            player = null;
            PlayTimeData worldTime = null;
            if ((player = Players.getOnlinePlayer(playerData.uuid)) != null) {
               if ((worldTime = Players.getWorldTime(playerData, player.getWorld().getName())) != null) {
                  worldTime.lastLogin = loginTime;
               } else {
                  Players.setWorldTime(playerData, player.getWorld().getName(), 0L, 0L, 0L, 0L, loginTime);
               }
            }
         }

         if (OnTime.multiServer) {
            player = null;
            if (Players.getWorldTime(playerData, OnTime.multiServerName) != null) {
               Players.getWorldTime(playerData, OnTime.multiServerName).lastLogin = loginTime;
            }
         }

         if (dayAdded) {
            _plugin.get_rewards().checkSpecialRewards(playerData.playerName, (int)daysOn);
         }

         LogFile.console(0, "Setting login time for " + playerData.playerName + " at : " + (new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss:SSSS] ")).format(loginTime) + " and daysOn at: " + playerData.daysOn);
         return loginTime;
      }
   }

   public long stripCount(long login) {
      long sec = TimeUnit.MILLISECONDS.toSeconds(login);
      long count = login - TimeUnit.SECONDS.toMillis(sec);
      return login - count;
   }

   public long getCount(long login) {
      long sec = TimeUnit.MILLISECONDS.toSeconds(login);
      return login - TimeUnit.SECONDS.toMillis(sec);
   }

   public boolean setDays(CommandSender sender, PlayerData playerData, int newDaysOn) {
      if (newDaysOn > this.getDaysAgo(playerData)) {
         sender.sendMessage("Warning: You have set 'DaysOn' to a value greater than the number of days (" + this.getDaysAgo(playerData) + ") since the player joined. ");
      }

      playerData.daysOn = newDaysOn;
      if (OnTime.dataStorage == DataIO.datastorage.MYSQL && !_plugin.get_logintime().playerIsOnline(playerData)) {
         _plugin.get_dataio().savePlayerDataMySQL(playerData, true);
      }

      return true;
   }

   public Long current(PlayerData playerData) {
      return this.current(playerData, (String)null);
   }

   public Long current(PlayerData playerData, String worldName) {
      if (playerData == null) {
         return 0L;
      } else if (!_plugin.get_logintime().playerIsOnline(playerData)) {
         return 0L;
      } else {
         Player player = Players.getOnlinePlayer(playerData.uuid);
         if (player == null && worldName == null) {
            worldName = OnTime.serverID;
         } else if (worldName == null) {
            if (OnTime.perWorldEnable) {
               worldName = player.getWorld().getName();
            } else {
               worldName = OnTime.serverID;
            }
         }

         long currentTime = 0L;
         PlayTimeData worldTime = Players.getWorldTime(playerData, worldName);
         if (worldTime == null) {
            return 0L;
         } else {
            currentTime = Calendar.getInstance().getTimeInMillis() - worldTime.lastLogin - _plugin.get_awayfk().getAFKTime(playerData);
            if (currentTime < 0L) {
               LogFile.write(10, "{LoginTime.current} " + playerData.playerName + " current time < 0 (" + currentTime + "); worldTime.lastLogin:" + worldTime.lastLogin + "; afkTime:" + _plugin.get_awayfk().getAFKTime(playerData));
               currentTime = 0L;
            }

            return currentTime;
         }
      }
   }

   public boolean playerIsOnline(PlayerData playerData) {
      UUID uuid = null;
      if (playerData != null) {
         uuid = playerData.uuid;
         if (uuid == null) {
            return false;
         }

         if (playerData.onLine) {
            return true;
         }
      }

      if (!OnTime.suspendOnTime) {
         Player player = _plugin.getServer().getPlayer(uuid);
         if (player == null) {
            return false;
         }

         if (player.isOnline() && _plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
            if (playerData.loginPending) {
               return false;
            }

            LogFile.write(11, "{playerIsOnline} Found player NOT online per OnTime record, but is online per server record. Logging in " + playerData.playerName);
            _plugin.get_playereventlistener().loginPlayer(player, false);
         }
      }

      return false;
   }

   public int getDaysAgo(PlayerData playerData) {
      long daysAgo = -1L;
      if (playerData != null) {
         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            daysAgo = playerData.firstLogin;
         } else if (playerData.uuid != null && _plugin.getServer().getOfflinePlayer(playerData.uuid).hasPlayedBefore()) {
            daysAgo = _plugin.getServer().getOfflinePlayer(playerData.uuid).getFirstPlayed();
         }
      }

      return daysAgo < 0L ? -1 : (int)TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis() - daysAgo);
   }
}
