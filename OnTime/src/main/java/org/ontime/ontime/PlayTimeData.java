package org.ontime.ontime;

public class PlayTimeData {
   public long totalTime;
   public long todayTime;
   public long weekTime;
   public long monthTime;
   public long lastLogin;
   public long rollOver;

   public PlayTimeData(long _totalTime, long _todayTime, long _weekTime, long _monthTime, long _lastLogin) {
      this.totalTime = _totalTime;
      this.todayTime = _todayTime;
      this.weekTime = _weekTime;
      this.monthTime = _monthTime;
      this.lastLogin = _lastLogin;
      this.rollOver = 0L;
   }
}
