package me.edge209.OnTime.Rewards;

import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.Commands;
import me.edge209.OnTime.DataIO;
import me.edge209.OnTime.LogFile;
import me.edge209.OnTime.OnTime;
import me.edge209.OnTime.Output;
import me.edge209.OnTime.PermissionsHandler;
import me.edge209.OnTime.PlayerData;
import me.edge209.OnTime.Players;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReferredCommands {
   private static OnTime _plugin;

   public ReferredCommands(OnTime plugin) {
      _plugin = plugin;
   }

   public boolean onReferredCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
      if (!OnTime.referredByEnable) {
         sender.sendMessage(ChatColor.RED + "Player referrals are not enabled on this server.");
         return true;
      } else if (args.length < 1) {
         OnTime.get_commands().syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Commands.KEYWORDS.REFER, (String)null);
         return true;
      } else {
         String playerName;
         if (args[0].equalsIgnoreCase("by")) {
            playerName = null;
            if (!(sender instanceof Player)) {
               sender.sendMessage("This command cannot be executed by console.");
               return true;
            } else {
               Player player = (Player)sender;
               if (!OnTime.permission.has(sender, "ontime.referredby.enable")) {
                  sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.noPermission"));
                  return true;
               } else {
                  String playerName = sender.getName();
                  if (!Rewards.get_rewardUtilities().canBeReferred((Player)sender)) {
                     sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.refer.used"));
                     return true;
                  } else if (args.length < 2) {
                     OnTime.get_commands().syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Commands.KEYWORDS.REFER, (String)null);
                     return true;
                  } else {
                     String sourceName = args[1];
                     if (!Players.hasOnTimeRecord(sourceName)) {
                        Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, sourceName, (RewardData)null);
                        if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                           String[] list = _plugin.get_dataio().getDataListFromMySQL("'%" + sourceName + "%'", " LIKE ", "playerName", "playerName", "playerName", "ASC");
                           if (list != null) {
                              sender.sendMessage(Output.OnTimeOutput.getString("output.possible"));
                              _plugin.get_output().displayList(sender, list, false);
                           }
                        }

                        return true;
                     } else if (playerName.equalsIgnoreCase(sourceName)) {
                        sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.refer.notYourself"));
                        return true;
                     } else if (OnTime.referredByMaxTime >= 0L && _plugin.get_playingtime().totalOntime(playerName) > TimeUnit.HOURS.toMillis(OnTime.referredByMaxTime)) {
                        sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.refer.tooLong"));
                        if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                           _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "referredby", "referredByMaxTime", playerName);
                        }

                        if (OnTime.referredByPermTrackEnable && !_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PAP, player, "ontime.referredby.success")) {
                           LogFile.console(3, "Error. Adding of 'ontime.referredby.success' permission for " + playerName + " failed.");
                        }

                        return true;
                     } else {
                        PlayerData playerData = Players.getData(sourceName);
                        boolean oneFound = false;

                        for(int i = 0; i < _plugin.get_rewards().getNumDefinedRewards(); ++i) {
                           RewardData reward = _plugin.get_rewards().getRewardData()[i];
                           boolean scheduleReward = true;
                           if (reward.occurs == RewardData.Occurs.REFERSOURCE) {
                              int referCount = 0;
                              if (OnTime.dataStorage == DataIO.datastorage.MYSQL && playerData != null) {
                                 if (reward.scope == RewardData.timeScope.TOTAL) {
                                    referCount = playerData.totalReferrals + 1;
                                 } else if (reward.scope == RewardData.timeScope.DAILY) {
                                    referCount = playerData.dailyReferrals + 1;
                                 } else if (reward.scope == RewardData.timeScope.WEEKLY) {
                                    referCount = playerData.weeklyReferrals + 1;
                                 } else if (reward.scope == RewardData.timeScope.MONTHLY) {
                                    referCount = playerData.monthlyReferrals + 1;
                                 }
                              }

                              if (playerData != null && reward.count > 0 && referCount % reward.count != 0) {
                                 scheduleReward = false;
                              }

                              if (scheduleReward) {
                                 String[] data = new String[]{"referred-by", sourceName, null};
                                 if (oneFound) {
                                    data[2] = "0";
                                 } else {
                                    data[2] = "1";
                                 }

                                 if (_plugin.get_playingtime().totalOntime(playerName) < reward.time) {
                                    _plugin.get_rewards().setReward(playerName, RewardData.EventReference.PLAYTIME, reward.time, i, reward, data);
                                 } else {
                                    _plugin.get_rewards().setReward(playerName, RewardData.EventReference.DELTATIME, TimeUnit.SECONDS.toMillis((long)(i + 1)), i, reward, data);
                                 }

                                 Output.generate("output.refer.sourceReward", sender, sourceName, reward);
                                 oneFound = true;
                              }
                           } else if (reward.occurs == RewardData.Occurs.REFERTARGET) {
                              if (_plugin.get_playingtime().totalOntime(playerName) < reward.time) {
                                 _plugin.get_rewards().setReward(playerName, RewardData.EventReference.PLAYTIME, reward.time, i, reward, (String[])null);
                              } else {
                                 _plugin.get_rewards().setReward(playerName, RewardData.EventReference.DELTATIME, TimeUnit.SECONDS.toMillis((long)(i + 1)), i, reward, (String[])null);
                              }

                              Output.generate("output.refer.targetReward", sender, sender.getName(), reward);
                           }
                        }

                        if (!oneFound) {
                           Output.generate((String)"output.refer.noReward", (CommandSender)sender, sourceName, (RewardData)null);
                           if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                              _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "referredby", sourceName, playerName);
                           }

                           if (OnTime.referredByPermTrackEnable && !_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PAP, player, "ontime.referredby.success")) {
                              LogFile.console(3, "Error. Adding of 'ontime.referredby.success' permission for " + playerName + " failed.");
                           }

                           _plugin.get_rewards().incrementReferrals(sourceName);
                        }

                        return true;
                     }
                  }
               }
            }
         } else if (args[0].equalsIgnoreCase("undo")) {
            if (!OnTime.permission.has(sender, "ontime.referred.admin")) {
               sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.noPermission"));
               return true;
            } else if (args.length < 2) {
               OnTime.get_commands().syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Commands.KEYWORDS.REFER, (String)null);
               return true;
            } else {
               playerName = args[1];
               if (!Players.hasOnTimeRecord(playerName)) {
                  Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[1], (RewardData)null);
                  return true;
               } else {
                  boolean undone = false;
                  if (OnTime.referredByPermTrackEnable) {
                     Player player = Players.getOfflinePlayer(playerName).getPlayer();
                     if (player != null) {
                        LogFile.console(0, "Server record found for " + playerName);
                        if (_plugin.get_permissionsHandler().playerHas(playerName, "ontime.referredby.success")) {
                           if (_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PRP, player, "ontime.referredby.success")) {
                              LogFile.console(0, "'ontime.referredby.success' removed for " + playerName);
                              undone = true;
                           } else {
                              LogFile.write(3, "Attempt to remove 'ontime.referredby.success' from " + playerName + " failed.");
                           }
                        } else {
                           LogFile.console(0, playerName + " did not have 'ontime.referredby.success'");
                        }
                     } else {
                        LogFile.console(0, "NO Server record found for " + playerName);
                        undone = true;
                     }

                     if (undone) {
                        sender.sendMessage("Permission removal (ontime.referredby.success) attempted for " + playerName);
                     }
                  }

                  if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                     if (Players.getData(playerName).referredBy == null) {
                        _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "referredby", "null", playerName);
                        undone = true;
                     } else if (!Players.getData(playerName).referredBy.equalsIgnoreCase("null")) {
                        _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "referredby", "null", playerName);
                        undone = true;
                     }
                  }

                  if (Players.playerHasData(playerName)) {
                     Players.getData(playerName).referredBy = "null";
                  }

                  if (Rewards.get_rewardUtilities().getReferredbyMap().containsKey(playerName)) {
                     undone = true;
                     Rewards.get_rewardUtilities().getReferredbyMap().remove(playerName);
                  }

                  if (Rewards.playerHasRewards(playerName)) {
                     RewardInstance[] rewards = Rewards.getPlayerRewards(playerName);

                     for(int j = 0; j < rewards.length; ++j) {
                        if (rewards[j].active && _plugin.get_rewards().getRewardData()[rewards[j].index].occurs == RewardData.Occurs.REFERSOURCE) {
                           if (rewards[j].data != null && rewards[j].data[0].equalsIgnoreCase("referred-by")) {
                              _plugin.getServer().getScheduler().cancelTask(rewards[j].scheduleID);
                              rewards[j].active = false;
                              undone = true;
                              LogFile.write(1, "{referred undo} 'Referred by' reward of " + playerName + " by " + rewards[j].data[1] + " cancelled.");
                           }

                           LogFile.write(1, "{referred undo} 'refersource' reward (" + _plugin.get_rewards().getRewardData()[rewards[j].index].identifier + ") at record " + j + " had no data.");
                        } else if (rewards[j].active && _plugin.get_rewards().getRewardData()[rewards[j].index].occurs == RewardData.Occurs.REFERTARGET) {
                           _plugin.getServer().getScheduler().cancelTask(rewards[j].scheduleID);
                           rewards[j].active = false;
                           undone = true;
                           LogFile.write(1, "{referred undo} 'Referred Taget' reward for " + playerName + " cancelled.");
                        }
                     }
                  }

                  if (undone) {
                     sender.sendMessage("Referral sucessfully undone for " + playerName);
                  } else {
                     sender.sendMessage(ChatColor.RED + "No referral information found for " + playerName);
                  }

                  return true;
               }
            }
         } else if (args[0].equalsIgnoreCase("list")) {
            if (OnTime.dataStorage != DataIO.datastorage.MYSQL) {
               sender.sendMessage(ChatColor.RED + "In OnTime/config.yml 'dataStorage: MYSQL' is required to use this command.");
               return true;
            } else if (!OnTime.permission.has(sender, "ontime.referredby.list")) {
               sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.noPermission"));
               return true;
            } else if (args.length < 2) {
               sender.sendMessage(ChatColor.RED + "/referred list <playername>");
               return true;
            } else {
               playerName = args[1];
               if (!Players.hasOnTimeRecord(playerName)) {
                  Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[1], (RewardData)null);
                  return true;
               } else {
                  String[] list = _plugin.get_dataio().getDataListFromMySQL("'" + playerName + "'", " = ", "referredby", "playerName", "playerName", "ASC");
                  if (list != null) {
                     Output.generate((String)"output.refer.others", (CommandSender)sender, playerName, (RewardData)null);
                     _plugin.get_output().displayList(sender, list, true);
                  } else {
                     Output.generate((String)"output.refer.none", (CommandSender)sender, playerName, (RewardData)null);
                  }

                  return true;
               }
            }
         } else {
            OnTime.get_commands().syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Commands.KEYWORDS.REFER, (String)null);
            return true;
         }
      }
   }
}
