package org.ontime.ontime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.Rewards.RewardData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PlayingTime {
   private static OnTime _plugin;
   static FileConfiguration playtimeImport;
   public HashMap map = new HashMap();
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$PlayingTime$timeScope;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$PlayingTime$topAdder;

   public PlayingTime(OnTime plugin) {
      _plugin = plugin;
   }

   public HashMap getPlaytimeMap() {
      return _plugin.get_playingtime().map;
   }

   public void setMap(HashMap map) {
      this.map = map;
   }

   public void buildPlaytimeMap(String worldName) {
      this.getPlaytimeMap().clear();
      Iterator var3 = _plugin.get_dataio().getPlayerMap().keySet().iterator();

      while(var3.hasNext()) {
         String key = (String)var3.next();
         PlayerData playerData = Players.getData(key);
         PlayTimeData worldData = Players.getWorldTime(playerData, worldName);
         if (worldData != null) {
            long current = _plugin.get_logintime().current(playerData, worldName);
            if (worldData.totalTime + current > 0L) {
               this.getPlaytimeMap().put(key, worldData.totalTime + current);
            }
         }
      }

   }

   public long totalOntime(String playerName) {
      return this.totalOntime(playerName, OnTime.serverID);
   }

   public long totalOntime(String playerName, String worldName) {
      if (!this.hasOnTimeRecord(playerName)) {
         return 0L;
      } else {
         PlayerData playerData = null;
         if ((playerData = Players.getData(playerName)) == null) {
            return 0L;
         } else {
            long baseTime = 0L;
            PlayTimeData worldData = null;
            if ((worldData = Players.getWorldTime(playerData, worldName)) != null) {
               baseTime = worldData.totalTime;
            }

            if (!_plugin.get_logintime().playerIsOnline(playerData)) {
               return baseTime;
            } else if (!worldName.equalsIgnoreCase(OnTime.serverID) && !playerData.lastWorld.equalsIgnoreCase(worldName)) {
               return baseTime;
            } else {
               long currentPlayTime = Calendar.getInstance().getTimeInMillis() - _plugin.get_logintime().lastLogin(playerData, worldName) - _plugin.get_awayfk().getAFKTime(playerData);
               return baseTime + currentPlayTime;
            }
         }
      }
   }

   public static boolean playerHasOnTimeRecord(String playerName) {
      return Players.hasOnTimeRecord(playerName);
   }

   public boolean hasOnTimeRecord(String playerName) {
      if (Players.playerHasData(playerName)) {
         return true;
      } else {
         return OnTime.dataStorage == DataIO.datastorage.MYSQL ? _plugin.get_dataio().loadPlayerDataMySQL(playerName) : false;
      }
   }

   public void updateGlobal(PlayerData playerData) {
      Long currentPlayTime = _plugin.get_logintime().current(playerData);
      if (currentPlayTime != 0L) {
         this.updateWorld(playerData, OnTime.serverID, 0L);
         if (OnTime.multiServer) {
            this.updateWorld(playerData, OnTime.multiServerName, 0L);
         }

         if (OnTime.perWorldEnable) {
            this.updateWorld(playerData, playerData.lastWorld, 0L);
         }
      } else {
         LogFile.write(0, "No update made " + playerData.playerName + " current PlayTime was zero.");
      }

      _plugin.get_awayfk().resetAFKTime(playerData);
   }

   public void updateWorld(PlayerData playerData, String worldName, long lastLogin) {
      if (worldName != null) {
         Long currentPlayTime = _plugin.get_logintime().current(playerData, worldName);
         PlayTimeData worldData = null;
         if ((worldData = Players.getWorldTime(playerData, worldName)) != null) {
            worldData.todayTime += currentPlayTime;
            worldData.weekTime += currentPlayTime;
            worldData.monthTime += currentPlayTime;
            worldData.totalTime = this.totalOntime(playerData.playerName, worldName);
            if (lastLogin > 0L) {
               worldData.lastLogin = lastLogin;
            }
         } else {
            Players.setWorldTime(playerData, worldName, currentPlayTime, currentPlayTime, currentPlayTime, currentPlayTime, lastLogin);
         }
      }

   }

   public static void topGamers(CommandSender sender, int listLength, timeScope scope, topAdder adder) {
      HashMap map = null;
      String[] data = new String[]{"play", scope.toString().toLowerCase(), null};
      switch (scope) {
         case TOTAL:
            _plugin.get_playingtime().buildPlaytimeMap(OnTime.serverID);
            map = _plugin.get_playingtime().getPlaytimeMap();
            data[2] = String.valueOf(OnTime.todayStart);
            break;
         case TODAY:
            _plugin.get_todaytime().buildTodaytimeMap(OnTime.serverID);
            map = _plugin.get_todaytime().getDayMap();
            data[2] = String.valueOf(OnTime.todayStart);
            break;
         case WEEK:
            _plugin.get_todaytime().buildWeektimeMap(OnTime.serverID);
            map = _plugin.get_todaytime().getWeekMap();
            data[2] = String.valueOf(OnTime.weekStart);
            break;
         case MONTH:
            _plugin.get_todaytime().buildMonthtimeMap(OnTime.serverID);
            map = _plugin.get_todaytime().getMonthMap();
            data[2] = String.valueOf(OnTime.monthStart);
      }

      if (map.size() == 0) {
         sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.topListError.noPlayers"));
      } else {
         ValueComparator bvc = new ValueComparator(map);
         TreeMap sorted_map = new TreeMap(bvc);
         sorted_map.putAll(map);
         if (sorted_map.size() < listLength) {
            listLength = sorted_map.size();
         }

         Output.generate("output.topListHeader", sender, data);
         String key2 = (String)sorted_map.firstKey();
         long last = 0L;
         PlayerData playerData = null;

         for(int i = 0; i < listLength; ++i) {
            StringBuilder sb = new StringBuilder(64);
            if (sorted_map.get(key2) == null) {
               sender.sendMessage("ONTIME ERROR>> No data found for " + playerData.playerName);
            } else {
               playerData = Players.getData(key2);
               String playerName = Output.getMixedName(playerData.playerName);
               if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                  while((playerData.permissions & PlayerData.OTPerms.TOPTEN.mask()) != 0) {
                     key2 = (String)sorted_map.higherKey(key2);
                     if (key2 == null) {
                        return;
                     }

                     playerData = Players.getData(key2);
                     playerName = Output.getMixedName(playerData.playerName);
                  }
               }

               sb.append("# ");
               if (i + 1 < 10) {
                  sb.append(" ");
               }

               String playingTime = Output.getTimeBreakdown((Long)sorted_map.get(key2), Output.TIMEDETAIL.LONG);
               sb.append(Integer.toString(i + 1) + ":" + ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListTimeColor").substring(1)) + playingTime + " " + ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListNameColor").substring(1)) + playerName);
               String adderText = null;
               PlayTimeData worldTime = Players.getWorldTime(playerData, OnTime.serverID);
               switch (adder) {
                  case ONLINE:
                  default:
                     if (_plugin.get_logintime().playerIsOnline(playerData)) {
                        adderText = ChatColor.GREEN + " [ONLINE]";
                     }
                     break;
                  case LOGIN:
                     sb.append(ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListExtrasColor").substring(1)));
                     last = _plugin.get_logintime().lastLogin(playerData, OnTime.serverID);
                     if (last > 0L) {
                        adderText = (new SimpleDateFormat(" [MM/dd/yyyy hh:mm] ")).format(last);
                     } else {
                        adderText = Output.OnTimeOutput.getString("output.topListError.lastLoginNA");
                     }
                     break;
                  case TODAY:
                     sb.append(ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListExtrasColor").substring(1)));
                     if (worldTime != null) {
                        adderText = " [" + Output.getTimeBreakdown(worldTime.todayTime, Output.TIMEDETAIL.SHORT) + "]";
                     } else {
                        adderText = ChatColor.RED + " [" + Output.OnTimeOutput.getString("output.topListError.notOnline") + " " + Output.OnTimeOutput.getString("output.scope." + adder.toString().toLowerCase()) + "]";
                     }
                     break;
                  case WEEK:
                     sb.append(ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListExtrasColor").substring(1)));
                     if (worldTime != null) {
                        adderText = " [" + Output.getTimeBreakdown(worldTime.weekTime, Output.TIMEDETAIL.SHORT) + "]";
                     } else {
                        adderText = ChatColor.RED + " [" + Output.OnTimeOutput.getString("output.topListError.notOnline") + " " + Output.OnTimeOutput.getString("output.scope." + adder.toString().toLowerCase()) + "]";
                     }
                     break;
                  case MONTH:
                     sb.append(ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListExtrasColor").substring(1)));
                     if (worldTime != null) {
                        adderText = " [" + Output.getTimeBreakdown(worldTime.monthTime, Output.TIMEDETAIL.SHORT) + "]";
                     } else {
                        adderText = ChatColor.RED + " [" + Output.OnTimeOutput.getString("output.topListError.notOnline") + " " + Output.OnTimeOutput.getString("output.scope." + adder.toString().toLowerCase()) + "]";
                     }
                     break;
                  case RANK:
                     sb.append(ChatColor.getByChar(Output.OnTimeOutput.getString("output.topListExtrasColor").substring(1)));
                     String rank = null;
                     if (OnTime.permission != null) {
                        rank = _plugin.get_rewards().getCurrentGroup(playerName);
                     }

                     if (rank != null) {
                        adderText = " [" + rank + "]";
                     } else {
                        adderText = ChatColor.RED + " [" + Output.OnTimeOutput.getString("output.error.noData") + "]";
                     }
               }

               if (adderText != null) {
                  sb.append(adderText);
               }

               sender.sendMessage(sb.toString());
            }

            key2 = (String)sorted_map.higherKey(key2);
            if (key2 == null) {
               return;
            }
         }

      }
   }

   public void purgeFile() {
      int numPurged = 0;
      PlayerData playerData = null;
      if (OnTime.purgeEnable) {
         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            if (!DataIO.mysqlNew.checkMySQLConnection()) {
               return;
            }

            _plugin.get_dataio().loadXXPlayerDataMySQL("lastlogin", DataIO.mysqlload.PURGE, 0);
         }

         String[] keys = new String[_plugin.get_dataio().getPlayerMap().size()];
         _plugin.get_dataio().getPlayerMap().keySet().toArray(keys);

         for(int index = _plugin.get_dataio().getPlayerMap().size() - 1; index >= 0; --index) {
            playerData = Players.getData(keys[index]);
            LogFile.console(0, "Looking to purge " + playerData.playerName);
            Boolean purgeExclude = false;
            if (OnTime.dataStorage == DataIO.datastorage.MYSQL && (playerData.permissions & PlayerData.OTPerms.PURGE.mask()) != 0) {
               purgeExclude = true;
            }

            if (!_plugin.get_logintime().playerIsOnline(playerData) && !purgeExclude) {
               long playTime = Players.getWorldTime(playerData, OnTime.serverID).totalTime;
               long minutes = TimeUnit.MILLISECONDS.toMinutes(playTime);
               long lastlogin = _plugin.get_logintime().lastLogin(playerData, OnTime.serverID);
               long currentTime = Calendar.getInstance().getTimeInMillis();
               if (minutes < (long)OnTime.purgeTimeMin) {
                  LogFile.write(2, "Have removed " + playerData.playerName + " Play time:" + TimeUnit.MILLISECONDS.toSeconds(playTime) + " seconds.");
                  _plugin.get_dataio().removePlayerCompletely(DataIO.REMOVEKEY.NAME_UUID, playerData);
                  ++numPurged;
               } else if (TimeUnit.MILLISECONDS.toDays(currentTime - lastlogin) > OnTime.purgeLoginDay) {
                  String datetime = (new SimpleDateFormat("[MM/dd/yyyy hh:mm:ss] ")).format(lastlogin);
                  LogFile.write(2, "Removed " + playerData.playerName + "   Last login was :" + datetime + "(" + TimeUnit.MILLISECONDS.toDays(currentTime - lastlogin) + " Days Ago)");
                  if (OnTime.purgeDemotionEnable) {
                     boolean demotionSuccess = false;
                     Player player = null;
                     if ((player = Players.getOnlinePlayer(playerData.playerName)) != null) {
                        demotionSuccess = _plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PAG, player, OnTime.purgeDemotionGroup);
                     }

                     if (!demotionSuccess) {
                        LogFile.write(3, "{PlayingTime.PurgeFIle} ' Auto Demotion Group change execution failed.");
                     } else {
                        LogFile.write(2, "Have demoted " + playerData.playerName + " to " + OnTime.purgeDemotionGroup);
                     }
                  }

                  if (this.purgeReward(playerData.playerName)) {
                     LogFile.console(0, "Executed purge reward for " + playerData.playerName);
                  }

                  _plugin.get_dataio().removePlayerCompletely(DataIO.REMOVEKEY.NAME_UUID, playerData);
                  ++numPurged;
               }
            }
         }

         if (numPurged > 0) {
            LogFile.console(1, "[OnTime] Purged " + numPurged + " players due to low play time.");
            LogFile.write(3, "[OnTime] Purged " + numPurged + " players due to low play time.");
         }
      }

   }

   private boolean purgeReward(String playerName) {
      RewardData reward = null;
      int index = true;
      boolean oneFound = false;

      for(int i = 0; i < _plugin.get_rewards().getNumDefinedRewards(); ++i) {
         reward = _plugin.get_rewards().getRewardData()[i];
         if (reward.occurs == RewardData.Occurs.PURGE) {
            oneFound = true;
            int index;
            if ((index = _plugin.get_rewards().setReward(playerName, RewardData.EventReference.REALTIME, reward.time, i, reward, (String[])null)) > -1) {
               _plugin.get_rewards().issue(playerName, reward, index);
            }
         }
      }

      return oneFound;
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$PlayingTime$timeScope() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$PlayingTime$timeScope;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[PlayingTime.timeScope.values().length];

         try {
            var0[PlayingTime.timeScope.MONTH.ordinal()] = 4;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[PlayingTime.timeScope.TODAY.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[PlayingTime.timeScope.TOTAL.ordinal()] = 1;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[PlayingTime.timeScope.WEEK.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$PlayingTime$timeScope = var0;
         return var0;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$PlayingTime$topAdder() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$PlayingTime$topAdder;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[PlayingTime.topAdder.values().length];

         try {
            var0[PlayingTime.topAdder.LOGIN.ordinal()] = 2;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[PlayingTime.topAdder.MONTH.ordinal()] = 5;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[PlayingTime.topAdder.ONLINE.ordinal()] = 1;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[PlayingTime.topAdder.RANK.ordinal()] = 6;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[PlayingTime.topAdder.TODAY.ordinal()] = 3;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[PlayingTime.topAdder.WEEK.ordinal()] = 4;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$PlayingTime$topAdder = var0;
         return var0;
      }
   }

   public static enum timeScope {
      TOTAL,
      TODAY,
      WEEK,
      MONTH;
   }

   public static enum topAdder {
      ONLINE,
      LOGIN,
      TODAY,
      WEEK,
      MONTH,
      RANK;
   }
}
