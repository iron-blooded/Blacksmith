package org.ontime.ontime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Updates {
   private static final String API_NAME_VALUE = "name";
   private static final String API_FILE_NAME_VALUE = "fileName";
   private static final String API_QUERY = "/servermods/files?projectIds=";
   private static final String API_HOST = "https://api.curseforge.com";
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Updates$SECTION;

   public static void updateSection(String section, BufferedWriter out) {
      me.edge209.OnTime.LogFile.console(0, "Updating section :" + section);
      File infile = new File(OnTime.onTimeDataFolder, "updates.txt");

      try {
         InputStream in = new FileInputStream(infile);
         BufferedReader inStream = new BufferedReader(new InputStreamReader(in));
         String line = inStream.readLine();

         while(true) {
            while(line != null) {
               if (line.contains("START_" + section)) {
                  line = inStream.readLine();
                  boolean more = true;

                  while(more) {
                     out.write(line);
                     out.newLine();
                     line = inStream.readLine();
                     if (line == null) {
                        more = false;
                     } else if (line.contains("END_" + section)) {
                        more = false;
                     }
                  }
               } else {
                  line = inStream.readLine();
               }
            }

            in.close();
            break;
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }

   }

   public static boolean saveConfig(OnTime plugin) {
      SECTION section = Updates.SECTION.SERVER;
      File outfile = new File(OnTime.onTimeDataFolder, "config.yml");
      createFile(outfile);

      try {
         BufferedWriter out = new BufferedWriter(new FileWriter(outfile, true));
         InputStream in = plugin.getResource("config.yml");
         BufferedReader inStream = new BufferedReader(new InputStreamReader(in));

         for(String line = inStream.readLine(); line != null; line = inStream.readLine()) {
            if (!line.startsWith("#") && line.length() != 0) {
               switch (section) {
                  case SERVER:
                     if (line.startsWith("configVersion:")) {
                        out.write("configVersion: 21");
                     } else if (line.startsWith("serverName:")) {
                        out.write("serverName: " + OnTime.serverName);
                     } else if (line.startsWith("multiServer:")) {
                        out.write("multiServer: " + OnTime.multiServer);
                     } else if (line.startsWith("multiServerName:")) {
                        if (OnTime.mulitServerNameERROR != null) {
                           out.write("multiServerName: " + OnTime.mulitServerNameERROR);
                        } else {
                           out.write("multiServerName: " + OnTime.multiServerName);
                        }
                     } else if (line.startsWith("primaryServer:")) {
                        out.write("primaryServer: " + OnTime.primaryServer);
                        section = Updates.SECTION.GENERAL;
                     }
                     break;
                  case GENERAL:
                     if (line.startsWith("startupDelay:")) {
                        out.write("startupDelay: " + OnTime.startupDelay);
                     } else if (line.startsWith("topListMax:")) {
                        out.write("topListMax: " + OnTime.topListMax);
                     } else if (line.startsWith("updateCheckEnable:")) {
                        out.write("updateCheckEnable: " + OnTime.updateCheckEnable);
                        section = Updates.SECTION.MESSAGES;
                     }
                     break;
                  case MESSAGES:
                     if (line.startsWith("messagesEnable:")) {
                        out.write("messagesEnable: " + OnTime.messagesEnable);
                     } else if (line.startsWith("welcomeEnable:")) {
                        out.write("welcomeEnable: " + OnTime.welcomeEnable);
                        section = Updates.SECTION.AFK;
                     }
                     break;
                  case AFK:
                     if (line.startsWith("afkCheckEnable:")) {
                        out.write("afkCheckEnable: " + OnTime.afkCheckEnable);
                     } else if (line.startsWith("afkTime:")) {
                        out.write("afkTime: " + OnTime.afkTime);
                        section = Updates.SECTION.DATA_STORAGE;
                     }
                     break;
                  case DATA_STORAGE:
                     if (line.startsWith("dataStorage")) {
                        out.write("dataStorage: " + OnTime.dataStorage);
                     } else if (line.startsWith("MySQL:")) {
                        out.write("MySQL: ");
                     } else if (line.startsWith("  enable:")) {
                        out.write("  enable: " + OnTime.MySQL_enable);
                     } else if (line.startsWith("  host:")) {
                        out.write("  host: " + OnTime.MySQL_host);
                     } else if (line.startsWith("  port:")) {
                        out.write("  port: " + OnTime.MySQL_port);
                     } else if (line.startsWith("  user:")) {
                        out.write("  user: " + OnTime.MySQL_user);
                     } else if (line.startsWith("  password:")) {
                        out.write("  password: " + OnTime.MySQL_password);
                     } else if (line.startsWith("  database:")) {
                        out.write("  database: " + OnTime.MySQL_database);
                     } else if (line.startsWith("  table:")) {
                        out.write("  table: " + OnTime.MySQL_table.substring(1, OnTime.MySQL_table.length() - 1));
                     } else if (line.startsWith("  multiServerTable:")) {
                        out.write("  multiServerTable: " + OnTime.MySQL_multiServerTable.substring(1, OnTime.MySQL_multiServerTable.length() - 1));
                     } else if (line.startsWith("autoSaveEnable:")) {
                        out.write("autoSaveEnable: " + OnTime.autoSaveEnable);
                     } else if (line.startsWith("autoSavePeriod:")) {
                        out.write("autoSavePeriod: " + OnTime.autoSavePeriod);
                     } else if (line.startsWith("autoBackupEnable:")) {
                        out.write("autoBackupEnable: " + OnTime.autoBackupEnable);
                     } else if (line.startsWith("autoBackupVersions:")) {
                        out.write("autoBackupVersions: " + OnTime.autoBackupVersions);
                     } else if (line.startsWith("uuidMergeEnable:")) {
                        out.write("uuidMergeEnable: " + OnTime.uuidMergeEnable);
                        section = Updates.SECTION.DATA_COLLECTION;
                     }
                     break;
                  case DATA_COLLECTION:
                     if (line.startsWith("perWorldEnable:")) {
                        out.write("perWorldEnable: " + OnTime.perWorldEnable);
                     } else if (line.startsWith("votifierStatsEnable:")) {
                        out.write("votifierStatsEnable: " + OnTime.votifierStatsEnable);
                     } else if (line.startsWith("collectPlayDetailEnable:")) {
                        out.write("collectPlayDetailEnable: " + OnTime.collectPlayDetailEnable);
                     } else if (line.startsWith("collectVoteDetailEnable:")) {
                        out.write("collectVoteDetailEnable: " + OnTime.collectVoteDetailEnable);
                     } else if (line.startsWith("collectReferDetailEnable:")) {
                        out.write("collectReferDetailEnable: " + OnTime.collectReferDetailEnable);
                     } else if (line.startsWith("collectAfkEnable:")) {
                        out.write("collectAfkEnable: " + OnTime.collectAfkEnable);
                        section = Updates.SECTION.DATA_PURGE;
                     }
                     break;
                  case DATA_PURGE:
                     if (line.startsWith("purgeEnable:")) {
                        out.write("purgeEnable: " + OnTime.purgeEnable);
                     } else if (line.startsWith("purgeTimeMin:")) {
                        out.write("purgeTimeMin: " + OnTime.purgeTimeMin);
                     } else if (line.startsWith("purgeLoginDay:")) {
                        out.write("purgeLoginDay: " + OnTime.purgeLoginDay);
                     } else if (line.startsWith("purgeDemotionEnable:")) {
                        out.write("purgeDemotionEnable: " + OnTime.purgeDemotionEnable);
                     } else if (line.startsWith("purgeDemotionGroup:")) {
                        out.write("purgeDemotionGroup: " + OnTime.purgeDemotionGroup);
                        section = Updates.SECTION.ONLINE_TRACKING;
                     }
                     break;
                  case ONLINE_TRACKING:
                     if (line.startsWith("onlineTrackingEnable:")) {
                        out.write("onlineTrackingEnable: " + OnTime.onlineTrackingEnable);
                     } else if (line.startsWith("onlineTrackingRefresh:")) {
                        out.write("onlineTrackingRefresh: " + OnTime.onlineTrackingRefresh);
                        section = Updates.SECTION.LOGGING;
                     }
                     break;
                  case LOGGING:
                     if (line.startsWith("logEnable:")) {
                        out.write("logEnable: " + OnTime.logEnable);
                     } else if (line.startsWith("logLevel:")) {
                        out.write("logLevel: " + OnTime.logLevel);
                     } else if (line.startsWith("consoleLogLevel")) {
                        out.write("consoleLogLevel: " + OnTime.consoleLogLevel);
                        section = Updates.SECTION.REPORTS;
                     }
                     break;
                  case REPORTS:
                     if (line.startsWith("firstDayofWeek")) {
                        out.write("firstDayofWeek: " + OnTime.firstDayofWeek);
                     } else if (line.startsWith("firstDayofMonth:")) {
                        out.write("firstDayofMonth: " + OnTime.firstDayofMonth);
                     } else if (line.startsWith("autoReportEnable:")) {
                        out.write("autoReportEnable: " + OnTime.autoReportEnable);
                     } else if (line.startsWith("dailyPlayReportEnable:")) {
                        out.write("dailyPlayReportEnable: " + OnTime.dailyPlayReportEnable);
                     } else if (line.startsWith("weeklyPlayReportEnable:")) {
                        out.write("weeklyPlayReportEnable: " + OnTime.weeklyPlayReportEnable);
                     } else if (line.startsWith("monthlyPlayReportEnable:")) {
                        out.write("monthlyPlayReportEnable: " + OnTime.monthlyPlayReportEnable);
                     } else if (line.startsWith("reportFolder:")) {
                        out.write("reportFolder: " + OnTime.reportFolder);
                     } else if (line.startsWith("dateFilenameFormat:")) {
                        out.write("dateFilenameFormat: " + OnTime.dateFilenameFormat);
                     } else if (line.startsWith("dateInFilenameEnable:")) {
                        out.write("dateInFilenameEnable: " + OnTime.dateInFilenameEnable);
                     } else if (line.startsWith("reportFormat:")) {
                        out.write("reportFormat: " + OnTime.reportFormat);
                     } else if (line.startsWith("afkReportPeriod:")) {
                        out.write("afkReportPeriod: " + OnTime.afkReportPeriod);
                     } else if (line.startsWith("dailyReportRetention:")) {
                        out.write("dailyReportRetention: " + OnTime.dailyReportRetention);
                     } else if (line.startsWith("weeklyReportRetention:")) {
                        out.write("weeklyReportRetention: " + OnTime.weeklyReportRetention);
                     } else if (line.startsWith("monthlyReportRetention:")) {
                        out.write("monthlyReportRetention: " + OnTime.monthlyReportRetention);
                     } else if (line.startsWith("afkReportRetention:")) {
                        out.write("afkReportRetention: " + OnTime.afkReportRetention);
                        section = Updates.SECTION.REFERRALS;
                     }
                     break;
                  case REFERRALS:
                     if (line.startsWith("referredByEnable:")) {
                        out.write("referredByEnable: " + OnTime.referredByEnable);
                     } else if (line.startsWith("referredByPermTrackEnable:")) {
                        out.write("referredByPermTrackEnable: " + OnTime.referredByPermTrackEnable);
                     } else if (line.startsWith("referredByMaxTime:")) {
                        out.write("referredByMaxTime: " + OnTime.referredByMaxTime);
                        section = Updates.SECTION.REWARDS_GENERAL;
                     }
                     break;
                  case REWARDS_GENERAL:
                     if (line.startsWith("rewardsEnable:")) {
                        out.write("rewardsEnable: " + OnTime.rewardsEnable);
                     } else if (line.startsWith("rewardNotifyEnable:")) {
                        out.write("rewardNotifyEnable: " + OnTime.rewardNotifyEnable);
                     } else if (line.startsWith("rewardBroadcastEnable:")) {
                        out.write("rewardBroadcastEnable: " + OnTime.rewardBroadcastEnable);
                        section = Updates.SECTION.REWARDS_TOP;
                     }
                     break;
                  case REWARDS_TOP:
                     if (line.startsWith("totalTopPlayReward:")) {
                        out.write("totalTopPlayReward: " + OnTime.totalTopPlayReward);
                     } else if (line.startsWith("totalTopVoteReward:")) {
                        out.write("totalTopVoteReward: " + OnTime.totalTopVoteReward);
                     } else if (line.startsWith("totalTopReferReward")) {
                        out.write("totalTopReferReward: " + OnTime.totalTopReferReward);
                     } else if (line.startsWith("totalTopPointReward:")) {
                        out.write("totalTopPointReward: " + OnTime.totalTopPointReward);
                        section = Updates.SECTION.POINTS;
                     }
                     break;
                  case POINTS:
                     if (line.startsWith("pointsEnable:")) {
                        out.write("pointsEnable: " + OnTime.pointsEnable);
                     } else if (line.startsWith("negativePointsEnable:")) {
                        out.write("negativePointsEnable: " + OnTime.negativePointsEnable);
                        section = Updates.SECTION.THE_END;
                     }
               }
            } else {
               out.write(line);
            }

            out.newLine();
         }

         out.close();
         in.close();
         return true;
      } catch (Exception var7) {
         var7.printStackTrace();
         return false;
      }
   }

   public static boolean checkConfigUpgrade(OnTime plugin, FileConfiguration config) {
      int version = config.getInt("configVersion");
      if (version == 21) {
         return false;
      } else if (version > 21) {
         LogFile.console(3, "Warning. You are using a config.yml version (" + version + ") that may not be compatible with this version of OnTime ");
         return false;
      } else {
         LogFile.console(1, "config.yml upgrade initiating.");
         File infile = new File(OnTime.onTimeDataFolder, "config.yml");
         File backup = createBackupFile("config", "yml");
         LogFile.console(0, "Backup File name: " + backup.getPath());
         if (!infile.renameTo(backup)) {
            LogFile.console(3, "config.yml auto backup error.  Backup file not created.");
         }

         if (version < 19) {
            if (OnTime.autoSavePeriod / 1200L > 1L) {
               OnTime.autoSavePeriod /= 1200L;
            }

            OnTime.MySQL_multiServerTable = "`ontime-multiServer`";
         }

         return saveConfig(plugin);
      }
   }

   public static boolean checkOutputUpgrade(FileConfiguration outputconfig) {
      int version = outputconfig.getInt("outputVersion");
      if (version == 20) {
         return false;
      } else if (version < 17) {
         LogFile.console(3, "Your OnTime/Output.yml file cannot be upgraded to latest version. Please upgrade to OnTime v3.13.3 before attempting to upgrade to any OnTime version 4.0.0 or greater.");
         return false;
      } else {
         File outfile = new File(OnTime.onTimeDataFolder, "output-temp.yml");
         File infile = new File(OnTime.onTimeDataFolder, "output.yml");
         createFile(outfile);

         try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outfile, true));
            InputStream in = new FileInputStream(infile);
            BufferedReader inStream = new BufferedReader(new InputStreamReader(in));

            for(String line = inStream.readLine(); line != null; line = inStream.readLine()) {
               if (line.contains("outputVersion:")) {
                  out.write("outputVersion: 20");
               } else if (line.startsWith("#")) {
                  if (version < 18) {
                     if (line.contains("endOntime-other")) {
                        out.write(line);
                        out.newLine();
                        updateSection("OUTPUT_V18_A", out);
                     } else if (line.contains("[weekStartDate]")) {
                        out.write(line);
                        out.newLine();
                        out.write("# [world] - Name of world (used for per-world tracking)");
                     } else {
                        out.write(line);
                     }
                  }

                  if (version < 20) {
                     if (line.contains("[daily]")) {
                        out.write(line);
                        out.newLine();
                        out.write("# [dailyRefer] - Player's count of referrals made for current day");
                        out.newLine();
                        out.write("# [dailyVote] - Player's count of votes cast for current day");
                     } else if (line.contains("[monthly]")) {
                        out.write(line);
                        out.newLine();
                        out.write("# [monthlyRefer] - Player's count of referrals made for current month");
                        out.newLine();
                        out.write("# [monthlyVote] - Player's count of votes cast for current month");
                     } else if (line.contains("[weekly]")) {
                        out.write(line);
                        out.newLine();
                        out.write("# [weeklyRefer] - Player's count of referrals made for current week");
                        out.newLine();
                        out.write("# [weeklyVote] - Player's count of votes cast for current week");
                     } else {
                        out.write(line);
                     }
                  } else {
                     out.write(line);
                  }
               } else if (line.endsWith("reward:")) {
                  out.write(line);
                  out.newLine();
                  line = inStream.readLine();
                  if (version < 19) {
                     updateSection("OUTPUT_V19_A", out);
                  }

                  out.write(line);
               } else {
                  int i;
                  if (line.endsWith("ontime-me:")) {
                     out.write(line);
                     out.newLine();
                     line = inStream.readLine();
                     if (version >= -1) {
                        out.write(line);
                     } else {
                        out.write("     lines: " + (outputconfig.getInt("output.ontime-me.lines") + 1));
                        out.newLine();

                        for(i = 1; i <= outputconfig.getInt("output.ontime-me.lines"); ++i) {
                           line = inStream.readLine();
                           out.write(line);
                           out.newLine();
                        }

                        out.write("     line-" + i++ + ": 'new line text goes here'");
                        out.newLine();
                     }
                  } else if (!line.endsWith("ontime-other:")) {
                     if (line.contains("noData:")) {
                        if (version < 18) {
                           out.write(line);
                           out.newLine();
                           out.write("        dateNotAvailable: 'Date Not Available'");
                           out.newLine();
                        } else {
                           out.write(line);
                        }
                     } else if (line.contains("the full old line text")) {
                        out.write("the full new line text'");
                     } else {
                        out.write(line);
                     }
                  } else {
                     out.write(line);
                     out.newLine();
                     line = inStream.readLine();
                     if (version >= -1) {
                        out.write(line);
                     } else {
                        out.write("     lines: " + (outputconfig.getInt("output.ontime-other.lines") + 1));
                        out.newLine();

                        for(i = 1; i <= outputconfig.getInt("output.ontime-other.lines"); ++i) {
                           line = inStream.readLine();
                           out.write(line);
                           out.newLine();
                        }

                        out.write("     line-" + i++ + ": 'new line text goes here'");
                        out.newLine();
                     }
                  }
               }

               out.newLine();
            }

            out.close();
            in.close();
            File backup = createBackupFile("output", "yml");
            LogFile.console(0, "Backup File name: " + backup.getPath());
            if (!infile.renameTo(backup)) {
               if (!infile.delete()) {
                  LogFile.console(3, "output.yml auto upgrade error.  Update Failed.");
                  return false;
               }

               LogFile.console(3, "output.yml auto upgrade error.  Backup file not created.");
            }

            if (!outfile.renameTo(infile)) {
               LogFile.console(3, "output.yml auto updgrade failure.  File rename failure. Update Failed.");
               return false;
            } else {
               return true;
            }
         } catch (Exception var9) {
            var9.printStackTrace();
            return false;
         }
      }
   }

   public static File createBackupFile(String fileName, String extension) {
      String datetime = (new SimpleDateFormat("-MM-dd-yy")).format(Calendar.getInstance().getTime());
      DataBackup.createBackupFolder(OnTime.onTimeDataFolder);
      File backupFolder = new File(OnTime.onTimeDataFolder, "backup");
      return new File(backupFolder, fileName + datetime + "." + extension);
   }

   public static void createFile(File file) {
      if (file.exists()) {
         file.delete();
      }

      try {
         file.createNewFile();
      } catch (IOException var2) {
         var2.printStackTrace();
      }

   }

   public static void UpdateVersion(int projectID, String thisVersion) {
      query(projectID, thisVersion);
   }

   public static void query(int projectID, String thisVersion) {
      URL url = null;
      LogFile.console(0, "Checking for new OnTime Version. Project ID =" + projectID);

      try {
         url = new URL("https://api.curseforge.com/servermods/files?projectIds=" + projectID);
      } catch (MalformedURLException var11) {
         var11.printStackTrace();
         return;
      }

      try {
         URLConnection conn = url.openConnection();
         conn.addRequestProperty("User-Agent", "OnTime Plugin");
         BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
         String response = reader.readLine();
         JSONArray array = (JSONArray)JSONValue.parse(response);
         if (array.size() > 0) {
            JSONObject latest = (JSONObject)array.get(array.size() - 1);
            String versionName = (String)latest.get("name");
            String versionFileName = (String)latest.get("fileName");
            if (!versionName.equalsIgnoreCase(thisVersion)) {
               LogFile.console(3, "*****************************************************");
               LogFile.console(3, "* YOU DO NOT HAVE THE LATEST VERSION OF " + versionFileName + "! *");
               LogFile.console(3, "*         CONSIDER DOWNLOADING '" + versionName + "'      *");
               LogFile.console(3, "*****************************************************");
               LogFile.console(1, "Server is currently running: '" + thisVersion + "'");
            } else {
               LogFile.console(1, "Running Version: " + versionName);
            }
         } else {
            LogFile.console(0, "No OnTime Versions found.");
            System.out.println("There are no files for this project");
         }

      } catch (IOException var10) {
         LogFile.console(3, "Unable to check for new version of OnTime.  Web service not responding.");
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Updates$SECTION() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Updates$SECTION;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[Updates.SECTION.values().length];

         try {
            var0[Updates.SECTION.AFK.ordinal()] = 4;
         } catch (NoSuchFieldError var15) {
         }

         try {
            var0[Updates.SECTION.DATA_COLLECTION.ordinal()] = 6;
         } catch (NoSuchFieldError var14) {
         }

         try {
            var0[Updates.SECTION.DATA_PURGE.ordinal()] = 7;
         } catch (NoSuchFieldError var13) {
         }

         try {
            var0[Updates.SECTION.DATA_STORAGE.ordinal()] = 5;
         } catch (NoSuchFieldError var12) {
         }

         try {
            var0[Updates.SECTION.GENERAL.ordinal()] = 2;
         } catch (NoSuchFieldError var11) {
         }

         try {
            var0[Updates.SECTION.LOGGING.ordinal()] = 9;
         } catch (NoSuchFieldError var10) {
         }

         try {
            var0[Updates.SECTION.MESSAGES.ordinal()] = 3;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[Updates.SECTION.ONLINE_TRACKING.ordinal()] = 8;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[Updates.SECTION.POINTS.ordinal()] = 14;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[Updates.SECTION.REFERRALS.ordinal()] = 11;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[Updates.SECTION.REPORTS.ordinal()] = 10;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[Updates.SECTION.REWARDS_GENERAL.ordinal()] = 12;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[Updates.SECTION.REWARDS_TOP.ordinal()] = 13;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[Updates.SECTION.SERVER.ordinal()] = 1;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[Updates.SECTION.THE_END.ordinal()] = 15;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Updates$SECTION = var0;
         return var0;
      }
   }

   static enum SECTION {
      SERVER,
      GENERAL,
      MESSAGES,
      AFK,
      DATA_STORAGE,
      DATA_COLLECTION,
      DATA_PURGE,
      ONLINE_TRACKING,
      LOGGING,
      REPORTS,
      REFERRALS,
      REWARDS_GENERAL,
      REWARDS_TOP,
      POINTS,
      THE_END;
   }
}
