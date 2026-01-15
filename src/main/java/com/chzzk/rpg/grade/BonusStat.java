package com.chzzk.rpg.grade;

import com.chzzk.rpg.stats.StatType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BonusStat {
    private final StatType type;
    private final double value;

    public String toStorageString() {
        return type.name() + ":" + value;
    }

    public static BonusStat fromStorageString(String str) {
        String[] parts = str.split(":");
        return new BonusStat(StatType.valueOf(parts[0]), Double.parseDouble(parts[1]));
    }

    public String getDisplayString() {
        String prefix = value >= 0 ? "+" : "";
        if (type == StatType.CRIT_DMG) {
            return type.getDisplayName() + " " + prefix + String.format("%.0f%%", value * 100);
        }
        if (type == StatType.CRIT || type == StatType.PEN) {
            return type.getDisplayName() + " " + prefix + String.format("%.1f%%", value);
        }
        return type.getDisplayName() + " " + prefix + String.format("%.1f", value);
    }
}
