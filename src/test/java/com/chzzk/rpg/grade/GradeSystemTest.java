package com.chzzk.rpg.grade;

import com.chzzk.rpg.stats.StatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("등급 시스템 테스트")
class GradeSystemTest {

    @Nested
    @DisplayName("WeaponGrade 테스트")
    class WeaponGradeTest {

        @Test
        @DisplayName("6단계 등급이 존재해야 함")
        void testGradeCount() {
            assertEquals(6, WeaponGrade.values().length, "등급은 6단계여야 함");
        }

        @Test
        @DisplayName("등급 순서 확인 (tier 기준)")
        void testGradeOrder() {
            assertEquals(0, WeaponGrade.COMMON.getTier());
            assertEquals(1, WeaponGrade.UNCOMMON.getTier());
            assertEquals(2, WeaponGrade.RARE.getTier());
            assertEquals(3, WeaponGrade.EPIC.getTier());
            assertEquals(4, WeaponGrade.LEGENDARY.getTier());
            assertEquals(5, WeaponGrade.MYTHIC.getTier());
        }

        @Test
        @DisplayName("등급별 한글 이름 확인")
        void testGradeDisplayNames() {
            assertEquals("일반", WeaponGrade.COMMON.getDisplayName());
            assertEquals("고급", WeaponGrade.UNCOMMON.getDisplayName());
            assertEquals("희귀", WeaponGrade.RARE.getDisplayName());
            assertEquals("영웅", WeaponGrade.EPIC.getDisplayName());
            assertEquals("전설", WeaponGrade.LEGENDARY.getDisplayName());
            assertEquals("신화", WeaponGrade.MYTHIC.getDisplayName());
        }

        @Test
        @DisplayName("등급별 보너스 스탯 슬롯 수 확인")
        void testBonusStatSlots() {
            assertEquals(0, WeaponGrade.COMMON.getBonusStatSlots(), "일반은 0개");
            assertEquals(1, WeaponGrade.UNCOMMON.getBonusStatSlots(), "고급은 1개");
            assertEquals(2, WeaponGrade.RARE.getBonusStatSlots(), "희귀는 2개");
            assertEquals(3, WeaponGrade.EPIC.getBonusStatSlots(), "영웅은 3개");
            assertEquals(4, WeaponGrade.LEGENDARY.getBonusStatSlots(), "전설은 4개");
            assertEquals(5, WeaponGrade.MYTHIC.getBonusStatSlots(), "신화는 5개");
        }

        @Test
        @DisplayName("등급별 스탯 배율 증가 확인")
        void testStatMultiplier() {
            assertTrue(WeaponGrade.COMMON.getStatMultiplier() < WeaponGrade.UNCOMMON.getStatMultiplier());
            assertTrue(WeaponGrade.UNCOMMON.getStatMultiplier() < WeaponGrade.RARE.getStatMultiplier());
            assertTrue(WeaponGrade.RARE.getStatMultiplier() < WeaponGrade.EPIC.getStatMultiplier());
            assertTrue(WeaponGrade.EPIC.getStatMultiplier() < WeaponGrade.LEGENDARY.getStatMultiplier());
            assertTrue(WeaponGrade.LEGENDARY.getStatMultiplier() < WeaponGrade.MYTHIC.getStatMultiplier());
        }

        @Test
        @DisplayName("다음 등급 반환 테스트")
        void testGetNextGrade() {
            assertEquals(WeaponGrade.UNCOMMON, WeaponGrade.COMMON.getNextGrade());
            assertEquals(WeaponGrade.RARE, WeaponGrade.UNCOMMON.getNextGrade());
            assertEquals(WeaponGrade.EPIC, WeaponGrade.RARE.getNextGrade());
            assertEquals(WeaponGrade.LEGENDARY, WeaponGrade.EPIC.getNextGrade());
            assertEquals(WeaponGrade.MYTHIC, WeaponGrade.LEGENDARY.getNextGrade());
            assertNull(WeaponGrade.MYTHIC.getNextGrade(), "신화는 최고 등급이므로 null");
        }

