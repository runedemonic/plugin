package com.chzzk.rpg.stats;

import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;

public class PlayerStats {
    @Getter
    private final Map<StatType, Double> stats = new EnumMap<>(StatType.class);

    public PlayerStats() {
        for (StatType type : StatType.values()) {
            stats.put(type, 0.0);
        }
        // Defaults
        stats.put(StatType.CRIT_DMG, 1.5); // 150% base
    }

    public double get(StatType type) {
        return stats.getOrDefault(type, 0.0);
    }

    public void set(StatType type, double value) {
        stats.put(type, value);
    }

    public void add(StatType type, double value) {
        stats.put(type, get(type) + value);
    }
}
