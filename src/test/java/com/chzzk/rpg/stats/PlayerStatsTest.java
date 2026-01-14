package com.chzzk.rpg.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerStats 테스트")
class PlayerStatsTest {

    private PlayerStats stats;

    @BeforeEach
    void setUp() {
        stats = new PlayerStats();
    }

    @Test
    @DisplayName("기본 스탯 초기화 - 모든 스탯이 0으로 초기화되어야 함")
    void testDefaultInitialization() {
        assertEquals(0.0, stats.get(StatType.ATK), "ATK 기본값은 0이어야 함");
        assertEquals(0.0, stats.get(StatType.DEF), "DEF 기본값은 0이어야 함");
        assertEquals(0.0, stats.get(StatType.HP), "HP 기본값은 0이어야 함");
        assertEquals(0.0, stats.get(StatType.CRIT), "CRIT 기본값은 0이어야 함");
        assertEquals(0.0, stats.get(StatType.PEN), "PEN 기본값은 0이어야 함");
    }

    @Test
    @DisplayName("CRIT_DMG 기본값 - 1.5(150%)로 초기화되어야 함")
    void testCritDmgDefault() {
        assertEquals(1.5, stats.get(StatType.CRIT_DMG), "CRIT_DMG 기본값은 1.5여야 함");
    }

    @Test
    @DisplayName("스탯 설정 테스트")
    void testSetStat() {
        stats.set(StatType.ATK, 100.0);
        assertEquals(100.0, stats.get(StatType.ATK));

        stats.set(StatType.DEF, 50.0);
        assertEquals(50.0, stats.get(StatType.DEF));
    }

    @Test
    @DisplayName("스탯 추가 테스트")
    void testAddStat() {
        stats.set(StatType.ATK, 100.0);
        stats.add(StatType.ATK, 25.0);
        assertEquals(125.0, stats.get(StatType.ATK));
    }

    @Test
    @DisplayName("스탯 누적 추가 테스트")
    void testCumulativeAdd() {
        stats.add(StatType.HP, 100.0);
        stats.add(StatType.HP, 50.0);
        stats.add(StatType.HP, 25.0);
        assertEquals(175.0, stats.get(StatType.HP));
    }

    @Test
    @DisplayName("음수 스탯 추가 테스트")
    void testNegativeAdd() {
        stats.set(StatType.DEF, 100.0);
        stats.add(StatType.DEF, -30.0);
        assertEquals(70.0, stats.get(StatType.DEF));
    }

    @Test
    @DisplayName("모든 StatType이 정상적으로 존재하는지 확인")
    void testAllStatTypesExist() {
        StatType[] expectedTypes = {
            StatType.ATK, StatType.DEF, StatType.HP,
            StatType.CRIT, StatType.CRIT_DMG, StatType.PEN
        };

        assertEquals(6, StatType.values().length, "StatType은 6개여야 함");

        for (StatType type : expectedTypes) {
            assertNotNull(stats.get(type), type.name() + "는 null이 아니어야 함");
        }
    }

    @Test
    @DisplayName("StatType displayName 확인")
    void testStatTypeDisplayNames() {
        assertEquals("공격력", StatType.ATK.getDisplayName());
        assertEquals("방어력", StatType.DEF.getDisplayName());
        assertEquals("최대 체력", StatType.HP.getDisplayName());
        assertEquals("치명타 확률", StatType.CRIT.getDisplayName());
        assertEquals("치명타 데미지", StatType.CRIT_DMG.getDisplayName());
        assertEquals("관통력", StatType.PEN.getDisplayName());
    }
}
