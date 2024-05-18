package org.blacksmith.blacksmith;

import org.blacksmith.blacksmith.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.meta.ItemMeta;
import java.sql.SQLException;
import java.util.*;
public final class Blacksmith extends JavaPlugin implements Listener {
    public FileConfiguration config = null;

    public SimpleSqlDatabase simpleSqlDatabase = new SimpleSqlDatabase(this, "blacksmith");
    @Override
    public void onEnable() {
        // Plugin startup logic
        this.saveDefaultConfig();
        config = getConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        getCommand("reload").setExecutor(new reload(this));
        getCommand("blacksmith_edit_config").setTabCompleter(new editConfigCompletion(this));
        getCommand("blacksmith_edit_config").setExecutor(new editConfig(this));

        getCommand("combine_enchantments").setTabCompleter(new combineEnchantmentsCompletion(this));
        getCommand("combine_enchantments").setExecutor(new combineEnchantments(this));

        getCommand("apply_enchantments").setTabCompleter(new applyingEnchantmentCompletion(this));
        getCommand("apply_enchantments").setExecutor(new applyingEnchantment(this));

        getCommand("split_enchantments").setTabCompleter(new applyingEnchantmentCompletion(this));
        getCommand("split_enchantments").setExecutor(new splitEnchantments(this));

        getCommand("get_count").setExecutor(new getCount(this));

        getCommand("set_count").setExecutor(new setCount(this));

        getCommand("rename_item").setExecutor(new renameItem(this));
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            simpleSqlDatabase.conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    public double rnd(){
        return new Random().nextDouble()*100.0F;
    }

    public boolean checkItemForEnchant(ItemStack item_in_off_hand, ItemStack item_in_main_hand, Player player){
        if (item_in_off_hand.getAmount() > 1 || item_in_main_hand.getAmount() > 1) {
            player.sendMessage("Нормальное количество возьми");
            return true;
        }
        List<String> li = new ArrayList<>();
        try {
            li.addAll(item_in_main_hand.getItemMeta().getLore());
        } catch (Exception e) {
        }
        try {
            li.addAll(item_in_off_hand.getItemMeta().getLore());
        } catch (Exception e) {
        }
        for (String l : li) {
            if (l.contains("§c") || l.contains("§b")) {
                player.sendMessage("Со своими кастомными зачарами иди к админам");
                if (player.hasPermission("blacksmithcolorbypass")){
                    player.sendMessage("Вы обходите ограничения цвета!");
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }
    public static int extractNumberFromString(String str) {
        int num = 0;
        int multiplier = 1;
        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                num += (c - '0') * multiplier;
                multiplier *= 10;
            } else {
                break;
            }
        }
        return num;
    }


    public double getMaxBlacksmithLVL(Player player, Enchantment enchantment, int level){
        String perm = "";
        for (String key : this.config.getConfigurationSection("enchantments_limits.permissions").getKeys(false)){
            if (player.hasPermission(key)){
                perm = key;
            }
        }
        if (perm == ""){
            player.sendMessage("Ты не кузнец");
            return 0;
        }
        double min_perm_level = 0;
        if (this.config.contains("enchantments_limits.min_perm_level."+enchantment.getKey().getKey())){
            min_perm_level = this.config.getDouble("enchantments_limits.min_perm_level."+enchantment.getKey().getKey());
        }
        else {
            min_perm_level = this.config.getDouble("enchantments_limits.min_perm_level.default");
        }
        if (this.config.getDouble("enchantments_limits.permissions."+perm) < min_perm_level){
            player.sendMessage("Ваших знаний не хватает.");
            return 0;
        }
        double result = 0;
        for (String key : this.config.getConfigurationSection("enchantments_limits.original").getKeys(false)){
            if (this.config.contains("enchantments_limits.original."+enchantment.getKey().getKey())){
                result = this.config.getDouble("enchantments_limits.original."+enchantment.getKey().getKey()) +
                        this.config.getDouble("enchantments_limits.permissions."+perm);
            }
            if (this.config.contains("enchantments_limits.cap_enchantments."+enchantment.getKey().getKey())){
                if (level >= this.config.getDouble("enchantments_limits.cap_enchantments."+enchantment.getKey().getKey())){
                    return -999999;
                }
            }
            else {
                if (level >= this.config.getDouble("enchantments_limits.cap_enchantments.default")){
                    return -999999;
                }
            }
        }
        if (result == 0) {
            result = this.config.getDouble("enchantments_limits.original.default");
        }
        result += this.config.getDouble("enchantments_limits.permissions."+perm);
        return result;
    }
    public double getChanceEnchant(Player player, Enchantment enchantment, int level){
        double reduce = config.getDouble("reduce");
        String perm = "";
        for (String key : this.config.getConfigurationSection("enchantments_limits").getKeys(false)){
            if (player.hasPermission(key)){
                perm = key;
            }
        }
        if (perm == ""){
            player.sendMessage("Ты не кузнец");
            return -1;
        }
        for (int i = 10; i >= 0; i--){
            String k = enchantment.getKey().getKey()+"_"+i;
            if (player.hasPermission(k)){
                level -= extractNumberFromString(k);
            }
        }
        String key = "enchantments_limits."+ perm + ".";
        double max_enchants_level = 0;
        if (config.contains(key+"max_enchants_level."+enchantment.getKey().getKey())){
            max_enchants_level = config.getDouble(key+"max_enchants_level."+enchantment.getKey().getKey());
        }
        else if (config.contains(key+"max_enchants_level.default")) {
            max_enchants_level = config.getDouble(key+"max_enchants_level.default");
        }
        double chance = 0;
        if (config.contains(key+"chance."+enchantment.getKey().getKey())){
            chance = config.getDouble(key+"chance."+enchantment.getKey().getKey());
        }
        else if (config.contains(key+"chance.default")) {
            chance = config.getDouble(key+"chance.default");
        }
        if (level > max_enchants_level){
            player.sendMessage("У вас не хватает навыков");
            return -1;
        }
        else if(level == max_enchants_level){
            return -2;
        }
        else {
            return chance+(max_enchants_level-level)*reduce;
        }
    }
    public int hasAnvilNearby(Player player, int radius, Material material) {
        Location playerLocation = player.getLocation();
        int num = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = playerLocation.clone().add(x, y, z).getBlock();
                    if (block.getType() == material) {
                        num += 1;
                    }
                }
            }
        }
        if (num > 3){
            num = 3;
        }
        return num;
    }
    public boolean hasItemWithName(Player player, String name) {
        Inventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    public boolean removeItemByName(Player player, String itemName) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equalsIgnoreCase(itemName)) {
                ItemStack itemStack = inventory.getItem(i);
                itemStack.setAmount(itemStack.getAmount()-1);
                return true;
            }
        }
        return false;
    }
    private double item_add_chance(Player player){
        double final_chances = 0;
        if (config.contains("item_add_chance")){
            for (String key : this.config.getConfigurationSection("item_add_chance").getKeys(false)){
                String patch = "item_add_chance."+key;
                String name = config.getString(patch+".name");
                if (hasItemWithName(player, name)){
                    double i = config.getDouble(patch+".add_chance");
                    if (rnd() <= config.getDouble(patch+".chance_break")) {
                        removeItemByName(player, name);
                    }
                    return i;
//                    final_chances += i;
                }
            }
        }
        return final_chances;
    }


    private double addChance(Player player){
        double final_chances = 0;
        final_chances = item_add_chance(player);
        if (hasAnvilNearby(player, 10, Material.ANVIL) < 1
                && hasAnvilNearby(player, 10, Material.CHIPPED_ANVIL) < 1
                && hasAnvilNearby(player, 10, Material.DAMAGED_ANVIL) < 1){
            player.sendMessage("Наковальни рядом нет, шансы уменьшены на 75%");
            final_chances -= 75;
        }
        int smithing_table = hasAnvilNearby(player, 10, Material.SMITHING_TABLE);
        final_chances += 3.5 * smithing_table;
        return final_chances;
    }

    public int checkBlacksmithLVL(Player player, Enchantment enchantment, int level){

//        double lvl_blacksmith = getMaxBlacksmithLVL(player, enchantment, level);
//        double reduce = this.config.getDouble("reduce");
//        double limit = this.config.getDouble("limit");
//        double final_chances = limit+(lvl_blacksmith - level)*reduce;
        double final_chances = getChanceEnchant(player, enchantment, level);
        if (final_chances < 0){
            return -1; // Шансы малы
        }
        final_chances += addChance(player);
        System.out.println(final_chances);
        if (final_chances >= 100){
            return 2;
        }
        else if (rnd() <= final_chances){
            return 1; // Успешно
        }
        else {
            return 0; // Проебался
        }
    }

    public void transferEnchantment(Enchantment enchantment, ItemStack item1, ItemStack item2, int addLevel) {
        Map<Enchantment, Integer> enchantments_in_item1 = getEnchantments(item1);
        int newLevel = enchantments_in_item1.get(enchantment) + addLevel;
        if (item1.getType() == Material.ENCHANTED_BOOK){
            EnchantmentStorageMeta meta1 = (EnchantmentStorageMeta) item1.getItemMeta();
            meta1.removeStoredEnchant(enchantment);
            item1.setItemMeta(meta1);
        }
        if (item2.getType() == Material.ENCHANTED_BOOK){
            EnchantmentStorageMeta meta2 = (EnchantmentStorageMeta) item2.getItemMeta();
            meta2.removeStoredEnchant(enchantment);
            item2.setItemMeta(meta2);
        }
        try {
            PersistentDataContainer container = item1.getItemMeta().getPersistentDataContainer();
            for (NamespacedKey k : container.getKeys()) {
                if (k.getKey().startsWith("PublicBukkitValues:")) {
                    container.remove(k);
                }
            }
            item1.setItemMeta(item1.getItemMeta());
        }
        catch (Exception e){}
        try {
            PersistentDataContainer container = item2.getItemMeta().getPersistentDataContainer();
            for (NamespacedKey k : container.getKeys()) {
                if (k.getKey().startsWith("PublicBukkitValues:")) {
                    container.remove(k);
                }
            }
            item2.setItemMeta(item2.getItemMeta());
        }
        catch (Exception e){}
        item1.removeEnchantment(enchantment);
        item2.removeEnchantment(enchantment);
        Map<Enchantment, Integer> m = new HashMap<>();
        if (newLevel > 0) {
            m.put(enchantment, newLevel);
        }
        item2.addUnsafeEnchantments(m);
        setNameEnch(item1, enchantment);
        setNameEnch(item2, enchantment);
    }
    private void setNameEnch(ItemStack item, Enchantment enchantment){
        Map<Enchantment, Integer> enchantments = getEnchantments(item);
        if (enchantments.size() == 0
                && !item.getItemMeta().getDisplayName().split(" ")[0].equals(enchantment.getName())){
            return;
        }
        String name = "";
        for (Enchantment ench : enchantments.keySet()) {
             name = ench.getName() + " " + enchantments.get(ench);
        }
        if (item.getItemMeta().getDisplayName().contains(name.split(" ")[0])){
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

    }
    private void resetNameEnch(ItemStack item, Enchantment enchantment, int level){
        if (item.getItemMeta().getDisplayName().equals(enchantment.getName() + " " + level)){
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("");
            item.setItemMeta(meta);
        }
    }
    public static Map<Enchantment, Integer> getMatchingEnchants(ItemStack item1, ItemStack item2){
        Map<Enchantment, Integer> matchingEnchants = new HashMap<>();
        Map<Enchantment, Integer> enchantments_in_item1 = getEnchantments(item1);
        Map<Enchantment, Integer> enchantments_in_item2 = getEnchantments(item2);
        for (Enchantment ench : enchantments_in_item1.keySet()) {
            if (enchantments_in_item2.containsKey(ench) && enchantments_in_item2.get(ench) == enchantments_in_item1.get(ench)) {
                matchingEnchants.put(ench, enchantments_in_item1.get(ench));
            }
        }
        return matchingEnchants;
    }

    public static Map<Enchantment, Integer> getEnchantments(ItemStack item) {
        Map<Enchantment, Integer> map = new HashMap<>();
        if (item.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            if (meta != null) {
                map.putAll(meta.getStoredEnchants());
            }
            map.putAll(item.getEnchantments());
        } else {
            return new HashMap<>(item.getEnchantments());
        }
        return map;
    }


}