        @Test
        @DisplayName("tier로 등급 조회 테스트")
        void testFromTier() {
            assertEquals(WeaponGrade.COMMON, WeaponGrade.fromTier(0));
            assertEquals(WeaponGrade.UNCOMMON, WeaponGrade.fromTier(1));
            assertEquals(WeaponGrade.RARE, WeaponGrade.fromTier(2));
            assertEquals(WeaponGrade.EPIC, WeaponGrade.fromTier(3));
            assertEquals(WeaponGrade.LEGENDARY, WeaponGrade.fromTier(4));
            assertEquals(WeaponGrade.MYTHIC, WeaponGrade.fromTier(5));
            // 존재하지 않는 tier는 COMMON(기본값)으로 반환
            assertEquals(WeaponGrade.COMMON, WeaponGrade.fromTier(99), "존재하지 않는 tier는 COMMON");
            assertEquals(WeaponGrade.COMMON, WeaponGrade.fromTier(-1), "음수 tier는 COMMON");
        }

        @Test
        @DisplayName("색상 코드가 포함된 이름 테스트")
        void testColoredName() {
            for (WeaponGrade grade : WeaponGrade.values()) {
                String coloredName = grade.getColoredName();
                assertTrue(coloredName.startsWith("§"), "색상 코드로 시작해야 함");
                assertTrue(coloredName.contains(grade.getDisplayName()), "이름이 포함되어야 함");
            }
        }
    }

    @Nested
    @DisplayName("BonusStat 테스트")
    class BonusStatTest {

        @Test
        @DisplayName("BonusStat 생성 및 getter 테스트")
        void testBonusStatCreation() {
            BonusStat stat = new BonusStat(StatType.ATK, 10.5);
            assertEquals(StatType.ATK, stat.getType());
            assertEquals(10.5, stat.getValue(), 0.001);
        }

        @Test
        @DisplayName("저장용 문자열 변환 테스트")
        void testToStorageString() {
            BonusStat stat = new BonusStat(StatType.DEF, 5.25);
            String storage = stat.toStorageString();
            assertEquals("DEF:5.25", storage);
        }

        @Test
        @DisplayName("저장용 문자열에서 복원 테스트")
        void testFromStorageString() {
            BonusStat stat = BonusStat.fromStorageString("DEF:20.0");
            assertEquals(StatType.DEF, stat.getType());
            assertEquals(20.0, stat.getValue(), 0.001);
        }

        @Test
        @DisplayName("저장/복원 왕복 테스트")
        void testRoundTrip() {
            for (StatType type : StatType.values()) {
                BonusStat original = new BonusStat(type, 12.34);
                BonusStat restored = BonusStat.fromStorageString(original.toStorageString());
                assertEquals(original.getType(), restored.getType());
                assertEquals(original.getValue(), restored.getValue(), 0.001);
            }
        }

        @Test
        @DisplayName("디스플레이 문자열 - ATK")
        void testDisplayStringAtk() {
            BonusStat stat = new BonusStat(StatType.ATK, 5.5);
            String display = stat.getDisplayString();
            assertTrue(display.contains("+5.5"), "양수는 + 기호 포함");
            assertTrue(display.contains(StatType.ATK.getDisplayName()));
        }

        @Test
        @DisplayName("디스플레이 문자열 - CRIT (퍼센트)")
        void testDisplayStringCrit() {
            BonusStat stat = new BonusStat(StatType.CRIT, 2.5);
            String display = stat.getDisplayString();
            assertTrue(display.contains("%"), "CRIT는 퍼센트 표시");
        }

        @Test
        @DisplayName("디스플레이 문자열 - CRIT_DMG (퍼센트)")
        void testDisplayStringCritDmg() {
            BonusStat stat = new BonusStat(StatType.CRIT_DMG, 0.15);
            String display = stat.getDisplayString();
            assertTrue(display.contains("%"), "CRIT_DMG는 퍼센트 표시");
            assertTrue(display.contains("15"), "0.15 = 15%");
        }
    }

    @Nested
    @DisplayName("BonusStatGenerator 테스트")
    class BonusStatGeneratorTest {

