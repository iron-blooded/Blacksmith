package org.ontime.ontime;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.Rewards.RewardData;
import me.edge209.OnTime.Rewards.RewardInstance;
import me.edge209.OnTime.Rewards.Rewards;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Output {
   private static OnTime _plugin;
   static File outputFile;
   public static FileConfiguration OnTimeOutput;
   static boolean isRewardString = false;
   static int rewardIndex = 0;
   static List onlineFields;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$timeScope;

   public Output(OnTime plugin) {
      _plugin = plugin;
   }

   public static List getOnlineFields() {
      return onlineFields;
   }

   private static void setOnlineFields(List _onlineFields) {
      onlineFields = _onlineFields;
   }

   public static boolean initOutput(File folder) {
      outputFile = new File(folder, "output.yml");
      if (!outputFile.exists()) {
         outputFile.getParentFile().mkdirs();
         _plugin.copy(_plugin.getResource("output.yml"), outputFile);
         LogFile.console(1, "Generated new output.yml file.");
      }

      OnTimeOutput = new YamlConfiguration();

      try {
         OnTimeOutput.load(outputFile);
      } catch (Exception var4) {
         var4.printStackTrace();
         return false;
      }

      if (Updates.checkOutputUpgrade(OnTimeOutput)) {
         LogFile.console(3, "Upgraded to latest version of output.yml");
         YamlConfiguration refresh = new YamlConfiguration();

         try {
            refresh.load(outputFile);
         } catch (Exception var3) {
            var3.printStackTrace();
            return false;
         }

         OnTimeOutput = refresh;
      }

      setOnlineFields(OnTimeOutput.getStringList("onlineReport"));
      if (!getOnlineFields().contains("player")) {
         LogFile.console(3, "Online Report must contain the 'player' field.  Online Tracking disabled.");
         OnTime.onlineTrackingEnable = false;
      }

      LogFile.console(0, "Online Data fields: " + getOnlineFields());
      LogFile.console(1, "Loading from output.yaml");
      return true;
   }

   public static boolean generate(String paragraph, CommandSender sender, String playerName, RewardData reward) {
      return generateSection(OnTimeOutput, paragraph, sender, playerName, reward, -1, (String[])null);
   }

   public static boolean generate(CommandSender sender, String paragraph, String playerName, String[] data) {
      return generateSection(OnTimeOutput, paragraph, sender, playerName, (RewardData)null, -1, data);
   }

   public static boolean generate(String paragraph, CommandSender sender, String playerName, RewardData reward, int index) {
      return generateSection(OnTimeOutput, paragraph, sender, playerName, reward, index, (String[])null);
   }

   public static boolean generate(String paragraph, String playerName, RewardData reward, int index) {
      return generateSection(OnTimeOutput, paragraph, (CommandSender)null, playerName, reward, index, (String[])null);
   }

   public static boolean generate(String paragraph, CommandSender sender, String[] data) {
      return generateSection(OnTimeOutput, paragraph, sender, (String)null, (RewardData)null, -1, data);
   }

   private static boolean generateSection(FileConfiguration output, String paragraph, CommandSender sender, String playerName, RewardData reward, int index, String[] data) {
      Player player = null;
      if (sender == null) {
         player = Players.getOnlinePlayer(playerName);
         if (player == null) {
            LogFile.write(10, "{output.generateSection} Section: " + paragraph + "   Player not found: " + playerName);
            return false;
         }
      } else if (sender instanceof Player) {
         player = (Player)sender;
         sender = null;
      }

      return sectionOut(output, paragraph, sender, player, playerName, reward, index, data);
   }

   private static boolean sectionOut(FileConfiguration output, String paragraph, CommandSender console, Player player, String playerName, RewardData reward, int playersRewardIndex, String[] data) {
      int numLines = false;
      isRewardString = false;
      int numLines = OnTimeOutput.getInt(paragraph + ".lines");
      if (numLines == 0 && reward.type != RewardData.RewardType.COMMAND) {
         LogFile.write(1, "ERROR {output.sectionOut} " + paragraph + ".lines returned zero");
         return false;
      } else {
         PlayerData playerData = null;
         if (playerName != null) {
            playerData = Players.getData(playerName);
         }

         for(int i = 1; i <= numLines; ++i) {
            rewardIndex = 0;
            String line = OnTimeOutput.getString(paragraph + ".line-" + i);

            do {
               String message = lineOut(output, line, playerName, playerData, reward, rewardIndex, playersRewardIndex, false, data);
               if (console != null) {
                  if (message != null) {
                     console.sendMessage(message);
                  }
               } else if (player != null) {
                  if (message != null) {
                     player.sendMessage(message);
                  }
               } else {
                  LogFile.console(3, "ERROR {output.sectionOut} Null 'player' for " + playerName);
                  LogFile.console(3, "ERROR Aborted output: " + message);
               }

               ++rewardIndex;
            } while(isRewardString);
         }

         return true;
      }
   }

   public static String lineOut(FileConfiguration output, String line, String playerName, PlayerData playerData, RewardData reward, int rewardIndex, int playersRewardIndex, boolean isMsg, String[] data) {
      if (line == null) {
         return "{ontime.output.lineout} Invalid (null) output line specified.";
      } else {
         String[] tokens = scan(line, reward);
         StringBuilder sb = new StringBuilder(64);

         for(int i = 0; i < tokens.length; ++i) {
            if (tokens[i].startsWith("&")) {
               sb.append(ChatColor.getByChar(tokens[i].substring(1)));
            } else if (!tokens[i].startsWith("[")) {
               sb.append(tokens[i]);
            } else {
               if (tokens[i].equals("[player]")) {
                  sb.append(getMixedName(playerName));
               } else if (tokens[i].equals("[afk]")) {
                  if (OnTime.afkCheckEnable && playerData != null && playerData.afkData.currentlyAFK) {
                     sb.append(OnTimeOutput.getString("output.error.playerAFK"));
                  }
               } else {
                  long todayDate;
                  if (tokens[i].startsWith("[afk")) {
                     if (OnTime.afkCheckEnable) {
                        if (playerData != null) {
                           todayDate = 0L;
                           if (playerData.afkData.currentlyAFK) {
                              todayDate = Calendar.getInstance().getTimeInMillis() - playerData.afkData.AFKStartTime;
                           }

                           todayDate += playerData.afkData.totalAFKTime;
                           if (tokens[i].equals("[afktime]")) {
                              sb.append(getTimeBreakdown(todayDate, Output.TIMEDETAIL.SHORT));
                           } else if (tokens[i].equals("[afkToday]")) {
                              if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                                 sb.append(getTimeBreakdown(playerData.afkData.todayAFKTime + todayDate, Output.TIMEDETAIL.SHORT));
                              } else {
                                 sb.append(outputError("output.error.noData"));
                              }
                           } else if (tokens[i].equals("[afkWeek]")) {
                              if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                                 sb.append(getTimeBreakdown(playerData.afkData.weekAFKTime + todayDate, Output.TIMEDETAIL.SHORT));
                              } else {
                                 sb.append(outputError("output.error.noData"));
                              }
                           } else if (tokens[i].equals("[afkMonth]")) {
                              if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                                 sb.append(getTimeBreakdown(playerData.afkData.monthAFKTime + todayDate, Output.TIMEDETAIL.SHORT));
                              } else {
                                 sb.append(outputError("output.error.noData"));
                              }
                           } else {
                              sb.append(tokens[i]);
                           }
                        } else {
                           sb.append(output.getString("output.error.noAFKTime"));
                        }
                     } else {
                        sb.append(output.getString("output.error.noAFKTime"));
                     }
                  } else if (tokens[i].equals("[balance]")) {
                     LogFile.console(0, "Found [balance]");
                     if (OnTime.economy != null) {
                        sb.append(" " + OnTime.economy.getBalance(Players.getOfflinePlayer(playerName)));
                     } else {
                        sb.append(outputError("output.error.noData"));
                     }
                  } else if (tokens[i].equals("[current]")) {
                     sb.append(getCurrentTime(playerData, data));
                  } else {
                     String worldName;
                     PlayTimeData worldData;
                     if (tokens[i].equals("[daily]")) {
                        if (OnTime.collectPlayDetailEnable) {
                           if (playerData != null) {
                              worldData = null;
                              worldName = OnTime.serverID;
                              if (data != null) {
                                 worldName = data[0];
                              }

                              worldData = Players.getWorldTime(playerData, worldName);
                              if (worldData != null) {
                                 sb.append(getTimeBreakdown(worldData.todayTime + currentWorldTime(playerData, worldName), Output.TIMEDETAIL.SHORT));
                              } else {
                                 sb.append(outputError("output.error.notOnToday"));
                              }
                           } else {
                              sb.append(outputError("output.error.notOnToday"));
                           }
                        } else {
                           sb.append(outputError("output.error.notEnabled"));
                        }
                     } else if (tokens[i].equals("[dailyRefer]")) {
                        if (OnTime.collectReferDetailEnable) {
                           if (playerData != null) {
                              sb.append(playerData.dailyReferrals);
                           } else {
                              sb.append(outputError("output.error.nodata"));
                           }
                        } else {
                           sb.append(outputError("output.error.notEnabled"));
                        }
                     } else if (tokens[i].equals("[dailyVote]")) {
                        if (OnTime.collectVoteDetailEnable) {
                           if (playerData != null) {
                              sb.append(playerData.dailyVotes);
                           } else {
                              sb.append(outputError("output.error.nodata"));
                           }
                        } else {
                           sb.append(outputError("output.error.notEnabled"));
                        }
                     } else if (tokens[i].equals("[daysAgo]")) {
                        int daysAgo = _plugin.get_logintime().getDaysAgo(playerData);
                        if (daysAgo < 0) {
                           sb.append(outputError("output.error.noDaysAgo"));
                        } else {
                           sb.append(String.valueOf(daysAgo));
                        }
                     } else if (tokens[i].equals("[daysOn]")) {
                        if (playerData != null) {
                           sb.append(String.valueOf(playerData.daysOn));
                        } else {
                           sb.append(outputError("output.error.noDaysOn"));
                        }
                     } else if (tokens[i].equals("[$]")) {
                        if (OnTime.economy != null) {
                           sb.append(" " + OnTime.economy.currencyNamePlural());
                        }
                     } else if (tokens[i].equals("[firstLogin]")) {
                        todayDate = _plugin.get_logintime().getFirstLogin(playerData);
                        if (todayDate == 0L) {
                           sb.append(outputError("output.error.noDaysAgo"));
                        } else {
                           sb.append((new SimpleDateFormat(output.getString("output.dateFormat") + " ")).format(todayDate));
                        }
                     } else if (tokens[i].equals("[lastLogin]")) {
                        todayDate = _plugin.get_logintime().lastLogin(playerData, OnTime.serverID);
                        if (todayDate != 0L) {
                           sb.append((new SimpleDateFormat(output.getString("output.dateTimeFormat") + " ")).format(todayDate));
                        } else {
                           sb.append(outputError("output.error.noLastLogin"));
                        }
                     } else if (tokens[i].equals("[lastvote]")) {
                        if (playerData != null) {
                           if (playerData.lastVoteDate != 0L) {
                              sb.append((new SimpleDateFormat(output.getString("output.dateTimeFormat") + " ")).format(playerData.lastVoteDate));
                           } else {
                              sb.append(outputError("output.error.noData"));
                           }
                        } else {
                           sb.append(outputError("output.error.noData"));
                        }
                     } else if (tokens[i].equals("[monthly]")) {
                        if (OnTime.collectPlayDetailEnable) {
                           if (playerData != null) {
                              worldData = null;
                              worldName = OnTime.serverID;
                              if (data != null) {
                                 worldName = data[0];
                              }

                              worldData = Players.getWorldTime(playerData, worldName);
                              if (worldData != null) {
                                 sb.append(getTimeBreakdown(worldData.monthTime + currentWorldTime(playerData, worldName), Output.TIMEDETAIL.SHORT));
                              } else {
                                 sb.append(outputError("output.error.notOnMonth"));
                              }
                           } else {
                              sb.append(outputError("output.error.notOnMonth"));
                           }
                        } else {
                           sb.append(outputError("output.error.notEnabled"));
                        }
                     } else if (tokens[i].equals("[monthlyRefer]")) {
                        if (OnTime.collectReferDetailEnable) {
                           if (playerData != null) {
                              sb.append(playerData.monthlyReferrals);
                           } else {
                              sb.append(outputError("output.error.nodata"));
                           }
                        } else {
                           sb.append(outputError("output.error.notEnabled"));
                        }
                     } else if (tokens[i].equals("[monthlyVote]")) {
                        if (OnTime.collectVoteDetailEnable) {
                           if (playerData != null) {
                              sb.append(playerData.monthlyVotes);
                           } else {
                              sb.append(outputError("output.error.nodata"));
                           }
                        } else {
                           sb.append(outputError("output.error.notEnabled"));
                        }
                     } else if (tokens[i].equals("[monthStartDate]")) {
                        if (OnTime.monthStart != 0L) {
                           sb.append(ChatColor.getByChar(output.getString("output.topListTimeColor").substring(1)) + (new SimpleDateFormat(output.getString("output.dateFormat") + " ")).format(OnTime.monthStart));
                        } else {
                           sb.append(outputError("output.error.dateNotAvailable"));
                        }
                     } else if (tokens[i].equals("[points]")) {
                        if (playerData != null) {
                           sb.append(playerData.points);
                        } else {
                           sb.append(outputError("output.error.noData"));
                        }
                     } else {
                        String worldName;
                        if (tokens[i].equals("[rank]")) {
                           worldName = null;
                           if (OnTime.permission != null) {
                              worldName = _plugin.get_rewards().getCurrentGroup(playerName);
                           }

                           if (worldName != null) {
                              sb.append(worldName);
                           } else {
                              sb.append(outputError("output.error.noData"));
                           }
                        } else if (tokens[i].equals("[referrals]")) {
                           if (playerData != null) {
                              sb.append(playerData.totalReferrals);
                           } else {
                              sb.append(outputError("output.error.noData"));
                           }
                        } else if (tokens[i].equals("[referredBy]")) {
                           if (playerData != null) {
                              if (playerData.referredBy != null) {
                                 if (!playerData.referredBy.equalsIgnoreCase("null")) {
                                    sb.append(playerData.referredBy);
                                 } else {
                                    sb.append(outputError("output.error.noData"));
                                 }
                              } else {
                                 sb.append(outputError("output.error.noData"));
                              }
                           } else {
                              sb.append(outputError("output.error.noData"));
                           }
                        } else if (tokens[i].equals("[reportDate]")) {
                           todayDate = OnTime.todayStart;
                           sb.append(ChatColor.getByChar(output.getString("output.topListTimeColor").substring(1)) + (new SimpleDateFormat(output.getString("output.dateFormat") + " ")).format(todayDate));
                        } else if (tokens[i].equals("[scope]")) {
                           if (reward != null) {
                              switch (reward.scope) {
                                 case TOTAL:
                                    sb.append(output.getString("output.scope.total"));
                                    break;
                                 case DAILY:
                                    sb.append(output.getString("output.scope.today"));
                                    break;
                                 case WEEKLY:
                                    sb.append(output.getString("output.scope.week"));
                                    break;
                                 case MONTHLY:
                                    sb.append(output.getString("output.scope.month"));
                                    break;
                                 default:
                                    sb.append(" <scope error> ");
                              }
                           } else if (data != null) {
                              sb.append(output.getString("output.scope." + data[1]));
                           }
                        } else if (tokens[i].equals("[scopetime]")) {
                           worldName = OnTime.serverID;
                           if (OnTime.perWorldEnable && !reward.world.equalsIgnoreCase("all")) {
                              worldName = playerData.lastWorld;
                           }

                           sb.append(getTimeBreakdown(_plugin.get_rewards().getCurrentScopeTime(playerData, reward.scope, worldName), Output.TIMEDETAIL.SHORT));
                        } else if (tokens[i].equals("[serverName]")) {
                           sb.append(OnTime.serverName);
                        } else if (tokens[i].equals("[serverTime]")) {
                           sb.append((new SimpleDateFormat(output.getString("output.dateTimeFormat"))).format(Calendar.getInstance().getTime()));
                        } else if (tokens[i].equals("[topListScope]")) {
                           if (data != null) {
                              if (data[2] != null) {
                                 sb.append(output.getString("output.topListScope." + data[1]));
                              } else {
                                 sb.append(ChatColor.getByChar(output.getString("output.error.errorColor").substring(1)) + output.getString("output.error.noData"));
                              }
                           } else {
                              sb.append(ChatColor.getByChar(output.getString("output.error.errorColor").substring(1)) + output.getString("output.error.noData"));
                           }
                        } else if (tokens[i].equals("[topDateTime]")) {
                           if (data != null) {
                              if (data[2] != null) {
                                 sb.append(ChatColor.getByChar(output.getString("output.topListTimeColor").substring(1)) + (new SimpleDateFormat(output.getString("output.dateFormat") + " ")).format(Long.parseLong(data[2])));
                              } else {
                                 sb.append(ChatColor.getByChar(output.getString("output.topListTimeColor").substring(1)) + output.getString("output.time.alltime"));
                              }
                           } else {
                              sb.append(outputError("output.error.dateNotAvailable"));
                           }
                        } else if (tokens[i].equals("[total]")) {
                           if (Players.hasOnTimeRecord(playerName)) {
                              if (data == null) {
                                 sb.append(getTimeBreakdown(_plugin.get_playingtime().totalOntime(playerName), Output.TIMEDETAIL.SHORT));
                              } else {
                                 sb.append(getTimeBreakdown(_plugin.get_playingtime().totalOntime(playerName, data[0]), Output.TIMEDETAIL.SHORT));
                              }
                           } else {
                              sb.append(outputError("output.error.noTotal"));
                           }
                        } else if (tokens[i].equals("[votes]")) {
                           if (playerData != null) {
                              sb.append(playerData.totalVotes);
                           } else {
                              sb.append(outputError("output.error.noData"));
                           }
                        } else if (tokens[i].equals("[voteService]")) {
                           if (Rewards.getPlayerRewards(playerName)[playersRewardIndex].data != null) {
                              sb.append(Rewards.getPlayerRewards(playerName)[playersRewardIndex].data[0]);
                           } else {
                              sb.append(outputError("output.error.noData"));
                           }
                        } else if (tokens[i].equals("[uuid]")) {
                           if (playerData != null) {
                              if (playerData.uuid != null) {
                                 sb.append(playerData.uuid);
                              } else {
                                 sb.append(outputError("output.error.noData"));
                              }
                           } else {
                              sb.append(outputError("output.error.noData"));
                           }
                        } else if (tokens[i].equals("[weekly]")) {
                           if (OnTime.collectPlayDetailEnable) {
                              if (playerData != null) {
                                 worldData = null;
                                 worldName = OnTime.serverID;
                                 if (data != null) {
                                    worldName = data[0];
                                 }

                                 worldData = Players.getWorldTime(playerData, worldName);
                                 if (worldData != null) {
                                    sb.append(getTimeBreakdown(worldData.weekTime + currentWorldTime(playerData, worldName), Output.TIMEDETAIL.SHORT));
                                 } else {
                                    sb.append(outputError("output.error.notOnWeek"));
                                 }
                              } else {
                                 sb.append(outputError("output.error.notOnWeek"));
                              }
                           } else {
                              sb.append(outputError("output.error.notEnabled"));
                           }
                        } else if (tokens[i].equals("[weeklyRefer]")) {
                           if (OnTime.collectReferDetailEnable) {
                              if (playerData != null) {
                                 sb.append(playerData.weeklyReferrals);
                              } else {
                                 sb.append(outputError("output.error.nodata"));
                              }
                           } else {
                              sb.append(outputError("output.error.notEnabled"));
                           }
                        } else if (tokens[i].equals("[weeklyVote]")) {
                           if (OnTime.collectVoteDetailEnable) {
                              if (playerData != null) {
                                 sb.append(playerData.weeklyVotes);
                              } else {
                                 sb.append(outputError("output.error.nodata"));
                              }
                           } else {
                              sb.append(outputError("output.error.notEnabled"));
                           }
                        } else if (tokens[i].equals("[weekStartDate]")) {
                           if (OnTime.weekStart != 0L) {
                              sb.append(ChatColor.getByChar(output.getString("output.topListTimeColor").substring(1)) + (new SimpleDateFormat(output.getString("output.dateFormat") + " ")).format(OnTime.weekStart));
                           } else {
                              sb.append(outputError("output.error.dateNotAvailable"));
                           }
                        } else if (tokens[i].equals("[world]")) {
                           if (data != null) {
                              sb.append(data[0]);
                           } else {
                              sb.append(outputError("output.error.noData"));
                           }
                        } else if (!isMsg) {
                           if (tokens[i].equals("[quantity]")) {
                              if (reward != null) {
                                 sb.append(reward.getQuantity());
                              } else {
                                 sb.append(outputError("output.error.noRewardQuantity"));
                              }
                           } else if (tokens[i].equals("[reward]")) {
                              RewardData local = null;
                              if (!OnTime.rewardsEnable) {
                                 sb.append(outputError("output.error.notEnabled"));
                              } else if (reward != null) {
                                 local = reward;
                              } else if (Rewards.playerHasRewards(playerName)) {
                                 local = _plugin.get_rewards().getRewardData()[Rewards.getPlayerRewards(playerName)[0].rewardID];
                              } else {
                                 sb.append(outputError("output.error.noReward"));
                              }

                              if (local.itemstack != null) {
                                 String[] material = local.reward.split("[+]");
                                 sb.append(material[0].toLowerCase() + _plugin.get_rewards().enchantmentList(local.itemstack).toLowerCase());
                              } else {
                                 sb.append(local.reward);
                              }
                           } else {
                              RewardInstance[] playerReward;
                              if (tokens[i].equals("[rewardString]")) {
                                 if (!OnTime.rewardsEnable && !OnTime.messagesEnable) {
                                    sb.append(outputError("output.error.notEnabled"));
                                 } else if (reward != null) {
                                    sb.append(_plugin.get_rewards().rewardString(reward));
                                 } else if (Rewards.playerHasRewards(playerName)) {
                                    playerReward = Rewards.getPlayerRewards(playerName);
                                    if (playerReward[rewardIndex] == null) {
                                       LogFile.console(3, "ERROR {Output.lineout} playerReward NULL pointer for " + playerName + " reward: " + Integer.toString(rewardIndex));
                                       return playerName + ": Reward - Output NULL Pointer Error: " + Integer.toString(rewardIndex);
                                    }

                                    if (playerReward.length > rewardIndex + 1) {
                                       isRewardString = true;
                                    } else {
                                       isRewardString = false;
                                    }

                                    if (!playerReward[rewardIndex].active) {
                                       return null;
                                    }

                                    if (playerReward[rewardIndex].rewardID < 0) {
                                       sb.append(playerReward[rewardIndex].identifier);
                                    } else {
                                       sb.append(_plugin.get_rewards().rewardString(_plugin.get_rewards().getRewardData()[playerReward[rewardIndex].rewardID]));
                                    }
                                 } else {
                                    sb.append(outputError("output.error.noReward"));
                                 }
                              } else if (tokens[i].equals("[rewardTime]")) {
                                 if (!OnTime.rewardsEnable && !OnTime.messagesEnable) {
                                    sb.append(outputError("output.error.notEnabled"));
                                 } else if (reward != null) {
                                    sb.append(getTimeBreakdown(reward.time, Output.TIMEDETAIL.SHORT));
                                 } else if (Rewards.playerHasRewards(playerName)) {
                                    sb.append(getTimeBreakdown(_plugin.get_rewards().getRewardData()[Rewards.getPlayerRewards(playerName)[rewardIndex].rewardID].time, Output.TIMEDETAIL.SHORT));
                                 } else {
                                    sb.append(outputError("output.error.noRewardTime"));
                                 }
                              } else if (tokens[i].equals("[timeToReward]")) {
                                 if (!OnTime.rewardsEnable && !OnTime.messagesEnable) {
                                    sb.append(outputError("output.error.notEnabled"));
                                 } else if (playerData.afkData.currentlyAFK) {
                                    sb.append(output.getString("output.error.playerAFK"));
                                 } else if (Rewards.playerHasRewards(playerName)) {
                                    playerReward = Rewards.getPlayerRewards(playerName);
                                    if (playerReward[rewardIndex].form == RewardInstance.RewardForm.STANDARD) {
                                       sb.append(output.getString("output.timeToReward.time") + " " + _plugin.get_rewards().timeToRewardString(playerData, rewardIndex));
                                    } else if (playerReward[rewardIndex].reference == RewardData.EventReference.LOGIN) {
                                       sb.append(output.getString("output.timeToReward.login"));
                                    } else if (playerReward[rewardIndex].reference == RewardData.EventReference.CHANGEWORLD) {
                                       sb.append(output.getString("output.timeToReward.worldChange"));
                                    } else {
                                       sb.append(output.getString("output.timeToReward.time") + " " + getTimeBreakdown(_plugin.get_rewards().getPersonalRemainingTime(playerReward[rewardIndex], playerName), Output.TIMEDETAIL.SHORT));
                                    }
                                 } else {
                                    sb.append(outputError("output.error.noRewardTime"));
                                 }
                              } else if (tokens[i].equals("[rewardType]")) {
                                 if (!OnTime.rewardsEnable && !OnTime.messagesEnable) {
                                    sb.append(outputError("output.error.notEnabled"));
                                 } else if (Rewards.playerHasRewards(playerName)) {
                                    playerReward = Rewards.getPlayerRewards(playerName);
                                    if (playerReward[rewardIndex].active) {
                                       if (playerReward[rewardIndex].form == RewardInstance.RewardForm.STANDARD) {
                                          if (_plugin.get_rewards().getRewardData()[playerReward[rewardIndex].rewardID].occurs != RewardData.Occurs.DELAY) {
                                             sb.append(_plugin.get_rewards().getRewardData()[playerReward[rewardIndex].rewardID].scope.label());
                                          } else {
                                             sb.append(outputError("output.error.noRewardTime"));
                                          }
                                       } else if (playerReward[rewardIndex].form == RewardInstance.RewardForm.PERSONAL) {
                                          if (_plugin.get_rewards().getRewardData()[playerReward[rewardIndex].rewardID].occurs == RewardData.Occurs.REFERSOURCE) {
                                             if (playerReward[rewardIndex].data[0].equalsIgnoreCase("referred-by")) {
                                                sb.append("payable to " + playerReward[rewardIndex].data[1]);
                                             } else {
                                                sb.append("referral of " + playerReward[rewardIndex].data[1]);
                                             }
                                          } else if (_plugin.get_rewards().getRewardData()[playerReward[rewardIndex].rewardID].occurs == RewardData.Occurs.REFERTARGET) {
                                             sb.append("referral target");
                                          } else {
                                             sb.append("I:" + playerReward[rewardIndex].reference);
                                          }
                                       } else if (playerReward[rewardIndex].form == RewardInstance.RewardForm.MESSAGE) {
                                          sb.append("msg");
                                       }
                                    } else {
                                       sb.append(outputError("output.error.noRewardTime"));
                                    }
                                 } else {
                                    sb.append(outputError("output.error.noRewardTime"));
                                 }
                              } else if (tokens[i].equals("[referredPlayer]")) {
                                 if (Rewards.getPlayerRewards(playerName)[playersRewardIndex].data != null) {
                                    sb.append(Rewards.getPlayerRewards(playerName)[playersRewardIndex].data[1]);
                                 } else {
                                    sb.append("another player");
                                 }
                              } else if (tokens[i].equals("[topSpot]")) {
                                 if (Rewards.getPlayerRewards(playerName)[playersRewardIndex].data != null) {
                                    sb.append(Rewards.getPlayerRewards(playerName)[playersRewardIndex].data[2]);
                                 } else {
                                    sb.append(output.getString("output.error.noTopData"));
                                 }
                              } else if (tokens[i].equals("[topPeriod]")) {
                                 if (Rewards.getPlayerRewards(playerName)[playersRewardIndex].data != null) {
                                    sb.append(Rewards.getPlayerRewards(playerName)[playersRewardIndex].data[1]);
                                 } else {
                                    sb.append(output.getString("output.error.noTopData"));
                                 }
                              } else if (tokens[i].equals("[eventRef]")) {
                                 if (playersRewardIndex >= 0) {
                                    sb.append(output.getString("output.eventRef." + _plugin.get_rewards().getRewardData()[Rewards.getPlayerRewards(playerName)[playersRewardIndex].rewardID].reference.label().toLowerCase()));
                                    LogFile.console(0, "looking for output.eventRef." + _plugin.get_rewards().getRewardData()[Rewards.getPlayerRewards(playerName)[playersRewardIndex].rewardID].reference.label().toLowerCase() + " for rewardID #" + Rewards.getPlayerRewards(playerName)[playersRewardIndex].rewardID);
                                 } else if (data != null) {
                                    sb.append(output.getString("output.eventRef." + data[0]));
                                    LogFile.console(0, "looking for output.eventRef." + data[0]);
                                 }
                              } else if (tokens[i].equals("[rewardWorld]")) {
                                 if (reward != null) {
                                    sb.append(reward.world);
                                 } else {
                                    sb.append(outputError("output.error.noData"));
                                 }
                              } else {
                                 sb.append(tokens[i]);
                              }
                           }
                        } else if (isMsg) {
                           if (tokens[i].equals("[otherPlayer]")) {
                              if (data != null) {
                                 sb.append(data[0]);
                              } else {
                                 sb.append("[otherPlayer] is not available");
                              }
                           } else if (tokens[i].equals("[otherTotal]")) {
                              if (data != null) {
                                 sb.append(data[1]);
                              } else {
                                 sb.append(ChatColor.getByChar(output.getString("messages.error.errorColor").substring(1)) + "[otherTotal] not available");
                              }
                           }
                        }
                     }
                  }
               }

               sb.append(" ");
            }
         }

         return sb.toString();
      }
   }

   private static String[] scan(String line, RewardData reward) {
      String[] temp = new String[100];
      int size = 0;
      int start = 0;

      int ending;
      for(int i = 0; i < line.length(); ++i) {
         char c = line.charAt(i);
         switch (c) {
            case '&':
               if (i != start) {
                  temp[size++] = line.substring(start, i);
               }

               temp[size++] = line.substring(i, i + 2);
               i += 2;
               start = i;
               break;
            case '\'':
               if (i > 0) {
                  temp[size++] = line.substring(start, i);
               }

               start = i + 1;
               break;
            case '<':
               if (!line.substring(i).contains(">")) {
                  break;
               }

               if (i != start) {
                  temp[size++] = line.substring(start, i);
               }

               ++i;
               ending = line.indexOf(">", i);
               String[] tokens = scan(line.substring(i, ending), (RewardData)null);

               label71:
               for(int j = 0; j < tokens.length; ++j) {
                  if (tokens[j].equalsIgnoreCase("[rewardWorld]") && OnTime.perWorldEnable && !reward.world.equalsIgnoreCase("all")) {
                     int k = 0;

                     while(true) {
                        if (k >= tokens.length) {
                           break label71;
                        }

                        temp[size++] = tokens[k];
                        ++k;
                     }
                  }
               }

               i = ending + 1;
               start = i;
               break;
            case '[':
               if (i > 0) {
                  temp[size++] = line.substring(start, i);
               }

               start = i;
               break;
            case ']':
               temp[size++] = line.substring(start, i + 1);
               start = i + 1;
         }
      }

      temp[size++] = line.substring(start);
      String[] result = new String[size];

      for(ending = 0; ending < size; ++ending) {
         result[ending] = temp[ending];
      }

      return result;
   }

   public static String outputError(String error) {
      return ChatColor.getByChar(OnTimeOutput.getString("output.error.errorColor").substring(1)) + OnTimeOutput.getString(error);
   }

   public static String getDateTime(long time) {
      return (new SimpleDateFormat(OnTimeOutput.getString("output.dateTimeFormat") + " ")).format(time);
   }

   public static String getTimeBreakdown(long millis, TIMEDETAIL detail) {
      String timeDetail = OnTimeOutput.getString("output.timeDetail");
      long days = 0L;
      long hours = 0L;
      long minutes = 0L;
      long seconds = 0L;
      if (millis < 0L) {
         return OnTimeOutput.getString("output.time.na");
      } else if (millis >= TimeUnit.DAYS.toMillis(9999L)) {
         return OnTimeOutput.getString("output.time.indi");
      } else if (TimeUnit.MILLISECONDS.toMinutes(millis) == 0L) {
         return TimeUnit.MILLISECONDS.toSeconds(millis) + " " + OnTimeOutput.getString("output.time.seconds");
      } else {
         if (timeDetail.contains("DD")) {
            days = TimeUnit.MILLISECONDS.toDays(millis);
            millis -= TimeUnit.DAYS.toMillis(days);
         }

         if (timeDetail.contains("HH")) {
            hours = TimeUnit.MILLISECONDS.toHours(millis);
            millis -= TimeUnit.HOURS.toMillis(hours);
         }

         if (timeDetail.contains("MM")) {
            minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
            millis -= TimeUnit.MINUTES.toMillis(minutes);
         }

         if (timeDetail.contains("SS")) {
            seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
         }

         StringBuilder sb = new StringBuilder(64);
         if ((days > 0L || detail == Output.TIMEDETAIL.LONG) && timeDetail.contains("DD")) {
            if (detail != Output.TIMEDETAIL.VSHORT) {
               if (days < 100L) {
                  sb.append(" ");
               }

               if (days < 10L) {
                  sb.append(" ");
               }
            }

            sb.append(days);
            if (detail != Output.TIMEDETAIL.VSHORT) {
               sb.append(" " + OnTimeOutput.getString("output.time.days") + " ");
            } else {
               sb.append("D ");
            }
         }

         if ((hours > 0L || detail == Output.TIMEDETAIL.LONG) && timeDetail.contains("HH")) {
            if (detail != Output.TIMEDETAIL.VSHORT && hours < 10L) {
               sb.append(" ");
            }

            sb.append(hours);
            if (detail != Output.TIMEDETAIL.VSHORT) {
               sb.append(" " + OnTimeOutput.getString("output.time.hours") + " ");
            } else {
               sb.append("H ");
            }
         }

         if ((minutes > 0L || detail == Output.TIMEDETAIL.LONG) && timeDetail.contains("MM")) {
            if (detail != Output.TIMEDETAIL.VSHORT && minutes < 10L) {
               sb.append(" ");
            }

            sb.append(minutes);
            if (detail != Output.TIMEDETAIL.VSHORT) {
               sb.append(" " + OnTimeOutput.getString("output.time.minutes") + " ");
            } else {
               sb.append("M ");
            }
         }

         if (minutes <= 5L && seconds > 0L && detail == Output.TIMEDETAIL.SHORT) {
            sb.append(seconds + " " + OnTimeOutput.getString("output.time.seconds"));
         }

         return sb.toString();
      }
   }

   public static boolean broadcast(FileConfiguration output, String paragraph, String permission, String subjectName, RewardData reward, int playersRewardIndex) {
      if (!OnTime.rewardBroadcastEnable) {
         return false;
      } else {
         int numLines = false;
         int numLines = output.getInt(paragraph + ".lines");
         if (numLines == 0) {
            LogFile.write(1, "ERROR {output.broadcast} " + paragraph + ".lines returned zero");
            return false;
         } else {
            PlayerData playerData = null;
            if ((playerData = Players.getData(subjectName)) == null) {
               LogFile.write(10, "{broadcast} Broadcast requested regarding " + subjectName + " whom has no playerData.");
               return false;
            } else {
               Iterator var9 = _plugin.getServer().getOnlinePlayers().iterator();

               while(true) {
                  Player player;
                  String targetName;
                  do {
                     do {
                        if (!var9.hasNext()) {
                           return true;
                        }

                        player = (Player)var9.next();
                        targetName = OnTime.getPlayerName(player);
                     } while(targetName.endsWith(subjectName));
                  } while(permission != null && !OnTime.permission.has(player, permission));

                  for(int k = 1; k <= numLines; ++k) {
                     rewardIndex = 0;
                     String line = output.getString(paragraph + ".line-" + k);
                     String message = lineOut(output, line, subjectName, playerData, reward, rewardIndex, playersRewardIndex, false, (String[])null);
                     if (player != null) {
                        if (message != null) {
                           player.sendMessage(message);
                        } else {
                           LogFile.console(0, "ERROR {output.broadcast} Error in output creation");
                        }
                     } else {
                        LogFile.console(3, "ERROR {output.boradcast} Null 'player' for " + targetName);
                        LogFile.console(3, "ERROR Aborted output: " + message);
                     }
                  }
               }
            }
         }
      }
   }

   public static long currentWorldTime(PlayerData playerData, String worldName) {
      Player player = Players.getOnlinePlayer(playerData.playerName);
      if (player == null) {
         return 0L;
      } else {
         return !worldName.equalsIgnoreCase(OnTime.serverID) && !player.getWorld().getName().equalsIgnoreCase(worldName) ? 0L : _plugin.get_logintime().current(playerData, worldName);
      }
   }

   public static String getCurrentTime(PlayerData playerData, String[] data) {
      long current = 0L;
      String worldName = OnTime.serverID;
      if (data != null) {
         worldName = data[0];
      }

      if (playerData != null) {
         PlayTimeData worldTime = null;
         long rollOver = 0L;
         if ((worldTime = Players.getWorldTime(playerData, worldName)) != null) {
            rollOver = worldTime.rollOver;
         }

         current = currentWorldTime(playerData, worldName) + rollOver;
         if (worldName.equalsIgnoreCase(OnTime.serverID)) {
            if (current != 0L) {
               return getTimeBreakdown(current, Output.TIMEDETAIL.SHORT);
            } else {
               return playerData.afkData.currentlyAFK ? OnTimeOutput.getString("output.error.playerAFK") : outputError("output.error.notOnline");
            }
         } else {
            return getTimeBreakdown(current, Output.TIMEDETAIL.SHORT);
         }
      } else {
         return outputError("output.error.notOnline");
      }
   }

   public static String getMixedName(String playerName) {
      if (!playerName.toLowerCase().equals(playerName)) {
         return playerName;
      } else {
         OfflinePlayer offPlayer = Players.getOfflinePlayer(playerName);
         return offPlayer != null && offPlayer.getName() != null ? offPlayer.getName() : playerName;
      }
   }

   public void displayList(CommandSender sender, String[] list, boolean truncate) {
      StringBuilder sb = new StringBuilder(72);

      for(int i = 0; i <= list.length / 4; ++i) {
         for(int j = 0; j < 4 && i * 4 + j < list.length; ++j) {
            if (list[i * 4 + j].length() > 14 && truncate) {
               list[i * 4 + j] = list[i * 4 + j].substring(0, 13);
            }

            sb.append(list[i * 4 + j]);
            if (truncate) {
               sb.append("                                                                                ", sb.length(), (j + 1) * 16);
            } else {
               sb.append("  ");
            }
         }

         sb.trimToSize();
         sender.sendMessage(sb.toString());
         sb.setLength(0);
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

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$timeScope() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$timeScope;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[RewardData.timeScope.values().length];

         try {
            var0[RewardData.timeScope.DAILY.ordinal()] = 2;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[RewardData.timeScope.MONTHLY.ordinal()] = 4;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[RewardData.timeScope.TOTAL.ordinal()] = 1;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[RewardData.timeScope.WEEKLY.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$timeScope = var0;
         return var0;
      }
   }

   public static enum TIMEDETAIL {
      LONG,
      SHORT,
      VSHORT;
   }
}
