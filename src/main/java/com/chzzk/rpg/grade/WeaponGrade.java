package com.chzzk.rpg.grade;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum WeaponGrade {
    COMMON(0, "일반", "§f", 0, 1.0),
    UNCOMMON(1, "고급", "§a", 1, 1.2),
    RARE(2, "희귀", "§9", 2, 1.5),
    EPIC(3, "영웅", "§5", 3, 2.0),
    LEGENDARY(4, "전설", "§6", 4, 2.5),
    MYTHIC(5, "신화", "§c", 5, 3.0);

    private final int tier;
    private final String displayName;
    private final String colorCode;
    private final int bonusStatSlots;
    private final double statMultiplier;

    public WeaponGrade getNextGrade() {
        int nextTier = this.tier + 1;
        for (WeaponGrade g : values()) {
            if (g.tier == nextTier) return g;
        }
        return null;
    }

    public static WeaponGrade fromTier(int tier) {
        for (WeaponGrade g : values()) {
            if (g.tier == tier) return g;
        }
        return COMMON;
    }

    public String getColoredName() {
        return colorCode + displayName;
    }
}
