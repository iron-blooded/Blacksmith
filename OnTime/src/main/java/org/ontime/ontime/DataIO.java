package org.ontime.ontime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.Rewards.RewardData;
import me.edge209.OnTime.Rewards.RewardUtilities;
import me.edge209.mysqlib.NewMySQL;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class DataIO {
   private static OnTime _plugin;
   public static NewMySQL mysqlNew = new NewMySQL();
   public HashMap playerMap = new HashMap();
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$DataIO$REMOVEKEY;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$OnTimeAPI$data;

   public DataIO(OnTime plugin) {
      _plugin = plugin;
   }

   public HashMap getPlayerMap() {
      return this.playerMap;
   }

   public void savePlayerDataMySQLAsync(String playerName, boolean saveReports) {
      try {
         _plugin.getServer().getScheduler().runTaskAsynchronously(_plugin, new _savePlayerDataMySQLAsync(playerName, saveReports));
      } catch (NoSuchMethodError var4) {
         _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new _savePlayerDataMySQLAsync(playerName, saveReports), 1L);
      }

   }

   public void incrementPlayerVotesAsync(String playerName) {
      try {
         _plugin.getServer().getScheduler().runTaskAsynchronously(_plugin, new _incrementPlayerVotes(playerName));
      } catch (NoSuchMethodError var3) {
         _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new _incrementPlayerVotes(playerName), 1L);
      }

   }

   public void removePlayerFromTableAsync(REMOVEKEY key, PlayerData playerData, String table) {
      try {
         _plugin.getServer().getScheduler().runTaskAsynchronously(_plugin, new _removePlayerFromTable(key, playerData, table));
      } catch (NoSuchMethodError var5) {
         _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new _removePlayerFromTable(key, playerData, table), 1L);
      }

   }

   public boolean loadAllData(mysqlload loading) {
      boolean loadResults = false;
      if (OnTime.dataStorage == DataIO.datastorage.YML) {
         if (this.loadPlayerDataYML("playerdata.yml", (String)null, true, false) >= 0) {
            loadResults = true;
         } else {
            LogFile.console(3, "Data corruption in 'playerdata.yml' file.");
         }
      } else if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         if (loading == DataIO.mysqlload.INIT) {
            loadResults = this.loadTopPlayerDataMySQL(loading, OnTime.topListMax);
         } else {
            loadResults = this.loadTopPlayerDataMySQL(loading, -1);
         }
      } else {
         LogFile.console(3, "Invalid value for dataStorage (" + OnTime.dataStorage.ordinal() + ")");
      }

      return loadResults;
   }

   public boolean saveAllData(File folder) {
      if (OnTime.dataStorage == DataIO.datastorage.YML) {
         this.savePlayerDataYML(folder, "playerdata.yml");
         return true;
      } else if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         return this.saveAllPlayerDataMySQL((CommandSender)null, DataIO.mysqlsave.ONLINE, -1, -1, (String)null);
      } else {
         LogFile.console(1, "Invalid data storage method specified: " + OnTime.dataStorage);
         return false;
      }
   }

   public boolean removePlayerFromTable(REMOVEKEY key, PlayerData playerData, String table) {
      if (playerData != null) {
         try {
            if (!mysqlNew.checkMySQLConnection()) {
               return false;
            }

            switch (key) {
               case PLAYERNAME:
                  mysqlNew.query("DELETE FROM " + table + " WHERE playername='" + playerData.playerName + "'");
                  break;
               case PLAYER:
                  mysqlNew.query("DELETE FROM " + table + " WHERE player='" + playerData.playerName + "'");
                  break;
               case UUID:
                  if (playerData.uuid == null) {
                     return false;
                  }

                  mysqlNew.query("DELETE FROM " + table + " WHERE uuid='" + playerData.uuid.toString() + "'");
                  break;
               case NAME_UUID:
                  if (playerData.uuid == null) {
                     mysqlNew.query("DELETE FROM " + table + " WHERE playername='" + playerData.playerName + "' AND (uuid IS NULL OR uuid='null')");
                  } else {
                     mysqlNew.query("DELETE FROM " + table + " WHERE playername='" + playerData.playerName + "' AND uuid='" + playerData.uuid.toString() + "'");
                  }
                  break;
               case NAME_UUID_ID:
                  if (playerData.uuid == null) {
                     mysqlNew.query("DELETE FROM " + table + " WHERE id=" + playerData.mysqlID + " AND playername='" + playerData.playerName + "' AND (uuid IS NULL OR uuid='null')");
                  } else {
                     mysqlNew.query("DELETE FROM " + table + " WHERE id=" + playerData.mysqlID + " AND playername='" + playerData.playerName + "' AND uuid='" + playerData.uuid.toString() + "' AND world='" + OnTime.serverID + "'");
                  }
            }

            return true;
         } catch (SQLException var5) {
            LogFile.console(1, "{removePlayerFromTable} Error Deleting " + playerData.playerName + " from  '" + table + "' : " + var5.getMessage());
         }
      } else {
         LogFile.write(10, "{removePlayerFromTable} playerData was NULL");
      }

      return false;
   }

   public void removePlayerCompletely(REMOVEKEY key, PlayerData playerData) {
      if (!playerData.playerName.equalsIgnoreCase("ontime-data")) {
         this.removePlayerFromAllMaps(playerData.playerName);
         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            this.removePlayerFromTableAsync(key, playerData, OnTime.MySQL_table);
         }
      }

   }

   public void savePlayerDataYML(File folder, String fileName) {
      File outfile = null;
      if (folder.equals(OnTime.onTimeDataFolder)) {
         outfile = new File(OnTime.onTimeDataFolder, "playerData_temp.yml");
      } else {
         outfile = new File(folder, fileName);
      }

      File infile = new File(OnTime.onTimeDataFolder, "playerdata.yml");
      RewardUtilities.createFile(outfile);
      int count = 0;

      try {
         if (!infile.exists()) {
            _plugin.copy(_plugin.getResource("playerdata.yml"), outfile);
         } else {
            InputStream in = new FileInputStream(infile);
            BufferedWriter out = new BufferedWriter(new FileWriter(outfile, true));
            BufferedReader inStream = new BufferedReader(new InputStreamReader(in));
            String line = inStream.readLine();

            while(!line.contains("playerDataVersion")) {
               out.write(line);
               out.newLine();
               line = inStream.readLine();
               if (line == null) {
                  break;
               }
            }

            out.write("playerDataVersion: 6");
            out.newLine();
            if (line != null) {
               line = inStream.readLine();
            }

            if (line != null) {
               out.write(line);
               out.newLine();
            }

            out.write("#");
            out.newLine();
            in.close();
            out.close();
         }

         writeLine(outfile, "players:");
         if (!_plugin.get_dataio().getPlayerMap().isEmpty()) {
            Iterator var24 = _plugin.get_dataio().getPlayerMap().keySet().iterator();

            while(var24.hasNext()) {
               String key = (String)var24.next();
               PlayerData playerData = Players.getData(key);
               PlayTimeData serverTime = Players.getWorldTime(playerData, OnTime.serverID);
               Long playingTime = serverTime.totalTime;
               Long todayTime = serverTime.todayTime;
               Long weekTime = serverTime.weekTime;
               Long monthTime = serverTime.monthTime;
               Long loginTime = serverTime.lastLogin;
               Integer totalVotes = playerData.totalVotes;
               Integer dailyVotes = playerData.dailyVotes;
               Integer weeklyVotes = playerData.weeklyVotes;
               Integer monthlyVotes = playerData.monthlyVotes;
               String playerName = playerData.playerName;
               String uuid_S = null;
               if (playerData.uuid != null) {
                  uuid_S = playerData.uuid.toString();
               }

               Integer daysOn = 0;
               daysOn = playerData.daysOn;
               if (key.length() > 0) {
                  writeLine(outfile, "   player" + count + ": '" + uuid_S + "," + playerName + "," + playingTime.toString() + "," + loginTime.toString() + "," + todayTime.toString() + "," + weekTime.toString() + "," + monthTime.toString() + "," + totalVotes.toString() + "," + dailyVotes.toString() + "," + weeklyVotes.toString() + "," + monthlyVotes.toString() + "," + daysOn.toString() + "'");
                  ++count;
               } else {
                  LogFile.write(3, "{savePlayerDataYML} key was empty string, so data not saved.");
               }
            }
         }

         writeLine(outfile, "#");
         writeLine(outfile, "TodayDate: " + String.valueOf(OnTime.todayStart));
         writeLine(outfile, "#");
         writeLine(outfile, "WeekStartDate: " + String.valueOf(OnTime.weekStart));
         writeLine(outfile, "#");
         writeLine(outfile, "MonthStartDate: " + String.valueOf(OnTime.monthStart));
         if (folder.equals(OnTime.onTimeDataFolder)) {
            infile.delete();
            outfile.renameTo(infile);
            outfile = infile;
         }

         LogFile.write(2, outfile.getName() + " updated.");
      } catch (Exception var22) {
         var22.printStackTrace();
      }

   }

   public void clearAllMaps() {
      _plugin.get_dataio().getPlayerMap().clear();
   }

   public void removePlayerFromAllMaps(String playerName) {
      Players.remove(playerName);
      Players.removeUuidMap(playerName);
   }

   public static long getPlayerTimeData(String playerName, OnTimeAPI.data data) {
      if (PlayingTime.playerHasOnTimeRecord(playerName)) {
         PlayerData playerData = Players.getData(playerName);
         Long currentPlayTime = _plugin.get_logintime().current(playerData);
         PlayTimeData serverTime = Players.getWorldTime(playerData, OnTime.serverID);
         switch (data) {
            case TOTALPLAY:
               return _plugin.get_playingtime().totalOntime(playerName);
            case TODAYPLAY:
               return serverTime.todayTime + currentPlayTime;
            case WEEKPLAY:
               return serverTime.weekTime + currentPlayTime;
            case MONTHPLAY:
               return serverTime.monthTime + currentPlayTime;
            case LASTLOGIN:
               return serverTime.lastLogin;
            case TOTALVOTE:
               return (long)playerData.totalVotes;
            case TODAYVOTE:
               return (long)playerData.dailyVotes;
            case WEEKVOTE:
               return (long)playerData.weeklyVotes;
            case MONTHVOTE:
               return (long)playerData.monthlyVotes;
            case LASTVOTE:
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  return playerData.lastVoteDate;
               }

               return -1L;
            case TOTALREFER:
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  return (long)playerData.totalReferrals;
               }

               return -1L;
            case TODAYREFER:
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  return (long)playerData.dailyReferrals;
               }

               return -1L;
            case WEEKREFER:
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  return (long)playerData.weeklyReferrals;
               }

               return -1L;
            case MONTHREFER:
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  return (long)playerData.monthlyReferrals;
               }

               return -1L;
            case TOTALPOINT:
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  return (long)playerData.points;
               }

               return -1L;
         }
      }

      return -1L;
   }

   public static String[] matchPlayerNames(String nameRoot) {
      return _plugin.get_dataio().getDataListFromMySQL("'%" + nameRoot + "%'", " LIKE ", "playerName", "playerName", "playerName", "ASC");
   }

   public int loadPlayerDataYML(String filename, String loadPlayer, Boolean replace, Boolean dataImport) {
      File playerDataFile = new File(_plugin.getDataFolder(), filename);
      if (!playerDataFile.exists()) {
         this.savePlayerDataYML(OnTime.onTimeDataFolder, "playerdata.yml");
         LogFile.console(3, "No Player Data loaded. " + playerDataFile.getPath() + " Did not exist, but has been now created.");
         return 0;
      } else {
         boolean loadYMLsuccess = true;
         YamlConfiguration playerData = new YamlConfiguration();

         try {
            playerData.load(playerDataFile);
         } catch (Exception var14) {
            LogFile.console(3, "{loadPlayerDataYML} FILE Error: " + var14.getMessage());
            loadYMLsuccess = false;
         }

         if (!loadYMLsuccess) {
            LogFile.write(3, "ERROR:  Format error in " + playerDataFile.getPath() + ".  See Console Log for details.");
            return -1;
         } else {
            LogFile.console(1, "Loading from " + playerDataFile.getPath());
            int playersLoaded = 0;
            int version = playerData.getInt("playerDataVersion");
            List players = null;
            String playerName = null;
            if (!dataImport) {
               long todayDate = 0L;
               if (playerData.getInt("playerDataVersion") == 1) {
                  todayDate = playerData.getLong("ReportDate");
               } else {
                  todayDate = playerData.getLong("TodayDate");
               }

               if (todayDate == 0L) {
                  todayDate = TodayTime.todayMidnight();
               }

               OnTime.todayStart = todayDate;
               todayDate = playerData.getLong("WeekStartDate");
               if (todayDate == 0L) {
                  todayDate = TodayTime.todayMidnight();
               }

               OnTime.weekStart = todayDate;
               todayDate = playerData.getLong("MonthStartDate");
               if (todayDate == 0L) {
                  todayDate = TodayTime.todayMidnight();
               }

               OnTime.monthStart = todayDate;
            }

            if (version < 3) {
               players = playerData.getStringList("players");
               if (players.size() == 0) {
                  LogFile.console(1, "No Playerdata to process.");
                  LogFile.write(1, "Loading from " + playerDataFile.getPath() + " No data to process.");
                  return 0;
               }
            } else {
               playerName = playerData.getString("players.player0");
               if (playerName == null) {
                  LogFile.console(1, "No Playerdata to process.");
                  LogFile.write(1, "Loading from " + playerDataFile.getPath() + " No data to process.");
                  return 0;
               }
            }

            if (version < 3) {
               Iterator var13 = players.iterator();

               while(var13.hasNext()) {
                  String s = (String)var13.next();
                  if (this.parseYMLPlayer(s, loadPlayer, replace, version)) {
                     ++playersLoaded;
                  }
               }
            } else {
               for(int count = 0; playerName != null; playerName = playerData.getString("players.player" + count)) {
                  if (this.parseYMLPlayer(playerName, loadPlayer, replace, version)) {
                     ++playersLoaded;
                  }

                  ++count;
               }
            }

            LogFile.write(2, "Loaded " + playersLoaded + " players from " + playerDataFile.getPath());
            return playersLoaded;
         }
      }
   }

   boolean parseYMLPlayer(String s, String loadPlayer, boolean replace, int version) {
      String delims = "[,]";
      String[] tokens = s.split(delims);
      String playerName = tokens[DataIO.playerYML.PLAYERNAME.v[version]];
      if (playerName.equalsIgnoreCase(loadPlayer) || loadPlayer == null) {
         if (!Players.hasOnTimeRecord(playerName) || replace) {
            long playTime = Long.valueOf(tokens[DataIO.playerYML.PLAYTIME.v[version]]);
            long todayTime = Long.valueOf(tokens[DataIO.playerYML.TODAYTIME.v[version]]);
            long weekTime = Long.valueOf(tokens[DataIO.playerYML.WEEKTIME.v[version]]);
            long monthTime = Long.valueOf(tokens[DataIO.playerYML.MONTHTIME.v[version]]);
            if (version >= 3) {
               PlayerData playerData = null;
               long loginTime = Long.valueOf(tokens[DataIO.playerYML.LOGINTIME.v[version]]);
               int totalVote = 0;
               int todayVote = 0;
               int weekVote = 0;
               int monthVote = 0;
               if (version > 3) {
                  totalVote = Integer.valueOf(tokens[DataIO.playerYML.TOTALVOTE.v[version]]);
                  todayVote = Integer.valueOf(tokens[DataIO.playerYML.TODAYVOTE.v[version]]);
                  weekVote = Integer.valueOf(tokens[DataIO.playerYML.WEEKVOTE.v[version]]);
                  monthVote = Integer.valueOf(tokens[DataIO.playerYML.MONTHVOTE.v[version]]);
               }

               UUID uuid = null;
               if (version > 4 && !tokens[DataIO.playerYML.UUID.v[version]].equalsIgnoreCase("null")) {
                  uuid = UUID.fromString(tokens[DataIO.playerYML.UUID.v[version]]);
               }

               int daysOn = 0;
               if (version > 5) {
                  daysOn = Integer.valueOf(tokens[DataIO.playerYML.DAYSON.v[version]]);
               }

               if (!playerName.equals(playerName.toLowerCase())) {
                  OfflinePlayer offlinePlayer = Players.getOfflinePlayer(playerName);
                  if (offlinePlayer != null && !playerName.equals(offlinePlayer.getName())) {
                     LogFile.write(3, "{parseYMLplayer} In DB, converted " + playerName + " to " + offlinePlayer.getName());
                     playerName = offlinePlayer.getName();
                  }
               }

               if (!Players.playerHasData(playerName)) {
                  playerData = Players.getNew(uuid, playerName, loginTime, totalVote, todayVote, weekVote, monthVote, daysOn, playTime, todayTime, weekTime, monthTime);
                  Players.putData(uuid, playerData);
               } else {
                  if (uuid != null) {
                     playerData = Players.getData(uuid);
                  }

                  if (playerData == null) {
                     playerData = Players.getData(playerName);
                  }

                  if (playerData.uuid == null & uuid != null) {
                     Players.putData(uuid, playerData);
                     Players.getuuidMap().put(playerName.toLowerCase(), uuid);
                  }

                  PlayTimeData serverTime = Players.getWorldTime(playerData, OnTime.serverID);
                  serverTime.lastLogin = loginTime;
                  serverTime.totalTime = playTime;
                  serverTime.todayTime = todayTime;
                  serverTime.weekTime = weekTime;
                  serverTime.monthTime = monthTime;
                  playerData.totalVotes = totalVote;
                  playerData.dailyVotes = todayVote;
                  playerData.weeklyVotes = weekVote;
                  playerData.monthlyVotes = monthVote;
                  playerData.uuid = uuid;
                  playerData.daysOn = daysOn;
                  if (daysOn == 0) {
                     playerData.daysOn = (int)_plugin.get_logintime().getCount(serverTime.lastLogin);
                     serverTime.lastLogin -= (long)playerData.daysOn;
                  }
               }
            }

            return true;
         }

         if (loadPlayer != null) {
            LogFile.write(1, "Data NOT loaded for: " + playerName + " player already in database.");
         }
      }

      return false;
   }

   public boolean loadTopPlayerDataMySQL(mysqlload loading, int playerCount) {
      long todaytime = 0L;
      long weektime = 0L;
      long monthtime = 0L;
      LogFile.console(0, "Loading Top Player data from MySQL");

      try {
         if (!mysqlNew.checkMySQLConnection()) {
            return false;
         }

         ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playerName='ontime-data'");
         rs.first();
         todaytime = rs.getLong("todaytime");
         weektime = rs.getLong("weektime");
         monthtime = rs.getLong("monthtime");
         if (loading == DataIO.mysqlload.ALL || loading == DataIO.mysqlload.INIT) {
            OnTime.todayStart = Long.valueOf(todaytime);
            if (weektime != 0L) {
               OnTime.weekStart = weektime;
            } else {
               OnTime.weekStart = todaytime;
            }

            if (monthtime != 0L) {
               OnTime.monthStart = monthtime;
            } else {
               OnTime.monthStart = todaytime;
            }

            LogFile.write(0, "TodayDate loaded: " + todaytime);
         }
      } catch (SQLException var11) {
         LogFile.console(3, "{loadTopPlayerDataMySQL} MYSQL Error: " + var11.getMessage());
      }

      this.loadXXPlayerDataMySQL("playtime", loading, playerCount);
      if (loading == DataIO.mysqlload.INIT) {
         this.loadXXPlayerDataMySQL("todaytime", loading, playerCount);
         this.loadXXPlayerDataMySQL("weektime", loading, playerCount);
         this.loadXXPlayerDataMySQL("monthtime", loading, playerCount);
         if (OnTime.pointsEnable) {
            this.loadXXPlayerDataMySQL("points", loading, playerCount);
         }
      }

      return true;
   }

   public boolean loadXXPlayerDataMySQL(String section, mysqlload loading, int playerCount) {
      String playerName = null;
      long todaytime = 0L;
      long weektime = 0L;
      long monthtime = 0L;
      long logintime = 0L;
      int points = 0;

      boolean more;
      try {
         int permissions = false;
         int playersLoaded = 0;
         int maxLoading = 0;
         mysqlNew.checkMySQLConnection();
         ResultSet rs;
         if (loading == DataIO.mysqlload.PURGE) {
            long lastLoginDay = Calendar.getInstance().getTimeInMillis() - TimeUnit.DAYS.toMillis(OnTime.purgeLoginDay);
            rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE `logintime` < " + lastLoginDay + " ORDER BY `logintime` ASC");
         } else {
            rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " ORDER BY `" + section + "` DESC");
         }

         more = rs.first();

         while(true) {
            boolean loadThisPlayer;
            int permissions;
            UUID uuid;
            do {
               do {
                  do {
                     if (!more) {
                        LogFile.write(2, "Loaded " + playersLoaded + " player's data from MySQL DB:" + OnTime.MySQL_database);
                        return true;
                     }

                     uuid = null;
                     playerName = rs.getString("playername");
                     todaytime = rs.getLong("todaytime");
                     weektime = rs.getLong("weektime");
                     monthtime = rs.getLong("monthtime");
                     logintime = rs.getLong("logintime");
                     permissions = rs.getInt("permissions");
                     if (rs.getString("uuid") != null && !rs.getString("uuid").equalsIgnoreCase("null")) {
                        uuid = UUID.fromString(rs.getString("uuid"));
                     }

                     if (OnTime.pointsEnable) {
                        points = rs.getInt("points");
                     }

                     if (!rs.next()) {
                        more = false;
                     }
                  } while(playerName.equalsIgnoreCase("ontime-data"));
               } while(playerName.equalsIgnoreCase("todaydate"));

               loadThisPlayer = false;
               if (loading == DataIO.mysqlload.INIT) {
                  if (Players.playerHasData(playerName)) {
                     ++maxLoading;
                  } else if (section.equalsIgnoreCase("todaytime") && todaytime == 0L) {
                     ++maxLoading;
                  } else if (section.equalsIgnoreCase("weektime") && weektime == 0L) {
                     ++maxLoading;
                  } else if (section.equalsIgnoreCase("monthtime") && monthtime == 0L) {
                     ++maxLoading;
                  } else if (section.equalsIgnoreCase("points") && points == 0) {
                     ++maxLoading;
                  } else {
                     loadThisPlayer = true;
                  }
               } else if (loading == DataIO.mysqlload.PURGE) {
                  if (logintime > 1L) {
                     loadThisPlayer = true;
                  }
               } else {
                  loadThisPlayer = true;
               }
            } while(!loadThisPlayer);

            boolean success = false;
            if (uuid != null) {
               success = this.loadPlayerDataMySQL(uuid);
            }

            if (uuid == null || !success) {
               success = this.loadPlayerDataMySQL(playerName);
            }

            if (success) {
               ++playersLoaded;
            } else {
               LogFile.write(10, "{loadXXPlayerDataMySQL} Failed to load " + playerName + "(UUID: " + rs.getString("uuid") + ")");
            }

            if ((permissions & PlayerData.OTPerms.TOPTEN.mask()) == 0) {
               ++maxLoading;
            }

            if (loading == DataIO.mysqlload.INIT && maxLoading >= playerCount) {
               more = false;
            }
         }
      } catch (SQLException var22) {
         LogFile.console(3, "{loadXXPlayerDataMySQL} MYSQL Error: " + var22.getMessage());
         more = false;
         return true;
      }
   }

   public ResultSet getTopPlayersDataMySQL(String dataItem) {
      ResultSet rs = null;

      try {
         mysqlNew.checkMySQLConnection();
         rs = mysqlNew.query("SELECT playerName,referredby,`" + dataItem + "`  FROM " + OnTime.MySQL_table + " ORDER BY `" + dataItem + "` DESC");
         return rs;
      } catch (SQLException var4) {
         LogFile.console(3, "{getTopPlayersDataMySQL} MySQL query failed.  Top player data not loaded. " + var4.getMessage());
         return null;
      }
   }

   public boolean loadAllPlayerDataMySQL(mysqlload loading) {
      boolean more;
      try {
         int playersLoaded = 0;
         mysqlNew.checkMySQLConnection();
         ResultSet rs;
         if (loading == DataIO.mysqlload.ALL) {
            rs = mysqlNew.query("SELECT playername,uuid,referredby FROM " + OnTime.MySQL_table + " WHERE world='" + OnTime.serverID + "'");
         } else {
            if (loading != DataIO.mysqlload.MISSINGUUID) {
               LogFile.write(10, "{loadAllPlayerDataMySQL} invaild load type :" + loading.toString());
               return false;
            }

            rs = mysqlNew.query("SELECT playername,uuid,referredby FROM " + OnTime.MySQL_table + " WHERE world= '" + OnTime.serverID + "' AND (uuid='null' OR uuid IS NULL)");
         }

         more = rs.first();
         if (!more) {
            LogFile.write(0, "{loadAllPlayers} No Players Found to load.");
         }

         while(more) {
            String playerName = rs.getString("playername");
            String uuid_S = rs.getString("uuid");
            UUID uuid = null;
            if (uuid_S != null && !uuid_S.equalsIgnoreCase("null")) {
               uuid = UUID.fromString(uuid_S);
            }

            long todaytime = 0L;
            if (loading != DataIO.mysqlload.ALL || !playerName.equalsIgnoreCase("ontime-data") && !playerName.equalsIgnoreCase("todaydate")) {
               boolean loadPlayer = true;
               String referredBy = rs.getString("referredby");
               if (referredBy != null && (referredBy.equalsIgnoreCase("votifier-service") || referredBy.equalsIgnoreCase("ontime-report"))) {
                  loadPlayer = false;
               }

               if (loadPlayer && uuid != null && this.loadPlayerDataMySQL(uuid)) {
                  loadPlayer = false;
                  ++playersLoaded;
               }

               if (loadPlayer && this.loadPlayerDataMySQL(playerName)) {
                  ++playersLoaded;
               }
            } else {
               ResultSet rs2 = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playername='ontime-data'");
               rs2.first();
               todaytime = rs2.getLong("todaytime");
               if (loading == DataIO.mysqlload.ALL) {
                  OnTime.todayStart = Long.valueOf(todaytime);
                  if ((OnTime.weekStart = rs2.getLong("weektime")) == 0L) {
                     OnTime.weekStart = todaytime;
                  }

                  if ((OnTime.monthStart = rs2.getLong("monthtime")) == 0L) {
                     OnTime.monthStart = todaytime;
                  }

                  LogFile.write(0, "TodayDate loaded: " + todaytime);
               }
            }

            if (!rs.next()) {
               more = false;
            }
         }

         LogFile.write(2, "Loaded " + playersLoaded + " player's data from MySQL DB:" + OnTime.MySQL_database);
      } catch (SQLException var12) {
         LogFile.console(10, "{loadAllPlayerDataMySQL} MYSQL Error: " + var12.getMessage());
         more = false;
      }

      return true;
   }

   public boolean loadPlayerDataMySQL(UUID uuid) {
      return this.loadPlayerDataMySQL("uuid", uuid.toString());
   }

   public boolean loadPlayerDataMySQL(String playerName) {
      return this.loadPlayerDataMySQL("playername", playerName);
   }

   public boolean loadPlayerDataMySQL(String field, String key) {
      PlayerData playerData = null;
      int mysqlID = 0;
      long lastVoteDate = 0L;
      int permissions = 0;
      int totalVotes = 0;
      int dailyVotes = 0;
      int weeklyVotes = 0;
      int monthlyVotes = 0;
      int totalReferrals = 0;
      int dailyReferrals = 0;
      int weeklyReferrals = 0;
      int monthlyReferrals = 0;
      long afkToday = 0L;
      long afkWeek = 0L;
      long afkMonth = 0L;
      int points = 0;
      int daysOn = 0;

      try {
         if (!mysqlNew.checkMySQLConnection()) {
            return false;
         }

         ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE " + field + "='" + key + "'");
         String referredBy = "N/A";
         rs.beforeFirst();

         label152:
         while(true) {
            while(true) {
               while(true) {
                  if (!rs.next()) {
                     break label152;
                  }

                  long totalTime = 0L;
                  long todaytime = 0L;
                  long weektime = 0L;
                  long monthtime = 0L;
                  long logintime = 0L;
                  long firstLogin = 0L;
                  String worldName = null;
                  String storedName = null;
                  String hostName = null;
                  UUID uuid = null;
                  worldName = rs.getString("world");
                  if (worldName == null) {
                     worldName = OnTime.serverID;
                  }

                  totalTime = rs.getLong("playtime");
                  logintime = rs.getLong("logintime");
                  todaytime = rs.getLong("todaytime");
                  weektime = rs.getLong("weektime");
                  monthtime = rs.getLong("monthtime");
                  storedName = rs.getString("playerName");
                  firstLogin = rs.getLong("firstlogin");
                  String uuidString = rs.getString("uuid");
                  if (uuidString != null && !uuidString.equalsIgnoreCase("null") && uuidString != null) {
                     try {
                        uuid = UUID.fromString(uuidString);
                     } catch (Exception var44) {
                        LogFile.write(10, "Invalid UUID found in MySQL of (" + uuidString + ") for " + key);
                     }
                  }

                  if (worldName.equalsIgnoreCase(OnTime.serverID)) {
                     permissions = rs.getInt("permissions");
                     hostName = rs.getString("hostName");
                     mysqlID = rs.getInt("id");
                     daysOn = rs.getInt("daysOn");
                     if (field.equalsIgnoreCase("playername") && !key.equals(key.toLowerCase()) && !storedName.equals(key)) {
                        mysqlNew.query("UPDATE " + OnTime.MySQL_table + " SET playerName='" + key + "' WHERE playerName='" + storedName + "'");
                        LogFile.write(3, "{loadPlayerDataMySQL} In DB, converted " + storedName + " to " + key);
                        storedName = key;
                     }

                     if (firstLogin == 0L & uuid != null && _plugin.getServer().getOfflinePlayer(uuid).hasPlayedBefore()) {
                        _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "firstlogin", _plugin.getServer().getOfflinePlayer(uuid).getFirstPlayed(), uuid);
                     }

                     referredBy = rs.getString("referredby");
                     lastVoteDate = rs.getLong("lastVote");
                     totalVotes = rs.getInt("votes");
                     totalReferrals = rs.getInt("referrals");
                     if (OnTime.collectVoteDetailEnable) {
                        dailyVotes = rs.getInt("voteToday");
                        weeklyVotes = rs.getInt("voteWeek");
                        monthlyVotes = rs.getInt("voteMonth");
                     }

                     if (OnTime.collectReferDetailEnable) {
                        dailyReferrals = rs.getInt("referToday");
                        weeklyReferrals = rs.getInt("referWeek");
                        monthlyReferrals = rs.getInt("referMonth");
                     }

                     if (OnTime.afkCheckEnable) {
                        afkToday = rs.getLong("afkToday");
                        afkWeek = rs.getLong("afkWeek");
                        afkMonth = rs.getLong("afkMonth");
                     }

                     if (OnTime.pointsEnable) {
                        points = rs.getInt("points");
                     }
                  }

                  if (Players.playerHasData(key)) {
                     if (worldName.equalsIgnoreCase(OnTime.serverID) && uuid == null && Players.getData(key).uuid != null) {
                        LogFile.write(10, "{loadPlayerDataMySQL} DB record for " + storedName + " with 'null' UUID found, while other DB record also exists.  This record was not loaded.");
                     } else {
                        if (playerData == null) {
                           if (uuid != null) {
                              playerData = Players.getData(uuid);
                           } else {
                              playerData = Players.getData(key);
                           }
                        }

                        if (playerData == null) {
                           LogFile.write(10, "{loadPlayerDataMySQL} Error accessing playerData for key:" + key + " or UUID " + uuid.toString());
                        } else {
                           if (worldName.equalsIgnoreCase(OnTime.serverID)) {
                              if (playerData.uuid == null & uuid != null) {
                                 Players.putData(uuid, playerData);
                                 Players.remove(storedName);
                                 Players.getuuidMap().put(storedName.toLowerCase(), uuid);
                              }

                              playerData.playerName = storedName;
                              playerData.uuid = uuid;
                              playerData.mysqlID = mysqlID;
                              Players.setWorldTime(playerData, OnTime.serverID, totalTime, todaytime, weektime, monthtime, logintime);
                              playerData.lastVoteDate = lastVoteDate;
                              playerData.totalVotes = totalVotes;
                              playerData.dailyVotes = dailyVotes;
                              playerData.weeklyVotes = weeklyVotes;
                              playerData.monthlyVotes = monthlyVotes;
                              playerData.totalReferrals = totalReferrals;
                              playerData.dailyReferrals = dailyReferrals;
                              playerData.weeklyReferrals = weeklyReferrals;
                              playerData.monthlyReferrals = monthlyReferrals;
                              playerData.referredBy = referredBy;
                              playerData.points = points;
                              playerData.hostName = hostName;
                              playerData.daysOn = daysOn;
                              if (daysOn == 0) {
                                 playerData.daysOn = (int)_plugin.get_logintime().getCount(logintime);
                                 PlayTimeData var10000 = Players.getWorldTime(playerData, OnTime.serverID);
                                 var10000.lastLogin -= (long)playerData.daysOn;
                              }

                              if (OnTime.afkCheckEnable) {
                                 playerData.afkData.todayAFKTime = afkToday;
                                 playerData.afkData.weekAFKTime = afkWeek;
                                 playerData.afkData.monthAFKTime = afkMonth;
                              }
                           } else {
                              Players.setWorldTime(playerData, worldName, totalTime, todaytime, weektime, monthtime, logintime);
                           }

                           if (uuid != null) {
                              LogFile.write(0, worldName + " MySQL data loaded to existing record for: " + key + " (" + uuid.toString() + ")");
                           } else {
                              LogFile.write(0, worldName + " MySQL data loaded to existing record  for: " + key + " (UUID = NULL)");
                           }
                        }
                     }
                  } else {
                     playerData = new PlayerData(uuid, storedName, hostName, firstLogin, logintime, (String)null, lastVoteDate, totalVotes, dailyVotes, weeklyVotes, monthlyVotes, totalReferrals, dailyReferrals, weeklyReferrals, monthlyReferrals, referredBy, permissions, points, daysOn, totalTime, todaytime, weektime, monthtime, afkToday, afkWeek, afkMonth);
                     if (!worldName.equalsIgnoreCase(OnTime.serverID)) {
                        Players.setWorldTime(playerData, worldName, totalTime, todaytime, weektime, monthtime, logintime);
                     }

                     Players.putData(uuid, playerData);
                     if (uuid != null) {
                        Players.addUuidMap(storedName, uuid);
                        LogFile.write(0, worldName + " MySQL data loaded to NEW record for: " + key + " (" + uuid.toString() + ")");
                     } else {
                        LogFile.write(0, worldName + " MySQL data loaded to NEW record for: " + key + " (UUID = NULL)");
                     }
                  }
               }
            }
         }
      } catch (SQLException var45) {
         LogFile.console(3, "{loadPlayerDataMySQL:playerName} MYSQL Error:" + var45.getMessage());
      }

      if (playerData != null) {
         if (OnTime.multiServer) {
            MultiServer.loadPlayerDataMySQL(playerData.uuid);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean refreshPlayerDataMySQL(PlayerData playerData) {
      if (OnTime.dataStorage != DataIO.datastorage.MYSQL) {
         return false;
      } else {
         try {
            if (!mysqlNew.checkMySQLConnection()) {
               return false;
            } else if (playerData == null) {
               return false;
            } else {
               ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playerName='" + playerData.playerName + "'");
               rs.beforeFirst();

               while(rs.next()) {
                  String worldName = rs.getString("world");
                  long totalTime = rs.getLong("playtime");
                  long todayTime = rs.getLong("todaytime");
                  long weekTime = rs.getLong("weektime");
                  long monthTime = rs.getLong("monthtime");
                  long lastLogin = rs.getLong("logintime");
                  Players.setWorldTime(playerData, worldName, totalTime, todayTime, weekTime, monthTime, lastLogin);
               }

               LogFile.write(0, "MySQL refereshed data for: " + playerData.playerName);
               return true;
            }
         } catch (SQLException var14) {
            LogFile.console(3, "[ONTIME] {refreshPlayerDataMySQL} MYSQL Error:" + var14.getMessage());
            return false;
         }
      }
   }

   public PlayerData getPlayerDataFromRS(ResultSet rs) {
      PlayerData playerData = null;
      String worldName = null;
      String storedName = null;
      String hostName = null;
      String referredBy = "N/A";
      UUID uuid = null;
      long lastVoteDate = 0L;
      int permissions = false;
      int totalVotes = false;
      int dailyVotes = 0;
      int weeklyVotes = 0;
      int monthlyVotes = 0;
      int totalReferrals = false;
      int dailyReferrals = 0;
      int weeklyReferrals = 0;
      int monthlyReferrals = 0;
      long afkToday = 0L;
      long afkWeek = 0L;
      long afkMonth = 0L;
      int points = 0;
      int daysOn = false;
      long totalTime = 0L;
      long todaytime = 0L;
      long weektime = 0L;
      long monthtime = 0L;
      long logintime = 0L;
      long firstLogin = 0L;

      try {
         worldName = rs.getString("world");
         if (worldName == null) {
            worldName = OnTime.serverID;
         } else if (!worldName.equalsIgnoreCase(OnTime.serverID)) {
            return null;
         }

         totalTime = rs.getLong("playtime");
         logintime = rs.getLong("logintime");
         todaytime = rs.getLong("todaytime");
         weektime = rs.getLong("weektime");
         monthtime = rs.getLong("monthtime");
         storedName = rs.getString("playerName");
         firstLogin = rs.getLong("firstlogin");
         String uuidString = rs.getString("uuid");
         if (uuidString != null) {
            if (!uuidString.equalsIgnoreCase("null")) {
               try {
                  uuid = UUID.fromString(uuidString);
               } catch (Exception var41) {
                  LogFile.write(10, "{getPlayerDataFromRS} Invalid UUID found in MySQL of (" + uuidString + ") for " + storedName);
                  uuid = null;
               }
            } else {
               uuid = null;
            }
         }

         int permissions = rs.getInt("permissions");
         hostName = rs.getString("hostName");
         int daysOn = rs.getInt("daysOn");
         referredBy = rs.getString("referredby");
         lastVoteDate = rs.getLong("lastVote");
         int totalVotes = rs.getInt("votes");
         int totalReferrals = rs.getInt("referrals");
         if (OnTime.collectVoteDetailEnable) {
            dailyVotes = rs.getInt("voteToday");
            weeklyVotes = rs.getInt("voteWeek");
            monthlyVotes = rs.getInt("voteMonth");
         }

         if (OnTime.collectReferDetailEnable) {
            dailyReferrals = rs.getInt("referToday");
            weeklyReferrals = rs.getInt("referWeek");
            monthlyReferrals = rs.getInt("referMonth");
         }

         if (OnTime.afkCheckEnable) {
            afkToday = rs.getLong("afkToday");
            afkWeek = rs.getLong("afkWeek");
            afkMonth = rs.getLong("afkMonth");
         }

         if (OnTime.pointsEnable) {
            points = rs.getInt("points");
         }

         playerData = new PlayerData(uuid, storedName, hostName, firstLogin, logintime, (String)null, lastVoteDate, totalVotes, dailyVotes, weeklyVotes, monthlyVotes, totalReferrals, dailyReferrals, weeklyReferrals, monthlyReferrals, referredBy, permissions, points, daysOn, totalTime, todaytime, weektime, monthtime, afkToday, afkWeek, afkMonth);
         return playerData;
      } catch (SQLException var42) {
         LogFile.console(3, "{getPlayerDataFromRS} ResultSet Error:" + var42.getMessage());
         LogFile.write(10, "{getPlayerDataFromRS} ResultSet Error:" + var42.getMessage());
         return null;
      }
   }

   public void setOntimeDataMySQL() {
      int maxID = false;
      Long todayTime = OnTime.todayStart;
      Long weekTime = OnTime.weekStart;
      Long monthTime = OnTime.monthStart;
      if (OnTime.primaryServer) {
         try {
            if (todayTime == 0L) {
               todayTime = TodayTime.todayMidnight();
            }

            if (weekTime == 0L) {
               weekTime = TodayTime.todayMidnight();
            }

            if (monthTime == 0L) {
               monthTime = TodayTime.todayMidnight();
            }

            mysqlNew.checkMySQLConnection();
            ResultSet read = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playerName='ontime-data'");
            if (!read.first()) {
               ResultSet maxid_RS = mysqlNew.query("SELECT MAX(id) FROM " + OnTime.MySQL_table);
               maxid_RS.next();
               int maxID;
               if (maxid_RS.getInt("MAX(id)") == 0) {
                  maxID = 0;
               } else {
                  maxID = maxid_RS.getInt("MAX(id)");
               }

               LogFile.write(3, "{DataIO.setOntimeDataMySQL} ontime-data not found.  New record created, setting maxID to " + maxID);
               mysqlNew.query("INSERT INTO " + OnTime.MySQL_table + "(id,playerName,todaytime, weektime, monthtime) VALUES (" + maxID + ",'ontime-data'," + todayTime + "," + weekTime + "," + monthTime + ")");
            } else {
               mysqlNew.query("UPDATE " + OnTime.MySQL_table + " SET todaytime=" + todayTime + ", weektime=" + weekTime + ", monthtime=" + monthTime + " WHERE playerName='ontime-data'");
            }
         } catch (SQLException var8) {
            LogFile.console(3, "{setOnTimeDataMYSQL} MYSQL Error: " + var8.getMessage());
         }
      }

   }

   public boolean createOnTimePlayersTable(String table) {
      mysqlNew.checkMySQLConnection();
      if (!mysqlNew.checkTable(table)) {
         mysqlNew.createTable("CREATE TABLE " + table + " (id  int, uuid varchar(128), playerName varchar(64), world varchar(32), playtime bigint, " + "logintime bigint, todaytime bigint, weektime bigint, monthtime bigint, referredby varchar(64)," + "votes int, lastVote bigint, referrals int, firstlogin bigint," + "afkToday bigint, afkWeek bigint, afkMonth bigint, permissions int," + "voteToday int, voteWeek int, voteMonth int, " + "points int," + "hostName varchar(128)," + "daysOn int," + "INDEX (playerName))");
         return true;
      } else {
         return false;
      }
   }

   public boolean createOnTimeReportTable(String tableName) {
      mysqlNew.checkMySQLConnection();
      if (!mysqlNew.checkTable(tableName)) {
         mysqlNew.createTable("CREATE TABLE `" + tableName + "` (" + "id int," + "playerName varchar(32)," + "playtime varchar(32)," + "logintime varchar(32)," + "todaytime varchar(32)," + "weektime varchar(32)," + "monthtime varchar(32)," + "referredby varchar(32)," + "votes int," + "lastVote varchar(32)," + "referrals int," + "firstlogin varchar(32)," + "afkToday varchar(32)," + "afkWeek varchar(32)," + "afkMonth varchar(32)," + "permissions int," + "referToday int," + "referWeek int," + "referMonth int," + "voteToday int," + "voteWeek int," + "voteMonth int," + "daysOn int" + ")");
         return true;
      } else {
         return false;
      }
   }

   public boolean createOnlineTrackingTable(String tableName) {
      StringBuilder sb = new StringBuilder(512);
      mysqlNew.checkMySQLConnection();
      if (!mysqlNew.checkTable(tableName)) {
         sb.append("CREATE TABLE `" + tableName + "` (");
         Iterator it = Output.getOnlineFields().iterator();
         boolean atLeastOne = false;

         while(it.hasNext()) {
            sb.append((String)it.next() + " varchar(32)");
            atLeastOne = true;
            if (it.hasNext()) {
               sb.append(",");
            }
         }

         if (!sb.toString().contains("player")) {
            LogFile.console(3, "Online tracking table *MUST* include the column 'player'.  Function disabled.");
            OnTime.onlineTrackingEnable = false;
            return false;
         } else {
            sb.append(", UNIQUE (player))");
            LogFile.console(0, "OnLineTable: " + sb.toString());
            if (atLeastOne) {
               if (mysqlNew.createTable(sb.toString())) {
                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void saveAllMySQL_async(CommandSender sender, int first, int last, String finishMessage, boolean suspend) {
      if (sender != null) {
         sender.sendMessage("Saving all OnTime records to MySQL database.  This may take several minutes.");
      }

      if (!_plugin.get_dataio().getPlayerMap().isEmpty()) {
         int count = 0;
         int scale = 10;
         Iterator var9 = _plugin.get_dataio().getPlayerMap().keySet().iterator();

         while(var9.hasNext()) {
            String key = (String)var9.next();
            if (count < first && first >= 0) {
               ++count;
            } else {
               this.savePlayerDataMySQL((PlayerData)_plugin.get_dataio().getPlayerMap().get(key), false);
               ++count;
               if (count >= 100) {
                  scale = 50;
               }

               if (count >= 500) {
                  scale = 100;
               }

               if (count % scale == 0 && sender != null) {
                  sender.sendMessage("OnTime MySQL processing record # " + count + " of " + _plugin.get_dataio().getPlayerMap().size());
               }
            }

            if (count > last && last > 0) {
               break;
            }
         }
      }

      if (sender != null) {
         sender.sendMessage(finishMessage);
      }

      if (suspend) {
         OnTime.get_commands().resumeOnTime();
      }

   }

   public boolean saveAllPlayerDataMySQL(final CommandSender sender, mysqlsave allOrOnline, final int first, final int last, final String finishMessage) {
      if (!mysqlNew.checkMySQLConnection()) {
         return false;
      } else {
         this.setOntimeDataMySQL();
         if (allOrOnline == DataIO.mysqlsave.ALL) {
            final boolean localSuspend = false;
            if (!OnTime.suspendOnTime) {
               OnTime.get_commands().suspendOnTime();
               localSuspend = true;
            }

            final boolean suspend = localSuspend;

            try {
               _plugin.getServer().getScheduler().runTaskLaterAsynchronously(_plugin, new Runnable() {
                  public void run() {
                     DataIO.this.saveAllMySQL_async(sender, first, last, finishMessage, suspend);
                  }
               }, 20L);
            } catch (NoSuchMethodError var9) {
               _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new Runnable() {
                  public void run() {
                     DataIO.this.saveAllMySQL_async(sender, first, last, finishMessage, localSuspend);
                  }
               }, 20L);
            }
         } else {
            Iterator var11 = _plugin.getServer().getOnlinePlayers().iterator();

            while(var11.hasNext()) {
               Player player = (Player)var11.next();
               if (_plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
                  PlayerData playerData = null;
                  if ((playerData = Players.getData(player.getUniqueId())) != null) {
                     this.savePlayerDataMySQL(playerData, false);
                  } else {
                     LogFile.write(1, player.getName() + " data not saved. PlayerData record not found for this player.");
                  }
               } else {
                  LogFile.write(1, player.getName() + " data not saved. OnTime tracking is not enabled for this player.");
               }
            }
         }

         return true;
      }
   }

   public boolean savePlayerDataMySQL(String playerName, boolean saveReports) {
      PlayerData playerData = null;
      if ((playerData = Players.getData(playerName)) == null) {
         playerData = Players.getNew(playerName, 0L, 0L, "null");
         Players.putData(playerName, playerData);
         LogFile.write(10, "{savePlayerDataMySQL} Missing player record for " + playerName + " had to be created prior to save.");
      }

      return this.savePlayerDataMySQL(playerData, saveReports);
   }

   public boolean savePlayerDataMySQL(PlayerData playerData, boolean saveReports) {
      String saveQuery = null;
      String playingTimeStr = null;
      String loginTimeStr = null;
      String todayTimeStr = null;
      String weekTimeStr = null;
      String monthTimeStr = null;
      String afkTodayStr = null;
      String afkWeekStr = null;
      String afkMonthStr = null;
      int mysqlID = 0;

      try {
         if (OnTime.afkCheckEnable) {
            afkTodayStr = Output.getTimeBreakdown(playerData.afkData.todayAFKTime, Output.TIMEDETAIL.LONG);
            afkWeekStr = Output.getTimeBreakdown(playerData.afkData.weekAFKTime, Output.TIMEDETAIL.LONG);
            afkMonthStr = Output.getTimeBreakdown(playerData.afkData.monthAFKTime, Output.TIMEDETAIL.LONG);
         }

         ResultSet read = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playerName='" + playerData.playerName + "'");
         String[] worlds = new String[20];

         for(int numWorlds = 0; read.next(); worlds[numWorlds++] = read.getString("world")) {
         }

         List worldList = Arrays.asList(worlds);
         Iterator var19 = playerData.worldTime.keySet().iterator();

         while(var19.hasNext()) {
            String worldKey = (String)var19.next();
            boolean updated = false;
            ResultSet write;
            if (worldList != null && worldList.contains(worldKey)) {
               saveQuery = this.buildUpdatePlayerQuery(playerData, worldKey);
               write = mysqlNew.query(saveQuery);
               if (playerData.referredBy == null) {
                  LogFile.write(1, "Updated world MySQL entry for " + playerData.playerName);
               } else if (!playerData.referredBy.equalsIgnoreCase("ontime-report") && !playerData.referredBy.equalsIgnoreCase("votifier-service")) {
                  if (playerData.uuid != null) {
                     LogFile.write(1, "Updated world MySQL entry for " + playerData.playerName + "(" + playerData.uuid.toString() + ") for world '" + worldKey + "'");
                  } else {
                     LogFile.write(1, "Updated world MySQL entry for " + playerData.playerName + "(UUID: null) for world '" + worldKey + "'");
                  }
               } else {
                  LogFile.write(1, "Updated world MySQL entry for " + playerData.playerName);
               }

               updated = true;
            }

            if (!updated) {
               if (worldKey == OnTime.serverID) {
                  String table = OnTime.MySQL_table;
                  if (OnTime.multiServer) {
                     table = OnTime.MySQL_multiServerTable;
                  }

                  read = mysqlNew.query("SELECT id FROM " + table + " WHERE playerName='ontime-data'");
                  if (!read.first()) {
                     this.setOntimeDataMySQL();
                     read = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playerName='ontime-data'");
                     read.first();
                  }

                  mysqlID = read.getInt("id");
                  playerData.mysqlID = mysqlID;
                  write = mysqlNew.query("UPDATE " + table + " SET id=" + (mysqlID + 1) + " WHERE playerName='ontime-data'");
               } else {
                  mysqlID = playerData.mysqlID;
               }

               saveQuery = this.buildNewPlayerQuery(mysqlID, playerData, worldKey);
               write = mysqlNew.query(saveQuery);
               LogFile.write(1, "Created new world MySQL entry for " + playerData.playerName + " for world '" + worldKey + "'");
            }
         }
      } catch (SQLException var23) {
         LogFile.write(10, "{savePlayerDataMySQL} MYSQL Write Error for " + playerData.playerName + " : " + saveQuery);
         LogFile.write(10, "{savePlayerDataMySQL} Exception: " + var23.getMessage());
         return false;
      }

      if (OnTime.reportFormat.equalsIgnoreCase("MYSQL") && saveReports) {
         PlayTimeData serverTime = Players.getWorldTime(playerData, OnTime.serverID);
         playingTimeStr = Output.getTimeBreakdown(serverTime.totalTime, Output.TIMEDETAIL.LONG);
         todayTimeStr = Output.getTimeBreakdown(serverTime.todayTime, Output.TIMEDETAIL.LONG);
         weekTimeStr = Output.getTimeBreakdown(serverTime.weekTime, Output.TIMEDETAIL.LONG);
         monthTimeStr = Output.getTimeBreakdown(serverTime.monthTime, Output.TIMEDETAIL.LONG);
         loginTimeStr = Output.getDateTime(serverTime.lastLogin);
         if (_plugin.isEnabled()) {
            try {
               _plugin.getServer().getScheduler().runTaskAsynchronously(_plugin, new saveMySQLReport(mysqlID, playerData.playerName, playingTimeStr, loginTimeStr, todayTimeStr, weekTimeStr, monthTimeStr, afkTodayStr, afkWeekStr, afkMonthStr, playerData));
            } catch (NoSuchMethodError var22) {
               _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new saveMySQLReport(mysqlID, playerData.playerName, playingTimeStr, loginTimeStr, todayTimeStr, weekTimeStr, monthTimeStr, afkTodayStr, afkWeekStr, afkMonthStr, playerData));
            }
         } else {
            this.saveMySQLReportsFunc(mysqlID, playerData.playerName, playingTimeStr, loginTimeStr, todayTimeStr, weekTimeStr, monthTimeStr, afkTodayStr, afkWeekStr, afkMonthStr, playerData);
         }
      }

      return true;
   }

   private String buildNewPlayerQuery(int playerID, PlayerData playerData, String worldName) {
      StringBuilder sb = new StringBuilder(512);
      String table = OnTime.MySQL_table;
      if (worldName.equalsIgnoreCase(OnTime.multiServerName)) {
         table = OnTime.MySQL_multiServerTable;
      }

      if (worldName.equalsIgnoreCase(OnTime.serverID)) {
         sb.append("INSERT INTO " + table + "(id, uuid, playerName, world, playtime, logintime, todaytime, weektime, monthtime,referredby," + "votes,lastVote,referrals,firstlogin,afkToday, afkWeek, afkMonth, permissions,hostName, daysOn");
         sb.append(", voteToday, voteWeek, voteMonth");
         if (OnTime.collectReferDetailEnable) {
            sb.append(", referToday, referWeek, referMonth");
         }

         if (OnTime.pointsEnable) {
            sb.append(", points");
         }
      } else {
         sb.append("INSERT INTO " + table + "(id, uuid, playerName, world, playtime, logintime, todaytime, weektime, monthtime,firstlogin");
      }

      PlayTimeData serverTime;
      if (worldName.equalsIgnoreCase(OnTime.serverID)) {
         serverTime = Players.getWorldTime(playerData, worldName);
         sb.append(") VALUES (" + playerID + ",'" + playerData.uuid + "','" + playerData.playerName + "','" + OnTime.serverID + "'," + serverTime.totalTime + "," + serverTime.lastLogin + "," + serverTime.todayTime + "," + serverTime.weekTime + "," + serverTime.monthTime + ",'" + playerData.referredBy + "'," + playerData.totalVotes + "," + playerData.lastVoteDate + "," + playerData.totalReferrals + "," + playerData.firstLogin + "," + playerData.afkData.todayAFKTime + "," + playerData.afkData.weekAFKTime + "," + playerData.afkData.monthAFKTime + "," + playerData.permissions + ",'" + playerData.hostName + "'" + "," + playerData.daysOn);
         sb.append("," + playerData.dailyVotes + "," + playerData.weeklyVotes + "," + playerData.monthlyVotes);
         if (OnTime.collectReferDetailEnable) {
            sb.append("," + playerData.dailyReferrals + "," + playerData.weeklyReferrals + "," + playerData.monthlyReferrals);
         }

         if (OnTime.pointsEnable) {
            sb.append("," + playerData.points);
         }
      } else {
         serverTime = Players.getWorldTime(playerData, worldName);
         sb.append(") VALUES (" + playerID + ",'" + playerData.uuid + "','" + playerData.playerName + "','" + worldName + "'" + "," + serverTime.totalTime + "," + serverTime.lastLogin + "," + serverTime.todayTime + "," + serverTime.weekTime + "," + serverTime.monthTime + "," + playerData.firstLogin);
      }

      sb.append(")");
      return sb.toString();
   }

   private String buildUpdatePlayerQuery(PlayerData playerData, String worldName) {
      StringBuilder sb = new StringBuilder(512);
      PlayTimeData worldTime = Players.getWorldTime(playerData, worldName);
      LogFile.console(0, "{buildUpdatePlayerQuery} worldName = " + worldName + " WorldTime:" + worldTime.toString());
      String table = OnTime.MySQL_table;
      if (worldName.equalsIgnoreCase(OnTime.multiServerName)) {
         table = OnTime.MySQL_multiServerTable;
      }

      sb.append("UPDATE " + table + " SET " + "playtime=" + worldTime.totalTime + ", logintime=" + worldTime.lastLogin + ", todaytime=" + worldTime.todayTime + ", weektime=" + worldTime.weekTime + ", monthtime=" + worldTime.monthTime);
      if (worldName.equalsIgnoreCase(OnTime.serverID)) {
         sb.append(", afkToday=" + playerData.afkData.todayAFKTime + ", afkWeek=" + playerData.afkData.weekAFKTime + ", afkMonth=" + playerData.afkData.monthAFKTime + ", permissions=" + playerData.permissions + ", hostName='" + playerData.hostName + "'" + ", daysOn=" + playerData.daysOn);
         if (OnTime.pointsEnable) {
            sb.append(", points=" + playerData.points);
         }
      }

      sb.append(" WHERE playerName='" + playerData.playerName + "' AND world='" + worldName + "'");
      return sb.toString();
   }

   public void updateMySQLField(String DB, String field, String data, UUID uuid) {
      try {
         mysqlNew.checkMySQLConnection();
         mysqlNew.query("UPDATE " + DB + " SET " + field + "='" + data + "' WHERE uuid='" + uuid.toString() + "'");
      } catch (SQLException var6) {
         LogFile.console(3, "{updateMySQLField} SQL Error: Update String: " + uuid.toString() + " : " + var6.getMessage());
      }

   }

   public void updateMySQLField(String DB, String field, String data, String playerName) {
      try {
         mysqlNew.checkMySQLConnection();
         mysqlNew.query("UPDATE " + DB + " SET " + field + "='" + data + "' WHERE playerName='" + playerName + "'");
      } catch (SQLException var6) {
         LogFile.console(3, "{updateMySQLField} SQL Error: Update String: " + playerName + " : " + var6.getMessage());
      }

   }

   public void updateMySQLField(String DB, String field, long data, UUID uuid) {
      try {
         mysqlNew.checkMySQLConnection();
         mysqlNew.query("UPDATE " + DB + " SET " + field + "=" + data + " WHERE uuid='" + uuid.toString() + "'");
      } catch (SQLException var7) {
         LogFile.console(3, "{updateMySQLField} SQL Error: Update String: " + uuid.toString() + " : " + var7.getMessage());
      }

   }

   public boolean updateMySQLField(String DB, String field, long data, String playerName) {
      ResultSet rs = null;

      try {
         mysqlNew.checkMySQLConnection();
         rs = mysqlNew.query("SELECT id FROM " + DB + " WHERE playerName='" + playerName + "'");
         if (rs.first()) {
            mysqlNew.query("UPDATE " + DB + " SET " + field + "=" + data + " WHERE playerName='" + playerName + "'");
            return true;
         } else {
            return false;
         }
      } catch (SQLException var8) {
         LogFile.console(3, "{updateMySQLField} SQL Error: Update Long: " + playerName + " : " + var8.getMessage());
         return false;
      }
   }

   public boolean updateMySQLField(String DB, String field, int data, String playerName) {
      return this.updateMySQLField(DB, field, (long)data, playerName);
   }

   public void updateAllMySQLField(String DB, String field, long data) {
      try {
         mysqlNew.checkMySQLConnection();
         mysqlNew.query("UPDATE " + DB + " SET " + field + "=" + data);
      } catch (SQLException var6) {
         LogFile.console(3, "{updateAllMySQLField} SQL Error: Update All for " + field + " : " + var6.getMessage());
      }

   }

   public int incrementMySQLField(String DB, String field, String playerName) {
      try {
         mysqlNew.checkMySQLConnection();
         ResultSet rs = mysqlNew.query("SELECT * FROM " + DB + " WHERE playerName='" + playerName + "'");
         if (rs.first()) {
            int fieldCount = rs.getInt(field) + 1;
            mysqlNew.query("UPDATE " + DB + " SET " + field + "=" + fieldCount + " WHERE playerName='" + playerName + "'");
            LogFile.write(0, "MySQL " + field + " incremented to " + fieldCount + " for " + playerName);
            return fieldCount;
         } else {
            LogFile.write(1, "MySQL field increment for " + playerName + " failed.  Player not found");
            return _plugin.get_dataio().savePlayerDataMySQL(playerName, true) ? this.incrementMySQLField(DB, field, playerName) : -1;
         }
      } catch (SQLException var6) {
         LogFile.console(3, "{DataIO.incrementMySQLField} MySQLError : " + var6.getMessage());
         return -1;
      }
   }

   public boolean incrementVoteCounts(String playerName) {
      StringBuilder sb = new StringBuilder(512);

      try {
         mysqlNew.checkMySQLConnection();
         ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playerName='" + playerName + "'");
         if (rs.first()) {
            sb.append("UPDATE " + OnTime.MySQL_table + " SET " + "votes=" + (rs.getInt("votes") + 1) + ", lastVote=" + Calendar.getInstance().getTimeInMillis());
            if (OnTime.collectVoteDetailEnable) {
               sb.append(", voteToday=" + (rs.getInt("voteToday") + 1) + ", voteWeek=" + (rs.getInt("voteWeek") + 1) + ", voteMonth=" + (rs.getInt("voteMonth") + 1));
            }

            sb.append(" WHERE playerName='" + playerName + "'");
            mysqlNew.query(sb.toString());
            LogFile.write(1, "Updated MySQL Vote data for " + playerName);
            return true;
         } else {
            LogFile.write(1, "MySQL VOTE increment for " + playerName + " failed.  Player not found. Attempting retry ....");
            return _plugin.get_dataio().savePlayerDataMySQL(playerName, true) ? this.incrementVoteCounts(playerName) : false;
         }
      } catch (SQLException var5) {
         LogFile.console(3, "{DataIO.incrementMySQLField} MySQLError : " + var5.getMessage());
         return false;
      }
   }

   public boolean updateAllPlayerDataMySQL(boolean clearDaily, boolean clearWeekly, boolean clearMonthly, String table) {
      mysqlNew.checkMySQLConnection();
      if (clearDaily) {
         _plugin.get_dataio().updateAllMySQLField(table, "todaytime", 0L);
         _plugin.get_dataio().updateAllMySQLField(table, "afkToday", 0L);
         if (OnTime.collectVoteDetailEnable) {
            _plugin.get_dataio().updateAllMySQLField(table, "voteToday", 0L);
         }

         if (OnTime.collectReferDetailEnable) {
            _plugin.get_dataio().updateAllMySQLField(table, "referToday", 0L);
         }
      }

      if (clearWeekly) {
         _plugin.get_dataio().updateAllMySQLField(table, "weektime", 0L);
         _plugin.get_dataio().updateAllMySQLField(table, "afkWeek", 0L);
         if (OnTime.collectVoteDetailEnable) {
            _plugin.get_dataio().updateAllMySQLField(table, "voteWeek", 0L);
         }

         if (OnTime.collectReferDetailEnable) {
            _plugin.get_dataio().updateAllMySQLField(table, "referWeek", 0L);
         }
      }

      if (clearMonthly) {
         _plugin.get_dataio().updateAllMySQLField(table, "monthtime", 0L);
         _plugin.get_dataio().updateAllMySQLField(table, "afkMonth", 0L);
         if (OnTime.collectVoteDetailEnable) {
            _plugin.get_dataio().updateAllMySQLField(table, "voteMonth", 0L);
         }

         if (OnTime.collectReferDetailEnable) {
            _plugin.get_dataio().updateAllMySQLField(table, "referMonth", 0L);
         }
      }

      return true;
   }

   public void saveMySQLReportsFunc(int id, String playerName, String playingTime, String loginTime, String todayTime, String weekTime, String monthTime, String afkToday, String afkWeek, String afkMonth, PlayerData playerData) {
      if (playerData.referredBy == null || !playerData.referredBy.equalsIgnoreCase("votifier-service") && !playerData.referredBy.equalsIgnoreCase("ontime-report")) {
         if (OnTime.dailyPlayReportEnable) {
            this.saveOneReport(OnTime.dailyReportRetention, Report.MySQLDaily, id, playerName, playingTime, loginTime, todayTime, weekTime, monthTime, afkToday, afkWeek, afkMonth, playerData);
         }

         if (OnTime.weeklyPlayReportEnable) {
            this.saveOneReport(OnTime.weeklyReportRetention, Report.MySQLWeekly, id, playerName, playingTime, loginTime, todayTime, weekTime, monthTime, afkToday, afkWeek, afkMonth, playerData);
         }

         if (OnTime.monthlyPlayReportEnable) {
            this.saveOneReport(OnTime.monthlyReportRetention, Report.MySQLMonthly, id, playerName, playingTime, loginTime, todayTime, weekTime, monthTime, afkToday, afkWeek, afkMonth, playerData);
         }

      }
   }

   public boolean saveOneReport(int expireTime, String reportTableName, int id, String playerName, String playingTime, String loginTime, String todayTime, String weekTime, String monthTime, String afkToday, String afkWeek, String afkMonth, PlayerData playerData) {
      long today = TodayTime.todayMidnight();
      if (this.saveMySQLReportLine(reportTableName, id, playerName, playingTime, loginTime, todayTime, weekTime, monthTime, afkToday, afkWeek, afkMonth, playerData)) {
         LogFile.console(0, "Added/Updated " + playerName + " to MYSQL Report " + reportTableName);
         return true;
      } else {
         if (!_plugin.get_dataio().createOnTimeReportTable(reportTableName)) {
            LogFile.console(3, "Failed to create daily MySQL report table '" + reportTableName + "'");
         } else {
            PlayerData playerdata = Players.getNew(reportTableName, today, today + TimeUnit.DAYS.toMillis((long)expireTime), "ontime-report");
            Players.putData(reportTableName, playerdata);
            _plugin.get_dataio().savePlayerDataMySQL(playerdata, false);
            if (this.saveMySQLReportLine(reportTableName, id, playerName, playingTime, loginTime, todayTime, weekTime, monthTime, afkToday, afkWeek, afkMonth, playerData)) {
               LogFile.console(0, "Added/Updated " + playerName + " to MYSQL Report " + reportTableName);
               return true;
            }

            LogFile.write(3, "Failed MYSQL add of " + playerName + " to MYSQL Report " + reportTableName);
         }

         return false;
      }
   }

   public boolean saveMySQLReportLine(String reportTableName, int id, String playerName, String playingTime, String loginTime, String todayTime, String weekTime, String monthTime, String afkToday, String afkWeek, String afkMonth, PlayerData playerData) {
      StringBuilder sb = new StringBuilder(512);
      String lastVoteDate = Output.getDateTime(playerData.lastVoteDate);
      String firstLoginDate = Output.getDateTime(playerData.firstLogin);

      try {
         ResultSet read = mysqlNew.query("SELECT * FROM `" + reportTableName + "` WHERE playerName='" + playerName + "'");
         ResultSet write;
         if (!read.next()) {
            sb.append("INSERT INTO `" + reportTableName + "`" + "(id,playerName,playtime, logintime, todaytime, weektime, monthtime," + "referredby,votes,lastVote,referrals,firstlogin,afkToday, afkWeek, afkMonth, permissions" + ", voteToday, voteWeek, voteMonth, referToday, referWeek, referMonth) ");
            sb.append("VALUES (" + id + ",'" + playerName + "','" + playingTime + "','" + loginTime + "','" + todayTime + "','" + weekTime + "','" + monthTime + "','" + playerData.referredBy + "'," + playerData.totalVotes + ",'" + lastVoteDate + "'," + playerData.totalReferrals + ",'" + firstLoginDate + "','" + afkToday + "','" + afkWeek + "','" + afkMonth + "'," + playerData.permissions);
            sb.append("," + playerData.dailyVotes + "," + playerData.weeklyVotes + "," + playerData.monthlyVotes);
            sb.append("," + playerData.dailyReferrals + "," + playerData.weeklyReferrals + "," + playerData.monthlyReferrals);
            sb.append(")");
            write = mysqlNew.query(sb.toString());
            LogFile.write(1, "Created new MySQL Report entry for " + playerName);
         } else {
            sb.append("UPDATE `" + reportTableName + "` SET playtime='" + playingTime + "', logintime='" + loginTime + "', todaytime='" + todayTime + "', weektime='" + weekTime + "', monthtime='" + monthTime + "', referredby='" + playerData.referredBy + "', votes=" + playerData.totalVotes + ", lastVote='" + lastVoteDate + "', referrals=" + playerData.totalReferrals + ", afkToday='" + afkToday + "', afkWeek='" + afkWeek + "', afkMonth='" + afkMonth + "', permissions=" + playerData.permissions);
            sb.append(", voteToday=" + playerData.dailyVotes + ", voteWeek=" + playerData.weeklyVotes + ", voteMonth=" + playerData.monthlyVotes);
            sb.append(", referToday=" + playerData.dailyReferrals + ", referWeek=" + playerData.weeklyReferrals + ", referMonth=" + playerData.monthlyReferrals);
            sb.append(" WHERE playerName='" + playerName + "'");
            write = mysqlNew.query(sb.toString());
         }

         return true;
      } catch (SQLException var19) {
         LogFile.write(3, "{savePlayerDataMySQL} MYSQL Write Error:" + var19.getMessage());
         return false;
      }
   }

   public void updateAllOnlineReport(String tableName) {
      LogFile.console(0, "Refreshing OnLine Report");
      mysqlNew.checkMySQLConnection();

      try {
         ResultSet read = mysqlNew.query("SELECT player FROM `" + tableName + "`");
         if (!read.next()) {
            LogFile.console(0, "{updateAllOnlineReport} No players in table to update");
            return;
         }

         do {
            if (this.updateOnlineReport(tableName, Players.getData(read.getString("player")))) {
               LogFile.console(0, "{updateAllOnlineReport} Updated sucessfully " + read.getString("player"));
            }
         } while(read.next());
      } catch (SQLException var4) {
         LogFile.write(10, " {updateAllOnlineReport} MYSQL Write Error:" + var4.getMessage());
      }

   }

   public boolean updateOnlineReport(String tableName, PlayerData playerData) {
      if (OnTime.MySQL_enable && playerData != null) {
         StringBuilder sb = new StringBuilder(512);
         mysqlNew.checkMySQLConnection();

         try {
            LogFile.console(0, "Updating OnLine Report for '" + playerData.playerName + "'");
            sb.append("UPDATE `" + tableName + "` SET ");
            Iterator it = Output.getOnlineFields().iterator();
            boolean atLeastOne = false;

            while(it.hasNext()) {
               String next = (String)it.next();
               if (!next.equalsIgnoreCase("player")) {
                  sb.append(next + "= '" + Output.lineOut(Output.OnTimeOutput, "[" + next + "]", playerData.playerName, playerData, (RewardData)null, -1, -1, false, (String[])null) + "'");
                  atLeastOne = true;
                  if (it.hasNext()) {
                     sb.append(",");
                  }
               }
            }

            sb.append(" WHERE player='" + playerData.playerName + "'");
            if (atLeastOne) {
               mysqlNew.query(sb.toString());
               return true;
            } else {
               return false;
            }
         } catch (SQLException var7) {
            LogFile.write(10, " {updateOnlineReport} MYSQL Write Error:" + var7.getMessage());
            LogFile.write(10, "OnLineTable update attempt: " + sb.toString());
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean saveOnlineReport(String tableName, PlayerData playerData) {
      if (!OnTime.MySQL_enable) {
         return false;
      } else {
         StringBuilder sb = new StringBuilder(512);
         mysqlNew.checkMySQLConnection();

         try {
            sb.append("INSERT INTO `" + tableName + "` (");
            Iterator it = Output.getOnlineFields().iterator();
            boolean atLeastOne = false;

            while(it.hasNext()) {
               sb.append((String)it.next());
               atLeastOne = true;
               if (it.hasNext()) {
                  sb.append(",");
               }
            }

            sb.append(") VALUES (");
            it = Output.getOnlineFields().iterator();

            while(it.hasNext()) {
               sb.append("'" + Output.lineOut(Output.OnTimeOutput, "[" + (String)it.next() + "]", playerData.playerName, playerData, (RewardData)null, -1, -1, false, (String[])null).trim() + "'");
               atLeastOne = true;
               if (it.hasNext()) {
                  sb.append(",");
               }
            }

            sb.append(")");
            if (atLeastOne) {
               mysqlNew.query(sb.toString());
               return true;
            } else {
               return false;
            }
         } catch (SQLException var6) {
            LogFile.write(10, " {saveOnlineReport} MYSQL Write Error:" + var6.getMessage());
            LogFile.write(10, "OnLineTable Save attempt: " + sb.toString());
            return false;
         }
      }
   }

   public ResultSet getCompletePlayerSet(String playerName1, UUID uuid1, String playerName2, UUID uuid2) {
      StringBuilder sb = new StringBuilder(512);
      sb.append("SELECT * FROM " + OnTime.MySQL_table + " WHERE playername='" + playerName1 + "'");
      if (playerName2 != null) {
         sb.append(" OR playername = '" + playerName2 + "'");
      }

      if (uuid1 != null) {
         sb.append(" OR uuid = '" + uuid1.toString() + "'");
      }

      if (uuid2 != null) {
         sb.append(" OR uuid = '" + uuid2.toString() + "'");
      }

      try {
         ResultSet rs = mysqlNew.query(sb.toString());
         return rs;
      } catch (Exception var8) {
         var8.printStackTrace();
         return null;
      }
   }

   public ResultSet getAbsentPlayerSet(long lastLoginDay) {
      try {
         ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE `logintime` < " + lastLoginDay + " ORDER BY `logintime` ASC");
         return rs;
      } catch (Exception var5) {
         return null;
      }
   }

   public boolean checkMySQLColumn(String columnName) {
      try {
         ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playerName='ontime-data'");
         if (rs.first()) {
            rs.getString(columnName);
         }

         return true;
      } catch (SQLException var4) {
         return false;
      }
   }

   public boolean updateMySQL() {
      boolean updated = false;
      if (!OnTime.MySQL_enable) {
         return false;
      } else {
         if (this.createOnTimePlayersTable(OnTime.MySQL_table)) {
            this.setOntimeDataMySQL();
            LogFile.console(1, "Created '" + OnTime.MySQL_table + "' table in MySQL Database.");
         }

         if (OnTime.multiServer && OnTime.primaryServer && this.createOnTimePlayersTable(OnTime.MySQL_multiServerTable)) {
            this.setOntimeDataMySQL();
            LogFile.console(1, "Created '" + OnTime.MySQL_multiServerTable + "' table in MySQL Database.");
         }

         if (OnTime.onlineTrackingEnable) {
            if (this.createOnlineTrackingTable("ontime-online")) {
               LogFile.console(1, "Created ontime-online table in MySQL Database.");
            } else {
               LogFile.console(1, "MYSQL Online Tracking table already exists or creation failure.");
            }
         } else {
            LogFile.console(1, "MYSQL Online Tracking Not Enabled.");
         }

         try {
            mysqlNew.checkMySQLConnection();
            if (!this.checkMySQLColumn("votes")) {
               LogFile.console(1, "Adding 'votes' field to MYSql Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN votes INT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("lastVote")) {
               LogFile.console(1, "Adding 'lastVote' field to MYSql Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN lastVote BIGINT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("referrals")) {
               LogFile.console(1, "Adding 'referrals' field to MYSql Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN referrals INT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("firstlogin")) {
               LogFile.console(1, "Adding 'firstlogin' field to MYSql Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN firstlogin BIGINT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("referredby")) {
               LogFile.console(1, "Adding 'referredby' field to MYSql Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN referredby varchar(64)");
               updated = true;
            }

            if (!this.checkMySQLColumn("afkToday")) {
               LogFile.console(1, "Adding 'afkToday' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN afkToday BIGINT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("afkWeek")) {
               LogFile.console(1, "Adding 'afkWeek' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN afkWeek BIGINT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("afkMonth")) {
               LogFile.console(1, "Adding 'afkMonth' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN afkMonth BIGINT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("permissions")) {
               LogFile.console(1, "Adding 'permissions' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN permissions INT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("voteToday")) {
               LogFile.console(1, "Adding 'voteToday', 'voteWeek', and 'voteMonth' fields to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN (voteToday INT  NOT NULL DEFAULT 0, voteWeek INT  NOT NULL DEFAULT 0, voteMonth INT  NOT NULL DEFAULT 0)");
               updated = true;
            }

            if (!this.checkMySQLColumn("referToday") && OnTime.collectReferDetailEnable) {
               LogFile.console(1, "Adding 'referToday', 'referWeek', and 'referMonth' fields to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN (referToday INT  NOT NULL DEFAULT 0, referWeek INT NOT NULL DEFAULT 0, referMonth INT NOT NULL DEFAULT 0)");
               updated = true;
            }

            if (!this.checkMySQLColumn("points") && OnTime.pointsEnable) {
               LogFile.console(1, "Adding 'points' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN points INT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("hostName")) {
               LogFile.console(1, "Adding 'hostName' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN hostName varchar(128) NOT NULL DEFAULT ''");
               updated = true;
            }

            if (!this.checkMySQLColumn("uuid")) {
               LogFile.console(1, "Adding 'uuid' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN uuid varchar(128)");
               updated = true;
            }

            if (!this.checkMySQLColumn("daysOn")) {
               LogFile.console(1, "Adding 'daysOn' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN daysOn INT NOT NULL DEFAULT 0");
               updated = true;
            }

            if (!this.checkMySQLColumn("world")) {
               LogFile.console(1, "Adding 'world' field to MySQL Database.");
               mysqlNew.query("ALTER TABLE  " + OnTime.MySQL_table + " ADD COLUMN world varchar(32) NOT NULL DEFAULT '" + OnTime.serverID + "'");
               updated = true;
            }

            if (OnTime.primaryServer) {
               ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " WHERE playerName='ontime-data'");
               if (rs.first()) {
                  long tableVersion = 0L;
                  long playtime = 0L;
                  long todaytime = 0L;
                  playtime = rs.getLong("playtime");
                  todaytime = rs.getLong("todaytime");
                  if (todaytime == 0L) {
                     if (playtime > 3L) {
                        todaytime = playtime;
                        playtime = 0L;
                     } else {
                        todaytime = TodayTime.todayMidnight();
                     }

                     mysqlNew.query("UPDATE " + OnTime.MySQL_table + " SET todaytime=" + todaytime + " WHERE playerName='ontime-data'");
                  }

                  if (playtime <= 3L) {
                     tableVersion = playtime;
                  } else {
                     tableVersion = 0L;
                  }

                  if (tableVersion < 1L && OnTime.primaryServer) {
                     mysqlNew.query("ALTER TABLE " + OnTime.MySQL_table + " ADD INDEX (playerName)");
                     mysqlNew.query("UPDATE " + OnTime.MySQL_table + " SET playtime=1 WHERE playerName='ontime-data'");
                     LogFile.console(1, "Added indexing to MySQL table " + OnTime.MySQL_table + " for column 'playerName'");
                     updated = true;
                     tableVersion = 1L;
                  }

                  if (tableVersion < 2L) {
                     mysqlNew.query("ALTER TABLE " + OnTime.MySQL_table + " MODIFY hostName VARCHAR(128)");
                     mysqlNew.query("UPDATE " + OnTime.MySQL_table + " SET playtime=2 WHERE playerName='ontime-data'");
                     LogFile.console(1, "Increased size of hostName to 128 in " + OnTime.MySQL_table);
                     updated = true;
                     tableVersion = 2L;
                  }

                  if (tableVersion < 3L) {
                     String table = OnTime.MySQL_table;
                     if (OnTime.multiServer) {
                        table = OnTime.MySQL_multiServerTable;
                     }

                     ResultSet data = mysqlNew.query("SELECT id FROM " + table + " WHERE playerName='ontime-data'");
                     if (data.first()) {
                        mysqlNew.query("UPDATE " + table + " SET id=" + (data.getInt("id") + 1) + " WHERE playerName='ontime-data'");
                        LogFile.console(1, "Incremented player id counter in for new handling rules " + OnTime.MySQL_table);
                     }

                     updated = true;
                     mysqlNew.query("UPDATE " + OnTime.MySQL_table + " SET playtime=3 WHERE playerName='ontime-data'");
                     tableVersion = 3L;
                  }
               }
            }

            mysqlNew.query("UPDATE " + OnTime.MySQL_table + " SET world='server' WHERE world='system'");
         } catch (SQLException var11) {
            LogFile.console(3, "{upateMySQL} ERROR Adding new fields. " + var11.getMessage());
         }

         if (updated) {
            LogFile.console(1, "Upgraded MySQL Database '" + OnTime.MySQL_table + "' table to the latest format.");
         }

         return updated;
      }
   }

   public static OnTimeAPI.topData[] getTopData(OnTimeAPI.data data) {
      return _plugin.get_dataio()._getTopData(data);
   }

   public OnTimeAPI.topData[] _getTopData(OnTimeAPI.data data) {
      OnTimeAPI.topData[] topData = new OnTimeAPI.topData[OnTime.topListMax];
      String column = null;
      switch (data) {
         case TOTALPLAY:
            column = "playtime";
            break;
         case TODAYPLAY:
            column = "todaytime";
            break;
         case WEEKPLAY:
            column = "weektime";
            break;
         case MONTHPLAY:
            column = "monthtime";
            break;
         case LASTLOGIN:
         case LASTVOTE:
         default:
            LogFile.console(0, "{_getTopData} Invalid dataType: " + data.toString());
            return null;
         case TOTALVOTE:
            column = "votes";
            break;
         case TODAYVOTE:
            column = "voteToday";
            break;
         case WEEKVOTE:
            column = "voteWeek";
            break;
         case MONTHVOTE:
            column = "voteMonth";
            break;
         case TOTALREFER:
            column = "referrals";
            break;
         case TODAYREFER:
            column = "referToday";
            break;
         case WEEKREFER:
            column = "referWeek";
            break;
         case MONTHREFER:
            column = "referMonth";
            break;
         case TOTALPOINT:
            column = "points";
      }

      Boolean more = true;

      try {
         ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " ORDER BY `" + column + "` DESC");
         more = rs.first();
         int i = 0;
         boolean emptyList = true;
         topData[0] = new OnTimeAPI.topData();

         while(more) {
            boolean next = false;
            boolean include = true;
            String referredBy = rs.getString("referredby");
            String playerName = rs.getString("playername");
            if (playerName.equalsIgnoreCase("ontime-data")) {
               include = false;
            }

            if (referredBy != null && (referredBy.equalsIgnoreCase("votifier-service") || referredBy.equalsIgnoreCase("ontime-report"))) {
               include = false;
            }

            if ((rs.getInt("permissions") & PlayerData.OTPerms.TOPTEN.mask()) != 0) {
               include = false;
            }

            if (include) {
               emptyList = false;
               topData[i].setPlayerName(rs.getString("playername"));
               switch (data) {
                  case TOTALPLAY:
                  case TODAYPLAY:
                  case WEEKPLAY:
                  case MONTHPLAY:
                     topData[i].setValue(rs.getLong(column));
                  case LASTLOGIN:
                  case LASTVOTE:
                  default:
                     break;
                  case TOTALVOTE:
                  case TODAYVOTE:
                  case WEEKVOTE:
                  case MONTHVOTE:
                  case TOTALREFER:
                  case TODAYREFER:
                  case WEEKREFER:
                  case MONTHREFER:
                  case TOTALPOINT:
                     topData[i].setValue((long)rs.getInt(column));
               }

               next = true;
            }

            if (next) {
               if (topData[i].getValue() > 0L) {
                  ++i;
               } else {
                  topData[i] = null;
                  more = false;
               }
            }

            if (more) {
               if (i == OnTime.topListMax) {
                  more = false;
               } else {
                  more = rs.next();
               }
            }

            if (more && next) {
               topData[i] = new OnTimeAPI.topData();
            }
         }

         if (emptyList) {
            LogFile.console(0, "{_getTopData} Empty List for : " + data.toString());
            return null;
         }
      } catch (SQLException var12) {
         LogFile.console(3, "{_getTopData} MYSQL Error: " + var12.getMessage());
         more = false;
      }

      return topData;
   }

   public boolean topListFromMySQL(CommandSender sender, String section, int playerCount, dataset dataSet) {
      String data = null;
      String playerName = null;
      StringBuilder sb = new StringBuilder(512);
      String referredBy = null;
      String label;
      if (section.contains("vote")) {
         label = Output.OnTimeOutput.getString("output.eventRef.vote");
      } else if (section.contains("refer")) {
         label = Output.OnTimeOutput.getString("output.eventRef.refer");
      } else if (section.contains("point")) {
         label = Output.OnTimeOutput.getString("output.eventRef.point");
      } else {
         label = Output.OnTimeOutput.getString("output.eventRef.afk");
      }

      boolean more = true;

      try {
         ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_table + " ORDER BY `" + section + "` DESC");
         more = rs.first();
         int i = 1;
         boolean emptyList = true;

         while(true) {
            do {
               do {
                  do {
                     do {
                        if (!more) {
                           if (emptyList) {
                              sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.topListError.noPlayers"));
                              return false;
                           }

                           return true;
                        }

                        playerName = rs.getString("playername");
                        data = rs.getString(section);
                        referredBy = rs.getString("referredby");
                        if (!rs.next()) {
                           more = false;
                        }
                     } while(playerName.equalsIgnoreCase("ontime-data"));
                  } while(playerName.equalsIgnoreCase("todaydate"));
               } while(data == null);
            } while((dataSet != DataIO.dataset.PLAYER || referredBy.equalsIgnoreCase("votifier-service")) && (dataSet != DataIO.dataset.SERVER || !referredBy.equalsIgnoreCase("votifier-service")));

            if (!data.equalsIgnoreCase("0")) {
               if (section.contains("afk")) {
                  data = Output.getTimeBreakdown(Long.valueOf(data), Output.TIMEDETAIL.SHORT);
               }

               if (playerName.length() > 14) {
                  playerName = playerName.substring(0, 13);
               }

               sb.append("# ");
               if (i < 10) {
                  sb.append(" ");
               }

               sb.append(Integer.toString(i) + ": " + ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListNameColor").substring(1)) + playerName);
               sb.append("                                            ", sb.length(), 24);
               sb.append(label + ": " + ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListExtrasColor").substring(1)) + data);
               sender.sendMessage(sb.toString());
               sb.delete(0, sb.length());
               emptyList = false;
            }

            ++i;
            if (i > playerCount) {
               more = false;
            }
         }
      } catch (SQLException var14) {
         LogFile.console(3, "{topListFromMySQL} MYSQL Error: " + var14.getMessage());
         more = false;
         return true;
      }
   }

   public String[] getDataListFromMySQL(String index, String opperand, String indexColumn, String orderColumn, String dataColumn, String ascDesc) {
      String query = null;

      try {
         query = "SELECT " + dataColumn + " FROM " + OnTime.MySQL_table + " WHERE " + indexColumn + opperand + index + " AND world='" + OnTime.serverID + "' ORDER BY " + orderColumn + " " + ascDesc;
         ResultSet rs = mysqlNew.query(query);
         if (!rs.first()) {
            return null;
         } else {
            int count;
            for(count = 1; rs.next(); ++count) {
            }

            String[] list = new String[count];
            count = 0;

            for(boolean more = rs.first(); more; ++count) {
               list[count] = rs.getString(dataColumn);
               more = rs.next();
            }

            return list;
         }
      } catch (SQLException var12) {
         LogFile.console(3, "{getDataListFromMySQL} MYSQL Error: " + var12.getMessage());
         return null;
      }
   }

   public int removeExpiredReportsFromMySQL(String reportName, Long expirationDate) {
      int count = 0;
      String query = null;

      try {
         query = "SELECT playerName FROM " + OnTime.MySQL_table + " WHERE playerName LIKE '%" + reportName + "%' AND lastVote < " + expirationDate;
         ResultSet rs = mysqlNew.query(query);
         if (!rs.first()) {
            return 0;
         } else {
            for(boolean more = rs.first(); more; more = rs.next()) {
               String table = rs.getString("playerName");
               this.dropTable(table);
               mysqlNew.query("DELETE FROM '" + OnTime.MySQL_table + "' WHERE playername='" + table + "'");
               ++count;
               LogFile.console(1, "Removed " + table + " table from MySQL database.");
            }

            return count;
         }
      } catch (SQLException var8) {
         LogFile.console(3, "{getDataListFromMySQL} MYSQL Error: " + var8.getMessage());
         LogFile.console(3, "{getDataListFromMySQL} " + query);
         return count;
      }
   }

   public boolean dropTable(String table) {
      try {
         mysqlNew.query("DROP TABLE IF EXISTS`" + table + "`");
         return true;
      } catch (SQLException var3) {
         LogFile.console(3, "{dropTable} MYSQL Error: " + var3.getMessage());
         return false;
      }
   }

   public static void createFile(File file) {
      if (file.exists()) {
         file.delete();
      }

      try {
         file.createNewFile();
      } catch (IOException var2) {
         LogFile.console(3, "{createFile} FILE Error: " + var2.getMessage());
      }

   }

   public static void writeLine(File file, String line) {
      try {
         BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
         out.write(line);
         out.newLine();
         out.close();
      } catch (IOException var3) {
         LogFile.console(3, "{writeLine} FILE Error: " + var3.getMessage());
      }

   }

   public void setupMySqlVar() {
      try {
         mysqlNew.setdb(OnTime.pluginName, OnTime.logger, OnTime.MySQL_host, OnTime.MySQL_port, OnTime.MySQL_user, OnTime.MySQL_password, OnTime.MySQL_database, OnTime.MySQL_table);
      } catch (NoSuchMethodError var2) {
         LogFile.console(3, "{setupMySqlVar} MySQL Error: " + var2.toString());
      }

   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$DataIO$REMOVEKEY() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$DataIO$REMOVEKEY;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[DataIO.REMOVEKEY.values().length];

         try {
            var0[DataIO.REMOVEKEY.MAX.ordinal()] = 6;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[DataIO.REMOVEKEY.NAME_UUID.ordinal()] = 4;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[DataIO.REMOVEKEY.NAME_UUID_ID.ordinal()] = 5;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[DataIO.REMOVEKEY.PLAYER.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[DataIO.REMOVEKEY.PLAYERNAME.ordinal()] = 1;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[DataIO.REMOVEKEY.UUID.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$DataIO$REMOVEKEY = var0;
         return var0;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$OnTimeAPI$data() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$OnTimeAPI$data;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[OnTimeAPI.data.values().length];

         try {
            var0[OnTimeAPI.data.LASTLOGIN.ordinal()] = 5;
         } catch (NoSuchFieldError var15) {
         }

         try {
            var0[OnTimeAPI.data.LASTVOTE.ordinal()] = 10;
         } catch (NoSuchFieldError var14) {
         }

         try {
            var0[OnTimeAPI.data.MONTHPLAY.ordinal()] = 4;
         } catch (NoSuchFieldError var13) {
         }

         try {
            var0[OnTimeAPI.data.MONTHREFER.ordinal()] = 14;
         } catch (NoSuchFieldError var12) {
         }

         try {
            var0[OnTimeAPI.data.MONTHVOTE.ordinal()] = 9;
         } catch (NoSuchFieldError var11) {
         }

         try {
            var0[OnTimeAPI.data.TODAYPLAY.ordinal()] = 2;
         } catch (NoSuchFieldError var10) {
         }

         try {
            var0[OnTimeAPI.data.TODAYREFER.ordinal()] = 12;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[OnTimeAPI.data.TODAYVOTE.ordinal()] = 7;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[OnTimeAPI.data.TOTALPLAY.ordinal()] = 1;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[OnTimeAPI.data.TOTALPOINT.ordinal()] = 15;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[OnTimeAPI.data.TOTALREFER.ordinal()] = 11;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[OnTimeAPI.data.TOTALVOTE.ordinal()] = 6;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[OnTimeAPI.data.WEEKPLAY.ordinal()] = 3;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[OnTimeAPI.data.WEEKREFER.ordinal()] = 13;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[OnTimeAPI.data.WEEKVOTE.ordinal()] = 8;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$OnTimeAPI$data = var0;
         return var0;
      }
   }

   public static enum DATATYPES {
      BIGINT,
      STRING,
      INTEGER;
   }

   public static enum REMOVEKEY {
      PLAYERNAME,
      PLAYER,
      UUID,
      NAME_UUID,
      NAME_UUID_ID,
      MAX;
   }

   public class _incrementPlayerVotes implements Runnable {
      private String playerName;

      public _incrementPlayerVotes(String _playerName) {
         this.playerName = _playerName;
      }

      public void run() {
         DataIO.this.incrementVoteCounts(this.playerName);
      }
   }

   public class _removePlayerFromTable implements Runnable {
      private REMOVEKEY key;
      private PlayerData playerData;
      private String table;

      public _removePlayerFromTable(REMOVEKEY _key, PlayerData _playerData, String _table) {
         this.key = _key;
         this.playerData = _playerData;
         this.table = _table;
      }

      public void run() {
         DataIO.this.removePlayerFromTable(this.key, this.playerData, this.table);
      }
   }

   public class _savePlayerDataMySQLAsync implements Runnable {
      private String playerName;
      private boolean saveReports;

      public _savePlayerDataMySQLAsync(String _playerName, boolean _saveReports) {
         this.playerName = _playerName;
         this.saveReports = _saveReports;
      }

      public void run() {
         DataIO.this.savePlayerDataMySQL(this.playerName, this.saveReports);
      }
   }

   public static enum dataset {
      PLAYER,
      SERVER;
   }

   public static enum datastorage {
      YML,
      MYSQL;
   }

   public static enum mysqlload {
      ALL,
      OFFLINE,
      INIT,
      PURGE,
      ABSENCE,
      MISSINGUUID;
   }

   public static enum mysqlsave {
      ALL,
      ONLINE;
   }

   public static enum playerYML {
      UUID(0, 0, 0, 0, 0, 0, 0),
      PLAYERNAME(0, 0, 0, 0, 0, 1, 1),
      PLAYTIME(1, 1, 1, 1, 1, 2, 2),
      LOGINTIME(2, 2, 2, 2, 2, 3, 3),
      TODAYTIME(-1, -1, -1, 3, 3, 4, 4),
      WEEKTIME(-1, -1, -1, 4, 4, 5, 5),
      MONTHTIME(-1, -1, -1, 5, 5, 6, 6),
      TOTALVOTE(-1, -1, -1, 6, 6, 7, 7),
      TODAYVOTE(-1, -1, -1, 7, 7, 8, 8),
      WEEKVOTE(-1, -1, -1, 8, 8, 9, 9),
      MONTHVOTE(-1, -1, -1, 9, 9, 10, 10),
      DAYSON(-1, -1, -1, 11, 11, 11, 11);

      private final int[] v = new int[7];

      private playerYML(int v0, int v1, int v2, int v3, int v4, int v5, int v6) {
         this.v[0] = v0;
         this.v[1] = v1;
         this.v[2] = v2;
         this.v[3] = v3;
         this.v[4] = v4;
         this.v[5] = v5;
         this.v[6] = v6;
      }
   }

   public class saveMySQLReport implements Runnable {
      private int id;
      private String playerName;
      private String playingTime;
      private String loginTime;
      private String todayTime;
      private String weekTime;
      private String monthTime;
      private String afkToday;
      private String afkWeek;
      private String afkMonth;
      private PlayerData playerData;

      public saveMySQLReport(int _id, String _playerName, String _playingTime, String _loginTime, String _todayTime, String _weekTime, String _monthTime, String _afkToday, String _afkWeek, String _afkMonth, PlayerData _playerData) {
         this.id = _id;
         this.playerName = _playerName;
         this.playingTime = _playingTime;
         this.loginTime = _loginTime;
         this.todayTime = _todayTime;
         this.weekTime = _weekTime;
         this.monthTime = _monthTime;
         this.afkToday = _afkToday;
         this.afkWeek = _afkWeek;
         this.afkMonth = _afkMonth;
         this.playerData = _playerData;
      }

      public void run() {
         DataIO.this.saveMySQLReportsFunc(this.id, this.playerName, this.playingTime, this.loginTime, this.todayTime, this.weekTime, this.monthTime, this.afkToday, this.afkWeek, this.afkMonth, this.playerData);
      }
   }
}
