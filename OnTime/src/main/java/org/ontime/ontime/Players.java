package org.ontime.ontime;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Players {
   private static OnTime _plugin;
   public static HashMap uuidMap = new HashMap();

   public Players(OnTime plugin) {
      _plugin = plugin;
   }

   public static HashMap getuuidMap() {
      return uuidMap;
   }

   public static boolean addUuidMap(String playerName, UUID uuid) {
      if (getuuidMap().containsKey(playerName.toLowerCase())) {
         if (uuid.compareTo((UUID)getuuidMap().get(playerName.toLowerCase())) == 0) {
            LogFile.write(0, "{addUuidMap} " + playerName + " already present in uuidMap");
         } else {
            LogFile.write(10, "[MULTIPLE UUID] " + playerName + " already has record under UUID: " + ((UUID)getuuidMap().get(playerName.toLowerCase())).toString() + " but has logged in with UUID: " + uuid.toString());
         }

         return false;
      } else {
         LogFile.write(0, "{addUuidMap} Adding " + playerName.toLowerCase() + " with UUID: " + uuid.toString());
         getuuidMap().put(playerName.toLowerCase(), uuid);
         return true;
      }
   }

   public static boolean removeUuidMap(String playerName) {
      if (getuuidMap().containsKey(playerName.toLowerCase())) {
         if (getuuidMap().remove(playerName.toLowerCase()) != null) {
            LogFile.write(0, "{removeUuidMap} " + playerName + " removed from uuidMap");
            return true;
         }

         LogFile.write(0, "{removeUuidMap} " + playerName + " failed removal from uuidMap");
      } else {
         LogFile.write(0, "{removeUuidMap} " + playerName + " not found in uuidMap. Removal attempt failed.");
      }

      return false;
   }

   public static UUID checkDuplicate(String playerName, UUID uuid) {
      if (getuuidMap().containsKey(playerName.toLowerCase()) && uuid.compareTo((UUID)getuuidMap().get(playerName.toLowerCase())) != 0) {
         LogFile.write(10, "[DUPLICATE USER NAME] " + playerName + " already has record under UUID: " + ((UUID)getuuidMap().get(playerName.toLowerCase())).toString() + " but has logged in with UUID: " + uuid.toString());
         return (UUID)getuuidMap().get(playerName.toLowerCase());
      } else {
         return null;
      }
   }

   public static PlayerData getNew(String playerName, long lastVoteDate, String ReferredBy) {
      return new PlayerData((UUID)null, playerName, (String)null, 0L, 0L, (String)null, lastVoteDate, 0, 0, 0, 0, 0, 0, 0, 0, ReferredBy, 0, 0, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
   }

   public static PlayerData getNew(String playerName, long firstLogin, long lastVoteDate, String ReferredBy) {
      return new PlayerData((UUID)null, playerName, (String)null, firstLogin, 0L, (String)null, lastVoteDate, 0, 0, 0, 0, 0, 0, 0, 0, ReferredBy, 0, 0, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
   }

   public static PlayerData getNew(UUID uuid, String playerName, long firstLogin, long playTime) {
      if (uuid != null) {
         addUuidMap(playerName, uuid);
      }

      return new PlayerData(uuid, playerName, (String)null, firstLogin, 0L, (String)null, 0L, 0, 0, 0, 0, 0, 0, 0, 0, (String)null, 0, 0, 0, playTime, 0L, 0L, 0L, 0L, 0L, 0L);
   }

   public static PlayerData getNew(UUID uuid, String playerName, long firstLogin, long lastLogin, String referredBy, long playTime) {
      if (uuid != null) {
         addUuidMap(playerName, uuid);
      }

      return new PlayerData(uuid, playerName, (String)null, firstLogin, lastLogin, (String)null, 0L, 0, 0, 0, 0, 0, 0, 0, 0, referredBy, 0, 0, 0, playTime, 0L, 0L, 0L, 0L, 0L, 0L);
   }

   public static PlayerData getNew(UUID uuid, String playerName, long lastLogin, int totalVotes, int dailyVotes, int weeklyVotes, int monthlyVotes, int daysOn, long playTime, long todayTime, long weekTime, long monthTime) {
      if (uuid != null) {
         addUuidMap(playerName, uuid);
      }

      return new PlayerData(uuid, playerName, (String)null, 0L, lastLogin, (String)null, 0L, totalVotes, dailyVotes, weeklyVotes, monthlyVotes, 0, 0, 0, 0, (String)null, 0, 0, daysOn, playTime, todayTime, weekTime, monthTime, 0L, 0L, 0L);
   }

   public static Player getOnlinePlayer(UUID uuid) {
      return uuid == null ? null : _plugin.getServer().getPlayer(uuid);
   }

   public static Player getOnlinePlayer(String playerName) {
      UUID uuid = null;
      if (playerHasData(playerName)) {
         uuid = getData(playerName).uuid;
      }

      return uuid == null ? null : _plugin.getServer().getPlayer(uuid);
   }

   public static OfflinePlayer getOfflinePlayer(UUID uuid) {
      return _plugin.getServer().getOfflinePlayer(uuid);
   }

   public static OfflinePlayer getOfflinePlayer(String playerName) {
      UUID uuid = null;
      if (playerHasData(playerName)) {
         uuid = getData(playerName).uuid;
      }

      return uuid == null ? null : _plugin.getServer().getOfflinePlayer(uuid);
   }

   public static String getOfflinePlayerName(UUID uuid) {
      return uuid == null ? null : _plugin.getServer().getOfflinePlayer(uuid).getName();
   }

   public static UUID getUUID(String playerName) {
      UUID uuid = null;
      if (playerHasData(playerName)) {
         uuid = getData(playerName).uuid;
      }

      if (uuid == null) {
         try {
            uuid = UUIDFetcher.getUUIDOf(playerName);
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }

      return uuid;
   }

   public static boolean hasOnTimeRecord(UUID uuid) {
      if (playerHasData(uuid)) {
         return true;
      } else {
         return OnTime.dataStorage == DataIO.datastorage.MYSQL ? _plugin.get_dataio().loadPlayerDataMySQL(uuid) : false;
      }
   }

   public static boolean hasOnTimeRecord(String query) {
      if (playerHasData(query)) {
         return true;
      } else {
         return OnTime.dataStorage == DataIO.datastorage.MYSQL ? _plugin.get_dataio().loadPlayerDataMySQL(query) : false;
      }
   }

   public static boolean playerHasData(UUID uuid) {
      return _plugin.get_dataio().getPlayerMap().containsKey(uuid.toString());
   }

   public static boolean playerHasData(String query) {
      LogFile.write(-1, "{playerHasData} Looking for '" + query + "'");
      if (_plugin.get_dataio().getPlayerMap().containsKey(query.toLowerCase())) {
         LogFile.write(-1, "{playerHasData} Found '" + query + "'");
         return true;
      } else {
         if (getuuidMap().containsKey(query.toLowerCase())) {
            LogFile.write(-1, "{playerHasData} Converting " + query + " to UUID ");
            if (_plugin.get_dataio().getPlayerMap().containsKey(((UUID)getuuidMap().get(query.toLowerCase())).toString())) {
               LogFile.write(-1, "{playerHasData} Found playerRecord via UUID for  " + query + "(" + ((UUID)getuuidMap().get(query.toLowerCase())).toString() + ")");
               return true;
            }

            LogFile.write(-1, "{playerHasData} Found UUID for  " + query + "(" + ((UUID)getuuidMap().get(query.toLowerCase())).toString() + ") but no player Record");
         }

         LogFile.write(-1, "{playerHasData} DID NOT FIND '" + query + "'");
         return false;
      }
   }

   public static PlayerData getData(Player player) {
      return _plugin.get_dataio().getPlayerMap().containsKey(player.getUniqueId().toString()) ? (PlayerData)_plugin.get_dataio().getPlayerMap().get(player.getUniqueId().toString()) : getData(player.getName());
   }

   public static PlayerData getData(UUID uuid) {
      return _plugin.get_dataio().getPlayerMap().containsKey(uuid.toString()) ? (PlayerData)_plugin.get_dataio().getPlayerMap().get(uuid.toString()) : null;
   }

   public static PlayerData getData(String query) {
      if (_plugin.get_dataio().getPlayerMap().containsKey(query.toLowerCase())) {
         return (PlayerData)_plugin.get_dataio().getPlayerMap().get(query.toLowerCase());
      } else {
         return getuuidMap().containsKey(query.toLowerCase()) && _plugin.get_dataio().getPlayerMap().containsKey(((UUID)getuuidMap().get(query.toLowerCase())).toString()) ? (PlayerData)_plugin.get_dataio().getPlayerMap().get(((UUID)getuuidMap().get(query.toLowerCase())).toString()) : null;
      }
   }

   public static boolean putData(UUID uuid, PlayerData playerData) {
      if (uuid != null) {
         if (_plugin.get_dataio().getPlayerMap().containsKey(playerData.playerName.toLowerCase())) {
            remove(playerData.playerName.toLowerCase());
         }

         LogFile.write(0, "{putData} Added map for " + playerData.playerName + " under UUID:" + uuid.toString());
         _plugin.get_dataio().getPlayerMap().put(uuid.toString(), playerData);
         return true;
      } else {
         putData(playerData.playerName.toLowerCase(), playerData);
         LogFile.write(0, "{putData} Added map for " + playerData.playerName + " under name:" + playerData.playerName.toLowerCase());
         return true;
      }
   }

   public static boolean putData(String playerName, PlayerData playerData) {
      _plugin.get_dataio().getPlayerMap().put(playerName.toLowerCase(), playerData);
      return true;
   }

   public static void remove(UUID uuid) {
      _plugin.get_dataio().getPlayerMap().remove(uuid.toString());
   }

   public static boolean remove(String query) {
      if (_plugin.get_dataio().getPlayerMap().remove(query.toLowerCase()) != null) {
         return true;
      } else {
         return getuuidMap().containsKey(query.toLowerCase()) && _plugin.get_dataio().getPlayerMap().remove(((UUID)getuuidMap().get(query.toLowerCase())).toString()) != null;
      }
   }

   public static PlayTimeData getWorldTime(PlayerData playerData, String worldName) {
      return playerData != null && playerData.worldTime != null && playerData.worldTime.containsKey(worldName) ? (PlayTimeData)playerData.worldTime.get(worldName) : null;
   }

   public static boolean putWorldTime(PlayerData playerData, String worldName, PlayTimeData worldData) {
      if (playerData != null) {
         if (worldName != null) {
            if (worldData != null) {
               playerData.worldTime.put(worldName, worldData);
               return true;
            } else {
               LogFile.write(10, "{putWorldTime} worldData null for " + playerData.playerName + " for world " + worldName);
               return false;
            }
         } else {
            playerData.worldTime.put(worldName, worldData);
            LogFile.write(11, "{putWorldTime} worldName null for " + playerData.playerName + " for worldData " + worldData.toString());
            return true;
         }
      } else {
         LogFile.write(11, "{putWorldTime} playerData null but world: " + worldName);
         return false;
      }
   }

   public static void setWorldTime(PlayerData playerData, String worldName, long totalTime, long todayTime, long weekTime, long monthTime, long lastLogin) {
      PlayTimeData worldData = null;
      if ((worldData = getWorldTime(playerData, worldName)) != null) {
         worldData.totalTime = totalTime;
         worldData.todayTime = todayTime;
         worldData.weekTime = weekTime;
         worldData.monthTime = monthTime;
         if (lastLogin > 0L) {
            worldData.lastLogin = lastLogin;
         }
      } else {
         worldData = new PlayTimeData(totalTime, todayTime, weekTime, monthTime, lastLogin);
         if (putWorldTime(playerData, worldName, worldData)) {
            LogFile.write(1, "Added world '" + worldName + "' tracking data for " + playerData.playerName);
         } else {
            LogFile.write(3, "{setWorldTime} Error Added world '" + worldName + "' tracking data for " + playerData.playerName);
         }
      }

   }
}
