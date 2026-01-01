package com.chzzk.rpg.gui;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.stats.PlayerProfile;
import com.chzzk.rpg.stats.PlayerStats;
import com.chzzk.rpg.stats.StatType;
import com.chzzk.rpg.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

public class StatsGui implements RpgGui {

    private final Player player;
    private final Inventory inventory;
    private final PlayerProfile profile;

    public StatsGui(Player player) {
        this.player = player;
        this.profile = ChzzkRPG.getInstance().getStatsManager().getProfile(player);
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Player Stats"));
        update();
    }

    public void update() {
        inventory.clear();

        // Info Item
        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name("§e" + player.getName())
                .lore(
                        "§7직업: " + profile.getCombatJob() + " / " + profile.getLifeJob(),
                        "§7스텟 포인트: §6" + profile.getStatPoints())
                .build());

        // Stats
        int[] slots = { 10, 19, 28, 37, 16, 25 }; // Layout positions
        StatType[] types = StatType.values();

        for (int i = 0; i < types.length && i < slots.length; i++) {
            StatType type = types[i];
            double val = profile.getTotalStats().get(type);
            double base = profile.getBaseStats().get(type);

            inventory.setItem(slots[i], new ItemBuilder(Material.PAPER)
                    .name("§b" + type.getDisplayName())
                    .lore(
                            "§f현재 수치: §a" + String.format("%.1f", val),
                            "§7(기본: " + String.format("%.1f", base) + ")",
                            "",
                            "§e클릭하여 포인트 투자")
                    .build());

            // Plus Button
            if (profile.getStatPoints() > 0) {
                inventory.setItem(slots[i] + 1, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                        .name("§a+1 " + type.getDisplayName())
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
        // Sound or effect
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null)
            return;

        int slot = event.getSlot();
        // Check if plus button
        // Simple logic: check if slot-1 is a stat slot

        int[] slots = { 10, 19, 28, 37, 16, 25 };
        StatType[] types = StatType.values();

        for (int i = 0; i < types.length && i < slots.length; i++) {
            if (slot == slots[i] + 1) {
                // Add point
                if (profile.getStatPoints() > 0) {
                    profile.setStatPoints(profile.getStatPoints() - 1);
                    profile.getBaseStats().add(types[i], getStatIncreaseAmount(types[i]));
                    profile.recalculateStats();
                    ChzzkRPG.getInstance().getStatsManager().saveProfile(player.getUniqueId()); // Save async usually
                    update();
                }
                return;
            }
        }
    }

    private double getStatIncreaseAmount(StatType type) {
        switch (type) {
            case HP:
                return 5.0;
            case CRIT:
                return 1.0; // 1%
            case CRIT_DMG:
                return 0.05; // 5%
            default:
                return 1.0;
        }
    }

    public void open() {
        player.openInventory(inventory);
    }
}
