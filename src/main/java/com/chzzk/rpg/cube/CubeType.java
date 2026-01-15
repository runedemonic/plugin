package com.chzzk.rpg.cube;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CubeType {
    BASIC("일반 큐브", "§f", false, 500L),
    ADVANCED("고급 큐브", "§6", true, 2000L);

    private final String displayName;
    private final String colorCode;
    private final boolean canRestore;
    private final long goldCost;

    public String getColoredName() {
        return colorCode + displayName;
    }
}
