package com.chzzk.rpg.enhance;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.items.WeaponData;
import java.util.Random;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EnhanceManager {

    private final ChzzkRPG plugin;
    private final Random random = new Random();

    public EnhanceManager(ChzzkRPG plugin) {
        this.plugin = plugin;
    }

    public enum EnhanceResult {
        SUCCESS,
        FAIL, // Just consume scroll
        DOWNGRADE, // -1 Level
        DESTROY // Item gone
    }

    // Overload for backward compatibility or direct usage
    public EnhanceResult calculateResult(int currentLevel) {
        return calculateResult(currentLevel, 0.0);
    }

    public EnhanceResult calculateResult(int currentLevel, double bonusRate) {
        // Simple Logic:
        // 0->1: 100%
        // 1->2: 90%
        // ...
        // 9->10: 10%

        if (currentLevel >= 10)
            return EnhanceResult.FAIL; // Max level

        double successRate = Math.max(10, 100 - (currentLevel * 10)); // 100, 90, 80 ... 10
        successRate += bonusRate; // Apply Bonus

        double roll = random.nextDouble() * 100;

        if (roll < successRate) {
            return EnhanceResult.SUCCESS;
        } else {
            // Fail Scenarios
            // If level > 5, chance to downgrade or destroy
            if (currentLevel >= 5) {
                double penaltyRoll = random.nextDouble() * 100;
                if (penaltyRoll < 10)
                    return EnhanceResult.DESTROY; // 10% chance to destroy on fail
                if (penaltyRoll < 40)
                    return EnhanceResult.DOWNGRADE; // 30% chance to downgrade
            }
            return EnhanceResult.FAIL;
        }
    }

    public void enhance(Player player, ItemStack weapon, ItemStack scroll) {
        if (!WeaponData.isWeapon(weapon)) {
            player.sendMessage(Component.text("§c강화할 수 없는 아이템입니다."));
            return;
        }

        WeaponData wd = new WeaponData(weapon);
        int currentLevel = wd.getEnhanceLevel();

        // Calculate Bonus from Job
        double bonus = 0.0;
        com.chzzk.rpg.stats.PlayerProfile profile = plugin.getStatsManager().getProfile(player);
        if (profile != null && profile.getLifeJob() == com.chzzk.rpg.jobs.LifeJob.BLACKSMITH) {
            bonus = 5.0 + (profile.getBlacksmithLevel() * 0.5); // Base 5% + 0.5% per level
            // NOTE: Need to verify if getBlacksmithLevel() is accessible/correct
        }

        EnhanceResult result = calculateResult(currentLevel, bonus);

        // Consume Scroll
        scroll.setAmount(scroll.getAmount() - 1);

        switch (result) {
            case SUCCESS:
                wd.setEnhanceLevel(currentLevel + 1);
                wd.save();
                player.sendMessage(Component.text("§a강화 성공! §e(+" + (currentLevel + 1) + ")"));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 2f);
                break;
            case FAIL:
                player.sendMessage(Component.text("§c강화 실패..."));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
                break;
            case DOWNGRADE:
                wd.setEnhanceLevel(Math.max(0, currentLevel - 1));
                wd.save();
                player.sendMessage(Component.text("§c강화 실패! 등급이 하락했습니다. §e(+" + (Math.max(0, currentLevel - 1)) + ")"));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                break;
            case DESTROY:
                weapon.setAmount(0); // Destroy item
                player.sendMessage(Component.text("§c강화 대실패! 아이템이 파괴되었습니다."));
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                break;
        }
    }
}
