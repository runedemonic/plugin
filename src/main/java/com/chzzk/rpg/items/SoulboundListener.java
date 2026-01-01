package com.chzzk.rpg.items;

import com.chzzk.rpg.ChzzkRPG;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;

public class SoulboundListener implements Listener {

    private final ChzzkRPG plugin;
    private final Map<UUID, List<ItemStack>> pendingReturns = new ConcurrentHashMap<>();

    public SoulboundListener(ChzzkRPG plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();

        if (!WeaponData.isWeapon(item)) {
            return;
        }

        WeaponData weaponData = new WeaponData(item);
        if (!weaponData.isOwnedBy(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§c이 장비는 귀속되어 있습니다.");
        }
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        if (!WeaponData.isWeapon(item)) {
            return;
        }

        WeaponData weaponData = new WeaponData(item);
        if (weaponData.getOwnerUuid() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (!WeaponData.isWeapon(item)) {
            return;
        }

        WeaponData weaponData = new WeaponData(item);
        if (weaponData.getOwnerUuid() != null) {
            event.setCancelled(true);
            player.sendMessage("§c귀속 장비는 버릴 수 없습니다.");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack cursor = event.getCursor();
        if (WeaponData.isWeapon(cursor)) {
            WeaponData weaponData = new WeaponData(cursor);
            if (weaponData.getOwnerUuid() != null && !(event.getClickedInventory() instanceof PlayerInventory)) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
                return;
            }
        }

        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR
                && !(event.getInventory() instanceof PlayerInventory)) {
            return;
        }

        if (event.isShiftClick() && !(event.getInventory() instanceof PlayerInventory)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
            return;
        }

        if (!(event.getInventory() instanceof PlayerInventory) && event.getHotbarButton() >= 0) {
            PlayerInventory playerInventory = ((Player) event.getWhoClicked()).getInventory();
            ItemStack hotbarItem = playerInventory.getItem(event.getHotbarButton());
            if (WeaponData.isWeapon(hotbarItem)) {
                WeaponData weaponData = new WeaponData(hotbarItem);
                if (weaponData.getOwnerUuid() != null) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
                    return;
                }
            }
        }

        ItemStack item = event.getCurrentItem();
        if (event.getClickedInventory() instanceof PlayerInventory) {
            return;
        }

        if (WeaponData.isWeapon(item)) {
            WeaponData weaponData = new WeaponData(item);
            if (weaponData.getOwnerUuid() != null) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
            }
            return;
        }

        ItemStack cursorItem = event.getCursor();
        if (!WeaponData.isWeapon(cursorItem)) {
            return;
        }

        WeaponData cursorWeapon = new WeaponData(cursorItem);
        if (cursorWeapon.getOwnerUuid() == null) {
            return;
        }

        event.setCancelled(true);
        event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack item = event.getOldCursor();
        if (!WeaponData.isWeapon(item)) {
            return;
        }

        WeaponData weaponData = new WeaponData(item);
        if (weaponData.getOwnerUuid() == null) {
            return;
        }

        if (event.getInventory() instanceof PlayerInventory) {
            return;
        }

        event.setCancelled(true);
        event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
    }

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (!WeaponData.isWeapon(item)) {
            return;
        }

        WeaponData weaponData = new WeaponData(item);
        if (weaponData.getOwnerUuid() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (!WeaponData.isWeapon(item)) {
            return;
        }

        WeaponData weaponData = new WeaponData(item);
        if (weaponData.getOwnerUuid() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemFramePlace(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame)) {
            return;
        }

        ItemFrame frame = (ItemFrame) event.getRightClicked();
        ItemStack frameItem = frame.getItem();
        if (WeaponData.isWeapon(frameItem)) {
            WeaponData weaponData = new WeaponData(frameItem);
            if (!weaponData.isOwnedBy(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c귀속 장비는 회수할 수 없습니다.");
                return;
            }
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (WeaponData.isWeapon(item)) {
            WeaponData weaponData = new WeaponData(item);
            if (weaponData.getOwnerUuid() != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c귀속 장비는 진열할 수 없습니다.");
            }
        }
    }

    @EventHandler
    public void onArmorStandPlace(PlayerArmorStandManipulateEvent event) {
        ItemStack item = event.getPlayerItem();
        if (!WeaponData.isWeapon(item)) {
            return;
        }

        WeaponData weaponData = new WeaponData(item);
        if (weaponData.getOwnerUuid() != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c귀속 장비는 진열할 수 없습니다.");
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        ItemStack mainItem = event.getMainHandItem();
        if (WeaponData.isWeapon(mainItem)) {
            WeaponData weaponData = new WeaponData(mainItem);
            if (weaponData.getOwnerUuid() != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c귀속 장비는 옮길 수 없습니다.");
                return;
            }
        }

        ItemStack offItem = event.getOffHandItem();
        if (WeaponData.isWeapon(offItem)) {
            WeaponData weaponData = new WeaponData(offItem);
            if (weaponData.getOwnerUuid() != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c귀속 장비는 옮길 수 없습니다.");
            }
        }
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (!WeaponData.isWeapon(item)) {
            return;
        }

        WeaponData weaponData = new WeaponData(item);
        if (weaponData.getOwnerUuid() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onShulkerStore(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack cursorItem = event.getCursor();
        if (!isShulker(cursorItem)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (!WeaponData.isWeapon(current)) {
            return;
        }

        WeaponData weaponData = new WeaponData(current);
        if (weaponData.getOwnerUuid() == null) {
            return;
        }

        event.setCancelled(true);
        event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack[] matrix = event.getInventory().getMatrix();
        for (ItemStack item : matrix) {
            if (!WeaponData.isWeapon(item)) {
                continue;
            }
            WeaponData weaponData = new WeaponData(item);
            if (weaponData.getOwnerUuid() != null) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§c귀속 장비는 사용할 수 없습니다.");
                return;
            }
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        for (ItemStack item : matrix) {
            if (!WeaponData.isWeapon(item)) {
                continue;
            }
            WeaponData weaponData = new WeaponData(item);
            if (weaponData.getOwnerUuid() != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    private boolean isShulker(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (!(item.getItemMeta() instanceof BlockStateMeta)) {
            return false;
        }
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        BlockState state = meta.getBlockState();
        return state instanceof ShulkerBox;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return;
        }

        Player player = event.getEntity();
        List<ItemStack> keptItems = new ArrayList<>();
        event.getDrops().removeIf(item -> {
            if (!WeaponData.isWeapon(item)) {
                return false;
            }
            WeaponData weaponData = new WeaponData(item);
            if (weaponData.isOwnedBy(player.getUniqueId())) {
                keptItems.add(item);
                return true;
            }
            return false;
        });

        if (!keptItems.isEmpty()) {
            pendingReturns.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>()).addAll(keptItems);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        List<ItemStack> items = pendingReturns.remove(playerId);
        if (items == null || items.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (ItemStack item : items) {
                event.getPlayer().getInventory().addItem(item);
            }
            event.getPlayer().sendMessage("§a귀속 장비가 복구되었습니다.");
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        List<ItemStack> items = pendingReturns.remove(playerId);
        if (items == null || items.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (ItemStack item : items) {
                event.getPlayer().getInventory().addItem(item);
            }
            event.getPlayer().sendMessage("§a귀속 장비가 복구되었습니다.");
        });
    }
}
