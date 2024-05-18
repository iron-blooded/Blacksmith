package org.goldenchest.goldenchest;

import java.io.File;
import java.sql.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.lang.*;
import java.io.*;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.*;

public class GoldenChest extends JavaPlugin implements Listener {
    private Map<String, Inventory> backpackInventories;
    private Connection connection;
    private FileConfiguration config = this.getConfig();

    private FileConfiguration configWeight;


    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        backpackInventories = new HashMap<>();
        setupDatabase();
        Objects.requireNonNull(getCommand("gc_reload")).setExecutor((commandSender, command, s, strings) -> {
            this.saveDefaultConfig();
            reloadConfig();
            config = getConfig();
            try {
                configWeight = YamlConfiguration.loadConfiguration(new FileReader(getDataFolder() + File.separator + "configW.yml"));
            } catch (Exception e) {
            }
            commandSender.sendMessage("Конфиги обновлены");
            return true;
        });
        Objects.requireNonNull(getCommand("get_gc")).setExecutor((commandSender, command, s, strings) -> {
            if (strings.length == 0) {
                commandSender.sendMessage("Название сундука то укажи");
                return true;
            } else {
                try {
                    Inventory inventory = backpackInventories.get(strings[0]);
                    Player player = (Player) commandSender;
                    player.openInventory(inventory);
                } catch (Exception e) {
                    commandSender.sendMessage("Такого хранилища нет");
                    return true;
                }
            }
            return false;
        });
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                saveBackpacks();
            }
        };
        runnable.runTaskTimer(this, 0, 20L * 25L);
    }

    @Override
    public void onDisable() {
        System.out.println("Выключается плагин");
        saveBackpacks();
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupDatabase() {
        try {
            configWeight = YamlConfiguration.loadConfiguration(new FileReader(getDataFolder() + File.separator + "configW.yml"));
        } catch (Exception e) {
        }
        try {
            File folder = new File(getDataFolder() + File.separator);
            if (!folder.exists()) {
                folder.mkdir();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + File.separator + "backpacks.db");
            PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS backpacks (player TEXT PRIMARY KEY, content TEXT)");
            statement.execute();
            loadBackpacks();
        } catch (ClassNotFoundException | SQLException e) {
            getLogger().severe("Failed to setup SQLite database.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void createNewDatabase() {
        String dbPath = getDataFolder().getAbsolutePath() + "/backpacks.db";
        String url = "jdbc:sqlite:" + dbPath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loadBackpacks() throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM backpacks");
        ResultSet result = statement.executeQuery();
//        statement.close();
        while (result.next()) {
            String player = result.getString("player");
            System.out.println(player);
            String content = result.getString("content");
            Inventory inventory = getServer().createInventory(null, 54);
            ItemStack[] contents = deserializeItems(content);
            if (contents != null) {
                inventory.setContents(contents);
            }
            backpackInventories.put(player, inventory);
        }
        result.close();
    }

    private void saveBackpacks() {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO backpacks (player, content) VALUES (?, ?) ON CONFLICT(player) DO UPDATE SET content=excluded.content");
            for (Map.Entry<String, Inventory> entry : backpackInventories.entrySet()) {
                statement.setString(1, entry.getKey());
                statement.setString(2, serializeItems(entry.getValue().getContents()));
                statement.addBatch();
            }
            statement.executeBatch();
            statement.close();
        } catch (SQLException e) {
            getLogger().severe("Failed to save backpacks.");
            e.printStackTrace();
        }
    }

    private String serializeItems(ItemStack[] items) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                builder.append(i).append('ᴗ').append(item.getAmount()).append('ᴗ').append(item.getType().name()).append('ᴗ');
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    builder.append(itemMetaToString(meta)).append('ᴗ');
//                    if (meta.hasDisplayName()) {
//                        builder.append(meta.getDisplayName().replaceAll("ᴗ", "")).append('ᴗ');
//                    } else {
//                        builder.append('ᴗ');
//                    }
//                    if (meta.hasLore()) {
//                        builder.append(meta.getLore().toString().replaceAll("ᴗ", "")).append('ᴗ');
//                    } else {
//                        builder.append('ᴗ');
//                    }
                } else {
                    builder.append('ᴗ');//.append('ᴗ');
                }
                builder.append(item.getDurability()).append('ᴗ').append(item.getEnchantments().toString()).append('•');

            }
        }
        return builder.toString();
    }

    private ItemStack[] deserializeItems(String str) {
        ItemStack[] items = new ItemStack[54];
        String[] pairs = str.split("•");
        for (String pair : pairs) {
            String[] values = pair.split("ᴗ");
            if (values.length >= 5) {
                int slot = Integer.parseInt(values[0]);
                if (slot >= 0 && slot < 54) {
                    int amount = Integer.parseInt(values[1]);
                    Material material = Material.getMaterial(values[2]);
                    if (material != null) {
                        ItemStack item = new ItemStack(material, amount);
                        if (!values[5].isEmpty()) {
                            item = new ItemStack(material, amount, Short.parseShort(values[4]));
                        }
                        ItemMeta meta = item.getItemMeta();
                        if (!values[3].isEmpty()) {
                            item.setItemMeta(stringToItemMeta(values[3]));
                            meta = item.getItemMeta();
//                            meta.setDisplayName(values[3]);
                        }
//                        if (!values[4].isEmpty()) {
//                            List<String> lore = Arrays.asList(values[4].substring(1, values[4].length()-1).split(", "));
//                            meta.setLore(lore);
//                        }
//                        if (!values[5].isEmpty()) {
//                            item.setItemMeta(meta);
//                            item.addUnsafeEnchantments(parseEnchantments(values[5]));
//                        }
                        items[slot] = item;
                    }
                }
            }
        }
        return items;
    }

    public static Map<Enchantment, Integer> parseEnchantments(String enchantmentsString) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        // Удаление скобок и разбиение строки на пары "enchantment=value"
        String[] pairs = enchantmentsString.replace("{", "").replace("}", "").split(", Enchantment");
        for (String pair : pairs) {
            // Разбиение пары на "enchantment" и "value"
            String[] parts = pair.split("]=");
            if (parts.length >= 1 && !parts[0].isEmpty()) {
                // Получение объекта Enchantment из строки "enchantment"
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].split("\\[")[1].split(", ")[0].replace("minecraft:", "")));
                // Получение значения int из строки "value"

                int value = Integer.parseInt(parts[1].replace("]", ""));
                // Добавление пары в Map
                enchantments.put(enchantment, value);
            }
        }
        return enchantments;
    }

    public static String itemMetaToString(ItemMeta itemMeta) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(itemMeta);
            dataOutput.close();
            return new String(outputStream.toByteArray(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemMeta stringToItemMeta(String itemMetaString) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(itemMetaString.getBytes(StandardCharsets.ISO_8859_1));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemMeta itemMeta = (ItemMeta) dataInput.readObject();
            dataInput.close();
            return itemMeta;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openChest(ItemMeta meta, Player player, Integer len, Integer blocked_slots, boolean need_id) {
        ItemStack item = player.getInventory().getItemInMainHand();
        String uid = player.getName();
        if (need_id) {
            String lore = "";
            try {
                lore = item.getItemMeta().getLore().get(0);
            } catch (Exception e) {
            }
            if (lore == "") {
                lore = String.valueOf(new Random().nextInt(9999999));
                ArrayList<String> l = new ArrayList<String>();
                l.add(lore);
                meta.setLore(l);
                item.setItemMeta(meta);
            }
            uid = lore;
        }
        Inventory inventory = backpackInventories.get(uid);
        if (inventory == null) {
            inventory = getServer().createInventory(null, len);
        }
        ItemStack void_panel = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta im = void_panel.getItemMeta();
        im.setDisplayName("§6VOID");
        ArrayList<String> lore = new ArrayList<String>();
//        lore.add(0, "Weight: 30");
        im.setLore(lore);
        void_panel.setItemMeta(im);
        inventory.remove(void_panel);
        for (int i = len - 1; i >= len - blocked_slots; i--) {
            inventory.setItem(i, void_panel);
        }
        Inventory inv2 = Bukkit.createInventory(inventory.getHolder(), len, "§6Yggdrasil");
        for (int i = 0; i < len; i++) {
            inv2.setItem(i, inventory.getItem(i));
        }
        inventory = inv2;
        backpackInventories.put(uid, inventory);
        player.getOpenInventory().close();
        player.closeInventory();
        player.openInventory(Bukkit.createInventory(null, 9, ""));
        player.openInventory(inventory);

    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        player = getServer().getPlayer(player.getUniqueId());
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK
                || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            for (List li : (List<List>) config.getList("chests")) {
                if (item.getAmount() >= 1) {
                    if ((Integer) li.get(3) != 0 && item.getAmount() != 1) {
                        return;
                    }
                }
                if (meta.getDisplayName().equals(li.get(0))) {
                    event.setCancelled(true);
                    if (event.getClickedBlock() == null
                            || (event.getClickedBlock().getType() != Material.CHEST
                            && event.getClickedBlock().getType() != Material.BARREL
                            && event.getClickedBlock().getType().name().indexOf("SHULKER_BOX") < 0)) {
                        openChest(meta, player, (Integer) li.get(1), (Integer) li.get(2), (Integer) li.get(3) != 0);
                    }
                    return;
                }
            }
        }
    }

//    @EventHandler
//    public void onPlayerOpenChest(InventoryOpenEvent event){
//        Player player = (Player) event.getPlayer();
//        ItemStack item = player.getInventory().getItemInMainHand();
//        ItemMeta meta = item.getItemMeta();
//        if (item == null || meta==null || event.getView().getTitle().equals("§6Yggdrasil")){
//            return;
//        }
//        for (List li : (List<List>) config.getList("chests")){
//            if (meta.getDisplayName().equals(li.get(0))){
//                if (!event.getView().getTitle().equals("§6Yggdrasil")) {
//                    event.setCancelled(true);
//                    System.out.println("yesssssss");
//                    return;
//                }
//                else {
//                    System.out.println(event.getInventory().getLocation());
//                }
//            }
//        }
//    }

    @EventHandler
    public void clickEvent(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE
                && event.getCurrentItem().getItemMeta().getDisplayName().equals("§6VOID")) {
            event.setCancelled(true);
            return;
        }
//        if (event.getView().getTitle().equals("§6Yggdrasil")
//                && event.getCurrentItem().getType().name().indexOf("SHULKER_BOX") >= 0
//        ) {
//            event.setCancelled(true);
//            return;
//        }
        for (List li : (List<List>) config.getList("chests")) {
            if (event.getView().getTitle().equals("§6Yggdrasil")
                    && event.getCurrentItem().getItemMeta().getDisplayName().equals(li.get(0))) {
                event.setCancelled(true);
                return;
            }
        }
    }


    public void setWeightItem(ItemStack item, ItemStack[] content, float weight) {
        for (ItemStack item_in_invent : content) {
            if (item_in_invent != null) {
                boolean weight_searching = false;
                List<String> lore = item_in_invent.getItemMeta().getLore();
                if (lore != null) {
                    for (String str_lore : lore) {
                        if (countMatches(str_lore, "Weight") > 0) {
                            weight += Float.valueOf(str_lore.split(" ")[1]) * item_in_invent.getAmount();
                            weight_searching = true;
                        }
                    }
                }
                if (!weight_searching) {
                    for (LinkedHashMap map : (List<LinkedHashMap>) configWeight.getList("materialWeights")) {
                        String material = (String) map.get("material");
                        String wei = map.values().toArray()[1].toString();
                        if (upperCase(material).equals(item_in_invent.getType().name())) {
                            weight += Float.valueOf(wei) * item_in_invent.getAmount();
                            weight_searching = true;
                        }

                    }
                    if (!weight_searching && item_in_invent.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                        weight += Float.valueOf(configWeight.getString("defaultWeight")) * item_in_invent.getAmount();
                    }
                }
            }
        }
        float chest_weight = 0;
        for (List li : (List<List>) config.getList("chests")) {
            if (item.getItemMeta().getDisplayName().equals(li.get(0))) {
                weight *= Float.valueOf(li.get(5).toString()) / 100;
                chest_weight = Float.valueOf(li.get(4).toString());
                weight += chest_weight;
            }
        }
        if(item.getItemMeta() instanceof BlockStateMeta){
            List<?> shulker = config.getList("shulker");
            weight *= Float.valueOf(shulker.get(1).toString()) / 100;
            chest_weight = Float.valueOf(shulker.get(0).toString());
            weight += chest_weight;
        }
        List<String> lore = item.getItemMeta().getLore();
        if (lore != null) {
            for (int il = lore.size() - 1; il >= 0; il--) {
                if (countMatches(lore.get(il), "Weight") > 0
                        || countMatches(lore.get(il), "Пусто") > 0) {
                    lore.remove(il);
                }
            }
        }
        ItemMeta meta = item.getItemMeta();
        try {
            lore.add("§0Weight: " + String.valueOf(weight));
            if (weight == chest_weight) {
                try {
                    lore.add("§f*Пусто*");
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            lore = new ArrayList<>();
            lore.add("§0Weight: " + String.valueOf(weight));
            if (weight == chest_weight) {
                try {
                    lore.add("§f*Пусто*");
                } catch (Exception es) {
                }
            }
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @EventHandler
    public void InventoryClose(InventoryCloseEvent event) {
        List<org.bukkit.entity.HumanEntity> viewers = event.getInventory().getViewers();
        for (HumanEntity viewer : viewers) {
            InventoryView inventory = viewer.getOpenInventory();
            if (inventory.getTitle().equals("§6Yggdrasil")) {
                ItemStack[] content = event.getInventory().getContents();
                ItemStack item = ((Player) event.getPlayer()).getItemInHand();
                setWeightItem(item, content, 0F);
            }
        }
    }

    @EventHandler
    public void playerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if(item.getItemMeta() instanceof BlockStateMeta){
            BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();
                if(meta.getBlockState() instanceof ShulkerBox){
                    ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                    ItemStack[] content = shulker.getInventory().getContents();
                    setWeightItem(item, content, 0F);
                }
        }
    }
}