        @RepeatedTest(10)
        @DisplayName("생성된 스탯 타입이 유효해야 함")
        void testGeneratedStatTypeValid() {
            BonusStat stat = BonusStatGenerator.generate(WeaponGrade.RARE);
            assertNotNull(stat);
            assertNotNull(stat.getType());
            assertTrue(stat.getValue() > 0, "스탯 값은 양수여야 함");
        }

        @Test
        @DisplayName("등급별 스탯 범위 테스트 - 높은 등급이 더 높은 값")
        void testHigherGradeHigherStats() {
            double commonTotal = 0;
            double mythicTotal = 0;
            int samples = 100;

            for (int i = 0; i < samples; i++) {
                commonTotal += BonusStatGenerator.generate(WeaponGrade.COMMON).getValue();
                mythicTotal += BonusStatGenerator.generate(WeaponGrade.MYTHIC).getValue();
            }

            double commonAvg = commonTotal / samples;
            double mythicAvg = mythicTotal / samples;

            assertTrue(mythicAvg > commonAvg,
                "신화 등급 평균(" + mythicAvg + ")이 일반 등급 평균(" + commonAvg + ")보다 높아야 함");
        }

        @Test
        @DisplayName("generateForGrade - 기존 스탯 유지하며 새 스탯 추가")
        void testGenerateForGradePreservesExisting() {
            List<BonusStat> existing = new ArrayList<>();
            existing.add(new BonusStat(StatType.ATK, 10.0));

            List<BonusStat> result = BonusStatGenerator.generateForGrade(WeaponGrade.RARE, existing);

            assertEquals(2, result.size(), "희귀 등급은 2개 슬롯");
            assertEquals(StatType.ATK, result.get(0).getType(), "기존 스탯 유지");
            assertEquals(10.0, result.get(0).getValue(), 0.001, "기존 값 유지");
        }

        @Test
        @DisplayName("generateForGrade - 슬롯 수 맞춤")
        void testGenerateForGradeSlotCount() {
            List<BonusStat> empty = new ArrayList<>();

            assertEquals(0, BonusStatGenerator.generateForGrade(WeaponGrade.COMMON, empty).size());
            assertEquals(1, BonusStatGenerator.generateForGrade(WeaponGrade.UNCOMMON, empty).size());
            assertEquals(2, BonusStatGenerator.generateForGrade(WeaponGrade.RARE, empty).size());
            assertEquals(3, BonusStatGenerator.generateForGrade(WeaponGrade.EPIC, empty).size());
            assertEquals(4, BonusStatGenerator.generateForGrade(WeaponGrade.LEGENDARY, empty).size());
            assertEquals(5, BonusStatGenerator.generateForGrade(WeaponGrade.MYTHIC, empty).size());
        }

        @Test
        @DisplayName("rerollAll - 지정된 개수만큼 생성")
        void testRerollAllCount() {
            for (int count = 0; count <= 5; count++) {
                List<BonusStat> stats = BonusStatGenerator.rerollAll(WeaponGrade.EPIC, count);
                assertEquals(count, stats.size(), count + "개 요청 시 " + count + "개 생성");
            }
        }

        @Test
        @DisplayName("rerollAll - 모든 스탯이 유효한 값")
        void testRerollAllValidValues() {
            List<BonusStat> stats = BonusStatGenerator.rerollAll(WeaponGrade.LEGENDARY, 5);

            for (BonusStat stat : stats) {
                assertNotNull(stat.getType());
                assertTrue(stat.getValue() > 0);
            }
        }

        @RepeatedTest(20)
        @DisplayName("스탯 타입 랜덤성 테스트 - 다양한 타입 생성")
        void testStatTypeVariety() {
            Set<StatType> generatedTypes = new HashSet<>();

            for (int i = 0; i < 50; i++) {
                BonusStat stat = BonusStatGenerator.generate(WeaponGrade.MYTHIC);
                generatedTypes.add(stat.getType());
            }

            assertTrue(generatedTypes.size() >= 3,
                "50회 생성 시 최소 3종류 이상의 스탯 타입이 나와야 함 (실제: " + generatedTypes.size() + ")");
        }
    }
}
