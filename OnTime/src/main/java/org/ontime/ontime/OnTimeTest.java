package org.ontime.ontime;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OnTimeTest {
   private static OnTime _plugin;
   private static long defaultLastLogin;
   public HashMap testMap = new HashMap();

   public OnTimeTest(OnTime plugin) {
      _plugin = plugin;
      setDefaultLastLogin(Calendar.getInstance().getTimeInMillis());
   }

   HashMap getTestMap() {
      return this.testMap;
   }

   public void setTestMap(HashMap map) {
      this.testMap = map;
   }

   public static boolean loginTestPlayer(String playerName, Long timeOffset, String worldName) {
      PlayerData playerData = null;
      if (_plugin.get_logintime().playerIsOnline(Players.getData(playerName))) {
         return false;
      } else {
         long playingTime = 0L;
         if (!Players.hasOnTimeRecord(playerName)) {
            if (timeOffset >= 0L) {
               playingTime = TimeUnit.HOURS.toMillis(timeOffset);
            } else {
               playingTime = TimeUnit.HOURS.toMillis(-timeOffset);
            }

            UUID uuid = UUID.randomUUID();
            playerData = Players.getNew(uuid, playerName, Calendar.getInstance().getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), "testPlayer", playingTime);
            Players.putData(uuid, playerData);
         } else {
            playerData = Players.getData(playerName);
            if (playingTime != 0L) {
               Players.getWorldTime(playerData, OnTime.serverID).totalTime = playingTime;
            }
         }

         if (worldName != null) {
            PlayTimeData worldTime = Players.getWorldTime(playerData, worldName);
            if (worldTime == null) {
               Players.setWorldTime(playerData, worldName, playingTime, 0L, 0L, 0L, Calendar.getInstance().getTimeInMillis());
            }
         }

         _plugin.get_logintime().setLogin(playerData, TimeUnit.MINUTES.toMillis(Math.abs(timeOffset)));
         playerData.onLine = true;
         if (Players.getOfflinePlayerName(playerData.uuid) == null) {
            _plugin.get_ontimetest().getTestMap().put(playerData.uuid, playerName);
         }

         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            _plugin.get_dataio().savePlayerDataMySQL(playerName, true);
         }

         LogFile.write(1, "Login : (TEST) " + playerName);
         return true;
      }
   }

   public boolean logout(UUID uuid) {
      PlayerData playerData;
      if (!_plugin.get_ontimetest().getTestMap().containsKey(uuid)) {
         playerData = null;
         String playerName = Players.getOfflinePlayerName(uuid);
         if (playerName == null) {
            LogFile.write(1, "Logout : (TEST) failed for UUID:" + uuid + " Player name not found.");
            return false;
         } else {
            PlayerData playerData = Players.getData(uuid);
            if (_plugin.get_logintime().playerIsOnline(playerData)) {
               PlayTimeData worldTime = null;
               if ((worldTime = Players.getWorldTime(playerData, OnTime.serverID)) != null) {
                  LogFile.write(1, "Logout : (TEST) Attempted " + playerName + " : OnTime is " + Output.getTimeBreakdown(worldTime.totalTime, Output.TIMEDETAIL.SHORT));
               } else {
                  LogFile.write(1, "Logout : (TEST) Attempted " + playerName + " : OnTime N/A ");
               }

               _plugin.get_playereventlistener().logoutPlayer(playerData);
               return true;
            } else {
               return false;
            }
         }
      } else {
         playerData = Players.getData(uuid);
         _plugin.get_dataio().refreshPlayerDataMySQL(playerData);
         _plugin.get_playingtime().updateGlobal(playerData);
         playerData.onLine = false;
         _plugin.get_ontimetest().getTestMap().remove(uuid);
         LogFile.write(1, "Logout : (TEST) " + playerData.playerName + " : OnTime is " + Output.getTimeBreakdown(Players.getWorldTime(playerData, OnTime.serverID).totalTime, Output.TIMEDETAIL.SHORT));
         return true;
      }
   }

   public void logoutAll() {
      if (_plugin.get_ontimetest().getTestMap().size() != 0) {
         UUID[] keys = new UUID[_plugin.get_ontimetest().getTestMap().size()];
         _plugin.get_ontimetest().getTestMap().keySet().toArray(keys);

         for(int index = _plugin.get_ontimetest().getTestMap().size() - 1; index >= 0; --index) {
            _plugin.get_ontimetest().logout(keys[index]);
         }

      }
   }

   public long getDefaultLastLogin() {
      return defaultLastLogin;
   }

   public static void setDefaultLastLogin(long defaultLastLogin) {
      OnTimeTest.defaultLastLogin = defaultLastLogin;
   }
}
