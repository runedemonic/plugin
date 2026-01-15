package com.chzzk.rpg.cube;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("큐브 시스템 테스트")
class CubeSystemTest {

    @Nested
    @DisplayName("CubeType 테스트")
    class CubeTypeTest {

        @Test
        @DisplayName("2종류의 큐브가 존재해야 함")
        void testCubeTypeCount() {
            assertEquals(2, CubeType.values().length, "큐브는 2종류여야 함");
        }

        @Test
        @DisplayName("일반 큐브 속성 확인")
        void testBasicCubeProperties() {
            CubeType basic = CubeType.BASIC;

            assertEquals("일반 큐브", basic.getDisplayName());
            assertFalse(basic.isCanRestore(), "일반 큐브는 복원 불가");
            assertTrue(basic.getGoldCost() > 0, "골드 비용이 있어야 함");
        }

        @Test
        @DisplayName("고급 큐브 속성 확인")
        void testAdvancedCubeProperties() {
            CubeType advanced = CubeType.ADVANCED;

            assertEquals("고급 큐브", advanced.getDisplayName());
            assertTrue(advanced.isCanRestore(), "고급 큐브는 복원 가능");
            assertTrue(advanced.getGoldCost() > 0, "골드 비용이 있어야 함");
        }

        @Test
        @DisplayName("고급 큐브가 일반 큐브보다 비싸야 함")
        void testAdvancedCostsMore() {
            assertTrue(CubeType.ADVANCED.getGoldCost() > CubeType.BASIC.getGoldCost(),
                "고급 큐브(" + CubeType.ADVANCED.getGoldCost() + ")가 " +
                "일반 큐브(" + CubeType.BASIC.getGoldCost() + ")보다 비싸야 함");
        }

        @Test
        @DisplayName("색상 코드가 포함된 이름 테스트")
        void testColoredName() {
            for (CubeType type : CubeType.values()) {
                String coloredName = type.getColoredName();
                assertTrue(coloredName.startsWith("§"), "색상 코드로 시작해야 함");
                assertTrue(coloredName.contains(type.getDisplayName()), "이름이 포함되어야 함");
            }
        }

        @Test
        @DisplayName("일반 큐브와 고급 큐브의 색상이 달라야 함")
        void testDifferentColors() {
            assertNotEquals(
                CubeType.BASIC.getColorCode(),
                CubeType.ADVANCED.getColorCode(),
                "일반과 고급 큐브의 색상 코드가 달라야 함"
            );
        }

        @Test
        @DisplayName("valueOf로 큐브 타입 조회")
        void testValueOf() {
            assertEquals(CubeType.BASIC, CubeType.valueOf("BASIC"));
            assertEquals(CubeType.ADVANCED, CubeType.valueOf("ADVANCED"));
        }

        @Test
        @DisplayName("잘못된 이름으로 valueOf 시 예외 발생")
        void testValueOfInvalid() {
            assertThrows(IllegalArgumentException.class, () -> {
                CubeType.valueOf("INVALID_CUBE");
            });
        }
    }

    @Nested
    @DisplayName("CubeManager.CubeResult 테스트")
    class CubeResultTest {

        @Test
        @DisplayName("모든 결과 타입이 존재해야 함")
        void testAllResultTypesExist() {
            CubeManager.CubeResult[] results = CubeManager.CubeResult.values();

            assertTrue(results.length >= 4, "최소 4개 이상의 결과 타입");

            // 필수 결과 타입 확인
            assertNotNull(CubeManager.CubeResult.SUCCESS);
            assertNotNull(CubeManager.CubeResult.NO_STATS_TO_REROLL);
            assertNotNull(CubeManager.CubeResult.NOT_OWNED);
            assertNotNull(CubeManager.CubeResult.INSUFFICIENT_GOLD);
        }
    }
}
