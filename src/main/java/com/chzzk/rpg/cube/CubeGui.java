package com.chzzk.rpg.cube;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.grade.BonusStat;
import com.chzzk.rpg.gui.RpgGui;
import com.chzzk.rpg.items.WeaponData;
import com.chzzk.rpg.utils.ItemBuilder;
import com.chzzk.rpg.utils.RpgKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class CubeGui implements RpgGui {
    private final ChzzkRPG plugin;
    private final Inventory inventory;
    private final Player player;

    // Slot positions
    private static final int SLOT_INFO = 4;
    private static final int SLOT_PREV_LABEL = 10;
    private static final int[] SLOTS_PREV_STATS = {11, 12, 13, 14, 15};
    private static final int SLOT_VS = 22;
    private static final int SLOT_CURR_LABEL = 28;
    private static final int[] SLOTS_CURR_STATS = {29, 30, 31, 32, 33};
    private static final int SLOT_WEAPON = 38;
    private static final int SLOT_CUBE = 42;
    private static final int SLOT_EXECUTE = 48;
    private static final int SLOT_RESTORE = 50;
    private static final int SLOT_CONFIRM = 51;

    public CubeGui(Player player) {
        this.plugin = ChzzkRPG.getInstance();
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§0큐브");
        initialize();
    }

    private void initialize() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Clear interactive slots
        inventory.setItem(SLOT_WEAPON, null);
        inventory.setItem(SLOT_CUBE, null);

        updateInfoSlot();
        updateButtons();
        updateStatDisplay();
    }

    private void updateInfoSlot() {
        inventory.setItem(SLOT_INFO, new ItemBuilder(Material.BOOK)
            .name("§e큐브 사용 안내")
            .lore(
                "§7무기와 큐브를 넣고 실행하세요.",
                "",
                "§f일반 큐브: §7즉시 재설정",
                "§6고급 큐브: §7재설정 후 복원 가능",
                "",
                "§c주의: 큐브 사용 시 모든 옵션이",
                "§c새로운 옵션으로 변경됩니다."
            )
            .build());
    }

    private void updateButtons() {
        boolean canRestore = plugin.getCubeManager().canRestore(player);

        // Execute button
        inventory.setItem(SLOT_EXECUTE, new ItemBuilder(Material.ENDER_EYE)
            .name("§a큐브 실행")
            .lore("§7무기의 추가 옵션을 재설정합니다.")
            .build());

        // Restore button (only active if advanced cube was used)
        if (canRestore) {
            inventory.setItem(SLOT_RESTORE, new ItemBuilder(Material.CLOCK)
                .name("§e이전 옵션으로 복원")
                .lore(
                    "§7고급 큐브 사용 후 1회 복원 가능",
                    "",
                    "§c클릭 시 이전 옵션으로 되돌립니다."
                )
                .build());

            inventory.setItem(SLOT_CONFIRM, new ItemBuilder(Material.LIME_DYE)
                .name("§a새 옵션 확정")
                .lore(
                    "§7현재 옵션을 확정합니다.",
                    "",
                    "§c확정 후에는 복원할 수 없습니다."
                )
                .build());
        } else {
            inventory.setItem(SLOT_RESTORE, new ItemBuilder(Material.GRAY_DYE)
                .name("§7복원 불가")
                .lore("§7고급 큐브 사용 후 복원 가능합니다.")
                .build());

            inventory.setItem(SLOT_CONFIRM, new ItemBuilder(Material.GRAY_DYE)
                .name("§7확정할 옵션 없음")
                .build());
        }
    }

    private void updateStatDisplay() {
        ItemStack weapon = inventory.getItem(SLOT_WEAPON);
        boolean canRestore = plugin.getCubeManager().canRestore(player);

        // VS separator
        inventory.setItem(SLOT_VS, new ItemBuilder(Material.COMPARATOR)
            .name("§7▼ VS ▼")
            .build());

        // Previous stats label
        inventory.setItem(SLOT_PREV_LABEL, new ItemBuilder(canRestore ? Material.ORANGE_WOOL : Material.GRAY_WOOL)
            .name(canRestore ? "§6이전 옵션" : "§7이전 옵션 없음")
            .build());

        // Current stats label
        inventory.setItem(SLOT_CURR_LABEL, new ItemBuilder(Material.LIME_WOOL)
            .name("§a현재 옵션")
            .build());

        // Clear stat slots
        ItemStack emptySlot = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .name("§7-")
            .build();
        for (int slot : SLOTS_PREV_STATS) {
            inventory.setItem(slot, emptySlot);
        }
        for (int slot : SLOTS_CURR_STATS) {
            inventory.setItem(slot, emptySlot);
        }

        // Display previous stats if available
        if (canRestore) {
            List<BonusStat> prevStats = plugin.getCubeManager().getPreviousStats(player);
            for (int i = 0; i < prevStats.size() && i < SLOTS_PREV_STATS.length; i++) {
                BonusStat stat = prevStats.get(i);
                inventory.setItem(SLOTS_PREV_STATS[i], new ItemBuilder(Material.PAPER)
                    .name("§6" + stat.getDisplayString())
                    .lore("§7이전 옵션 #" + (i + 1))
                    .build());
            }
        }

        // Display current stats if weapon is present
        if (weapon != null && WeaponData.isWeapon(weapon)) {
            WeaponData wd = new WeaponData(weapon);
            List<BonusStat> currentStats = wd.getBonusStatList();
            for (int i = 0; i < currentStats.size() && i < SLOTS_CURR_STATS.length; i++) {
                BonusStat stat = currentStats.get(i);
                inventory.setItem(SLOTS_CURR_STATS[i], new ItemBuilder(Material.PAPER)
                    .name("§a" + stat.getDisplayString())
                    .lore("§7현재 옵션 #" + (i + 1))
                    .build());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        updateStatDisplay();
        updateButtons();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();

        // Allow interaction with player inventory (bottom)
        if (slot >= 54) {
            event.setCancelled(false);
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        // Allow item placement in weapon/cube slots
        if (slot == SLOT_WEAPON || slot == SLOT_CUBE) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateStatDisplay();
                updateButtons();
            }, 1L);
            return;
        }

        // Execute button
        if (slot == SLOT_EXECUTE) {
            handleExecute();
            return;
        }

        // Restore button
        if (slot == SLOT_RESTORE) {
            handleRestore();
            return;
        }

        // Confirm button
        if (slot == SLOT_CONFIRM) {
            handleConfirm();
        }
    }

    private void handleExecute() {
        ItemStack weapon = inventory.getItem(SLOT_WEAPON);
        ItemStack cube = inventory.getItem(SLOT_CUBE);

        if (weapon == null || !WeaponData.isWeapon(weapon)) {
            player.sendMessage("§c무기를 올려주세요.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        if (cube == null || !isCube(cube)) {
            player.sendMessage("§c큐브를 올려주세요.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        CubeType cubeType = getCubeType(cube);
        if (cubeType == null) {
            player.sendMessage("§c올바른 큐브가 아닙니다.");
            return;
        }

        CubeManager.CubeResult result = plugin.getCubeManager().useCube(player, weapon, cube, cubeType);

        if (result == CubeManager.CubeResult.SUCCESS) {
            // Update cube slot if all consumed
            if (cube.getAmount() <= 0) {
                inventory.setItem(SLOT_CUBE, null);
            }
        }

        updateStatDisplay();
        updateButtons();
    }

    private void handleRestore() {
        if (!plugin.getCubeManager().canRestore(player)) {
            player.sendMessage("§c복원할 수 있는 옵션이 없습니다.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        ItemStack weapon = inventory.getItem(SLOT_WEAPON);
        if (weapon == null) {
            player.sendMessage("§c무기를 올려주세요.");
            return;
        }

        plugin.getCubeManager().restorePreviousStats(player, weapon);
        updateStatDisplay();
        updateButtons();
    }

    private void handleConfirm() {
        if (!plugin.getCubeManager().canRestore(player)) {
            player.sendMessage("§c확정할 옵션이 없습니다.");
            return;
        }

        plugin.getCubeManager().confirmNewStats(player);
        updateStatDisplay();
        updateButtons();
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // Return items to player
        Player p = (Player) event.getPlayer();
        returnItem(p, inventory.getItem(SLOT_WEAPON));
        returnItem(p, inventory.getItem(SLOT_CUBE));
    }

    private void returnItem(Player p, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            if (p.getInventory().firstEmpty() == -1) {
                p.getWorld().dropItemNaturally(p.getLocation(), item);
            } else {
                p.getInventory().addItem(item);
            }
        }
    }

    private boolean isCube(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(RpgKeys.CUBE_TYPE, PersistentDataType.STRING);
    }

    private CubeType getCubeType(ItemStack item) {
        if (!isCube(item)) return null;
        String typeStr = item.getItemMeta().getPersistentDataContainer()
            .get(RpgKeys.CUBE_TYPE, PersistentDataType.STRING);
        try {
            return CubeType.valueOf(typeStr);
        } catch (Exception e) {
            return null;
        }
    }
}
