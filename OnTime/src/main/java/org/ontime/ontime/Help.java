package org.ontime.ontime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.bukkit.command.CommandSender;

public class Help {
   public Help(OnTime plugin) {
   }

   public static boolean outputHelp(CommandSender sender, String search) {
      boolean foundSomething = false;
      String permissionNode = null;
      File infile = new File(OnTime.onTimeDataFolder, "help.txt");

      try {
         InputStream in = new FileInputStream(infile);
         BufferedReader inStream = new BufferedReader(new InputStreamReader(in));
         String line = inStream.readLine();

         for(boolean permission = true; line != null; line = inStream.readLine()) {
            if (line.contains("START_" + search)) {
               if (line.contains(":")) {
                  LogFile.console(0, "Checking Permission: " + line.substring(line.indexOf(":") + 1));
                  if (!OnTime.permission.has(sender, line.substring(line.indexOf(":") + 1))) {
                     permission = false;
                  }
               }

               for(line = inStream.readLine(); line != null; line = inStream.readLine()) {
                  if (line.contains("END_" + search)) {
                     permission = true;
                     break;
                  }

                  if (permission && !line.contains("START_") && !line.contains("END_") && !line.startsWith("#")) {
                     sender.sendMessage(line);
                     foundSomething = true;
                  }
               }
            } else if (line.contains("START_") && line.contains(":")) {
               if (!OnTime.permission.has(sender, line.substring(line.indexOf(":") + 1))) {
                  permissionNode = line.substring(line.indexOf("_") + 1, line.indexOf(":"));
                  permission = false;
               }
            } else if (!permission && line.contains("END_") && line.contains(permissionNode)) {
               permission = true;
            } else if (line.toLowerCase().contains(search.toLowerCase()) && permission && !line.contains("START_") && !line.contains("END_") && !line.startsWith("#")) {
               sender.sendMessage(line);
               foundSomething = true;
            }
         }

         inStream.close();
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      return foundSomething;
   }
}
