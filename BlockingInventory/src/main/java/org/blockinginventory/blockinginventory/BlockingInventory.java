package org.blockinginventory.blockinginventory;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockingInventory extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    public void mainBlock(Inventory inventory, Player player, boolean forced){
        int block_cell = getPermissionNumber("locked_cell_", player, 27);
        if ((block_cell <= 0 && !forced) || player.getGameMode() == GameMode.CREATIVE){
            return;
        }
        ItemStack itemBlock = new ItemStack(Material.BARRIER);
        ItemMeta im = itemBlock.getItemMeta();
        im.setDisplayName("§0§kVOID");
        itemBlock.setItemMeta(im);
        for (ItemStack itemStack: inventory.getContents()){
            if (itemStack != null && itemStack.equals(itemBlock)){
                itemStack.setAmount(0);
            }
        }
//        ItemStack itemStacks [] = inventory.getContents();
        for (int i = 0; i < block_cell ; i++){
            if (inventory.getContents()[9+i] == null) {
                inventory.setItem(9+i, itemBlock);
            }
        }
    }
    public int getPermissionNumber(String permission, Player player, int check) {
        for (int i = 0; i <= check; i++) {
            String perm = permission + i;
            if (player.hasPermission(perm)) {
                return i;
            }
        }
        return 0; // если игрок не имеет никаких пермишенов с данным префиксом
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();
        mainBlock(inventory, player, true);
    }
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();
        mainBlock(inventory, player, true);
    }

    public boolean canselIfBarrier(ItemStack item){
        if (item == null) {
            return false;
        }
        if (item.getType() == Material.BARRIER
                && item.getItemMeta().getDisplayName().equals("§0§kVOID")) {
            return true;
        }
        return false;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() == GameMode.CREATIVE){
            return;
        }
        ItemStack clickedItem = event.getCurrentItem();
        Inventory inventory = event.getClickedInventory();
        if (inventory == null){
            return;
        }
        if (inventory.getType() == InventoryType.PLAYER) {
            mainBlock(inventory, player, false);
        }
        if (canselIfBarrier(event.getCurrentItem())) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }
    }
     @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (canselIfBarrier(event.getItemInHand())){
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
     }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        if (canselIfBarrier(item)){
            event.setCancelled(true);
            player.updateInventory();

        }
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Inventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (canselIfBarrier(item)) {
                item.setAmount(0);
                player.updateInventory();
            }
        }
    }
}
