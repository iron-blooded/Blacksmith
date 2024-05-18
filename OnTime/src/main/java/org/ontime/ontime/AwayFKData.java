package org.ontime.ontime;

public class AwayFKData {
   boolean currentlyAFK;
   long totalAFKTime;
   long AFKStartTime;
   long lastRewardTime;
   long firstActionTime;
   long lastActionTime;
   long todayAFKTime;
   long weekAFKTime;
   long monthAFKTime;

   public AwayFKData(long _today, long _week, long _month) {
      this.todayAFKTime = _today;
      this.weekAFKTime = _week;
      this.monthAFKTime = _month;
      this.currentlyAFK = false;
      this.totalAFKTime = 0L;
      this.AFKStartTime = 0L;
      this.lastRewardTime = 0L;
      this.firstActionTime = 0L;
   }
}
