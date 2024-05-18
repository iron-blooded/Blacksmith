package org.fishingspots.fishingspots;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.*;
import java.util.*;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.configuration.file.FileConfiguration;

import static org.apache.commons.lang.StringUtils.*;

public final class FishingSpots extends JavaPlugin implements Listener {
    private FileConfiguration config = null;
    private List<FishItem> fishItems = new ArrayList<>();


    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = getConfig();
        getConfigItems();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("reload")).setExecutor((commandSender, command, s, strings) -> {
            this.saveDefaultConfig();
            reloadConfig();
            config = getConfig();
            try {
                getConfigItems();
                commandSender.sendMessage("Конфиги обновлены");
            }
            catch (Exception e){
                commandSender.sendMessage("Конфиги не валидные");
            }
            return true;
        });
        getCommand("add_item").setTabCompleter(new AddItemTabCompletion());
        getCommand("add_item").setExecutor(new AddItemCommand(this));
    }

    @Override
    public void onDisable() {
    }
    private static int rnd(){
        return new Random().nextInt(100);
    }


    private void getConfigItems(){
        fishItems =  new ArrayList<>();
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            double chance = config.getDouble("items." + key + ".chance");
            String name = config.getString("items." + key + ".name").replace("&", "§");
            Material material = Material.valueOf(upperCase(config.getString("items." + key + ".material")));
            List<String> lore = config.getStringList("items." + key + ".lore");
            ArrayList<String> l = new ArrayList<String>();
            for (String lor: lore){
                l.add(lor.replace("&", "§"));
            }
            lore = l;
            ItemStack item = new ItemStack(material);
            ItemMeta itemMeta = item.getItemMeta();
                itemMeta.setLore(lore);
                itemMeta.setDisplayName(name);
            if (material == Material.POTION) {
                PotionMeta meta = (PotionMeta) itemMeta;
                meta.setColor(Color.fromRGB(config.getInt("items." + key + ".color")));
                try {
                    meta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(upperCase(config.getString("items." + key + ".effect"))),
                            config.getInt("items." + key + ".duration"),
                            config.getInt("items." + key + ".amplifier")), true);
                }
                catch (Exception e){}
                itemMeta = meta;
            }
            item.setItemMeta(itemMeta);
            ArrayList<Biome> whitelist_biomes = new ArrayList<>();
            ArrayList<Biome> blacklist_biomes = new ArrayList<>();
            if (!config.getStringList("items." + key + ".whitelist_biomes").isEmpty()){
                List<String> str_wh_biomes =  config.getStringList("items." + key + ".whitelist_biomes");
                for (String biom: str_wh_biomes){
                    whitelist_biomes.add(Biome.valueOf(upperCase(biom)));
                }
            } else if (!config.getStringList("items." + key + ".blacklist_biomes").isEmpty()) {
                List<String> str_bl_biomes =  config.getStringList("items." + key + ".blacklist_biomes");
                for (String biom: str_bl_biomes){
                    blacklist_biomes.add(Biome.valueOf(upperCase(biom)));
                }
            }
            fishItems.add(new FishItem(item, chance, blacklist_biomes, whitelist_biomes));
            }
        }

    private void setRandomItem(ItemStack fishItem, Biome biome, int lure){
        @NotNull int cfg_lure = 33;
        try {
            config.getInt("factor_lucky");
        } catch (Exception e){}
        lure = lure*cfg_lure;
        if (lure == 0){
            lure = 1;
        }
        for (FishItem item : fishItems){
            if (rnd() <= item.getChance()*lure &&(
                    (item.getBlacklist_biomes().isEmpty() || !item.getBlacklist_biomes().contains(biome)) &&
                            (item.getWhitelist_biomes().isEmpty() || item.getWhitelist_biomes().contains(biome))
                    )){
                fishItem.setType(item.getItem().getType());
                fishItem.setItemMeta(item.getItem().getItemMeta());
                return;
            }
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getCaught() instanceof Item && event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            @NotNull int lure = 0;
            try{
                lure = event.getPlayer().getInventory().getItemInMainHand().getEnchantments().get(Enchantment.LURE);
            }
            catch (Exception e){}
            Item caughtFish = (Item) event.getCaught();
            ItemStack fishItem = caughtFish.getItemStack();
            Biome biome = event.getHook().getLocation().getBlock().getBiome();
            setRandomItem(fishItem, biome, lure);
        }
    }
    private static class FishItem {
        private final ItemStack item;
        private final double chance;

        private final ArrayList<Biome> blacklist_biomes;
        private final ArrayList<Biome> whitelist_biomes;

        private FishItem(ItemStack item, double chance, ArrayList<Biome> blacklist_biomes, ArrayList<Biome> whitelist_biomes) {
            this.item = item;
            this.chance = chance;
            this.blacklist_biomes = blacklist_biomes;
            this.whitelist_biomes = whitelist_biomes;
        }
        public double getChance() {
            return chance;
        }
        public ItemStack getItem(){
            return item;
        }
        public ArrayList<Biome> getBlacklist_biomes(){
            return blacklist_biomes;
        }
        public ArrayList<Biome> getWhitelist_biomes(){
            return whitelist_biomes;
        }

    }

}
