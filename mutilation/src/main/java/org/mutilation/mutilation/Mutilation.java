package org.mutilation.mutilation;

import java.io.File;
import java.security.Permission;
import java.security.Permissions;
import java.sql.*;
import java.util.*;

//import jdk.jfr.internal.MetadataDescriptor;
//import net.kyori.adventure.sound.Sound;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.lang.*;
import java.io.*;

import java.util.HashMap;
import java.util.Map;
import static org.apache.commons.lang.StringUtils.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.text.SimpleDateFormat;
import java.util.Date;
public final class Mutilation extends JavaPlugin implements Listener{
    public void chechFolder(){
        File folder = new File(getDataFolder().getAbsolutePath());
        if (!folder.exists()) {
            folder.mkdir();
        }
        File file = new File(getDataFolder() + File.separator + "logs.txt");
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void logsNewLine(String str){
        chechFolder();
        File file = new File(getDataFolder() + File.separator + "logs.txt");
        FileWriter fr = null;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM.dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        try {
            fr = new FileWriter(file,true);
            fr.write("§d" + dtf.format(now) + ": " + str);

        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public String[] logsGetLines(String name, int len){
        chechFolder();
        FileReader file = null;
        try {
            file = new FileReader(getDataFolder() + File.separator + "logs.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Scanner scan = new Scanner(file);
        String result = "";
        int i = 0;
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.indexOf(name) >= 0) {
                result += line + "\n";
            }
            i++;
        }
        try {
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String new_result = "";
        for (i = result.split("\n").length-1; new_result.split("\n").length<len && i >= 0; i--){
            new_result = result.split("\n")[i] + "\n" + new_result;
        }
        return new_result.split("\n");
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
//        Objects.requireNonNull(getCommand("history_mutilation")).setExecutor(new commandHistory());
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("history_mutilation")).setExecutor((@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) -> {
            if (args.length >= 1){
                String name = args[0];
                int len = 10;
                if (args.length >= 2){
                    len = Integer.valueOf(args[1]);
                }
                String[] logs = logsGetLines(name, len);
                sender.sendMessage("§b==========↓Увечия↓==========");
                sender.sendMessage(logs);
                sender.sendMessage("§b==========↑Увечия↑==========");
            }
            return true;
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    private static int rnd(){
        return new Random().nextInt(100);
    }

    private float checkAndReturnEnchants(ItemStack item, Enchantment enchantment, float num){
        float dmg = 1F;
        if (item == null){
            return dmg;
        }
        for (Map.Entry<Enchantment, Integer> enchant : item.getEnchantments().entrySet()){
            dmg += checkAndReturnEnchant(enchant, enchantment, num);
        }
        return dmg;
    }

    private float checkAndReturnEnchant(Map.Entry<Enchantment, Integer> enchant, Enchantment enchantment, float num){
        float chance = 1F;
        if(enchant.getKey().equals(enchantment)){ // острота
            chance += enchant.getValue() * num;
        }
        return chance;
    }


    private float getFinalDamage(float damage, float chance_cut, float itemProtection){
        float result = (float) (Math.pow((damage*5), 1/1.095F)*chance_cut/itemProtection)/2;
        if (result > 100){
            result = 100;
        }
        return result;
    }

    private void sendMessageRadius(String message, int radius, Location locate){
        for (Player player : getServer().getWorld(locate.getWorld().getUID()).getPlayers()) {
            if(player.getLocation().distance(locate) <= radius) {
                player.sendMessage(message);
            }
        }
        logsNewLine(message + "\n");
    }

    public void setTimerDamage(float damage, Player player, long time){
        float finalDamage_set = damage;
        if (finalDamage_set <= 0){
            return;
        }
        if (finalDamage_set >= 6){
            player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_GOAT_SCREAMING_DEATH, 2.0f, 0.5f);

        } else if (finalDamage_set >= 3) {
            player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 1.0f);
        }
        player.damage(finalDamage_set*2);
        return;
//        BukkitRunnable runnable = new BukkitRunnable() {
//            @Override
//            public void run() {
//                player.damage(finalDamage_set*2);
//            }
//        };
//        runnable.runTaskLater(this, 20L * time);
    }
    public void dropItemInInventory(Player player, ItemStack item){
        player.getInventory().remove(item);
        player.getWorld().dropItemNaturally(player.getLocation(), item);
    }

    public void amputeDropItem(Player player, ItemStack item){
        Set<String> tags =  player.getScoreboardTags();
        if (item != null
                && item.getType() != Material.AIR) {
            if (tags.contains("MainHandShoulderAmputation")
                    || tags.contains("MainHandArmAmputation")
                    || tags.contains("MainHandWristAmputation")){
                dropItemInInventory(player, item);
            }
            if (tags.contains("OffHandShoulderAmputation")
                    ||tags.contains("OffHandArmAmputation")
                    ||tags.contains("OffHandWristAmputation")) {
                player.getInventory().setItemInOffHand(null);
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }
    public int getPermissionNumber(String permission, Player player) {
        for (int i = 100; i >= 0; i--) {
            String perm = permission + i;
            if (player.hasPermission(perm)) {
                return i;
            }
        }
        return 0; // если игрок не имеет никаких пермишенов с данным префиксом
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event){
        if (event.getEntity() instanceof  Player){
            Player player = (Player) event.getEntity();
//            if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent || true){
            if (event.getDamager() instanceof Player){
                Player playerDamage = null;
                try {
//                    playerDamage = (Player) ((EntityDamageByEntityEvent) player.getLastDamageCause()).getDamager();
//                    playerDamage.getName();
                    playerDamage = (Player) event.getDamager();
                } catch (Exception e){
                    return;
                }
                float damage = (float) event.getFinalDamage();
                if (player.getLocation().distance(playerDamage.getLocation()) > 10
                || damage <= 3
                || event.isCancelled()
                ||!event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)){
                    return;
                }
                ItemStack item_damage = playerDamage.getInventory().getItemInMainHand();
                ItemStack item_in_main_hand = player.getInventory().getItemInMainHand();
                ItemStack item_in_off_hand = player.getInventory().getItemInOffHand();
                Map<Enchantment, Integer> enchantsDamage = item_damage.getEnchantments();
                float chance_cut = 1F; // шанс поранить
                if (item_damage != null){
                    for (Map.Entry<Enchantment, Integer> enchant: enchantsDamage.entrySet()){
                        chance_cut += (checkAndReturnEnchant(enchant, Enchantment.DAMAGE_ALL, 10F)); // острота
                        chance_cut += (checkAndReturnEnchant(enchant, Enchantment.IMPALING, 7.5F)); // пронзатель
                        chance_cut += (checkAndReturnEnchant(enchant, Enchantment.ARROW_DAMAGE, 7.5F)); // сила
                        chance_cut += (checkAndReturnEnchant(enchant, Enchantment.PIERCING, 2F)); // пронзающая стрела
                    }
                }
                ItemStack[] itemsProtection = player.getInventory().getArmorContents();
                float itemProtection = 0;
                float itog = 0;
                int radius_message = 6;
                float damage_set = 0;
                if (rnd() <= 5 && !player.hasPermission("parts.disable_head")){ // Голова
                    itemProtection = checkAndReturnEnchants(itemsProtection[3], Enchantment.PROTECTION_ENVIRONMENTAL, 12);
                    itog = getFinalDamage(damage, chance_cut, itemProtection);
                    itog -= getPermissionNumber("parts.head_resistance_", player);
                    PersistentDataContainer dataContainer = player.getPersistentDataContainer();
                    if (itog >= rnd()){
                        Location locate = player.getLocation();
                        if (rnd() <= 6){
                            sendMessageRadius("§c"+playerDamage.getName()+" нанес легкое повреждение мозгу "+player.getName(), radius_message, locate);
                            player.addScoreboardTag("BrainDamage1");
                            damage_set += 30;
                        } else if (rnd() <= 1) {
                            sendMessageRadius("§4"+playerDamage.getName()+" нанес тяжелое повреждение мозгу "+player.getName(), radius_message, locate);
                            player.addScoreboardTag("BrainDamage2");
                            damage_set += 9999;
                        } else if (rnd() <= 6){
                            sendMessageRadius("§4"+playerDamage.getName()+" повредил шею "+player.getName(), radius_message, locate);
                            player.addScoreboardTag("NeckDamage");
                            damage_set += 9;
                        } else if (rnd() <= 15) {
                            sendMessageRadius("§4"+playerDamage.getName()+" повредил глаз "+player.getName(), radius_message, locate);
                            player.addScoreboardTag("EyeDamage");
                            damage_set += 3.5;
                        } else if (rnd() <= 20){
                            sendMessageRadius("§c"+playerDamage.getName()+" нанес сотресение мозга "+player.getName(), radius_message, locate);
                            player.addScoreboardTag("BrainConfusion");
                            damage_set += 3;
                        } else {
                            sendMessageRadius("§c"+playerDamage.getName()+" оставил шрам на лице "+player.getName(), radius_message, locate);
                            player.addScoreboardTag("FaceScar");
                        }
                        setTimerDamage(damage_set, player, 4L);
                    } else {
                        sendMessageRadius("§e"+playerDamage.getName()+" не удалось пробить голову "+player.getName(), radius_message/2, player.getLocation());
                    }
                    return;
                }
                else if (rnd() <= 20 && !player.hasPermission("parts.disable_body")) { // Тело
                    itemProtection = checkAndReturnEnchants(itemsProtection[2], Enchantment.PROTECTION_ENVIRONMENTAL, 12);
                    itog = getFinalDamage(damage, chance_cut, itemProtection);
                    itog -= getPermissionNumber("parts.body_resistance_", player);
                    if (itog >= rnd()) {
                        Location locate = player.getLocation();
                        if (rnd() <= 2) {
                            sendMessageRadius("§4" + playerDamage.getName() + " пробил сердце " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("HeartImpale");
                            damage_set += 25;
                        } else if (rnd() <= 11) {
                            sendMessageRadius("§4" + playerDamage.getName() + " нанес тяжелое повреждение корпусу " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("CorpseDamage2");
                            damage_set += 8;
                        } else if (rnd() <= 7) {
                            sendMessageRadius("§4" + playerDamage.getName() + " проткнул легкое " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("LungImpale");
                            damage_set += 6;
                        } else if (rnd() <= 11) {
                            sendMessageRadius("§4" + playerDamage.getName() + " пронзил орган " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("OrganImpale");
                            damage_set += 4;
                        } else if (rnd() <= 25) {
                            sendMessageRadius("§c" + playerDamage.getName() + " нанес легкое повреждение корпусу " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("CorpseDamage1");
                            damage_set += 4;
                        } else {
                            sendMessageRadius("§e" + playerDamage.getName() + " оставил шрам на теле " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("CorpseScar");
                        }
                        setTimerDamage(damage_set, player, 0L);
                    } else {
                        sendMessageRadius("§e"+playerDamage.getName()+" не удалось нанести повреждение корпусу "+player.getName(), radius_message/2, player.getLocation());
                    }
                    return;
                }
                else if (rnd() <= 20 && !player.hasPermission("parts.disable_legs")) { // Ноги
                    itemProtection = checkAndReturnEnchants(itemsProtection[0], Enchantment.PROTECTION_ENVIRONMENTAL, 12);
                    itemProtection += checkAndReturnEnchants(itemsProtection[1], Enchantment.PROTECTION_ENVIRONMENTAL, 12);
                    itog = getFinalDamage(damage, chance_cut, itemProtection);
                    itog -= getPermissionNumber("parts.legs_resistance_", player);
                    if (itog >= rnd()){
                        Location locate = player.getLocation();
                        if (rnd() <= 2) {
                            sendMessageRadius("§4" + playerDamage.getName() + " порезал артерию на ноге " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("ArterialLegDamage");
                            damage_set += 8;
                        } else if (rnd() <= 1) {
                            sendMessageRadius("§c" + playerDamage.getName() + " ампутировал стопу " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("FootAmputation");
                            damage_set += 8;
                        } else if (rnd() <= 15) {
                            sendMessageRadius("§c" + playerDamage.getName() + " нанес повреждение стопе " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("FootDamage");
                            damage_set += 1;
                        } else if (rnd() <= 25) {
                            sendMessageRadius("§4" + playerDamage.getName() + " нанес тяжелое повреждение ноге " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("LegDamage2");
                            damage_set += 6;
                        } else if (rnd() <= 2 && player.getScoreboardTags().contains("LegsAmputation")) {
                            sendMessageRadius("§4" + playerDamage.getName() + " ампутировал последнюю ногу " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("LegAmputation");
                            damage_set += 13;
                        } else if (rnd() <= 2 && player.getScoreboardTags().contains("LegAmputation")) {
                            sendMessageRadius("§4" + playerDamage.getName() + " ампутировал ногу " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("LegsAmputation");
                            damage_set += 8;
                        } else {
                            sendMessageRadius("§c" + playerDamage.getName() + " нанес легкое повреждение ноге " + player.getName(), radius_message, locate);
                            player.addScoreboardTag("LegDamage1");
                            damage_set += 1.5;
                        }
                        setTimerDamage(damage_set, player, 2L);
                    } else {
                        sendMessageRadius("§e"+playerDamage.getName()+" не удалось нанести повреждения ногам "+player.getName(), radius_message/2, player.getLocation());
                    }
                    return;
                }
                else if (rnd() <= 35 && !player.hasPermission("parts.disable_arms")) { // Руки
                    itemProtection = checkAndReturnEnchants(itemsProtection[2], Enchantment.PROTECTION_ENVIRONMENTAL, 12);
                    itog = getFinalDamage(damage, chance_cut, itemProtection);
                    itog -= getPermissionNumber("parts.arms_resistance_", player);
                    if (itog >= rnd()){
                        Location locate = player.getLocation();
                        int r = 50;
                        if (player.getInventory().getItemInMainHand() != null){
                            r += 25;
                        }
                        if (r >= rnd()){ // Главная рука
                            if (rnd() <= 1) {
                                sendMessageRadius("§4" + playerDamage.getName() + " ампутировал основную руку ниже плеча " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("MainHandShoulderAmputation");
                                dropItemInInventory(player, item_in_main_hand);
                                damage_set += 9.5;
                            } else if (rnd() <= 2) {
                                sendMessageRadius("§4" + playerDamage.getName() + " ампутировал основную руку ниже предплечья " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("MainHandArmAmputation");
                                dropItemInInventory(player, item_in_main_hand);
                                damage_set += 6.5;
                            } else if (rnd() <= 3) {
                                sendMessageRadius("§4" + playerDamage.getName() + " ампутировал запястье основной руки " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("MainHandWristAmputation");
                                dropItemInInventory(player, item_in_main_hand);
                                damage_set += 4.5;
                            } else if (rnd() <= 4) {
                                sendMessageRadius("§4" + playerDamage.getName() + " ампутировал палец на основной руке " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("MainHandFingerAmputation");
                                damage_set += 1;
                            } else if (rnd() <= 10) {
                                sendMessageRadius("§c" + playerDamage.getName() + " повредил плечо на основной руке " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("HandShoulderDamage");
                                damage_set += 4.25;
                            } else if (rnd() <= 15) {
                                sendMessageRadius("§c" + playerDamage.getName() + " нанес повреждение предплечью на основной руке " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("MainHandArmDamage");
                                damage_set += 2.25;
                            } else {
                                sendMessageRadius("§c" + playerDamage.getName() + " нанес повреждение запястью основной руки " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("MainHandWristDamage");
                                damage_set += 1.75;
                            }
                        } else { // Второстепенная
                            if (rnd() <= 1) {
                                sendMessageRadius("§4" + playerDamage.getName() + " ампутировал второстепенную руку ниже плеча " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("OffHandShoulderAmputation");
                                dropItemInInventory(player, item_in_off_hand);
                                damage_set += 9.5;
                            } else if (rnd() <= 1) {
                                sendMessageRadius("§4" + playerDamage.getName() + " ампутировал руку ниже предплечья второстепенной руки " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("OffHandArmAmputation");
                                dropItemInInventory(player, item_in_off_hand);
                                damage_set += 6.5;
                            } else if (rnd() <= 1) {
                                sendMessageRadius("§4" + playerDamage.getName() + " ампутировал запястье на второстепенной руке " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("OffHandWristAmputation");
                                dropItemInInventory(player, item_in_off_hand);
                                damage_set += 4.5;
                            } else if (rnd() <= 1) {
                                sendMessageRadius("§4" + playerDamage.getName() + " ампутировал палец на второстепенной руке " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("OffHandFingerAmputation");
                                damage_set += 1;
                            } else if (rnd() <= 15) {
                                sendMessageRadius("§c" + playerDamage.getName() + " нанес повреждение предплечью второстепенной руки " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("OffHandArmDamage");
                                damage_set += 4.25;
                            } else {
                                sendMessageRadius("§c" + playerDamage.getName() + " нанес повреждение запястье на второстепенной руке " + player.getName(), radius_message, locate);
                                player.addScoreboardTag("OffHandWristDamage");
                                damage_set += 1.75;
                            }
                        }
                        setTimerDamage(damage_set, player, 0L);
                    } else {
                        sendMessageRadius("§e"+playerDamage.getName()+" не удалось нанести повреждения рукам "+player.getName(), radius_message/2, player.getLocation());
                    }
                    return;
                }
                event.setCancelled(true);
            }
        }
    }

}
