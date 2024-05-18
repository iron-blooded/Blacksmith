package org.ontime.ontime;

import java.util.Iterator;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PermissionsHandler {
   private static OnTime _plugin;
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$me$edge209$OnTime$PermissionsHandler$ACTION;

   public PermissionsHandler(OnTime plugin) {
      _plugin = plugin;
   }

   public boolean addOrRemove(ACTION action, Player player, String group_or_permission) {
      return this._addOrRemove(action, player, (String)null, group_or_permission);
   }

   public boolean addOrRemove(ACTION action, String targetGroup, String permission) {
      return this._addOrRemove(action, (Player)null, targetGroup, permission);
   }

   private boolean _addOrRemove(ACTION action, Player player, String targetGroup, String group_or_permission) {
      if (_plugin.get_rewards().isGlobal()) {
         if (!this.action(action, (String)null, player, targetGroup, group_or_permission)) {
            LogFile.write(1, "Global " + action.label() + " of " + group_or_permission + " failed for " + targetGroup);
            return false;
         } else {
            return true;
         }
      } else {
         Iterator it = _plugin.get_rewards().getEnabledWorlds().iterator();
         boolean success = false;

         while(it.hasNext()) {
            String world = (String)it.next();
            if (this.action(action, world, player, targetGroup, group_or_permission)) {
               success = true;
            }
         }

         return success;
      }
   }

   public boolean action(ACTION action, String world, Player player, String targetGroup, String group_or_perm) {
      LogFile.write(0, "{action} :" + action.toString() + " " + world + " " + targetGroup + " " + group_or_perm);
      switch (action) {
         case GAP:
            return OnTime.permission.groupAdd(world, targetGroup, group_or_perm);
         case GRP:
            return OnTime.permission.groupRemove(world, targetGroup, group_or_perm);
         case PAG:
            return OnTime.permission.playerAddGroup(world, player, group_or_perm);
         case PAP:
            return OnTime.permission.playerAdd(world, player, group_or_perm);
         case PRG:
            return OnTime.permission.playerRemoveGroup(world, player, group_or_perm);
         case PRP:
            return OnTime.permission.playerRemove(world, player, group_or_perm);
         default:
            return false;
      }
   }

   public Boolean playerHas(Player player, String permission) {
      if (player == null) {
         LogFile.write(11, "{playerHas} 'player' passed was NULL when checking for " + permission);
         return false;
      } else {
         UUID uuid = player.getUniqueId();
         if (uuid != null) {
            OfflinePlayer offlinePlayer = Players.getOfflinePlayer(uuid);
            if (offlinePlayer != null && offlinePlayer.getName() != null) {
               if (OnTime.permission.playerHas((String)null, offlinePlayer, permission)) {
                  return true;
               } else {
                  return OnTime.permission.playerHas(offlinePlayer.getPlayer(), permission) ? true : false;
               }
            } else {
               LogFile.write(0, "{playerHas} offlinePlayer/name was null for " + uuid.toString());
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public Boolean playerHas(String playerName, String permission) {
      OfflinePlayer offlinePlayer = Players.getOfflinePlayer(playerName);
      if (offlinePlayer != null && offlinePlayer.getName() != null) {
         if (OnTime.permission.playerHas((String)null, offlinePlayer, permission)) {
            return true;
         } else {
            return OnTime.permission.playerHas(offlinePlayer.getPlayer(), permission) ? true : false;
         }
      } else {
         LogFile.write(0, "{playerHas} offlinePlayer was null for " + playerName);
         return false;
      }
   }

   public Boolean playerInGroup(String playerName, String group) {
      OfflinePlayer offlinePlayer = Players.getOfflinePlayer(playerName);
      if (_plugin.get_rewards().isGlobal()) {
         return OnTime.permission.playerInGroup((String)null, offlinePlayer, group) ? true : false;
      } else {
         return OnTime.permission.playerInGroup(offlinePlayer.getPlayer().getWorld().getName(), offlinePlayer.getPlayer(), group) ? true : false;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$me$edge209$OnTime$PermissionsHandler$ACTION() {
      int[] var10000 = $SWITCH_TABLE$me$edge209$OnTime$PermissionsHandler$ACTION;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[PermissionsHandler.ACTION.values().length];

         try {
            var0[PermissionsHandler.ACTION.GAP.ordinal()] = 1;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[PermissionsHandler.ACTION.GRP.ordinal()] = 2;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[PermissionsHandler.ACTION.PAG.ordinal()] = 3;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[PermissionsHandler.ACTION.PAP.ordinal()] = 4;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[PermissionsHandler.ACTION.PRG.ordinal()] = 5;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[PermissionsHandler.ACTION.PRP.ordinal()] = 6;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$me$edge209$OnTime$PermissionsHandler$ACTION = var0;
         return var0;
      }
   }

   public static enum ACTION {
      GAP("Group Add Permission"),
      GRP("Group Remove Permission"),
      PAG("Player Add Group"),
      PAP("Player Add Permission"),
      PRG("Player Remove Group"),
      PRP("Player Remove Permission");

      private final String label;

      private ACTION(String label) {
         this.label = label;
      }

      public String label() {
         return this.label;
      }
   }
}
