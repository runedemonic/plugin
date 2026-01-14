package com.chzzk.rpg.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 데미지 계산 공식 테스트
 *
 * 공식:
 * - effectiveDef = DEF * (1 - PEN/100)
 * - baseDamage = max(1.0, totalAtk - max(0, effectiveDef))
 * - finalDamage = baseDamage * critDmg (크리티컬 시)
 */
@DisplayName("데미지 계산 테스트")
class DamageCalculationTest {

    /**
     * 데미지 계산 로직 (DamageListener에서 추출)
     */
    private double calculateDamage(double playerAtk, double weaponAtk, double def, double pen, boolean isCrit, double critDmg) {
        // Penetration 제한 (0-100%)
        pen = Math.max(0.0, Math.min(100.0, pen));

        // Total Attack
        double totalAtk = playerAtk + weaponAtk;

        // Effective Defense (관통력 적용)
        double effectiveDef = def * (1 - (pen / 100.0));

        // Base Damage (최소 1.0)
        double baseDamage = Math.max(1.0, totalAtk - Math.max(0, effectiveDef));

        // Critical Damage
        double finalDamage = baseDamage;
        if (isCrit) {
            finalDamage *= Math.max(1.0, critDmg);
        }

        return finalDamage;
    }

    @Test
    @DisplayName("기본 데미지 계산 - 방어력 없음")
    void testBasicDamageNoDefense() {
        double damage = calculateDamage(100, 50, 0, 0, false, 1.5);
        assertEquals(150.0, damage, "ATK 100 + 무기 50 = 150 데미지");
    }

    @Test
    @DisplayName("기본 데미지 계산 - 방어력 있음")
    void testBasicDamageWithDefense() {
        double damage = calculateDamage(100, 50, 30, 0, false, 1.5);
        assertEquals(120.0, damage, "ATK 150 - DEF 30 = 120 데미지");
    }

    @Test
    @DisplayName("관통력 적용 테스트 - 50% 관통")
    void testPenetration() {
        // DEF 100, PEN 50% -> effectiveDef = 100 * 0.5 = 50
        // damage = 150 - 50 = 100
        double damage = calculateDamage(100, 50, 100, 50, false, 1.5);
        assertEquals(100.0, damage, "50% 관통으로 방어력 50만 적용");
    }

    @Test
    @DisplayName("관통력 적용 테스트 - 100% 관통")
    void testFullPenetration() {
        // DEF 100, PEN 100% -> effectiveDef = 0
        double damage = calculateDamage(100, 50, 100, 100, false, 1.5);
        assertEquals(150.0, damage, "100% 관통으로 방어력 무시");
    }

    @Test
    @DisplayName("크리티컬 데미지 계산")
    void testCriticalDamage() {
        // 기본 데미지 150, 크리티컬 배율 1.5
        double damage = calculateDamage(100, 50, 0, 0, true, 1.5);
        assertEquals(225.0, damage, "150 * 1.5 = 225 크리티컬 데미지");
    }

    @Test
    @DisplayName("크리티컬 + 방어력 + 관통력 복합 계산")
    void testComplexDamageCalculation() {
        // ATK 100, 무기 50, DEF 80, PEN 25%
        // effectiveDef = 80 * 0.75 = 60
        // baseDamage = 150 - 60 = 90
        // critDamage = 90 * 2.0 = 180
        double damage = calculateDamage(100, 50, 80, 25, true, 2.0);
        assertEquals(180.0, damage);
    }

    @Test
    @DisplayName("최소 데미지 보장 (1.0)")
    void testMinimumDamage() {
        // ATK 10, DEF 1000 -> 여전히 최소 1.0 데미지
        double damage = calculateDamage(10, 0, 1000, 0, false, 1.5);
        assertEquals(1.0, damage, "최소 데미지는 1.0");
    }

    @Test
    @DisplayName("관통력 범위 제한 테스트 - 음수")
    void testPenetrationLowerBound() {
        // PEN -50 -> 0으로 제한
        double damage = calculateDamage(100, 0, 50, -50, false, 1.5);
        assertEquals(50.0, damage, "음수 관통력은 0으로 처리");
    }

    @Test
    @DisplayName("관통력 범위 제한 테스트 - 100 초과")
    void testPenetrationUpperBound() {
        // PEN 150 -> 100으로 제한
        double damage = calculateDamage(100, 0, 50, 150, false, 1.5);
        assertEquals(100.0, damage, "100 초과 관통력은 100으로 처리");
    }

    @ParameterizedTest
    @DisplayName("다양한 데미지 시나리오 테스트")
    @CsvSource({
        "100, 0, 0, 0, false, 1.5, 100.0",      // 기본
        "100, 50, 0, 0, false, 1.5, 150.0",     // 무기 추가
        "100, 50, 50, 0, false, 1.5, 100.0",    // 방어력
        "100, 50, 100, 50, false, 1.5, 100.0",  // 관통력
        "100, 50, 0, 0, true, 2.0, 300.0",      // 크리티컬
        "50, 25, 200, 0, false, 1.5, 1.0",      // 최소 데미지
    })
    void testDamageScenarios(double playerAtk, double weaponAtk, double def,
                             double pen, boolean isCrit, double critDmg, double expected) {
        double damage = calculateDamage(playerAtk, weaponAtk, def, pen, isCrit, critDmg);
        assertEquals(expected, damage, 0.001);
    }

    @Test
    @DisplayName("크리티컬 배율 최소값 보장 (1.0)")
    void testMinimumCritMultiplier() {
        // critDmg 0.5 -> 1.0으로 제한
        double damage = calculateDamage(100, 0, 0, 0, true, 0.5);
        assertEquals(100.0, damage, "크리티컬 배율 최소값은 1.0");
    }
}
