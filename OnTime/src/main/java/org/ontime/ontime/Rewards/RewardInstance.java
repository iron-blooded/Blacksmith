package me.edge209.OnTime.Rewards;

public class RewardInstance {
   public boolean active = false;
   public boolean scheduleNext = true;
   public int index = 0;
   public RewardForm form;
   public int scheduleID;
   public Integer rewardID;
   public RewardData.EventReference reference;
   public Long time;
   public String identifier;
   public int count;
   public String[] data = null;

   public static enum RewardForm {
      STANDARD,
      PERSONAL,
      MESSAGE;
   }
}
