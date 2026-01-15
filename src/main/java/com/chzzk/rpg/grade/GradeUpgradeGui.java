package com.chzzk.rpg.grade;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.gui.RpgGui;
import com.chzzk.rpg.items.WeaponData;
import com.chzzk.rpg.jobs.LifeJob;
import com.chzzk.rpg.stats.PlayerProfile;
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

import java.util.ArrayList;
import java.util.List;

public class GradeUpgradeGui implements RpgGui {
    private final ChzzkRPG plugin;
    private final Inventory inventory;

    private static final int SLOT_WEAPON = 10;
    private static final int SLOT_PREVIEW = 13;
    private static final int SLOT_STONE = 16;
    private static final int SLOT_BUTTON = 22;
    private static final int SLOT_INFO = 4;

    public GradeUpgradeGui() {
        this.plugin = ChzzkRPG.getInstance();
        this.inventory = Bukkit.createInventory(this, 27, "§0등급 승급");
        initialize();
    }

    private void initialize() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            if (i == SLOT_WEAPON || i == SLOT_STONE || i == SLOT_BUTTON ||
                i == SLOT_PREVIEW || i == SLOT_INFO) continue;
            inventory.setItem(i, filler);
        }

        // Arrows
        inventory.setItem(11, new ItemBuilder(Material.ARROW).name("§7→").build());
        inventory.setItem(12, new ItemBuilder(Material.ARROW).name("§7→").build());
        inventory.setItem(14, new ItemBuilder(Material.ARROW).name("§7←").build());
        inventory.setItem(15, new ItemBuilder(Material.ARROW).name("§7←").build());

        updateInfoSlot();
        updateButton();
        updatePreview();
    }

    private void updateInfoSlot() {
        inventory.setItem(SLOT_INFO, new ItemBuilder(Material.BOOK)
            .name("§e등급 승급 안내")
            .lore(
                "§7무기를 넣고 등급석을 소모하여",
                "§7등급을 승급할 수 있습니다.",
                "",
                "§7실패해도 등급은 하락하지 않습니다.",
                "",
                "§a대장장이 직업만 사용 가능합니다."
            )
            .build());
    }

    private void updateButton() {
        inventory.setItem(SLOT_BUTTON, new ItemBuilder(Material.SMITHING_TABLE)
            .name("§a등급 승급하기")
            .lore("§7무기와 등급석을 넣고 클릭하세요.")
            .build());
    }

    private void updatePreview() {
        ItemStack weapon = inventory.getItem(SLOT_WEAPON);
        if (weapon == null || !WeaponData.isWeapon(weapon)) {
            inventory.setItem(SLOT_PREVIEW, new ItemBuilder(Material.BARRIER)
                .name("§c무기를 넣어주세요")
                .build());
            return;
        }

        WeaponData wd = new WeaponData(weapon);
        WeaponGrade current = wd.getGrade();
        WeaponGrade next = current.getNextGrade();

        List<String> lore = new ArrayList<>();
        lore.add("§7현재 등급: " + current.getColoredName());

        if (next != null) {
            lore.add("§7목표 등급: " + next.getColoredName());
            lore.add("");
            lore.add("§7필요 등급석: §e" + plugin.getGradeManager().getStoneCost(current) + "개");
            lore.add("§7필요 골드: §e" + plugin.getGradeManager().getGoldCost(current) + "원");
            lore.add("");
            int newSlots = next.getBonusStatSlots() - current.getBonusStatSlots();
            if (newSlots > 0) {
                lore.add("§7새로 해금되는 옵션: §a+" + newSlots + "개");
            }
        } else {
            lore.add("");
            lore.add("§c이미 최고 등급입니다.");
        }

        inventory.setItem(SLOT_PREVIEW, new ItemBuilder(Material.NETHER_STAR)
            .name("§e승급 정보")
            .lore(lore.toArray(new String[0]))
            .build());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        updatePreview();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot >= 27) {
            event.setCancelled(false);
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        if (slot == SLOT_WEAPON || slot == SLOT_STONE) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(plugin, this::updatePreview, 1L);
            return;
        }

        if (slot == SLOT_BUTTON) {
            ItemStack weapon = inventory.getItem(SLOT_WEAPON);
            ItemStack stone = inventory.getItem(SLOT_STONE);

            if (weapon == null || !WeaponData.isWeapon(weapon)) {
                player.sendMessage("§c무기를 올려주세요.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                return;
            }

            if (stone == null || !isGradeStone(stone)) {
                player.sendMessage("§c등급석을 올려주세요.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                return;
            }

            PlayerProfile profile = plugin.getStatsManager().getProfile(player);
            if (profile == null || profile.getLifeJob() != LifeJob.BLACKSMITH) {
                player.sendMessage("§c대장장이 직업만 사용할 수 있습니다.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                return;
            }

            plugin.getGradeManager().attemptUpgrade(player, weapon, stone);

            if (stone.getAmount() <= 0) inventory.setItem(SLOT_STONE, null);

            updatePreview();
        }
    }

    private boolean isGradeStone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(RpgKeys.GRADE_STONE, PersistentDataType.STRING);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        returnItem(player, inventory.getItem(SLOT_WEAPON));
        returnItem(player, inventory.getItem(SLOT_STONE));
    }

    private void returnItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }
}
