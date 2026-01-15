package com.chzzk.rpg.cube;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.grade.BonusStat;
import com.chzzk.rpg.grade.BonusStatGenerator;
import com.chzzk.rpg.items.WeaponData;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CubeManager {
    private final ChzzkRPG plugin;

    // Stores previous stats for advanced cube restoration (player UUID -> previous stats)
    private final Map<UUID, List<BonusStat>> previousStats = new HashMap<>();
    private final Map<UUID, ItemStack> activeWeapons = new HashMap<>();

    public CubeManager(ChzzkRPG plugin) {
        this.plugin = plugin;
    }

    public enum CubeResult {
        SUCCESS,
        NO_STATS_TO_REROLL,
        NOT_OWNED,
        INSUFFICIENT_GOLD,
        INSUFFICIENT_CUBES
    }

    /**
     * Uses a cube to re-roll weapon bonus stats.
     * For advanced cubes, stores the previous stats for potential restoration.
     */
    public CubeResult useCube(Player player, ItemStack weapon, ItemStack cubeItem, CubeType cubeType) {
        WeaponData wd = new WeaponData(weapon);

        if (!wd.isOwnedBy(player.getUniqueId())) {
            player.sendMessage(Component.text("§c이 무기는 귀속되어 있습니다."));
            return CubeResult.NOT_OWNED;
        }

        int currentStatCount = wd.getBonusStatList().size();
        if (currentStatCount == 0) {
            player.sendMessage(Component.text("§c재설정할 추가 옵션이 없습니다."));
            return CubeResult.NO_STATS_TO_REROLL;
        }

        // Check gold cost (from config)
        long goldCost = getGoldCost(cubeType);
        if (!plugin.getVaultHook().hasMoney(player, goldCost)) {
            player.sendMessage(Component.text("§c골드가 부족합니다. (필요: " + goldCost + "원)"));
            return CubeResult.INSUFFICIENT_GOLD;
        }

        // Check cube amount
        if (cubeItem.getAmount() < 1) {
            player.sendMessage(Component.text("§c큐브가 부족합니다."));
            return CubeResult.INSUFFICIENT_CUBES;
        }

        // Store previous stats if advanced cube
        if (cubeType.isCanRestore()) {
            previousStats.put(player.getUniqueId(), new ArrayList<>(wd.getBonusStatList()));
            activeWeapons.put(player.getUniqueId(), weapon);
        }

        // Consume materials
        cubeItem.setAmount(cubeItem.getAmount() - 1);
        plugin.getVaultHook().withdraw(player, goldCost);

        // Re-roll all stats
        List<BonusStat> newStats = BonusStatGenerator.rerollAll(wd.getGrade(), currentStatCount);
        wd.setBonusStatList(newStats);
        wd.save();

        player.sendMessage(Component.text("§a옵션이 재설정되었습니다!"));
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);

        if (cubeType.isCanRestore()) {
            player.sendMessage(Component.text("§7고급 큐브를 사용하여 이전 옵션으로 복원할 수 있습니다."));
        }

        return CubeResult.SUCCESS;
    }

    /**
     * Restores the weapon to its previous stats (advanced cube only).
     * Can only be used once per cube usage.
     */
    public boolean restorePreviousStats(Player player, ItemStack weapon) {
        UUID uuid = player.getUniqueId();

        if (!previousStats.containsKey(uuid)) {
            player.sendMessage(Component.text("§c복원할 수 있는 옵션이 없습니다."));
            return false;
        }

        ItemStack storedWeapon = activeWeapons.get(uuid);
        if (storedWeapon == null || !storedWeapon.equals(weapon)) {
            player.sendMessage(Component.text("§c큐브를 사용한 무기가 아닙니다."));
            return false;
        }

        WeaponData wd = new WeaponData(weapon);
        wd.setBonusStatList(previousStats.get(uuid));
        wd.save();

        // Clear stored data (restoration is one-time only)
        previousStats.remove(uuid);
        activeWeapons.remove(uuid);

        player.sendMessage(Component.text("§a이전 옵션으로 복원되었습니다!"));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);

        return true;
    }

    /**
     * Confirms the new stats and clears the restoration option.
     */
    public void confirmNewStats(Player player) {
        UUID uuid = player.getUniqueId();
        previousStats.remove(uuid);
        activeWeapons.remove(uuid);
        player.sendMessage(Component.text("§a새로운 옵션이 확정되었습니다!"));
    }

    /**
     * Checks if the player can restore to previous stats.
     */
    public boolean canRestore(Player player) {
        return previousStats.containsKey(player.getUniqueId());
    }

    /**
     * Gets the stored previous stats for display.
     */
    public List<BonusStat> getPreviousStats(Player player) {
        return previousStats.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    /**
     * Clears restoration data for a player (called on logout, etc.)
     */
    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        previousStats.remove(uuid);
        activeWeapons.remove(uuid);
    }

    /**
     * Gets the gold cost for a cube type from config.
     */
    public long getGoldCost(CubeType cubeType) {
        return plugin.getConfig().getLong("cube.types." + cubeType.name() + ".gold-cost", cubeType.getGoldCost());
    }
}
