package me.edge209.OnTime.Rewards;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.util.Calendar;
import me.edge209.OnTime.DataIO;
import me.edge209.OnTime.LogFile;
import me.edge209.OnTime.OnTime;
import me.edge209.OnTime.PlayerData;
import me.edge209.OnTime.Players;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class VotifierEventListener implements Listener {
   private static OnTime _plugin;

   public VotifierEventListener(OnTime plugin) {
      _plugin = plugin;
   }

   public void incrementVoteServiceAsync(String voteService) {
      try {
         _plugin.getServer().getScheduler().runTaskAsynchronously(_plugin, new _incrementVoteService(voteService));
      } catch (NoSuchMethodError var3) {
         _plugin.getServer().getScheduler().scheduleAsyncDelayedTask(_plugin, new _incrementVoteService(voteService));
      }

   }

   @EventHandler(
      priority = EventPriority.NORMAL
   )
   public void onVotifierEvent(VotifierEvent event) {
      int voteTotalCount = 0;
      int voteDailyCount = 0;
      int voteWeeklyCount = 0;
      int voteMonthlyCount = 0;
      Vote vote = event.getVote();
      String playerName = vote.getUsername();
      if (playerName == null) {
         LogFile.console(3, ChatColor.RED + "Votifier vote received from " + vote.getServiceName() + " but no player name was specified.");
         LogFile.write(3, "Votifier vote received from " + vote.getServiceName() + " but no player name was specified.");
      } else if (!Players.hasOnTimeRecord(playerName)) {
         LogFile.console(3, ChatColor.RED + "Votifier vote received from " + vote.getServiceName() + " for " + playerName + ", but no OnTime record found for this player.");
         LogFile.write(3, "Votifier vote received from " + vote.getServiceName() + " for " + playerName + ", but no OnTime record found for this player.");
      } else {
         if (OnTime.dataStorage == DataIO.datastorage.MYSQL & OnTime.votifierStatsEnable) {
            LogFile.console(0, "Incremeting votifier stats for: " + playerName);
            _plugin.get_dataio().incrementPlayerVotesAsync(playerName);
            this.incrementVoteServiceAsync(vote.getServiceName());
         }

         PlayerData playerData = null;
         if ((playerData = Players.getData(playerName)) != null) {
            voteTotalCount = ++playerData.totalVotes;
            voteDailyCount = ++playerData.dailyVotes;
            voteWeeklyCount = ++playerData.weeklyVotes;
            voteMonthlyCount = ++playerData.monthlyVotes;
            playerData.lastVoteDate = Calendar.getInstance().getTimeInMillis();
            LogFile.console(0, "voteDailyCount:" + voteDailyCount + "  playerData.dailyVotes:" + playerData.dailyVotes);
         } else {
            LogFile.write(3, "{votifierEvent} No playerRecord for " + playerName + " found. Vote data not saved.");
         }

         RewardData reward = null;
         boolean oneFound = false;
         boolean rewardSet = false;

         for(int i = 0; i < _plugin.get_rewards().getNumDefinedRewards(); ++i) {
            reward = _plugin.get_rewards().getRewardData()[i];
            if (reward.occurs == RewardData.Occurs.VOTE_P || reward.occurs == RewardData.Occurs.VOTE_S) {
               oneFound = true;
               int voteCount = 0;
               if (reward.scope == RewardData.timeScope.TOTAL) {
                  voteCount = voteTotalCount;
               } else if (reward.scope == RewardData.timeScope.DAILY) {
                  voteCount = voteDailyCount;
               } else if (reward.scope == RewardData.timeScope.WEEKLY) {
                  voteCount = voteWeeklyCount;
               } else if (reward.scope == RewardData.timeScope.MONTHLY) {
                  voteCount = voteMonthlyCount;
               }

               LogFile.console(0, "voteCount:" + voteCount + " reward.scope:" + reward.scope + " reward.count: " + reward.count);
               LogFile.write(0, "voteCount:" + voteCount + " reward.scope:" + reward.scope + " reward.count: " + reward.count);
               if (reward.count == -1 || reward.occurs == RewardData.Occurs.VOTE_P && voteCount % reward.count == 0 || reward.occurs == RewardData.Occurs.VOTE_S && voteCount == reward.count) {
                  String[] data = new String[]{vote.getServiceName()};
                  if (_plugin.get_rewards().setReward(playerName, reward.reference, reward.time, i, reward, data) > -1) {
                     LogFile.write(3, "Votifier reward of " + _plugin.get_rewards().rewardString(reward) + " set for " + playerName);
                     rewardSet = true;
                  }
               }
            }
         }

         if (!oneFound) {
            LogFile.write(3, "No valid votifier reward found.  No reward scheduled for " + playerName);
         } else if (!rewardSet) {
            LogFile.write(1, "Votifier reward found but not issued to " + playerName);
         }

      }
   }

   public void incrementVoteService(String voteService) {
      if (!_plugin.get_dataio().updateMySQLField(OnTime.MySQL_table, "lastVote", Calendar.getInstance().getTimeInMillis(), voteService)) {
         PlayerData playerdata = Players.getNew(voteService, Calendar.getInstance().getTimeInMillis(), "votifier-service");
         Players.putData(voteService.toLowerCase(), playerdata);
         _plugin.get_dataio().savePlayerDataMySQL(voteService, false);
      }

      _plugin.get_dataio().incrementMySQLField(OnTime.MySQL_table, "votes", voteService);
   }

   public class _incrementVoteService implements Runnable {
      private String voteService;

      public _incrementVoteService(String _voteService) {
         this.voteService = _voteService;
      }

      public void run() {
         VotifierEventListener.this.incrementVoteService(this.voteService);
      }
   }
}
