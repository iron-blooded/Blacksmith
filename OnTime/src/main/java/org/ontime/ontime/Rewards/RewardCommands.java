package me.edge209.OnTime.Rewards;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.DataIO;
import me.edge209.OnTime.LogFile;
import me.edge209.OnTime.Messages;
import me.edge209.OnTime.OnTime;
import me.edge209.OnTime.Output;
import me.edge209.OnTime.Players;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RewardCommands {
   private static OnTime _plugin;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardCommands$KEYWORDS;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$RewardType;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardCommands$SYNTAXERROR;

   public RewardCommands(OnTime plugin) {
      _plugin = plugin;
   }

   public KEYWORDS checkKeyword(String word) {
      KEYWORDS[] var5;
      int var4 = (var5 = RewardCommands.KEYWORDS.values()).length;

      for(int var3 = 0; var3 < var4; ++var3) {
         KEYWORDS s1 = var5[var3];
         if (word.length() >= s1.input().length() && word.substring(0, s1.input().length()).equalsIgnoreCase(s1.input())) {
            return s1;
         }
      }

      LogFile.console(0, " Keyword " + word + " not found.  Length:" + word.length());
      return RewardCommands.KEYWORDS.NA;
   }

   public boolean onRewardCommand(CommandSender sender, Command cmd, RewardOrPenalty rOrP, String[] args) {
      int rewardID = -1;
      long time = -1L;
      int count = -1;
      RewardData.timeScope scope = null;
      RewardData.EventReference reference = null;
      String msgTag = null;
      String idTag = null;
      String world = "all";
      if (!OnTime.rewardsEnable && !OnTime.suspendOnTime) {
         sender.sendMessage(ChatColor.RED + "OnTime Rewards are not enabled on this server.");
         return true;
      } else {
         KEYWORDS Keyword = RewardCommands.KEYWORDS.NA;
         if (args.length < 2) {
            this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
            return true;
         } else {
            Keyword = this.checkKeyword(args[1]);
            if (!OnTime.permission.has(sender, "ontime.rewards.admin") && Keyword != RewardCommands.KEYWORDS.PURCHASE) {
               sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.noPermission"));
               return true;
            } else {
               Player player = null;
               if (sender instanceof Player) {
                  player = (Player)sender;
               }

               if (args.length >= 2 && Keyword != null) {
                  if (Keyword != RewardCommands.KEYWORDS.NEXT && Keyword != RewardCommands.KEYWORDS.LIST && Keyword != RewardCommands.KEYWORDS.L && args.length < 3) {
                     this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                     return true;
                  } else {
                     switch (Keyword) {
                        case ABSENCE:
                        case DAYSON:
                        case DEATH:
                        case EDIT:
                        case EXCLUSIVE:
                        case GENERAL:
                        case INDI:
                        case LINK:
                        case PERP:
                        case PURCHASE:
                        case PURGE:
                        case REFER:
                        case RECUR:
                        case REMOVE:
                        case SET:
                        case SINGLE:
                        case SHOP:
                        case TOP:
                        case VOTIFIER:
                           rewardID = this.checkRewardID(sender, args[2]);
                           if (rewardID == -1) {
                              return true;
                           }
                           break;
                        case INFO:
                           if (!args[2].equalsIgnoreCase("ALL")) {
                              rewardID = this.checkRewardID(sender, args[2]);
                              if (rewardID == -1) {
                                 return true;
                              }
                           } else {
                              rewardID = -1;
                           }
                     }

                     int numGroups;
                     switch (Keyword) {
                        case ADD:
                        case DAYSON:
                        case EDIT:
                        case PERP:
                        case RECUR:
                        case SET:
                           for(numGroups = 3; numGroups < args.length; ++numGroups) {
                              if (args[numGroups].length() > 1) {
                                 if ((args[numGroups].endsWith("D") || args[numGroups].endsWith("d")) && args[numGroups].substring(0, args[numGroups].length() - 1).matches("[+]?\\d+(\\/\\d+)?")) {
                                    time += TimeUnit.DAYS.toMillis((long)Integer.parseInt(args[numGroups].substring(0, args[numGroups].length() - 1)));
                                    if (time < 0L) {
                                       time = 0L;
                                    }
                                 } else if ((args[numGroups].endsWith("H") || args[numGroups].endsWith("h")) && args[numGroups].substring(0, args[numGroups].length() - 1).matches("[+]?\\d+(\\/\\d+)?")) {
                                    time += TimeUnit.HOURS.toMillis((long)Integer.parseInt(args[numGroups].substring(0, args[numGroups].length() - 1)));
                                    if (time < 0L) {
                                       time = 0L;
                                    }
                                 } else if ((args[numGroups].endsWith("M") || args[numGroups].endsWith("m")) && args[numGroups].substring(0, args[numGroups].length() - 1).matches("[+]?\\d+(\\/\\d+)?")) {
                                    time += TimeUnit.MINUTES.toMillis((long)Integer.parseInt(args[numGroups].substring(0, args[numGroups].length() - 1)));
                                    if (time < 0L) {
                                       time = 0L;
                                    }
                                 }
                              }
                           }

                           if (time > 0L) {
                              time += 5L;
                           }
                        default:
                           int existing;
                           switch (Keyword) {
                              case ADD:
                              case EDIT:
                                 for(numGroups = 3; numGroups < args.length; ++numGroups) {
                                    if (args[numGroups].length() > 1) {
                                       if (args[numGroups].startsWith("msg=") || args[numGroups].startsWith("MSG=")) {
                                          msgTag = args[numGroups].substring(args[numGroups].indexOf("=") + 1);
                                          if (!msgTag.equalsIgnoreCase("default") && !msgTag.equalsIgnoreCase("off") && Messages.messages.getString("message." + msgTag) == null) {
                                             sender.sendMessage(ChatColor.RED + "A message with tag '" + msgTag + "' cannot be found.");
                                             return true;
                                          }
                                       }

                                       if (args[numGroups].startsWith("tag=") || args[numGroups].startsWith("TAG=")) {
                                          int existing = true;
                                          idTag = args[numGroups].substring(args[numGroups].indexOf("=") + 1);
                                          if (idTag.length() == 0) {
                                             sender.sendMessage(ChatColor.RED + "An ID tag must be specified.");
                                             return true;
                                          }

                                          if ((existing = _plugin.get_rewards()._validateIdentifier(idTag, rewardID)) > 0) {
                                             sender.sendMessage(ChatColor.RED + "Reward with ID = " + (existing + 1) + " already has assigned the tag '" + idTag + "'");
                                             return true;
                                          }
                                       }

                                       if (args[numGroups].startsWith("world=") || args[numGroups].startsWith("world=")) {
                                          world = args[numGroups].substring(args[numGroups].indexOf("=") + 1);
                                          if (world.length() == 0) {
                                             sender.sendMessage(ChatColor.RED + "A world must be specified.");
                                             return true;
                                          }
                                       }
                                    }
                                 }
                              default:
                                 int i;
                                 int newID;
                                 RewardData.EventReference r1;
                                 RewardData.EventReference[] var30;
                                 switch (Keyword) {
                                    case ADD:
                                    case LIST:
                                    case L:
                                    case TOP:
                                       for(numGroups = 2; numGroups < args.length; ++numGroups) {
                                          RewardData.timeScope[] var21;
                                          newID = (var21 = RewardData.timeScope.values()).length;

                                          for(i = 0; i < newID; ++i) {
                                             RewardData.timeScope s1 = var21[i];
                                             if (args[numGroups].length() >= 3 && args[numGroups].substring(0, 3).equalsIgnoreCase(s1.label().substring(0, 3))) {
                                                scope = s1;
                                             }
                                          }

                                          newID = (var30 = RewardData.EventReference.values()).length;

                                          for(i = 0; i < newID; ++i) {
                                             r1 = var30[i];
                                             if (args[numGroups].length() >= 3 && args[numGroups].substring(0, 3).equalsIgnoreCase(r1.label().substring(0, 3))) {
                                                reference = r1;
                                             }
                                          }
                                       }
                                    default:
                                       switch (Keyword) {
                                          case REFER:
                                          case RECUR:
                                          case VOTIFIER:
                                             for(numGroups = 3; numGroups < args.length; ++numGroups) {
                                                if (args[numGroups].length() > 6 && (args[numGroups].startsWith("COUNT=") || args[numGroups].startsWith("count=")) && args[numGroups].substring(6, args[numGroups].length()).matches("[+]?\\d+(\\/\\d+)?")) {
                                                   count = Integer.parseInt(args[numGroups].substring(6, args[numGroups].length()));
                                                }
                                             }
                                          default:
                                             String playerName;
                                             KEYWORDS keyword4;
                                             KEYWORDS keyword2;
                                             RewardData reward;
                                             boolean goodEdit;
                                             switch (Keyword) {
                                                case ABSENCE:
                                                   switch (_plugin.get_rewards().getRewardData()[rewardID].type) {
                                                      case DELAY:
                                                      case ITEM:
                                                      case KIT:
                                                      case XP:
                                                         sender.sendMessage("'absence' designation for not supported for " + _plugin.get_rewards().getRewardData()[rewardID].type.toString() + " rewards.");
                                                         return true;
                                                      default:
                                                         if (_plugin.get_rewards().getRewardData()[rewardID].time < TimeUnit.DAYS.toMillis(1L)) {
                                                            sender.sendMessage(ChatColor.RED + "An 'absence' reward must have time value of at least 1 full day.  Request Failed.");
                                                            return true;
                                                         }

                                                         _plugin.get_rewards().getRewardData()[rewardID].reference = RewardData.EventReference.ABSENCE;
                                                         sender.sendMessage("'Abasense' designation for Reward# " + (rewardID + 1) + " successfully set.");
                                                         _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                         RewardUtilities.saveRewards(Rewards.rewardFile);
                                                         return true;
                                                   }
                                                case ADDGROUP:
                                                case ADDPERM:
                                                case COMMAND:
                                                case CMD:
                                                case DAILY:
                                                case DENIAL:
                                                case DEMOTION:
                                                case ECON:
                                                case EVENT:
                                                case GROUP:
                                                case GROUPREMOVE:
                                                case ITEM:
                                                case KIT:
                                                case MESSAGE:
                                                case MSG:
                                                case MONTHLY:
                                                case PERMISSION:
                                                case PLAYTIME:
                                                case POINTS:
                                                case PROMOTION:
                                                case REMOVEGROUP:
                                                case REMOVEPERM:
                                                case SPLICE:
                                                case TAG:
                                                case TIME:
                                                case TOTAL:
                                                default:
                                                   return false;
                                                case ADD:
                                                   if (msgTag == null && this.checkKeyword(args[2]) != RewardCommands.KEYWORDS.MESSAGE) {
                                                      msgTag = "default";
                                                   }

                                                   if (scope == null) {
                                                      scope = RewardData.timeScope.TOTAL;
                                                   }

                                                   if (reference == null) {
                                                      reference = RewardData.EventReference.PLAYTIME;
                                                   }

                                                   return this.addReward(sender, args, rewardID, idTag, time, scope, reference, msgTag, world);
                                                case DAYSON:
                                                   numGroups = (int)TimeUnit.MILLISECONDS.toDays(time);
                                                   if (numGroups < 1) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                      sender.sendMessage(ChatColor.RED + "At least one day must be specified.");
                                                      return true;
                                                   }

                                                   _plugin.get_rewards().setRecurring(RewardData.Occurs.DAYSON, rewardID, 0L, numGroups);
                                                   _plugin.get_rewards().setTime(rewardID, 0L);
                                                   sender.sendMessage("Reward " + (rewardID + 1) + " set to " + numGroups + " 'Days On'.");
                                                   return true;
                                                case DEATH:
                                                   _plugin.get_rewards().getRewardData()[rewardID].reference = RewardData.EventReference.DEATH;
                                                   sender.sendMessage("'Death' designation for Reward# " + (rewardID + 1) + " successfully set.");
                                                   _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                   RewardUtilities.saveRewards(Rewards.rewardFile);
                                                   return true;
                                                case DELETE:
                                                   playerName = args[2];
                                                   if (_plugin.get_rewards().deleteAllIndiRewards(playerName)) {
                                                      sender.sendMessage("Individual rewards sucessfully deleted for " + playerName);
                                                   } else {
                                                      sender.sendMessage(ChatColor.RED + "No individual rewards existed for " + playerName);
                                                   }

                                                   return true;
                                                case EDIT:
                                                   goodEdit = false;
                                                   if (args.length < 4) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                      return true;
                                                   } else {
                                                      keyword4 = this.checkKeyword(args[3]);
                                                      switch (keyword4) {
                                                         case MSG:
                                                            break;
                                                         case TAG:
                                                            idTag = args[4];
                                                            break;
                                                         case TIME:
                                                            if (time < 0L) {
                                                               this.syntaxError(sender, RewardCommands.SYNTAXERROR.MISSINGTIME, Keyword, (String)null);
                                                               return true;
                                                            }

                                                            String rewardTag = _plugin.get_rewards().getRewardData()[rewardID].identifier;
                                                            if (_plugin.get_rewards().setTime(rewardID, time)) {
                                                               sender.sendMessage(" Reward #" + (rewardID + 1) + " time updated to " + Output.getTimeBreakdown(time, Output.TIMEDETAIL.SHORT));
                                                               newID = _plugin.get_rewards().getRewardID(rewardTag);
                                                               if (rewardID != newID) {
                                                                  sender.sendMessage(" RewardID has changed from " + (rewardID + 1) + " to " + (newID + 1));
                                                                  rewardID = newID;
                                                               }

                                                               goodEdit = true;
                                                            } else {
                                                               sender.sendMessage(" Reward #" + (rewardID + 1) + " time update failed. ");
                                                            }
                                                            break;
                                                         case WORLD:
                                                            if (args[4].startsWith("+")) {
                                                               _plugin.get_rewards().getRewardData()[rewardID].world = args[4].substring(1);
                                                               _plugin.get_rewards().getRewardData()[rewardID].onWorld = true;
                                                            } else {
                                                               _plugin.get_rewards().getRewardData()[rewardID].world = args[4];
                                                               _plugin.get_rewards().getRewardData()[rewardID].onWorld = false;
                                                            }

                                                            goodEdit = true;
                                                            break;
                                                         default:
                                                            this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                                                            return true;
                                                      }

                                                      if (idTag != null) {
                                                         _plugin.get_rewards().updateLink(_plugin.get_rewards().getRewardData()[rewardID].identifier, idTag);
                                                         if (_plugin.get_rewards().getRewardData()[rewardID].exclusive.equalsIgnoreCase("E")) {
                                                            _plugin.get_rewards().updatePermInAllGroups(_plugin.get_rewards().getRewardData()[rewardID].permissionString, "ontime.reward." + idTag);
                                                         }

                                                         _plugin.get_rewards().getRewardData()[rewardID].identifier = idTag;
                                                         _plugin.get_rewards().getRewardData()[rewardID].permissionString = "ontime.reward." + idTag;
                                                         _plugin.get_rewards().updateIndiRewards(rewardID);
                                                         goodEdit = true;
                                                      }

                                                      if (msgTag != null) {
                                                         _plugin.get_rewards().getRewardData()[rewardID].message = msgTag;
                                                         goodEdit = true;
                                                      }

                                                      if (!goodEdit) {
                                                         this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                         return true;
                                                      }

                                                      _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                      RewardUtilities.saveRewards(Rewards.rewardFile);
                                                      return true;
                                                   }
                                                case EXCLUSIVE:
                                                   if (args.length < 4) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                      return true;
                                                   } else if (!args[3].equalsIgnoreCase("add") && !args[3].equalsIgnoreCase("remove")) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                                                      return true;
                                                   } else {
                                                      numGroups = args.length - 4;
                                                      String[] groups = null;
                                                      if (numGroups > 0) {
                                                         groups = new String[numGroups];

                                                         for(i = 0; i < numGroups; ++i) {
                                                            if (!_plugin.get_rewards().isValidGroup(args[i + 4])) {
                                                               this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, Keyword, args[i + 4]);
                                                               return true;
                                                            }

                                                            groups[i] = args[i + 4];
                                                         }
                                                      }

                                                      _plugin.get_rewards().setExclusive("E", args[3], rewardID, numGroups, groups);
                                                      sender.sendMessage("'Exclusive' settings for Reward# " + (rewardID + 1) + " successfully modified.");
                                                      _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                      return true;
                                                   }
                                                case GENERAL:
                                                   _plugin.get_rewards().setExclusive("A", (String)null, rewardID, 0, (String[])null);
                                                   sender.sendMessage("'General' designation for Reward# " + (rewardID + 1) + " successfully set.");
                                                   _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                   return true;
                                                case INDI:
                                                   _plugin.get_rewards().setRecurring(RewardData.Occurs.INDIVIDUAL, rewardID, 0L, 0);
                                                   sender.sendMessage("Individual instance for Reward# " + (rewardID + 1) + " successfully set.");
                                                   _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                   return true;
                                                case INFO:
                                                   _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                   return true;
                                                case LINK:
                                                   goodEdit = false;
                                                   if (args.length < 4) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                      return true;
                                                   } else {
                                                      if (args.length >= 5) {
                                                         if (this.checkKeyword(args[4]) != RewardCommands.KEYWORDS.SPLICE) {
                                                            this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[4]);
                                                            return true;
                                                         }

                                                         goodEdit = true;
                                                      }

                                                      existing = this.checkRewardID(sender, args[3]);
                                                      if (existing == -1) {
                                                         return true;
                                                      }

                                                      sender.sendMessage(_plugin.get_rewards().setChain(rewardID, existing, goodEdit));
                                                      _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                      return true;
                                                   }
                                                case LIST:
                                                case L:
                                                   keyword2 = null;
                                                   if (args.length > 2) {
                                                      keyword2 = this.checkKeyword(args[2]);
                                                   }

                                                   if (keyword2 != null && keyword2 != RewardCommands.KEYWORDS.EVENT) {
                                                      if (keyword2 == RewardCommands.KEYWORDS.TAG) {
                                                         _plugin.get_rewards().list(sender, Rewards.listRequest.TAG);
                                                      } else {
                                                         this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[2]);
                                                      }
                                                   } else {
                                                      _plugin.get_rewards().list(sender, Rewards.listRequest.EVENT);
                                                   }

                                                   return true;
                                                case NEXT:
                                                   _plugin.get_rewards().showSchedule(sender);
                                                   return true;
                                                case PERP:
                                                   reward = _plugin.get_rewards().getRewardData()[rewardID];
                                                   if (reward.type != RewardData.RewardType.PROMOTION && reward.type != RewardData.RewardType.PERMISSION) {
                                                      if (reward.occurs == RewardData.Occurs.VOTE_P) {
                                                         sender.sendMessage(ChatColor.RED + "This voting reward is already perpetual.");
                                                         return true;
                                                      }

                                                      if (time < 0L && reward.occurs != RewardData.Occurs.VOTE_S) {
                                                         this.syntaxError(sender, RewardCommands.SYNTAXERROR.MISSINGTIME, Keyword, (String)null);
                                                         return true;
                                                      }

                                                      if (reward.occurs == RewardData.Occurs.VOTE_S) {
                                                         _plugin.get_rewards().setRecurring(RewardData.Occurs.VOTE_P, rewardID, 0L, reward.count);
                                                      } else {
                                                         _plugin.get_rewards().setRecurring(RewardData.Occurs.PERPETUAL, rewardID, time, -1);
                                                      }

                                                      sender.sendMessage("Perpetual for Reward# " + (rewardID + 1) + " successfully set.");
                                                      _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                      return true;
                                                   }

                                                   sender.sendMessage(ChatColor.RED + "'group' and 'permission' rewards cannot repeat. ");
                                                   return true;
                                                case PURCHASE:
                                                   if (args.length < 3) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                      return true;
                                                   } else {
                                                      if (args.length >= 4) {
                                                         if (!OnTime.permission.has(sender, "ontime.rewards.admin")) {
                                                            sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.noPermission"));
                                                            return true;
                                                         }

                                                         playerName = args[3];
                                                      } else {
                                                         if (player == null) {
                                                            sender.sendMessage("A player must be specified when this command is run from the console.");
                                                            return true;
                                                         }

                                                         if (!_plugin.get_permissionsHandler().playerHas(player, "ontime.rewards.purchase")) {
                                                            OnTime.get_commands().noPermission(player);
                                                            return true;
                                                         }

                                                         playerName = OnTime.getPlayerName(player);
                                                      }

                                                      if (!Players.hasOnTimeRecord(playerName)) {
                                                         this.syntaxError(sender, RewardCommands.SYNTAXERROR.SPECIAL, Keyword, args[3] + " does not have an OnTime record.");
                                                         return true;
                                                      } else {
                                                         reward = _plugin.get_rewards().getRewardData()[rewardID];
                                                         if (reward.reference != RewardData.EventReference.SHOP_POINTS && reward.reference != RewardData.EventReference.SHOP_ECON) {
                                                            sender.sendMessage(ChatColor.RED + "Reward " + reward.identifier + " is not a shop reward.");
                                                            return true;
                                                         } else {
                                                            if (_plugin.get_rewards()._processShopReward(playerName, reward.identifier)) {
                                                               if (player == null) {
                                                                  sender.sendMessage("Reward purchase of " + reward.identifier + " sucessfully set for " + playerName);
                                                               }

                                                               return true;
                                                            }

                                                            if (player == null) {
                                                               sender.sendMessage(ChatColor.RED + "Reward purchase of " + reward.identifier + " failed for " + playerName);
                                                            }

                                                            return true;
                                                         }
                                                      }
                                                   }
                                                case PURGE:
                                                   _plugin.get_rewards().setRecurring(RewardData.Occurs.PURGE, rewardID, 0L, -1);
                                                   sender.sendMessage("Purge flag set for Reward# " + (rewardID + 1) + " successfully set.");
                                                   _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                   return true;
                                                case REFER:
                                                   String sourceTarget = "source";
                                                   if (args.length >= 4) {
                                                      if (args[3].equalsIgnoreCase("source")) {
                                                         if (count > 0 && OnTime.dataStorage != DataIO.datastorage.MYSQL) {
                                                            sender.sendMessage(ChatColor.RED + "OnTime is not configured to support a referral count. MySQL storage required.");
                                                            return true;
                                                         }
                                                      } else {
                                                         if (!args[3].equalsIgnoreCase("target")) {
                                                            this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                                                            return true;
                                                         }

                                                         if (count > 0) {
                                                            this.syntaxError(sender, RewardCommands.SYNTAXERROR.SPECIAL, Keyword, "'count' is not valid for referral target rewards");
                                                            return true;
                                                         }

                                                         sourceTarget = "target";
                                                      }
                                                   }

                                                   sender.sendMessage(_plugin.get_rewards().setReference(rewardID, sourceTarget, count));
                                                   _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                   return true;
                                                case RECUR:
                                                   RewardData.RewardType rewardType = _plugin.get_rewards().getRewardData()[rewardID].type;
                                                   if (rewardType != RewardData.RewardType.PROMOTION && rewardType != RewardData.RewardType.DEMOTION && rewardType != RewardData.RewardType.PERMISSION && rewardType != RewardData.RewardType.DENIAL && rewardType != RewardData.RewardType.ADDGROUP && rewardType != RewardData.RewardType.REMOVEGROUP) {
                                                      if (time < 0L) {
                                                         this.syntaxError(sender, RewardCommands.SYNTAXERROR.MISSINGTIME, Keyword, (String)null);
                                                         return true;
                                                      }

                                                      _plugin.get_rewards().setRecurring(RewardData.Occurs.RECURRING, rewardID, time, count);
                                                      sender.sendMessage("Recurrance for Reward# " + (rewardID + 1) + " successfully set.");
                                                      _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                      return true;
                                                   }

                                                   sender.sendMessage(ChatColor.RED + "'group' and 'permission' rewards cannot repeat. ");
                                                   return true;
                                                case REMOVE:
                                                   _plugin.get_rewards().remove(sender, rewardID);
                                                   return true;
                                                case SET:
                                                   if (args.length < 5) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                      return true;
                                                   } else {
                                                      playerName = args[4];
                                                      RewardData.EventReference eventReference = null;
                                                      newID = (var30 = RewardData.EventReference.values()).length;

                                                      for(i = 0; i < newID; ++i) {
                                                         r1 = var30[i];
                                                         if (args[3].equalsIgnoreCase(r1.label())) {
                                                            eventReference = r1;
                                                         }
                                                      }

                                                      if (eventReference != RewardData.EventReference.REALTIME && eventReference != RewardData.EventReference.DELTATIME && eventReference != RewardData.EventReference.PLAYTIME) {
                                                         if (eventReference != RewardData.EventReference.LOGIN) {
                                                            this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                                                            return true;
                                                         }

                                                         time = 0L;
                                                      } else {
                                                         if (time < 0L) {
                                                            this.syntaxError(sender, RewardCommands.SYNTAXERROR.MISSINGTIME, Keyword, (String)null);
                                                            return true;
                                                         }

                                                         if (eventReference == RewardData.EventReference.PLAYTIME && Players.hasOnTimeRecord(playerName) && time <= _plugin.get_playingtime().totalOntime(playerName)) {
                                                            sender.sendMessage(ChatColor.RED + playerName + " has already been on longer than the reward time specified.");
                                                            return true;
                                                         }
                                                      }

                                                      if (_plugin.get_rewards().getRewardData()[rewardID].occurs.code().startsWith("F")) {
                                                         sender.sendMessage(ChatColor.RED + "That is a referral reward,  It cannot be 'set' for a player.");
                                                         return true;
                                                      } else {
                                                         if (playerName == null) {
                                                            sender.sendMessage(ChatColor.RED + "No OnTime record of " + args[4]);
                                                            return true;
                                                         }

                                                         _plugin.get_awayfk().checkPlayerAFK(Players.getData(playerName));
                                                         if (_plugin.get_rewards().setReward(playerName, eventReference, time, rewardID, _plugin.get_rewards().getRewardData()[rewardID], (String[])null) > -1) {
                                                            sender.sendMessage("Reward sucessfully set for " + playerName + ".");
                                                         } else {
                                                            sender.sendMessage("Reward set failed for " + playerName + ".");
                                                         }

                                                         return true;
                                                      }
                                                   }
                                                case SINGLE:
                                                   reward = _plugin.get_rewards().getRewardData()[rewardID];
                                                   if (reward.occurs == RewardData.Occurs.VOTE_S) {
                                                      sender.sendMessage(ChatColor.RED + "This voting reward is already a single instance.");
                                                      return true;
                                                   }

                                                   if (reward.occurs == RewardData.Occurs.VOTE_P) {
                                                      _plugin.get_rewards().setRecurring(RewardData.Occurs.VOTE_S, rewardID, reward.time, reward.count);
                                                   } else {
                                                      _plugin.get_rewards().setRecurring(RewardData.Occurs.SINGLE, rewardID, reward.time, -1);
                                                   }

                                                   sender.sendMessage("Single instance for Reward# " + (rewardID + 1) + " successfully set.");
                                                   _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                   return true;
                                                case SHOP:
                                                   if (args.length < 5) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                      return true;
                                                   } else if (!args[4].matches("[+]?\\d+(\\/\\d+)?")) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, Keyword, args[4]);
                                                      return true;
                                                   } else {
                                                      _plugin.get_rewards().getRewardData()[rewardID].count = Integer.parseInt(args[4]);
                                                      _plugin.get_rewards().getRewardData()[rewardID].time = 0L;
                                                      keyword2 = this.checkKeyword(args[3]);
                                                      switch (keyword2) {
                                                         case ECON:
                                                            _plugin.get_rewards().getRewardData()[rewardID].reference = RewardData.EventReference.SHOP_ECON;
                                                            sender.sendMessage("'Point Shop' designation for Reward# " + (rewardID + 1) + " successfully set. " + args[4] + " econ units are required to purchase this reward.");
                                                            break;
                                                         case POINTS:
                                                            if (!_plugin.get_points().pointsEnabled(sender)) {
                                                               return true;
                                                            }

                                                            _plugin.get_rewards().getRewardData()[rewardID].reference = RewardData.EventReference.SHOP_POINTS;
                                                            sender.sendMessage("'Point Shop' designation for Reward# " + (rewardID + 1) + " successfully set. " + args[4] + " points are required to purchase this reward.");
                                                            break;
                                                         default:
                                                            this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, keyword2.toString());
                                                            return true;
                                                      }

                                                      _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                      RewardUtilities.saveRewards(Rewards.rewardFile);
                                                      return true;
                                                   }
                                                case TOP:
                                                   if (args.length < 6) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                                                      return true;
                                                   } else if (scope == null) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[4]);
                                                      return true;
                                                   } else if (reference == null) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                                                      return true;
                                                   } else if (reference == RewardData.EventReference.POINTS && !_plugin.get_points().pointsEnabled(sender)) {
                                                      return true;
                                                   } else {
                                                      if ((reference != RewardData.EventReference.POINTS || scope != RewardData.timeScope.TOTAL || !OnTime.totalTopPointReward.equalsIgnoreCase("disable")) && (reference != RewardData.EventReference.REFER || scope != RewardData.timeScope.TOTAL || !OnTime.totalTopReferReward.equalsIgnoreCase("disable")) && (reference != RewardData.EventReference.VOTES || scope != RewardData.timeScope.TOTAL || !OnTime.totalTopVoteReward.equalsIgnoreCase("disable"))) {
                                                         if (reference == RewardData.EventReference.POINTS && scope != RewardData.timeScope.TOTAL) {
                                                            sender.sendMessage(ChatColor.RED + "Only 'Total' Points is supported for Top Player rewards.");
                                                            return true;
                                                         }

                                                         if (!args[5].matches("[+]?\\d+(\\/\\d+)?")) {
                                                            this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, Keyword, args[5]);
                                                            return true;
                                                         }

                                                         if (Integer.parseInt(args[5]) <= 0) {
                                                            sender.sendMessage(ChatColor.RED + "The 'start place' must be greater than zero.");
                                                            return true;
                                                         }

                                                         if (args.length == 7) {
                                                            if (!args[6].matches("[+]?\\d+(\\/\\d+)?")) {
                                                               this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, Keyword, args[6]);
                                                               return true;
                                                            }

                                                            if (Integer.parseInt(args[6]) < Integer.parseInt(args[5])) {
                                                               sender.sendMessage(ChatColor.RED + "The 'end place' must be greater than or equal to the 'start place'");
                                                               return true;
                                                            }

                                                            _plugin.get_rewards().setTop(rewardID, scope, reference, Integer.valueOf(args[5]), Integer.valueOf(args[6]));
                                                         } else {
                                                            _plugin.get_rewards().setTop(rewardID, scope, reference, Integer.valueOf(args[5]), Integer.valueOf(args[5]));
                                                         }

                                                         sender.sendMessage("'Top' reward sucessfully set.");
                                                         _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                         return true;
                                                      }

                                                      sender.sendMessage(ChatColor.RED + "Total Top Rewards for " + reference.toString() + " is currently set to 'disable' in OnTime/config.yml. Request Failed.");
                                                      return true;
                                                   }
                                                case VOTIFIER:
                                                   if (_plugin.getServer().getPluginManager().getPlugin("Votifier") == null) {
                                                      sender.sendMessage(ChatColor.RED + "Votifier plugin is not installed on this server.  Command failed.");
                                                      return true;
                                                   } else if (args.length < 4) {
                                                      _plugin.get_rewards().setRecurring(RewardData.Occurs.VOTE_S, rewardID, 0L, -1);
                                                      sender.sendMessage("Votifier flag for Reward# " + args[2] + " successfully set.");
                                                      return true;
                                                   } else if (count < 1) {
                                                      this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, Keyword, args[3]);
                                                      return true;
                                                   } else if (OnTime.dataStorage != DataIO.datastorage.MYSQL) {
                                                      sender.sendMessage(ChatColor.RED + "OnTime is not configured to support a votifier count. MySQL storage required.");
                                                      return true;
                                                   } else {
                                                      RewardData.Occurs occurs = RewardData.Occurs.VOTE_P;
                                                      if (args.length > 4) {
                                                         keyword4 = this.checkKeyword(args[4]);
                                                         if (keyword4 == RewardCommands.KEYWORDS.SINGLE) {
                                                            occurs = RewardData.Occurs.VOTE_S;
                                                         } else {
                                                            if (keyword4 != RewardCommands.KEYWORDS.PERP) {
                                                               this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[4]);
                                                               return true;
                                                            }

                                                            occurs = RewardData.Occurs.VOTE_P;
                                                         }
                                                      }

                                                      _plugin.get_rewards().setRecurring(occurs, rewardID, 0L, count);
                                                      sender.sendMessage("Votifier flag for Reward# " + (rewardID + 1) + " with count = " + count + " successfully set.");
                                                      _plugin.get_rewards().displayInfo(sender, rewardID, "");
                                                      return true;
                                                   }
                                             }
                                       }
                                 }
                           }
                     }
                  }
               } else {
                  if (Keyword != RewardCommands.KEYWORDS.PURCHASE) {
                     this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  } else {
                     sender.sendMessage(ChatColor.RED + "/ontime rewards purchase <rewardTag>");
                  }

                  return true;
               }
            }
         }
      }
   }

   private boolean addReward(CommandSender sender, String[] args, int RewardID, String idTag, long time, RewardData.timeScope scope, RewardData.EventReference reference, String msgTag, String world) {
      if (args.length < 3) {
         this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, RewardCommands.KEYWORDS.ADD, (String)null);
         return true;
      } else {
         if (time < 0L) {
            time = 0L;
         }

         KEYWORDS rewardType = this.checkKeyword(args[2]);
         if (rewardType == null) {
            this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, args[2]);
            return true;
         } else if (args.length < 4) {
            this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, rewardType, (String)null);
            return true;
         } else {
            if (reference == RewardData.EventReference.ABSENCE) {
               switch (rewardType) {
                  case ITEM:
                  case KIT:
                  case XP:
                     sender.sendMessage("'absence' designation for not supported for " + rewardType.toString() + " rewards.");
                     return true;
                  default:
                     if (TimeUnit.MILLISECONDS.toDays(time) == 0L) {
                        sender.sendMessage(ChatColor.RED + " time must be at least one day for an absence reward.");
                        return true;
                     }

                     time = TimeUnit.DAYS.toMillis(TimeUnit.MILLISECONDS.toDays(time));
               }
            }

            if (rewardType != RewardCommands.KEYWORDS.ADDGROUP && rewardType != RewardCommands.KEYWORDS.REMOVEGROUP) {
               if (rewardType == RewardCommands.KEYWORDS.ECON) {
                  if (OnTime.economy == null) {
                     sender.sendMessage(ChatColor.RED + "No economy currently enabled.  Econ reward cannot be created.");
                     return true;
                  } else if (!args[3].matches("[-+]?\\d+(\\/\\d+)?")) {
                     this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, args[3]);
                     return true;
                  } else {
                     _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.ECONOMY, 1, args[3], idTag, scope, reference, msgTag, world);
                     return true;
                  }
               } else if (rewardType == RewardCommands.KEYWORDS.ITEM) {
                  if (args.length < 5) {
                     this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, rewardType, (String)null);
                     return true;
                  } else if (!args[3].matches("[+]?\\d+(\\/\\d+)?")) {
                     this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, args[3]);
                     return true;
                  } else {
                     int quantity = Integer.parseInt(args[3]);
                     String[] tokens = args[4].split("[+:]");
                     Material material = Material.matchMaterial(tokens[0]);
                     if (material == null) {
                        sender.sendMessage(ChatColor.RED + "Item " + args[4] + " not recognized.");
                        return true;
                     } else {
                        String materialString = material.name().toLowerCase();
                        if (tokens.length > 1) {
                           if (args[4].contains("+")) {
                              ItemStack itemstack = new ItemStack(material, quantity);

                              for(int i = 1; i < tokens.length; ++i) {
                                 if (!tokens[i].matches("[+]?\\d+(\\/\\d+)?")) {
                                    this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, tokens[i]);
                                    return true;
                                 }

                                 Enchantment enchantment = Enchantment.getById(Integer.parseInt(tokens[i]));
                                 if (enchantment == null) {
                                    this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, tokens[i]);
                                    return true;
                                 }

                                 if (!enchantment.canEnchantItem(itemstack)) {
                                    sender.sendMessage(ChatColor.RED + material.name() + " cannot be enchanted with " + enchantment.getName());
                                    return true;
                                 }

                                 materialString = materialString.concat("+" + tokens[i]);
                              }
                           } else {
                              if (!tokens[1].matches("[+]?\\d+(\\/\\d+)?")) {
                                 this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, tokens[1]);
                                 return true;
                              }

                              materialString = materialString.concat(":" + tokens[1]);
                           }
                        }

                        _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.ITEM, quantity, materialString, idTag, scope, reference, msgTag, world);
                        return true;
                     }
                  }
               } else if (rewardType == RewardCommands.KEYWORDS.KIT) {
                  boolean foundIt = false;
                  Iterator var14 = _plugin.get_rewards().kits.iterator();

                  while(var14.hasNext()) {
                     String k = (String)var14.next();
                     sender.sendMessage("looking at kit: " + k);
                     if (k.contains(args[3])) {
                        foundIt = true;
                     }
                  }

                  if (!foundIt) {
                     sender.sendMessage(ChatColor.RED + "No kit found with name of " + args[3] + ".");
                     return true;
                  } else {
                     _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.KIT, 0, args[3], idTag, scope, reference, msgTag, world);
                     return true;
                  }
               } else if (rewardType == RewardCommands.KEYWORDS.GROUP) {
                  sender.sendMessage(ChatColor.RED + "The 'group' keyword has been replaced with 'promotion'");
                  this.syntaxError(sender, RewardCommands.SYNTAXERROR.UNKNOWNWORD, rewardType, "group");
                  return true;
               } else if (rewardType != RewardCommands.KEYWORDS.PROMOTION && rewardType != RewardCommands.KEYWORDS.DEMOTION) {
                  if (rewardType == RewardCommands.KEYWORDS.MESSAGE) {
                     if (!OnTime.messagesEnable) {
                        sender.sendMessage(ChatColor.RED + "OnTime Messaging s is not currently enabled on the server.");
                        return true;
                     } else if (msgTag == null) {
                        this.syntaxError(sender, RewardCommands.SYNTAXERROR.TOOFEW, rewardType, (String)null);
                        return true;
                     } else {
                        _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.MESSAGE, 1, msgTag, idTag, scope, reference, msgTag, world);
                        return true;
                     }
                  } else if (rewardType != RewardCommands.KEYWORDS.PERMISSION && rewardType != RewardCommands.KEYWORDS.ADDPERM) {
                     if (rewardType != RewardCommands.KEYWORDS.DENIAL && rewardType != RewardCommands.KEYWORDS.REMOVEPERM) {
                        if (rewardType != RewardCommands.KEYWORDS.COMMAND && rewardType != RewardCommands.KEYWORDS.CMD) {
                           if (rewardType == RewardCommands.KEYWORDS.XP) {
                              if (!args[3].matches("[-+]?\\d+(\\/\\d+)?")) {
                                 this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, args[3]);
                                 return true;
                              } else {
                                 _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.XP, Integer.parseInt(args[3]), "XP", idTag, scope, reference, msgTag, world);
                                 return true;
                              }
                           } else if (rewardType == RewardCommands.KEYWORDS.POINTS) {
                              if (_plugin.get_points().pointsEnabled(sender)) {
                                 if (!args[3].matches("[-+]?\\d+(\\/\\d+)?")) {
                                    this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, args[3]);
                                    return true;
                                 } else {
                                    _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.POINTS, Integer.parseInt(args[3]), "points", idTag, scope, reference, msgTag, world);
                                    return true;
                                 }
                              } else {
                                 return true;
                              }
                           } else {
                              this.syntaxError(sender, RewardCommands.SYNTAXERROR.INVALIDDATA, rewardType, args[2]);
                              return true;
                           }
                        } else if (_plugin.get_rewards().getCommand(args[3]) == null) {
                           sender.sendMessage(ChatColor.RED + "No command '" + args[3] + "' defined.");
                           return true;
                        } else {
                           _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.COMMAND, 0, args[3], idTag, scope, reference, msgTag, world);
                           return true;
                        }
                     } else if (!OnTime.permission.isEnabled()) {
                        sender.sendMessage(ChatColor.RED + "Permissions plugin is not currently enabled on the server.");
                        return true;
                     } else {
                        _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.DENIAL, 1, args[3], idTag, scope, reference, msgTag, world);
                        return true;
                     }
                  } else if (!OnTime.permission.isEnabled()) {
                     sender.sendMessage(ChatColor.RED + "Permissions plugin is not currently enabled on the server.");
                     return true;
                  } else {
                     _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.PERMISSION, 1, args[3], idTag, scope, reference, msgTag, world);
                     return true;
                  }
               } else if (!OnTime.permission.isEnabled()) {
                  sender.sendMessage(ChatColor.RED + "Permissions plugin not enbled. 'Promotion/Demotion' rewards cannot be added.");
                  return true;
               } else if (_plugin.get_rewards().findGroup(args[3]) == 0) {
                  this.syntaxError(sender, RewardCommands.SYNTAXERROR.SPECIAL, rewardType, "Group " + args[3] + " is not listed in OnTime/rewards.yml");
                  return true;
               } else if (!_plugin.get_rewards().isValidGroup(args[3])) {
                  sender.sendMessage(ChatColor.RED + "Group " + args[3] + " not defined in " + OnTime.permission.getName());
                  return true;
               } else {
                  if (rewardType == RewardCommands.KEYWORDS.PROMOTION) {
                     _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.PROMOTION, 1, args[3], idTag, scope, reference, msgTag, world);
                  } else {
                     _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.DEMOTION, 1, args[3], idTag, scope, reference, msgTag, world);
                  }

                  return true;
               }
            } else if (!OnTime.permission.isEnabled()) {
               sender.sendMessage(ChatColor.RED + "Permissions plugin not enbled. An 'addgroup/rmgroup' reward cannot be defined.");
               return true;
            } else if (_plugin.get_rewards().findGroup(args[3]) == 0) {
               this.syntaxError(sender, RewardCommands.SYNTAXERROR.SPECIAL, rewardType, "Group " + args[3] + " is not listed in OnTime/rewards.yml");
               return true;
            } else if (!_plugin.get_rewards().isValidGroup(args[3])) {
               sender.sendMessage(ChatColor.RED + "Group " + args[3] + " not defined in " + OnTime.permission.getName());
               return true;
            } else {
               if (rewardType == RewardCommands.KEYWORDS.ADDGROUP) {
                  _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.ADDGROUP, 1, args[3], idTag, scope, reference, msgTag, world);
               } else {
                  _plugin.get_rewards().add(sender, RewardData.Occurs.SINGLE, time, RewardData.RewardType.REMOVEGROUP, 1, args[3], idTag, scope, reference, msgTag, world);
               }

               return true;
            }
         }
      }
   }

   private void syntaxError(CommandSender sender, SYNTAXERROR error, KEYWORDS command, String data) {
      if (command != null) {
         label71:
         switch (command) {
            case ABSENCE:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards absence <RewardID/RewardTag>'");
               break;
            case ADDGROUP:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add addgroup <group name> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case ADDPERM:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add addperm <permission string> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case ADD:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add [addgroup/addperm/command/demotion/econ/item/kit/points/promotion/rmgroup/rmperm/xp] {parameters}");
               break;
            case COMMAND:
            case CMD:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add command <commandID> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               sender.sendMessage(ChatColor.RED + "Defined commands:");
               Iterator commands = _plugin.get_rewards().getCommands().iterator();
               if (!commands.hasNext()) {
                  sender.sendMessage(ChatColor.RED + "   No Commands defined in rewards.yml");
               }

               while(true) {
                  if (!commands.hasNext()) {
                     break label71;
                  }

                  sender.sendMessage(ChatColor.RED + "  " + (String)commands.next());
               }
            case DAILY:
            case EVENT:
            case GROUPREMOVE:
            case MSG:
            case MONTHLY:
            case PLAYTIME:
            case SPLICE:
            case TAG:
            case TIME:
            case TOTAL:
            case WEEKLY:
            case WORLD:
            default:
               sender.sendMessage(ChatColor.RED + "Syntax: Unknown Keyword: " + command.toString());
               break;
            case DAYSON:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards dayson <RewardID/RewardTag> <dd>D");
               break;
            case DEATH:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards death <RewardID/RewardTag>'");
               break;
            case DELETE:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards delete <playerName>'");
               break;
            case DENIAL:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add denial <permission string> {<scope>} {<dd>D <hh>H <mm>M}' {<event>} {[tag=]<RewardTag>} {[world=]<world>}");
               break;
            case DEMOTION:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add demotion <group name> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case ECON:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add econ <amount> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case EDIT:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards edit <RewardID/RewardTag> {[TAG/TIME/WORLD] {<tag>}/{<world>}/{<dd>D <hh>H <mm>M}} {msg=<messageID>}'");
               break;
            case EXCLUSIVE:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards exclusive <RewardID/RewardTag> [ADD/REMOVE] {<group list>}' ");
               break;
            case GENERAL:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards general <RewardID/RewardTag>'");
               break;
            case GROUP:
            case PROMOTION:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add promotion <group name> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case INDI:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards indi <RewardID/RewardTag>'");
               break;
            case INFO:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards info [ALL]/<RewardID/RewardTag>'");
               break;
            case ITEM:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add item <quantity> <item> {[+]<modifier>}{[:]<code>} {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               sender.sendMessage(ChatColor.RED + "  Use '/ontime help enchant {[armor/sword/tool/bow]} for enchantment modifiers");
               sender.sendMessage(ChatColor.RED + "  Use '/ontime help {item} for item codes");
               sender.sendMessage(ChatColor.RED + "  Use '/ontime help potion {potion keyword} for potion codes");
               break;
            case KIT:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add kit <kit name> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case LINK:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards link <RewardID/RewardTag> <linked rewardID>");
               break;
            case LIST:
            case L:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards list {#}'");
               break;
            case NEXT:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards next'");
               break;
            case MESSAGE:
               sender.sendMessage(ChatColor.RED + "Sytax: '/ontime rewards add message msg=<msgtag> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}");
               break;
            case PERMISSION:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add permission <permission string> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case PERP:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards perp <RewardID/RewardTag> {<dd>D <hh>H <mm>M}'");
               break;
            case POINTS:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add points <amount> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case PURCHASE:
               if (OnTime.permission.has(sender, "ontime.rewards.admin")) {
                  sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards purchase <RewardID/RewardTag> <playerName>'");
               } else {
                  sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards purchase <ShopTag>'");
               }
               break;
            case PURGE:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards purge <RewardID/RewardTag>'");
               break;
            case REFER:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards refer <RewardID/RewardTag> {[source/target]} {[count=]<count>}'");
               break;
            case RECUR:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards recur <RewardID/RewardTag> {<dd>D <hh>H <mm>M} {[count=]<count>}'");
               break;
            case REMOVEGROUP:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add rmgroup <group name> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case REMOVEPERM:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add rmperm <permission string> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case REMOVE:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards remove <RewardID/RewardTag>'");
               break;
            case SET:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards set <RewardID/RewardTag> [real/delta/play/login] <playerName> {<dd>D <hh>H <mm>M} '");
               break;
            case SINGLE:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards single <RewardID/RewardTag>'");
               break;
            case SHOP:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards shop <RewardID/RewardTag> [points/econ] <#>'");
               break;
            case TOP:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards top <RewardID/RewardTag> [play/points/refer/vote] [daily/weekly/monthly/total] <start place> {<end place>}'");
               break;
            case VOTIFIER:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards votifier <RewardID/RewardTag> {[count=]<count>} {[perpetual/single]}'");
               break;
            case XP:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards add xp <amount> {<scope>} {<dd>D <hh>H <mm>M} {<event>} {[tag=]<RewardTag>} {[world=]<world>}'");
               break;
            case NA:
               sender.sendMessage(ChatColor.RED + "Syntax: '/ontime rewards [absence/add/chain/dayson/death/delete/edit/exclusive/gen/indi/info/list/next/perp/purc/refer/recur/rem/set/single/top/votiifer]'");
         }
      }

      switch (error) {
         case TOOFEW:
         default:
            break;
         case MISSINGTIME:
            sender.sendMessage(ChatColor.RED + "Time must be specified with any combination of: <dd>D <hh>H <mm>M");
            break;
         case UNKNOWNWORD:
            sender.sendMessage(ChatColor.RED + "Sytnax error at '" + data + "'");
            break;
         case INVALIDDATA:
            sender.sendMessage(ChatColor.RED + "Invalid data provided: '" + data + "'");
            break;
         case SPECIAL:
            sender.sendMessage(ChatColor.RED + data);
      }

   }

   public int checkRewardID(CommandSender sender, String rewardID) {
      int lookup = true;
      if (!rewardID.matches("[+]?\\d+(\\/\\d+)?")) {
         int lookup;
         if ((lookup = _plugin.get_rewards()._validateIdentifier(rewardID, -1)) < 0) {
            if (sender != null) {
               sender.sendMessage(ChatColor.RED + "Invalid reward tag ( " + rewardID + " ) specified.");
            }

            return -1;
         } else {
            return lookup;
         }
      } else if (_plugin.get_rewards().getNumDefinedRewards() == 0) {
         if (sender != null) {
            sender.sendMessage(ChatColor.RED + "No rewards are currently defined.");
         }

         return -1;
      } else if (Integer.parseInt(rewardID) > 0 && Integer.parseInt(rewardID) <= _plugin.get_rewards().getNumDefinedRewards()) {
         return Integer.parseInt(rewardID) - 1;
      } else {
         if (sender != null) {
            sender.sendMessage(ChatColor.RED + rewardID + " is not a valid Rewards ID.  Must be between 1 and " + _plugin.get_rewards().getNumDefinedRewards());
         }

         return -1;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardCommands$KEYWORDS() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardCommands$KEYWORDS;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[RewardCommands.KEYWORDS.values().length];

         try {
            var0[RewardCommands.KEYWORDS.ABSENCE.ordinal()] = 1;
         } catch (NoSuchFieldError var55) {
         }

         try {
            var0[RewardCommands.KEYWORDS.ADD.ordinal()] = 4;
         } catch (NoSuchFieldError var54) {
         }

         try {
            var0[RewardCommands.KEYWORDS.ADDGROUP.ordinal()] = 2;
         } catch (NoSuchFieldError var53) {
         }

         try {
            var0[RewardCommands.KEYWORDS.ADDPERM.ordinal()] = 3;
         } catch (NoSuchFieldError var52) {
         }

         try {
            var0[RewardCommands.KEYWORDS.CMD.ordinal()] = 6;
         } catch (NoSuchFieldError var51) {
         }

         try {
            var0[RewardCommands.KEYWORDS.COMMAND.ordinal()] = 5;
         } catch (NoSuchFieldError var50) {
         }

         try {
            var0[RewardCommands.KEYWORDS.DAILY.ordinal()] = 7;
         } catch (NoSuchFieldError var49) {
         }

         try {
            var0[RewardCommands.KEYWORDS.DAYSON.ordinal()] = 8;
         } catch (NoSuchFieldError var48) {
         }

         try {
            var0[RewardCommands.KEYWORDS.DEATH.ordinal()] = 9;
         } catch (NoSuchFieldError var47) {
         }

         try {
            var0[RewardCommands.KEYWORDS.DELETE.ordinal()] = 10;
         } catch (NoSuchFieldError var46) {
         }

         try {
            var0[RewardCommands.KEYWORDS.DEMOTION.ordinal()] = 12;
         } catch (NoSuchFieldError var45) {
         }

         try {
            var0[RewardCommands.KEYWORDS.DENIAL.ordinal()] = 11;
         } catch (NoSuchFieldError var44) {
         }

         try {
            var0[RewardCommands.KEYWORDS.ECON.ordinal()] = 13;
         } catch (NoSuchFieldError var43) {
         }

         try {
            var0[RewardCommands.KEYWORDS.EDIT.ordinal()] = 14;
         } catch (NoSuchFieldError var42) {
         }

         try {
            var0[RewardCommands.KEYWORDS.EVENT.ordinal()] = 15;
         } catch (NoSuchFieldError var41) {
         }

         try {
            var0[RewardCommands.KEYWORDS.EXCLUSIVE.ordinal()] = 16;
         } catch (NoSuchFieldError var40) {
         }

         try {
            var0[RewardCommands.KEYWORDS.GENERAL.ordinal()] = 17;
         } catch (NoSuchFieldError var39) {
         }

         try {
            var0[RewardCommands.KEYWORDS.GROUP.ordinal()] = 18;
         } catch (NoSuchFieldError var38) {
         }

         try {
            var0[RewardCommands.KEYWORDS.GROUPREMOVE.ordinal()] = 19;
         } catch (NoSuchFieldError var37) {
         }

         try {
            var0[RewardCommands.KEYWORDS.INDI.ordinal()] = 20;
         } catch (NoSuchFieldError var36) {
         }

         try {
            var0[RewardCommands.KEYWORDS.INFO.ordinal()] = 21;
         } catch (NoSuchFieldError var35) {
         }

         try {
            var0[RewardCommands.KEYWORDS.ITEM.ordinal()] = 22;
         } catch (NoSuchFieldError var34) {
         }

         try {
            var0[RewardCommands.KEYWORDS.KIT.ordinal()] = 23;
         } catch (NoSuchFieldError var33) {
         }

         try {
            var0[RewardCommands.KEYWORDS.L.ordinal()] = 26;
         } catch (NoSuchFieldError var32) {
         }

         try {
            var0[RewardCommands.KEYWORDS.LINK.ordinal()] = 24;
         } catch (NoSuchFieldError var31) {
         }

         try {
            var0[RewardCommands.KEYWORDS.LIST.ordinal()] = 25;
         } catch (NoSuchFieldError var30) {
         }

         try {
            var0[RewardCommands.KEYWORDS.MESSAGE.ordinal()] = 28;
         } catch (NoSuchFieldError var29) {
         }

         try {
            var0[RewardCommands.KEYWORDS.MONTHLY.ordinal()] = 30;
         } catch (NoSuchFieldError var28) {
         }

         try {
            var0[RewardCommands.KEYWORDS.MSG.ordinal()] = 29;
         } catch (NoSuchFieldError var27) {
         }

         try {
            var0[RewardCommands.KEYWORDS.NA.ordinal()] = 55;
         } catch (NoSuchFieldError var26) {
         }

         try {
            var0[RewardCommands.KEYWORDS.NEXT.ordinal()] = 27;
         } catch (NoSuchFieldError var25) {
         }

         try {
            var0[RewardCommands.KEYWORDS.PERMISSION.ordinal()] = 31;
         } catch (NoSuchFieldError var24) {
         }

         try {
            var0[RewardCommands.KEYWORDS.PERP.ordinal()] = 32;
         } catch (NoSuchFieldError var23) {
         }

         try {
            var0[RewardCommands.KEYWORDS.PLAYTIME.ordinal()] = 33;
         } catch (NoSuchFieldError var22) {
         }

         try {
            var0[RewardCommands.KEYWORDS.POINTS.ordinal()] = 34;
         } catch (NoSuchFieldError var21) {
         }

         try {
            var0[RewardCommands.KEYWORDS.PROMOTION.ordinal()] = 35;
         } catch (NoSuchFieldError var20) {
         }

         try {
            var0[RewardCommands.KEYWORDS.PURCHASE.ordinal()] = 36;
         } catch (NoSuchFieldError var19) {
         }

         try {
            var0[RewardCommands.KEYWORDS.PURGE.ordinal()] = 37;
         } catch (NoSuchFieldError var18) {
         }

         try {
            var0[RewardCommands.KEYWORDS.RECUR.ordinal()] = 39;
         } catch (NoSuchFieldError var17) {
         }

         try {
            var0[RewardCommands.KEYWORDS.REFER.ordinal()] = 38;
         } catch (NoSuchFieldError var16) {
         }

         try {
            var0[RewardCommands.KEYWORDS.REMOVE.ordinal()] = 42;
         } catch (NoSuchFieldError var15) {
         }

         try {
            var0[RewardCommands.KEYWORDS.REMOVEGROUP.ordinal()] = 40;
         } catch (NoSuchFieldError var14) {
         }

         try {
            var0[RewardCommands.KEYWORDS.REMOVEPERM.ordinal()] = 41;
         } catch (NoSuchFieldError var13) {
         }

         try {
            var0[RewardCommands.KEYWORDS.SET.ordinal()] = 43;
         } catch (NoSuchFieldError var12) {
         }

         try {
            var0[RewardCommands.KEYWORDS.SHOP.ordinal()] = 46;
         } catch (NoSuchFieldError var11) {
         }

         try {
            var0[RewardCommands.KEYWORDS.SINGLE.ordinal()] = 44;
         } catch (NoSuchFieldError var10) {
         }

         try {
            var0[RewardCommands.KEYWORDS.SPLICE.ordinal()] = 45;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[RewardCommands.KEYWORDS.TAG.ordinal()] = 47;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[RewardCommands.KEYWORDS.TIME.ordinal()] = 48;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[RewardCommands.KEYWORDS.TOP.ordinal()] = 49;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[RewardCommands.KEYWORDS.TOTAL.ordinal()] = 50;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[RewardCommands.KEYWORDS.VOTIFIER.ordinal()] = 51;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[RewardCommands.KEYWORDS.WEEKLY.ordinal()] = 52;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[RewardCommands.KEYWORDS.WORLD.ordinal()] = 53;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[RewardCommands.KEYWORDS.XP.ordinal()] = 54;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardCommands$KEYWORDS = var0;
         return var0;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$RewardType() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$RewardType;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[RewardData.RewardType.values().length];

         try {
            var0[RewardData.RewardType.ADDGROUP.ordinal()] = 1;
         } catch (NoSuchFieldError var14) {
         }

         try {
            var0[RewardData.RewardType.COMMAND.ordinal()] = 2;
         } catch (NoSuchFieldError var13) {
         }

         try {
            var0[RewardData.RewardType.DELAY.ordinal()] = 3;
         } catch (NoSuchFieldError var12) {
         }

         try {
            var0[RewardData.RewardType.DEMOTION.ordinal()] = 4;
         } catch (NoSuchFieldError var11) {
         }

         try {
            var0[RewardData.RewardType.DENIAL.ordinal()] = 5;
         } catch (NoSuchFieldError var10) {
         }

         try {
            var0[RewardData.RewardType.ECONOMY.ordinal()] = 6;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[RewardData.RewardType.ITEM.ordinal()] = 7;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[RewardData.RewardType.KIT.ordinal()] = 8;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[RewardData.RewardType.MESSAGE.ordinal()] = 9;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[RewardData.RewardType.PERMISSION.ordinal()] = 10;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[RewardData.RewardType.POINTS.ordinal()] = 11;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[RewardData.RewardType.PROMOTION.ordinal()] = 12;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[RewardData.RewardType.REMOVEGROUP.ordinal()] = 13;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[RewardData.RewardType.XP.ordinal()] = 14;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$RewardType = var0;
         return var0;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardCommands$SYNTAXERROR() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardCommands$SYNTAXERROR;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[RewardCommands.SYNTAXERROR.values().length];

         try {
            var0[RewardCommands.SYNTAXERROR.INVALIDDATA.ordinal()] = 4;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[RewardCommands.SYNTAXERROR.MISSINGTIME.ordinal()] = 2;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[RewardCommands.SYNTAXERROR.SPECIAL.ordinal()] = 5;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[RewardCommands.SYNTAXERROR.TOOFEW.ordinal()] = 1;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[RewardCommands.SYNTAXERROR.UNKNOWNWORD.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardCommands$SYNTAXERROR = var0;
         return var0;
      }
   }

   protected static enum KEYWORDS {
      ABSENCE("ABS"),
      ADDGROUP("ADDG"),
      ADDPERM("ADDP"),
      ADD("ADD"),
      COMMAND("COM"),
      CMD("CMD"),
      DAILY("DAI"),
      DAYSON("DAYS"),
      DEATH("DEA"),
      DELETE("DEL"),
      DENIAL("DEN"),
      DEMOTION("DEM"),
      ECON("ECON"),
      EDIT("EDIT"),
      EVENT("EVE"),
      EXCLUSIVE("EXC"),
      GENERAL("GEN"),
      GROUP("GRO"),
      GROUPREMOVE("GROUPR"),
      INDI("INDI"),
      INFO("INFO"),
      ITEM("ITEM"),
      KIT("KIT"),
      LINK("LINK"),
      LIST("LIST"),
      L("L"),
      NEXT("NEXT"),
      MESSAGE("MES"),
      MSG("MSG"),
      MONTHLY("MON"),
      PERMISSION("PERM"),
      PERP("PERP"),
      PLAYTIME("PLA"),
      POINTS("POIN"),
      PROMOTION("PRO"),
      PURCHASE("PURC"),
      PURGE("PURG"),
      REFER("REF"),
      RECUR("REC"),
      REMOVEGROUP("RMG"),
      REMOVEPERM("RMP"),
      REMOVE("REM"),
      SET("SET"),
      SINGLE("SIN"),
      SPLICE("SPL"),
      SHOP("SHO"),
      TAG("TAG"),
      TIME("TIME"),
      TOP("TOP"),
      TOTAL("TOT"),
      VOTIFIER("VOT"),
      WEEKLY("WEE"),
      WORLD("WOR"),
      XP("XP"),
      NA("N/A");

      private final String input;

      private KEYWORDS(String input) {
         this.input = input;
      }

      public String input() {
         return this.input;
      }
   }

   public static enum RewardOrPenalty {
      REWARD,
      PENALTY;
   }

   protected static enum SYNTAXERROR {
      TOOFEW,
      MISSINGTIME,
      UNKNOWNWORD,
      INVALIDDATA,
      SPECIAL;
   }
}
