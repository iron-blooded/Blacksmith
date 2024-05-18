package org.ontime.ontime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.Rewards.RewardData;
import org.bukkit.entity.Player;

public class TodayTime {
   private static OnTime _plugin;
   public HashMap dayMap = new HashMap();
   public HashMap weekMap = new HashMap();
   public HashMap monthMap = new HashMap();

   public TodayTime(OnTime plugin) {
      _plugin = plugin;
   }

   public HashMap getDayMap() {
      return this.dayMap;
   }

   public void setDayMap(HashMap map) {
      this.dayMap = map;
   }

   public HashMap getWeekMap() {
      return this.weekMap;
   }

   public void setWeekMap(HashMap map) {
      this.weekMap = map;
   }

   public HashMap getMonthMap() {
      return this.monthMap;
   }

   public void setMonthMap(HashMap map) {
      this.monthMap = map;
   }

   public void buildTodaytimeMap(String worldName) {
      this.getDayMap().clear();
      Iterator var3 = _plugin.get_dataio().getPlayerMap().keySet().iterator();

      while(var3.hasNext()) {
         String key = (String)var3.next();
         PlayerData playerData = Players.getData(key);
         PlayTimeData worldData = Players.getWorldTime(playerData, worldName);
         if (worldData != null) {
            long current = _plugin.get_logintime().current(playerData, worldName);
            if (worldData.todayTime + current > 0L) {
               this.getDayMap().put(key, worldData.todayTime + current);
            }
         }
      }

   }

   public void buildWeektimeMap(String worldName) {
      this.getWeekMap().clear();
      Iterator var3 = _plugin.get_dataio().getPlayerMap().keySet().iterator();

      while(var3.hasNext()) {
         String key = (String)var3.next();
         PlayerData playerData = Players.getData(key);
         PlayTimeData worldData = Players.getWorldTime(playerData, worldName);
         if (worldData != null) {
            long current = _plugin.get_logintime().current(playerData, worldName);
            if (worldData.weekTime + current > 0L) {
               this.getWeekMap().put(key, worldData.weekTime + current);
            }
         }
      }

   }

   public void buildMonthtimeMap(String worldName) {
      this.getMonthMap().clear();
      Iterator var3 = _plugin.get_dataio().getPlayerMap().keySet().iterator();

      while(var3.hasNext()) {
         String key = (String)var3.next();
         PlayerData playerData = Players.getData(key);
         PlayTimeData worldData = Players.getWorldTime(playerData, worldName);
         if (worldData != null) {
            long current = _plugin.get_logintime().current(playerData, worldName);
            if (worldData.monthTime + current > 0L) {
               this.getMonthMap().put(key, worldData.monthTime + current);
            }
         }
      }

   }

   public static void checkNewDay() {
      long today = todayMidnight();
      String todayStr = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(today);
      if (OnTime.todayStart != today) {
         String oldday = (new SimpleDateFormat("[MM/dd/yyyy] ")).format(OnTime.todayStart);
         String newday = (new SimpleDateFormat("[MM/dd/yyyy] ")).format(today);
         LogFile.console(0, "{checkNewDay - New Day} Old:" + oldday + " New: " + newday);
         boolean newWeek = false;
         boolean newMonth = false;
         if (Calendar.getInstance().get(7) == OnTime.firstDayofWeek) {
            newWeek = true;
         }

         if (Calendar.getInstance().get(5) == OnTime.firstDayofMonth) {
            newMonth = true;
         }

         Iterator var9 = _plugin.getServer().getOnlinePlayers().iterator();

         Player player;
         PlayerData playerData;
         while(var9.hasNext()) {
            player = (Player)var9.next();
            if (_plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
               playerData = Players.getData(player.getUniqueId());
               if (playerData != null) {
                  PlayTimeData var10000 = Players.getWorldTime(playerData, OnTime.serverID);
                  var10000.rollOver += _plugin.get_logintime().current(playerData, OnTime.serverID);
                  if (OnTime.perWorldEnable) {
                     var10000 = Players.getWorldTime(playerData, player.getWorld().getName());
                     var10000.rollOver += _plugin.get_logintime().current(playerData, player.getWorld().getName());
                  }

                  _plugin.get_awayfk().update(playerData);
                  _plugin.get_dataio().refreshPlayerDataMySQL(playerData);
                  _plugin.get_playingtime().updateGlobal(playerData);
               } else {
                  LogFile.write(10, "{checkNewDay} Online player " + player.getName() + " did not have OnTime record. No recovery attempted.");
               }
            }
         }

         if (OnTime.autoBackupEnable && today != _plugin.get_databackup().getLastBackup()) {
            _plugin.get_databackup().backup(OnTime.onTimeDataFolder);
         }

         _plugin.get_ontimetest().logoutAll();
         if (OnTime.collectPlayDetailEnable) {
            _plugin.get_todaytime().buildTodaytimeMap(OnTime.serverID);
            if (OnTime.autoReportEnable && OnTime.dailyPlayReportEnable) {
               if (!OnTime.reportFormat.equalsIgnoreCase("MYSQL")) {
                  LogFile.console(1, "[OnTime] Generated Daily Report for " + todayStr);
                  Report.generate(_plugin.get_todaytime().getDayMap(), OnTime.onTimeReportsFolder, "DailyReport", Report.ReportType.TODAYTIME);
               }

               if (Report.purgeReports(OnTime.onTimeReportsFolder, Report.ReportType.TODAYTIME)) {
                  LogFile.console(1, "Successfully purged old daily reports.");
               }
            }

            _plugin.get_rewards().setTopPlayReward(RewardData.timeScope.DAILY);
            if (OnTime.totalTopPlayReward.equalsIgnoreCase("daily")) {
               _plugin.get_rewards().setTopPlayReward(RewardData.timeScope.TOTAL);
            }
         }

         if (OnTime.collectVoteDetailEnable) {
            _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.VOTES, RewardData.timeScope.DAILY);
            if (OnTime.totalTopVoteReward.equalsIgnoreCase("daily")) {
               _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.VOTES, RewardData.timeScope.TOTAL);
            }
         }

