package org.ontime.ontime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import me.edge209.OnTime.Rewards.RewardData;
import me.edge209.OnTime.Rewards.RewardInstance;
import me.edge209.OnTime.Rewards.Rewards;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Messages {
   private static OnTime _plugin;
   static File messagesFile;
   public static FileConfiguration messages;
   public static int adhocCount;

   public Messages(OnTime plugin) {
      _plugin = plugin;
   }

   public static void initMessages(File folder) {
      messagesFile = new File(folder, "messages.yml");
      if (!messagesFile.exists()) {
         messagesFile.getParentFile().mkdirs();
         _plugin.copy(_plugin.getResource("messages.yml"), messagesFile);
      }

      messages = new YamlConfiguration();

      try {
         messages.load(messagesFile);
      } catch (Exception var4) {
         var4.printStackTrace();
      }

      if (checkUpgrade(messages)) {
         LogFile.console(1, "[OnTime] Upgraded to latest version of messages.yml");
         YamlConfiguration refresh = new YamlConfiguration();

         try {
            refresh.load(messagesFile);
         } catch (Exception var3) {
            var3.printStackTrace();
         }

         messages = refresh;
      }

      adhocCount = messages.getInt("adhocCount");
      LogFile.console(1, "[OnTime] Loading from messages.yml");
   }

   public boolean handleMessage(String msgTag, String[] args, int msgStart, msgAction action, CommandSender sender) {
      boolean foundem = false;
      boolean newMessage = false;
      int oldlines = 0;
      if (action == Messages.msgAction.ADHOC) {
         if (messages.getString("message." + msgTag) != null) {
            return false;
         }

         newMessage = true;
      } else if (action == Messages.msgAction.ADD) {
         if (messages.getString("message." + msgTag) == null) {
            newMessage = true;
         } else {
            oldlines = messages.getInt("message." + msgTag + ".lines");
         }
      } else if (action == Messages.msgAction.REMOVE && !msgTag.contains("adhoc") && messages.getString("message." + msgTag) == null) {
         return false;
      }

      File outfile = new File(OnTime.onTimeDataFolder, "messages-temp.yml");
      File infile = new File(OnTime.onTimeDataFolder, "messages.yml");
      createFile(outfile);

      try {
         BufferedWriter out = new BufferedWriter(new FileWriter(outfile, true));
         InputStream in = new FileInputStream(infile);
         BufferedReader inStream = new BufferedReader(new InputStreamReader(in));
         String line = inStream.readLine();
         int i;
         if (action != Messages.msgAction.ADD && action != Messages.msgAction.ADHOC) {
            if (action == Messages.msgAction.REMOVE) {
               for(; line != null; line = inStream.readLine()) {
                  if (!line.contains(msgTag)) {
                     out.write(line);
                     out.newLine();
                  } else {
                     foundem = true;
                     i = messages.getInt("message." + msgTag + ".lines");
                     if (i > 0) {
                        for(int i = 0; i <= i; ++i) {
                           line = inStream.readLine();
                        }
                     }
                  }
               }
            } else if (action == Messages.msgAction.LIST) {
               for(; line != null && !foundem; line = inStream.readLine()) {
                  if (line.contains("message:")) {
                     foundem = true;
                  }
               }

               for(foundem = false; line != null && !line.contains("endFile"); line = inStream.readLine()) {
                  if (!line.contains("#") && line.length() > 0) {
                     sender.sendMessage(line);
                     foundem = true;
                  }
               }
            }
         } else {
            while(line != null) {
               if (line.contains("adhocCount")) {
                  this.outline(out, "adhocCount: " + adhocCount);
               } else if (line.startsWith("#")) {
                  this.outline(out, line);
               } else if (line.startsWith("message:")) {
                  this.outline(out, line);
                  if (newMessage && action == Messages.msgAction.ADHOC) {
                     out.newLine();
                     this.outline(out, "   " + msgTag + ": '");

                     for(i = msgStart; i < args.length; ++i) {
                        this.outline(out, args[i] + " ");
                     }

                     this.outline(out, "'");
                     foundem = true;
                  } else if (newMessage && action == Messages.msgAction.ADD) {
                     out.newLine();
                     this.outline(out, "   " + msgTag + ":");
                     out.newLine();
                     this.outline(out, "      lines: 1");
                     out.newLine();
                     this.outline(out, "      line-1: '");

                     for(i = msgStart; i < args.length; ++i) {
                        this.outline(out, args[i] + " ");
                     }

                     this.outline(out, "'");
                     foundem = true;
                  }
               } else if (!line.contains(msgTag + ":")) {
                  this.outline(out, line);
               } else {
                  this.outline(out, line);
                  out.newLine();
                  this.outline(out, "      lines: " + (oldlines + 1));
                  out.newLine();
                  line = inStream.readLine();

                  for(i = 0; i < oldlines; ++i) {
                     line = inStream.readLine();
                     this.outline(out, line);
                     out.newLine();
                  }

                  this.outline(out, "      line-" + (oldlines + 1) + ": '");

                  for(i = msgStart; i < args.length; ++i) {
                     this.outline(out, args[i] + " ");
                  }

                  this.outline(out, "'");
                  foundem = true;
               }

               out.newLine();
               line = inStream.readLine();
            }
         }

         out.close();
         in.close();
         if (action == Messages.msgAction.LIST) {
            outfile.delete();
         } else {
            infile.delete();
            outfile.renameTo(infile);
            initMessages(OnTime.onTimeDataFolder);
         }

         return foundem;
      } catch (Exception var17) {
         var17.printStackTrace();
         return false;
      }
   }

   void outline(BufferedWriter out, String line) {
      try {
         out.write(line);
      } catch (IOException var4) {
         var4.printStackTrace();
      }

   }

   public boolean setAdhocMessage(String playerName, RewardData.EventReference relation, long time, String[] args, int messageStart, CommandSender sender) {
      LogFile.write(1, "{Messages.setAdhocMessage}");
      ++adhocCount;
      String msgTag = "adhoc" + adhocCount;
      _plugin.get_messages().handleMessage(msgTag, args, messageStart, Messages.msgAction.ADHOC, sender);
      return this.setMessage(playerName, relation, time, msgTag);
   }

   public boolean setMessage(final String playerName, RewardData.EventReference reference, long time, final String msgTag) {
      RewardInstance[] rewards = null;
      long delayTime = 0L;
      if (!OnTime.messagesEnable) {
         return false;
      } else {
         final int record = _plugin.get_rewards().getRewardSlot(playerName);
         rewards = Rewards.getPlayerRewards(playerName);
         rewards[record].reference = reference;
         if (rewards[record].reference != RewardData.EventReference.DELTATIME && rewards[record].reference != RewardData.EventReference.REALTIME) {
            if (rewards[record].reference == RewardData.EventReference.PLAYTIME) {
               delayTime = time - _plugin.get_playingtime().totalOntime(playerName);
            } else {
               delayTime = 0L;
            }
         } else {
            delayTime = time;
         }

         int rewardTaskID;
         if (_plugin.get_logintime().playerIsOnline(Players.getData(playerName)) && rewards[record].reference != RewardData.EventReference.LOGIN) {
            LogFile.write(1, "Message " + msgTag + " set for " + playerName);
            rewardTaskID = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new Runnable() {
               public void run() {
                  Messages._plugin.get_messages().issue(playerName, msgTag, record, (String[])null);
               }
            }, delayTime / 50L);
         } else {
            rewardTaskID = -1;
         }

         rewards[record].index = record;
         rewards[record].active = true;
         rewards[record].scheduleID = rewardTaskID;
         rewards[record].rewardID = -1;
         rewards[record].identifier = "message." + msgTag;
         rewards[record].form = RewardInstance.RewardForm.MESSAGE;
         rewards[record].time = _plugin.get_rewards().getPersonalRewardTime(time, rewards[record].reference, playerName);
         Rewards.putPlayerRewards(playerName, rewards);
         return true;
      }
   }

   public void loginAnnouncement(String msgTag, Player player) {
      String excludedPlayer = player.getName();
      if (OnTime.permission.has(player, "ontime.welcome_msg")) {
         String[] data = new String[]{excludedPlayer, Output.getTimeBreakdown(_plugin.get_playingtime().totalOntime(excludedPlayer), Output.TIMEDETAIL.SHORT)};
         Iterator var6 = _plugin.getServer().getOnlinePlayers().iterator();

         while(var6.hasNext()) {
            Player otherPlayer = (Player)var6.next();
            String playerName = OnTime.getPlayerName(otherPlayer);
            if (!playerName.toLowerCase().endsWith(excludedPlayer.toLowerCase())) {
               this.generate(msgTag, playerName, data);
            }
         }

      }
   }

   public void issue(String playerName, String msgTag, int position, String[] data) {
      this.generate(msgTag, playerName, data);
      if (msgTag.contains("adhoc")) {
         this.handleMessage(msgTag, (String[])null, 0, Messages.msgAction.REMOVE, _plugin.getServer().getConsoleSender());
      }

      RewardInstance[] playersRewards = Rewards.getPlayerRewards(playerName);
      playersRewards[position].active = false;
      Rewards.putPlayerRewards(playerName, playersRewards);
   }

   public boolean generate(String msgTag, String playerName, String[] data) {
      int numLines = false;
      PlayerData playerData = null;
      if ((playerData = Players.getData(playerName)) == null) {
         LogFile.write(10, "{Messages.generate} No playerdata found for " + playerName + " when attempting message: " + msgTag);
         return false;
      } else {
         Player player = _plugin.getServer().getPlayer(playerData.uuid);
         if (player == null) {
            LogFile.write(3, "{Message.generate} 'player' came back null for " + playerName + "( UUID:" + playerData.uuid + ")");
            return false;
         } else {
            int numLines = messages.getInt(msgTag + ".lines");
            String line = null;
            if (numLines == 0) {
               line = messages.getString("message." + msgTag);
               if (line != null) {
                  msgTag = "message." + msgTag;
                  numLines = messages.getInt(msgTag + ".lines");
                  line = null;
               }
            }

            for(int i = 1; i <= numLines; ++i) {
               if (line == null) {
                  line = messages.getString(msgTag + ".line-" + i);
               }

               if (line != null) {
                  String message = Output.lineOut(Output.OnTimeOutput, line, playerName, playerData, (RewardData)null, -1, -1, true, data);
                  if (message != null) {
                     player.sendMessage(message);
                  } else {
                     LogFile.write(3, "{Messages.generate} Error processing message: '" + line + "'");
                  }

                  line = null;
               } else {
                  LogFile.write(3, "{Message.Write} Message not found " + msgTag);
               }
            }

            return true;
         }
      }
   }

   public static boolean checkUpgrade(FileConfiguration messagesconfig) {
      int version = messagesconfig.getInt("messagesVersion");
      if (version == 3) {
         return false;
      } else {
         File outfile = new File(OnTime.onTimeDataFolder, "messages-temp.yml");
         File infile = new File(OnTime.onTimeDataFolder, "messages.yml");
         createFile(outfile);

         try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outfile, true));
            InputStream in = new FileInputStream(infile);
            BufferedReader inStream = new BufferedReader(new InputStreamReader(in));

            for(String line = inStream.readLine(); line != null; line = inStream.readLine()) {
               if (line.contains("messagesVersion:")) {
                  out.write("messagesVersion: 3");
               } else if (!line.contains("dateTimeFormat")) {
                  if (line.startsWith("message:")) {
                     out.write(line);
                     out.newLine();

                     for(line = inStream.readLine(); !line.startsWith("#"); line = inStream.readLine()) {
                        out.write(line.substring(0, line.indexOf(":") + 1));
                        out.newLine();
                        out.write("      lines: 1");
                        out.newLine();
                        out.write("      line-1:" + line.substring(line.indexOf(":") + 1));
                        out.newLine();
                        out.newLine();
                     }
                  } else {
                     out.write(line);
                  }
               }

               out.newLine();
            }

            out.close();
            in.close();
            infile.delete();
            outfile.renameTo(infile);
            return true;
         } catch (Exception var8) {
            var8.printStackTrace();
            return false;
         }
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

   public static enum msgAction {
      ADD,
      REMOVE,
      LIST,
      ADHOC;
   }
}
