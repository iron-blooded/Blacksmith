package org.ontime.ontime;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.Rewards.RewardData;
import me.edge209.OnTime.Rewards.Rewards;
import me.edge209.afkTerminator.AfkTerminatorAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventListener implements Listener {
   private static OnTime _plugin;
   public static final List tekkitFakePlayers = Arrays.asList("buildcraft", "computercraft", "industrialcraft", "redpower", "railcraft");

   public PlayerEventListener(OnTime plugin) {
      _plugin = plugin;
   }

   public void loginPlayer(Player player, boolean newlogin) {
      PlayerData playerData = null;
      if ((playerData = Players.getData(player)) != null) {
         if (playerData.loginPending) {
            LogFile.write(0, "{loginPlayer} Login already pending for " + player.getName() + ". Login aborted.");
            return;
         }

         playerData.loginPending = true;
      }

      try {
         _plugin.getServer().getScheduler().runTaskLaterAsynchronously(_plugin, new loginPlayerFunc(player, newlogin), 20L);
      } catch (NoSuchMethodError var5) {
         _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new loginPlayerFunc(player, newlogin), 1L);
      }

   }

   public void delayChangeWorld(Player player, String fromWorld, boolean firstAttempt) {
      LogFile.write(10, "{delayChangeWorld} ChangeWorld processing delayed in order to give login event processing to complete for " + player.getName());

      try {
         _plugin.getServer().getScheduler().runTaskLaterAsynchronously(_plugin, new changeWorldFunc(player, fromWorld, firstAttempt), 20L);
      } catch (NoSuchMethodError var5) {
         _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new changeWorldFunc(player, fromWorld, firstAttempt), 1L);
      }

   }

   @EventHandler
   public void onPlayerEvent(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (player == null) {
         LogFile.console(2, "{onPlayerEvent (login)} 'player' passed by Bukkit was null.");
         LogFile.write(10, "Login Error: 'player' was null.");
      } else {
         String playerName = OnTime.getPlayerName(player);
         if (playerName == null) {
            LogFile.console(2, "{onPlayerEvent (login)} 'player' passed by Bukkit had 'null' playername.");
            LogFile.write(10, "Login Error:  'player' passed by Bukkit had 'null' playername.");
         } else if (tekkitFakePlayers.contains(playerName)) {
            LogFile.write(2, "Login Error: Ignoring Tekkit Fake Player : " + playerName);
            LogFile.console(0, "{onPlayerEvent(login)} Ignoring Tekkit Fake Player : " + playerName);
         } else {
            if (!OnTime.suspendOnTime) {
               this.loginPlayer(player, true);
            } else {
               LogFile.write(2, "Login Error: " + playerName + " no login due to OnTime suspend.");
            }

         }
      }
   }

   @EventHandler
   public void onPlayerEvent(PlayerKickEvent event) {
      Player player = event.getPlayer();
      if (player == null) {
         LogFile.console(2, "{onPlayerEvent (Quit)} 'player' passed by Bukkit was null.");
         LogFile.write(3, "{onPlayerEvent (Kick)} 'player' was null.");
      } else {
         LogFile.write(3, player.getName() + " was kicked from server.  Reason: " + event.getReason());
      }

   }

   @EventHandler
   public void onPlayerEvent(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      if (player == null) {
         LogFile.console(2, "{onPlayerEvent (Quit)} 'player' passed by Bukkit was null.");
         LogFile.write(3, "{onPlayerEvent (Quit)} 'player' was null.");
      } else if (OnTime.permission != null && _plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
         this.logoutPlayer(player);
         String playerName = OnTime.getPlayerName(player);
         if (playerName != null) {
            if (Players.playerHasData(playerName)) {
               LogFile.write(2, "Quit : " + playerName + " : OnTime is " + Output.getTimeBreakdown(Players.getWorldTime(Players.getData(player), OnTime.serverID).totalTime, Output.TIMEDETAIL.SHORT));
            } else {
               LogFile.write(2, "Quit : " + playerName + " No current OnTime record.");
            }
         } else {
            LogFile.write(3, "{PEL.quitevent} event player had null playerName.");
         }
      }

   }

   @EventHandler
   public void onPlayerEvent(PlayerMoveEvent event) {
      if (_plugin.get_permissionsHandler().playerHas(event.getPlayer(), "ontime.track") && OnTime.afkCheckEnable) {
         if (OnTime.AfkTerminator && AfkTerminatorAPI.isAFKMachineSuspected(event.getPlayer().getName())) {
            return;
         }

         _plugin.get_awayfk().notAFK((PlayerData)null, event.getPlayer());
      }

   }

   @EventHandler
   public void onPlayerEvent(PlayerInteractEvent event) {
      if (OnTime.afkCheckEnable) {
         _plugin.get_awayfk().notAFK((PlayerData)null, event.getPlayer());
      }

   }

   @EventHandler
   public void onPlayerEvent(PlayerChangedWorldEvent event) {
      if (!OnTime.suspendOnTime) {
         Player player = event.getPlayer();
         if (player == null) {
            LogFile.console(2, "{PlayerChangedWorldEvent} 'player' passed by Bukkit was null.");
            LogFile.write(10, "{PlayerChangedWorldEvent} 'player' was null.");
            return;
         }

         this._changeWorld(player, event.getFrom().getName(), true);
      }

   }

   private void _changeWorld(Player player, String fromWorld, boolean firstAttempt) {
      if (!OnTime.suspendOnTime) {
         if (player == null) {
            LogFile.console(2, "{_changeWorld} 'player' passed was null.");
            LogFile.write(10, "{_changeWorld} 'player' was null.");
            return;
         }

         if (_plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
            String playerName = OnTime.getPlayerName(player);
            PlayerData playerData = null;
            if ((playerData = Players.getData(player)) == null) {
               LogFile.write(10, "{_changeWorld} playerData record not found for " + player.getName());
               if (firstAttempt) {
                  this.delayChangeWorld(player, fromWorld, false);
               } else {
                  LogFile.write(10, "{_changeWorld} changeWorld processing failed for  " + player.getName() + ". PlayerData record still not found.");
               }

               return;
            }

            if (!playerData.onLine) {
               LogFile.write(10, "{_changeWorld} " + player.getName() + " was marked 'offline' ");
               if (firstAttempt) {
                  this.delayChangeWorld(player, fromWorld, false);
               } else {
                  LogFile.write(10, "{_changeWorld} changeWorld processing failed for  " + player.getName() + ". Player remained offline");
               }

               return;
            }

            if (OnTime.perWorldEnable) {
               _plugin.get_playingtime().updateWorld(playerData, fromWorld, Calendar.getInstance().getTimeInMillis());
               LogFile.console(0, "{_changeWorld} Updating world '" + fromWorld + "' for " + playerName);
               String worldName = player.getWorld().getName();
               PlayTimeData worldData = null;
               if ((worldData = Players.getWorldTime(playerData, worldName)) == null) {
                  Players.setWorldTime(playerData, worldName, 0L, 0L, 0L, 0L, Calendar.getInstance().getTimeInMillis());
                  LogFile.console(0, "{_changeWorld} Adding storage for world '" + worldName + "' for " + playerName);
               } else {
                  worldData.lastLogin = Calendar.getInstance().getTimeInMillis();
               }

               playerData.lastWorld = worldName;
               _plugin.get_rewards().cancelPlayerRewardTasks(playerName, "all");
               _plugin.get_rewards().scheduleNextReward(playerName, (RewardData.timeScope)null);
               _plugin.get_rewards().scheduleRepeatingReward(Players.getData(player), -1);
               _plugin.get_dataio().savePlayerDataMySQLAsync(playerName, true);
            } else if (OnTime.rewardsEnable && OnTime.permission.has(player, "ontime.rewards.receive")) {
               _plugin.get_rewards().scheduleIndiRewards(playerName, Rewards.indiScheduleSource.CHANGEWORLD);
            }
         }
      }

   }

   private void _loginPlayer(Player player, boolean newlogin) {
      String playerName = OnTime.getPlayerName(player);
      LogFile.write(0, "Starting login process for " + playerName);
      if (player.isOnline()) {
         if (_plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
            UUID uuid = player.getUniqueId();
            if (OnTime.dataStorage == DataIO.datastorage.MYSQL && !_plugin.get_dataio().loadPlayerDataMySQL(uuid)) {
               LogFile.write(1, "Could not load player from MySQL per uuid (" + uuid.toString() + ") Attempting load via playerName: " + playerName);
               _plugin.get_dataio().loadPlayerDataMySQL(OnTime.getPlayerName(player));
            }

            PlayerData playerData = null;
            UUID duplicateUuid = null;
            playerData = Players.getData(uuid);
            if (playerData != null && OnTime.uuidMergeEnable && OnTime.dataStorage == DataIO.datastorage.MYSQL && _plugin.get_import().mergeOrCleanRecords(Import.UUIDFUNC.MERGE, playerData, (PlayerData)null)) {
               LogFile.write(10, "Duplicate records for " + playerData.playerName + "(" + playerData.uuid.toString() + ") combined into single record.");
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  _plugin.get_dataio().loadPlayerDataMySQL(uuid);
               }
            }

            if ((duplicateUuid = Players.checkDuplicate(player.getName(), uuid)) != null) {
               LogFile.write(10, "Player Login Failed: " + player.getName() + " already has OnTime record under different UUID : (" + duplicateUuid.toString() + ")");
               if ((playerData = Players.getData(uuid)) != null) {
                  playerData.loginPending = false;
               }

               return;
            }

            if ((playerData = Players.getData(player)) == null) {
               playerData = Players.getNew(uuid, player.getName(), player.getFirstPlayed(), 0L);
               Players.putData(uuid, playerData);
               LogFile.write(1, "New PlayerData record created for " + playerName + " (" + uuid.toString() + ")");
            } else if (playerData.uuid == null) {
               playerData.uuid = uuid;
               Players.putData(uuid, playerData);
               Players.getuuidMap().put(playerName.toLowerCase(), uuid);
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "uuid", player.getUniqueId().toString(), playerName);
               }

               LogFile.write(1, "Saving missing UUID (" + player.getUniqueId().toString() + ") for " + playerName);
            } else if (!playerData.playerName.equalsIgnoreCase(player.getName())) {
               String newPlayerName = player.getName();
               LogFile.write(3, "Player with UUID " + uuid.toString() + " has changed name from " + playerData.playerName + " to " + newPlayerName);
               Players.removeUuidMap(playerData.playerName);
               Players.getuuidMap().put(newPlayerName.toLowerCase(), uuid);
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  _plugin.get_dataio().removePlayerFromTable(DataIO.REMOVEKEY.NAME_UUID, Players.getNew((UUID)null, Output.getMixedName(newPlayerName), 0L, 0L), OnTime.MySQL_table);
                  playerData.playerName = newPlayerName;
                  _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "playerName", newPlayerName, uuid);
               } else {
                  playerData.playerName = newPlayerName;
               }
            }

            if (playerData.onLine) {
               this.logoutPlayer(player);
               LogFile.write(10, "{_loginPlayer} " + playerName + " was still marked as OnLine by OnTime.  Logout executed.");
            }

            _plugin.get_awayfk().resetAFKTime(playerData);
            playerData.onLine = true;
            _plugin.get_logintime().setLogin(playerData, -TimeUnit.SECONDS.toMillis((long)OnTime.playerLoginDelay));
            if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
               _plugin.get_dataio().savePlayerDataMySQL(playerData, true);
            }

            playerData.lastWorld = player.getWorld().getName();
            if (newlogin) {
               LogFile.write(2, "Login : " + playerName + " Previous OnTime: " + Output.getTimeBreakdown(Players.getWorldTime(playerData, OnTime.serverID).totalTime, Output.TIMEDETAIL.SHORT));
            } else {
               LogFile.write(10, "Audit Re-Login : " + playerName + " Previous OnTime: " + Output.getTimeBreakdown(Players.getWorldTime(playerData, OnTime.serverID).totalTime, Output.TIMEDETAIL.SHORT));
            }

            if (OnTime.rewardsEnable) {
               _plugin.get_rewards().cancelPlayerRewardTasks(playerName, "all");
               _plugin.get_rewards().scheduleRewardTask(playerName, 0, 80L, _plugin.get_rewards().delayReward);
            }

            playerData.loginPending = false;

            try {
               _plugin.getServer().getScheduler().runTaskLaterAsynchronously(_plugin, new finishLoginEvent(player, newlogin), 100L);
            } catch (NoSuchMethodError var8) {
               _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new finishLoginEvent(player, newlogin), 100L);
            }
         } else {
            LogFile.write(1, playerName + " is not enabled for OnTime tracking.");
         }
      } else {
         LogFile.write(1, "{_loginPlayer} Player exited prior to online process complete. " + playerName + ". No further action taken.");
      }

   }

   public void finishLogin(Player player, boolean newlogin) {
      PlayerData playerData = Players.getData(player);
      if (player.isOnline()) {
         if (OnTime.rewardsEnable && OnTime.permission.has(player, "ontime.rewards.receive")) {
            _plugin.get_rewards().scheduleRepeatingReward(playerData, -1);
            _plugin.get_rewards().scheduleIndiRewards(playerData.playerName, Rewards.indiScheduleSource.LOGIN);
         }

         if (OnTime.messagesEnable && OnTime.welcomeEnable && newlogin) {
            _plugin.get_messages().generate("playerLogin", playerData.playerName, (String[])null);
            _plugin.get_messages().loginAnnouncement("loginAnnouncement", player);
         }

         if (OnTime.dataStorage == DataIO.datastorage.MYSQL && newlogin) {
            if (_plugin.get_permissionsHandler().playerHas(player, "ontime.top.exclude")) {
               playerData.permissions |= PlayerData.OTPerms.TOPTEN.mask();
            } else {
               playerData.permissions = playerData.permissions &= '\uffff' ^ PlayerData.OTPerms.TOPTEN.mask();
            }

            if (_plugin.get_permissionsHandler().playerHas(player, "ontime.purge.exclude")) {
               playerData.permissions |= PlayerData.OTPerms.PURGE.mask();
            } else {
               playerData.permissions = playerData.permissions &= '\uffff' ^ PlayerData.OTPerms.PURGE.mask();
            }

            if (playerData.hostName == null || playerData.hostName.length() == 0) {
               playerData.hostName = player.getAddress().getHostName();
               _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "hostName", player.getAddress().getHostName(), playerData.playerName);
               LogFile.write(1, playerData.playerName + " hostName stored to DB as " + player.getAddress().getHostName());
            }

            if (OnTime.onlineTrackingEnable) {
               if (_plugin.get_dataio().saveOnlineReport("ontime-online", playerData)) {
                  LogFile.console(0, "Created Online Record for " + playerData.playerName);
               } else {
                  LogFile.console(0, "Failed to create Online Record for " + playerData.playerName);
               }
            }
         }
      } else if (playerData.onLine) {
         LogFile.write(10, "{finishLogin} Player exited prior to online process complete." + playerData.playerName + " offline per server, but OnTime still had as OnLine. Logout Executed.");
         this.logoutPlayer(player);
      } else {
         LogFile.write(1, "{finishLogin} Player exited prior to online process complete. " + playerData.playerName + " offline per server, and OnTime status was offline. No further action taken.");
      }

      LogFile.console(0, "Login process completed for " + playerData.playerName);
   }

   public void logoutPlayer(Player player) {
      this.logoutPlayer(Players.getData(player));
   }

   public void logoutPlayer(PlayerData playerData) {
      if (playerData == null) {
         LogFile.write(10, "{OnTime.logoutPlayer} logout of 'null' playerData attempted.");
      } else if (!playerData.onLine) {
         LogFile.write(10, "{logout} " + playerData.playerName + " was not marked as 'online' by ontime.  Logout failed. No data update. ");
      } else {
         _plugin.get_awayfk().notAFK(playerData, (Player)null);
         _plugin.get_awayfk().update(playerData);
         _plugin.get_dataio().refreshPlayerDataMySQL(playerData);
         _plugin.get_playingtime().updateGlobal(playerData);
         _plugin.get_rewards().cancelPlayerRewardTasks(playerData.playerName, "all");
         playerData.onLine = false;

         String key;
         for(Iterator var3 = playerData.worldTime.keySet().iterator(); var3.hasNext(); Players.getWorldTime(playerData, key).rollOver = 0L) {
            key = (String)var3.next();
         }

         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            if (_plugin.isEnabled()) {
               _plugin.get_dataio().savePlayerDataMySQLAsync(playerData.playerName, true);
            } else {
               _plugin.get_dataio().savePlayerDataMySQL(playerData.playerName, true);
            }
         }

         if (OnTime.onlineTrackingEnable) {
            _plugin.get_dataio().removePlayerFromTable(DataIO.REMOVEKEY.PLAYER, playerData, "`ontime-online`");
         }

         LogFile.write(0, "{logoutPlayer} Successful for " + playerData.playerName);
      }
   }

   public void auditLogout() {
      Iterator var2 = _plugin.get_dataio().getPlayerMap().keySet().iterator();

      PlayerData playerData;
      while(var2.hasNext()) {
         String key = (String)var2.next();
         playerData = Players.getData(key);
         if (playerData.onLine && !_plugin.get_ontimetest().getTestMap().containsKey(playerData.uuid)) {
            Player player = null;
            if ((player = _plugin.getServer().getPlayer(playerData.uuid)) != null) {
               if (!_plugin.getServer().getPlayer(playerData.uuid).isOnline()) {
                  LogFile.write(10, "{auditLogout} Offline player " + playerData.playerName + " was OnLine per OnTime Records. Logging them out now.");
                  this.logoutPlayer(player);
               }
            } else {
               LogFile.write(10, "{auditLogout} Player with no server record: " + playerData.playerName + " was OnLine per OnTime Records. Logging them OUT now.");
               this.logoutPlayer(playerData);
            }
         }
      }

      var2 = _plugin.getServer().getOnlinePlayers().iterator();

      while(var2.hasNext()) {
         Player player = (Player)var2.next();
         if (_plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
            playerData = null;
            if (Players.hasOnTimeRecord(player.getUniqueId())) {
               if ((playerData = Players.getData(player.getUniqueId())) != null) {
                  if (!playerData.onLine) {
                     LogFile.write(10, "{auditLogout} OnLine player: " + player.getName() + " was NOT online per OnTime Records. Logging them IN now.");
                     this.loginPlayer(player, false);
                  }
               } else {
                  LogFile.write(10, "{auditLogout} Player: " + player.getName() + " (" + player.getUniqueId().toString() + ") has OnTime record, but could not find playerData. No action taken.");
               }
            } else {
               LogFile.write(1, "{auditLogout} OnLine player: " + player.getName() + " (" + player.getUniqueId().toString() + ") is online per server, but does not have OnTime record. No action taken.");
            }
         }
      }

   }

   public class changeWorldFunc implements Runnable {
      private Player player;
      private String fromWorld;
      private boolean firstAttempt;

      public changeWorldFunc(Player _player, String _fromWorld, boolean _firstAttempt) {
         this.player = _player;
         this.fromWorld = _fromWorld;
         this.firstAttempt = _firstAttempt;
      }

      public void run() {
         PlayerEventListener.this._changeWorld(this.player, this.fromWorld, this.firstAttempt);
      }
   }

   public class finishLoginEvent implements Runnable {
      private Player player;
      private boolean newlogin;

      public finishLoginEvent(Player _player, boolean _newlogin) {
         this.player = _player;
         this.newlogin = _newlogin;
      }

      public void run() {
         PlayerEventListener.this.finishLogin(this.player, this.newlogin);
      }
   }

   public class loginPlayerFunc implements Runnable {
      private Player player;
      private boolean newlogin;

      public loginPlayerFunc(Player _player, boolean _newlogin) {
         this.player = _player;
         this.newlogin = _newlogin;
      }

      public void run() {
         PlayerEventListener.this._loginPlayer(this.player, this.newlogin);
      }
   }
}
