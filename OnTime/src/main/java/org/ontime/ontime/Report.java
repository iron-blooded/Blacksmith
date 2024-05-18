package org.ontime.ontime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class Report {
   private static OnTime _plugin;
   static long serverTodayTime = 0L;
   static String MySQLDaily = null;
   static String MySQLWeekly = null;
   static String MySQLMonthly = null;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Report$ReportType;

   public Report(OnTime plugin) {
      _plugin = plugin;
   }

   public void setReportNames() {
      if (OnTime.reportFormat.equalsIgnoreCase("MYSQL")) {
         String reportDay = null;
         if (OnTime.dailyPlayReportEnable) {
            reportDay = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(TodayTime.todayMidnight());
            MySQLDaily = "ontime-daily-" + reportDay;
            LogFile.console(0, "MySQLDaily set to " + MySQLDaily);
         }

         int addDays;
         if (OnTime.weeklyPlayReportEnable) {
            addDays = OnTime.firstDayofWeek - Calendar.getInstance().get(7);
            if (addDays <= 0) {
               addDays += 6;
            }

            reportDay = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(TodayTime.todayMidnight() + TimeUnit.DAYS.toMillis((long)addDays));
            MySQLWeekly = "ontime-weekly-" + reportDay;
            LogFile.console(0, "MySQLWeekly set to " + MySQLWeekly);
         }

         if (OnTime.monthlyPlayReportEnable) {
            addDays = OnTime.firstDayofMonth - Calendar.getInstance().get(5);
            if (addDays <= 0) {
               addDays = addDays + Calendar.getInstance().getActualMaximum(5) - 1;
            }

            reportDay = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(TodayTime.todayMidnight() + TimeUnit.DAYS.toMillis((long)addDays));
            MySQLMonthly = "ontime-monthly-" + reportDay;
            LogFile.console(0, "MySQLMonthly set to " + MySQLMonthly);
         }

      }
   }

   public static void generate(HashMap map, File fileFolder, String fileNameBase, ReportType type) {
      String date = "OnTime";
      ResultSet rs = null;
      if (type != Report.ReportType.AFK || OnTime.collectAfkEnable) {
         StringBuilder sb = new StringBuilder();
         String fileName;
         if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
            fileName = fileNameBase + ".htm";
         } else {
            fileName = fileNameBase + ".txt";
         }

         switch (type) {
            case ONTIME:
            case AFK:
               date = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(Calendar.getInstance().getTime());
               break;
            case TODAYTIME:
               date = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(OnTime.todayStart);
               break;
            case WEEKLY:
               date = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(OnTime.weekStart);
               break;
            case MONTHLY:
               date = (new SimpleDateFormat(OnTime.dateFilenameFormat)).format(OnTime.monthStart);
         }

         if (OnTime.dateInFilenameEnable) {
            fileName = date + " " + fileName;
         }

         File file = new File(fileFolder + File.separator + fileName);
         serverTodayTime = 0L;
         createFile(file);
         if (OnTime.dataStorage != DataIO.datastorage.MYSQL && map.isEmpty()) {
            if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
               write(file, "<b>There was no data to report.</b>");
            } else {
               write(file, "There was no data to report.");
            }

         } else {
            if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
               sb.append("<b><H1 style = \"color:#2E2EFE\">");
            }

            sb.append(OnTime.serverName);
            if (type == Report.ReportType.TODAYTIME) {
               sb.append(" Top Gamers for today ");
            } else if (type == Report.ReportType.ONTIME) {
               sb.append(" Top Gamers!  as of ");
            } else if (type == Report.ReportType.WEEKLY) {
               sb.append(" Top Gamers for the Week Starting: ");
            } else if (type == Report.ReportType.MONTHLY) {
               sb.append(" Top Gamers for the Month Starting: ");
            } else if (type == Report.ReportType.AFK) {
               sb.append(" Top Players Found AFK (ordered by " + OnTime.afkReportPeriod + ") as of: ");
            }

            sb.append(date);
            if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
               sb.append("</H1></b>");
            }

            write(file, sb.toString());
            if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
               if (type == Report.ReportType.ONTIME) {
                  write(file, "<br><br><table  border=1><tr><th>Rank</th><th>IGN</th><th>Total Time</th><th>Last Login</th></tr>");
               } else if (type == Report.ReportType.TODAYTIME) {
                  write(file, "<br><br><table  border=1><tr><th>Rank</th><th>IGN</th><th>Today Time</th><th>Total Time</th></tr>");
               } else if (type == Report.ReportType.WEEKLY) {
                  write(file, "<br><br><table  border=1><tr><th>Rank</th><th>IGN</th><th>This Week</th><th>Total Time</th></tr>");
               } else if (type == Report.ReportType.MONTHLY) {
                  write(file, "<br><br><table  border=1><tr><th>Rank</th><th>IGN</th><th>This Month</th><th>Total Time</th></tr>");
               } else if (type == Report.ReportType.AFK) {
                  write(file, "<br><br><table  border=1><tr><th>Rank</th><th>IGN</th><th>Today</th><th>This Week</th><th>This Month</th></tr>");
               }
            }

            sb.delete(0, sb.length());
            String order;
            long data;
            if (OnTime.dataStorage != DataIO.datastorage.MYSQL) {
               ValueComparator bvc = new ValueComparator(map);
               TreeMap sorted_map = new TreeMap(bvc);
               sorted_map.putAll(map);
               order = (String)sorted_map.firstKey();
               int count = 0;

               for(int i = 0; i < sorted_map.size(); ++i) {
                  if (order == null || sorted_map.get(order) == null) {
                     LogFile.console(3, "{report.generate} ERROR: Null key2. " + i + " of " + sorted_map.size());
                     LogFile.write(3, "{report.generate} ERROR: Null key2. " + i + " of " + sorted_map.size());
                     write(file, "{report.generate} ERROR: Null key2. " + i + " of " + sorted_map.size());
                     return;
                  }

                  data = -1L;
                  PlayerData playerData = Players.getData(order);
                  if (playerData != null) {
                     if (type == Report.ReportType.ONTIME) {
                        data = _plugin.get_logintime().lastLogin(playerData, OnTime.serverID);
                     } else {
                        data = Players.getWorldTime(playerData, OnTime.serverID).totalTime;
                     }

                     if (data >= 0L && (Long)sorted_map.get(order) != 0L) {
                        writeReportLine(type, file, playerData.playerName, count, (Long)sorted_map.get(order), data, rs);
                        ++count;
                     }
                  }

                  order = (String)sorted_map.higherKey(order);
               }
            } else {
               int count = 0;
               String section = "";
               order = "";
               if (type == Report.ReportType.WEEKLY) {
                  section = "playtime, weektime";
                  order = "weektime";
               } else if (type == Report.ReportType.MONTHLY) {
                  section = "playtime, monthtime";
                  order = "monthtime";
               } else if (type == Report.ReportType.TODAYTIME) {
                  section = "playtime, todaytime";
                  order = "todaytime";
               } else if (type == Report.ReportType.ONTIME) {
                  section = "playtime, logintime";
                  order = "playtime";
               } else if (type == Report.ReportType.AFK) {
                  section = "afkToday, afkWeek, afkMonth";
                  order = "afk" + OnTime.afkReportPeriod;
               }

               DataIO.mysqlNew.checkMySQLConnection();
               boolean more = false;

               try {
                  rs = DataIO.mysqlNew.query("SELECT playerName, " + section + " FROM " + OnTime.MySQL_table + " ORDER BY " + order + " DESC");
                  rs.first();

                  for(more = rs.first(); more; more = rs.next()) {
                     boolean skip = false;
                     if (rs.getString("playerName").equalsIgnoreCase("ontime-data")) {
                        skip = true;
                     }

                     if (type == Report.ReportType.AFK && rs.getLong("afkMonth") <= 0L) {
                        skip = true;
                     }

                     if (!skip) {
                        data = 0L;
                        if (type == Report.ReportType.ONTIME) {
                           data = rs.getLong("logintime");
                        } else if (type != Report.ReportType.AFK) {
                           data = rs.getLong("playtime");
                        }

                        if (rs.getLong(order) > 0L) {
                           writeReportLine(type, file, rs.getString("playerName"), count, rs.getLong(order), data, rs);
                           ++count;
                        }
                     }
                  }
               } catch (SQLException var17) {
                  var17.printStackTrace();
                  more = false;
               }

               if (count == 0) {
                  write(file, " ");
                  write(file, " No players found with data for this report.");
               }
            }

            if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
               write(file, "</table>");
            }

            if (type == Report.ReportType.TODAYTIME || type == Report.ReportType.WEEKLY || type == Report.ReportType.MONTHLY) {
               if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
                  write(file, "<br \\>");
                  write(file, "<b> Total Server time for all players was:" + Output.getTimeBreakdown(serverTodayTime, Output.TIMEDETAIL.LONG) + "</b>");
               } else {
                  write(file, " ");
                  write(file, "Total Server time for all players was:" + Output.getTimeBreakdown(serverTodayTime, Output.TIMEDETAIL.LONG));
               }
            }

         }
      }
   }

   public static void writeReportLine(ReportType type, File file, String playerName, int i, long time, long data, ResultSet rs) {
      StringBuilder sb = new StringBuilder();
      String datetime = "N/A";
      if (playerName.length() > 16) {
         playerName = playerName.substring(0, 16);
      }

      if (type == Report.ReportType.ONTIME) {
         if (Players.getOfflinePlayer(playerName) != null) {
            datetime = (new SimpleDateFormat("[MM/dd/yyyy hh:mm:ss] ")).format(data);
         }

         if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
            sb.append("<tr><td>");
            sb.append("#" + (i + 1));
            sb.append("</td><td>");
            sb.append(playerName);
            sb.append("</td><td>");
            sb.append(Output.getTimeBreakdown(time, Output.TIMEDETAIL.LONG));
            sb.append("</td><td>");
            sb.append(datetime);
            sb.append("</td></tr>");
            write(file, sb.toString());
            sb.delete(0, sb.length());
         } else {
            sb.append("#" + (i + 1) + " " + playerName);
            sb.append("                              ", sb.length(), 23);
            sb.append(Output.getTimeBreakdown(time, Output.TIMEDETAIL.LONG) + " Last Login:" + datetime);
            write(file, sb.toString());
            sb.delete(0, sb.length());
         }
      } else if (type == Report.ReportType.AFK) {
         try {
            if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
               sb.append("<tr><td>");
               sb.append("#" + i);
               sb.append("</td><td>");
               sb.append(playerName);
               sb.append("</td><td>");
               sb.append(Output.getTimeBreakdown(rs.getLong("afkToday"), Output.TIMEDETAIL.LONG));
               sb.append("</td><td>");
               sb.append(Output.getTimeBreakdown(rs.getLong("afkWeek"), Output.TIMEDETAIL.LONG));
               sb.append("</td><td>");
               sb.append(Output.getTimeBreakdown(rs.getLong("afkMonth"), Output.TIMEDETAIL.LONG));
               sb.append("</td><td>");
               write(file, sb.toString());
               sb.delete(0, sb.length());
            } else {
               sb.append("#" + (i + 1) + " " + playerName);
               sb.append("                              ", sb.length(), 22);
               sb.append(" Today: " + Output.getTimeBreakdown(rs.getLong("afkToday"), Output.TIMEDETAIL.LONG));
               sb.append(" This Week: " + Output.getTimeBreakdown(rs.getLong("afkWeek"), Output.TIMEDETAIL.LONG));
               sb.append(" This Month: " + Output.getTimeBreakdown(rs.getLong("afkMonth"), Output.TIMEDETAIL.LONG));
               write(file, sb.toString());
               sb.delete(0, sb.length());
               serverTodayTime += time;
            }
         } catch (SQLException var12) {
            var12.printStackTrace();
         }
      } else if (time >= 0L) {
         if (OnTime.reportFormat.equalsIgnoreCase("HTML")) {
            sb.append("<tr><td>");
            sb.append("#" + (i + 1));
            sb.append("</td><td>");
            sb.append(playerName);
            sb.append("</td><td>");
            sb.append(Output.getTimeBreakdown(time, Output.TIMEDETAIL.LONG));
            sb.append("</td><td>");
            sb.append(Output.getTimeBreakdown(data, Output.TIMEDETAIL.LONG));
            sb.append("</td></tr>");
            write(file, sb.toString());
            sb.delete(0, sb.length());
         } else {
            sb.append("#" + (i + 1) + " " + playerName);
            sb.append("                              ", sb.length(), 23);
            if (type == Report.ReportType.TODAYTIME) {
               sb.append(" Today: " + Output.getTimeBreakdown(time, Output.TIMEDETAIL.LONG));
            } else if (type == Report.ReportType.WEEKLY) {
               sb.append(" This Week: " + Output.getTimeBreakdown(time, Output.TIMEDETAIL.LONG));
            } else {
               sb.append(" This Month: " + Output.getTimeBreakdown(time, Output.TIMEDETAIL.LONG));
            }

            sb.append(" Total: " + Output.getTimeBreakdown(data, Output.TIMEDETAIL.LONG));
            write(file, sb.toString());
            sb.delete(0, sb.length());
         }

         serverTodayTime += time;
      }

   }

   public static void write(File logfile, String string) {
      try {
         BufferedWriter out = new BufferedWriter(new FileWriter(logfile, true));
         out.write(string);
         out.newLine();
         out.close();
      } catch (IOException var3) {
         var3.printStackTrace();
      }

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

   public static boolean purgeReports(File fileFolder, ReportType type) {
      long latestDay = 0L;
      long today = Calendar.getInstance().getTimeInMillis();
      String reportName;
      String sqlRoot;
      switch (type) {
         case TODAYTIME:
            if (OnTime.dailyReportRetention < 1) {
               return false;
            }

            latestDay = today - TimeUnit.DAYS.toMillis((long)OnTime.dailyReportRetention);
            reportName = "DailyReport";
            sqlRoot = "ontime-daily";
            break;
         case WEEKLY:
            if (OnTime.weeklyReportRetention < 1) {
               return false;
            }

            latestDay = today - TimeUnit.DAYS.toMillis((long)OnTime.weeklyReportRetention);
            reportName = "WeeklyReport";
            sqlRoot = "ontime-monthly";
            break;
         case MONTHLY:
            if (OnTime.monthlyReportRetention < 1) {
               return false;
            }

            latestDay = today - TimeUnit.DAYS.toMillis((long)OnTime.monthlyReportRetention);
            reportName = "MonthlyReport";
            sqlRoot = "ontime-monthly";
            break;
         case AFK:
            if (OnTime.afkReportRetention < 1) {
               return false;
            }

            latestDay = today - TimeUnit.DAYS.toMillis((long)OnTime.afkReportRetention);
            reportName = "AFKReport";
            sqlRoot = "ontime-afk";
            break;
         default:
            return false;
      }

      boolean deletedOne = false;
      if (!OnTime.reportFormat.equalsIgnoreCase("MYSQL")) {
         File[] listOfFiles = fileFolder.listFiles();

         for(int i = 0; i < listOfFiles.length; ++i) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(reportName) && listOfFiles[i].lastModified() < latestDay) {
               if (listOfFiles[i].delete()) {
                  LogFile.write(2, "Purged old report file:" + listOfFiles[i].getName());
                  deletedOne = true;
               } else {
                  LogFile.write(3, "Attempt to purged old report file FAILED:" + listOfFiles[i].getName());
               }
            }
         }
      } else {
         int count = _plugin.get_dataio().removeExpiredReportsFromMySQL(sqlRoot, today);
         if (count > 0) {
            LogFile.write(2, count + " " + reportName + " reports removed from MySQL.");
            deletedOne = true;
         } else {
            LogFile.console(0, "No report tables to remove from MySQL");
         }
      }

      return deletedOne;
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Report$ReportType() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Report$ReportType;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[Report.ReportType.values().length];

         try {
            var0[Report.ReportType.AFK.ordinal()] = 5;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[Report.ReportType.MONTHLY.ordinal()] = 4;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[Report.ReportType.ONTIME.ordinal()] = 1;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[Report.ReportType.TODAYTIME.ordinal()] = 2;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[Report.ReportType.WEEKLY.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Report$ReportType = var0;
         return var0;
      }
   }

   public static enum ReportType {
      ONTIME,
      TODAYTIME,
      WEEKLY,
      MONTHLY,
      AFK;
   }
}
