package org.ontime.ontime;

public class OnTimeAPI {
   public static boolean playerHasOnTimeRecord(String playerName) {
      return OnTime.enableOnTime ? PlayingTime.playerHasOnTimeRecord(playerName) : false;
   }

   public static String[] matchPlayerNames(String nameRoot) {
      return OnTime.enableOnTime ? DataIO.matchPlayerNames(nameRoot) : null;
   }

   public static long getPlayerTimeData(String playerName, data data) {
      return OnTime.enableOnTime ? DataIO.getPlayerTimeData(playerName, data) : -1L;
   }

   public static topData[] getTopData(data data) {
      return OnTime.enableOnTime ? DataIO.getTopData(data) : null;
   }

   public static enum data {
      TOTALPLAY,
      TODAYPLAY,
      WEEKPLAY,
      MONTHPLAY,
      LASTLOGIN,
      TOTALVOTE,
      TODAYVOTE,
      WEEKVOTE,
      MONTHVOTE,
      LASTVOTE,
      TOTALREFER,
      TODAYREFER,
      WEEKREFER,
      MONTHREFER,
      TOTALPOINT;
   }

   public static class topData {
      String playerName = null;
      long value = 0L;

      public String getPlayerName() {
         return this.playerName;
      }

      public void setPlayerName(String playerName) {
         this.playerName = playerName;
      }

      public long getValue() {
         return this.value;
      }

      public void setValue(long value) {
         this.value = value;
      }
   }
}
