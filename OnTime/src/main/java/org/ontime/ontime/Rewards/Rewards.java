package me.edge209.OnTime.Rewards;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import me.edge209.OnTime.DataIO;
import me.edge209.OnTime.LogFile;
import me.edge209.OnTime.Messages;
import me.edge209.OnTime.OnTime;
import me.edge209.OnTime.Output;
import me.edge209.OnTime.PermissionsHandler;
import me.edge209.OnTime.PlayTimeData;
import me.edge209.OnTime.PlayerData;
import me.edge209.OnTime.Players;
import me.edge209.OnTime.TodayTime;
import me.edge209.OnTime.ValueComparator;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Rewards {
   private static OnTime _plugin;
   private static RewardUtilities _rewardUtilities;
   public static File rewardFile;
   FileConfiguration rewards;
   static int definedRewards = 0;
   static int rewardIDCounter = 0;
   public int[] scopeCount;
   public RewardData delayReward = this.createDelayReward((RewardData.timeScope)null);
   private RewardData[] rewardData;
   File indiRewardFile;
   FileConfiguration indiRewardData;
   List groups;
   String[] groupList = null;
   List kits;
   List enabledWorlds;
   private List commands;
   public static boolean deathReward = false;
   public HashMap playerRewardMap = new HashMap();
   public HashMap potionsMap = new HashMap();
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$timeScope;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$EventReference;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$RewardType;

   public Rewards(OnTime plugin) {
      _plugin = plugin;
      this.set_rewardUtilities(new RewardUtilities(plugin));
   }

   public List getEnabledWorlds() {
      return this.enabledWorlds;
   }

   private void setEnabledWorlds(List _enabledWorlds) {
      this.enabledWorlds = _enabledWorlds;
   }

   public HashMap getRewardMap() {
      return this.playerRewardMap;
   }

   public static RewardInstance[] getPlayerRewards(String playerName) {
      return (RewardInstance[])_plugin.get_rewards().getRewardMap().get(playerName.toLowerCase());
   }

   public static void putPlayerRewards(String playerName, RewardInstance[] rewards) {
      _plugin.get_rewards().getRewardMap().put(playerName.toLowerCase(), rewards);
   }

   public static boolean playerHasRewards(String playerName) {
      return _plugin.get_rewards().getRewardMap().containsKey(playerName.toLowerCase());
   }

   public HashMap getPotionsMap() {
      return this.potionsMap;
   }

   public void setPotionsMap(HashMap map) {
      this.potionsMap = map;
   }

   public RewardData createDelayReward(RewardData.timeScope scope) {
      return new RewardData(RewardData.Occurs.SINGLE, "A", scope, RewardData.EventReference.PLAYTIME, (String)null, "default", "all", 0L, 0L, 1, RewardData.RewardType.DELAY, 0, "delay", "ontime.reward.delay");
   }

   public void initRewards(File folder) {
      rewardFile = new File(folder, "rewards.yml");
      if (!rewardFile.exists()) {
         rewardFile.getParentFile().mkdirs();
         _plugin.copy(_plugin.getResource("rewards.yml"), rewardFile);
         LogFile.console(1, "new 'rewards.yml' created.");
      }

      this.rewards = new YamlConfiguration();

      try {
         this.rewards.load(rewardFile);
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      RewardUtilities.loadPotionNames(OnTime.onTimeDataFolder);
      if (RewardUtilities.checkUpgrade(rewardFile, this.rewards)) {
         LogFile.console(1, "Upgraded rewards.yaml to latest version.");
         YamlConfiguration refresh = new YamlConfiguration();

         try {
            refresh.load(rewardFile);
         } catch (Exception var4) {
            var4.printStackTrace();
         }

         this.rewards = refresh;
      }

      if (OnTime.rewardsEnable || OnTime.messagesEnable) {
         LogFile.console(1, "Loading from rewards.yaml");
         if (OnTime.permission.isEnabled()) {
            List wordList = null;
            this.groupList = OnTime.permission.getGroups();
            if (this.groupList != null) {
               wordList = Arrays.asList(this.groupList);
            }

            LogFile.console(1, "Groups supported by " + OnTime.permission.getName() + ":" + wordList);
         }

         this.initialize(this.rewards);
      }

   }

   public void initIndiRewards(File folder) {
      this.indiRewardFile = new File(folder, "indirewards.yml");
      if (this.indiRewardFile.exists()) {
         this.indiRewardData = new YamlConfiguration();

         try {
            this.indiRewardData.load(this.indiRewardFile);
         } catch (Exception var16) {
            var16.printStackTrace();
            return;
         }

         LogFile.console(1, "Loading from indirewards.yaml");
         List rewards = this.indiRewardData.getStringList("indirewards");
         if (rewards.size() > 0) {
            Iterator var6 = rewards.iterator();

            while(true) {
               while(var6.hasNext()) {
                  String s = (String)var6.next();
                  String[] tokens = s.split("[,]");
                  String playerName = tokens[0];
                  int rewardID = -1;
                  if (Integer.parseInt(tokens[1]) >= 0) {
                     rewardID = this._validateIdentifier(tokens[4], Integer.parseInt(tokens[1]));
                  }

                  if (rewardID < 0 && !tokens[4].startsWith("message.")) {
                     LogFile.write(3, "Saved indi reward (" + tokens[4] + ") no longer exists.  Reward was cancelled for " + playerName);
                  } else {
                     if (tokens[2].equalsIgnoreCase("ONTIME")) {
                        tokens[2] = "Play";
                     }

                     RewardData.EventReference reference = RewardData.EventReference.PLAYTIME;
                     RewardData.EventReference[] var15;
                     int var14 = (var15 = RewardData.EventReference.values()).length;

                     for(int var13 = 0; var13 < var14; ++var13) {
                        RewardData.EventReference r1 = var15[var13];
                        if (tokens[2].equalsIgnoreCase(r1.label())) {
                           reference = r1;
                        }
                     }

                     long rewardTime;
                     if (reference == RewardData.EventReference.DELTATIME) {
                        rewardTime = Long.valueOf(tokens[3]) - _plugin.get_playingtime().totalOntime(playerName);
                        if (rewardTime < 0L) {
                           rewardTime = 0L;
                        }
                     } else if (reference == RewardData.EventReference.PLAYTIME) {
                        rewardTime = Long.valueOf(tokens[3]);
                     } else if (reference == RewardData.EventReference.REALTIME) {
                        rewardTime = Long.valueOf(tokens[3]) - Calendar.getInstance().getTimeInMillis();
                     } else {
                        rewardTime = 0L;
                     }

                     String[] data = null;
                     if (tokens.length > 5) {
                        data = new String[tokens.length - 5];

                        for(int i = 0; i < tokens.length - 5; ++i) {
                           data[i] = tokens[5 + i];
                        }
                     }

                     if (tokens[4].startsWith("message.")) {
                        LogFile.write(1, "Indi message: " + tokens[4] + " set for " + playerName + " time: " + Output.getTimeBreakdown(rewardTime, Output.TIMEDETAIL.SHORT));
                        _plugin.get_messages().setMessage(playerName, reference, rewardTime, tokens[4].substring(8, tokens[4].length()));
                     } else {
                        LogFile.write(1, "Indi reward: " + tokens[4] + " set for " + playerName + " time: " + Output.getTimeBreakdown(rewardTime, Output.TIMEDETAIL.SHORT));
                        this.setReward(playerName, reference, rewardTime, rewardID, this.getRewardData()[rewardID], data);
                     }
                  }
               }

               LogFile.write(2, "Individual Rewards:  " + rewards.size() + " rewards loaded.");
               return;
            }
         }
      }

      LogFile.console(1, "No Individual Rewards to process.");
      LogFile.write(1, "Individual Reward initialization: No rewards to process.");
   }

   public void initialize(FileConfiguration rewardconfig) {
      argClass ARG = new argClass();
      Boolean saveUpdate = false;
      if (this.scopeCount == null) {
         this.scopeCount = new int[4];
      }

      for(int i = 0; i < 4; ++i) {
         this.scopeCount[i] = 0;
      }

      List rewards = rewardconfig.getStringList("rewards");
      rewardIDCounter = rewardconfig.getInt("rewardIDCounter");
      String delims;
      if (rewards.size() == 0) {
         LogFile.console(1, "Rewards enabled, but no rewards defined.");
         LogFile.write(2, "Reward initialization: Rewards enabled, but no rewards defined.");
         this.setNumDefinedRewards(0);
      } else {
         this.setRewardData(new RewardData[rewards.size()]);
         int index = 0;

         for(Iterator var7 = rewards.iterator(); var7.hasNext(); ++index) {
            delims = (String)var7.next();
            LogFile.write(0, "Processing reward string:" + delims);
            String[] tokens = delims.split("[,]");
            long time = 0L;
            long rtime = 0L;
            if (tokens[ARG.RECURRANCE].startsWith("T")) {
               time = Long.parseLong(tokens[ARG.TIME_M]);
               rtime = Long.parseLong(tokens[ARG.RTIME_M]);
            } else {
               time = TimeUnit.DAYS.toMillis(Long.parseLong(tokens[ARG.TIME_D])) + TimeUnit.HOURS.toMillis(Long.parseLong(tokens[ARG.TIME_H])) + TimeUnit.MINUTES.toMillis(Long.parseLong(tokens[ARG.TIME_M]));
               rtime = TimeUnit.DAYS.toMillis(Long.parseLong(tokens[ARG.RTIME_D])) + TimeUnit.HOURS.toMillis(Long.parseLong(tokens[ARG.RTIME_H])) + TimeUnit.MINUTES.toMillis(Long.parseLong(tokens[ARG.RTIME_M]));

               for(int i = 0; i < index; ++i) {
                  if (time == this.getRewardData()[i].time) {
                     ++time;
                  }
               }
            }

            String identifier = tokens[ARG.ID];
            if (identifier.equalsIgnoreCase("TBD")) {
               identifier = tokens[ARG.RECURRANCE] + String.valueOf(rewardIDCounter) + tokens[ARG.TYPE] + tokens[ARG.QUANTITY] + tokens[ARG.REWARD];
               ++rewardIDCounter;
               saveUpdate = true;
            }

            RewardData.timeScope scope = RewardData.timeScope.TOTAL;
            RewardData.timeScope[] var18;
            int var17 = (var18 = RewardData.timeScope.values()).length;

            for(int var16 = 0; var16 < var17; ++var16) {
               RewardData.timeScope s1 = var18[var16];
               if (tokens[ARG.SCOPE].equalsIgnoreCase(s1.code())) {
                  scope = s1;
                  int var10002 = this.scopeCount[s1.ordinal()]++;
               }
            }

            RewardData.EventReference reference = RewardData.EventReference.PLAYTIME;
            RewardData.EventReference[] var19;
            int var37 = (var19 = RewardData.EventReference.values()).length;

            for(var17 = 0; var17 < var37; ++var17) {
               RewardData.EventReference r1 = var19[var17];
               if (tokens[ARG.REFERENCE].equalsIgnoreCase(r1.code())) {
                  reference = r1;
               }
            }

            if (reference == RewardData.EventReference.DEATH) {
               deathReward = true;
            }

            RewardData.RewardType type = RewardData.RewardType.DELAY;
            RewardData.RewardType[] var20;
            int var39 = (var20 = RewardData.RewardType.values()).length;

            for(var37 = 0; var37 < var39; ++var37) {
               RewardData.RewardType t1 = var20[var37];
               if (tokens[ARG.TYPE].equalsIgnoreCase(t1.code())) {
                  type = t1;
               }
            }

            RewardData.Occurs occurs = RewardData.Occurs.SINGLE;
            RewardData.Occurs[] var21;
            int var41 = (var21 = RewardData.Occurs.values()).length;

            for(var39 = 0; var39 < var41; ++var39) {
               RewardData.Occurs o1 = var21[var39];
               if (tokens[ARG.RECURRANCE].equalsIgnoreCase(o1.code())) {
                  occurs = o1;
               }
            }

            if (tokens[ARG.LINK].equalsIgnoreCase("null")) {
               tokens[ARG.LINK] = null;
            }

            if (!tokens[ARG.MESSAGE].equalsIgnoreCase("default") && !tokens[ARG.MESSAGE].equalsIgnoreCase("off") && Messages.messages.getString("message." + tokens[ARG.MESSAGE]) == null) {
               LogFile.console(3, "WARNING: Reward with tag =" + identifier + " is assigned an undefined message: " + tokens[ARG.MESSAGE]);
            }

            this.getRewardData()[index] = new RewardData(occurs, tokens[ARG.EXCLUSIVE], scope, reference, tokens[ARG.LINK], tokens[ARG.MESSAGE], tokens[ARG.WORLD], time, rtime, Integer.parseInt(tokens[ARG.RCOUNT]), type, Integer.parseInt(tokens[ARG.QUANTITY]), tokens[ARG.REWARD], identifier);
            LogFile.write(1, "Reward #" + index + " set at " + this.getRewardData()[index].time + " millis: " + this.getRewardData()[index].reward);
         }

         this.setNumDefinedRewards(index);
         Arrays.sort(this.getRewardData());
         LogFile.write(2, "Reward initialization:  " + rewards.size() + " rewards loaded.");
      }

      this.groups = rewardconfig.getStringList("groups");
      if (this.groups.size() == 0) {
         LogFile.console(1, "Rewards: No groups defined.");
      } else if (OnTime.permission.isEnabled()) {
         LogFile.console(1, "Rewards: Groups defined: " + this.groups);
      } else {
         LogFile.console(3, "Rewards: 'group' rewards defined but no permissions plugin enabled.");
      }

      this.kits = rewardconfig.getStringList("kits");
      if (this.kits.size() == 0) {
         LogFile.console(1, "Rewards: No kits defined.");
      } else {
         boolean error = false;
         delims = "[,]";
         Iterator var28 = this.kits.iterator();

         label167:
         while(true) {
            String k;
            String[] tokens;
            while(var28.hasNext()) {
               k = (String)var28.next();
               tokens = k.split(delims);
               LogFile.console(1, "Rewards: Validating Kit " + tokens[0]);

               for(int i = 0; i < Integer.parseInt(tokens[1]); ++i) {
                  if (tokens.length < i * 2 + 3) {
                     LogFile.console(3, "Rewards: invalid kit defintion for " + tokens[0] + " too few elements.");
                     error = true;
                     break;
                  }

                  if (!tokens[i * 2 + 2].matches("[+]?\\d+(\\/\\d+)?")) {
                     LogFile.console(3, "Rewards: invalid kit defintion for " + tokens[0] + " at " + tokens[i * 2 + 2]);
                     error = true;
                  } else if (Material.matchMaterial(tokens[i * 2 + 3]) == null) {
                     LogFile.console(3, "Rewards: unknown item in kit " + tokens[0] + ": " + tokens[i * 2 + 3]);
                     error = true;
                  }
               }
            }

            if (error) {
               this.kits = null;
               LogFile.console(3, "Rewards: No Kits Loaded due to kit definition error.");
               break;
            } else {
               var28 = this.kits.iterator();

               while(true) {
                  kitClass[] kit;
                  do {
                     if (!var28.hasNext()) {
                        LogFile.console(1, "Rewards: " + this.kits.size() + " Kits defind.");
                        saveUpdate = true;
                        break label167;
                     }

                     k = (String)var28.next();
                     tokens = k.split(delims);
                     kit = this.getKit(tokens[0]);
                  } while(this.getKitElements(tokens[0])[0] != 0);

                  for(int i = 0; i < kit.length; ++i) {
                     this.add((CommandSender)null, RewardData.Occurs.KITELEMENT, TimeUnit.DAYS.toMillis(9999L) + TimeUnit.MINUTES.toMillis((long)i), RewardData.RewardType.ITEM, kit[i].quantity, kit[i].item, "kit-" + tokens[0] + "-" + (i + 1), RewardData.timeScope.TOTAL, RewardData.EventReference.PLAYTIME, "default", (String)null);
                  }
               }
            }
         }
      }

      this.setEnabledWorlds(rewardconfig.getStringList("worlds"));
      if (this.getEnabledWorlds().size() == 0) {
         LogFile.console(3, "Rewards: Enabled worlds not defined.  Defaulting to 'global'");
         this.setEnabledWorlds(new ArrayList());
         this.getEnabledWorlds().add("global");
      } else if (this.getEnabledWorlds().size() == 1 && ((String)this.getEnabledWorlds().get(0)).equalsIgnoreCase("default")) {
         LogFile.console(1, "Rewards: Enabled worlds updated from 'default' to 'global'");
         this.setEnabledWorlds(new ArrayList());
         this.getEnabledWorlds().add("global");
      } else if (this.getEnabledWorlds().size() == 1 && ((String)this.getEnabledWorlds().get(0)).equalsIgnoreCase("all")) {
         List worlds = _plugin.getServer().getWorlds();
         Iterator it = worlds.iterator();
         this.getEnabledWorlds().clear();

         while(it.hasNext()) {
            World world = (World)it.next();
            this.getEnabledWorlds().add(world.getName());
         }
      }

      LogFile.console(1, "Rewards: Worlds defined: " + this.getEnabledWorlds());
      this.setCommands(rewardconfig.getStringList("commands"));
      if (this.getCommands().size() == 0) {
         LogFile.console(1, "Rewards: No commands defined.");
      } else {
         LogFile.console(1, "Rewards: Commands defined: " + this.getCommands());
      }

      if (saveUpdate) {
         RewardUtilities.saveRewards(rewardFile);
      }

   }

   public int getRewardSlot(String playerName) {
      RewardInstance[] rewards;
      if (!playerHasRewards(playerName)) {
         rewards = new RewardInstance[]{new RewardInstance()};
         putPlayerRewards(playerName, rewards);
         LogFile.console(0, "Created new RewardMap entry for " + playerName);
         return 0;
      } else {
         RewardInstance[] oldReward = getPlayerRewards(playerName);

         int i;
         for(i = 0; i < oldReward.length; ++i) {
            if (!oldReward[i].active) {
               return i;
            }
         }

         rewards = new RewardInstance[oldReward.length + 1];

         for(i = 0; i < oldReward.length; ++i) {
            rewards[i] = oldReward[i];
         }

         int record = oldReward.length;
         rewards[record] = new RewardInstance();
         putPlayerRewards(playerName, rewards);
         return record;
      }
   }

   public RewardInstance scheduleRewardTask(String playerName, int rewardID, long delaytime, RewardData reward) {
      if (playerName == null) {
         LogFile.write(3, "{rewards.scheduleRewardTask} playerName was null. " + this.rewardString(reward) + " not issued.");
         return null;
      } else {
         if (delaytime < 0L) {
            delaytime = 0L;
            LogFile.write(3, "{rewards.scheduleRewardTask} delaytime was <0. Info: player:" + playerName + " reward: rewardString(reward)");
         }

         if (!OnTime.rewardsEnable && !OnTime.messagesEnable) {
            return null;
         } else {
            int record = this.getRewardSlot(playerName);
            int rewardTaskID = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new issueReward(playerName, reward, record), delaytime);
            RewardInstance[] rewards = getPlayerRewards(playerName);
            rewards[record].index = record;
            rewards[record].form = RewardInstance.RewardForm.STANDARD;
            rewards[record].identifier = reward.identifier;
            rewards[record].scheduleID = rewardTaskID;
            rewards[record].rewardID = rewardID;
            rewards[record].scheduleNext = true;
            rewards[record].active = true;
            putPlayerRewards(playerName, rewards);
            return rewards[record];
         }
      }
   }

   public int setReward(final String playerName, RewardData.EventReference reference, long time, int rewardID, final RewardData reward, String[] data) {
      RewardInstance[] rewards = null;
      int record = true;
      long delayTime = 0L;
      Player player = Players.getOnlinePlayer(playerName);
      if (player != null && !OnTime.permission.has(player, "ontime.rewards.receive")) {
         return -1;
      } else if (!OnTime.rewardsEnable && !OnTime.messagesEnable) {
         return -1;
      } else {
         final int record = this.getRewardSlot(playerName);
         rewards = getPlayerRewards(playerName);
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

         if (delayTime < 1000L) {
            delayTime = (long)(1000 + 500 * (rewardID + 1));
         }

         int rewardTaskID;
         if (this.canReceiveReward(playerName)) {
            if (reference != RewardData.EventReference.LOGIN && reference != RewardData.EventReference.CHANGEWORLD) {
               LogFile.write(1, "Reward of " + this.rewardString(reward) + " set for " + playerName + " reward should happen in " + Output.getTimeBreakdown(delayTime / 50L, Output.TIMEDETAIL.SHORT));
               rewardTaskID = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new Runnable() {
                  public void run() {
                     Rewards._plugin.get_rewards().issue(playerName, reward, record);
                  }
               }, delayTime / 50L);
            } else {
               rewardTaskID = -1;
            }
         } else {
            rewardTaskID = -1;
         }

         rewards[record].index = record;
         rewards[record].active = true;
         rewards[record].scheduleID = rewardTaskID;
         rewards[record].rewardID = rewardID;
         rewards[record].identifier = reward.identifier;
         rewards[record].form = RewardInstance.RewardForm.PERSONAL;
         rewards[record].time = this.getPersonalRewardTime(time, rewards[record].reference, playerName);
         if (data != null) {
            rewards[record].data = new String[data.length];

            for(int j = 0; j < data.length; ++j) {
               rewards[record].data[j] = data[j].toString();
            }
         }

         if (reward.occurs == RewardData.Occurs.REFERSOURCE && data[0].equalsIgnoreCase("referred-by")) {
            get_rewardUtilities().getReferredbyMap().put(playerName, true);
         }

         LogFile.write(1, "Set new Reward at position " + Integer.toString(record) + " of " + rewards[record].form + " for" + playerName);
         putPlayerRewards(playerName, rewards);
         return record;
      }
   }

   public boolean scheduleRepeatingReward(PlayerData playerData, int rewardID) {
      LogFile.write(0, "{scheduleRepeatingReward} Checking for repeating rewards for " + playerData.playerName);
      boolean oneScheduled = false;
      boolean[] scopeRecurSet = new boolean[4];

      int i;
      for(i = 0; i < 4; ++i) {
         scopeRecurSet[i] = false;
      }

      if (!this.canReceiveReward(playerData.playerName)) {
         return false;
      } else {
         i = 0;
         int max = this.getNumDefinedRewards();
         if (rewardID >= 0) {
            i = rewardID;
            max = rewardID + 1;
         }

         for(; i < max; ++i) {
            long time = 0L;
            if (OnTime.perWorldEnable && !this.rewardData[i].world.equalsIgnoreCase("all")) {
               time = this.getCurrentScopeTime(playerData, this.rewardData[i].scope, this.rewardData[i].world);
            } else {
               time = this.getCurrentScopeTime(playerData, this.rewardData[i].scope, OnTime.serverID);
            }

            if (time >= this.getRewardData()[i].time && (this.getRewardData()[i].occurs == RewardData.Occurs.RECURRING || this.getRewardData()[i].occurs == RewardData.Occurs.PERPETUAL) && (!OnTime.perWorldEnable || this.getRewardData()[i].world.equalsIgnoreCase("all") || playerData.lastWorld.equalsIgnoreCase(this.getRewardData()[i].world))) {
               if (this.getRewardData()[i].exclusive.equalsIgnoreCase("E") && !_plugin.get_permissionsHandler().playerHas(playerData.playerName, this.getRewardData()[i].permissionString)) {
                  LogFile.write(1, playerData.playerName + " did not have permission '" + this.getRewardData()[i].permissionString + "' repeating reward not scheduled.");
               } else {
                  boolean issueReward = false;
                  String typeOutput = "recurring";
                  if (this.getRewardData()[i].occurs == RewardData.Occurs.PERPETUAL) {
                     issueReward = true;
                     typeOutput = "perpetual";
                  } else if (this.getRewardData()[i].occurs == RewardData.Occurs.RECURRING && scopeRecurSet[this.getRewardData()[i].scope.ordinal()]) {
                     LogFile.write(0, "{scheduleRepeatingReward} Reward not issued to " + playerData.playerName + " Reward at scope " + this.getRewardData()[i].scope + "already set.");
                     issueReward = false;
                  } else if (this.getRewardData()[i].count > 0) {
                     int issued = (int)((time - this.getRewardData()[i].time) / this.getRewardData()[i].recurranceTime);
                     if (issued + 1 < this.getRewardData()[i].count) {
                        issueReward = true;
                     } else {
                        LogFile.write(0, "{scheduleRepeatingReward} Reward not issued to " + playerData.playerName + " Reward recur count (" + this.getRewardData()[i].count + ") exceeded");
                     }
                  } else if (i == this.getNumDefinedRewards() - 1) {
                     issueReward = true;
                  } else {
                     RewardData nextReward = this.getNextScopeReward(i);
                     if (nextReward != null) {
                        if (time + this.getRewardData()[i].recurranceTime < nextReward.time) {
                           issueReward = true;
                        } else {
                           LogFile.write(0, "{scheduleRepeatingReward} Reward not issued to " + playerData.playerName + " Not enough time left until next recur reward.");
                        }
                     } else {
                        issueReward = true;
                     }
                  }

                  if (issueReward) {
                     oneScheduled = true;
                     long occurrenceTime = this.getNextOccurrence(playerData.playerName, i);
                     RewardInstance newReward = this.scheduleRewardTask(playerData.playerName, i, (occurrenceTime - time + 2000L) / 50L, this.getRewardData()[i]);
                     LogFile.write(1, "Next " + typeOutput + " reward SET for " + playerData.playerName + " of " + this.rewardString(this.getRewardData()[i]) + " will be at " + Output.getTimeBreakdown(occurrenceTime, Output.TIMEDETAIL.SHORT) + " Reward Index: " + newReward.index);
                     LogFile.write(1, playerData.playerName + " OnTime = " + Output.getTimeBreakdown(time, Output.TIMEDETAIL.SHORT) + " " + typeOutput + " reward should happen in " + Output.getTimeBreakdown(occurrenceTime - time + 2000L, Output.TIMEDETAIL.SHORT));
                     if (this.getRewardData()[i].occurs == RewardData.Occurs.RECURRING) {
                        scopeRecurSet[this.getRewardData()[i].scope.ordinal()] = true;
                     }
                  }
               }
            }
         }

         return oneScheduled;
      }
   }

   public long getCurrentScopeTime(PlayerData playerData, RewardData.timeScope scope) {
      return this.getCurrentScopeTime(playerData, scope, OnTime.serverID);
   }

   public long getCurrentScopeTime(PlayerData playerData, RewardData.timeScope scope, String worldName) {
      PlayTimeData worldTime = Players.getWorldTime(playerData, worldName);
      if (worldTime == null) {
         return 0L;
      } else {
         long currentPlayTime = _plugin.get_logintime().current(playerData, worldName);
         switch (scope) {
            case TOTAL:
               long time = _plugin.get_playingtime().totalOntime(playerData.playerName, worldName);
               if (TimeUnit.MILLISECONDS.toSeconds(time) < 5L) {
                  time = 0L;
               }

               return time;
            case DAILY:
               return worldTime.todayTime + currentPlayTime;
            case WEEKLY:
               return worldTime.weekTime + currentPlayTime;
            case MONTHLY:
               return worldTime.monthTime + currentPlayTime;
            default:
               return -1L;
         }
      }
   }

   private RewardData getNextScopeReward(int rewardID) {
      for(int i = rewardID + 1; i < this.getNumDefinedRewards(); ++i) {
         if (this.rewardData[rewardID].scope == this.rewardData[i].scope && (this.rewardData[i].occurs == RewardData.Occurs.SINGLE || this.rewardData[i].occurs == RewardData.Occurs.RECURRING || this.rewardData[i].occurs == RewardData.Occurs.PERPETUAL)) {
            return this.rewardData[i];
         }
      }

      return null;
   }

   private String getNthPlace(TreeMap sorted_map, int place) {
      String key = (String)sorted_map.firstKey();

      for(int i = 0; i <= place; ++i) {
         if (i > 0) {
            key = (String)sorted_map.higherKey(key);
            if (key == null) {
               return null;
            }
         }

         if (!Players.playerHasData(key)) {
            LogFile.write(10, "Top Reward error, no PlayerData found for " + key);
            return null;
         }

         if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
            while((Players.getData(key).permissions & PlayerData.OTPerms.TOPTEN.mask()) != 0) {
               LogFile.write(1, "Top Reward not issued to " + Players.getData(key).playerName + " because they did not have permission (ontime.top.exclude=true).");
               key = (String)sorted_map.higherKey(key);
               if (key == null) {
                  return null;
               }

               if (!Players.playerHasData(key)) {
                  LogFile.write(10, "Top Reward error, no PlayerData found for " + key);
                  return null;
               }
            }
         }

         if (key == null) {
            return null;
         }
      }

      return Players.getData(key).playerName;
   }

   public boolean setTopPlayReward(RewardData.timeScope scopeRequest) {
      boolean oneSet = false;
      long periodStartTime = 0L;
      HashMap localMap;
      long delay;
      if (scopeRequest == RewardData.timeScope.MONTHLY) {
         localMap = _plugin.get_todaytime().getMonthMap();
         periodStartTime = OnTime.monthStart;
         delay = 3000L;
      } else if (scopeRequest == RewardData.timeScope.WEEKLY) {
         localMap = _plugin.get_todaytime().getWeekMap();
         periodStartTime = OnTime.weekStart;
         delay = 2000L;
      } else if (scopeRequest == RewardData.timeScope.DAILY) {
         localMap = _plugin.get_todaytime().getDayMap();
         periodStartTime = OnTime.todayStart;
         delay = 1000L;
      } else {
         _plugin.get_playingtime().buildPlaytimeMap(OnTime.serverID);
         localMap = _plugin.get_playingtime().getPlaytimeMap();
         delay = 4000L;
      }

      ValueComparator bvc = new ValueComparator(localMap);
      TreeMap sorted_map = new TreeMap(bvc);
      sorted_map.putAll(localMap);
      if (sorted_map.size() < 1) {
         return false;
      } else {
         for(int j = 0; j < this.getNumDefinedRewards(); ++j) {
            if (this.getRewardData()[j].occurs == RewardData.Occurs.TOP && scopeRequest == this.getRewardData()[j].scope && this.getRewardData()[j].reference == RewardData.EventReference.PLAYTIME) {
               for(int i = (int)this.getRewardData()[j].time - 1; i < (int)this.getRewardData()[j].recurranceTime; ++i) {
                  String playerName = this.getNthPlace(sorted_map, i);
                  if (playerName != null) {
                     String[] data = new String[]{RewardData.EventReference.PLAYTIME.label().toLowerCase(), null, String.valueOf(i + 1)};
                     if (scopeRequest != RewardData.timeScope.TOTAL) {
                        data[1] = (new SimpleDateFormat(Output.OnTimeOutput.getString("output.dateFormat") + " ")).format(periodStartTime);
                     } else {
                        data[1] = (new SimpleDateFormat(Output.OnTimeOutput.getString("output.dateFormat") + " ")).format(TodayTime.todayMidnight());
                     }

                     this.setReward(playerName, RewardData.EventReference.REALTIME, delay, j, this.getRewardData()[j], data);
                     LogFile.write(1, "Top Reward (" + this.getRewardData()[j].occurs + " #" + (i + 1) + ") of " + this.rewardString(this.getRewardData()[j]) + " set for " + playerName);
                     oneSet = true;
                  } else {
                     LogFile.write(1, "Top Reward (" + this.getRewardData()[j].occurs + " #" + (i + 1) + ") not issued as there was no player in that position.");
                  }
               }
            }
         }

         return oneSet;
      }
   }

   public boolean setTopMiscRewards(RewardData.EventReference reference, RewardData.timeScope scope) {
      ResultSet rs = null;
      boolean more = true;
      String dataItem = null;
      String playerName = null;
      int topData = false;
      String[] data = new String[3];
      boolean voteService = false;
      LogFile.console(0, "Checking for TOP reward of: " + reference.toString() + scope.toString());

      label126:
      for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
         if (this.getRewardData()[i].occurs == RewardData.Occurs.TOP && this.getRewardData()[i].reference == reference && this.getRewardData()[i].scope == scope) {
            if (reference == RewardData.EventReference.VOTES) {
               dataItem = "vote";
            } else if (reference == RewardData.EventReference.REFER) {
               dataItem = "refer";
            } else {
               if (reference != RewardData.EventReference.POINTS) {
                  return false;
               }

               dataItem = "point";
            }

            if (scope == RewardData.timeScope.DAILY) {
               dataItem = dataItem + "Today";
               data[1] = (new SimpleDateFormat(Output.OnTimeOutput.getString("output.dateFormat") + " ")).format(OnTime.todayStart);
            } else if (scope == RewardData.timeScope.WEEKLY) {
               dataItem = dataItem + "Week";
               data[1] = (new SimpleDateFormat(Output.OnTimeOutput.getString("output.dateFormat") + " ")).format(OnTime.weekStart);
            } else if (scope == RewardData.timeScope.MONTHLY) {
               dataItem = dataItem + "Month";
               data[1] = (new SimpleDateFormat(Output.OnTimeOutput.getString("output.dateFormat") + " ")).format(OnTime.monthStart);
            } else {
               if (scope != RewardData.timeScope.TOTAL) {
                  return false;
               }

               switch (reference) {
                  case VOTES:
                  case POINTS:
                     dataItem = dataItem + "s";
                     break;
                  case REFER:
                     dataItem = dataItem + "rals";
                  case ABSENCE:
                  case DEATH:
                  case SHOP_POINTS:
                  case SHOP_ECON:
               }

               data[1] = (new SimpleDateFormat(Output.OnTimeOutput.getString("output.dateFormat") + " ")).format(OnTime.todayStart);
            }

            data[0] = reference.label().toLowerCase();
            LogFile.console(0, "processing top reward for " + dataItem + " reward " + this.getRewardData()[i].identifier);

            try {
               if (rs == null) {
                  rs = _plugin.get_dataio().getTopPlayersDataMySQL(dataItem);
               }

               int position = 0;
               more = rs.first();

               while(true) {
                  while(true) {
                     label109:
                     do {
                        if (!more || (long)position >= this.getRewardData()[i].recurranceTime) {
                           continue label126;
                        }

                        while(true) {
                           while(true) {
                              if (!more || (long)position >= this.getRewardData()[i].time - 1L) {
                                 continue label109;
                              }

                              if (!rs.next()) {
                                 more = false;
                              } else if (reference != RewardData.EventReference.VOTES || !rs.getString("referredby").equalsIgnoreCase("votifier-service")) {
                                 ++position;
                              }
                           }
                        }
                     } while(!more);

                     int topData = rs.getInt(dataItem);
                     if (topData > 0) {
                        playerName = rs.getString("playername");
                        if (reference == RewardData.EventReference.VOTES && rs.getString("referredby").equalsIgnoreCase("votifier-service")) {
                           voteService = true;
                        } else {
                           voteService = false;
                           data[2] = String.valueOf(position + 1);
                           this.setReward(playerName, RewardData.EventReference.REALTIME, 1000L, i, this.getRewardData()[i], data);
                           LogFile.write(1, "Top #" + (position + 1) + " " + reference.label() + " " + scope.label() + " scheduled for " + playerName);
                        }

                        if (!rs.next()) {
                           more = false;
                        } else if (topData != rs.getInt(dataItem) && !voteService) {
                           ++position;
                        }
                     } else {
                        more = false;
                     }
                  }
               }
            } catch (SQLException var12) {
               var12.printStackTrace();
               more = false;
            }
         }
      }

      return true;
   }

   private boolean canReceiveReward(String playerName) {
      Player player = Players.getOnlinePlayer(playerName);
      if (player == null) {
         LogFile.write(1, "{rewards.canReceiveReward} Schedule Reward unknown player " + playerName + " specified.");
         return false;
      } else if (!_plugin.get_logintime().playerIsOnline(Players.getData(playerName))) {
         LogFile.write(1, "{rewards.canReceiveReward} No reward set for " + playerName + ". Player offline.");
         return false;
      } else if (!OnTime.permission.has(player, "ontime.rewards.receive")) {
         LogFile.write(2, "{rewards.canReceiveReward} No reward set for " + playerName + " 'ontime.rewards.receive' permission not set.");
         if (playerHasRewards(playerName)) {
            this.getRewardMap().remove(playerName);
         }

         return false;
      } else if (!this.isGlobal() && !this.getEnabledWorlds().contains(player.getWorld().getName())) {
         LogFile.write(1, "No reward set for " + playerName + ". World (" + player.getWorld().getName() + ") not enabled for rewards.");
         return false;
      } else {
         return true;
      }
   }

   private rewardEligibility isEligible(String playerName, RewardData reward) {
      if (reward.exclusive.equalsIgnoreCase("E") && !_plugin.get_permissionsHandler().playerHas(playerName, reward.permissionString)) {
         LogFile.write(1, playerName + " did not have permission '" + reward.permissionString + "' when reward was checked.");
         return Rewards.rewardEligibility.EXCLUSIVE;
      } else if (!reward.world.equalsIgnoreCase("all") && !Players.getOnlinePlayer(playerName).getWorld().getName().equalsIgnoreCase(reward.world)) {
         LogFile.console(0, "Looking at reward: " + reward.identifier + "; world is:" + reward.world);
         return reward.onWorld ? Rewards.rewardEligibility.OFFWORLD : Rewards.rewardEligibility.OTHERWORLD;
      } else {
         if (reward.type != RewardData.RewardType.PROMOTION && reward.type != RewardData.RewardType.DEMOTION) {
            if (reward.type == RewardData.RewardType.PERMISSION) {
               if (_plugin.get_permissionsHandler().playerHas(playerName, reward.reward)) {
                  LogFile.write(1, playerName + " already has the permission '" + reward.reward + "' permission reward not scheduled.");
                  return Rewards.rewardEligibility.HAS_PERM;
               }
            } else if (reward.type == RewardData.RewardType.DENIAL && !_plugin.get_permissionsHandler().playerHas(playerName, reward.reward)) {
               LogFile.write(1, playerName + " does not have the permission '" + reward.reward + "' denial reward not scheduled.");
               return Rewards.rewardEligibility.NO_PERM;
            }
         } else {
            promoOrDemo pORd = this.promotionORdemotion(playerName, reward);
            if (reward.type == RewardData.RewardType.PROMOTION && (pORd == Rewards.promoOrDemo.DEMOTION || pORd == Rewards.promoOrDemo.NO_CHANGE)) {
               LogFile.write(1, playerName + " already at higher group level. '" + reward.reward + "' promotion reward not scheduled.");
               return Rewards.rewardEligibility.HIGHERGROUP;
            }

            if (reward.type == RewardData.RewardType.DEMOTION && (pORd == Rewards.promoOrDemo.PROMOTION || pORd == Rewards.promoOrDemo.NO_CHANGE)) {
               LogFile.write(1, playerName + " already at same or lower group level. '" + reward.reward + "' demotion reward not scheduled.");
               return Rewards.rewardEligibility.LOWERGROUP;
            }
         }

         return Rewards.rewardEligibility.ELIGIBLE;
      }
   }

   public void scheduleNextReward(String playerName, RewardData.timeScope scope) {
      if (this.canReceiveReward(playerName)) {
         PlayerData playerData = Players.getData(playerName);
         long serverTime;
         long worldTime;
         if ((scope == null || scope == RewardData.timeScope.TOTAL) && this.scopeCount[RewardData.timeScope.TOTAL.ordinal()] > 0) {
            serverTime = this.getCurrentScopeTime(playerData, RewardData.timeScope.TOTAL, OnTime.serverID);
            worldTime = this.getCurrentScopeTime(playerData, RewardData.timeScope.TOTAL, playerData.lastWorld);
            this.findNextReward(playerName, serverTime, worldTime, RewardData.timeScope.TOTAL);
         }

         if ((scope == null || scope == RewardData.timeScope.DAILY) && this.scopeCount[RewardData.timeScope.DAILY.ordinal()] > 0) {
            if (Players.playerHasData(playerName)) {
               serverTime = this.getCurrentScopeTime(playerData, RewardData.timeScope.DAILY, OnTime.serverID);
               worldTime = this.getCurrentScopeTime(playerData, RewardData.timeScope.DAILY, playerData.lastWorld);
               this.findNextReward(playerName, serverTime, worldTime, RewardData.timeScope.DAILY);
            } else {
               LogFile.write(3, "{scheduleNextReward} Could not find ontime record for " + playerName);
            }
         }

         if ((scope == null || scope == RewardData.timeScope.WEEKLY) && this.scopeCount[RewardData.timeScope.WEEKLY.ordinal()] > 0) {
            if (Players.playerHasData(playerName)) {
               LogFile.write(0, "Checking for weekly reward for " + playerName);
               serverTime = this.getCurrentScopeTime(playerData, RewardData.timeScope.WEEKLY, OnTime.serverID);
               worldTime = this.getCurrentScopeTime(playerData, RewardData.timeScope.WEEKLY, playerData.lastWorld);
               this.findNextReward(playerName, serverTime, worldTime, RewardData.timeScope.WEEKLY);
            } else {
               LogFile.write(3, "{scheduleNextReward} Could not find 'weekly' record for " + playerName);
            }
         }

         if ((scope == null || scope == RewardData.timeScope.MONTHLY) && this.scopeCount[RewardData.timeScope.MONTHLY.ordinal()] > 0) {
            if (Players.playerHasData(playerName)) {
               LogFile.write(0, "Checking for monthly reward for " + playerName);
               serverTime = this.getCurrentScopeTime(playerData, RewardData.timeScope.MONTHLY, OnTime.serverID);
               worldTime = this.getCurrentScopeTime(playerData, RewardData.timeScope.MONTHLY, playerData.lastWorld);
               this.findNextReward(playerName, serverTime, worldTime, RewardData.timeScope.MONTHLY);
            } else {
               LogFile.write(3, "{scheduleNextReward} Could not find 'monthly' record for " + playerName);
            }
         }

      }
   }

   public void findNextReward(String playerName, long serverTime, long worldTime, RewardData.timeScope scope) {
      RewardInstance newReward = null;
      boolean oneMore = false;
      Player player = Players.getOnlinePlayer(playerName);
      if (TimeUnit.MILLISECONDS.toSeconds(serverTime) < 1L) {
         serverTime = 0L;
      }

      if (TimeUnit.MILLISECONDS.toSeconds(worldTime) < 1L) {
         worldTime = 0L;
      }

      for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
         boolean worldMatch = true;
         long time = serverTime;
         if (!this.getRewardData()[i].world.endsWith("all") && player != null) {
            if (this.getRewardData()[i].world.equalsIgnoreCase(player.getWorld().getName()) && OnTime.perWorldEnable) {
               time = worldTime;
            } else {
               worldMatch = false;
            }
         }

         if (!OnTime.perWorldEnable || OnTime.perWorldEnable && worldMatch) {
            if (this.getRewardData()[i].reference != RewardData.EventReference.SHOP_ECON && this.getRewardData()[i].reference != RewardData.EventReference.SHOP_POINTS) {
               if (this.getRewardData()[i].scope != scope || this.getRewardData()[i].occurs != RewardData.Occurs.SINGLE && this.getRewardData()[i].occurs != RewardData.Occurs.RECURRING && this.getRewardData()[i].occurs != RewardData.Occurs.PERPETUAL) {
                  if (oneMore) {
                     newReward.scheduleNext = true;
                     return;
                  }
               } else if (time >= this.getRewardData()[i].time && time != 0L) {
                  if (OnTime.permission.isEnabled() && this.getRewardData()[i].type == RewardData.RewardType.PROMOTION) {
                     if (this.getRewardData()[i].exclusive.equalsIgnoreCase("E") && !_plugin.get_permissionsHandler().playerHas(playerName, this.getRewardData()[i].permissionString)) {
                        LogFile.write(1, playerName + " did not have permission '" + this.getRewardData()[i].permissionString + "' (missed promotion) reward not scheduled.");
                     } else if (this.getRewardData()[i].reference != RewardData.EventReference.SHOP_ECON && this.getRewardData()[i].reference != RewardData.EventReference.SHOP_POINTS && this.promotionORdemotion(playerName, this.getRewardData()[i]) == Rewards.promoOrDemo.PROMOTION) {
                        LogFile.write(2, "Missed reward of " + playerName + " of " + this.rewardString(this.getRewardData()[i]) + " scheduled immediately.");
                        this.scheduleRewardTask(playerName, i, 40L, this.getRewardData()[i]);
                        return;
                     }
                  }
               } else {
                  promoOrDemo rankChange = null;
                  if (this.getRewardData()[i].type == RewardData.RewardType.PROMOTION || this.getRewardData()[i].type == RewardData.RewardType.DEMOTION) {
                     rankChange = this.promotionORdemotion(playerName, this.getRewardData()[i]);
                  }

                  if ((this.getRewardData()[i].type == RewardData.RewardType.PROMOTION || this.getRewardData()[i].type == RewardData.RewardType.DEMOTION || this.getRewardData()[i].type == RewardData.RewardType.PERMISSION || this.getRewardData()[i].type == RewardData.RewardType.DENIAL || this.getRewardData()[i].type == RewardData.RewardType.ADDGROUP || this.getRewardData()[i].type == RewardData.RewardType.REMOVEGROUP) && !OnTime.permission.isEnabled()) {
                     if (oneMore) {
                        newReward.scheduleNext = true;
                        return;
                     }
                  } else if ((this.getRewardData()[i].type == RewardData.RewardType.PROMOTION || this.getRewardData()[i].type == RewardData.RewardType.DEMOTION || this.getRewardData()[i].type == RewardData.RewardType.ADDGROUP || this.getRewardData()[i].type == RewardData.RewardType.REMOVEGROUP) && !this.isValidGroup(this.getRewardData()[i].reward)) {
                     LogFile.console(3, "Group reward listed " + this.getRewardData()[i].reward + " in rewards.yml not defined in " + OnTime.permission.getName() + " configuration.");
                     LogFile.write(3, "Group reward listed " + this.getRewardData()[i].reward + " in rewards.yml not defined in " + OnTime.permission.getName() + " configuration.");
                     if (oneMore) {
                        newReward.scheduleNext = true;
                        return;
                     }
                  } else if (this.getRewardData()[i].exclusive.equalsIgnoreCase("E") && !_plugin.get_permissionsHandler().playerHas(playerName, this.getRewardData()[i].permissionString)) {
                     LogFile.write(1, playerName + " did not have permission '" + this.getRewardData()[i].permissionString + "' (next) reward not scheduled.");
                     if (oneMore) {
                        newReward.scheduleNext = true;
                        LogFile.write(0, "{scheduleNextReward} Exiting because no permission");
                        return;
                     }
                  } else if (this.getRewardData()[i].type == RewardData.RewardType.PROMOTION && (rankChange == Rewards.promoOrDemo.DEMOTION || rankChange == Rewards.promoOrDemo.ERROR)) {
                     LogFile.write(0, playerName + " already at higher group level. '" + this.getRewardData()[i].reward + "' group change reward not scheduled.");
                     if (oneMore) {
                        newReward.scheduleNext = true;
                        return;
                     }
                  } else {
                     LogFile.write(1, scope.label() + " reward scheduled for " + playerName + " of " + this.rewardString(this.getRewardData()[i]) + " will be at " + Output.getTimeBreakdown(this.getRewardData()[i].time, Output.TIMEDETAIL.SHORT));
                     LogFile.write(1, playerName + " OnTime = " + Output.getTimeBreakdown(time, Output.TIMEDETAIL.SHORT) + " reward should happen in " + Output.getTimeBreakdown(this.getRewardData()[i].time - time + 2000L, Output.TIMEDETAIL.SHORT));
                     newReward = this.scheduleRewardTask(playerName, i, (this.getRewardData()[i].time - time + 2000L) / 50L, this.getRewardData()[i]);
                     if (this.getRewardData()[i].occurs == RewardData.Occurs.SINGLE && i + 1 < this.getNumDefinedRewards()) {
                        if (TimeUnit.MILLISECONDS.toSeconds(this.getRewardData()[i].time) != TimeUnit.MILLISECONDS.toSeconds(this.getRewardData()[i + 1].time)) {
                           return;
                        }

                        LogFile.write(0, "{scheduleNextReward} Looking to scheule one more: " + this.getRewardData()[i + 1].identifier);
                        newReward.scheduleNext = false;
                        oneMore = true;
                     }
                  }
               }
            } else {
               LogFile.write(0, " Skipping Shop Reward:" + this.getRewardData()[i].identifier + " for " + playerName);
            }
         } else {
            LogFile.write(0, " Skipping Per-World Reward:" + this.getRewardData()[i].identifier + " for " + playerName + " who is not in world:" + this.getRewardData()[i].world);
         }
      }

   }

   public void incrementReferrals(String playerName) {
      if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
         LogFile.write(1, "{rewards.issue} Incrementing number of referrals for " + playerName);
         _plugin.get_dataio().incrementMySQLField(OnTime.MySQL_table, "referrals", playerName);
         if (OnTime.collectReferDetailEnable) {
            _plugin.get_dataio().incrementMySQLField(OnTime.MySQL_table, "referToday", playerName);
            _plugin.get_dataio().incrementMySQLField(OnTime.MySQL_table, "referWeek", playerName);
            _plugin.get_dataio().incrementMySQLField(OnTime.MySQL_table, "referMonth", playerName);
         }

         PlayerData sourcePlayerData = null;
         if ((sourcePlayerData = Players.getData(playerName)) != null) {
            ++sourcePlayerData.totalReferrals;
            if (OnTime.collectReferDetailEnable) {
               ++sourcePlayerData.dailyReferrals;
               ++sourcePlayerData.weeklyReferrals;
               ++sourcePlayerData.monthlyReferrals;
            }
         }
      }

   }

   public void issue(final String playerName, final RewardData reward, int index) {
      if (playerName == null) {
         LogFile.write(3, "{Rewards.issue} playerName was null. " + this.rewardString(reward) + " not issued.");
      } else {
         Player player = null;
         if ((player = Players.getOnlinePlayer(playerName)) == null) {
            LogFile.write(3, "{Rewards.issue} 'player' was null. " + this.rewardString(reward) + " not issued.");
         } else {
            OfflinePlayer offlinePlayer = Players.getOfflinePlayer(playerName);
            RewardInstance[] playersRewards;
            if (reward.type == RewardData.RewardType.DELAY) {
               playersRewards = getPlayerRewards(playerName);
               playersRewards[index].active = false;
               if (OnTime.permission.has(player, "ontime.rewards.receive")) {
                  LogFile.write(1, "Reward scheduling initiated for " + playerName);
                  this.scheduleNextReward(playerName, reward.scope);
               }

            } else {
               String[] data;
               if (reward.occurs == RewardData.Occurs.REFERSOURCE) {
                  if (getPlayerRewards(playerName)[index].data[0].equalsIgnoreCase("referred-by")) {
                     playersRewards = getPlayerRewards(playerName);
                     String sourcePlayer = getPlayerRewards(playerName)[index].data[1];
                     PlayerData sourcePlayerData = Players.getData(sourcePlayer);
                     if (playersRewards[index].form == RewardInstance.RewardForm.PERSONAL) {
                        playersRewards[index].active = false;
                     }

                     if (getPlayerRewards(playerName)[index].data[2].equalsIgnoreCase("1")) {
                        this.incrementReferrals(sourcePlayer);
                     }

                     if (OnTime.referredByPermTrackEnable && !_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PAP, player, "ontime.referredby.success")) {
                        LogFile.console(3, "Error. Adding of 'ontime.referredby.success' permission for " + playerName + " failed.");
                     }

                     if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                        _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "referredby", getPlayerRewards(playerName)[index].data[1], playerName);
                     }

                     if (Players.playerHasData(playerName)) {
                        Players.getData(playerName).referredBy = getPlayerRewards(playerName)[index].data[1];
                     }

                     int referCount = 0;
                     if (OnTime.dataStorage == DataIO.datastorage.MYSQL) {
                        if (reward.scope == RewardData.timeScope.TOTAL) {
                           referCount = sourcePlayerData.totalReferrals;
                        } else if (reward.scope == RewardData.timeScope.DAILY) {
                           referCount = sourcePlayerData.dailyReferrals;
                        } else if (reward.scope == RewardData.timeScope.WEEKLY) {
                           referCount = sourcePlayerData.weeklyReferrals;
                        } else if (reward.scope == RewardData.timeScope.MONTHLY) {
                           referCount = sourcePlayerData.monthlyReferrals;
                        }
                     }

                     if (reward.count >= 0 && (reward.count < 0 || referCount % reward.count != 0)) {
                        LogFile.write(1, "Referral reward not issued to  " + getPlayerRewards(playerName)[index].data[1] + " for referral of  " + playerName + ". Insufficient referrals.");
                     } else {
                        data = new String[]{"has-referred", playerName.toString()};
                        this.setReward(getPlayerRewards(playerName)[index].data[1], RewardData.EventReference.DELTATIME, TimeUnit.SECONDS.toMillis((long)(2 * (index + 1))), getPlayerRewards(playerName)[index].rewardID, reward, data);
                     }

                     LogFile.write(3, "Successful referral of " + playerName + " by " + getPlayerRewards(playerName)[index].data[1] + " completed.");
                     this.scheduleLinkReward(playerName, reward);
                     return;
                  }

                  if (reward.exclusive.equalsIgnoreCase("E") && !_plugin.get_permissionsHandler().playerHas(playerName, reward.permissionString)) {
                     LogFile.write(2, "Referral reward not issued to " + playerName + "for " + getPlayerRewards(playerName)[index].data[1] + ". No permission for:" + reward.permissionString);
                     return;
                  }

                  LogFile.write(3, "Attempting issue of reward to " + playerName + " of " + this.rewardString(reward) + " for sucessful referral of " + getPlayerRewards(playerName)[index].data[1]);
               } else {
                  LogFile.write(3, "Attempting issue of reward to " + playerName + " of " + this.rewardString(reward));
               }

               if (!_plugin.get_logintime().playerIsOnline(Players.getData(player))) {
                  LogFile.write(10, "{rewards.issue} Player not online to receive reward issue: " + playerName);
               } else {
                  boolean success = false;
                  rewardEligibility eligibility = this.isEligible(playerName, reward);
                  if (eligibility == Rewards.rewardEligibility.ELIGIBLE) {
                     _plugin.get_awayfk().updateLastReward(player, Calendar.getInstance().getTimeInMillis());
                     String command;
                     if (reward.type == RewardData.RewardType.ECONOMY) {
                        command = Output.getMixedName(playerName);
                        if (OnTime.economy != null) {
                           data = null;
                           EconomyResponse response;
                           if (Integer.parseInt(reward.reward) > 0) {
                              response = OnTime.economy.depositPlayer(offlinePlayer, (double)Integer.parseInt(reward.reward));
                              if (response.type == ResponseType.SUCCESS) {
                                 this.rewardNotifyOut("output.reward.econ", playerName, reward, index);
                                 LogFile.write(1, "Econ reward issued: " + playerName + "   Amount delta:" + response.amount + "    New Balance:" + response.balance);
                              } else {
                                 LogFile.write(3, "Econ reward not issued to " + command + ". Econ plugin returned error: " + response.errorMessage);
                              }
                           } else {
                              response = OnTime.economy.withdrawPlayer(offlinePlayer, (double)(-Integer.parseInt(reward.reward)));
                              if (response.type == ResponseType.SUCCESS) {
                                 this.rewardNotifyOut("output.reward.tax", playerName, reward, index);
                                 LogFile.write(1, "Tax collected: " + playerName + "   Amount delta:" + response.amount + "    New Balance:" + response.balance);
                              } else {
                                 LogFile.write(3, "Tax not collected from " + command + ". Econ plugin returned error: " + response.errorMessage);
                              }
                           }
                        } else {
                           LogFile.write(3, "Econ reward not issued to " + command + " because Economy not found.");
                        }
                     } else if (reward.type == RewardData.RewardType.KIT) {
                        Integer[] elements = this.getKitElements(reward.reward);
                        if (elements[0] > 0) {
                           for(int i = 1; i <= elements[0]; ++i) {
                              this.setReward(playerName, RewardData.EventReference.DELTATIME, 0L, elements[i], this.getRewardData()[elements[i]], (String[])null);
                           }

                           this.rewardNotifyOut("output.reward.kit", playerName, reward, index);
                        } else {
                           LogFile.write(3, "Elements for Kit reward (" + reward.reward + ") for " + playerName + " not found.");
                        }
                     } else if (reward.type == RewardData.RewardType.ITEM) {
                        String[] tokens = reward.reward.split("[+:]");
                        Material material = Material.matchMaterial(tokens[0]);
                        if (material == null) {
                           LogFile.console(3, "{Rewards.issue} Invalid item specified (" + reward.reward + ") no reward issued.");
                        } else {
                           Inventory inventory = Players.getOnlinePlayer(playerName).getInventory();
                           ItemStack itemstack = reward.itemstack.clone();
                           if (!inventory.addItem(new ItemStack[]{itemstack}).isEmpty()) {
                              Output.generate("output.reward.inventoryFull", playerName, reward, index);
                              LogFile.write(3, "Rescheduled Item reward for " + playerName + " because their inventory was full.");
                              playersRewards = getPlayerRewards(playerName);
                              playersRewards[index].active = false;
                              putPlayerRewards(playerName, playersRewards);
                              this.setReward(playerName, RewardData.EventReference.DELTATIME, TimeUnit.MINUTES.toMillis(1L), getPlayerRewards(playerName)[index].rewardID, reward, getPlayerRewards(playerName)[index].data);
                              return;
                           }

                           if (reward.occurs != RewardData.Occurs.KITELEMENT) {
                              this.rewardNotifyOut("output.reward.item", playerName, reward, index);
                           } else {
                              this.rewardNotifyOut("output.reward.kitelement", playerName, reward, index);
                           }

                           success = true;
                        }
                     } else if (reward.type == RewardData.RewardType.PERMISSION) {
                        if (OnTime.permission.isEnabled()) {
                           if (!_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PAP, player, reward.reward)) {
                              LogFile.console(3, "{Rewards.issue} ' Permission execution failed. Reward unsuccessful.");
                           } else {
                              this.rewardNotifyOut("output.reward.permission", playerName, reward, index);
                              success = true;
                           }
                        } else {
                           LogFile.write(3, "ERROR {rewards.issue} 'permissions' reward was scheduled, but permissions plugin not enabled.  No reward issued.");
                        }
                     } else if (reward.type == RewardData.RewardType.DENIAL) {
                        if (OnTime.permission.isEnabled()) {
                           if (!_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PRP, player, reward.reward)) {
                              LogFile.console(3, "{Rewards.issue} ' Denial (Permission Remove) execution failed.");
                           } else {
                              this.rewardNotifyOut("output.reward.denial", playerName, reward, index);
                              success = true;
                           }
                        } else {
                           LogFile.write(3, "ERROR {rewards.issue} 'permission remove' reward was scheduled, but permissions plugin not enabled.  No reward issued.");
                        }
                     } else if (reward.type != RewardData.RewardType.PROMOTION && reward.type != RewardData.RewardType.DEMOTION) {
                        if (reward.type == RewardData.RewardType.ADDGROUP) {
                           if (OnTime.permission.isEnabled()) {
                              if (!_plugin.get_permissionsHandler().playerInGroup(playerName, reward.reward)) {
                                 if (!_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PAG, player, reward.reward)) {
                                    LogFile.write(3, "{Rewards.issue} ' Group add execution failed. Reward unsuccessful.");
                                 } else {
                                    this.rewardNotifyOut("output.reward.addgroup", playerName, reward, index);
                                    success = true;
                                 }
                              }
                           } else {
                              LogFile.write(3, "ERROR {rewards.issue} 'add group' reward was scheduled, but permissions plugin not enabled.  No reward issued.");
                           }
                        } else if (reward.type == RewardData.RewardType.REMOVEGROUP) {
                           if (OnTime.permission.isEnabled()) {
                              if (_plugin.get_permissionsHandler().playerInGroup(playerName, reward.reward)) {
                                 if (!_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PRG, player, reward.reward)) {
                                    LogFile.write(3, "{reward.issue.removegroup} removal of " + playerName + " from group " + reward.reward + " failed.");
                                 } else {
                                    this.rewardNotifyOut("output.reward.removegroup", playerName, reward, index);
                                    success = true;
                                 }
                              } else {
                                 LogFile.write(3, "{rewards.issue}  ' Group (" + reward.reward + ") remove reward not issued for " + playerName + ".  Player was not in that group.");
                              }
                           } else {
                              LogFile.write(3, "ERROR {rewards.issue} 'remove group' reward was scheduled, but permissions plugin not enabled.  No reward issued.");
                           }
                        } else if (reward.type == RewardData.RewardType.COMMAND) {
                           command = this.getCommand(reward.reward);
                           if (command == null) {
                              LogFile.write(3, "Command Defintion reward error. " + reward.reward + " not defined.");
                           } else {
                              String commandString = this.getCommand(reward.reward);
                              String actualName = Players.getOnlinePlayer(playerName).getName();
                              if (commandString.startsWith("'")) {
                                 commandString = commandString.substring(1);
                              }

                              if (commandString.endsWith("'")) {
                                 commandString = commandString.substring(0, commandString.length() - 1);
                              }

                              commandString = this.replaceString("[player]", actualName, commandString);
                              if (getPlayerRewards(playerName)[index].data != null) {
                                 commandString = this.replaceString("[voteSource]", getPlayerRewards(playerName)[index].data[0], commandString);
                              }

                              playersRewards = getPlayerRewards(playerName);
                              playersRewards[index].active = false;
                              if (!_plugin.getServer().dispatchCommand(_plugin.getServer().getConsoleSender(), commandString)) {
                                 LogFile.write(3, "Command reward Execution error. " + commandString + " failed.");
                              } else {
                                 this.rewardNotifyOut("output.reward.command", playerName, reward, index);
                                 LogFile.write(3, "Command (" + commandString + ") execution for " + playerName + " successful.");
                                 success = true;
                              }
                           }
                        } else if (reward.type == RewardData.RewardType.XP) {
                           Players.getOnlinePlayer(playerName).giveExp(reward.getQuantity());
                           this.rewardNotifyOut("output.reward.xp", playerName, reward, index);
                           success = true;
                        } else if (reward.type == RewardData.RewardType.POINTS) {
                           _plugin.get_points().addPoints(playerName, reward.getQuantity());
                           this.rewardNotifyOut("output.reward.points", playerName, reward, index);
                           success = true;
                        } else if (reward.type == RewardData.RewardType.MESSAGE) {
                           _plugin.get_messages().generate(reward.reward, playerName, (String[])null);
                           success = true;
                        } else {
                           LogFile.console(3, "{Rewards.issue} Reward definition error on reward.type:" + reward.type);
                        }
                     } else if (OnTime.permission.isEnabled()) {
                        command = this.getCurrentGroup(playerName);
                        if (command != null && !_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PRG, player, command)) {
                           LogFile.write(3, "{reward.issue} removal of " + playerName + " from group " + command + " failed.");
                        }

                        if (_plugin.get_permissionsHandler().playerInGroup(playerName, reward.reward) && getPlayerRewards(playerName)[index].form == RewardInstance.RewardForm.STANDARD) {
                           playersRewards = getPlayerRewards(playerName);
                           playersRewards[index].active = false;
                           putPlayerRewards(playerName, playersRewards);
                           this.scheduleRewardTask(playerName, 0, 100L, this.createDelayReward(reward.scope));
                           if (reward.link != null) {
                              _plugin.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new Runnable() {
                                 public void run() {
                                    Rewards.this.scheduleLinkReward(playerName, reward);
                                 }
                              }, 80L);
                           }

                           return;
                        }

                        if (!_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.PAG, player, reward.reward)) {
                           LogFile.write(3, "{Rewards.issue} ' Group promtion/demotion execution failed. Reward unsuccessful.");
                        } else {
                           if (reward.type == RewardData.RewardType.PROMOTION) {
                              this.rewardNotifyOut("output.reward.promotion", playerName, reward, index);
                           } else {
                              this.rewardNotifyOut("output.reward.demotion", playerName, reward, index);
                           }

                           success = true;
                        }

                        if (getPlayerRewards(playerName)[index].form == RewardInstance.RewardForm.STANDARD) {
                           playersRewards = getPlayerRewards(playerName);
                           playersRewards[index].active = false;
                           putPlayerRewards(playerName, playersRewards);
                           this.scheduleRewardTask(playerName, 0, 100L, this.createDelayReward(reward.scope));
                           if (reward.link != null) {
                              _plugin.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new Runnable() {
                                 public void run() {
                                    Rewards.this.scheduleLinkReward(playerName, reward);
                                 }
                              }, 80L);
                           }

                           return;
                        }
                     } else {
                        LogFile.write(3, "ERROR {rewards.issue} 'group' promotion/demotion was scheduled, but permissions plugin not enabled.  No reward issued.");
                     }
                  } else if (eligibility == Rewards.rewardEligibility.OTHERWORLD) {
                     this.setReward(playerName, RewardData.EventReference.CHANGEWORLD, 0L, getPlayerRewards(playerName)[index].rewardID, reward, getPlayerRewards(playerName)[index].data);
                     Output.generate("output.reward.otherworld", playerName, reward, index);
                     LogFile.write(1, playerName + " was not in correct world (" + reward.world + ")  to receive reward " + reward.identifier + ". Reward set for future issue.");
                  } else {
                     LogFile.write(1, "{issue} player " + playerName + " not eligible for reward " + reward.identifier + " Reason: " + eligibility.toString());
                  }

                  if (success) {
                     LogFile.write(3, "Issued reward to " + playerName + " of " + this.rewardString(reward));
                  }

                  playersRewards = getPlayerRewards(playerName);
                  playersRewards[index].active = false;
                  this.scheduleLinkReward(playerName, reward);
                  if (reward.occurs == RewardData.Occurs.SINGLE && playersRewards[index].form == RewardInstance.RewardForm.STANDARD && playersRewards[index].scheduleNext) {
                     this.scheduleNextReward(playerName, reward.scope);
                  } else if (reward.occurs == RewardData.Occurs.RECURRING || reward.occurs == RewardData.Occurs.PERPETUAL) {
                     this.scheduleRepeatingReward(Players.getData(playerName), playersRewards[index].rewardID);
                  }

               }
            }
         }
      }
   }

   String replaceString(String token, String insert, String s) {
      StringBuilder sb = new StringBuilder(64);
      if (s.contains(token)) {
         sb.append(s.substring(0, s.indexOf(token)));
         sb.append(insert);
         sb.append(s.substring(s.indexOf(token) + token.length()));
         return sb.toString();
      } else {
         return s;
      }
   }

   boolean scheduleLinkReward(String playerName, RewardData reward) {
      if (reward.link != null) {
         LogFile.write(0, "Found link from " + reward.identifier + " to " + reward.link);
         int linkID = this.getRewardID(reward.link);
         if (linkID >= 0) {
            if (this.isEligible(playerName, this.rewardData[linkID]) == Rewards.rewardEligibility.ELIGIBLE) {
               int rewardSlot = this.findPlayerRewardTask(playerName, linkID);
               if (rewardSlot >= 0) {
                  RewardInstance playerReward = getPlayerRewards(playerName)[rewardSlot];
                  LogFile.write(1, "Canceling old chain reward " + (linkID + 1) + " so it can be reset.");
                  _plugin.getServer().getScheduler().cancelTask(playerReward.scheduleID);
                  playerReward.scheduleID = -1;
                  playerReward.active = false;
               }

               this.setReward(playerName, this.rewardData[linkID].reference, this.rewardData[linkID].time, linkID, this.rewardData[linkID], (String[])null);
               return true;
            } else {
               return false;
            }
         } else {
            LogFile.write(10, "Reward " + reward.identifier + " is linked with an invalid rewardTag " + reward.link + ". No link reward scheduled for " + playerName);
            return false;
         }
      } else {
         return false;
      }
   }

   public void processAbsenceRewards() {
      RewardData reward = null;

      for(int i = 0; i < _plugin.get_rewards().getNumDefinedRewards(); ++i) {
         reward = _plugin.get_rewards().getRewardData()[i];
         if (reward.reference == RewardData.EventReference.ABSENCE) {
            long lastLoginDay = TimeUnit.DAYS.toMillis(TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis())) - reward.time;
            ResultSet rs = _plugin.get_dataio().getAbsentPlayerSet(lastLoginDay);

            try {
               if (rs.first()) {
                  for(boolean more = true; more; more = rs.next()) {
                     String refer = rs.getString("referredby");
                     String playerName = rs.getString("playerName");
                     if (!refer.contains("ontime") && !refer.contains("votifier")) {
                        OfflinePlayer offlinePlayer = Players.getOfflinePlayer(playerName);
                        switch (reward.type) {
                           case ADDGROUP:
                           case COMMAND:
                           case DEMOTION:
                           case DENIAL:
                           case MESSAGE:
                           case PERMISSION:
                           case PROMOTION:
                           case REMOVEGROUP:
                              if (!this.checkForReward(playerName, reward.identifier) && _plugin.get_rewards().setReward(playerName, RewardData.EventReference.LOGIN, 0L, i, reward, (String[])null) > -1) {
                                 LogFile.write(2, "Absence reward of " + _plugin.get_rewards().rewardString(reward) + " set for " + playerName);
                              }
                           case DELAY:
                           case ITEM:
                           case KIT:
                           default:
                              break;
                           case ECONOMY:
                              if (OnTime.economy != null) {
                                 EconomyResponse response;
                                 if (Integer.parseInt(reward.reward) > 0) {
                                    response = OnTime.economy.depositPlayer(offlinePlayer, (double)Integer.parseInt(reward.reward));
                                 } else {
                                    response = OnTime.economy.withdrawPlayer(offlinePlayer, (double)(-Integer.parseInt(reward.reward)));
                                 }

                                 if (response.type == ResponseType.SUCCESS) {
                                    LogFile.write(2, "Absence Econ reward issued: " + playerName + "   Amount delta:" + response.amount + "    New Balance:" + response.balance);
                                 } else {
                                    LogFile.write(2, "Absence Econ reward not issued to " + playerName + ". Econ plugin returned error: " + response.errorMessage);
                                 }
                              } else {
                                 LogFile.write(2, "Absence Econ reward not issued to " + playerName + " because Economy not found.");
                              }
                              break;
                           case POINTS:
                              _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "points", rs.getInt("points") + reward.getQuantity(), playerName);
                              LogFile.write(2, "Absence point change issued to " + playerName + ". New points total: " + (rs.getInt("points") + reward.getQuantity()));
                        }
                     }
                  }
               }
            } catch (SQLException var11) {
               var11.printStackTrace();
            }
         }
      }

   }

   public void processDeathRewards(String playerName) {
      RewardData reward = null;
      if (deathReward) {
         for(int i = 0; i < _plugin.get_rewards().getNumDefinedRewards(); ++i) {
            reward = _plugin.get_rewards().getRewardData()[i];
            if (reward.reference == RewardData.EventReference.DEATH && _plugin.get_rewards().setReward(playerName, RewardData.EventReference.PLAYTIME, 0L, i, reward, (String[])null) > -1) {
               LogFile.console(0, "Death reward of " + _plugin.get_rewards().rewardString(reward) + " set for " + playerName);
            }
         }
      } else {
         LogFile.console(0, "No death reward defined.");
      }

   }

   public static boolean processShopReward(String playerName, String idTag) {
      return _plugin.get_rewards()._processShopReward(playerName, idTag);
   }

   public boolean _processShopReward(String playerName, String idTag) {
      RewardData reward = null;
      int rewardID = true;
      Player player = Players.getOnlinePlayer(playerName);
      OfflinePlayer offlinePlayer = Players.getOfflinePlayer(playerName);
      int rewardID;
      if ((rewardID = this._validateIdentifier(idTag, -1)) >= 0) {
         PlayerData playerData = Players.getData(playerName);
         reward = _plugin.get_rewards().getRewardData()[rewardID];
         rewardEligibility isEligible = this.isEligible(playerName, reward);
         if (isEligible == Rewards.rewardEligibility.ELIGIBLE) {
            if (reward.reference == RewardData.EventReference.SHOP_POINTS) {
               if (playerData.points >= reward.count) {
                  playerData.points -= reward.count;
                  _plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "points", playerData.points, playerName);
                  if (_plugin.get_rewards().setReward(playerName, reward.reference, 0L, rewardID, reward, (String[])null) > -1) {
                     LogFile.write(1, "Points shop reward of " + _plugin.get_rewards().rewardString(reward) + " set for " + playerName);
                     Output.generate("output.shop.success", playerName, reward, rewardID);
                     return true;
                  }

                  LogFile.write(3, "{_processRewardShop} setReward failed for " + playerName + " purchase reward of " + reward.identifier);
                  return false;
               }

               Output.generate("output.shop.noPoints", playerName, reward, rewardID);
               return false;
            }

            if (reward.reference == RewardData.EventReference.SHOP_ECON) {
               if (OnTime.economy != null) {
                  double balance = OnTime.economy.getBalance(offlinePlayer);
                  if (balance >= (double)reward.count) {
                     EconomyResponse response = OnTime.economy.withdrawPlayer(offlinePlayer, (double)reward.count);
                     if (response.type == ResponseType.SUCCESS) {
                        if (_plugin.get_rewards().setReward(playerName, reward.reference, 0L, rewardID, reward, (String[])null) > -1) {
                           LogFile.write(1, "Econ shop reward of " + _plugin.get_rewards().rewardString(reward) + " set for " + playerName);
                           Output.generate("output.shop.success", playerName, reward, rewardID);
                           return true;
                        }

                        LogFile.write(3, "{_processRewardShop} setReward failed for " + playerName + " purchase reward of " + reward.identifier);
                        return false;
                     }

                     LogFile.write(10, "Econ Shop purchase failed for " + playerName + ". Econ plugin returned error: " + response.errorMessage);
                  } else {
                     Output.generate("output.shop.noFunds", playerName, reward, rewardID);
                  }
               } else {
                  LogFile.console(3, "Econ shop purchase failed for " + playerName + " because Economy not found.");
               }
            } else {
               LogFile.write(10, "{_processShopReward} reward.reference invalid: " + reward.reference.toString());
            }
         } else if (player != null) {
            if (isEligible == Rewards.rewardEligibility.EXCLUSIVE) {
               Output.generate("output.shop.noPermission", playerName, reward, rewardID);
            } else if (isEligible != Rewards.rewardEligibility.HIGHERGROUP && isEligible != Rewards.rewardEligibility.LOWERGROUP) {
               Output.generate("output.shop.notEligible", playerName, reward, rewardID);
            } else {
               Output.generate("output.shop.noRankChange", playerName, reward, rewardID);
            }
         } else {
            LogFile.write(10, "{_processShopReward} player was null: " + playerName + " rewardTag =" + idTag);
         }
      } else {
         LogFile.write(10, "{_processShopReward} Reward Tag not found: " + idTag + " player =" + playerName);
      }

      return false;
   }

   void rewardNotifyOut(String outString, String playerName, RewardData reward, int index) {
      if (OnTime.rewardNotifyEnable && !reward.message.equalsIgnoreCase("off")) {
         if (!reward.message.equalsIgnoreCase("default")) {
            if (Messages.messages.getString("message." + reward.message) == null) {
               LogFile.write(3, "{rewardNotifyOut} Reward " + reward.identifier + " assigned undefined message:" + reward.message + ".  No message displayed to player");
            } else {
               _plugin.get_messages().generate(reward.message, playerName, (String[])null);
            }

            return;
         }

         if (reward.occurs == RewardData.Occurs.REFERSOURCE) {
            Output.generate("output.reward.referred", playerName, reward, index);
            Output.broadcast(Output.OnTimeOutput, "output.broadcast.referred", (String)null, playerName, reward, index);
         } else if (reward.occurs == RewardData.Occurs.REFERTARGET) {
            Output.generate("output.reward.referralTarget", playerName, reward, index);
            Output.broadcast(Output.OnTimeOutput, "output.broadcast.referralTarget", (String)null, playerName, reward, index);
         } else if (reward.occurs == RewardData.Occurs.DAYSON) {
            Output.generate("output.reward.dayson", playerName, reward, index);
         } else if (reward.occurs == RewardData.Occurs.TOP) {
            if (reward.scope == RewardData.timeScope.TOTAL) {
               Output.generate("output.reward.topTotal", playerName, reward, index);
               Output.broadcast(Output.OnTimeOutput, "output.broadcast.topTotal", (String)null, playerName, reward, index);
            } else if (reward.scope == RewardData.timeScope.DAILY) {
               Output.generate("output.reward.topDaily", playerName, reward, index);
               Output.broadcast(Output.OnTimeOutput, "output.broadcast.topDaily", (String)null, playerName, reward, index);
            } else if (reward.scope == RewardData.timeScope.WEEKLY) {
               Output.generate("output.reward.topWeekly", playerName, reward, index);
               Output.broadcast(Output.OnTimeOutput, "output.broadcast.topWeekly", (String)null, playerName, reward, index);
            } else if (reward.scope == RewardData.timeScope.MONTHLY) {
               Output.generate("output.reward.topMontly", playerName, reward, index);
               Output.broadcast(Output.OnTimeOutput, "output.broadcast.topMonthly", (String)null, playerName, reward, index);
            }
         } else if (reward.occurs != RewardData.Occurs.VOTE_P && reward.occurs != RewardData.Occurs.VOTE_S) {
            if (reward.reference != RewardData.EventReference.SHOP_ECON && reward.reference != RewardData.EventReference.SHOP_POINTS) {
               Output.generate(outString, playerName, reward, index);
               if (reward.type == RewardData.RewardType.PROMOTION) {
                  Output.broadcast(Output.OnTimeOutput, "output.broadcast.group", (String)null, playerName, reward, index);
               }
            } else {
               Output.generate("output.reward.shop", playerName, reward, index);
            }
         } else {
            Output.generate("output.reward.votifier", playerName, reward, index);
            Output.broadcast(Output.OnTimeOutput, "output.broadcast.votifier", (String)null, playerName, reward, index);
         }
      }

      if (getPlayerRewards(playerName)[index].reference == RewardData.EventReference.CHANGEWORLD) {
         Output.generate("output.reward.rightworld", playerName, reward, index);
      }

   }

   public void cancelPlayerRewardTasks(String playerName, String worldName) {
      LogFile.write(0, "{cancelPlayerRewardTasks} Attempting to cancel all rewards for " + playerName);
      if (playerHasRewards(playerName)) {
         RewardInstance[] reward = getPlayerRewards(playerName);

         for(int i = 0; i < reward.length; ++i) {
            if (reward[i].active && reward[i].scheduleID >= 0 && (worldName.equalsIgnoreCase("all") || worldName.equalsIgnoreCase(this.getRewardData()[reward[i].index].world))) {
               _plugin.getServer().getScheduler().cancelTask(reward[i].scheduleID);
               reward[i].scheduleID = -1;
               if (reward[i].form == RewardInstance.RewardForm.STANDARD) {
                  reward[i].active = false;
               }

               LogFile.write(1, "Reward: " + reward[i].identifier + " for " + playerName + " cancelled.");
            }
         }
      }

   }

   public int findPlayerRewardTask(String playerName, int rewardID) {
      if (playerHasRewards(playerName)) {
         RewardInstance[] reward = getPlayerRewards(playerName);

         for(int i = 0; i < reward.length; ++i) {
            if (reward[i].active && reward[i].rewardID == rewardID) {
               return i;
            }
         }
      }

      return -1;
   }

   public boolean deleteAllIndiRewards(String playerName) {
      boolean rewardsDeleted = false;
      if (playerHasRewards(playerName)) {
         RewardInstance[] reward = getPlayerRewards(playerName);

         for(int i = 0; i < reward.length; ++i) {
            if (reward[i].active && reward[i].form == RewardInstance.RewardForm.PERSONAL) {
               if (reward[i].scheduleID > 0) {
                  _plugin.getServer().getScheduler().cancelTask(reward[i].scheduleID);
                  reward[i].scheduleID = -1;
               }

               reward[i].active = false;
               rewardsDeleted = true;
               LogFile.write(3, "Reward " + i + " for " + playerName + " deleted via command.");
            }
         }
      }

      return rewardsDeleted;
   }

   public void resetRewardTasks() {
      LogFile.write(0, "{resetRewardTasks} Resetting all rewards.");
      if (OnTime.rewardsEnable) {
         this.cancelAllRewardTasks();
         this.scheduleAllRewardTasks();
      }

   }

   public void cancelAllRewardTasks() {
      Iterator var3 = _plugin.getServer().getOnlinePlayers().iterator();

      while(var3.hasNext()) {
         Player player = (Player)var3.next();
         String playerName = OnTime.getPlayerName(player);
         LogFile.write(0, "{cancelRewardTasks} Cancelling all rewards for " + playerName);
         this.cancelPlayerRewardTasks(playerName, "all");
      }

   }

   public void scheduleAllRewardTasks() {
      Iterator var3 = _plugin.getServer().getOnlinePlayers().iterator();

      while(var3.hasNext()) {
         Player player = (Player)var3.next();
         String playerName = OnTime.getPlayerName(player);
         if (_plugin.get_permissionsHandler().playerHas(playerName, "ontime.track")) {
            this.scheduleNextReward(playerName, (RewardData.timeScope)null);
            this.scheduleRepeatingReward(Players.getData(playerName), -1);
         } else {
            LogFile.write(1, playerName + " not scheduled for rewards. OnTime tracking is not enabled for this player.");
         }
      }

      var3 = this.getRewardMap().keySet().iterator();

      while(var3.hasNext()) {
         String key = (String)var3.next();
         this.scheduleIndiRewards(key, Rewards.indiScheduleSource.RESET);
      }

   }

   public long getPersonalRemainingTime(RewardInstance reward, String playerName) {
      if (reward.reference == RewardData.EventReference.REALTIME) {
         return reward.time - Calendar.getInstance().getTimeInMillis();
      } else {
         return reward.reference != RewardData.EventReference.DELTATIME && reward.reference != RewardData.EventReference.PLAYTIME ? 0L : reward.time - _plugin.get_playingtime().totalOntime(playerName);
      }
   }

   public long getPersonalRewardTime(long time, RewardData.EventReference reference, String playerName) {
      if (reference == RewardData.EventReference.REALTIME) {
         return Calendar.getInstance().getTimeInMillis() + time;
      } else if (reference == RewardData.EventReference.DELTATIME) {
         return _plugin.get_playingtime().totalOntime(playerName) + time;
      } else {
         return reference == RewardData.EventReference.PLAYTIME ? time : 0L;
      }
   }

   public void list(CommandSender sender, listRequest request) {
      StringBuilder sb = new StringBuilder(64);
      if (this.getNumDefinedRewards() == 0) {
         sender.sendMessage("No rewards defined.");
      } else {
         for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
            int j = i + 1;
            RewardData reward = this.getRewardData()[i];
            sb.append("#" + j);
            if (request != Rewards.listRequest.EVENT) {
               sb.append(" Tag= " + reward.identifier);
            } else {
               boolean appendTime = true;
               if (reward.occurs == RewardData.Occurs.TOP) {
                  sb.append(" Top " + reward.scope.label() + " " + reward.reference.label() + " ");
                  sb.append("# " + reward.time);
                  if (reward.time != reward.recurranceTime) {
                     sb.append("-" + reward.recurranceTime);
                  }

                  appendTime = false;
               } else if (reward.occurs == RewardData.Occurs.KITELEMENT) {
                  sb.append(" " + reward.identifier);
                  appendTime = false;
               } else if (reward.occurs == RewardData.Occurs.DAYSON) {
                  sb.append(" DaysOn: " + reward.count + " ");
                  appendTime = false;
               } else if (reward.reference == RewardData.EventReference.DEATH) {
                  sb.append(" Death ");
                  appendTime = false;
               } else if (reward.reference == RewardData.EventReference.SHOP_POINTS) {
                  sb.append(" Shop " + reward.count + " " + Output.OnTimeOutput.getString("output.eventRef.point"));
                  appendTime = false;
               } else if (reward.reference == RewardData.EventReference.SHOP_ECON) {
                  sb.append(" Shop " + reward.count + " " + OnTime.economy.currencyNamePlural());
                  appendTime = false;
               } else if (reward.occurs == RewardData.Occurs.VOTE_P || reward.occurs == RewardData.Occurs.VOTE_S) {
                  sb.append(" Votes: ");
                  if (reward.count > 0) {
                     sb.append(reward.count);
                  } else {
                     sb.append("1");
                  }

                  if (reward.time <= 1000L) {
                     appendTime = false;
                  }
               }

               if (appendTime) {
                  sb.append(" " + reward.scope.label() + ": ");
                  sb.append(RewardUtilities.getTerseRewardTimeBreakdown(reward.time));
                  if (reward.reference == RewardData.EventReference.ABSENCE) {
                     sb.append(" absence ");
                  }
               }
            }

            this.tab(sb, 27);
            sb.append(" R: ");
            sb.append(this.rewardString(reward) + " (" + reward.occurs.code() + reward.exclusive + ")");
            sender.sendMessage(sb.toString());
            sb.setLength(0);
            if (this.getRewardData()[i].occurs == RewardData.Occurs.VOTE_P) {
               sb.append("   Interval: " + this.getRewardData()[i].count + " votes");
               sender.sendMessage(sb.toString());
               sb.setLength(0);
            } else if (this.getRewardData()[i].occurs == RewardData.Occurs.RECURRING || this.getRewardData()[i].occurs == RewardData.Occurs.PERPETUAL) {
               sb.append("   Interval: " + Output.getTimeBreakdown(this.getRewardData()[i].recurranceTime, Output.TIMEDETAIL.SHORT));
               if (this.getRewardData()[i].occurs == RewardData.Occurs.RECURRING && this.getRewardData()[i].count > 0) {
                  sb.append(" Count: " + this.getRewardData()[i].count);
               }

               sender.sendMessage(sb.toString());
               sb.setLength(0);
            }

            if (this.getRewardData()[i].occurs == RewardData.Occurs.REFERSOURCE && this.getRewardData()[i].count > 0) {
               sender.sendMessage(" Count: " + this.getRewardData()[i].count);
            }
         }

      }
   }

   public void add(CommandSender sender, RewardData.Occurs occurs, long newTime, RewardData.RewardType type, int quantity, String reward, String rewardID, RewardData.timeScope scope, RewardData.EventReference reference, String message, String world) {
      for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
         if (newTime == this.getRewardData()[i].time) {
            ++newTime;
            if (sender != null) {
               sender.sendMessage("Warning: A reward of " + this.getRewardData()[i].reward + " already exists for " + Output.getTimeBreakdown(newTime, Output.TIMEDETAIL.SHORT));
            }
         }
      }

      RewardData[] newArray = new RewardData[this.getNumDefinedRewards() + 1];
      if (this.getNumDefinedRewards() > 0) {
         System.arraycopy(this.getRewardData(), 0, newArray, 0, this.getNumDefinedRewards());
      }

      String identifier = rewardID;
      if (rewardID == null) {
         identifier = occurs.code() + String.valueOf(rewardIDCounter) + type + quantity + reward;
      }

      newArray[this.getNumDefinedRewards()] = new RewardData(occurs, "A", scope, reference, (String)null, message, world, newTime, 0L, -1, type, quantity, reward, identifier);
      int var10002 = this.scopeCount[scope.ordinal()]++;
      Arrays.sort(newArray);
      this.setRewardData(newArray);
      this.setNumDefinedRewards(this.getNumDefinedRewards() + 1);
      ++rewardIDCounter;
      if (sender != null) {
         RewardUtilities.saveRewards(rewardFile);
         this.resetRewardTasks();
      }

      if (sender != null) {
         for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
            if (this.rewardData[i].identifier.equalsIgnoreCase(identifier)) {
               sender.sendMessage("New reward successfully added:");
               this.displayInfo(sender, i, "");
               break;
            }
         }
      }

      if (reference == RewardData.EventReference.DEATH) {
         deathReward = true;
      }

   }

   public boolean setRecurring(RewardData.Occurs occurs, int rewardID, long time, int count) {
      if (rewardID < this.getNumDefinedRewards() && rewardID >= 0) {
         this.getRewardData()[rewardID].occurs = occurs;
         this.getRewardData()[rewardID].count = count;
         this.getRewardData()[rewardID].recurranceTime = time;
         if (this.getRewardData()[rewardID].recurranceTime == 0L) {
            this.getRewardData()[rewardID].recurranceTime = TimeUnit.MINUTES.toMillis(1L);
         }

         if (occurs == RewardData.Occurs.INDIVIDUAL) {
            this.getRewardData()[rewardID].time = TimeUnit.DAYS.toMillis(9999L);
            Arrays.sort(this.getRewardData());
         } else if (this.getRewardData()[rewardID].time >= TimeUnit.DAYS.toMillis(9999L)) {
            this.getRewardData()[rewardID].time = 1L;
         }

         RewardUtilities.saveRewards(rewardFile);
         this.resetRewardTasks();
         return true;
      } else {
         LogFile.write(3, "{rewards.setRecurring} ERROR. Bad rewardID specified. (" + rewardID + "). Max defined reward: " + this.getNumDefinedRewards());
         return false;
      }
   }

   public String setReference(int rewardID, String sourceTarget, int count) {
      if (rewardID < this.getNumDefinedRewards() && rewardID >= 0) {
         if (this.getRewardData()[rewardID].occurs == RewardData.Occurs.INDIVIDUAL) {
            return ChatColor.RED + "An 'indi' reward cannot be used for referrals.";
         } else {
            if (sourceTarget.equalsIgnoreCase("target")) {
               this.getRewardData()[rewardID].occurs = RewardData.Occurs.REFERTARGET;
            } else {
               this.getRewardData()[rewardID].occurs = RewardData.Occurs.REFERSOURCE;
            }

            this.getRewardData()[rewardID].count = count;
            RewardUtilities.saveRewards(rewardFile);
            this.resetRewardTasks();
            return "Referral reward sucessfully set.";
         }
      } else {
         return ChatColor.RED + "Invalid RewarID specified.";
      }
   }

   public boolean setTime(int rewardID, long time) {
      if (rewardID < this.getNumDefinedRewards() && rewardID >= 0) {
         this.getRewardData()[rewardID].time = time;
         Arrays.sort(this.getRewardData());
         RewardUtilities.saveRewards(rewardFile);
         this.resetRewardTasks();
         return true;
      } else {
         LogFile.console(3, "{rewards.setRecurring} ERROR. Bad rewardID specified. (" + rewardID + "). Max defined reward: " + this.getNumDefinedRewards());
         return false;
      }
   }

   public String setChain(int rewardID, int chainID, boolean splice) {
      this.rewardData[rewardID].link = this.rewardData[chainID].identifier;
      if (!splice) {
         this.rewardData[chainID].occurs = RewardData.Occurs.CHAIN;
      }

      RewardUtilities.saveRewards(rewardFile);
      this.resetRewardTasks();
      return "Reward " + (rewardID + 1) + " now linked with reward " + (chainID + 1);
   }

   public int getPreviousLink(int chainID) {
      for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
         if (this.rewardData[i].link != null && this.rewardData[i].link.equalsIgnoreCase(this.rewardData[chainID].identifier)) {
            return i;
         }
      }

      return -1;
   }

   public boolean updateLink(String oldLink, String newLink) {
      boolean changedOne = false;

      for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
         if (this.rewardData[i].link != null && this.rewardData[i].link.equalsIgnoreCase(oldLink)) {
            this.rewardData[i].link = newLink;
            changedOne = true;
         }
      }

      return changedOne;
   }

   public boolean setExclusive(String exclusive, String addRemove, int rewardID, int numGroups, String[] groups) {
      if (rewardID < this.getNumDefinedRewards() && rewardID >= 0) {
         this.getRewardData()[rewardID].exclusive = exclusive;
         if (exclusive.equalsIgnoreCase("E")) {
            if (OnTime.permission.isEnabled()) {
               for(int i = 0; i < numGroups; ++i) {
                  if (addRemove.equalsIgnoreCase("add")) {
                     _plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.GAP, groups[i], this.rewardData[rewardID].permissionString);
                     LogFile.write(1, "Permission " + this.rewardData[rewardID].permissionString + " added to group " + groups[i]);
                  } else {
                     _plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.GRP, groups[i], this.rewardData[rewardID].permissionString);
                     LogFile.write(1, "Permission " + this.rewardData[rewardID].permissionString + " removed from group " + groups[i]);
                  }
               }
            }
         } else {
            this.updatePermInAllGroups(this.rewardData[rewardID].permissionString, (String)null);
         }

         RewardUtilities.saveRewards(rewardFile);
         this.resetRewardTasks();
         return true;
      } else {
         LogFile.write(3, "{rewards.setRecurring} ERROR. Bad rewardID specified. (" + rewardID + "). Max defined reward: " + this.getNumDefinedRewards());
         return false;
      }
   }

   public boolean setTop(Integer rewardID, RewardData.timeScope scope, RewardData.EventReference reference, Integer first, Integer last) {
      this.getRewardData()[rewardID].occurs = RewardData.Occurs.TOP;
      this.getRewardData()[rewardID].scope = scope;
      this.getRewardData()[rewardID].reference = reference;
      this.getRewardData()[rewardID].time = (long)first;
      this.getRewardData()[rewardID].recurranceTime = (long)last;
      RewardUtilities.saveRewards(rewardFile);
      return true;
   }

   public String enchantmentList(ItemStack itemstack) {
      StringBuilder sb = new StringBuilder();
      sb.append("");
      if (itemstack != null && itemstack.getEnchantments() != null) {
         Map map = itemstack.getEnchantments();
         Iterator var5 = map.keySet().iterator();

         while(var5.hasNext()) {
            Enchantment key = (Enchantment)var5.next();
            sb.append("+" + key.getName());
         }
      }

      return sb.toString();
   }

   public String rewardString(RewardData reward) {
      if (reward.type == RewardData.RewardType.ECONOMY) {
         return OnTime.economy == null ? reward.reward + " " + " <Economy not found>" : reward.reward + " " + OnTime.economy.currencyNamePlural();
      } else if (reward.type == RewardData.RewardType.ITEM) {
         String[] material = reward.reward.split("[+:]");
         if (material[0].equalsIgnoreCase("potion") && _plugin.get_rewards().getPotionsMap().containsKey(reward.itemstack.getDurability())) {
            return reward.getQuantity() + " of " + (String)_plugin.get_rewards().getPotionsMap().get(reward.itemstack.getDurability());
         } else {
            return reward.itemstack.getDurability() != 0 ? (reward.getQuantity() + " of " + material[0].toLowerCase() + this.enchantmentList(reward.itemstack)).toLowerCase() + ":" + reward.itemstack.getDurability() : (reward.getQuantity() + " of " + material[0].toLowerCase() + this.enchantmentList(reward.itemstack)).toLowerCase();
         }
      } else if (reward.type == RewardData.RewardType.PROMOTION) {
         return "promotion to " + reward.reward;
      } else if (reward.type == RewardData.RewardType.DEMOTION) {
         return "demotion to " + reward.reward;
      } else if (reward.type == RewardData.RewardType.ADDGROUP) {
         return "add group " + reward.reward;
      } else if (reward.type == RewardData.RewardType.REMOVEGROUP) {
         return "remove group " + reward.reward;
      } else if (reward.type == RewardData.RewardType.PERMISSION) {
         return "+permission: " + reward.reward;
      } else if (reward.type == RewardData.RewardType.DENIAL) {
         return "-permission: " + reward.reward;
      } else if (reward.type == RewardData.RewardType.XP) {
         return reward.getQuantity() + " XP";
      } else if (reward.type == RewardData.RewardType.POINTS) {
         return reward.getQuantity() + " Loyalty Points";
      } else if (reward.type == RewardData.RewardType.COMMAND) {
         String command = this.getCommand(reward.reward);
         return command == null ? "undefined command " + reward.reward : "command execution: " + this.getCommand(reward.reward);
      } else if (reward.type == RewardData.RewardType.KIT) {
         return reward.reward + " kit";
      } else {
         return reward.type == RewardData.RewardType.MESSAGE ? "Message: " + reward.reward : "UNKNOWN REWARD TYPE";
      }
   }

   public String getCurrentGroup(String playerName) {
      if (OnTime.permission == null) {
         LogFile.write(10, "{getCurrentGroup} No Permissions plugin is latched with OnTime.");
         return null;
      } else if (OnTime.permission.getName().equalsIgnoreCase("SuperPerms")) {
         LogFile.write(10, "{getCurrentGroup} " + OnTime.permission.getName() + " does not support group functions. ");
         return null;
      } else {
         OfflinePlayer player = null;
         if ((player = Players.getOfflinePlayer(playerName)) == null) {
            return null;
         } else if (player.getName() == null) {
            return null;
         } else {
            String currentGroup = null;
            if (OnTime.rewardsEnable) {
               int currentGroupID = 0;
               String[] playerGroups = null;
               playerGroups = OnTime.permission.getPlayerGroups((String)null, player);
               if (playerGroups != null && playerGroups.length > 0) {
                  if (playerGroups.length > 0) {
                     for(int i = 0; i < playerGroups.length; ++i) {
                        int checkGroupID = this.findGroup(playerGroups[i]);
                        if (checkGroupID > currentGroupID) {
                           currentGroupID = checkGroupID;
                           currentGroup = playerGroups[i];
                        }
                     }
                  }

                  if (currentGroup == null) {
                     LogFile.write(10, "{rewards.getCurrentGroup} Could not find " + playerName + " group(s) " + Arrays.asList(playerGroups) + " in reward.yml");
                  }

                  return currentGroup;
               }
            }

            currentGroup = OnTime.permission.getPrimaryGroup((String)null, player);
            return currentGroup;
         }
      }
   }

   public boolean isValidGroup(String group) {
      if (this.groupList == null) {
         return false;
      } else if (this.groupList.length == 0) {
         return false;
      } else {
         for(int i = 0; i < this.groupList.length; ++i) {
            if (this.groupList[i].equalsIgnoreCase(group)) {
               return true;
            }
         }

         return false;
      }
   }

   public int findGroup(String group) {
      Iterator it = this.groups.iterator();
      int count = 0;

      boolean foundGroup;
      for(foundGroup = false; it.hasNext(); ++count) {
         String value = (String)it.next();
         if (value.equalsIgnoreCase(group)) {
            foundGroup = true;
            break;
         }
      }

      return foundGroup ? count + 1 : 0;
   }

   public void updatePermInAllGroups(String oldPerm, String newPerm) {
      if (this.groupList != null && this.groupList.length != 0) {
         for(int i = 0; i < this.groupList.length; ++i) {
            if (_plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.GRP, this.groupList[i], oldPerm) && newPerm != null) {
               _plugin.get_permissionsHandler().addOrRemove(PermissionsHandler.ACTION.GAP, this.groupList[i], newPerm);
            }
         }

      }
   }

   public String getCommand(String command) {
      Iterator it = this.getCommands().iterator();

      while(it.hasNext()) {
         String value = (String)it.next();
         if (value.startsWith(command + ":")) {
            return value.substring(command.length() + 1);
         }
      }

      return null;
   }

   public promoOrDemo promotionORdemotion(String playerName, RewardData reward) {
      int currentGroupID = false;
      String currentGroup = "none";
      Player player = Players.getOnlinePlayer(playerName);
      if (player == null) {
         LogFile.write(3, "{rewards.canPromote} " + playerName + " expected, but is not OnLine.");
         return Rewards.promoOrDemo.ERROR;
      } else {
         currentGroup = this.getCurrentGroup(playerName);
         int currentGroupID;
         if (currentGroup == null) {
            LogFile.write(1, playerName + "current group not found in " + OnTime.permission.getName() + " config file.  Group change will be attempted.");
            currentGroupID = 0;
         } else {
            currentGroupID = this.findGroup(currentGroup);
         }

         int newGroupID = this.findGroup(reward.reward);
         LogFile.write(0, "{ontime.canPromote} " + playerName + " Old: " + currentGroup + " New: " + reward.reward);
         if (newGroupID < currentGroupID) {
            return Rewards.promoOrDemo.DEMOTION;
         } else {
            return newGroupID > currentGroupID ? Rewards.promoOrDemo.PROMOTION : Rewards.promoOrDemo.NO_CHANGE;
         }
      }
   }

   public static String validateShopIdentifer(String identifier) {
      return _plugin.get_rewards()._validateShopIdentifier(identifier);
   }

   public String _validateShopIdentifier(String identifier) {
      int rewardID = _plugin.get_rewards()._validateIdentifier(identifier, -1);
      if (rewardID < 0) {
         return null;
      } else {
         return this.getRewardData()[rewardID].reference != RewardData.EventReference.SHOP_POINTS && this.getRewardData()[rewardID].reference != RewardData.EventReference.SHOP_ECON ? null : this.getRewardData()[rewardID].reference.code();
      }
   }

   public int _validateIdentifier(String identifier, int rewardID) {
      if (rewardID > 0 && rewardID < this.getNumDefinedRewards() && this.getRewardData()[rewardID].identifier.equalsIgnoreCase(identifier)) {
         return rewardID;
      } else {
         int newLocation = -1;

         for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
            if (this.getRewardData()[i].identifier.equalsIgnoreCase(identifier)) {
               newLocation = i;
            }
         }

         return newLocation;
      }
   }

   public void remove(CommandSender sender, int rewardNo) {
      if (rewardNo <= this.getNumDefinedRewards() + 1 && rewardNo >= 0) {
         RewardData reward = this.rewardData[rewardNo];
         if (this.getRewardData()[rewardNo].exclusive.equalsIgnoreCase("E")) {
            this.updatePermInAllGroups(reward.permissionString, (String)null);
         }

         for(int link = this.getPreviousLink(rewardNo); link > 0; link = this.getPreviousLink(rewardNo)) {
            this.rewardData[link].link = null;
            sender.sendMessage("Warning.  This reward was linked *BY* rewardTAG = " + this.rewardData[link].identifier + " and chain has been broken.");
         }

         if (this.rewardData[rewardNo].link != null) {
            sender.sendMessage("Warning.  This reward was linked *TO* rewardTAG = " + this.rewardData[rewardNo].link + " and chain has been broken.");
         }

         int var10002 = this.scopeCount[reward.scope.ordinal()]--;
         RewardData[] newArray = new RewardData[this.getNumDefinedRewards()];
         long remTime = this.getRewardData()[rewardNo].time;
         String remReward = this.rewardString(this.getRewardData()[rewardNo]);
         if (rewardNo == 0) {
            System.arraycopy(this.getRewardData(), 1, newArray, 0, this.getNumDefinedRewards() - 1);
         } else if (rewardNo == this.getNumDefinedRewards() - 1) {
            System.arraycopy(this.getRewardData(), 0, newArray, 0, this.getNumDefinedRewards() - 1);
         } else {
            System.arraycopy(this.getRewardData(), 0, newArray, 0, rewardNo);
            System.arraycopy(this.getRewardData(), rewardNo + 1, newArray, rewardNo, this.getNumDefinedRewards() - rewardNo - 1);
         }

         this.setRewardData(newArray);
         this.setNumDefinedRewards(this.getNumDefinedRewards() - 1);
         int removed = rewardNo + 1;
         sender.sendMessage("Reward # " + removed + " of " + remReward + " for " + Output.getTimeBreakdown(remTime, Output.TIMEDETAIL.SHORT) + " sucessfully deleted.");
         this.updateIndiRewards(-1);
         RewardUtilities.saveRewards(rewardFile);
         this.resetRewardTasks();
      } else {
         sender.sendMessage(ChatColor.RED + "There is no such reward record .");
      }
   }

   public String timeToRewardString(PlayerData playerData, int rewardIndex) {
      long playerTime = 0L;
      int rewardID = getPlayerRewards(playerData.playerName)[rewardIndex].rewardID;
      RewardData reward = this.getRewardData()[rewardID];
      String worldName = OnTime.serverID;
      if (OnTime.perWorldEnable && !reward.world.equalsIgnoreCase("all")) {
         worldName = reward.world;
      }

      playerTime = this.getCurrentScopeTime(playerData, reward.scope, worldName);
      if (reward.occurs == RewardData.Occurs.SINGLE) {
         return Output.getTimeBreakdown(reward.time - playerTime, Output.TIMEDETAIL.SHORT);
      } else {
         return reward.occurs != RewardData.Occurs.RECURRING && reward.occurs != RewardData.Occurs.PERPETUAL ? ": def. error. RewardID:" + rewardID + " Bad recurrence code: " + reward.occurs : Output.getTimeBreakdown(this.getNextOccurrence(playerData.playerName, rewardID) - playerTime, Output.TIMEDETAIL.SHORT);
      }
   }

   public void showSchedule(CommandSender sender) {
      if (_plugin.getServer().getOnlinePlayers().size() == 0) {
         sender.sendMessage(Output.OnTimeOutput.getString("output.rewardCMD.next.noPlayersOnline"));
      } else {
         Output.generate("output.rewardCMD.next.header", sender, (String[])null);
         Iterator var4 = _plugin.getServer().getOnlinePlayers().iterator();

         while(true) {
            while(var4.hasNext()) {
               Player player = (Player)var4.next();
               String playerName = OnTime.getPlayerName(player);
               if (playerHasRewards(playerName)) {
                  RewardInstance[] reward = getPlayerRewards(playerName);
                  boolean foundone = false;

                  for(int j = 0; j < reward.length; ++j) {
                     if (reward[j].active) {
                        foundone = true;
                     }
                  }

                  if (foundone) {
                     Output.generate((String)"output.rewardCMD.next.reward", (CommandSender)sender, playerName, (RewardData)null);
                  } else {
                     Output.generate((String)"output.rewardCMD.next.none", (CommandSender)sender, playerName, (RewardData)null);
                  }
               } else {
                  Output.generate((String)"output.rewardCMD.next.none", (CommandSender)sender, playerName, (RewardData)null);
               }
            }

            return;
         }
      }
   }

   public long getNextOccurrence(String playerName, int rewardID) {
      if (this.getRewardData()[rewardID].recurranceTime == 0L) {
         return 0L;
      } else {
         String worldName = OnTime.serverID;
         if (OnTime.perWorldEnable && !this.getRewardData()[rewardID].world.equalsIgnoreCase("all")) {
            worldName = this.getRewardData()[rewardID].world;
         }

         long currentPlayTime = this.getCurrentScopeTime(Players.getData(playerName), this.getRewardData()[rewardID].scope, worldName);

         long nextOccurrence;
         for(nextOccurrence = this.getRewardData()[rewardID].time; currentPlayTime >= nextOccurrence; nextOccurrence += this.getRewardData()[rewardID].recurranceTime) {
         }

         return nextOccurrence;
      }
   }

   public boolean checkSpecialRewards(String playerName, int count) {
      RewardData reward = null;
      boolean rewardSet = false;

      for(int i = 0; i < _plugin.get_rewards().getNumDefinedRewards(); ++i) {
         reward = _plugin.get_rewards().getRewardData()[i];
         if (reward.occurs == RewardData.Occurs.DAYSON) {
            if (reward.count == count) {
               rewardSet = true;
               _plugin.get_rewards().setReward(playerName, RewardData.EventReference.DELTATIME, 0L, i, reward, (String[])null);
               LogFile.write(1, "DaysON reward of " + _plugin.get_rewards().rewardString(reward) + " set for " + playerName);
            }

            if (reward.count > count) {
               LogFile.write(1, "DaysON reward (" + reward.identifier + ") not issued, " + playerName + " has not been on for " + reward.count + " yet. ");
            }
         }
      }

      return rewardSet;
   }

   public void updateIndiRewards(int newID) {
      if (!this.getRewardMap().isEmpty()) {
         Iterator var4 = this.getRewardMap().keySet().iterator();

         while(var4.hasNext()) {
            String key = (String)var4.next();
            RewardInstance[] playerRewards = getPlayerRewards(key);

            for(int i = 0; i < playerRewards.length; ++i) {
               if (playerRewards[i].active && playerRewards[i].form != RewardInstance.RewardForm.STANDARD) {
                  if (newID >= 0) {
                     if (playerRewards[i].rewardID == newID) {
                        playerRewards[i].identifier = this.getRewardData()[newID].identifier;
                     }
                  } else {
                     playerRewards[i].rewardID = this._validateIdentifier(playerRewards[i].identifier, playerRewards[i].rewardID);
                     if (playerRewards[i].rewardID < 0) {
                        playerRewards[i].active = false;
                        LogFile.write(3, "{updateIndiRewards} Indi reward " + playerRewards[i].identifier + " for " + key + " removed because reward no longer exists.");
                     }
                  }
               }
            }
         }
      }

   }

   public void saveIndiRewards(File folder) {
      StringBuilder sb = new StringBuilder(128);
      boolean firstOne = true;
      this.indiRewardFile = new File(folder, "indirewards.yml");
      RewardUtilities.deleteFile(this.indiRewardFile);
      if (!this.getRewardMap().isEmpty()) {
         Iterator var6 = this.getRewardMap().keySet().iterator();

         while(var6.hasNext()) {
            String key = (String)var6.next();
            RewardInstance[] playerRewards = getPlayerRewards(key);

            for(int i = 0; i < playerRewards.length; ++i) {
               if (playerRewards[i].active && playerRewards[i].form != RewardInstance.RewardForm.STANDARD) {
                  if (firstOne) {
                     RewardUtilities.createFile(this.indiRewardFile);
                     _plugin.copy(_plugin.getResource("indirewards.yml"), this.indiRewardFile);
                     RewardUtilities.writeLine(this.indiRewardFile, "indirewards:");
                     firstOne = false;
                  }

                  sb.append("   - " + key + "," + playerRewards[i].rewardID.toString() + "," + playerRewards[i].reference.label() + "," + playerRewards[i].time.toString() + "," + playerRewards[i].identifier);
                  if (playerRewards[i].data != null) {
                     for(int j = 0; j < playerRewards[i].data.length; ++j) {
                        sb.append("," + playerRewards[i].data[j]);
                     }
                  }

                  RewardUtilities.writeLine(this.indiRewardFile, sb.toString());
                  sb.delete(0, sb.length());
               }
            }
         }

         if (!firstOne) {
            LogFile.console(1, this.indiRewardFile.getName() + " updated.");
         }
      }

   }

   public void scheduleIndiRewards(final String playerName, indiScheduleSource caller) {
      if (playerHasRewards(playerName)) {
         RewardInstance[] rewards = getPlayerRewards(playerName);
         if (!this.canReceiveReward(playerName)) {
            for(int j = 0; j < rewards.length; ++j) {
               if (rewards[j].active && rewards[j].form == RewardInstance.RewardForm.PERSONAL) {
                  rewards[j].rewardID = this._validateIdentifier(rewards[j].identifier, rewards[j].rewardID);
                  if (rewards[j].rewardID < 0) {
                     rewards[j].active = false;
                  }
               }
            }

         } else {
            long delayTime = 0L;
            int count = 0;

            for(final int i = 0; i < rewards.length; ++i) {
               if (rewards[i].active && rewards[i].form != RewardInstance.RewardForm.STANDARD) {
                  boolean schedule = true;
                  if (rewards[i].form != RewardInstance.RewardForm.MESSAGE) {
                     if (this._validateIdentifier(rewards[i].identifier, rewards[i].rewardID) < 0) {
                        rewards[i].rewardID = -1;
                        rewards[i].active = false;
                        LogFile.write(3, "Active indi reward (" + rewards[i].identifier + ") no longer exists.  Reward was cancelled for " + playerName);
                        schedule = false;
                     } else if (rewards[i].reference == RewardData.EventReference.LOGIN && caller != Rewards.indiScheduleSource.LOGIN) {
                        schedule = false;
                     } else if (rewards[i].reference == RewardData.EventReference.CHANGEWORLD) {
                        if (caller != Rewards.indiScheduleSource.CHANGEWORLD && caller != Rewards.indiScheduleSource.LOGIN) {
                           schedule = false;
                        } else {
                           Player player = Players.getOnlinePlayer(playerName);
                           if (player != null && !player.getWorld().getName().equalsIgnoreCase(this.getRewardData()[rewards[i].rewardID].world)) {
                              schedule = false;
                           }
                        }
                     }
                  }

                  if (schedule) {
                     ++count;
                     if (rewards[i].reference != RewardData.EventReference.DELTATIME && rewards[i].reference != RewardData.EventReference.PLAYTIME) {
                        if (rewards[i].reference == RewardData.EventReference.REALTIME) {
                           delayTime = rewards[i].time - Calendar.getInstance().getTimeInMillis();
                        } else {
                           delayTime = 0L;
                        }
                     } else {
                        delayTime = rewards[i].time - _plugin.get_playingtime().totalOntime(playerName);
                     }

                     if (delayTime < 1000L) {
                        delayTime = TimeUnit.SECONDS.toMillis((long)(2 * count));
                     }

                     if (rewards[i].form == RewardInstance.RewardForm.PERSONAL) {
                        RewardData tempdata = this.getRewardData()[rewards[i].rewardID];
                        LogFile.write(1, "Indi reward of " + this.rewardString(tempdata) + " set for " + playerName + " in " + Output.getTimeBreakdown(delayTime, Output.TIMEDETAIL.SHORT));
                        rewards[i].scheduleID = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new issueReward(playerName, tempdata, i), delayTime / 50L);
                     } else {
                        final String tempID = rewards[i].identifier.substring(8);
                        LogFile.write(1, "Message of " + tempID + " set for " + playerName + " in " + Output.getTimeBreakdown(delayTime, Output.TIMEDETAIL.SHORT));
                        rewards[i].scheduleID = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(_plugin, new Runnable() {
                           public void run() {
                              Rewards._plugin.get_messages().issue(playerName, tempID, i, (String[])null);
                           }
                        }, delayTime / 50L);
                     }
                  }
               }
            }

         }
      }
   }

   public void displayInfo(CommandSender sender, int request, String preamble) {
      int first = request;
      int last = request;
      StringBuilder sb = new StringBuilder(64);
      if (request < 0) {
         first = 0;
         last = this.getNumDefinedRewards() - 1;
      }

      for(int id = first; id <= last; ++id) {
         sender.sendMessage(preamble + "Reward #" + (id + 1));
         sender.sendMessage(preamble + "(#" + (id + 1) + ") " + this.rewardString(this.rewardData[id]));
         if (this.rewardData[id].type == RewardData.RewardType.KIT) {
            kitClass[] kit = this.getKit(this.rewardData[id].reward);
            if (kit != null) {
               for(int i = 0; i < kit.length; ++i) {
                  sender.sendMessage(preamble + "(#" + (id + 1) + ") Kit Item #" + i + " : " + kit[i].quantity + " of " + kit[i].item);
               }
            } else {
               sender.sendMessage(preamble + "(#" + (id + 1) + ") " + this.rewardString(this.rewardData[id]) + " ERROR - Kit elements not found.");
            }
         }

         sb.append(this.rewardData[id].occurs.label() + " ");
         if (this.rewardData[id].exclusive.equalsIgnoreCase("A")) {
            sb.append("+General ");
         } else {
            sb.append("+Exclusive");
         }

         if (!this.rewardData[id].message.equalsIgnoreCase("default")) {
            if (this.rewardData[id].message.equalsIgnoreCase("off")) {
               sb.append("+msgOFF");
            } else {
               sb.append("+msgID =" + this.rewardData[id].message);
            }
         }

         sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Flags: " + sb.toString());
         sb.setLength(0);
         if (this.rewardData[id].occurs == RewardData.Occurs.TOP) {
            sb.append(preamble + "(#" + (id + 1) + ") " + "Top Rewarded Position(s): " + this.rewardData[id].time);
            if (this.rewardData[id].time != this.rewardData[id].recurranceTime) {
               sb.append(" - " + this.rewardData[id].recurranceTime);
            }

            sender.sendMessage(sb.toString());
            sb.setLength(0);
            sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Scope: " + this.rewardData[id].scope.label() + "     Reference: " + this.rewardData[id].reference.label());
         } else if (this.rewardData[id].occurs != RewardData.Occurs.KITELEMENT) {
            if (this.rewardData[id].occurs == RewardData.Occurs.DAYSON) {
               sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "DaysOn: " + this.rewardData[id].count);
            } else {
               sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Time: " + Output.getTimeBreakdown(this.rewardData[id].time, Output.TIMEDETAIL.SHORT));
               sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Scope: " + this.rewardData[id].scope.label() + "     Reference: " + this.rewardData[id].reference.label());
            }
         }

         if (this.rewardData[id].occurs == RewardData.Occurs.RECURRING || this.rewardData[id].occurs == RewardData.Occurs.PERPETUAL) {
            sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Interval: " + Output.getTimeBreakdown(this.rewardData[id].recurranceTime, Output.TIMEDETAIL.SHORT));
         }

         if (!this.rewardData[id].world.equalsIgnoreCase("all")) {
            if (this.rewardData[id].onWorld) {
               sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "World: +" + this.rewardData[id].world);
            } else {
               sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "World: " + this.rewardData[id].world);
            }
         }

         if (this.rewardData[id].reference == RewardData.EventReference.SHOP_POINTS) {
            sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Cost: " + this.rewardData[id].count + " " + Output.OnTimeOutput.getString("output.eventRef.point"));
         } else if (this.rewardData[id].reference == RewardData.EventReference.SHOP_ECON) {
            sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Cost: " + this.rewardData[id].count + " " + OnTime.economy.currencyNamePlural());
         } else if (this.rewardData[id].count > 0 && this.rewardData[id].occurs != RewardData.Occurs.DAYSON) {
            sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Count: " + this.rewardData[id].count);
         }

         sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Tag: " + this.rewardData[id].identifier);
         if (this.rewardData[id].exclusive.equalsIgnoreCase("E")) {
            sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Permission String: " + this.rewardData[id].permissionString);
            sb.append(preamble + "(#" + (id + 1) + ") " + "Group(s): ");
            boolean foundOne = false;
            if (this.groupList != null && this.groupList.length > 0) {
               String world = null;
               if (!this.isGlobal()) {
                  world = (String)_plugin.get_rewards().getEnabledWorlds().get(0);
               }

               for(int i = 0; i < this.groupList.length; ++i) {
                  if (OnTime.permission.groupHas(world, this.groupList[i], this.rewardData[id].permissionString)) {
                     sb.append(this.groupList[i] + " ");
                     foundOne = true;
                  }
               }
            }

            if (!foundOne) {
               sb.append("none");
            }

            sender.sendMessage(sb.toString());
            sb.setLength(0);
         }

         if (this.rewardData[id].link != null) {
            int linkID = this.getRewardID(this.rewardData[id].link);
            if (linkID >= 0) {
               sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Reward Chain Next RewardID =  " + (linkID + 1));
               this.displayInfo(sender, linkID, "LINK==>");
            } else {
               sender.sendMessage(preamble + "(#" + (id + 1) + ") " + "Reward Chain Invalid Link ID: " + this.rewardData[id].link);
            }
         }
      }

   }

   public int getRewardID(String Identifier) {
      for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
         if (this.rewardData[i].identifier.equalsIgnoreCase(Identifier)) {
            return i;
         }
      }

      return -1;
   }

   public kitClass[] getKit(String kitID) {
      kitClass[] returnKit = null;
      if (this.kits.size() == 0) {
         return null;
      } else {
         Iterator var4 = this.kits.iterator();

         while(true) {
            String[] tokens;
            kitClass[] kit;
            do {
               if (!var4.hasNext()) {
                  return returnKit;
               }

               String k = (String)var4.next();
               tokens = k.split("[,]");
               kit = null;
            } while(!tokens[0].equalsIgnoreCase(kitID));

            kit = new kitClass[Integer.parseInt(tokens[1])];

            for(int i = 0; i < Integer.parseInt(tokens[1]); ++i) {
               kit[i] = new kitClass();
               kit[i].quantity = Integer.parseInt(tokens[i * 2 + 2]);
               kit[i].item = tokens[i * 2 + 3];
            }

            returnKit = kit;
         }
      }
   }

   public boolean checkForReward(String playerName, String rewardID) {
      if (playerHasRewards(playerName)) {
         RewardInstance[] rewards = getPlayerRewards(playerName);

         for(int j = 0; j < rewards.length; ++j) {
            if (rewards[j].active && rewards[j].identifier.endsWith(rewardID)) {
               LogFile.console(0, playerName + " already has " + rewardID);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public Integer[] getKitElements(String KitID) {
      Integer[] elements = new Integer[100];
      int numElements = 0;

      for(int i = 0; i < this.getNumDefinedRewards(); ++i) {
         if (this.getRewardData()[i].identifier.contains("kit-" + KitID)) {
            ++numElements;
            elements[numElements] = i;
         }
      }

      elements[0] = numElements;
      return elements;
   }

   public boolean isGlobal() {
      if (OnTime.rewardsEnable) {
         if (_plugin.get_rewards().getEnabledWorlds() != null) {
            return _plugin.get_rewards().getEnabledWorlds().contains("global") || _plugin.get_rewards().getEnabledWorlds().contains("Global");
         } else {
            LogFile.write(10, "{isGlobal} rewardsEnable = true, but 'enabledWorlds' was NULL");
            return false;
         }
      } else {
         return false;
      }
   }

   private StringBuilder tab(StringBuilder sb, int end) {
      if (sb.length() > end) {
         sb.setLength(end);
      } else {
         sb.append("                              ", sb.length(), end);
      }

      return sb;
   }

   public int getNumDefinedRewards() {
      return definedRewards;
   }

   public void setNumDefinedRewards(int levelCount) {
      definedRewards = levelCount;
   }

   public RewardData[] getRewardData() {
      return this.rewardData;
   }

   public void setRewardData(RewardData[] data) {
      this.rewardData = data;
   }

   public List getCommands() {
      return this.commands;
   }

   public void setCommands(List commands) {
      this.commands = commands;
   }

   public void set_rewardUtilities(RewardUtilities _rewardUtilities) {
      Rewards._rewardUtilities = _rewardUtilities;
   }

   public static RewardUtilities get_rewardUtilities() {
      return _rewardUtilities;
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

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$EventReference() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$EventReference;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[RewardData.EventReference.values().length];

         try {
            var0[RewardData.EventReference.ABSENCE.ordinal()] = 7;
         } catch (NoSuchFieldError var12) {
         }

         try {
            var0[RewardData.EventReference.CHANGEWORLD.ordinal()] = 12;
         } catch (NoSuchFieldError var11) {
         }

         try {
            var0[RewardData.EventReference.DEATH.ordinal()] = 8;
         } catch (NoSuchFieldError var10) {
         }

         try {
            var0[RewardData.EventReference.DELTATIME.ordinal()] = 3;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[RewardData.EventReference.LOGIN.ordinal()] = 4;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[RewardData.EventReference.PLAYTIME.ordinal()] = 1;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[RewardData.EventReference.POINTS.ordinal()] = 11;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[RewardData.EventReference.REALTIME.ordinal()] = 2;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[RewardData.EventReference.REFER.ordinal()] = 6;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[RewardData.EventReference.SHOP_ECON.ordinal()] = 10;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[RewardData.EventReference.SHOP_POINTS.ordinal()] = 9;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[RewardData.EventReference.VOTES.ordinal()] = 5;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$Rewards$RewardData$EventReference = var0;
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

   public class argClass {
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

   public static enum indiScheduleSource {
      LOGIN,
      RESET,
      AFK,
      COMMAND,
      CHANGEWORLD;
   }

   public class issueReward implements Runnable {
      private String playerName;
      private RewardData reward;
      private int record;

      public issueReward(String _playerName, RewardData _reward, int _record) {
         this.playerName = _playerName;
         this.reward = _reward;
         this.record = _record;
      }

      public void run() {
         Rewards.this.issue(this.playerName, this.reward, this.record);
      }
   }

   public class kitClass {
      String item = "TEST";
      int quantity = 0;
   }

   public static enum listRequest {
      EVENT,
      TAG;
   }

   public static enum promoOrDemo {
      NO_CHANGE,
      PROMOTION,
      DEMOTION,
      ERROR;
   }

   public static enum rewardEligibility {
      ELIGIBLE,
      HIGHERGROUP,
      LOWERGROUP,
      EXCLUSIVE,
      HAS_PERM,
      NO_PERM,
      OTHERWORLD,
      OFFWORLD;
   }
}
