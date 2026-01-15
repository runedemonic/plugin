package com.chzzk.rpg.grade;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.items.WeaponData;
import com.chzzk.rpg.items.WeaponType;
import com.chzzk.rpg.jobs.LifeJob;
import com.chzzk.rpg.stats.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class GradeManager {
    private final ChzzkRPG plugin;
    private final Random random = new Random();

    public GradeManager(ChzzkRPG plugin) {
        this.plugin = plugin;
    }

    public enum UpgradeResult {
        SUCCESS,
        FAIL,
        NOT_BLACKSMITH,
        INSUFFICIENT_MATERIALS,
        INSUFFICIENT_GOLD,
        MAX_GRADE,
        NOT_OWNED
    }

    public UpgradeResult attemptUpgrade(Player player, ItemStack weapon, ItemStack gradeStone) {
        PlayerProfile profile = plugin.getStatsManager().getProfile(player);
        if (profile == null || profile.getLifeJob() != LifeJob.BLACKSMITH) {
            player.sendMessage(Component.text("§c등급 승급은 대장장이 직업만 가능합니다."));
            return UpgradeResult.NOT_BLACKSMITH;
        }

        WeaponData wd = new WeaponData(weapon);

        if (!wd.isOwnedBy(player.getUniqueId())) {
            player.sendMessage(Component.text("§c이 무기는 귀속되어 있습니다."));
            return UpgradeResult.NOT_OWNED;
        }

        WeaponGrade currentGrade = wd.getGrade();
        WeaponGrade nextGrade = currentGrade.getNextGrade();

        if (nextGrade == null) {
            player.sendMessage(Component.text("§c이미 최고 등급입니다."));
            return UpgradeResult.MAX_GRADE;
        }

        int stoneCost = getStoneCost(currentGrade);
        if (gradeStone.getAmount() < stoneCost) {
            player.sendMessage(Component.text("§c등급석이 부족합니다. (필요: " + stoneCost + "개)"));
            return UpgradeResult.INSUFFICIENT_MATERIALS;
        }

        long goldCost = getGoldCost(currentGrade);
        if (!plugin.getVaultHook().hasMoney(player, goldCost)) {
            player.sendMessage(Component.text("§c골드가 부족합니다. (필요: " + goldCost + "원)"));
            return UpgradeResult.INSUFFICIENT_GOLD;
        }

        // Consume materials
        gradeStone.setAmount(gradeStone.getAmount() - stoneCost);
        plugin.getVaultHook().withdraw(player, goldCost);

        // Calculate success rate
        double baseRate = getSuccessRateConfig(currentGrade);
        double blacksmithBonus = getBlacksmithBonus(profile.getBlacksmithLevel());
        double finalRate = Math.min(100.0, baseRate + blacksmithBonus);

        double roll = random.nextDouble() * 100;

        if (roll < finalRate) {
            // SUCCESS
            wd.setGrade(nextGrade);

            // Increase base ATK based on weapon type and new grade
            double newBaseAtk = getBaseAtkForGrade(wd.getWeaponType(), nextGrade);
            wd.setBaseAtk(newBaseAtk);

            // Generate new bonus stats
            List<BonusStat> newStats = BonusStatGenerator.generateForGrade(nextGrade, wd.getBonusStatList());
            wd.setBonusStatList(newStats);
            wd.save();

            player.sendMessage(Component.text("§a등급 승급 성공! " +
                currentGrade.getColoredName() + " §a-> " + nextGrade.getColoredName()));
            player.sendMessage(Component.text("§7기본 공격력: §c" + String.format("%.1f", newBaseAtk)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

            return UpgradeResult.SUCCESS;
        } else {
            // FAIL - No downgrade, just fail
            player.sendMessage(Component.text("§c등급 승급 실패..."));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);

            return UpgradeResult.FAIL;
        }
    }

    // Config-based getters
    private double getSuccessRateConfig(WeaponGrade grade) {
        return plugin.getConfig().getDouble("grade.success-rates." + grade.name(), 50.0);
    }

    private double getBlacksmithBonus(int level) {
        double base = plugin.getConfig().getDouble("grade.blacksmith-bonus.base", 5.0);
        double perLevel = plugin.getConfig().getDouble("grade.blacksmith-bonus.per-level", 0.5);
        return base + (level * perLevel);
    }

    public int getStoneCost(WeaponGrade grade) {
        return plugin.getConfig().getInt("grade.stone-costs." + grade.name(), 1);
    }

    public long getGoldCost(WeaponGrade grade) {
        return plugin.getConfig().getLong("grade.gold-costs." + grade.name(), 1000L);
    }

    public double getBaseAtkForGrade(WeaponType type, WeaponGrade grade) {
        String path = "weapon-base-atk." + type.name() + "." + grade.name();
        // Default values based on weapon type
        double defaultAtk = switch (type) {
            case SWORD -> 10.0 * grade.getStatMultiplier();
            case AXE -> 12.0 * grade.getStatMultiplier();
            case RANGED -> 8.0 * grade.getStatMultiplier();
            case STAFF -> 9.0 * grade.getStatMultiplier();
            default -> 5.0 * grade.getStatMultiplier();
        };
        return plugin.getConfig().getDouble(path, defaultAtk);
    }

    public double getSuccessRate(WeaponGrade currentGrade, int blacksmithLevel) {
        double baseRate = getSuccessRateConfig(currentGrade);
        double bonus = getBlacksmithBonus(blacksmithLevel);
        return Math.min(100.0, baseRate + bonus);
    }
}
