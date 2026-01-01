package com.chzzk.rpg.stats;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerProfile {
    private final UUID uuid;
    private String name;

    // Stats
    private PlayerStats baseStats; // From points/base
    private PlayerStats totalStats; // Calculated (Gear + Buffs + Jobs)

    private int statPoints;

    // Buffs
    private final java.util.List<com.chzzk.rpg.chef.Buff> activeBuffs = new java.util.ArrayList<>();

    // Jobs
    private com.chzzk.rpg.jobs.CombatJob combatJob;
    private com.chzzk.rpg.jobs.LifeJob lifeJob;

    // Life Job Levels (Could be separate object)
    private int blacksmithLevel;
    private double blacksmithExp;
    private int chefLevel;
    private double chefExp;
    private int builderLevel;
    private double builderExp;

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.baseStats = new PlayerStats();
        this.totalStats = new PlayerStats();
        this.statPoints = 0;

        this.combatJob = com.chzzk.rpg.jobs.CombatJob.NONE;
        this.lifeJob = com.chzzk.rpg.jobs.LifeJob.NONE;

        this.blacksmithLevel = 1;
        this.builderLevel = 1;
        this.chefLevel = 1;
    }

    public void addBuff(com.chzzk.rpg.chef.Buff buff) {
        activeBuffs.add(buff);
        recalculateStats();
    }

    public void cleanupBuffs() {
        if (activeBuffs.removeIf(com.chzzk.rpg.chef.Buff::isExpired)) {
            recalculateStats();
        }
    }

    public void recalculateStats() {
        // Copy base to total
        for (StatType type : StatType.values()) {
            totalStats.set(type, baseStats.get(type));
        }

        // Apply Job Bonuses
        combatJob.applyStats(totalStats);

        // Apply Buffs
        for (com.chzzk.rpg.chef.Buff buff : activeBuffs) {
            if (!buff.isExpired()) {
                PlayerStats bs = buff.getStats();
                for (StatType type : StatType.values()) {
                    totalStats.add(type, bs.get(type));
                }
            }
        }

        // TODO: Add Gear stats
    }
}
