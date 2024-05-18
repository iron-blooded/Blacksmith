package me.edge209.OnTime.Rewards;

import me.edge209.OnTime.LogFile;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;

public class RewardData implements Comparable {
   public Occurs occurs;
   public String exclusive;
   public timeScope scope;
   public EventReference reference;
   public String link;
   public String message;
   public String world;
   public boolean onWorld;
   public long time;
   public long recurranceTime;
   public int count;
   public RewardType type;
   private int quantity;
   public String reward;
   public String identifier;
   public String permissionString;
   public ItemStack itemstack;

   public RewardData(Occurs occurs, String exclusive, timeScope scope, EventReference reference, String link, String message, String world, long time, long recurranceTime, int count, RewardType type, int quantity, String reward, String identifier) {
      this.occurs = occurs;
      this.exclusive = exclusive;
      this.scope = scope;
      this.reference = reference;
      this.link = link;
      this.world = world;
      this.onWorld = false;
      if (world != null && world.startsWith("+")) {
         this.world = world.substring(1);
         this.onWorld = true;
      }

      this.time = time;
      this.recurranceTime = recurranceTime;
      this.message = message;
      this.count = count;
      this.type = type;
      this.setQuantity(quantity);
      this.reward = reward;
      this.identifier = identifier;
      this.permissionString = "ontime.reward." + identifier;
      String[] tokens = reward.split("[+:]");
      LogFile.console(0, "Creating new reward " + reward);
      if (this.type == RewardData.RewardType.ITEM) {
         Material material = Material.matchMaterial(tokens[0]);
         if (tokens.length > 1) {
            if (reward.contains("+")) {
               this.itemstack = new ItemStack(material, quantity);

               for(int i = 1; i < tokens.length; ++i) {
                  Enchantment enchantment = new EnchantmentWrapper(Integer.parseInt(tokens[i]));
                  if (enchantment.canEnchantItem(this.itemstack)) {
                     this.itemstack.addEnchantment(enchantment, enchantment.getStartLevel());
                  } else {
                     LogFile.write(3, "ERROR {RewardData} Invalid item (" + material.name() + ") and enchantment (" + enchantment.getName());
                  }
               }
            } else {
               this.itemstack = new ItemStack(material, quantity, (short)Integer.parseInt(tokens[1]));
            }
         } else {
            this.itemstack = new ItemStack(material, quantity);
         }
      } else {
         this.itemstack = null;
      }

   }

   public int compareTo(RewardData argv) {
      if (this.time < argv.time) {
         return -1;
      } else {
         return this.time > argv.time ? 1 : 0;
      }
   }

   public int getQuantity() {
      return this.quantity;
   }

   public void setQuantity(int quantity) {
      this.quantity = quantity;
   }

   public static enum EventReference {
      PLAYTIME("P", "Play"),
      REALTIME("R", "Real"),
      DELTATIME("D", "Delta"),
      LOGIN("L", "Login"),
      VOTES("V", "Vote"),
      REFER("F", "Refer"),
      ABSENCE("A", "Absence"),
      DEATH("D", "Death"),
      SHOP_POINTS("SP", "Shop-Points"),
      SHOP_ECON("SE", "Shop-Econ"),
      POINTS("PT", "Point"),
      CHANGEWORLD("CW", "ChangeWorld");

      private final String code;
      private final String label;

      private EventReference(String code, String label) {
         this.code = code;
         this.label = label;
      }

      public String code() {
         return this.code;
      }

      public String label() {
         return this.label;
      }
   }

   public static enum Occurs {
      CHAIN("C", "Chain"),
      DAYSON("DO", "DaysOn"),
      DELAY("D", "Delay"),
      SINGLE("S", "Single"),
      RECURRING("R", "Recurring"),
      INDIVIDUAL("I", "Individual"),
      REFERSOURCE("FS", "Referred by"),
      REFERTARGET("FT", "Referral target"),
      KITELEMENT("KE", "Kit Element"),
      PERPETUAL("P", "Perpetual"),
      TOP("T", "Top"),
      VOTE_S("VS", "Votes Single"),
      VOTE_P("VP", "Votes Perpetual"),
      PURGE("G", "Purge");

      private final String code;
      private final String label;

      private Occurs(String code, String label) {
         this.code = code;
         this.label = label;
      }

      public String code() {
         return this.code;
      }

      public String label() {
         return this.label;
      }
   }

   public static enum RewardType {
      ADDGROUP("+G"),
      COMMAND("C"),
      DELAY("D"),
      DEMOTION("-R"),
      DENIAL("-P"),
      ECONOMY("E"),
      ITEM("I"),
      KIT("K"),
      MESSAGE("M"),
      PERMISSION("+P"),
      POINTS("LP"),
      PROMOTION("+R"),
      REMOVEGROUP("-G"),
      XP("X");

      private final String code;

      private RewardType(String code) {
         this.code = code;
      }

      public String code() {
         return this.code;
      }
   }

   public static enum timeScope {
      TOTAL("T", "Total"),
      DAILY("D", "Daily"),
      WEEKLY("W", "Weekly"),
      MONTHLY("M", "Monthly");

      private final String code;
      private final String label;

      private timeScope(String code, String label) {
         this.code = code;
         this.label = label;
      }

      public String code() {
         return this.code;
      }

      public String label() {
         return this.label;
      }
   }
}
