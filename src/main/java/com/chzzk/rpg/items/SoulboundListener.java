package com.chzzk.rpg.items;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.gui.RpgGui;
import java.io.File;
import java.io.IOException;
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
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

public class SoulboundListener implements Listener {

    private final ChzzkRPG plugin;
    private final Map<UUID, List<ItemStack>> pendingReturns = new ConcurrentHashMap<>();
    private final File pendingFile;
    private BukkitTask saveTask;

    public SoulboundListener(ChzzkRPG plugin) {
        this.plugin = plugin;
        this.pendingFile = new File(plugin.getDataFolder(), "soulbound_returns.yml");
        loadPendingReturns();
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
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (!pendingReturns.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        scheduleSave();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Allow soulbound items in RPG GUIs (Enhance, Grade, Cube, etc.)
        if (event.getInventory().getHolder() instanceof RpgGui) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        boolean clickedPlayerInventory = event.getClickedInventory() instanceof PlayerInventory;
        boolean clickedTopInventory = event.getClickedInventory() != null && !clickedPlayerInventory;

        ItemStack cursor = event.getCursor();
        if (WeaponData.isWeapon(cursor)) {
            WeaponData weaponData = new WeaponData(cursor);
            if (weaponData.getOwnerUuid() != null
                    && clickedTopInventory) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
                return;
            }
        }

        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR
                && !(event.getInventory() instanceof PlayerInventory)) {
            for (ItemStack stack : event.getInventory().getContents()) {
                if (!WeaponData.isWeapon(stack)) {
                    continue;
                }
                WeaponData weaponData = new WeaponData(stack);
                if (weaponData.getOwnerUuid() != null && !weaponData.isOwnedBy(player.getUniqueId())) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
                    return;
                }
            }
            return;
        }

        if (event.isShiftClick()
                && !(event.getInventory() instanceof PlayerInventory)
                && clickedPlayerInventory) {
            ItemStack shiftItem = event.getCurrentItem();
            if (WeaponData.isWeapon(shiftItem)) {
                WeaponData weaponData = new WeaponData(shiftItem);
                if (weaponData.getOwnerUuid() != null) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
                    return;
                }
            }
        }

        if (!(event.getInventory() instanceof PlayerInventory) && event.getHotbarButton() >= 0) {
            PlayerInventory playerInventory = player.getInventory();
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
        if (clickedPlayerInventory) {
            return;
        }

        if (WeaponData.isWeapon(item)) {
            WeaponData weaponData = new WeaponData(item);
            if (weaponData.getOwnerUuid() != null && !weaponData.isOwnedBy(player.getUniqueId())) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§c귀속 장비는 회수할 수 없습니다.");
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

        if (event.getClickedInventory() != null) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Allow soulbound items in RPG GUIs
        if (event.getInventory().getHolder() instanceof RpgGui) {
            return;
        }

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

        int topSize = event.getView().getTopInventory().getSize();
        boolean draggingToTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (draggingToTop) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage("§c귀속 장비는 보관할 수 없습니다.");
        }
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
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        if (isSoulboundWeapon(first) || isSoulboundWeapon(second)) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        ItemStack upper = event.getInventory().getUpperItem();
        ItemStack lower = event.getInventory().getLowerItem();
        if (isSoulboundWeapon(upper) || isSoulboundWeapon(lower)) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack base = event.getInventory().getInputEquipment();
        ItemStack addition = event.getInventory().getInputMineral();
        if (isSoulboundWeapon(base) || isSoulboundWeapon(addition)) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        if (isSoulboundWeapon(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (isSoulboundWeapon(item)) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage("§c귀속 장비는 인챈트할 수 없습니다.");
        }
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

    private boolean isSoulboundWeapon(ItemStack item) {
        if (!WeaponData.isWeapon(item)) {
            return false;
        }
        WeaponData weaponData = new WeaponData(item);
        return weaponData.getOwnerUuid() != null;
    }

    @SuppressWarnings("unchecked")
    private void loadPendingReturns() {
        if (!pendingFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(pendingFile);
        for (String key : config.getKeys(false)) {
            List<ItemStack> items = (List<ItemStack>) config.getList(key);
            if (items == null || items.isEmpty()) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                pendingReturns.put(uuid, new ArrayList<>(items));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void savePendingReturns() {
        if (!pendingFile.getParentFile().exists()) {
            pendingFile.getParentFile().mkdirs();
        }
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> entry : pendingReturns.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(pendingFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save soulbound returns: " + e.getMessage());
        }
    }

    public void flushPendingReturns() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        savePendingReturns();
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
            scheduleSave();
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        List<ItemStack> items = pendingReturns.remove(playerId);
        if (items == null || items.isEmpty()) {
            return;
        }
        scheduleSave();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            deliverPendingItems(event.getPlayer(), items);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        List<ItemStack> items = pendingReturns.remove(playerId);
        if (items == null || items.isEmpty()) {
            return;
        }
        scheduleSave();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            deliverPendingItems(event.getPlayer(), items);
        });
    }

    private void deliverPendingItems(Player player, List<ItemStack> items) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        if (!leftovers.isEmpty()) {
            pendingReturns.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>())
                    .addAll(leftovers.values());
            scheduleSave();
            player.sendMessage("§e인벤토리가 가득 차서 일부 귀속 장비가 보관되었습니다.");
        } else {
            player.sendMessage("§a귀속 장비가 복구되었습니다.");
        }
    }

    private void scheduleSave() {
        if (saveTask != null) {
            return;
        }
        saveTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            savePendingReturns();
            saveTask = null;
        }, 20L);
    }
}