         if (OnTime.collectReferDetailEnable) {
            _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.REFER, RewardData.timeScope.DAILY);
            if (OnTime.totalTopReferReward.equalsIgnoreCase("daily")) {
               _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.REFER, RewardData.timeScope.TOTAL);
            }
         }

         if (OnTime.pointsEnable && OnTime.totalTopPointReward.equalsIgnoreCase("daily")) {
            _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.POINTS, RewardData.timeScope.TOTAL);
         }

         OnTime.todayStart = today;
         _plugin.get_playingtime().purgeFile();

         try {
            _plugin.getServer().getScheduler().runTaskLaterAsynchronously(_plugin, new Runnable() {
               public void run() {
                  TodayTime._plugin.get_rewards().processAbsenceRewards();
               }
            }, 20L);
         } catch (NoSuchMethodError var14) {
            _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new Runnable() {
               public void run() {
                  TodayTime._plugin.get_rewards().processAbsenceRewards();
               }
            }, 20L);
         }

         String datetime;
         PlayerData playerdata;
         if (newWeek) {
            if (OnTime.collectPlayDetailEnable) {
               _plugin.get_todaytime().buildWeektimeMap(OnTime.serverID);
               if (OnTime.autoReportEnable && OnTime.weeklyPlayReportEnable) {
                  if (OnTime.weekStart != 0L) {
                     datetime = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(OnTime.weekStart);
                  } else {
                     datetime = "ERROR: No Weekly start date found. Using Today's Date";
                     OnTime.weekStart = today;
                  }

                  if (!OnTime.reportFormat.equalsIgnoreCase("MYSQL")) {
                     LogFile.write(1, "[OnTime] Generated Weekly Report for week starting " + datetime);
                     Report.generate(_plugin.get_todaytime().getWeekMap(), OnTime.onTimeReportsFolder, "WeeklyReport", Report.ReportType.WEEKLY);
                  } else if (!_plugin.get_dataio().createOnTimeReportTable("ontime-weekly-" + todayStr)) {
                     LogFile.console(3, "Failed to create weekly MySQL report table 'ontime-weekly-" + todayStr + "'");
                  } else {
                     Report.MySQLWeekly = "ontime-weekly-" + todayStr;
                     playerdata = Players.getNew(Report.MySQLWeekly, today, today + TimeUnit.DAYS.toMillis(7L), "ontime-report");
                     Players.putData(Report.MySQLWeekly, playerdata);
                  }

                  if (Report.purgeReports(OnTime.onTimeReportsFolder, Report.ReportType.WEEKLY)) {
                     LogFile.console(1, "Successfully purged old weekly reports.");
                  }
               } else {
                  LogFile.write(1, "Weekly report not generated. 'weeklyPlayReportEnable' set to '" + OnTime.weeklyPlayReportEnable + "'");
               }

               _plugin.get_rewards().setTopPlayReward(RewardData.timeScope.WEEKLY);
               if (OnTime.totalTopPlayReward.equalsIgnoreCase("weekly")) {
                  _plugin.get_rewards().setTopPlayReward(RewardData.timeScope.TOTAL);
               }
            }

            if (OnTime.collectVoteDetailEnable) {
               _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.VOTES, RewardData.timeScope.WEEKLY);
               if (OnTime.totalTopVoteReward.equalsIgnoreCase("weekly")) {
                  _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.VOTES, RewardData.timeScope.TOTAL);
               }
            }

            if (OnTime.collectReferDetailEnable) {
               _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.REFER, RewardData.timeScope.WEEKLY);
               if (OnTime.totalTopReferReward.equalsIgnoreCase("weekly")) {
                  _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.REFER, RewardData.timeScope.TOTAL);
               }
            }

            if (OnTime.pointsEnable && OnTime.totalTopPointReward.equalsIgnoreCase("weekly")) {
               _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.POINTS, RewardData.timeScope.TOTAL);
            }

            OnTime.weekStart = today;
         } else {
            LogFile.write(1, "Weekly updates/rewards/reports not handled. First day of week set to '" + OnTime.firstDayofWeek + "'");
         }

         if (newMonth) {
            if (OnTime.collectPlayDetailEnable) {
               _plugin.get_todaytime().buildMonthtimeMap(OnTime.serverID);
               if (OnTime.autoReportEnable && OnTime.monthlyPlayReportEnable) {
                  if (OnTime.monthStart != 0L) {
                     datetime = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(OnTime.monthStart);
                  } else {
                     datetime = "ERROR: No Month start date found. Using Today's Date";
                     OnTime.monthStart = today;
                  }

                  if (!OnTime.reportFormat.equalsIgnoreCase("MYSQL")) {
                     LogFile.console(1, "[OnTime] Generated Monthly Report for month starting " + datetime);
                     Report.generate(_plugin.get_todaytime().getMonthMap(), OnTime.onTimeReportsFolder, "MonthlyReport", Report.ReportType.MONTHLY);
                  } else if (!_plugin.get_dataio().createOnTimeReportTable("ontime-monthly-" + todayStr)) {
                     LogFile.console(3, "Failed to create monthly MySQL report table 'ontime-monthly-" + todayStr + "'");
                  } else {
                     Report.MySQLMonthly = "ontime-monthly-" + todayStr;
                     playerdata = Players.getNew(Report.MySQLMonthly, today, today + TimeUnit.DAYS.toMillis(31L), "ontime-report");
                     Players.putData(Report.MySQLMonthly, playerdata);
                  }

                  if (Report.purgeReports(OnTime.onTimeReportsFolder, Report.ReportType.MONTHLY)) {
                     LogFile.console(1, "Successfully purged old monthly reports.");
                  }
               }

               _plugin.get_rewards().setTopPlayReward(RewardData.timeScope.MONTHLY);
               if (OnTime.totalTopPlayReward.equalsIgnoreCase("monthly")) {
                  _plugin.get_rewards().setTopPlayReward(RewardData.timeScope.TOTAL);
               }
            }

            if (OnTime.collectVoteDetailEnable) {
               _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.VOTES, RewardData.timeScope.MONTHLY);
               if (OnTime.totalTopVoteReward.equalsIgnoreCase("monthly")) {
                  _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.VOTES, RewardData.timeScope.TOTAL);
               }
            }

            if (OnTime.collectReferDetailEnable) {
               _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.REFER, RewardData.timeScope.MONTHLY);
               if (OnTime.totalTopReferReward.equalsIgnoreCase("monthly")) {
                  _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.REFER, RewardData.timeScope.TOTAL);
               }
            }

            if (OnTime.pointsEnable && OnTime.totalTopPointReward.equalsIgnoreCase("monthly")) {
               _plugin.get_rewards().setTopMiscRewards(RewardData.EventReference.POINTS, RewardData.timeScope.TOTAL);
            }

            OnTime.monthStart = today;
         }

         var9 = _plugin.get_dataio().getPlayerMap().keySet().iterator();

         while(var9.hasNext()) {
            String playerName = (String)var9.next();
            playerData = Players.getData(playerName);
            Iterator var12 = playerData.worldTime.keySet().iterator();

            while(var12.hasNext()) {
               String worldName = (String)var12.next();
               PlayTimeData worldTime = Players.getWorldTime(playerData, worldName);
               worldTime.todayTime = 0L;
               if (newWeek) {
                  worldTime.weekTime = 0L;
               }

               if (newMonth) {
                  worldTime.monthTime = 0L;
               }
            }
         }

         if (OnTime.reportFormat.equalsIgnoreCase("MYSQL")) {
            _plugin.get_report().setReportNames();
         }

         if (OnTime.collectAfkEnable && (OnTime.afkReportPeriod.equalsIgnoreCase("Today") || OnTime.afkReportPeriod.equalsIgnoreCase("Week") && newWeek || OnTime.afkReportPeriod.equalsIgnoreCase("Month") && newMonth)) {
            LogFile.console(1, "Generated AFK Report for " + OnTime.afkReportPeriod);
            Report.generate((HashMap)null, OnTime.onTimeReportsFolder, "AFKReport", Report.ReportType.AFK);
         }

         var9 = _plugin.getServer().getOnlinePlayers().iterator();

         while(var9.hasNext()) {
            player = (Player)var9.next();
            _plugin.get_logintime().setLogin(Players.getData(player.getUniqueId()), 0L);
            playerData = null;
            if ((playerData = Players.getData(player.getUniqueId())) != null) {
               playerData.dailyReferrals = 0;
               playerData.dailyVotes = 0;
               if (newWeek) {
                  playerData.weeklyReferrals = 0;
                  playerData.weeklyVotes = 0;
               }

               if (newMonth) {
                  playerData.monthlyReferrals = 0;
                  playerData.monthlyVotes = 0;
               }
            }
         }

         _plugin.get_rewards().resetRewardTasks();
         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            if (OnTime.primaryServer) {
               LogFile.console(0, "Resetting daily/weekly/monthly data for online players in the local MySQL database.");
               _plugin.get_dataio().updateAllPlayerDataMySQL(true, newWeek, newMonth, OnTime.MySQL_table);
               _plugin.get_dataio().setOntimeDataMySQL();
            }

            if (OnTime.multiServer && OnTime.primaryServer) {
               LogFile.console(0, "Resetting daily/weekly/monthly data for online players in the local MySQL database.");
               _plugin.get_dataio().updateAllPlayerDataMySQL(true, newWeek, newMonth, OnTime.MySQL_multiServerTable);
               _plugin.get_dataio().setOntimeDataMySQL();
            }

            if (!_plugin.get_dataio().getPlayerMap().isEmpty()) {
               String[] keys = new String[_plugin.get_dataio().getPlayerMap().size()];
               _plugin.get_dataio().getPlayerMap().keySet().toArray(keys);

               for(int index = _plugin.get_dataio().getPlayerMap().size() - 1; index >= 0; --index) {
                  String key = keys[index];
                  PlayerData playerData = Players.getData(key);
                  LogFile.write(0, "{checkNewDay} Processing " + playerData.playerName + " to see if should be removed.");
                  if (playerData.uuid != null) {
                     if (!playerData.onLine) {
                        if (_plugin.getServer().getPlayer(playerData.uuid) == null) {
                           _plugin.get_dataio().removePlayerFromAllMaps(playerData.playerName);
                           LogFile.write(0, "{checkNewDay} Removing " + playerData.playerName + " from internal records.");
                        } else if (!_plugin.getServer().getPlayer(playerData.uuid).isOnline()) {
                           _plugin.get_dataio().removePlayerFromAllMaps(playerData.playerName);
                           LogFile.write(0, "{checkNewDay} Removing " + playerData.playerName + " from internal records.");
                        } else {
                           _plugin.get_playereventlistener().loginPlayer(_plugin.getServer().getPlayer(playerData.uuid), false);
                           LogFile.write(10, "{checkNewDay} Server 'online' record did not mactch OnTime record for  " + playerData.playerName + ". Player logged back in. ");
                        }
                     } else {
                        LogFile.write(0, "{checkNewDay} NOT REMOVING " + playerData.playerName + " from internal records.");
                     }
                  } else {
                     LogFile.write(10, "{checkNewDay} Missing UUID for " + playerData.playerName + ". Player not purged from internal records.");
                  }
               }

               _plugin.get_dataio().loadTopPlayerDataMySQL(DataIO.mysqlload.INIT, OnTime.topListMax * 2);
            }
         }

      }
   }

   public static long todayMidnight() {
      Calendar cal = Calendar.getInstance();
      cal.set(11, 0);
      cal.set(12, 0);
      cal.set(13, 0);
      cal.set(14, 0);
      return cal.getTimeInMillis();
   }

   public static enum CollectionPeriod {
      DAILY,
      WEEKLY,
      MONTHLY,
      TOTAL;
   }
}
