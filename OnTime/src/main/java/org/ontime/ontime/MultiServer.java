package org.ontime.ontime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import me.edge209.mysqlib.NewMySQL;

public class MultiServer {
   public static boolean loadPlayerDataMySQL(UUID uuid) {
      NewMySQL mysqlNew = DataIO.mysqlNew;
      PlayerData playerData = null;
      String playerName = null;

      try {
         if (!mysqlNew.checkMySQLConnection()) {
            return false;
         }

         ResultSet rs = mysqlNew.query("SELECT * FROM " + OnTime.MySQL_multiServerTable + " WHERE uuid='" + uuid + "'");
         rs.beforeFirst();

         while(rs.next()) {
            long totalTime = 0L;
            long todaytime = 0L;
            long weektime = 0L;
            long monthtime = 0L;
            long lastLogin = 0L;
            totalTime = rs.getLong("playtime");
            lastLogin = rs.getLong("logintime");
            todaytime = rs.getLong("todaytime");
            weektime = rs.getLong("weektime");
            monthtime = rs.getLong("monthtime");
            playerName = rs.getString("playerName");
            if ((playerData = Players.getData(uuid)) != null) {
               Players.setWorldTime(playerData, OnTime.multiServerName, totalTime, todaytime, weektime, monthtime, lastLogin);
               LogFile.write(0, OnTime.multiServerName + " MySQL loaded multiServer data for: " + playerName);
               return true;
            }
         }
      } catch (SQLException var15) {
         LogFile.write(10, "{MultiSever.loadPlayerDataMySQL} MYSQL Error:" + var15.getMessage());
      }

      return false;
   }
}
