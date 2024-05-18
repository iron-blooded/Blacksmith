package me.edge209.OnTime.Rewards;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import me.edge209.OnTime.DataIO;
import me.edge209.OnTime.LogFile;
import me.edge209.OnTime.OnTime;
import me.edge209.OnTime.PlayerData;
import me.edge209.OnTime.Players;
import me.edge209.OnTime.Updates;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class RewardUtilities {
   private static OnTime _plugin;
   public static final Logger logger = Logger.getLogger("Minecraft");
   public HashMap referredbyMap = new HashMap();

   public RewardUtilities(OnTime plugin) {
      _plugin = plugin;
   }

   public HashMap getReferredbyMap() {
      return this.referredbyMap;
   }

   public void setReferredMap(HashMap map) {
      this.referredbyMap = map;
   }

   public static boolean checkUpgrade(File file, FileConfiguration rewardconfig) {
      int version = rewardconfig.getInt("version");
      if (version == 13) {
         return false;
      } else if (version > 13) {
         LogFile.console(3, "Error. You are using a rewards.yml version (" + version + ") that may not be compatible with " + OnTime.pluginVersion);
         return false;
      } else if (version == 0) {
         logger.severe("[ONTIME] Rewards have been disabled due to a corruption of your rewards.yml. Each rewards.yml file should contain ");
         logger.severe("[ONTIME] a 'version' number specified on the 11th line of that file, but this seems to be missing.");
         logger.severe("[ONTIME] You can delete your rewards.yml and let OnTime generate a new one, or contact OnTime support for help.");
         OnTime.rewardsEnable = false;
         return false;
      } else if (version <= 7) {
         logger.severe("[ONTIME] Rewards have been disabled due to rewards.yml incompatibility. Your rewards.yml is version 7 or earlier");
         logger.severe("[ONTIME] You must install and reset server with OnTime version 3.6.5, before installing OnTime version 3.11.0 or later.");
         OnTime.rewardsEnable = false;
         return false;
      } else {
         File outfile = new File(OnTime.onTimeDataFolder, "rewards_temp.yml");
         File infile = new File(OnTime.onTimeDataFolder, "rewards.yml");
         createFile(outfile);

         try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outfile, true));
            InputStream in = new FileInputStream(infile);
            BufferedReader inStream = new BufferedReader(new InputStreamReader(in));

            label129:
            for(String line = inStream.readLine(); line != null; line = inStream.readLine()) {
               if (line.startsWith("#")) {
                  out.write(line);
                  out.newLine();
               } else if (line.contains("version")) {
                  out.write("version: 13");
                  out.newLine();
               } else if (!line.startsWith("rewards")) {
                  out.write(line);
                  out.newLine();
               } else {
                  List rewards = rewardconfig.getStringList("rewards");
                  out.write("rewards:");
                  out.newLine();
                  if (rewards.size() == 0) {
                     LogFile.console(1, "[OnTime] Reward file conversion: no rewards found to convert.");
                  } else {
                     class argClass {
                        int RECURRANCE = 0;
                        int EXCLUSIVE = 1;
                        int SCOPE = 2;
                        int REFERENCE = 3;
                        int LINK = 4;
                        int MESSAGE = 5;
                        int WORLD = 6;
                        int TIME_D = 7;
                        int TIME_H = 8;
                        int TIME_M = 9;
                        int RTIME_D = 10;
                        int RTIME_H = 11;
                        int RTIME_M = 12;
                        int RCOUNT = 13;
                        int TYPE = 14;
                        int QUANTITY = 15;
                        int REWARD = 16;
                        int ID = 17;
                     }

                     argClass ARG = new argClass();

                     for(Iterator var12 = rewards.iterator(); var12.hasNext(); out.newLine()) {
                        String s = (String)var12.next();
                        boolean change = false;
                        String delims = "[,]";
                        String[] tokens = s.split(delims);
                        String[] copy;
                        if (version < 13) {
                           copy = new String[ARG.ID + 1];

                           int i;
                           for(i = ARG.ID; i >= ARG.TIME_D; --i) {
                              copy[i] = tokens[i - 1];
                           }

                           copy[ARG.WORLD] = "all";

                           for(i = ARG.MESSAGE; i >= ARG.RECURRANCE; --i) {
                              copy[i] = tokens[i];
                           }

                           tokens = copy;
                           change = true;
                        }

                        copy = null;
                        String occurs = null;
                        String scope;
                        if (tokens[ARG.RECURRANCE].startsWith("T") && tokens[ARG.RECURRANCE].length() > 1) {
                           occurs = "T";
                           scope = tokens[ARG.RECURRANCE].substring(1);
                           change = true;
                        } else {
                           occurs = tokens[ARG.RECURRANCE];
                           scope = tokens[ARG.SCOPE];
                        }

                        if (tokens[ARG.TYPE].equalsIgnoreCase("G")) {
                           tokens[ARG.TYPE] = "+R";
                           change = true;
                        }

                        if (tokens[ARG.TYPE].equalsIgnoreCase("P")) {
                           tokens[ARG.TYPE] = "+P";
                           change = true;
                        }

                        if (tokens[ARG.RECURRANCE].equalsIgnoreCase("V")) {
                           int count = Integer.parseInt(tokens[ARG.RCOUNT]);
                           if (count > 0) {
                              occurs = "VP";
                           } else {
                              occurs = "VS";
                           }

                           change = true;
                        }

                        if (change) {
                           out.write("   - " + occurs + "," + tokens[ARG.EXCLUSIVE] + "," + scope + "," + tokens[ARG.REFERENCE] + "," + tokens[ARG.LINK] + "," + tokens[ARG.MESSAGE] + "," + tokens[ARG.WORLD] + "," + tokens[ARG.TIME_D] + "," + tokens[ARG.TIME_H] + "," + tokens[ARG.TIME_M] + "," + tokens[ARG.RTIME_D] + "," + tokens[ARG.RTIME_H] + "," + tokens[ARG.RTIME_M] + "," + tokens[ARG.RCOUNT] + "," + tokens[ARG.TYPE] + "," + tokens[ARG.QUANTITY] + "," + tokens[ARG.REWARD] + "," + tokens[ARG.ID]);
                        } else {
                           out.write("   - " + s);
                        }
                     }

                     line = inStream.readLine();

                     do {
                        if (line.contains("#")) {
                           continue label129;
                        }

                        line = inStream.readLine();
                     } while(line != null);

                     line = "#";
                  }
               }
            }

            out.close();
            in.close();
            File backup = Updates.createBackupFile("rewards", "yml");
            LogFile.console(0, "Backup File name: " + backup.getPath());
            if (!infile.renameTo(backup)) {
               if (!infile.delete()) {
                  LogFile.console(3, "reward.yml auto upgrade error.  Update Failed.");
                  return false;
               }

               LogFile.console(3, "reward.yml auto upgrade error.  Backup file not created.");
            }

            if (!outfile.renameTo(infile)) {
               LogFile.console(3, "reward.yml auto updgrade failure.  File rename failure. Update Failed.");
               return false;
            }
         } catch (Exception var19) {
            var19.printStackTrace();
         }

         return true;
      }
   }

   public static void saveRewards(File file) {
      File outfile = new File(OnTime.onTimeDataFolder, "temp.yml");
      File infile = new File(OnTime.onTimeDataFolder, "rewards.yml");
      createFile(outfile);

      try {
         BufferedWriter out = new BufferedWriter(new FileWriter(outfile, true));
         InputStream in = new FileInputStream(infile);
         BufferedReader inStream = new BufferedReader(new InputStreamReader(in));

         for(String line = inStream.readLine(); line != null; line = inStream.readLine()) {
            if (!line.startsWith("#")) {
               if (line.contains("rewardIDCounter")) {
                  out.write("rewardIDCounter: " + Rewards.rewardIDCounter);
                  out.newLine();
                  line = inStream.readLine();
               } else {
                  Iterator it;
                  if (line.contains("worlds")) {
                     out.write(line);
                     out.newLine();
                     it = _plugin.get_rewards().enabledWorlds.iterator();

                     while(it.hasNext()) {
                        out.write("   - " + (String)it.next());
                        out.newLine();
                     }

                     while(!line.contains("#")) {
                        line = inStream.readLine();
                     }
                  } else if (line.contains("groups")) {
                     out.write(line);
                     out.newLine();
                     it = _plugin.get_rewards().groups.iterator();

                     while(it.hasNext()) {
                        out.write("   - " + (String)it.next());
                        out.newLine();
                     }

                     while(!line.contains("#")) {
                        line = inStream.readLine();
                     }
                  } else if (line.contains("commands")) {
                     out.write(line);
                     out.newLine();
                     it = _plugin.get_rewards().getCommands().iterator();

                     while(it.hasNext()) {
                        out.write("   - " + (String)it.next());
                        out.newLine();
                     }

                     while(!line.contains("#")) {
                        line = inStream.readLine();
                     }
                  } else if (line.contains("rewards")) {
                     out.write(line);
                     out.newLine();

                     for(int i = 0; i < _plugin.get_rewards().getNumDefinedRewards(); ++i) {
                        long days = 0L;
                        long hours = 0L;
                        long mins = 0L;
                        long r_days = 0L;
                        long r_hours = 0L;
                        long r_mins = 0L;
                        if (_plugin.get_rewards().getRewardData()[i].occurs == RewardData.Occurs.TOP) {
                           mins = _plugin.get_rewards().getRewardData()[i].time;
                           r_mins = _plugin.get_rewards().getRewardData()[i].recurranceTime;
                        } else {
                           days = TimeUnit.MILLISECONDS.toDays(_plugin.get_rewards().getRewardData()[i].time);
                           hours = TimeUnit.MILLISECONDS.toHours(_plugin.get_rewards().getRewardData()[i].time - TimeUnit.DAYS.toMillis(days));
                           mins = TimeUnit.MILLISECONDS.toMinutes(_plugin.get_rewards().getRewardData()[i].time - TimeUnit.HOURS.toMillis(hours) - TimeUnit.DAYS.toMillis(days));
                           r_days = TimeUnit.MILLISECONDS.toDays(_plugin.get_rewards().getRewardData()[i].recurranceTime);
                           r_hours = TimeUnit.MILLISECONDS.toHours(_plugin.get_rewards().getRewardData()[i].recurranceTime - TimeUnit.DAYS.toMillis(r_days));
                           r_mins = TimeUnit.MILLISECONDS.toMinutes(_plugin.get_rewards().getRewardData()[i].recurranceTime - TimeUnit.HOURS.toMillis(r_hours) - TimeUnit.DAYS.toMillis(r_days));
                        }

                        String world = null;
                        if (_plugin.get_rewards().getRewardData()[i].onWorld) {
                           world = "+" + _plugin.get_rewards().getRewardData()[i].world;
                        } else {
                           world = _plugin.get_rewards().getRewardData()[i].world;
                        }

                        out.write("   - " + _plugin.get_rewards().getRewardData()[i].occurs.code() + "," + _plugin.get_rewards().getRewardData()[i].exclusive + "," + _plugin.get_rewards().getRewardData()[i].scope.code() + "," + _plugin.get_rewards().getRewardData()[i].reference.code() + "," + _plugin.get_rewards().getRewardData()[i].link + "," + _plugin.get_rewards().getRewardData()[i].message + "," + world + "," + days + "," + hours + "," + mins + "," + r_days + "," + r_hours + "," + r_mins + "," + _plugin.get_rewards().getRewardData()[i].count + "," + _plugin.get_rewards().getRewardData()[i].type.code() + "," + _plugin.get_rewards().getRewardData()[i].getQuantity() + "," + _plugin.get_rewards().getRewardData()[i].reward + "," + _plugin.get_rewards().getRewardData()[i].identifier);
                        out.newLine();
                     }

                     while(!line.contains("#")) {
                        line = inStream.readLine();
                        if (line == null) {
                           line = "#";
                        }
                     }
                  }
               }
            }

            out.write(line);
            out.newLine();
         }

         out.close();
         in.close();
         infile.delete();
         outfile.renameTo(infile);
      } catch (Exception var22) {
         var22.printStackTrace();
      }

   }

   public static String getTerseRewardTimeBreakdown(long millis) {
      StringBuilder sb = new StringBuilder(64);
      if (millis < 0L) {
         sb.append(" N/A");
         return sb.toString();
      } else if (millis >= TimeUnit.DAYS.toMillis(9999L)) {
         sb.append(" (Individual)");
         return sb.toString();
      } else {
         if (millis < 1000L) {
            sb.append("Immediate");
         } else {
            long days = TimeUnit.MILLISECONDS.toDays(millis);
            millis -= TimeUnit.DAYS.toMillis(days);
            long hours = TimeUnit.MILLISECONDS.toHours(millis);
            millis -= TimeUnit.HOURS.toMillis(hours);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
            millis -= TimeUnit.MINUTES.toMillis(minutes);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
            if (days > 0L) {
               sb.append(days);
               sb.append("D ");
            }

            if (hours > 0L) {
               sb.append(hours);
               sb.append("H ");
            }

            if (minutes > 0L) {
               sb.append(minutes);
               sb.append("M ");
            }

            if (minutes < 5L && seconds > 0L) {
               return seconds + "S ";
            }
         }

         return sb.toString();
      }
   }

   public static void writeLine(File file, String line) {
      try {
         BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
         out.write(line);
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

   public static boolean deleteFile(File file) {
      if (file.exists()) {
         file.delete();
         return true;
      } else {
         return false;
      }
   }

   public static void loadPotionNames(File directory) {
      File infile = new File(OnTime.onTimeDataFolder, "help.txt");
      if (!infile.exists()) {
         infile.getParentFile().mkdirs();
         _plugin.copy(_plugin.getResource("help.txt"), infile);
         LogFile.console(1, "Created file 'help.txt'");
      }

      try {
         InputStream in = new FileInputStream(infile);
         BufferedReader inStream = new BufferedReader(new InputStreamReader(in));

         for(String line = inStream.readLine(); line != null; line = inStream.readLine()) {
            if (line.contains("START_potion")) {
               for(line = inStream.readLine(); line != null && !line.contains("END_potion"); line = inStream.readLine()) {
                  if (!line.contains("START_") && !line.contains("END_") && !line.startsWith("#") && !line.isEmpty()) {
                     String[] tokens = line.split("[-]");
                     tokens[0] = tokens[0].trim();
                     if (tokens.length == 2) {
                        if (tokens[0].matches("[+]?\\d+(\\/\\d+)?")) {
                           tokens[1].trim();
                           _plugin.get_rewards().getPotionsMap().put(Short.parseShort(tokens[0]), tokens[1]);
                        } else {
                           LogFile.write(1, "Invalid number in ontime/help.txt at " + line);
                        }
                     } else {
                        LogFile.write(1, "Invalid line in ontime/help.txt at: " + line);
                     }
                  }
               }
            }
         }

         inStream.close();
      } catch (Exception var6) {
         var6.printStackTrace();
      }

   }

   public boolean canBeReferred(Player player) {
      String playerName = OnTime.getPlayerName(player);
      if (OnTime.referredByPermTrackEnable && _plugin.get_permissionsHandler().playerHas(playerName, "ontime.referredby.success")) {
         LogFile.console(0, "{canBeReferred} " + playerName + " has ontime.referredby.success permission string");
         return false;
      } else {
         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            PlayerData playerData = null;
            if ((playerData = Players.getData(playerName)) == null) {
               LogFile.console(3, "ERROR: {canBeReferred} " + playerName + " playerData not found.");
               return false;
            }

            if (playerData.referredBy == null) {
               return true;
            }

            if (!playerData.referredBy.equalsIgnoreCase("null")) {
               LogFile.console(0, "{canBeReferred} " + playerName + " has source (" + playerData.referredBy + ") identified in MYSQL");
               return false;
            }

            LogFile.console(0, "{canBeReferred} " + playerName + " has source (*null*) identified in MYSQL");
         }

         if (Rewards.get_rewardUtilities().getReferredbyMap().containsKey(playerName)) {
            LogFile.console(0, "{canBeReferred} " + playerName + " found in temp map.");
            return false;
         } else {
            return true;
         }
      }
   }
}
