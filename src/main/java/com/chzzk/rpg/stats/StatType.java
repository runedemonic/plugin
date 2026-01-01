package com.chzzk.rpg.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StatType {
    ATK("공격력"),
    DEF("방어력"),
    HP("최대 체력"),
    CRIT("치명타 확률"),
    CRIT_DMG("치명타 데미지"),
    PEN("관통력");

    private final String displayName;
}
