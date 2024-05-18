package org.ontime.ontime;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.Rewards.ReferredCommands;
import me.edge209.OnTime.Rewards.RewardCommands;
import me.edge209.OnTime.Rewards.RewardData;
import me.edge209.OnTime.Rewards.Rewards;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands {
   private static OnTime _plugin;
   private static RewardCommands _rewardCommand;
   private static ReferredCommands _referredCommand;
   int suspendTaskID = -1;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Commands$KEYWORDS;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Commands$SYNTAXERROR;

   public Commands(OnTime plugin) {
      _plugin = plugin;
      _rewardCommand = new RewardCommands(_plugin);
      _referredCommand = new ReferredCommands(_plugin);
   }

   public KEYWORDS checkKeyword(String word) {
      KEYWORDS[] var5;
      int var4 = (var5 = Commands.KEYWORDS.values()).length;

      for(int var3 = 0; var3 < var4; ++var3) {
         KEYWORDS s1 = var5[var3];
         if (word.length() >= s1.input().length() && word.substring(0, s1.input().length()).equalsIgnoreCase(s1.input())) {
            return s1;
         }
      }

      return Commands.KEYWORDS.NA;
   }

   public boolean noPermission(CommandSender sender) {
      sender.sendMessage(ChatColor.RED + Output.OnTimeOutput.getString("output.noPermission"));
      return true;
   }

   public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
      long time = -1L;
      int messageStart = -1;
      KEYWORDS parm1 = null;
      KEYWORDS parm2 = null;
      KEYWORDS parm3 = null;
      String world = null;
      if (commandLabel.equalsIgnoreCase("ontime") || commandLabel.equalsIgnoreCase("ont")) {
         if (!OnTime.enableOnTime) {
            sender.sendMessage(ChatColor.RED + "OnTime Plugin is NOT Enabled.");
            return true;
         }

         Player player = null;
         if (sender instanceof Player) {
            player = (Player)sender;
         }

         if (args.length == 0) {
            if (player == null) {
               sender.sendMessage("This command without parameters cannot be executed by console.");
               return true;
            }

            if (!OnTime.permission.has(sender, "ontime.me")) {
               return this.noPermission(sender);
            }

            if (!Output.generate((String)"output.ontime-me", (CommandSender)sender, sender.getName(), (RewardData)null)) {
               sender.sendMessage(ChatColor.RED + "[OnTime] Unexpected Error with /ontime output: Please inform server admin.");
            }

            return true;
         }

         Iterator playerData;
         for(int i = 1; i < args.length; ++i) {
            if (args[i].length() > 1 && (args[i].startsWith("world=") || args[i].startsWith("w="))) {
               if (!OnTime.perWorldEnable) {
                  sender.sendMessage(ChatColor.RED + "per-world time tracking is not enabled on this server.");
                  return true;
               }

               world = args[i].substring(args[i].indexOf("=") + 1);
               if (world.length() == 0) {
                  sender.sendMessage(ChatColor.RED + "A world must be specified.");
                  return true;
               }

               boolean good = false;
               if (world.equalsIgnoreCase("all")) {
                  good = true;
               }

               List worlds = _plugin.getServer().getWorlds();
               playerData = worlds.iterator();

               while(playerData.hasNext() && !good) {
                  if (world.equalsIgnoreCase(((World)playerData.next()).getName())) {
                     good = true;
                     break;
                  }
               }

               if (!good) {
                  sender.sendMessage(ChatColor.RED + "World '" + world + "' is not defined on this server.");
                  return true;
               }
            }
         }

         if (Players.hasOnTimeRecord(args[0])) {
            return this.ontimeOther(sender, args);
         }

         KEYWORDS Keyword = this.checkKeyword(args[0]);
         if (Keyword == Commands.KEYWORDS.NA) {
            Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[0], (RewardData)null);
            String[] lookupName = new String[]{this.autoCompleteName(sender, args[0])};
            if (lookupName[0] != null) {
               return this.ontimeOther(sender, lookupName);
            }

            return true;
         }

         switch (Keyword) {
            case ADD:
            case MESSAGE:
            case SET:
            case SUBTRACT:
            case SUSPEND:
               int i = 1;

               while(true) {
                  if (i >= args.length) {
                     if (time >= 0L) {
                        time += 5L;
                     }
                     break;
                  }

                  if (args[i].length() > 1) {
                     if ((args[i].endsWith("D") || args[i].endsWith("d")) && args[i].substring(0, args[i].length() - 1).matches("[+]?\\d+(\\/\\d+)?")) {
                        time += TimeUnit.DAYS.toMillis((long)Integer.parseInt(args[i].substring(0, args[i].length() - 1)));
                        if (time == -1L) {
                           time = 0L;
                        }
                     } else if ((args[i].endsWith("H") || args[i].endsWith("h")) && args[i].substring(0, args[i].length() - 1).matches("[+]?\\d+(\\/\\d+)?")) {
                        time += TimeUnit.HOURS.toMillis((long)Integer.parseInt(args[i].substring(0, args[i].length() - 1)));
                        if (time == -1L) {
                           time = 0L;
                        }
                     } else if ((args[i].endsWith("M") || args[i].endsWith("m")) && args[i].substring(0, args[i].length() - 1).matches("[+]?\\d+(\\/\\d+)?")) {
                        time += TimeUnit.MINUTES.toMillis((long)Integer.parseInt(args[i].substring(0, args[i].length() - 1)));
                        if (time == -1L) {
                           time = 0L;
                        }
                     }

                     if (args[i].startsWith("msg=") || args[i].startsWith("MSG=")) {
                        messageStart = i;
                     }
                  }

                  ++i;
               }
         }

         int first;
         Import.UUIDFUNC playerID;
         String dbColumn;
         VotifierEvent worldTime;
         String playerName;
         PlayerData playerData;
         Player msgTag;
         String playerID;
         int i;
         label1144:
         switch (Keyword) {
            case ADD:
               if (!OnTime.permission.has(sender, "ontime.moduser")) {
                  return this.noPermission(sender);
               }

               if (args.length < 2) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               if (parm1 == Commands.KEYWORDS.PLAYER && args.length < 3 || parm1 != Commands.KEYWORDS.PLAYER && args.length < 4) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               playerName = args[2];
               playerData = null;
               if ((playerData = Players.getData(playerName)) != null) {
                  if (parm1 == Commands.KEYWORDS.PLAYER) {
                     sender.sendMessage(ChatColor.RED + playerName + " already exists in OnTime database.");
                     return true;
                  }
               } else if (parm1 != Commands.KEYWORDS.PLAYER) {
                  Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[2], (RewardData)null);
                  return true;
               }

               switch (parm1) {
                  case PLAYER:
                     long playTime = TimeUnit.MINUTES.toMillis((long)OnTime.purgeTimeMin) + 1L;
                     playerData = Players.getNew((UUID)null, playerName, 0L, Calendar.getInstance().getTimeInMillis(), "null", playTime);
                     Players.putData(playerName, playerData);
                     if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                        _plugin.get_dataio().savePlayerDataMySQL(playerData, false);
                     }

                     sender.sendMessage(playerName + " now has an OnTime record.");
                     return true;
                  case POINT:
                     if (_plugin.get_points().pointsEnabled(sender)) {
                        if (!args[3].matches("[+]?\\d+(\\/\\d+)?")) {
                           this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                           return true;
                        }

                        this.modifyPoints(sender, playerName, Integer.parseInt(args[3]));
                        return true;
                     }

                     return true;
                  case TOTAL:
                     if (time == -1L) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.MISSINGTIME, (KEYWORDS)null, (String)null);
                     } else {
                        this.modifyTime(sender, playerData, time, world);
                     }

                     return true;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
                     return true;
               }
            case BACKUP:
               if (!OnTime.permission.has(sender, "ontime.save")) {
                  return this.noPermission(sender);
               }

               if (_plugin.get_databackup().backup(OnTime.onTimeDataFolder)) {
                  sender.sendMessage("OnTime data backup successful.");
               } else {
                  sender.sendMessage("OnTime data backup failed.");
               }

               return true;
            case CONSOLE:
               if (!OnTime.permission.has(sender, "ontime.logfile.admin")) {
                  return this.noPermission(sender);
               }

               if (args.length < 3) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               switch (parm1) {
                  case DISABLE:
                     this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "The OnTime console display cannot be completely disabled.");
                     break;
                  case ENABLE:
                     if (this.badNumber(sender, args[2])) {
                        return true;
                     }

                     first = Integer.parseInt(args[2]);
                     if (first < 0 || first > 3) {
                        sender.sendMessage("Console detail level must be a value between 1 and 3, inclusive.");
                        return true;
                     }

                     OnTime.consoleLogLevel = first;
                     Updates.saveConfig(_plugin);
                     sender.sendMessage("OnTime console logging has been set to level " + first);
                     LogFile.write(4, "ONTIME console logging has been set to " + first + " by " + sender.getName());
                     break;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
               }

               return true;
            case EXPORT:
               if (!OnTime.permission.has(sender, "ontime.save")) {
                  return this.noPermission(sender);
               }

               if (args.length < 2) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               switch (parm1) {
                  case DAT:
                     this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "OnTime no longer supports the DAT storage option.");
                     return true;
                  case MYSQL:
                     first = -1;
                     int last = -1;
                     if (!OnTime.MySQL_enable) {
                        sender.sendMessage(ChatColor.RED + "MySQL is not enabled for OnTime.");
                        return true;
                     }

                     if (args.length > 2) {
                        if (this.badNumber(sender, args[2])) {
                           return true;
                        }

                        first = Integer.parseInt(args[2]);
                        if (args.length != 4) {
                           this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                           return true;
                        }

                        if (this.badNumber(sender, args[3])) {
                           return true;
                        }

                        last = Integer.parseInt(args[3]);
                        if (first >= last) {
                           this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "The 'start' must come before the 'last'.");
                           return true;
                        }
                     }

                     if (!DataIO.mysqlNew.checkMySQLConnection()) {
                        sender.sendMessage(ChatColor.RED + "MySQL Error.  Database access failed.");
                        return true;
                     }

                     if (_plugin.get_dataio().createOnTimePlayersTable(OnTime.MySQL_table)) {
                        _plugin.get_dataio().setOntimeDataMySQL();
                        LogFile.console(1, "Created " + OnTime.MySQL_table + " table in MySQL Database.");
                     }

                     _plugin.get_dataio().saveAllPlayerDataMySQL(sender, DataIO.mysqlsave.ALL, first, last, "Player data successfully exported to MySQL.");
                     return true;
                  case YML:
                     if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                        _plugin.get_dataio().loadAllPlayerDataMySQL(DataIO.mysqlload.ALL);
                     }

                     _plugin.get_dataio().savePlayerDataYML(OnTime.onTimeDataFolder, "playerdata.yml");
                     sender.sendMessage("Player data successfully exported to 'playerdata.yml'");
                     return true;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
                     return true;
               }
            case HELP:
               KEYWORDS keyword = Commands.KEYWORDS.ALL;
               if (args.length > 1) {
                  keyword = this.checkKeyword(args[1]);
                  if (keyword == Commands.KEYWORDS.NA && Help.outputHelp(sender, args[1])) {
                     return true;
                  }
               }

               if (args.length > 1) {
                  keyword = this.checkKeyword(args[1]);
               }

               if (keyword == Commands.KEYWORDS.ALL && OnTime.permission.has(sender, "ontime.me")) {
                  sender.sendMessage(ChatColor.GREEN + "/ontime");
                  sender.sendMessage("       see your OnTime data");
               }

               if (keyword == Commands.KEYWORDS.ALL && OnTime.permission.has(sender, "ontime.other")) {
                  sender.sendMessage(ChatColor.GREEN + "/ontime <playerName>");
                  sender.sendMessage("       see another's OnTime data");
               }

               if ((keyword == Commands.KEYWORDS.ALL || keyword == Commands.KEYWORDS.ONLINE) && OnTime.permission.has(sender, "ontime.online")) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.HELP, Commands.KEYWORDS.ONLINE, (String)null);
                  sender.sendMessage("       see current login or toal for all online.");
               }

               if ((keyword == Commands.KEYWORDS.ALL || keyword == Commands.KEYWORDS.SAVE) && OnTime.permission.has(sender, "ontime.save")) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.HELP, Commands.KEYWORDS.SAVE, (String)null);
                  sender.sendMessage("       Save current OnTime player data to file/database");
               }

               if ((keyword == Commands.KEYWORDS.ALL || keyword == Commands.KEYWORDS.TOP) && OnTime.permission.has(sender, "ontime.top")) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.HELP, Commands.KEYWORDS.TOP, (String)null);
                  sender.sendMessage("       Show the top players based on OnTime collected data");
               }

               if (OnTime.permission.has(sender, "ontime.report")) {
                  sender.sendMessage("/ontime report : Generates user reports in /plugin/OnTime server directory.");
               }

               if (OnTime.permission.has(sender, "ontime.remove")) {
                  sender.sendMessage("/ontime remove <userid> : Remove specified (offline) user from OnTime datafiles.");
               }

               if (OnTime.permission.has(sender, "ontime.rewards.admin")) {
                  sender.sendMessage("/ontime reward [list/add/remove] <param> : Show/add/remove reward levels.");
               }

               if (OnTime.permission.has(sender, "ontime.moduser")) {
                  sender.sendMessage("/ontime set total <userid> <hh> <mm> <ss> : Set total OnTime");
                  sender.sendMessage("/ontime set days <userid> <hh> <mm> <ss> : Set days on");
                  sender.sendMessage("/ontime add <userid> <hh> <mm> <ss> : Add to total OnTime");
               }

               return true;
            case IMPORT:
               if (!OnTime.permission.has(sender, "ontime.moduser")) {
                  return this.noPermission(sender);
               }

               if (args.length < 2) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               switch (parm1) {
                  case LOGBLOCK:
                     if (args.length > 2) {
                        parm2 = this.checkKeyword(args[2]);
                        if (parm2 == Commands.KEYWORDS.REPLACE) {
                           sender.sendMessage(_plugin.get_import().fromLogBlock(true));
                           return true;
                        }

                        this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, parm1, (String)null);
                        return true;
                     }

                     sender.sendMessage(_plugin.get_import().fromLogBlock(false));
                     return true;
                  case REPORT:
                     if (args.length < 3) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, parm1, (String)null);
                        return true;
                     }

                     if (args.length > 3) {
                        parm2 = this.checkKeyword(args[3]);
                        if (parm2 == Commands.KEYWORDS.REPLACE) {
                           if (args.length > 4) {
                              sender.sendMessage(_plugin.get_import().fromReport(args[2], true, args[4]));
                           } else {
                              sender.sendMessage(_plugin.get_import().fromReport(args[2], true, (String)null));
                           }

                           return true;
                        }

                        sender.sendMessage(_plugin.get_import().fromReport(args[2], false, args[3]));
                        return true;
                     }

                     sender.sendMessage(_plugin.get_import().fromReport(args[2], false, (String)null));
                     return true;
                  case YML:
                     if (args.length < 3) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, parm1, (String)null);
                        return true;
                     }

                     if (args.length > 3) {
                        parm2 = this.checkKeyword(args[3]);
                        if (parm2 == Commands.KEYWORDS.REPLACE) {
                           if (args.length > 4) {
                              sender.sendMessage(_plugin.get_import().fromYML(args[2], true, args[4]));
                           } else {
                              sender.sendMessage(_plugin.get_import().fromYML(args[2], true, (String)null));
                           }

                           return true;
                        }

                        sender.sendMessage(_plugin.get_import().fromYML(args[2], false, args[3]));
                        return true;
                     }

                     sender.sendMessage(_plugin.get_import().fromYML(args[2], false, (String)null));
                     return true;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
                     return true;
               }
            case LOGFILE:
               if (!OnTime.permission.has(sender, "ontime.logfile.admin")) {
                  return this.noPermission(sender);
               }

               if (args.length < 2) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               switch (parm1) {
                  case DISABLE:
                     LogFile.write(4, "ONTIME Logfile Disabled by " + sender.getName());
                     OnTime.logEnable = false;
                     Updates.saveConfig(_plugin);
                     sender.sendMessage("OnTime debug logging has been disabled.");
                     break;
                  case ENABLE:
                     if (args.length < 3) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                        return true;
                     }

                     if (this.badNumber(sender, args[2])) {
                        return true;
                     }

                     first = Integer.parseInt(args[2]);
                     if (first < 0 || first > 3) {
                        sender.sendMessage("Log detail level must be a value between 1 and 3, inclusive.");
                        return true;
                     }

                     OnTime.logEnable = true;
                     OnTime.logLevel = first;
                     Updates.saveConfig(_plugin);
                     sender.sendMessage("OnTime debug logging has been enabled at level " + first);
                     LogFile.initialize(OnTime.onTimeDataFolder, "OnTimeLog.txt", "OnTime");
                     LogFile.write(4, "ONTIME Logfile Enabled at level " + first + " by " + sender.getName());
                     break;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
               }

               return true;
            case MESSAGE:
            case MSG:
               if (!OnTime.messagesEnable) {
                  sender.sendMessage(ChatColor.RED + "OnTime Messages are not enabled on this server.");
                  return true;
               }

               if (!OnTime.permission.has(sender, "ontime.message.admin")) {
                  return this.noPermission(sender);
               }

               if (args.length < 2) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               switch (parm1) {
                  case ADD:
                     if (args.length < 4) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, " Expected: '/ontime message add <msgtag> <message string>");
                        return true;
                     }

                     if (!_plugin.get_messages().handleMessage(args[2], args, 3, Messages.msgAction.ADD, sender)) {
                        sender.sendMessage(ChatColor.RED + "A message with tag " + args[2] + " already exists.  Request failed.");
                        return true;
                     }

                     sender.sendMessage("Message successfully added.");
                     return true;
                  case LIST:
                     if (!_plugin.get_messages().handleMessage((String)null, args, 0, Messages.msgAction.LIST, sender)) {
                        sender.sendMessage(ChatColor.RED + "There are no messages defined.  Request failed.");
                        return true;
                     }

                     return true;
                  case REMOVE:
                     if (args.length < 3) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "Syntax: '/ontime message remove <msgtag>");
                        return true;
                     }

                     if (!_plugin.get_messages().handleMessage(args[2], args, 0, Messages.msgAction.REMOVE, sender)) {
                        sender.sendMessage(ChatColor.RED + "A message with tag " + args[2] + " does not exist.  Request failed.");
                        return true;
                     }

                     sender.sendMessage("Message successfully removed.");
                     return true;
                  case SET:
                     if (args.length < 5) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "Expected: '/ontime message set <playername> [delta/login/play/real] {<dd>D <hh>H <mm>M} [<msgtag>/<adhoc msg>]");
                        return true;
                     }

                     playerName = args[2];
                     if (!Players.hasOnTimeRecord(playerName)) {
                        Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, playerName, (RewardData)null);
                        return true;
                     }

                     RewardData.EventReference reference = null;
                     RewardData.EventReference[] var51;
                     int var48 = (var51 = RewardData.EventReference.values()).length;

                     for(int var49 = 0; var49 < var48; ++var49) {
                        RewardData.EventReference r1 = var51[var49];
                        if (args[3].equalsIgnoreCase(r1.label())) {
                           reference = r1;
                        }
                     }

                     if (reference == null) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "Syntax: '/ontime message set <playername> [delta/ontime/real] {<dd>D <hh>H <mm>M} [msg=]<msgtag>/<adhoc msg>");
                        return true;
                     }

                     if (reference != RewardData.EventReference.LOGIN && time < 0L) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "Syntax: '/ontime message set <playername> [delta/ontime/real] {<dd>D <hh>H <mm>M} [msg=]<msgtag>/<adhoc msg>");
                        return true;
                     }

                     if (messageStart < 0) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "Expected: '/ontime message set <playername> [delta/login/play/real] {<dd>D <hh>H <mm>M} [msg=]<msgtag>/<adhoc msg>");
                        return true;
                     }

                     args[messageStart] = args[messageStart].substring(args[messageStart].indexOf("=") + 1);
                     msgTag = null;
                     String msgTag = args[messageStart];
                     if (Messages.messages.getString("message." + msgTag) == null) {
                        if (args.length == messageStart - 1) {
                           sender.sendMessage(ChatColor.RED + "A message with tag '" + msgTag + "' cannot be found.");
                           return true;
                        }

                        _plugin.get_messages().setAdhocMessage(playerName, reference, time, args, messageStart, sender);
                        msgTag = "adhoc" + Messages.adhocCount;
                     } else {
                        _plugin.get_messages().setMessage(playerName, reference, time, msgTag);
                     }

                     sender.sendMessage("Message " + msgTag + " successfully set for " + playerName);
                     return true;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
                     break label1144;
               }
            case ONLINE:
               if (!OnTime.permission.has(sender, "ontime.online")) {
                  return this.noPermission(sender);
               }

               first = _plugin.getServer().getOnlinePlayers().size();
               if (first < 1) {
                  sender.sendMessage("No players are currently online.");
                  return true;
               }

               if (args.length < 2) {
                  parm1 = Commands.KEYWORDS.TOTAL;
               } else {
                  parm1 = this.checkKeyword(args[1]);
               }

               Iterator var44;
               switch (parm1) {
                  case LOGIN:
                     sender.sendMessage("Current Login Time for All Online:");
                     var44 = _plugin.getServer().getOnlinePlayers().iterator();

                     while(var44.hasNext()) {
                        msgTag = (Player)var44.next();
                        playerName = OnTime.getPlayerName(msgTag);
                        long currentOnTime = _plugin.get_logintime().current(Players.getData(msgTag));
                        String OnTimeString = Output.getTimeBreakdown(currentOnTime, Output.TIMEDETAIL.LONG);
                        sender.sendMessage(playerName + ": " + OnTimeString);
                     }

                     return true;
                  case NEXT:
                     _plugin.get_rewards().showSchedule(sender);
                     break;
                  case TOTAL:
                     sender.sendMessage("Current Total Server Time for All Online:");
                     var44 = _plugin.getServer().getOnlinePlayers().iterator();

                     while(var44.hasNext()) {
                        msgTag = (Player)var44.next();
                        playerName = OnTime.getPlayerName(msgTag);
                        String OnTimeString = Output.getTimeBreakdown(_plugin.get_playingtime().totalOntime(playerName), Output.TIMEDETAIL.LONG);
                        sender.sendMessage(playerName + ": " + OnTimeString);
                     }

                     return true;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
               }

               return true;
            case RELOAD:
               if (!OnTime.permission.has(sender, "ontime.save")) {
                  return this.noPermission(sender);
               }

               boolean reloadRewards = false;
               boolean reloadAllPlayers = false;
               boolean reloadOfflinePlayers = false;
               boolean reloadMessages = false;
               boolean reloadConfig = false;
               boolean reloadOutput = false;
               File updateFile = null;
               if (args.length == 1) {
                  reloadRewards = true;
                  reloadAllPlayers = true;
                  reloadMessages = true;
                  reloadConfig = true;
                  reloadOutput = true;
               } else {
                  parm1 = this.checkKeyword(args[1]);
                  switch (parm1) {
                     case ALL:
                        reloadRewards = true;
                        reloadAllPlayers = true;
                        reloadMessages = true;
                        reloadConfig = true;
                        reloadOutput = true;
                        break;
                     case CONFIG:
                        reloadConfig = true;
                        break;
                     case MESSAGE:
                     case MSG:
                        reloadMessages = true;
                        break;
                     case OUTPUT:
                        reloadOutput = true;
                        break;
                     case PLAYER:
                        if (args.length == 2) {
                           reloadAllPlayers = true;
                        } else if (args[2].equalsIgnoreCase("offline")) {
                           if (OnTime.dataStorage != DataIO.datastorage.MYSQL) {
                              sender.sendMessage(ChatColor.RED + "Reloading of Offline players only valid when using MySQL storage.");
                              return true;
                           }

                           reloadOfflinePlayers = true;
                        } else {
                           reloadAllPlayers = true;
                        }
                        break;
                     case REWARD:
                        reloadRewards = true;
                        break;
                     default:
                        this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
                  }
               }

               if (reloadConfig || reloadOutput) {
                  updateFile = new File(OnTime.onTimeDataFolder, "updates.txt");
                  if (!updateFile.exists()) {
                     updateFile.getParentFile().mkdirs();
                     _plugin.copy(_plugin.getResource("updates.txt"), updateFile);
                  }
               }

               LogFile.write(3, "OnTime Reload initiated by " + sender.getName());
               if (reloadConfig) {
                  _plugin.initConfig(OnTime.onTimeDataFolder);
                  _plugin.get_dataio().updateMySQL();
                  sender.sendMessage("OnTime config.yml sucessfully reloaded.");
               }

               if (reloadAllPlayers) {
                  _plugin.get_rewards().saveIndiRewards(_plugin.getDataFolder());
                  _plugin.get_rewards().cancelAllRewardTasks();
                  _plugin.get_rewards().getRewardMap().clear();
                  _plugin.get_dataio().clearAllMaps();
                  if (_plugin.get_dataio().loadAllData(DataIO.mysqlload.INIT)) {
                     sender.sendMessage("OnTime player data sucessfully reloaded for all players.");
                  } else {
                     sender.sendMessage(ChatColor.RED + "OnTime player data reload failed.");
                  }
               }

               if (reloadOfflinePlayers) {
                  if (_plugin.get_dataio().loadTopPlayerDataMySQL(DataIO.mysqlload.ALL, OnTime.topListMax)) {
                     sender.sendMessage("OnTime player data sucessfully reloaded for offline players.");
                  } else {
                     sender.sendMessage(ChatColor.RED + "OnTime player data reload failed.");
                  }
               }

               if (reloadRewards) {
                  if (!OnTime.rewardsEnable && !OnTime.suspendOnTime) {
                     sender.sendMessage("OnTime Rewards not enabled.  Reward data not reloaded.");
                  } else {
                     if (!reloadAllPlayers) {
                        _plugin.get_rewards().saveIndiRewards(_plugin.getDataFolder());
                        _plugin.get_rewards().cancelAllRewardTasks();
                        _plugin.get_rewards().getRewardMap().clear();
                     }

                     _plugin.get_rewards().initRewards(OnTime.onTimeDataFolder);
                     _plugin.get_rewards().initIndiRewards(OnTime.onTimeDataFolder);
                     if (!reloadAllPlayers) {
                        _plugin.get_rewards().scheduleAllRewardTasks();
                        sender.sendMessage("OnTime Rewards data reloaded, and rewards reset for online players.");
                     } else {
                        sender.sendMessage("OnTime Rewards data reloaded.");
                     }
                  }
               }

               if (reloadMessages && OnTime.messagesEnable) {
                  Messages.initMessages(OnTime.onTimeDataFolder);
                  LogFile.write(3, "OnTime Messages sucessfully reloaded.");
                  sender.sendMessage("OnTime Messages sucessfully reloaded.");
               }

               if (reloadOutput) {
                  if (Output.initOutput(OnTime.onTimeDataFolder)) {
                     sender.sendMessage("OnTime Output.yml sucessfully reloaded.");
                     LogFile.write(3, "OnTime Output.yml sucessfully reloaded.");
                  } else {
                     sender.sendMessage("Reload of output.yml failed !!  OnTime will not function correctly until this is addressed.");
                  }
               }

               if (reloadConfig || reloadOutput) {
                  updateFile.delete();
               }

               if (reloadAllPlayers) {
                  Iterator var58 = _plugin.getServer().getOnlinePlayers().iterator();

                  while(var58.hasNext()) {
                     Player playerReload = (Player)var58.next();
                     _plugin.get_playereventlistener().loginPlayer(playerReload, true);
                  }

                  if (_plugin.getServer().getOnlinePlayers().size() > 0) {
                     sender.sendMessage("All online players re-logged in and new rewards set if applicable.");
                  }
               }

               return true;
            case REMOVE:
               if (!OnTime.permission.has(sender, "ontime.remove")) {
                  return this.noPermission(sender);
               }

               if (args.length < 2) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               playerName = args[1];
               if (!Players.hasOnTimeRecord(playerName)) {
                  Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[1], (RewardData)null);
                  return true;
               }

               if (_plugin.get_logintime().playerIsOnline(Players.getData(playerName))) {
                  sender.sendMessage(ChatColor.RED + args[1] + " is online and online players cannot be removed. ");
                  return true;
               }

               _plugin.get_dataio().removePlayerCompletely(DataIO.REMOVEKEY.PLAYERNAME, Players.getData(playerName));
               sender.sendMessage(playerName + " has been removed from total OnTime records.");
               LogFile.write(3, sender.getName() + " has removed " + playerName + " from the OnTime records.");
               return true;
            case REPORT:
               if (!OnTime.permission.has(sender, "ontime.report")) {
                  return this.noPermission(sender);
               }

               _plugin.get_playingtime().buildPlaytimeMap(OnTime.serverID);
               Report.generate(_plugin.get_playingtime().getPlaytimeMap(), OnTime.onTimeReportsFolder, "OnTimeReport", Report.ReportType.ONTIME);
               sender.sendMessage("OnTime report generated in " + OnTime.onTimeReportsFolder);
               if (OnTime.dailyPlayReportEnable) {
                  _plugin.get_todaytime().buildTodaytimeMap(OnTime.serverID);
                  Report.generate(_plugin.get_todaytime().getDayMap(), OnTime.onTimeReportsFolder, "DailyReport", Report.ReportType.TODAYTIME);
                  sender.sendMessage("Daily report generated in " + OnTime.onTimeReportsFolder);
               }

               if (OnTime.weeklyPlayReportEnable) {
                  _plugin.get_todaytime().buildWeektimeMap(OnTime.serverID);
                  Report.generate(_plugin.get_todaytime().getWeekMap(), OnTime.onTimeReportsFolder, "WeeklyReport", Report.ReportType.WEEKLY);
                  sender.sendMessage("Weekly report generated in " + OnTime.onTimeReportsFolder);
               }

               if (OnTime.monthlyPlayReportEnable) {
                  _plugin.get_todaytime().buildMonthtimeMap(OnTime.serverID);
                  Report.generate(_plugin.get_todaytime().getMonthMap(), OnTime.onTimeReportsFolder, "MonthlyReport", Report.ReportType.MONTHLY);
                  sender.sendMessage("Monthly report generated in " + OnTime.onTimeReportsFolder);
               }

               if (OnTime.collectAfkEnable) {
                  Report.generate((HashMap)null, OnTime.onTimeReportsFolder, "AFKReport", Report.ReportType.AFK);
                  sender.sendMessage("AFK report generated in " + OnTime.onTimeReportsFolder);
               }

               return true;
            case RESUME:
               if (!OnTime.permission.has(sender, "ontime.suspend")) {
                  return this.noPermission(sender);
               }

               if (!OnTime.suspendOnTime) {
                  sender.sendMessage("OnTime is not currently suspended.");
                  return true;
               }

               this.resumeOnTime();
               if (this.suspendTaskID != -1) {
                  _plugin.getServer().getScheduler().cancelTask(this.suspendTaskID);
                  this.suspendTaskID = -1;
               }

               sender.sendMessage("OnTime functions have all be resumed.");
               LogFile.write(3, "OnTime Resume initiated by " + sender.getName());
               return true;
            case REWARD:
            case RW:
               return _rewardCommand.onRewardCommand(sender, cmd, RewardCommands.RewardOrPenalty.REWARD, args);
            case SAVE:
               if (!OnTime.permission.has(sender, "ontime.save")) {
                  return this.noPermission(sender);
               }

               LogFile.write(3, "'ontime save' executed by " + sender.getName());
               if (_plugin.get_dataio().saveAllData(OnTime.onTimeDataFolder)) {
                  sender.sendMessage("OnTime data saved.");
               } else {
                  sender.sendMessage("OnTime data save attempt failed.");
               }

               return true;
            case SET:
               if (!OnTime.permission.has(sender, "ontime.moduser")) {
                  return this.noPermission(sender);
               }

               if (args.length < 4) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               playerName = args[2];
               if ((playerData = Players.getData(playerName)) == null) {
                  Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[2], (RewardData)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               switch (parm1) {
                  case DAYS:
                  case FIRST:
                  case TOTAL:
                     if (time == -1L) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.MISSINGTIME, (KEYWORDS)null, (String)null);
                        return true;
                     }
               }

               int days = false;
               int days;
               switch (parm1) {
                  case DAYS:
                     days = (int)TimeUnit.MILLISECONDS.toDays(time);
                     LogFile.console(0, "Setting Days for " + playerName + " to " + days);
                     if (_plugin.get_logintime().setDays(sender, playerData, days)) {
                        sender.sendMessage(playerName + " Different Days On is now  " + days);
                        LogFile.write(3, sender.getName() + " has set " + playerName + "'s Different Days On to " + days);
                     } else {
                        sender.sendMessage("Request to change 'days on' failed.  No OnTime record found for " + playerName);
                     }

                     return true;
                  case FIRST:
                     days = (int)TimeUnit.MILLISECONDS.toDays(time);
                     if (OnTime.dataStorage != DataIO.datastorage.MYSQL) {
                        sender.sendMessage(ChatColor.RED + "'/ontime set first <userid> <dd>' requires use of MySQL Storage option.");
                        return true;
                     }

                     if (!DataIO.mysqlNew.checkMySQLConnection()) {
                        sender.sendMessage(ChatColor.RED + "Command failed.  MySQL Database connection failed.");
                        return true;
                     }

                     long firstLogin = TodayTime.todayMidnight() - TimeUnit.DAYS.toMillis((long)days);
                     _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "firstlogin", firstLogin, playerName);
                     playerID = (new SimpleDateFormat(Output.OnTimeOutput.getString("output.dateFormat") + " ")).format(firstLogin);
                     sender.sendMessage(playerName + " First Login Set to  " + playerID);
                     LogFile.write(3, sender.getName() + " has set " + playerName + "'s First Login to " + playerID);
                     return true;
                  case POINT:
                     int points = false;
                     if (_plugin.get_points().pointsEnabled(sender)) {
                        if (!args[3].matches("[+]?\\d+(\\/\\d+)?")) {
                           this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                           return true;
                        }

                        i = Integer.parseInt(args[3]);
                        if (_plugin.get_points().setPoints(playerName, i)) {
                           sender.sendMessage(playerName + " Points set to  " + i);
                           LogFile.write(3, sender.getName() + " has set " + playerName + "'s points to " + i);
                        }

                        return true;
                     }

                     return true;
                  case TOTAL:
                     long playingTime = Long.valueOf(time);
                     PlayTimeData serverTime = Players.getWorldTime(playerData, OnTime.serverID);
                     if (world != null) {
                        worldTime = null;
                        PlayTimeData worldTime;
                        if ((worldTime = Players.getWorldTime(playerData, world)) != null) {
                           worldTime.totalTime = playingTime;
                           if (worldTime.todayTime > playingTime) {
                              worldTime.todayTime = playingTime;
                           }

                           if (worldTime.weekTime > playingTime) {
                              worldTime.weekTime = playingTime;
                           }

                           if (worldTime.monthTime > playingTime) {
                              worldTime.monthTime = playingTime;
                           }

                           if (_plugin.get_logintime().playerIsOnline(playerData)) {
                              worldTime.lastLogin = Calendar.getInstance().getTimeInMillis();
                           }
                        } else {
                           Players.setWorldTime(playerData, world, playingTime, 0L, 0L, 0L, Calendar.getInstance().getTimeInMillis());
                        }
                     }

                     if (world == null || serverTime.totalTime < playingTime) {
                        serverTime.totalTime = playingTime;
                        if (serverTime.todayTime > playingTime) {
                           serverTime.todayTime = playingTime;
                        }

                        if (serverTime.weekTime > playingTime) {
                           serverTime.weekTime = playingTime;
                        }

                        if (serverTime.monthTime > playingTime) {
                           serverTime.monthTime = playingTime;
                        }

                        if (_plugin.get_logintime().playerIsOnline(playerData)) {
                           serverTime.lastLogin = Calendar.getInstance().getTimeInMillis();
                        }
                     }

                     _plugin.get_awayfk().resetAFKTime(playerData);
                     if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                        _plugin.get_dataio().savePlayerDataMySQL(playerName, true);
                     }

                     if (_plugin.get_logintime().playerIsOnline(playerData) && OnTime.rewardsEnable) {
                        _plugin.get_rewards().cancelPlayerRewardTasks(playerName, "all");
                        _plugin.get_rewards().scheduleNextReward(playerName, (RewardData.timeScope)null);
                        _plugin.get_rewards().scheduleRepeatingReward(playerData, -1);
                        _plugin.get_rewards().scheduleIndiRewards(playerName, Rewards.indiScheduleSource.COMMAND);
                     }

                     sender.sendMessage(playerName + " Total OnTime is now  " + Output.getTimeBreakdown(playingTime, Output.TIMEDETAIL.SHORT));
                     LogFile.write(3, sender.getName() + " has set " + playerName + "'s total OnTime to " + Output.getTimeBreakdown(playingTime, Output.TIMEDETAIL.SHORT));
                     return true;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
                     return true;
               }
            case SUBTRACT:
               if (!OnTime.permission.has(sender, "ontime.moduser")) {
                  return this.noPermission(sender);
               }

               if (args.length < 4) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               playerName = args[2];
               if ((playerData = Players.getData(playerName)) == null) {
                  Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[2], (RewardData)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               switch (parm1) {
                  case POINT:
                     if (_plugin.get_points().pointsEnabled(sender)) {
                        if (!args[3].matches("[+]?\\d+(\\/\\d+)?")) {
                           this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                           return true;
                        }

                        this.modifyPoints(sender, playerName, -Integer.parseInt(args[3]));
                        return true;
                     }

                     return true;
                  case TOTAL:
                     if (time == -1L) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.MISSINGTIME, (KEYWORDS)null, (String)null);
                     } else {
                        this.modifyTime(sender, playerData, -time, world);
                     }

                     return true;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
                     return true;
               }
            case SUSPEND:
               if (!OnTime.permission.has(sender, "ontime.suspend")) {
                  return this.noPermission(sender);
               }

               if (OnTime.suspendOnTime) {
                  sender.sendMessage("OnTime is already suspended.");
                  return true;
               }

               if (time == -1L) {
                  LogFile.console(0, "No time specified - default to 10m");
                  time = TimeUnit.MINUTES.toMillis(10L);
               }

               this.suspendOnTime();
               this.suspendTaskID = _plugin.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new Runnable() {
                  public void run() {
                     Commands.this.resumeOnTime();
                     Commands.this.suspendTaskID = -1;
                     LogFile.console(3, "OnTime Suspend timeout. OnTime functions have resumed.");
                  }
               }, time / 50L);
               sender.sendMessage("OnTime functions have all be suspended.  OnTime functions will resume in " + TimeUnit.MILLISECONDS.toMinutes(time) + " minute(s).");
               LogFile.write(3, "OnTime Suspend initiated by " + sender.getName() + " Duration: " + TimeUnit.MILLISECONDS.toMinutes(time) + " minute(s).");
               return true;
            case TEST:
               if (!OnTime.permission.has(sender, "ontime.test")) {
                  return this.noPermission(sender);
               }

               if (args.length < 2) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               switch (parm1) {
                  case AUDIT:
                     _plugin.get_playereventlistener().auditLogout();
                     LogFile.write(3, "'ontime test auidt' excuted by " + sender.getName());
                     sender.sendMessage("Logout Audit Executed");
                     return true;
                  case LOGIN:
                     int numPlayers = 1;
                     if (args.length < 4) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, parm1, (String)null);
                        return true;
                     }

                     if (this.badNumber(sender, args[3])) {
                        return true;
                     }

                     Integer timeOffset = Integer.parseInt(args[3]);
                     if (args.length == 5) {
                        if (this.badNumber(sender, args[4])) {
                           return true;
                        }

                        numPlayers = Integer.parseInt(args[4]);
                     }

                     playerID = null;

                     for(i = 0; i < numPlayers; ++i) {
                        if (numPlayers > 1) {
                           playerID = args[2] + "_" + i;
                        } else {
                           playerID = args[2];
                        }

                        if (!OnTimeTest.loginTestPlayer(playerID, (long)(timeOffset + i), world)) {
                           sender.sendMessage(ChatColor.RED + "Test Login failed. Player (" + playerID + ") is alredy logged in.");
                           return true;
                        }

                        sender.sendMessage(playerID + " is now 'logged in'.");
                     }

                     return true;
                  case LOGOUT:
                     if (args.length < 3) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, parm1, (String)null);
                        return true;
                     }

                     if (!_plugin.get_ontimetest().logout(Players.getUUID(args[2]))) {
                        sender.sendMessage(args[2] + " user ID not found as 'online'");
                        return true;
                     }

                     sender.sendMessage(args[2] + " has been 'logged out'");
                     return true;
                  case TODAY:
                     LogFile.write(3, "'ontime test today' excuted by " + sender.getName());
                     dbColumn = (new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss] ")).format(Calendar.getInstance().getTime());
                     Calendar todayMidnight = Calendar.getInstance();
                     todayMidnight.setTimeInMillis(TodayTime.todayMidnight());
                     String tonight = (new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss] ")).format(todayMidnight.getTime());
                     sender.sendMessage("Checking to see if its a new Day ....");
                     sender.sendMessage("datetime: " + dbColumn + "   TodayMidnight:" + tonight);
                     TodayTime.checkNewDay();
                     return true;
                  case VOTIFIER:
                     if (!OnTime.votifierEnable) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "Votifier plugin is not installed on this server.");
                        return true;
                     }

                     if (args.length < 4) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, parm1, (String)null);
                        return true;
                     }

                     Vote vote = new Vote();
                     vote.setUsername(args[2]);
                     vote.setServiceName(args[3]);
                     worldTime = new VotifierEvent(vote);
                     _plugin.get_votifiereventlistener().onVotifierEvent(worldTime);
                     sender.sendMessage("Vote by " + args[2] + " successfully cast.");
                     LogFile.write(3, "'ontime test votifier' excuted by " + sender.getName() + " for " + args[2] + " and site " + args[3]);
                     return true;
                  case YESTERDAY:
                     LogFile.write(3, "'ontime test yesterday' excuted by " + sender.getName());
                     dbColumn = (new SimpleDateFormat("[MM/dd/yyyy] ")).format(TodayTime.todayMidnight());
                     sender.sendMessage("Today is " + dbColumn);
                     OnTime.todayStart = TodayTime.todayMidnight() - TimeUnit.DAYS.toMillis(1L);
                     OnTime.weekStart = TodayTime.todayMidnight() - TimeUnit.DAYS.toMillis(7L);
                     OnTime.monthStart = TodayTime.todayMidnight() - TimeUnit.DAYS.toMillis(30L);
                     dbColumn = (new SimpleDateFormat("[MM/dd/yyyy] ")).format(OnTime.todayStart);
                     sender.sendMessage("But have set 'Today' in datafiles as " + dbColumn);
                     return true;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, parm1, args[1]);
                     return true;
               }
            case TOP:
               if (!OnTime.permission.has(sender, "ontime.top")) {
                  return this.noPermission(sender);
               }

               Integer listLength = OnTime.topListMax;
               if (args.length > 1) {
                  if (this.badNumber(sender, args[1])) {
                     return true;
                  }

                  listLength = Integer.parseInt(args[1]);
                  if (listLength >= 1 && listLength <= OnTime.topListMax) {
                     PlayingTime.topAdder topAdder = PlayingTime.topAdder.ONLINE;
                     String[] data = new String[3];
                     dbColumn = null;
                     if (args.length > 2) {
                        parm1 = this.checkKeyword(args[2]);
                        switch (parm1) {
                           case AFK:
                              data[1] = "today";
                              dbColumn = "afk";
                              break;
                           case PLAYTIME:
                              _plugin.get_playingtime().buildPlaytimeMap(OnTime.serverID);
                              break;
                           case POINT:
                              if (!OnTime.pointsEnable) {
                                 sender.sendMessage(ChatColor.RED + "OnTime is not configured for points collection & reporting.");
                                 return true;
                              }

                              dbColumn = "point";
                              break;
                           case REFER:
                              dbColumn = "refer";
                              break;
                           case VOTE:
                              dbColumn = "vote";
                              break;
                           default:
                              this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[2]);
                              return true;
                        }

                        if (args.length > 3) {
                           parm2 = this.checkKeyword(args[3]);
                           switch (parm2) {
                              case MONTH:
                              case TODAY:
                              case WEEK:
                                 if (parm1 == Commands.KEYWORDS.PLAYTIME && !OnTime.collectPlayDetailEnable || parm1 == Commands.KEYWORDS.REFER && !OnTime.collectReferDetailEnable || parm1 == Commands.KEYWORDS.VOTE && !OnTime.collectVoteDetailEnable || parm1 == Commands.KEYWORDS.AFK && !OnTime.collectAfkEnable) {
                                    sender.sendMessage(ChatColor.RED + "OnTime is not configured for detailed " + parm1.toString().toLowerCase() + " collection & reporting.");
                                    return true;
                                 }

                                 if (parm1 == Commands.KEYWORDS.POINT) {
                                    sender.sendMessage(ChatColor.RED + "OnTime does not support daily, weekly, or monthly point collection and reporting.");
                                    return true;
                                 }
                                 break;
                              case SITES:
                                 if (parm1 != Commands.KEYWORDS.VOTE) {
                                    sender.sendMessage(ChatColor.RED + "'sites' is only supported for 'voting' top lists.");
                                    return true;
                                 }
                                 break;
                              case TOTAL:
                                 if (parm1 == Commands.KEYWORDS.AFK) {
                                    sender.sendMessage(ChatColor.RED + "Only 'Today', 'Weekly' & 'Monthly' is supported for AFK top lists.");
                                    return true;
                                 }
                                 break;
                              default:
                                 this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[3]);
                                 return true;
                           }

                           switch (parm1) {
                              case AFK:
                              case POINT:
                              case REFER:
                              case VOTE:
                                 if (!OnTime.permission.has(sender, "ontime.top.extra")) {
                                    return this.noPermission(sender);
                                 }

                                 if (OnTime.dataStorage != DataIO.datastorage.MYSQL) {
                                    sender.sendMessage(ChatColor.RED + "OnTime is not configured to support this command. MySQL storage required.");
                                    return true;
                                 }

                                 data[0] = parm1.toString().toLowerCase();
                                 data[1] = parm2.toString().toLowerCase();
                                 data[2] = String.valueOf(OnTime.todayStart);
                                 switch (parm2) {
                                    case MONTH:
                                       data[2] = String.valueOf(OnTime.monthStart);
                                       Output.generate("output.topListHeader", sender, data);
                                       dbColumn = dbColumn + "Month";
                                       _plugin.get_dataio().topListFromMySQL(sender, dbColumn, listLength, DataIO.dataset.PLAYER);
                                       return true;
                                    case SITES:
                                       data[1] = "total";
                                       Output.generate("output.topListHeader", sender, data);
                                       _plugin.get_dataio().topListFromMySQL(sender, "votes", listLength, DataIO.dataset.SERVER);
                                       return true;
                                    case TODAY:
                                       Output.generate("output.topListHeader", sender, data);
                                       dbColumn = dbColumn + "Today";
                                       _plugin.get_dataio().topListFromMySQL(sender, dbColumn, listLength, DataIO.dataset.PLAYER);
                                       return true;
                                    case TOTAL:
                                       Output.generate("output.topListHeader", sender, data);
                                       if (parm1 == Commands.KEYWORDS.REFER) {
                                          dbColumn = dbColumn + "rals";
                                       } else {
                                          dbColumn = dbColumn + "s";
                                       }

                                       _plugin.get_dataio().topListFromMySQL(sender, dbColumn, listLength, DataIO.dataset.PLAYER);
                                       return true;
                                    case WEEK:
                                       data[2] = String.valueOf(OnTime.weekStart);
                                       Output.generate("output.topListHeader", sender, data);
                                       dbColumn = dbColumn + "Week";
                                       _plugin.get_dataio().topListFromMySQL(sender, dbColumn, listLength, DataIO.dataset.PLAYER);
                                       return true;
                                    default:
                                       this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, parm1, args[3]);
                                       return true;
                                 }
                              case PLAYTIME:
                                 switch (parm2) {
                                    case MONTH:
                                       PlayingTime.topGamers(sender, listLength, PlayingTime.timeScope.MONTH, topAdder);
                                       return true;
                                    case TODAY:
                                       PlayingTime.topGamers(sender, listLength, PlayingTime.timeScope.TODAY, topAdder);
                                       return true;
                                    case TOTAL:
                                       if (args.length > 4) {
                                          parm3 = this.checkKeyword(args[4]);
                                          if (parm3 == null) {
                                             this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[4]);
                                             return true;
                                          }

                                          if ((parm3 == Commands.KEYWORDS.P_MONTH || parm3 == Commands.KEYWORDS.P_TODAY || parm3 == Commands.KEYWORDS.P_WEEK) && !OnTime.collectPlayDetailEnable) {
                                             sender.sendMessage(ChatColor.RED + "OnTime is not configured for detailed playtime collection & reporting.");
                                             return true;
                                          }

                                          switch (parm3) {
                                             case P_LOGIN:
                                                topAdder = PlayingTime.topAdder.LOGIN;
                                                break;
                                             case P_MONTH:
                                                topAdder = PlayingTime.topAdder.MONTH;
                                                break;
                                             case P_RANK:
                                                topAdder = PlayingTime.topAdder.RANK;
                                                break;
                                             case P_TODAY:
                                                topAdder = PlayingTime.topAdder.TODAY;
                                                break;
                                             case P_WEEK:
                                                topAdder = PlayingTime.topAdder.WEEK;
                                                break;
                                             default:
                                                this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[4]);
                                                return true;
                                          }

                                          if (!OnTime.permission.has(sender, "ontime.top.extra")) {
                                             sender.sendMessage(ChatColor.RED + "You don't have permission for " + args[4] + " it will be ignored.");
                                             topAdder = null;
                                          }
                                       }

                                       PlayingTime.topGamers(sender, listLength, PlayingTime.timeScope.TOTAL, topAdder);
                                       return true;
                                    case WEEK:
                                       PlayingTime.topGamers(sender, listLength, PlayingTime.timeScope.WEEK, topAdder);
                                       return true;
                                    default:
                                       this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[2]);
                                       return true;
                                 }
                              default:
                                 this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[2]);
                           }
                        }

                        switch (parm1) {
                           case PLAYTIME:
                              PlayingTime.topGamers(sender, listLength, PlayingTime.timeScope.TOTAL, PlayingTime.topAdder.ONLINE);
                              return true;
                           case POINT:
                           case REFER:
                           case VOTE:
                              data[1] = "total";
                           case AFK:
                              if (!OnTime.permission.has(sender, "ontime.top.extra")) {
                                 return this.noPermission(sender);
                              }

                              if (OnTime.dataStorage != DataIO.datastorage.MYSQL) {
                                 sender.sendMessage(ChatColor.RED + "OnTime is not configured to support this command. MySQL storage required.");
                                 return true;
                              }

                              data[0] = parm1.toString().toLowerCase();
                              data[2] = String.valueOf(OnTime.todayStart);
                              Output.generate("output.topListHeader", sender, data);
                              if (parm1 == Commands.KEYWORDS.VOTE) {
                                 _plugin.get_dataio().topListFromMySQL(sender, "votes", listLength, DataIO.dataset.PLAYER);
                              } else if (parm1 == Commands.KEYWORDS.REFER) {
                                 _plugin.get_dataio().topListFromMySQL(sender, "referrals", listLength, DataIO.dataset.PLAYER);
                              } else if (parm1 == Commands.KEYWORDS.AFK) {
                                 _plugin.get_dataio().topListFromMySQL(sender, "afkToday", listLength, DataIO.dataset.PLAYER);
                              } else if (parm1 == Commands.KEYWORDS.POINT) {
                                 _plugin.get_dataio().topListFromMySQL(sender, "points", listLength, DataIO.dataset.PLAYER);
                              }

                              return true;
                        }
                     }

                     _plugin.get_playingtime().buildPlaytimeMap(OnTime.serverID);
                     PlayingTime.topGamers(sender, listLength, PlayingTime.timeScope.TOTAL, PlayingTime.topAdder.ONLINE);
                     return true;
                  }

                  sender.sendMessage("Please enter a value between 1 and " + OnTime.topListMax);
                  return true;
               }

               _plugin.get_playingtime().buildPlaytimeMap(OnTime.serverID);
               PlayingTime.topGamers(sender, listLength, PlayingTime.timeScope.TOTAL, PlayingTime.topAdder.ONLINE);
               return true;
            case UUID:
               playerName = null;
               String playerName2 = null;
               if (!OnTime.permission.has(sender, "ontime.moduser")) {
                  return this.noPermission(sender);
               }

               if (args.length < 2) {
                  this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                  return true;
               }

               parm1 = this.checkKeyword(args[1]);
               playerID = null;
               switch (parm1) {
                  case CLEAN:
                     if (OnTime.dataStorage == DataIO.datastorage.YML) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "'clean' is only supported with MySQL player data storage.");
                        return true;
                     }

                     playerID = Import.UUIDFUNC.CLEAN;
                     break;
                  case FIND:
                     playerID = Import.UUIDFUNC.FIND;
                     break;
                  case MERGE:
                     playerID = Import.UUIDFUNC.MERGE;
                     if (args.length < 3) {
                        this.syntaxError(sender, Commands.SYNTAXERROR.TOOFEW, Keyword, (String)null);
                        return true;
                     }

                     if (!args[2].equalsIgnoreCase("all")) {
                        if (!Players.hasOnTimeRecord(args[2])) {
                           Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[2], (RewardData)null);
                           return true;
                        }

                        if (OnTime.dataStorage == DataIO.datastorage.YML && args.length < 4) {
                           this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "Two player names to merge must be specified.");
                           return true;
                        }

                        playerName = args[2];
                     } else {
                        if (args.length > 3) {
                           this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[2]);
                           return true;
                        }

                        if (OnTime.dataStorage == DataIO.datastorage.YML) {
                           this.syntaxError(sender, Commands.SYNTAXERROR.SPECIAL, Keyword, "'all' is only supported with MySQL player data storage.");
                           return true;
                        }
                     }

                     if (args.length == 4) {
                        if (!Players.hasOnTimeRecord(args[3])) {
                           Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, args[3], (RewardData)null);
                           return true;
                        }

                        playerName2 = args[3];
                     }
                     break;
                  case PURGE:
                     playerID = Import.UUIDFUNC.PURGE;
                     break;
                  case REPLACE:
                     playerID = Import.UUIDFUNC.REPLACE;
                     break;
                  default:
                     this.syntaxError(sender, Commands.SYNTAXERROR.UNKNOWNWORD, Keyword, args[1]);
                     return true;
               }

               if (Import.uuidRetryTask != null) {
                  _plugin.getServer().getScheduler().cancelTask(Import.uuidRetryTask.getTaskId());
                  LogFile.write(3, "{commands} UUID Retry Task cancelled by execution of new UUID command.");
                  Import.uuidRetryTask = null;
               }

               _plugin.get_import().scheduleUpdateUUID(sender, playerID, playerName, playerName2);
               sender.sendMessage("UUID Update Initiated");
               return true;
            default:
               LogFile.write(10, "{onCommand} Hit default. Arg[0] = " + args[0] + " keyword:" + Keyword);
               return false;
         }
      }

      if (commandLabel.equalsIgnoreCase("referred")) {
         if (!OnTime.enableOnTime) {
            sender.sendMessage(ChatColor.RED + "OnTime Plugin is NOT Enabled.");
            return true;
         } else {
            return _referredCommand.onReferredCommand(sender, cmd, commandLabel, args);
         }
      } else {
         return false;
      }
   }

   public void syntaxError(CommandSender sender, SYNTAXERROR error, KEYWORDS command, String data) {
      StringBuilder sb = new StringBuilder(128);
      if (error != Commands.SYNTAXERROR.HELP) {
         sb.append(ChatColor.RED + "Syntax: ");
      } else {
         sb.append(ChatColor.GREEN);
      }

      if (command != null) {
         switch (command) {
            case ADD:
               sb.append("/ontime add [player/points/total] <userid> {<points>} / {<dd>D <hh>H <mm>M}");
            case AFK:
            case ALL:
            case AUDIT:
            case BACKUP:
            case CLEAN:
            case CONFIG:
            case DAT:
            case DAYS:
            case DISABLE:
            case FIND:
            case FIRST:
            case HELP:
            case LIMITS:
            case LIST:
            case L:
            case MERGE:
            case MSG:
            case MONTH:
            case MYSQL:
            case NEXT:
            case OUTPUT:
            case PLAYER:
            case PLAYTIME:
            case POINT:
            case P_LOGIN:
            case P_MONTH:
            case P_RANK:
            case P_TODAY:
            case P_WEEK:
            case PURGE:
            case REPLACE:
            case RESUME:
            case REWARD:
            case RW:
            case SITES:
            case TODAY:
            case UNKNOWNWORD:
            case WEEK:
            case YESTERDAY:
            default:
               break;
            case CONSOLE:
               sb.append("/ontime console [enable] <detail level>");
               break;
            case ENABLE:
               sb.append("/ontime console enable <level #>");
               break;
            case EXPORT:
               sb.append("/ontime export [mysql/yml]");
               break;
            case IMPORT:
               sb.append("/ontime import [logblock/report/yml] {<parameters>}");
               break;
            case LOGBLOCK:
               sb.append("/ontime import logblock {replace}");
               break;
            case LOGFILE:
               sb.append("/ontime logfile [enable/disable] <level #>");
               break;
            case LOGIN:
               sb.append("/ontime test login <ID> <timeoffset>");
               break;
            case LOGOUT:
               sb.append("/ontime test logout <ID>");
               break;
            case MESSAGE:
               sb.append("ontime message [add/set/remove] <parameters>");
               break;
            case ONLINE:
               sb.append("/ontime online {[login/total/next]}");
               break;
            case REFER:
               sb.append("/referred [by/list/undo] <playerName>");
               break;
            case RELOAD:
               sb.append("/ontime reload [all/config/rewards/messages/players] {offline}");
               break;
            case REMOVE:
               sb.append("/ontime remove <playerName>");
               break;
            case REPORT:
               sb.append("/ontime import report <filename> {replace}");
               break;
            case SAVE:
               sb.append("/ontime save");
               break;
            case SET:
               sb.append("/ontime set [days/first/points/total] <userid> {<dd>D <hh>H <mm>M} / {<points>}");
               break;
            case SUBTRACT:
               sb.append("/ontime sub [points/total] <userid> {<points>} / {<dd>D <hh>H <mm>M}");
               break;
            case SUSPEND:
               sb.append("/ontime suspend {<min>[M]}");
               break;
            case TEST:
               sb.append("/ontime test [login/logout/yesterday/today/votifier] {<ID> <timeoffset>}");
               break;
            case TOP:
               sb.append("/ontime top {<#> [playtime/refer/votes] [total/today/week/month] {[+login/+rank/+today/+week/+month]}/{[players/sites]}}");
               break;
            case TOTAL:
               sb.append("/ontime set total <playerName> {<dd>D <hh>H <mm>M}");
               break;
            case UUID:
               sb.append("/ontime uuid [clean/find/merge/purge/reload] {<playerName> / all}");
               break;
            case VOTE:
               sb.append("/ontime top votes {[players/sites]}");
               break;
            case VOTIFIER:
               sb.append("/ontime test votifier <playername> <webservice name>");
               break;
            case YML:
               sb.append("/ontime import yml <filename> {[replace]} {<playername>}");
         }
      }

      sender.sendMessage(sb.toString());
      switch (error) {
         case TOOFEW:
         case HELP:
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

   private boolean badNumber(CommandSender sender, String number) {
      if (!number.matches("[+-]?\\d+(\\/\\d+)?")) {
         sender.sendMessage("Number expected, " + number + " found instead.");
         return true;
      } else {
         return false;
      }
   }

   public void resumeOnTime() {
      OnTime.suspendOnTime = false;
      if (OnTime.rewardsEnableConfig) {
         OnTime.rewardsEnable = true;
      }

      Iterator var2 = _plugin.getServer().getOnlinePlayers().iterator();

      while(var2.hasNext()) {
         Player player = (Player)var2.next();
         if (!PlayerEventListener.tekkitFakePlayers.contains(player)) {
            _plugin.get_playereventlistener().loginPlayer(player, true);
         }
      }

      _plugin.scheduleAuditLogout();
   }

   public void suspendOnTime() {
      Iterator var2 = _plugin.getServer().getOnlinePlayers().iterator();

      while(var2.hasNext()) {
         Player player = (Player)var2.next();
         if (_plugin.get_permissionsHandler().playerHas(player, "ontime.track")) {
            _plugin.get_playereventlistener().logoutPlayer(player);
         }
      }

      _plugin.cancelAuditLogout();
      OnTime.suspendOnTime = true;
      OnTime.rewardsEnable = false;
   }

   public boolean ontimeOther(CommandSender sender, String[] args) {
      if (!OnTime.permission.has(sender, "ontime.other")) {
         return this.noPermission(sender);
      } else {
         String playerName = args[0];
         if (!Players.hasOnTimeRecord(playerName)) {
            Output.generate((String)"output.noOnTimeRecord", (CommandSender)sender, playerName, (RewardData)null);
            return true;
         } else {
            PlayerData playerData = Players.getData(playerName);
            if (_plugin.get_logintime().playerIsOnline(playerData)) {
               _plugin.get_awayfk().checkPlayerAFK(playerData);
            }

            String[] data = new String[1];
            String worldName = "all";
            boolean isWorld = false;
            if (args.length > 1 && OnTime.perWorldEnable) {
               List worlds = _plugin.getServer().getWorlds();
               Iterator it = worlds.iterator();
               if (args[1].equalsIgnoreCase("all")) {
                  isWorld = true;
               } else {
                  while(it.hasNext() && !isWorld) {
                     String tempWorld = ((World)it.next()).getName();
                     if (tempWorld.startsWith(args[1])) {
                        isWorld = true;
                        worldName = tempWorld;
                        break;
                     }
                  }
               }

               if (worldName.equalsIgnoreCase("all")) {
                  it = worlds.iterator();

                  while(it.hasNext()) {
                     if (Players.getWorldTime(playerData, data[0] = ((World)it.next()).getName()) != null) {
                        Output.generate(sender, "output.ontime-other-world", playerName, data);
                     }
                  }
               } else {
                  data[0] = worldName;
                  Output.generate(sender, "output.ontime-other-world", playerName, data);
               }
            } else {
               Output.generate((String)"output.ontime-other", (CommandSender)sender, playerName, (RewardData)null);
            }

            return true;
         }
      }
   }

   public String autoCompleteName(CommandSender sender, String inputName) {
      if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         int i = -1;
         int count_ = 0;

         int countP;
         for(countP = 0; (i = inputName.indexOf(95, i + 1)) != -1; ++count_) {
         }

         for(i = -1; (i = inputName.indexOf(37, i + 1)) != -1; ++countP) {
         }

         if (count_ + countP == inputName.length()) {
            return null;
         }

         String[] list = _plugin.get_dataio().getDataListFromMySQL("'%" + inputName + "%'", " LIKE ", "playerName", "playerName", "playerName", "ASC");
         if (list == null) {
            return null;
         }

         if (list.length == 1) {
            return list[0];
         }

         sender.sendMessage(Output.OnTimeOutput.getString("output.possible"));
         _plugin.get_output().displayList(sender, list, false);
      }

      return null;
   }

   public void modifyTime(CommandSender sender, PlayerData playerData, long time, String worldName) {
      String playerName = playerData.playerName;
      PlayTimeData serverTime = Players.getWorldTime(playerData, OnTime.serverID);
      serverTime.totalTime += time;
      if (worldName != null) {
         if (playerData.worldTime.containsKey(worldName)) {
            PlayTimeData var10000 = (PlayTimeData)playerData.worldTime.get(worldName);
            var10000.totalTime += time;
         } else {
            Players.setWorldTime(playerData, worldName, time, 0L, 0L, 0L, 0L);
         }
      }

      _plugin.get_awayfk().resetAFKTime(playerData);
      if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         _plugin.get_dataio().savePlayerDataMySQL(playerData, true);
      }

      if (_plugin.get_logintime().playerIsOnline(playerData) && OnTime.rewardsEnable) {
         _plugin.get_rewards().cancelPlayerRewardTasks(playerName, "all");
         _plugin.get_rewards().scheduleNextReward(playerName, (RewardData.timeScope)null);
         _plugin.get_rewards().scheduleRepeatingReward(playerData, -1);
         _plugin.get_rewards().scheduleIndiRewards(playerName, Rewards.indiScheduleSource.COMMAND);
      }

      sender.sendMessage(playerName + "'s Total OnTime has been modified to " + Output.getTimeBreakdown(serverTime.totalTime + _plugin.get_logintime().current(playerData), Output.TIMEDETAIL.SHORT));
      LogFile.write(3, sender.getName() + " has modified " + playerName + "'s total OnTime.  New total is " + Output.getTimeBreakdown(serverTime.totalTime, Output.TIMEDETAIL.SHORT));
   }

   public void modifyPoints(CommandSender sender, String playerName, int points) {
      int newPoints = _plugin.get_points().addPoints(playerName, points);
      if (newPoints > 0) {
         sender.sendMessage(playerName + "'s new points total is " + newPoints);
      } else {
         sender.sendMessage(ChatColor.RED + " failed to modify point total for " + playerName);
      }

   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Commands$KEYWORDS() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Commands$KEYWORDS;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[Commands.KEYWORDS.values().length];

         try {
            var0[Commands.KEYWORDS.ADD.ordinal()] = 1;
         } catch (NoSuchFieldError var66) {
         }

         try {
            var0[Commands.KEYWORDS.AFK.ordinal()] = 2;
         } catch (NoSuchFieldError var65) {
         }

         try {
            var0[Commands.KEYWORDS.ALL.ordinal()] = 3;
         } catch (NoSuchFieldError var64) {
         }

         try {
            var0[Commands.KEYWORDS.AUDIT.ordinal()] = 4;
         } catch (NoSuchFieldError var63) {
         }

         try {
            var0[Commands.KEYWORDS.BACKUP.ordinal()] = 5;
         } catch (NoSuchFieldError var62) {
         }

         try {
            var0[Commands.KEYWORDS.CLEAN.ordinal()] = 6;
         } catch (NoSuchFieldError var61) {
         }

         try {
            var0[Commands.KEYWORDS.CONFIG.ordinal()] = 7;
         } catch (NoSuchFieldError var60) {
         }

         try {
            var0[Commands.KEYWORDS.CONSOLE.ordinal()] = 8;
         } catch (NoSuchFieldError var59) {
         }

         try {
            var0[Commands.KEYWORDS.DAT.ordinal()] = 9;
         } catch (NoSuchFieldError var58) {
         }

         try {
            var0[Commands.KEYWORDS.DAYS.ordinal()] = 10;
         } catch (NoSuchFieldError var57) {
         }

         try {
            var0[Commands.KEYWORDS.DISABLE.ordinal()] = 11;
         } catch (NoSuchFieldError var56) {
         }

         try {
            var0[Commands.KEYWORDS.ENABLE.ordinal()] = 12;
         } catch (NoSuchFieldError var55) {
         }

         try {
            var0[Commands.KEYWORDS.EXPORT.ordinal()] = 13;
         } catch (NoSuchFieldError var54) {
         }

         try {
            var0[Commands.KEYWORDS.FIND.ordinal()] = 14;
         } catch (NoSuchFieldError var53) {
         }

         try {
            var0[Commands.KEYWORDS.FIRST.ordinal()] = 15;
         } catch (NoSuchFieldError var52) {
         }

         try {
            var0[Commands.KEYWORDS.HELP.ordinal()] = 16;
         } catch (NoSuchFieldError var51) {
         }

         try {
            var0[Commands.KEYWORDS.IMPORT.ordinal()] = 17;
         } catch (NoSuchFieldError var50) {
         }

         try {
            var0[Commands.KEYWORDS.L.ordinal()] = 24;
         } catch (NoSuchFieldError var49) {
         }

         try {
            var0[Commands.KEYWORDS.LIMITS.ordinal()] = 18;
         } catch (NoSuchFieldError var48) {
         }

         try {
            var0[Commands.KEYWORDS.LIST.ordinal()] = 19;
         } catch (NoSuchFieldError var47) {
         }

         try {
            var0[Commands.KEYWORDS.LOGBLOCK.ordinal()] = 20;
         } catch (NoSuchFieldError var46) {
         }

         try {
            var0[Commands.KEYWORDS.LOGFILE.ordinal()] = 21;
         } catch (NoSuchFieldError var45) {
         }

         try {
            var0[Commands.KEYWORDS.LOGIN.ordinal()] = 22;
         } catch (NoSuchFieldError var44) {
         }

         try {
            var0[Commands.KEYWORDS.LOGOUT.ordinal()] = 23;
         } catch (NoSuchFieldError var43) {
         }

         try {
            var0[Commands.KEYWORDS.MERGE.ordinal()] = 25;
         } catch (NoSuchFieldError var42) {
         }

         try {
            var0[Commands.KEYWORDS.MESSAGE.ordinal()] = 26;
         } catch (NoSuchFieldError var41) {
         }

         try {
            var0[Commands.KEYWORDS.MONTH.ordinal()] = 28;
         } catch (NoSuchFieldError var40) {
         }

         try {
            var0[Commands.KEYWORDS.MSG.ordinal()] = 27;
         } catch (NoSuchFieldError var39) {
         }

         try {
            var0[Commands.KEYWORDS.MYSQL.ordinal()] = 29;
         } catch (NoSuchFieldError var38) {
         }

         try {
            var0[Commands.KEYWORDS.NA.ordinal()] = 66;
         } catch (NoSuchFieldError var37) {
         }

         try {
            var0[Commands.KEYWORDS.NEXT.ordinal()] = 30;
         } catch (NoSuchFieldError var36) {
         }

         try {
            var0[Commands.KEYWORDS.ONLINE.ordinal()] = 31;
         } catch (NoSuchFieldError var35) {
         }

         try {
            var0[Commands.KEYWORDS.OUTPUT.ordinal()] = 32;
         } catch (NoSuchFieldError var34) {
         }

         try {
            var0[Commands.KEYWORDS.PLAYER.ordinal()] = 33;
         } catch (NoSuchFieldError var33) {
         }

         try {
            var0[Commands.KEYWORDS.PLAYTIME.ordinal()] = 34;
         } catch (NoSuchFieldError var32) {
         }

         try {
            var0[Commands.KEYWORDS.POINT.ordinal()] = 35;
         } catch (NoSuchFieldError var31) {
         }

         try {
            var0[Commands.KEYWORDS.PURGE.ordinal()] = 41;
         } catch (NoSuchFieldError var30) {
         }

         try {
            var0[Commands.KEYWORDS.P_LOGIN.ordinal()] = 36;
         } catch (NoSuchFieldError var29) {
         }

         try {
            var0[Commands.KEYWORDS.P_MONTH.ordinal()] = 37;
         } catch (NoSuchFieldError var28) {
         }

         try {
            var0[Commands.KEYWORDS.P_RANK.ordinal()] = 38;
         } catch (NoSuchFieldError var27) {
         }

         try {
            var0[Commands.KEYWORDS.P_TODAY.ordinal()] = 39;
         } catch (NoSuchFieldError var26) {
         }

         try {
            var0[Commands.KEYWORDS.P_WEEK.ordinal()] = 40;
         } catch (NoSuchFieldError var25) {
         }

         try {
            var0[Commands.KEYWORDS.REFER.ordinal()] = 42;
         } catch (NoSuchFieldError var24) {
         }

         try {
            var0[Commands.KEYWORDS.RELOAD.ordinal()] = 43;
         } catch (NoSuchFieldError var23) {
         }

         try {
            var0[Commands.KEYWORDS.REMOVE.ordinal()] = 44;
         } catch (NoSuchFieldError var22) {
         }

         try {
            var0[Commands.KEYWORDS.REPLACE.ordinal()] = 45;
         } catch (NoSuchFieldError var21) {
         }

         try {
            var0[Commands.KEYWORDS.REPORT.ordinal()] = 46;
         } catch (NoSuchFieldError var20) {
         }

         try {
            var0[Commands.KEYWORDS.RESUME.ordinal()] = 47;
         } catch (NoSuchFieldError var19) {
         }

         try {
            var0[Commands.KEYWORDS.REWARD.ordinal()] = 48;
         } catch (NoSuchFieldError var18) {
         }

         try {
            var0[Commands.KEYWORDS.RW.ordinal()] = 49;
         } catch (NoSuchFieldError var17) {
         }

         try {
            var0[Commands.KEYWORDS.SAVE.ordinal()] = 50;
         } catch (NoSuchFieldError var16) {
         }

         try {
            var0[Commands.KEYWORDS.SET.ordinal()] = 51;
         } catch (NoSuchFieldError var15) {
         }

         try {
            var0[Commands.KEYWORDS.SITES.ordinal()] = 52;
         } catch (NoSuchFieldError var14) {
         }

         try {
            var0[Commands.KEYWORDS.SUBTRACT.ordinal()] = 53;
         } catch (NoSuchFieldError var13) {
         }

         try {
            var0[Commands.KEYWORDS.SUSPEND.ordinal()] = 54;
         } catch (NoSuchFieldError var12) {
         }

         try {
            var0[Commands.KEYWORDS.TEST.ordinal()] = 55;
         } catch (NoSuchFieldError var11) {
         }

         try {
            var0[Commands.KEYWORDS.TODAY.ordinal()] = 56;
         } catch (NoSuchFieldError var10) {
         }

         try {
            var0[Commands.KEYWORDS.TOP.ordinal()] = 57;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[Commands.KEYWORDS.TOTAL.ordinal()] = 58;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[Commands.KEYWORDS.UNKNOWNWORD.ordinal()] = 59;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[Commands.KEYWORDS.UUID.ordinal()] = 60;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[Commands.KEYWORDS.VOTE.ordinal()] = 61;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[Commands.KEYWORDS.VOTIFIER.ordinal()] = 62;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[Commands.KEYWORDS.WEEK.ordinal()] = 63;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[Commands.KEYWORDS.YESTERDAY.ordinal()] = 64;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[Commands.KEYWORDS.YML.ordinal()] = 65;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Commands$KEYWORDS = var0;
         return var0;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Commands$SYNTAXERROR() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Commands$SYNTAXERROR;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[Commands.SYNTAXERROR.values().length];

         try {
            var0[Commands.SYNTAXERROR.HELP.ordinal()] = 6;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[Commands.SYNTAXERROR.INVALIDDATA.ordinal()] = 4;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[Commands.SYNTAXERROR.MISSINGTIME.ordinal()] = 2;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[Commands.SYNTAXERROR.SPECIAL.ordinal()] = 5;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[Commands.SYNTAXERROR.TOOFEW.ordinal()] = 1;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[Commands.SYNTAXERROR.UNKNOWNWORD.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Commands$SYNTAXERROR = var0;
         return var0;
      }
   }

   public static enum KEYWORDS {
      ADD("ADD"),
      AFK("AFK"),
      ALL("ALL"),
      AUDIT("AUD"),
      BACKUP("BAC"),
      CLEAN("CLE"),
      CONFIG("CONF"),
      CONSOLE("CONS"),
      DAT("DAT"),
      DAYS("DAYS"),
      DISABLE("DIS"),
      ENABLE("ENA"),
      EXPORT("EXP"),
      FIND("FIN"),
      FIRST("FIR"),
      HELP("HEL"),
      IMPORT("IMP"),
      LIMITS("LIM"),
      LIST("LIST"),
      LOGBLOCK("LOGB"),
      LOGFILE("LOGF"),
      LOGIN("LOGI"),
      LOGOUT("LOGO"),
      L("L"),
      MERGE("MER"),
      MESSAGE("MES"),
      MSG("MSG"),
      MONTH("MON"),
      MYSQL("MYS"),
      NEXT("NEXT"),
      ONLINE("ONL"),
      OUTPUT("OUT"),
      PLAYER("PLAYE"),
      PLAYTIME("PLAYT"),
      POINT("POI"),
      P_LOGIN("+LO"),
      P_MONTH("+MO"),
      P_RANK("+RA"),
      P_TODAY("+TO"),
      P_WEEK("+WE"),
      PURGE("PUR"),
      REFER("REF"),
      RELOAD("REL"),
      REMOVE("REM"),
      REPLACE("REPL"),
      REPORT("REPO"),
      RESUME("RES"),
      REWARD("REW"),
      RW("RW"),
      SAVE("SAVE"),
      SET("SET"),
      SITES("SIT"),
      SUBTRACT("SUB"),
      SUSPEND("SUS"),
      TEST("TES"),
      TODAY("TOD"),
      TOP("TOP"),
      TOTAL("TOT"),
      UNKNOWNWORD("UNK"),
      UUID("UUID"),
      VOTE("VOTE"),
      VOTIFIER("VOTI"),
      WEEK("WEE"),
      YESTERDAY("YES"),
      YML("YML"),
      NA("N/A");

      private final String input;

      private KEYWORDS(String input) {
         this.input = input;
      }

      public String input() {
         return this.input;
      }
   }

   public static enum SYNTAXERROR {
      TOOFEW,
      MISSINGTIME,
      UNKNOWNWORD,
      INVALIDDATA,
      SPECIAL,
      HELP;
   }
}
