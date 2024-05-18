package org.ontime.ontime;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class DataBackup {
   private static OnTime _plugin;

   public DataBackup(OnTime plugin) {
      _plugin = plugin;
   }

   public boolean backup(File sourceFolder) {
      if (!createBackupFolder(sourceFolder)) {
         LogFile.console(3, "ERROR Failed to create backup folder.");
         return false;
      } else {
         File folder = new File(sourceFolder, "backup");
         if (OnTime.dataStorage == DataIO.datastorage.YML) {
            this.renameFiles(folder, "playerdata.yml");
            if (!_plugin.get_dataio().saveAllData(folder)) {
               return false;
            }
         } else {
            this.renameFiles(folder, "ontime-player-delta.yml");
            _plugin.get_dataio().savePlayerDataYML(folder, "ontime-player-delta.yml");
         }

         LogFile.console(1, "Data backup successful.");
         return true;
      }
   }

   public boolean renameFiles(File folder, String fileName) {
      boolean success = false;
      File[] files = new File[OnTime.autoBackupVersions + 1];
      files[0] = new File(folder, fileName);

      int j;
      for(j = 1; j <= OnTime.autoBackupVersions; ++j) {
         files[j] = new File(folder, fileName + "._" + (j + 1));
      }

      for(j = OnTime.autoBackupVersions - 2; j >= 0; --j) {
         LogFile.console(0, "checking " + files[j].getPath());
         if (files[j].exists()) {
            if (files[j + 1].exists()) {
               files[j + 1].delete();
            }

            if (files[j].renameTo(files[j + 1])) {
               success = true;
            }

            LogFile.console(0, "Renaming " + files[j].getPath() + " to " + files[j + 1].getPath());
         }
      }

      if (!success) {
         LogFile.console(1, "No backup made.  AutoBackupVersions: " + OnTime.autoBackupVersions);
      }

      return success;
   }

   public long getLastBackup() {
      File file = new File(OnTime.onTimeDataFolder + "/backup/playerdata.yml");
      return file.exists() ? TimeUnit.DAYS.toMillis(TimeUnit.MILLISECONDS.toDays(file.lastModified())) : 0L;
   }

   public static boolean createBackupFolder(File pluginFolder) {
      File folder = new File(pluginFolder, "backup");
      if (!folder.isDirectory()) {
         if (!folder.mkdirs()) {
            return false;
         }

         LogFile.console(1, "plugin/backup folder created.");
      }

      return true;
   }
}
