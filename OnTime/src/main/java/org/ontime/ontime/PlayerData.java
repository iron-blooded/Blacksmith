package org.ontime.ontime;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;

public class PlayerData {
   public UUID uuid;
   public String playerName;
   public int mysqlID;
   public boolean onLine;
   public boolean loginPending;
   public String hostName;
   public long firstLogin;
   public String lastWorld;
   public long lastVoteDate;
   public int totalVotes;
   public int dailyVotes;
   public int weeklyVotes;
   public int monthlyVotes;
   public int totalReferrals;
   public int dailyReferrals;
   public int weeklyReferrals;
   public int monthlyReferrals;
   public String referredBy;
   public int daysOn;
   public int permissions;
   public int points;
   public me.edge209.OnTime.AwayFKData afkData;
   public HashMap worldTime;

   public PlayerData(UUID uuid, String playerName, String hostName, long firstLogin, long lastLogin, String lastWorld, long lastVoteDate, int totalVotes, int dailyVotes, int weeklyVotes, int monthlyVotes, int totalReferrals, int dailyReferrals, int weeklyReferrals, int monthlyReferrals, String referredBy, int permissions, int points, int daysOn, long totalTime, long todayTime, long weekTime, long monthTime, long afkToday, long afkWeek, long afkMonth) {
      this.uuid = uuid;
      this.playerName = playerName;
      this.onLine = false;
      this.loginPending = false;
      this.hostName = hostName;
      this.firstLogin = firstLogin;
      this.lastWorld = lastWorld;
      this.lastVoteDate = lastVoteDate;
      this.totalVotes = totalVotes;
      this.dailyVotes = dailyVotes;
      this.weeklyVotes = weeklyVotes;
      this.monthlyVotes = monthlyVotes;
      this.totalReferrals = totalReferrals;
      this.dailyReferrals = dailyReferrals;
      this.weeklyReferrals = weeklyReferrals;
      this.monthlyReferrals = monthlyReferrals;
      this.referredBy = referredBy;
      this.permissions = permissions;
      this.points = points;
      this.daysOn = daysOn;
      this.afkData = new me.edge209.OnTime.AwayFKData(afkToday, afkWeek, afkMonth);
      this.worldTime = new HashMap();
      Players.putWorldTime(this, OnTime.serverID, new PlayTimeData(totalTime, todayTime, weekTime, monthTime, lastLogin));
      if (OnTime.multiServer) {
         Players.putWorldTime(this, OnTime.multiServerName, new PlayTimeData(totalTime, todayTime, weekTime, monthTime, lastLogin));
      }

      if (OnTime.perWorldEnable) {
         Player player = null;
         if ((player = Players.getOnlinePlayer(uuid)) != null) {
            Players.putWorldTime(this, player.getWorld().getName(), new PlayTimeData(totalTime, todayTime, weekTime, monthTime, lastLogin));
         }
      }

   }

   public static enum OTPerms {
      TOPTEN(1),
      PURGE(2);

      private final int mask;

      private OTPerms(int mask) {
         this.mask = mask;
      }

      public int mask() {
         return this.mask;
      }
   }
}
