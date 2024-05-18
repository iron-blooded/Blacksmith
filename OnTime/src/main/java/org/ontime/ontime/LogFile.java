package org.ontime.ontime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

public class LogFile {
   public static final Logger logger = Logger.getLogger("Minecraft");
   private static File file;
   private static File errorFile;
   private static String pluginName;

   public static void initialize(File dataFolder, String fileName, String plugin) {
      pluginName = plugin.toUpperCase();
      errorFile = new File(dataFolder, plugin + "_ERROR.txt");
      file = new File(dataFolder + "/" + fileName);
      if (openCreateLog(file)) {
         console(1, " Verifed that " + file.getPath() + " exists.");
      } else {
         console(1, " Logfile created: " + dataFolder + "/" + fileName);
      }

   }

   public static Boolean openCreateLog(File logfile) {
      if (!logfile.exists()) {
         try {
            logfile.createNewFile();
         } catch (IOException var2) {
            var2.printStackTrace();
         }

         return false;
      } else {
         return true;
      }
   }

   public static void write(int level, String string) {
      String datetime;
      BufferedWriter out;
      if (level >= 10) {
         openCreateLog(errorFile);

         try {
            datetime = (new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss] ")).format(Calendar.getInstance().getTime());
            out = new BufferedWriter(new FileWriter(errorFile, true));
            out.write(OnTime.pluginVersion + " " + datetime + string);
            out.newLine();
            if (level == 11) {
               Throwable t = new Throwable();
               StringWriter sw = new StringWriter();
               t.printStackTrace(new PrintWriter(sw));
               out.write(sw.toString());
               out.newLine();
            }

            out.close();
         } catch (IOException var7) {
            var7.printStackTrace();
         }
      }

      if (OnTime.logEnable) {
         if (level >= OnTime.logLevel) {
            try {
               datetime = (new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss] ")).format(Calendar.getInstance().getTime());
               out = new BufferedWriter(new FileWriter(file, true));
               out.write(datetime + string);
               out.newLine();
               out.close();
            } catch (IOException var6) {
               var6.printStackTrace();
            }

         }
      }
   }

   public static void console(int level, String string) {
      if (level >= OnTime.consoleLogLevel) {
         logger.info("[" + pluginName + "] " + string);
      }
   }
}
