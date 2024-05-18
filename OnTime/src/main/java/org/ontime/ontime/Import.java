package org.ontime.ontime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

public class Import {
   public int uuidRetry = 0;
   public static BukkitTask uuidRetryTask = null;
   private static OnTime _plugin;
   static FileConfiguration playtimeImport;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Import$UUIDFUNC;

   public Import(OnTime plugin) {
      _plugin = plugin;
   }

   public String fromYML(String fileName, boolean replace, String loadPlayer) {
      File file = new File(OnTime.onTimeDataFolder, fileName);
      if (!file.exists()) {
         return file.getPath() + " does not exist.";
      } else {
         playtimeImport = new YamlConfiguration();

         try {
            playtimeImport.load(file);
         } catch (Exception var20) {
            var20.printStackTrace();
            return "ERROR Reading import file " + file.getPath();
         }

         boolean millis = false;
         boolean minutes = false;
         String timeUnit = null;
         int numberImported = 0;
         if (playtimeImport.getInt("playerDataVersion") == 0) {
            LogFile.console(1, "Importing from " + file.getPath() + " (Generic Format)");
            timeUnit = playtimeImport.getString("timeUnit");
            if (timeUnit == null) {
               return "ERROR Reading import file " + file.getPath();
            }

            if (timeUnit.equalsIgnoreCase("minutes")) {
               minutes = true;
            } else if (timeUnit.equalsIgnoreCase("millis")) {
               millis = true;
            }

            List playerList = playtimeImport.getStringList("players");
            if (playerList.size() == 0) {
               return " There are no players listed in " + file.getPath();
            }

            String[] names = new String[playerList.size() + 1];

            String s;
            Iterator var15;
            int i;
            for(var15 = playerList.iterator(); var15.hasNext(); names[i++] = s.substring(0, s.indexOf(",") - 1)) {
               s = (String)var15.next();
               i = 0;
            }

            var15 = playerList.iterator();

            label82:
            while(true) {
               String playerName;
               String[] tokens;
               do {
                  if (!var15.hasNext()) {
                     break label82;
                  }

                  s = (String)var15.next();
                  String delims = "[,]";
                  tokens = s.split(delims);
                  LogFile.console(4, tokens[0] + " -> " + tokens[1]);
                  playerName = tokens[0];
               } while(!playerName.equalsIgnoreCase(loadPlayer) && loadPlayer != null);

               if (!tokens[1].matches("[+-]?\\d+(\\/\\d+)?")) {
                  return "Import aborted.  Data '" + tokens[1] + "' invalid for " + tokens[0];
               }

               long playingTime = Long.valueOf(tokens[1]);
               if (!millis) {
                  if (minutes) {
                     playingTime = TimeUnit.MINUTES.toMillis(playingTime);
                  } else {
                     playingTime = TimeUnit.SECONDS.toMillis(playingTime);
                  }
               }

               if (replace && Players.playerHasData(playerName)) {
                  Players.remove(playerName);
               }

               if (!Players.playerHasData(playerName)) {
                  long firstLogin = 0L;
                  Players.putData(playerName, Players.getNew((UUID)null, playerName, firstLogin, playingTime));
               }

               ++numberImported;
            }
         } else {
            LogFile.console(1, "Importing from " + file.getPath() + " (OnTime Format)");
            if (playtimeImport.getString("players.player0") == null) {
               return " There are no players listed in " + file.getPath();
            }

            numberImported = _plugin.get_dataio().loadPlayerDataYML(fileName, loadPlayer, replace, true);
            if (numberImported < 0) {
               return "Import of " + loadPlayer + " failed.  No record found in " + file.getPath();
            }
         }

         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            _plugin.get_dataio().saveAllPlayerDataMySQL((CommandSender)null, DataIO.mysqlsave.ALL, -1, -1, (String)null);
         } else {
            _plugin.get_dataio().saveAllData(OnTime.onTimeDataFolder);
         }

         this.scheduleUpdateUUID((CommandSender)null, Import.UUIDFUNC.FIND, (String)null, (String)null);
         LogFile.write(2, "PlayTime Import of " + numberImported + " player's data loaded from " + file.getPath());
         return "Successful import of " + numberImported + " player's data from " + file.getPath();
      }
   }

   public List getNameList(String query, String column) throws SQLException {
      List result = new ArrayList();
      ResultSet rset = null;

      try {
         rset = DataIO.mysqlNew.query(query);

         while(rset.next()) {
            result.add(rset.getString(column));
         }
      } finally {
         if (rset != null) {
            try {
               rset.close();
            } catch (SQLException var10) {
            }
         }

      }

      return result;
   }

   public String fromLogBlock(boolean replace) {
      if (!OnTime.MySQL_enable) {
         return ChatColor.RED + "MYSQL Database Access not enabled for OnTime.  Please see ontime/config.yml";
      } else {
         LogFile.console(3, "LogBlock MySQL Database Opened");
         int numberImported = 0;
         boolean more = true;
         String onlinetime = null;
         String playerName = null;
         String lastLogin = null;
         Long loginTime = 0L;
         Timestamp loginTimestamp = null;
         UUID uuid = null;

         ResultSet rs;
         try {
            rs = DataIO.mysqlNew.query("SELECT * FROM `lb-players`");
            more = rs.first();
         } catch (SQLException var19) {
            return ChatColor.RED + "LogBlock 'lb-players' table does not exist in MYSQL Database: " + OnTime.MySQL_database;
         }

         Long currentTime = Calendar.getInstance().getTimeInMillis();

         while(more) {
            onlinetime = null;
            playerName = null;
            lastLogin = null;
            loginTime = 0L;
            loginTimestamp = null;

            try {
               playerName = rs.getString("playername");
               onlinetime = rs.getString("onlinetime");
               LogFile.console(1, "LogBlock Import of " + playerName + "->" + onlinetime);
            } catch (SQLException var18) {
               var18.printStackTrace();
               more = false;
            }

            try {
               loginTimestamp = rs.getTimestamp("lastlogin");
               loginTime = loginTimestamp.getTime();
               LogFile.console(0, "LogBlock: 'lastlogin' (" + loginTimestamp.toString() + ")");
            } catch (SQLException var17) {
               LogFile.console(2, "LogBlock Import Error.  Could not read timestamp from 'lastlogin' (" + lastLogin + ") for " + playerName);
               loginTime = currentTime;
            }

            try {
               if (!rs.next()) {
                  more = false;
               }
            } catch (SQLException var16) {
               more = false;
            }

            if (onlinetime != null) {
               long playingTime = 0L;
               if (onlinetime.contains(":")) {
                  String delims = "[:]";
                  String[] tokens = onlinetime.split(delims);
                  playingTime = TimeUnit.HOURS.toMillis(Long.valueOf(tokens[0])) + TimeUnit.MINUTES.toMillis(Long.valueOf(tokens[1])) + TimeUnit.SECONDS.toMillis(Long.valueOf(tokens[2]));
               } else {
                  playingTime = TimeUnit.SECONDS.toMillis(Long.valueOf(onlinetime));
               }

               if ((Players.playerHasData(playerName) || replace) && playingTime > 0L) {
                  LogFile.write(1, "Imported: " + playerName + " with total OnTime of " + Output.getTimeBreakdown(playingTime, Output.TIMEDETAIL.LONG));
                  LogFile.console(1, "Imported: " + playerName + " with total OnTime of " + Output.getTimeBreakdown(playingTime, Output.TIMEDETAIL.LONG));
                  if (this.storePlayer((UUID)uuid, playerName, playingTime, loginTime, replace)) {
                     ++numberImported;
                  }
               } else {
                  LogFile.console(0, playerName + " not imported.");
               }
            }
         }

         if (numberImported > 0) {
            if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
               _plugin.get_dataio().saveAllPlayerDataMySQL((CommandSender)null, DataIO.mysqlsave.ALL, -1, -1, (String)null);
            } else {
               _plugin.get_dataio().saveAllData(OnTime.onTimeDataFolder);
            }
         }

         LogFile.write(2, "Import of " + numberImported + " player's data from MySQL DB:" + OnTime.MySQL_database);
         this.scheduleUpdateUUID((CommandSender)null, Import.UUIDFUNC.FIND, (String)null, (String)null);
         return "LogBlock Import Complete";
      }
   }

   public String fromReport(String fileName, boolean replace, String loadPlayer) {
      File infile = new File(OnTime.onTimeDataFolder, fileName);
      if (!infile.exists()) {
         return infile.getPath() + " does not exist.";
      } else {
         StringBuilder sb = new StringBuilder(65536);
         int playersLoaded = 0;

         FileInputStream in;
         BufferedReader inStream;
         String dayString;
         try {
            in = new FileInputStream(infile);
            inStream = new BufferedReader(new InputStreamReader(in));

            for(dayString = inStream.readLine(); dayString != null; dayString = inStream.readLine()) {
               if (dayString.startsWith("#")) {
                  String[] tokens = dayString.split(" ");
                  sb.append(tokens[1] + ",");
               }
            }

            in.close();
         } catch (FileNotFoundException var26) {
            var26.printStackTrace();
            return "Error accessing Report file. File Not Found";
         } catch (IOException var27) {
            var27.printStackTrace();
            return "Error accessing Report file. IO Exception";
         }

         try {
            in = new FileInputStream(infile);
            inStream = new BufferedReader(new InputStreamReader(in));
            dayString = Output.OnTimeOutput.getString("output.time.days").trim();
            String hourString = Output.OnTimeOutput.getString("output.time.hours").trim();
            String minString = Output.OnTimeOutput.getString("output.time.minutes").trim();

            for(String line = inStream.readLine(); line != null; line = inStream.readLine()) {
               if (line.startsWith("#") && (loadPlayer == null || line.contains(loadPlayer))) {
                  long days = 0L;
                  long hours = 0L;
                  long min = 0L;
                  String[] tokens = line.split(" ");
                  int i = 0;
                  if (line.contains(" Total:")) {
                     while(!tokens[i].equalsIgnoreCase("Total:")) {
                        ++i;
                     }
                  }

                  if (line.contains(" " + dayString + " ")) {
                     while(!tokens[i].equalsIgnoreCase(dayString)) {
                        ++i;
                     }

                     if (tokens[i - 1].length() > 0) {
                        days = (long)Integer.parseInt(tokens[i - 1]);
                     } else {
                        days = (long)Integer.parseInt(tokens[i - 2]);
                     }

                     ++i;
                  }

                  if (line.contains(" " + hourString + " ")) {
                     while(!tokens[i].equalsIgnoreCase(hourString)) {
                        ++i;
                     }

                     if (tokens[i - 1].length() > 0) {
                        hours = (long)Integer.parseInt(tokens[i - 1]);
                     } else {
                        hours = (long)Integer.parseInt(tokens[i - 2]);
                     }

                     ++i;
                  }

                  if (line.contains(" " + minString + " ")) {
                     while(!tokens[i].equalsIgnoreCase(minString)) {
                        ++i;
                     }

                     if (tokens[i - 1].length() > 0) {
                        min = (long)Integer.parseInt(tokens[i - 1]);
                     } else {
                        min = (long)Integer.parseInt(tokens[i - 2]);
                     }
                  }

                  long playtime = TimeUnit.DAYS.toMillis(days) + TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(min);
                  long firstLogin = 0L;
                  if (this.storePlayer((UUID)null, tokens[1], playtime, firstLogin, replace)) {
                     ++playersLoaded;
                  }
               }
            }

            in.close();
         } catch (Exception var25) {
            var25.printStackTrace();
            return "Error accessing Report file.";
         }

         if (playersLoaded > 0) {
            if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
               _plugin.get_dataio().saveAllPlayerDataMySQL((CommandSender)null, DataIO.mysqlsave.ALL, -1, -1, (String)null);
            } else {
               _plugin.get_dataio().saveAllData(OnTime.onTimeDataFolder);
            }

            this.scheduleUpdateUUID((CommandSender)null, Import.UUIDFUNC.FIND, (String)null, (String)null);
         }

         LogFile.write(2, "Report Import of " + playersLoaded + " player's data loaded from " + infile.getPath());
         return "Successful import of " + playersLoaded + " player's data from " + infile.getPath();
      }
   }

   private boolean storePlayer(UUID uuid, String playerName, long playingTime, long firstLogin, boolean replace) {
      boolean savedPlayer = true;
      if (Players.hasOnTimeRecord(playerName)) {
         if (replace) {
            PlayerData playerdata = Players.getData(uuid);
            playerdata.uuid = uuid;
            Players.getWorldTime(playerdata, OnTime.serverID).totalTime = playingTime;
            playerdata.firstLogin = firstLogin;
         } else {
            savedPlayer = false;
         }
      } else {
         Players.putData(uuid, Players.getNew(uuid, playerName, firstLogin, playingTime));
      }

      if (savedPlayer) {
         LogFile.write(1, "Imported: " + playerName + " with total OnTime of " + Output.getTimeBreakdown(playingTime, Output.TIMEDETAIL.LONG));
         return true;
      } else {
         return false;
      }
   }

   public void scheduleUpdateUUID(CommandSender sender, UUIDFUNC uuidFunc, String playerName, String playerName2) {
      try {
         _plugin.getServer().getScheduler().runTaskAsynchronously(_plugin, new updateUUIDevent(sender, uuidFunc, playerName, playerName2));
      } catch (NoSuchMethodError var6) {
         _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new updateUUIDevent(sender, uuidFunc, playerName, playerName2), 1L);
      }

      if (sender == null) {
         LogFile.console(3, "Update of missing UUIDs scheduled.");
      }

   }

   public void scheduleUpdateUUIDdelayed(CommandSender sender, UUIDFUNC uuidFunc, String playerName, String playerName2) {
      uuidRetryTask = _plugin.getServer().getScheduler().runTaskLaterAsynchronously(_plugin, new updateUUIDevent(sender, uuidFunc, playerName, playerName2), 12000L);
      if (sender == null) {
         LogFile.console(3, "Update of missing UUIDs scheduled in 10 minutes.");
      }

   }

   public void updateUUID(CommandSender sender, UUIDFUNC uuidFunc, String playerName1, String playerName2) {
      String[] keys = null;
      String[] names = null;
      int updateCount = 0;
      int purgeCount = 0;
      int missingCount = 0;
      int dupDeletedCount = 0;
      int mergedCount = 0;
      if (playerName1 == null) {
         playerName1 = "null";
      }

      if (playerName2 == null) {
         playerName2 = "null";
      }

      if (sender != null) {
         LogFile.write(3, "UUID Function : " + uuidFunc.toString() + " initiated by " + sender.getName() + " on " + playerName1 + " and " + playerName2);
      }

      OnTime.get_commands().suspendOnTime();
      if (sender != null && OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         if (uuidFunc != Import.UUIDFUNC.FIND && uuidFunc != Import.UUIDFUNC.PURGE) {
            if (uuidFunc == Import.UUIDFUNC.MERGE) {
               if (playerName1.equalsIgnoreCase("all")) {
                  _plugin.get_dataio().loadAllPlayerDataMySQL(DataIO.mysqlload.ALL);
               } else {
                  _plugin.get_dataio().loadPlayerDataMySQL(playerName1);
                  if (!playerName2.equalsIgnoreCase("null")) {
                     _plugin.get_dataio().loadPlayerDataMySQL(playerName2);
                  }
               }
            } else {
               _plugin.get_dataio().loadAllPlayerDataMySQL(DataIO.mysqlload.ALL);
            }
         } else {
            _plugin.get_dataio().loadAllPlayerDataMySQL(DataIO.mysqlload.MISSINGUUID);
         }
      }

      if (_plugin.get_dataio().getPlayerMap().isEmpty()) {
         if (sender != null) {
            sender.sendMessage("UUID Import: No player records found to update.");
         } else {
            LogFile.console(3, "{updateUUID} No player records found to update.");
         }

         OnTime.get_commands().resumeOnTime();
      } else {
         int nameCount;
         if (uuidFunc != Import.UUIDFUNC.FIND && uuidFunc != Import.UUIDFUNC.REPLACE) {
            PlayerData playerData1;
            if (uuidFunc != Import.UUIDFUNC.CLEAN && (uuidFunc != Import.UUIDFUNC.MERGE || !playerName1.equalsIgnoreCase("all"))) {
               PlayerData playerData1 = Players.getData(playerName1);
               playerData1 = null;
               if (!playerName2.equalsIgnoreCase("null")) {
                  playerData1 = Players.getData(playerName2);
               }

               if (this.mergeOrCleanRecords(uuidFunc, playerData1, playerData1)) {
                  if (playerData1 == null) {
                     sender.sendMessage("Duplicate records for " + playerData1.playerName + "(" + playerData1.uuid.toString() + ") combined into single record.");
                     LogFile.write(3, "Duplicate records for " + playerData1.playerName + "(" + playerData1.uuid.toString() + ") combined into single record.");
                  } else {
                     sender.sendMessage("Duplicate records for " + playerData1.playerName + "(" + playerData1.uuid.toString() + ") and " + playerName2 + " combined into single record.");
                     LogFile.write(3, "Duplicate records for " + playerData1.playerName + "(" + playerData1.uuid.toString() + ") and " + playerName2 + " combined into single record.");
                  }

                  ++mergedCount;
               }
            } else {
               OnTime.get_commands().suspendOnTime();
               keys = new String[_plugin.get_dataio().getPlayerMap().size()];
               _plugin.get_dataio().getPlayerMap().keySet().toArray(keys);
               _plugin.cancelAuditLogout();

               for(nameCount = _plugin.get_dataio().getPlayerMap().size() - 1; nameCount >= 0; --nameCount) {
                  playerData1 = Players.getData(keys[nameCount]);
                  if (this.mergeOrCleanRecords(uuidFunc, playerData1, (PlayerData)null)) {
                     if (uuidFunc == Import.UUIDFUNC.CLEAN) {
                        ++dupDeletedCount;
                     } else {
                        sender.sendMessage("Duplicate records for " + playerData1.playerName + "(" + playerData1.uuid.toString() + ") combined into single record.");
                        LogFile.write(3, "Duplicate records for " + playerData1.playerName + "(" + playerData1.uuid.toString() + ") combined into single record.");
                        ++mergedCount;
                     }
                  }
               }

               OnTime.get_commands().resumeOnTime();
            }
         } else {
            nameCount = 0;
            int totalNames = 0;
            String key;
            Iterator uuidMap;
            if (uuidFunc == Import.UUIDFUNC.REPLACE) {
               totalNames = _plugin.get_dataio().getPlayerMap().size();
            } else {
               uuidMap = _plugin.get_dataio().getPlayerMap().keySet().iterator();

               while(uuidMap.hasNext()) {
                  key = (String)uuidMap.next();
                  if (Players.getData(key).uuid == null) {
                     ++totalNames;
                  }
               }
            }

            if (totalNames == 0) {
               if (sender != null) {
                  sender.sendMessage("UUID Import: No player found to be missing UUIDs.");
               } else {
                  LogFile.console(3, "{updateUUID} No player found to be missing UUIDs.");
               }

               OnTime.get_commands().resumeOnTime();
               return;
            }

            names = new String[totalNames];
            uuidMap = _plugin.get_dataio().getPlayerMap().keySet().iterator();

            label223:
            while(true) {
               PlayerData playerData;
               do {
                  if (!uuidMap.hasNext()) {
                     LogFile.write(0, "{updateUUID} Looking for: " + nameCount + " missing UUIDs");
                     UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(names));
                     uuidMap = null;

                     try {
                        Map uuidMap = fetcher.call();
                        LogFile.write(3, "{updateUUID} UUID Fetcher found " + uuidMap.size() + " players.");
                        if (uuidMap.size() > 0) {
                           keys = new String[_plugin.get_dataio().getPlayerMap().size()];
                           _plugin.get_dataio().getPlayerMap().keySet().toArray(keys);

                           for(int index = _plugin.get_dataio().getPlayerMap().size() - 1; index >= 0; --index) {
                              PlayerData playerData = Players.getData(keys[index]);
                              if (playerData.uuid == null || uuidFunc == Import.UUIDFUNC.REPLACE) {
                                 if (uuidMap.containsKey(playerData.playerName)) {
                                    playerData.uuid = (UUID)uuidMap.get(playerData.playerName);
                                    if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                                       _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "uuid", playerData.uuid.toString(), playerData.playerName);
                                    }

                                    LogFile.write(0, "{updateUUID} Found UUID for " + playerData.playerName + "(" + playerData.uuid.toString() + ")");
                                    ++updateCount;
                                    if (playerData.firstLogin == 0L && (playerData.firstLogin = _plugin.getServer().getOfflinePlayer(playerData.uuid).getFirstPlayed()) != 0L && OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                                       _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "firstlogin", playerData.firstLogin, playerData.playerName);
                                    }
                                 } else {
                                    ++missingCount;
                                    LogFile.write(2, "{updateUUID} UUID not found for " + playerData.playerName);
                                    if (uuidFunc == Import.UUIDFUNC.PURGE) {
                                       boolean removePlayer = true;
                                       if (playerData.playerName.equalsIgnoreCase("ontime-data")) {
                                          removePlayer = false;
                                       }

                                       if (playerData.referredBy != null && (playerData.referredBy.equalsIgnoreCase("votifier-service") || playerData.referredBy.equalsIgnoreCase("ontime-report"))) {
                                          removePlayer = false;
                                       }

                                       if (removePlayer) {
                                          _plugin.get_dataio().removePlayerCompletely(DataIO.REMOVEKEY.NAME_UUID, playerData);
                                          if (sender != null) {
                                             sender.sendMessage(playerData.playerName + " purged from OnTime Record due to lack of UUID. ");
                                          } else {
                                             LogFile.console(3, "{updateUUID}" + playerData.playerName + " purged from OnTime Record due to lack of UUID. ");
                                          }

                                          LogFile.write(2, playerData.playerName + " purged from OnTime Record due to lack of UUID. ");
                                          ++purgeCount;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                        break label223;
                     } catch (Exception var19) {
                        if (var19.getMessage().contains("429")) {
                           if (this.uuidRetry < 5) {
                              ++this.uuidRetry;
                              sender.sendMessage("UUID Import: Mojang's 600 UUID / 10 minute limit exceeded. OnTime will try again automatically in 10 minutes. ");
                              LogFile.write(10, "UUID Update failed to complete.  OnTime will retry in 10 min. (Attempt " + this.uuidRetry + " of 5)  Error Msg: " + var19.getMessage());
                              _plugin.get_import().scheduleUpdateUUIDdelayed(sender, uuidFunc, (String)null, (String)null);
                           } else {
                              sender.sendMessage("UUID Import: Mojang's 600 UUID / 10 minute limit exceeded. Max retries attempted. ");
                              LogFile.write(10, "UUID Update failed to complete.  (Max retries attempted)  Error Msg: " + var19.getMessage());
                              this.uuidRetry = 0;
                           }
                        } else {
                           LogFile.console(3, "Exception while running UUIDFetcher");
                           LogFile.write(10, "UUID fetch failed with exception. Error Msg: " + var19.getMessage());
                           var19.printStackTrace();
                        }

                        OnTime.get_commands().resumeOnTime();
                        return;
                     }
                  }

                  key = (String)uuidMap.next();
                  playerData = Players.getData(key);
               } while(playerData.uuid != null && uuidFunc != Import.UUIDFUNC.REPLACE);

               if (playerData.playerName != null) {
                  names[nameCount++] = playerData.playerName;
               } else {
                  LogFile.write(0, "{updateUUID} Found a null playerName at " + nameCount);
               }
            }
         }

         switch (uuidFunc) {
            case CLEAN:
               if (sender != null) {
                  sender.sendMessage(dupDeletedCount + " duplicate player records deleted");
               } else {
                  LogFile.console(3, dupDeletedCount + " duplicate player records deleted");
               }

               LogFile.write(3, dupDeletedCount + " duplicate player records deleted");
               break;
            case MERGE:
               if (sender != null) {
                  sender.sendMessage(mergedCount + " duplicate player records merged.");
               } else {
                  LogFile.console(3, mergedCount + " duplicate player records merged.");
               }

               LogFile.write(3, mergedCount + " duplicate player records merged.");
               break;
            case FIND:
            case REPLACE:
               if (updateCount > 0 && OnTime.dataStorage == DataIO.datastorage.YML) {
                  _plugin.get_dataio().saveAllData(OnTime.onTimeDataFolder);
               }

               if (sender != null) {
                  sender.sendMessage("Missing UUIDs updated for " + updateCount + " players.");
               } else {
                  LogFile.console(3, "Missing UUIDs updated for " + updateCount + " players.");
               }

               LogFile.write(3, "Missing UUIDs updated for " + updateCount + " players.");
               if (sender != null) {
                  sender.sendMessage("Could not find UUIDs for " + missingCount + " players.");
               } else {
                  LogFile.console(3, "Could not find UUIDs for " + missingCount + " players.");
               }

               LogFile.write(3, "Could not find UUIDs for " + missingCount + " players.");
               break;
            case PURGE:
               if (sender != null) {
                  sender.sendMessage(purgeCount + " players deleted due to lack of UUID");
               } else {
                  LogFile.console(3, purgeCount + " players deleted due to lack of UUID");
               }

               LogFile.write(3, purgeCount + " players deleted due to lack of UUID");
         }

         OnTime.get_commands().resumeOnTime();
      }
   }

   public boolean mergeOrCleanRecords(UUIDFUNC uuidFunc, PlayerData playerData1, PlayerData playerData2) {
      if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         ResultSet rs = null;
         if (playerData2 == null) {
            rs = _plugin.get_dataio().getCompletePlayerSet(playerData1.playerName, playerData1.uuid, (String)null, (UUID)null);
         } else {
            rs = _plugin.get_dataio().getCompletePlayerSet(playerData1.playerName, playerData1.uuid, playerData2.playerName, playerData2.uuid);
         }

         PlayerData first_PD = null;
         PlayerData second_PD = null;
         String firstName = null;
         String firstUuid = null;
         int firstId = 0;
         long firstPlaytime = 0L;
         long firstLogintime = 0L;
         if (rs != null) {
            try {
               boolean first = true;

               for(boolean more = rs.first(); more; more = rs.next()) {
                  if (rs.getString("world").equalsIgnoreCase(OnTime.serverID)) {
                     if (firstName == null) {
                        LogFile.write(0, "Processing duplicate for " + rs.getString("playername") + " UUID: " + rs.getString("uuid") + " ID:" + rs.getInt("id"));
                        firstName = rs.getString("playername");
                        firstUuid = rs.getString("uuid");
                        firstId = rs.getInt("id");
                        firstPlaytime = rs.getLong("playtime");
                        firstLogintime = rs.getLong("logintime");
                        first_PD = _plugin.get_dataio().getPlayerDataFromRS(rs);
                     } else {
                        if (first) {
                           LogFile.console(1, "*** Duplicates Found ***");
                           LogFile.write(3, "*** Duplicates Found ***");
                           LogFile.console(1, "playerName: " + firstName + " UUID: " + firstUuid + " ID:" + firstId);
                           LogFile.write(3, "playerName: " + firstName + " UUID: " + firstUuid + " ID:" + firstId);
                           first = false;
                        }

                        LogFile.console(1, "playerName: " + rs.getString("playername") + " UUID: " + rs.getString("uuid") + " ID:" + rs.getInt("id"));
                        LogFile.write(3, "playerName: " + rs.getString("playername") + " UUID: " + rs.getString("uuid") + " ID:" + rs.getInt("id"));
                        if (uuidFunc == Import.UUIDFUNC.CLEAN) {
                           if (firstUuid != null && !firstUuid.equalsIgnoreCase("null")) {
                              if (rs.getString("uuid") != null && !rs.getString("uuid").equalsIgnoreCase("null")) {
                                 if (firstUuid.equalsIgnoreCase(rs.getString("uuid")) && firstPlaytime == rs.getLong("playtime") && firstLogintime == rs.getLong("logintime")) {
                                    _plugin.get_dataio().removePlayerFromTableAsync(DataIO.REMOVEKEY.NAME_UUID_ID, Players.getData(firstName), OnTime.MySQL_table);
                                    LogFile.console(1, "Attempting removal of " + rs.getString("playername") + " UUID: " + rs.getString("uuid") + " ID=" + Players.getData(firstName).mysqlID);
                                    return true;
                                 }
                                 continue;
                              }

                              _plugin.get_dataio().removePlayerFromTableAsync(DataIO.REMOVEKEY.NAME_UUID, Players.getNew((UUID)null, rs.getString("playername"), 0L, 0L), OnTime.MySQL_table);
                              LogFile.console(1, "Attempting removal of " + rs.getString("playername") + " UUID: " + rs.getString("uuid"));
                              return true;
                           }

                           _plugin.get_dataio().removePlayerFromTableAsync(DataIO.REMOVEKEY.NAME_UUID, Players.getNew((UUID)null, firstName, 0L, 0L), OnTime.MySQL_table);
                           LogFile.console(1, "Attempting removal of " + firstName + " UUID: " + firstUuid);
                           return true;
                        } else if (firstUuid.equalsIgnoreCase(rs.getString("uuid")) || playerData2 != null) {
                           second_PD = _plugin.get_dataio().getPlayerDataFromRS(rs);
                           if (playerData2 != null) {
                              this.mergePlayerData(playerData1.playerName, first_PD, playerData2.playerName, second_PD);
                           } else {
                              this.mergePlayerData(playerData1.playerName, first_PD, (String)null, second_PD);
                           }

                           return true;
                        }
                     }
                  }
               }
            } catch (SQLException var16) {
               var16.printStackTrace();
            }
         }

         return false;
      } else if (playerData2 == null) {
         return false;
      } else {
         this.mergePlayerData(playerData1.playerName, playerData1, playerData2.playerName, playerData2);
         return true;
      }
   }

   public void mergePlayerData(String playerName1, PlayerData first_PD, String playerName2, PlayerData second_PD) {
      if (second_PD.firstLogin < first_PD.firstLogin && second_PD.firstLogin > 0L) {
         first_PD.firstLogin = second_PD.firstLogin;
      }

      if (first_PD.hostName == null) {
         first_PD.hostName = second_PD.hostName;
      }

      if (first_PD.lastWorld == null) {
         first_PD.lastWorld = second_PD.lastWorld;
      }

      if (second_PD.lastVoteDate > first_PD.lastVoteDate) {
         first_PD.lastVoteDate = second_PD.lastVoteDate;
      }

      first_PD.dailyVotes += second_PD.dailyVotes;
      first_PD.weeklyVotes += second_PD.weeklyVotes;
      first_PD.monthlyVotes += second_PD.monthlyVotes;
      first_PD.totalVotes += second_PD.totalVotes;
      first_PD.dailyReferrals += second_PD.dailyReferrals;
      first_PD.weeklyReferrals += second_PD.weeklyReferrals;
      first_PD.monthlyReferrals += second_PD.monthlyReferrals;
      first_PD.totalReferrals += second_PD.totalReferrals;
      if (first_PD.referredBy == null) {
         first_PD.referredBy = second_PD.referredBy;
      }

      first_PD.daysOn += second_PD.daysOn;
      if (first_PD.permissions == 0) {
         first_PD.permissions = second_PD.permissions;
      }

      first_PD.points += second_PD.points;
      AwayFKData var10000 = first_PD.afkData;
      var10000.totalAFKTime += second_PD.afkData.totalAFKTime;
      var10000 = first_PD.afkData;
      var10000.todayAFKTime += second_PD.afkData.todayAFKTime;
      var10000 = first_PD.afkData;
      var10000.weekAFKTime += second_PD.afkData.weekAFKTime;
      var10000 = first_PD.afkData;
      var10000.monthAFKTime += second_PD.afkData.monthAFKTime;
      PlayTimeData first_WD = Players.getWorldTime(first_PD, OnTime.serverID);
      PlayTimeData second_WD = Players.getWorldTime(second_PD, OnTime.serverID);
      first_WD.totalTime += second_WD.totalTime;
      first_WD.todayTime += second_WD.todayTime;
      first_WD.weekTime += second_WD.weekTime;
      first_WD.monthTime += second_WD.monthTime;
      if (second_WD.lastLogin > first_WD.lastLogin) {
         first_WD.lastLogin = second_WD.lastLogin;
      }

      if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         _plugin.get_dataio().removePlayerFromTable(DataIO.REMOVEKEY.UUID, first_PD, OnTime.MySQL_table);
      }

      if (first_PD.uuid != null) {
         Players.remove(first_PD.uuid);
      } else {
         Players.remove(first_PD.playerName);
      }

      Players.removeUuidMap(first_PD.playerName);
      if (playerName2 != null) {
         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            _plugin.get_dataio().removePlayerFromTable(DataIO.REMOVEKEY.UUID, second_PD, OnTime.MySQL_table);
         }

         if (second_PD.uuid != null) {
            Players.remove(second_PD.uuid);
         } else {
            Players.remove(second_PD.playerName);
         }

         Players.removeUuidMap(second_PD.playerName);
         if (second_PD.playerName.equalsIgnoreCase(playerName2)) {
            first_PD.playerName = second_PD.playerName;
            first_PD.uuid = second_PD.uuid;
         }
      } else if (second_PD.playerName.equalsIgnoreCase(playerName1)) {
         first_PD.playerName = second_PD.playerName;
         first_PD.uuid = second_PD.uuid;
      }

      if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         _plugin.get_dataio().savePlayerDataMySQL(first_PD, false);
      }

      Players.putData(first_PD.uuid, first_PD);
      Players.addUuidMap(first_PD.playerName, first_PD.uuid);
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Import$UUIDFUNC() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Import$UUIDFUNC;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[Import.UUIDFUNC.values().length];

         try {
            var0[Import.UUIDFUNC.CLEAN.ordinal()] = 1;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[Import.UUIDFUNC.FIND.ordinal()] = 3;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[Import.UUIDFUNC.MERGE.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[Import.UUIDFUNC.PURGE.ordinal()] = 4;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[Import.UUIDFUNC.REPLACE.ordinal()] = 5;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Import$UUIDFUNC = var0;
         return var0;
      }
   }

   public static enum UUIDFUNC {
      CLEAN,
      MERGE,
      FIND,
      PURGE,
      REPLACE;
   }

   public class updateUUIDevent implements Runnable {
      CommandSender sender = null;
      UUIDFUNC uuidFunc = null;
      String playerName = null;
      String playerName2 = null;

      public updateUUIDevent(CommandSender _sender, UUIDFUNC _uuidFunc, String _playerName, String _playerName2) {
         this.sender = _sender;
         this.uuidFunc = _uuidFunc;
         this.playerName = _playerName;
         this.playerName2 = _playerName2;
      }

      public void run() {
         Import.this.updateUUID(this.sender, this.uuidFunc, this.playerName, this.playerName2);
      }
   }
}
