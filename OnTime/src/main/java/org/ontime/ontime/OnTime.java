package org.ontime.ontime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import me.edge209.OnTime.DataIO;
import me.edge209.OnTime.Players;
import me.edge209.OnTime.Rewards.Rewards;
import me.edge209.OnTime.Rewards.VotifierEventListener;
//import net.milkbowl.vault.economy.Economy;
//import net.milkbowl.vault.permission.Permission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class OnTime extends JavaPlugin {
   static final int configYMLversion = 21;
   static final int indirewardsYMLversion = 1;
   static final int messagesYMLversion = 3;
   static final int outputYMLversion = 20;
   static final int playerdataYMLversion = 6;
   public static final int rewardsYMLversion = 13;
   public static final int mySQLtableVersion = 3;
   public static final Logger logger = Logger.getLogger("Minecraft");
   public static File onTimeDataFolder;
   public static File onTimeReportsFolder;
   File configFile;
   FileConfiguration config;
   File rewardFile;
   FileConfiguration rewards;
   String datetime;
   public static String pluginName;
   File updateFile;
   static int rewardTaskID = -1;
   private static Rewards _rewards;
   private static me.edge209.OnTime.Commands _commands;
   private static me.edge209.OnTime.TodayTime _todaytime;
   private static me.edge209.OnTime.LoginTime _logintime;
   private static me.edge209.OnTime.OnTimeTest _ontimetest;
   private static me.edge209.OnTime.PlayingTime _playingtime;
   private static me.edge209.OnTime.Report _report;
   private static me.edge209.OnTime.PlayerEventListener _playereventlistener;
   private static me.edge209.OnTime.EntityEventListener _entityeventlistener;
   private static VotifierEventListener _votifiereventListener;
   private static me.edge209.OnTime.Output _output;
   private static me.edge209.OnTime.DataBackup _databackup;
   private static AwayFK _awayfk;
   private static me.edge209.OnTime.Import _import;
   private static me.edge209.OnTime.DataIO _dataio;
   private static me.edge209.OnTime.Messages _messages;
   private static me.edge209.OnTime.PermissionsHandler _permissionsHandler;
   private static me.edge209.OnTime.Points _points;
   private static Players _players;
   public static Permission permission = null;
   public static Economy economy = null;
   public static Boolean AfkTerminator = false;
   public static Boolean OnTimeLimits = false;
   public static Boolean OnSign = false;
   public static String serverName = "<Your Server Name Here>";
   public static boolean multiServer = false;
   public static String multiServerName = "myServers";
   public static String mulitServerNameERROR = null;
   public static boolean primaryServer = true;
   static int startupDelay = 1;
   public static int topListMax = 10;
   public static boolean updateCheckEnable = true;
   public static boolean messagesEnable = true;
   public static boolean welcomeEnable = true;
   static boolean afkCheckEnable = true;
   static int afkTime = 10;
   public static me.edge209.OnTime.DataIO.datastorage dataStorage;
   static boolean MySQL_enable;
   static String MySQL_host;
   static int MySQL_port;
   static String MySQL_database;
   public static String MySQL_table;
   public static String MySQL_multiServerTable;
   static String MySQL_user;
   static String MySQL_password;
   static boolean autoSaveEnable;
   static long autoSavePeriod;
   static boolean autoBackupEnable;
   static int autoBackupVersions;
   static boolean uuidMergeEnable;
   public static boolean perWorldEnable;
   public static String serverID;
   public static boolean collectPlayDetailEnable;
   public static boolean collectVoteDetailEnable;
   public static boolean collectReferDetailEnable;
   static boolean collectAfkEnable;
   static boolean purgeEnable;
   static int purgeTimeMin;
   static long purgeLoginDay;
   static boolean purgeDemotionEnable;
   static String purgeDemotionGroup;
   public static boolean onlineTrackingEnable;
   public static int onlineTrackingRefresh;
   static boolean logEnable;
   static int logLevel;
   static int consoleLogLevel;
   static int firstDayofWeek;
   static int firstDayofMonth;
   static boolean autoReportEnable;
   static boolean dailyPlayReportEnable;
   static boolean weeklyPlayReportEnable;
   static boolean monthlyPlayReportEnable;
   static String reportFolder;
   static String dateFilenameFormat;
   public static boolean votifierStatsEnable;
   static boolean dateInFilenameEnable;
   static String reportFormat;
   static String afkReportPeriod;
   static int dailyReportRetention;
   static int weeklyReportRetention;
   static int monthlyReportRetention;
   static int afkReportRetention;
   public static boolean referredByEnable;
   public static boolean referredByPermTrackEnable;
   public static long referredByMaxTime;
   public static boolean rewardsEnable;
   public static boolean rewardsEnableConfig;
   public static boolean rewardNotifyEnable;
   public static boolean rewardBroadcastEnable;
   public static String totalTopPlayReward;
   public static String totalTopVoteReward;
   public static String totalTopReferReward;
   public static String totalTopPointReward;
   public static boolean pointsEnable;
   public static boolean negativePointsEnable;
   public static boolean votifierEnable;
   public static boolean enableOnTime;
   public static boolean suspendOnTime;
   public static String pluginVersion;
   public static long todayStart;
   public static long weekStart;
   public static long monthStart;
   public static int afkTaskID;
   public static int newDayTaskID;
   public static int auditLogoutTaskID;
   public static int autoSaveTaskID;
   public static BukkitTask autoSaveTask;
   public static BukkitTask onlineReportTask;
   public static BukkitTask checkVersionTask;
   public static int playerLoginDelay;

   static {
      dataStorage = me.edge209.OnTime.DataIO.datastorage.YML;
      MySQL_enable = false;
      MySQL_host = "localhost";
      MySQL_port = 3306;
      MySQL_database = "minecraft";
      MySQL_table = "`ontime-players`";
      MySQL_multiServerTable = "`ontime-multiServer`";
      MySQL_user = "root";
      MySQL_password = "password";
      autoSaveEnable = true;
      autoSavePeriod = 60L;
      autoBackupEnable = true;
      autoBackupVersions = 3;
      uuidMergeEnable = false;
      perWorldEnable = false;
      serverID = "server";
      collectPlayDetailEnable = true;
      collectVoteDetailEnable = false;
      collectReferDetailEnable = false;
      collectAfkEnable = false;
      purgeEnable = true;
      purgeTimeMin = 10;
      purgeLoginDay = 60L;
      purgeDemotionEnable = false;
      purgeDemotionGroup = "default";
      onlineTrackingEnable = false;
      onlineTrackingRefresh = 5;
      logEnable = false;
      logLevel = 1;
      consoleLogLevel = 3;
      firstDayofWeek = 2;
      firstDayofMonth = 1;
      autoReportEnable = true;
      dailyPlayReportEnable = true;
      weeklyPlayReportEnable = true;
      monthlyPlayReportEnable = true;
      reportFolder = "/";
      dateFilenameFormat = "yyyy.MM.dd";
      votifierStatsEnable = true;
      dateInFilenameEnable = true;
      reportFormat = "TXT";
      afkReportPeriod = "Week";
      dailyReportRetention = -1;
      weeklyReportRetention = -1;
      monthlyReportRetention = -1;
      afkReportRetention = -1;
      referredByEnable = true;
      referredByPermTrackEnable = true;
      referredByMaxTime = -1L;
      rewardsEnable = true;
      rewardsEnableConfig = true;
      rewardNotifyEnable = true;
      rewardBroadcastEnable = true;
      totalTopPlayReward = "weekly";
      totalTopVoteReward = "disable";
      totalTopReferReward = "disable";
      totalTopPointReward = "disable";
      pointsEnable = false;
      negativePointsEnable = false;
      votifierEnable = false;
      enableOnTime = true;
      suspendOnTime = true;
      pluginVersion = null;
      todayStart = 0L;
      weekStart = 0L;
      monthStart = 0L;
      afkTaskID = 0;
      newDayTaskID = 0;
      auditLogoutTaskID = 0;
      autoSaveTaskID = 0;
      autoSaveTask = null;
      onlineReportTask = null;
      checkVersionTask = null;
      playerLoginDelay = 1;
   }

   private me.edge209.OnTime.LogFile LogFile;

   public void onDisable() {
      PluginDescriptionFile pdfFile = this.getDescription();
      if (enableOnTime) {
         this.get_awayfk().forceAllFromAFK();
         this.get_rewards().saveIndiRewards(onTimeDataFolder);
         Iterator var3 = this.getServer().getOnlinePlayers().iterator();

         while(var3.hasNext()) {
            Player player = (Player)var3.next();
            if (this.get_permissionsHandler().playerHas(player, "ontime.track")) {
               this.get_playereventlistener().logoutPlayer(player);
            } else {
               me.edge209.OnTime.LogFile.write(1, player.getName() + " did not require logout. OnTime tracking is not enabled for this player.");
            }
         }

         _ontimetest.logoutAll();
         if (enableOnTime) {
            this.get_dataio().saveAllData(onTimeDataFolder);
            this.get_dataio().clearAllMaps();
         }

         if (MySQL_enable) {
            if (onlineTrackingEnable) {
               this.get_dataio().dropTable("ontime-online");
            }

            this.get_dataio();
            me.edge209.OnTime.DataIO.mysqlNew.close();
         }
      }

      this.getServer().getScheduler().cancelTasks(this);
      logger.info("[" + pdfFile.getName() + "] version: " + pdfFile.getVersion() + " DISABLED. ");
   }

   public void onEnable() {
      PluginDescriptionFile pdfFile = this.getDescription();
      pluginName = "[" + pdfFile.getName() + "]";
      this.set_rewards(new Rewards(this));
      this.set_commands(new Commands(this));
      this.set_todaytime(new me.edge209.OnTime.TodayTime(this));
      this.set_logintime(new me.edge209.OnTime.LoginTime(this));
      this.set_ontimetest(new OnTimeTest(this));
      this.set_playingtime(new PlayingTime(this));
      this.set_report(new Report(this));
      this.set_playereventlistener(new PlayerEventListener(this));
      this.set_entityeventlistener(new EntityEventListener(this));
      this.set_votifiereventlistener(new VotifierEventListener(this));
      this.set_output(new Output(this));
      this.set_databackup(new DataBackup(this));
      this.set_awayfk(new AwayFK(this));
      this.set_import(new Import(this));
      this.set_dataio(new DataIO(this));
      this.set_messages(new Messages(this));
      set_permissionsHandler(new PermissionsHandler(this));
      this.set_points(new Points(this));
      this.set_players(new Players(this));
      this.createDataDirectory(this.getDataFolder());
      onTimeDataFolder = this.getDataFolder();
      this.updateFile = new File(onTimeDataFolder, "updates.txt");
      if (this.updateFile.exists()) {
         this.updateFile.delete();
      }

      this.updateFile.getParentFile().mkdirs();
      this.copy(this.getResource("updates.txt"), this.updateFile);
      LogFile.initialize(onTimeDataFolder, "OnTimeLog.txt", "OnTime");
      if (!this.initConfig(onTimeDataFolder)) {
         this.disableOnTimeOnError("Plugin configuration failed.  All OnTime functions disabled.");
      } else {
         onTimeReportsFolder = new File(onTimeDataFolder + File.separator + reportFolder);
         this.createDataDirectory(onTimeReportsFolder);
         pluginVersion = "OnTime v" + pdfFile.getVersion();
         LogFile.write(3, "******************************************");
         LogFile.write(3, "Enable Initiated: " + pluginVersion);
         LogFile.write(3, "******************************************");
         if (updateCheckEnable) {
            this.checkVersion(pluginVersion);
         }

         File infile = new File(onTimeDataFolder, "help.txt");
         if (!infile.exists()) {
            infile.getParentFile().mkdirs();
            this.copy(this.getResource("help.txt"), infile);
            LogFile.console(1, "Created file 'help.txt'");
         }

         if (!Output.initOutput(onTimeDataFolder)) {
            this.disableOnTimeOnError("FILE CORRUPTION ERROR: output.yml file was not read sucessfully.");
         } else if (dataStorage == DataIO.datastorage.MYSQL && !MySQL_enable) {
            this.disableOnTimeOnError("CONFIGURATION ERROR: In config.yml 'dataStorage = MYSQL' but MySQL database is not enabled.");
         } else {
            this.setupMySQL();
            if (!this.get_dataio().loadAllData(DataIO.mysqlload.INIT)) {
               this.disableOnTimeOnError("Player data load error.");
            } else {
               if (autoSaveEnable) {
                  try {
                     autoSaveTask = this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
                        public void run() {
                           OnTime.this.get_dataio().saveAllData(OnTime.onTimeDataFolder);
                           OnTime.this.get_rewards().saveIndiRewards(OnTime.onTimeDataFolder);
                           LogFile.console(2, "Data auto-save successful.");
                        }
                     }, autoSavePeriod * 1200L, autoSavePeriod * 1200L);
                  } catch (NoSuchMethodError var5) {
                     autoSaveTaskID = this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
                        public void run() {
                           OnTime.this.get_dataio().saveAllData(OnTime.onTimeDataFolder);
                           OnTime.this.get_rewards().saveIndiRewards(OnTime.onTimeDataFolder);
                           me.edge209.OnTime.LogFile.console(2, "Data auto-save successful.");
                        }
                     }, autoSavePeriod * 1200L, autoSavePeriod * 1200L);
                  }

                  me.edge209.OnTime.LogFile.console(1, "Data file auto-saves schdeduled.");
               }

               if (afkCheckEnable) {
                  afkTaskID = this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                     public void run() {
                        OnTime.this.checkAFK();
                     }
                  }, TimeUnit.MINUTES.toMillis((long)afkTime) / 50L);
                  me.edge209.OnTime.LogFile.console(1, "AFK Checking Enabled.");
               }

               long firstcheck = TimeUnit.SECONDS.toMillis((long)(3610 - Calendar.getInstance().get(12) * 60 - Calendar.getInstance().get(13)));
               newDayTaskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                  public void run() {
                     me.edge209.OnTime.TodayTime.checkNewDay();
                     me.edge209.OnTime.LogFile.console(0, "Checking if New Day");
                  }
               }, firstcheck / 50L, 72000L);
               LogFile.console(0, "First Scheduled Check of new day in " + TimeUnit.MILLISECONDS.toMinutes(firstcheck) + " Minutes");
               this.scheduleAuditLogout();
               if (onlineTrackingEnable && onlineTrackingRefresh > 0) {
                  onlineReportTask = this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
                     public void run() {
                        OnTime.this.updateAllOnlineReport("ontime-online");
                     }
                  }, TimeUnit.MINUTES.toMillis((long)onlineTrackingRefresh) / 50L, TimeUnit.MINUTES.toMillis((long)onlineTrackingRefresh) / 50L);
               }

               this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                  public void run() {
                     OnTime.this.enablePluginDependencies();
                  }
               }, (long)((startupDelay + 1) * 20));
            }
         }
      }
   }

   private void disableOnTimeOnError(String reason) {
      LogFile.console(3, reason);
      enableOnTime = false;
      this.setEnabled(false);
      MySQL_enable = false;
   }

   public void checkAFK() {
      this.get_awayfk().checkAFK();
   }

   public void scheduleAuditLogout() {
      this.cancelAuditLogout();
      auditLogoutTaskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
         public void run() {
            OnTime.this.auditLogout();
         }
      }, 6000L, 6000L);
      LogFile.write(1, "{scheduledAuditLogout} Logout audit scheduled.");
   }

   public void cancelAuditLogout() {
      if (auditLogoutTaskID != 0) {
         this.getServer().getScheduler().cancelTask(auditLogoutTaskID);
         auditLogoutTaskID = 0;
         me.edge209.OnTime.LogFile.write(1, "{cancelAuditLogout} Logout audit cancelled.");
      }

   }

   public void auditLogout() {
      this.get_playereventlistener().auditLogout();
   }

   public void updateAllOnlineReport(String tableName) {
      this.get_dataio().updateAllOnlineReport(tableName);
   }

   public void setupMySQL() {
      if (MySQL_enable) {
         LogFile.console(1, "MySQL Enabled.  Attempting connection to database: " + MySQL_database);

         try {
            Class.forName("com.mysql.jdbc.Driver");
         } catch (Exception var3) {
            var3.printStackTrace();
            this.disableOnTimeOnError("Could not fetch JDBC Drivers.");
            return;
         }

         this.get_dataio().setupMySqlVar();

         try {
            this.get_dataio();
            if (!DataIO.mysqlNew.open()) {
               this.disableOnTimeOnError("Could not reach MySQL Database.");
               return;
            }

            this.get_dataio().updateMySQL();
            LogFile.console(1, "MySQL connection to database: " + MySQL_database + " SUCCESSFUL");
         } catch (NoSuchMethodError var2) {
            this.get_dataio();
            DataIO.mysqlNew.open();
         }
      }

   }

   public void enablePluginDependencies() {
      if (this.setupEconomy()) {
         LogFile.console(1, "Economy plugin (" + economy.getName() + ") latched.");
      }

      if (this.setupPermissions()) {
         LogFile.console(1, "Permissions plugin (" + permission.getName() + ") latched.");
      }

      if (this.getServer().getPluginManager().getPlugin("Votifier") != null) {
         this.getServer().getPluginManager().registerEvents(_votifiereventListener, this);
         votifierEnable = true;
      }

      if (this.getServer().getPluginManager().getPlugin("afkTerminator") != null) {
         LogFile.console(1, "AfkTeminator Plugin Located");
         AfkTerminator = true;
      }

      if (this.getServer().getPluginManager().getPlugin("OnTimeLimits") != null) {
         LogFile.console(1, "OnTimeLimits Plugin Located");
         OnTimeLimits = true;
      }

      if (this.getServer().getPluginManager().getPlugin("OnSign") != null) {
         LogFile.console(1, "OnSign Plugin Located");
         OnSign = true;
      }

      if (messagesEnable) {
         Messages.initMessages(onTimeDataFolder);
      }

      if (rewardsEnable || messagesEnable) {
         _rewards.initRewards(onTimeDataFolder);
      }

      if (!permission.isEnabled() && referredByEnable && referredByPermTrackEnable) {
         referredByPermTrackEnable = false;
         if (dataStorage == DataIO.datastorage.YML) {
            referredByEnable = false;
            LogFile.console(3, "No permisssion plugin enabled, so 'referred by' function has been disabled.");
         } else {
            LogFile.console(3, "No permisssion plugin enabled, so permission string tracking of 'referred by' status disabled.");
         }
      }

      this.updateFile.delete();
      this.get_report().setReportNames();
      TodayTime.checkNewDay();
      get_commands().resumeOnTime();
      if (rewardsEnable) {
         _rewards.initIndiRewards(onTimeDataFolder);
      }

      this.getServer().getPluginManager().registerEvents(_playereventlistener, this);
      this.getServer().getPluginManager().registerEvents(_entityeventlistener, this);
      playerLoginDelay = 0;
   }

   public boolean initConfig(File onTimeDataFolder) {
      this.configFile = new File(onTimeDataFolder, "config.yml");
      if (!this.configFile.exists()) {
         this.configFile.getParentFile().mkdirs();
         this.copy(this.getResource("config.yml"), this.configFile);
      }

      this.config = new YamlConfiguration();

      try {
         this.config.load(this.configFile);
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      logEnable = this.config.getBoolean("logEnable");
      logLevel = this.config.getInt("logLevel");
      consoleLogLevel = this.config.getInt("consoleLogLevel");
      int version = this.config.getInt("configVersion");
      if (!this.readConfigItems(version)) {
         return false;
      } else if (version < 18) {
         LogFile.console(3, "Your version of OnTime/config.yml is out of date, and too old to be auto updated.");
         LogFile.console(3, "Please download and run OnTime v3.13.1, which will update your config.yml to ");
         LogFile.console(3, "a version that can be converted to the new format.  Then reload and install");
         LogFile.console(3, "OnTime v4.0.0 or greater.  OR delete yoru config.yml and let OnTime generate ");
         LogFile.console(3, "a clean one, but you must then reconfigure by hand your settings.");
         return false;
      } else {
         if (Updates.checkConfigUpgrade(this, this.config)) {
            LogFile.console(1, "/plugin/ontime/config.yml version auto upgradged to latest version.");

            try {
               this.config.load(this.configFile);
            } catch (Exception var4) {
               var4.printStackTrace();
            }

            version = this.config.getInt("configVersion");
            if (!this.readConfigItems(version)) {
               return false;
            }
         }

         LogFile.console(1, "/plugin/ontime/config.yml loaded.");
         return true;
      }
   }

   private boolean readConfigItems(int version) {
      serverName = this.config.getString("serverName");
      multiServer = this.config.getBoolean("multiServer");
      multiServerName = this.config.getString("multiServerName");
      if (version < 21) {
         mulitServerNameERROR = this.config.getString("mulitServerName");
      }

      primaryServer = this.config.getBoolean("primaryServer");
      startupDelay = this.config.getInt("startupDelay");
      updateCheckEnable = this.config.getBoolean("updateCheckEnable");
      playerLoginDelay = startupDelay + 1;
      messagesEnable = this.config.getBoolean("messagesEnable");
      welcomeEnable = this.config.getBoolean("welcomeEnable");
      if (!messagesEnable && welcomeEnable) {
         LogFile.console(3, "Invalid configuration.  'messagesEnable' must be 'true' if 'welcomeEnable' is 'true'");
         LogFile.console(3, "'messagesEnable' set to 'true' by OnTime.  Please fix your config.yml appropriately.");
         messagesEnable = true;
      }

      afkCheckEnable = this.config.getBoolean("afkCheckEnable");
      afkTime = this.config.getInt("afkTime");
      String storage = this.config.getString("dataStorage");
      dataStorage = DataIO.datastorage.YML;
      if (storage.equalsIgnoreCase("MYSQL")) {
         dataStorage = DataIO.datastorage.MYSQL;
      }

      MySQL_enable = this.config.getBoolean("MySQL.enable");
      if (dataStorage == DataIO.datastorage.MYSQL && !MySQL_enable) {
         LogFile.console(3, "Configuration Error: dataStorage = MYSQL, but MYSQL is not enabled.");
         return false;
      } else {
         MySQL_host = this.config.getString("MySQL.host");
         MySQL_port = this.config.getInt("MySQL.port");
         MySQL_database = this.config.getString("MySQL.database");
         MySQL_table = "`" + this.config.getString("MySQL.table") + "`";
         MySQL_multiServerTable = "`" + this.config.getString("MySQL.multiServerTable") + "`";
         MySQL_user = this.config.getString("MySQL.user");
         MySQL_password = this.config.getString("MySQL.password");
         autoSaveEnable = this.config.getBoolean("autoSaveEnable");
         autoSavePeriod = this.config.getLong("autoSavePeriod");
         autoBackupEnable = this.config.getBoolean("autoBackupEnable");
         autoBackupVersions = this.config.getInt("autoBackupVersions");
         if (dataStorage == DataIO.datastorage.MYSQL) {
            uuidMergeEnable = this.config.getBoolean("uuidMergeEnable");
         } else {
            uuidMergeEnable = false;
         }

         perWorldEnable = this.config.getBoolean("perWorldEnable");
         if (perWorldEnable && dataStorage != DataIO.datastorage.MYSQL) {
            perWorldEnable = false;
            LogFile.console(3, "Per-world tracking requires MYSQL storage.  perWorldEnable has now been set to 'false'");
         }

         votifierStatsEnable = this.config.getBoolean("votifierStatsEnable");
         collectPlayDetailEnable = this.config.getBoolean("collectPlayDetailEnable");
         collectVoteDetailEnable = this.config.getBoolean("collectVoteDetailEnable");
         collectReferDetailEnable = this.config.getBoolean("collectReferDetailEnable");
         collectAfkEnable = this.config.getBoolean("collectAfkEnable");
         purgeEnable = this.config.getBoolean("purgeEnable");
         purgeTimeMin = this.config.getInt("purgeTimeMin");
         purgeLoginDay = this.config.getLong("purgeLoginDay");
         purgeDemotionEnable = this.config.getBoolean("purgeDemotionEnable");
         purgeDemotionGroup = this.config.getString("purgeDemotionGroup");
         onlineTrackingEnable = this.config.getBoolean("onlineTrackingEnable");
         onlineTrackingRefresh = this.config.getInt("onlineTrackingRefresh");
         logEnable = this.config.getBoolean("logEnable");
         logLevel = this.config.getInt("logLevel");
         consoleLogLevel = this.config.getInt("consoleLogLevel");
         firstDayofWeek = this.config.getInt("firstDayofWeek");
         firstDayofMonth = this.config.getInt("firstDayofMonth");
         autoReportEnable = this.config.getBoolean("autoReportEnable");
         dailyPlayReportEnable = this.config.getBoolean("dailyPlayReportEnable");
         weeklyPlayReportEnable = this.config.getBoolean("weeklyPlayReportEnable");
         monthlyPlayReportEnable = this.config.getBoolean("monthlyPlayReportEnable");
         dateInFilenameEnable = this.config.getBoolean("dateInFilenameEnable");
         dateFilenameFormat = this.config.getString("dateFilenameFormat");
         reportFolder = this.config.getString("reportFolder");
         afkReportPeriod = this.config.getString("afkReportPeriod");
         dailyReportRetention = this.config.getInt("dailyReportRetention");
         weeklyReportRetention = this.config.getInt("weeklyReportRetention");
         monthlyReportRetention = this.config.getInt("monthlyReportRetention");
         afkReportRetention = this.config.getInt("afkReportRetention");
         reportFormat = this.config.getString("reportFormat");
         if (reportFormat.equalsIgnoreCase("MYSQL") && !MySQL_enable) {
            LogFile.console(3, "Invalid 'reportFormat' configuration. MySQL is not enabled.  Setting reportFormat to 'TXT'");
            reportFormat = "TXT";
         }

         referredByEnable = this.config.getBoolean("referredByEnable");
         referredByPermTrackEnable = this.config.getBoolean("referredByPermTrackEnable");
         referredByMaxTime = this.config.getLong("referredByMaxTime");
         if (referredByEnable && dataStorage == DataIO.datastorage.YML && !referredByPermTrackEnable) {
            LogFile.console(3, "Invalid 'referredBy' configuration for referredByPermTrackEnable ");
            LogFile.console(3, "referredByPermTrackEnable now set to TRUE");
            referredByPermTrackEnable = true;
         }

         rewardsEnableConfig = this.config.getBoolean("rewardsEnable");
         rewardsEnable = rewardsEnableConfig;
         rewardNotifyEnable = this.config.getBoolean("rewardNotifyEnable");
         rewardBroadcastEnable = this.config.getBoolean("rewardBroadcastEnable");
         totalTopPlayReward = this.config.getString("totalTopPlayReward");
         totalTopVoteReward = this.config.getString("totalTopVoteReward");
         totalTopReferReward = this.config.getString("totalTopReferReward");
         totalTopPointReward = this.config.getString("totalTopPointReward");
         pointsEnable = this.config.getBoolean("pointsEnable");
         if (pointsEnable) {
            if (dataStorage == DataIO.datastorage.MYSQL) {
               negativePointsEnable = this.config.getBoolean("negativePointsEnable");
               LogFile.console(1, "Points system enabled.");
            } else {
               LogFile.console(3, "'Points' requires dataStorage=MYSQL. pointsEnable set to 'false'");
               pointsEnable = false;
            }
         }

         if (version >= 19) {
            topListMax = this.config.getInt("topListMax");
         }

         return true;
      }
   }

   public void copy(InputStream in, File file) {
      if (in == null) {
         LogFile.console(3, "{Ontime.Copy} Invalid 'yml' source specified for " + file.getName());
         LogFile.write(10, "{Ontime.Copy} Invalid 'yml' source specified for " + file.getName());
      } else {
         try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];

            int len;
            while((len = in.read(buf)) > 0) {
               out.write(buf, 0, len);
            }

            out.close();
            in.close();
         } catch (Exception var6) {
            var6.printStackTrace();
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

   private boolean createDataDirectory(File file) {
      if (!file.isDirectory()) {
         LogFile.console(1, "Creating directory: " + file.toString());
         if (!file.mkdirs()) {
            LogFile.console(3, "ERROR when trying to create directory: " + file.toString());
            return false;
         }
      }

      return true;
   }

   private Boolean setupPermissions() {
      RegisteredServiceProvider permissionProvider = this.getServer().getServicesManager().getRegistration(Permission.class);
      if (permissionProvider != null) {
         permission = (Permission)permissionProvider.getProvider();
      }

      return permission != null ? true : false;
   }

   private Boolean setupEconomy() {
      RegisteredServiceProvider economyProvider = this.getServer().getServicesManager().getRegistration(Economy.class);
      if (economyProvider != null) {
         economy = (Economy)economyProvider.getProvider();
      }

      return economy != null ? true : false;
   }

   public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
      return _commands.onCommand(sender, cmd, commandLabel, args);
   }

   public void checkVersion(final String s) {
      try {
         checkVersionTask = this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            public void run() {
               Updates.UpdateVersion(36891, s);
            }
         });
      } catch (NoSuchMethodError var3) {
         this.getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
            public void run() {
               Updates.UpdateVersion(36891, s);
            }
         });
      }

   }

   public static String getPlayerName(Player player) {
      return player.getName();
   }

   public Rewards get_rewards() {
      return _rewards;
   }

   public void set_rewards(Rewards _rewards) {
      OnTime._rewards = _rewards;
   }

   public static Commands get_commands() {
      return _commands;
   }

   public void set_commands(Commands _commands) {
      OnTime._commands = _commands;
   }

   public TodayTime get_todaytime() {
      return _todaytime;
   }

   public void set_todaytime(TodayTime _todaytime) {
      OnTime._todaytime = _todaytime;
   }

   public LoginTime get_logintime() {
      return _logintime;
   }

   public void set_logintime(LoginTime _logintime) {
      OnTime._logintime = _logintime;
   }

   public OnTimeTest get_ontimetest() {
      return _ontimetest;
   }

   public void set_ontimetest(OnTimeTest _ontimetest) {
      OnTime._ontimetest = _ontimetest;
   }

   public PlayingTime get_playingtime() {
      return _playingtime;
   }

   public void set_playingtime(PlayingTime _playingtime) {
      OnTime._playingtime = _playingtime;
   }

   public Report get_report() {
      return _report;
   }

   public void set_report(Report _report) {
      OnTime._report = _report;
   }

   public PlayerEventListener get_playereventlistener() {
      return _playereventlistener;
   }

   public void set_playereventlistener(PlayerEventListener _playereventlistner) {
      _playereventlistener = _playereventlistner;
   }

   public EntityEventListener get_entityeventlistener() {
      return _entityeventlistener;
   }

   public void set_entityeventlistener(EntityEventListener _entityeventlistner) {
      _entityeventlistener = _entityeventlistner;
   }

   public VotifierEventListener get_votifiereventlistener() {
      return _votifiereventListener;
   }

   public void set_votifiereventlistener(VotifierEventListener _votifiereventlistner) {
      _votifiereventListener = _votifiereventlistner;
   }

   public Output get_output() {
      return _output;
   }

   public void set_output(Output _output) {
      OnTime._output = _output;
   }

   public DataBackup get_databackup() {
      return _databackup;
   }

   public void set_databackup(DataBackup _databackup) {
      OnTime._databackup = _databackup;
   }

   public AwayFK get_awayfk() {
      return _awayfk;
   }

   public void set_awayfk(AwayFK _awayfk) {
      OnTime._awayfk = _awayfk;
   }

   public me.edge209.OnTime.Import get_import() {
      return _import;
   }

   public void set_import(me.edge209.OnTime.Import _import) {
      OnTime._import = _import;
   }

   public DataIO get_dataio() {
      return _dataio;
   }

   public void set_dataio(DataIO _dataio) {
      OnTime._dataio = _dataio;
   }

   public me.edge209.OnTime.Messages get_messages() {
      return _messages;
   }

   public void set_messages(me.edge209.OnTime.Messages _messages) {
      OnTime._messages = _messages;
   }

   public me.edge209.OnTime.PermissionsHandler get_permissionsHandler() {
      return _permissionsHandler;
   }

   public static void set_permissionsHandler(me.edge209.OnTime.PermissionsHandler _permissionsHandler) {
      OnTime._permissionsHandler = _permissionsHandler;
   }

   public me.edge209.OnTime.Points get_points() {
      return _points;
   }

   public void set_points(me.edge209.OnTime.Points _points) {
      OnTime._points = _points;
   }

   public Players get_players() {
      return _players;
   }

   public void set_players(me.edge209.OnTime.Players _players) {
      OnTime._players = _players;
   }
}
