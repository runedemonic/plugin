package com.chzzk.rpg.jobs;

import com.chzzk.rpg.stats.PlayerStats;
import com.chzzk.rpg.stats.StatType;
import lombok.Getter;

@Getter
public enum CombatJob {
    NONE("무직", 1.0, 1.0),
    WARRIOR("전사", 1.2, 1.1), // Atk 1.2x, Def 1.1x
    ARCHER("궁수", 1.1, 0.9), // Atk 1.1x, Def 0.9x
    MAGE("마법사", 1.3, 0.8), // Atk 1.3x, Def 0.8x
    ROGUE("도적", 1.1, 0.8); // Atk 1.1x, Crit focused (handled logic elsewhere)

    private final String displayName;
    private final double atkMultiplier;
    private final double defMultiplier;

    CombatJob(String displayName, double atkMultiplier, double defMultiplier) {
        this.displayName = displayName;
        this.atkMultiplier = atkMultiplier;
        this.defMultiplier = defMultiplier;
    }

    public void applyStats(PlayerStats currentStats) {
        double atk = currentStats.get(StatType.ATK);
        double def = currentStats.get(StatType.DEF);

        currentStats.set(StatType.ATK, atk * atkMultiplier);
        currentStats.set(StatType.DEF, def * defMultiplier);

        if (this == ROGUE) {
            currentStats.add(StatType.CRIT, 10.0); // +10% Crit Rate
            currentStats.add(StatType.CRIT_DMG, 0.5); // +50% Crit Dmg
        }
    }
}
