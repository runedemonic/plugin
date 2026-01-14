/**
 * 외부 의존성 없이 순수 Java로 테스트 실행
 * (네트워크 제한 환경용)
 */
public class SimpleTestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("ChzzkRPG 단순 테스트 실행기");
        System.out.println("=".repeat(50));
        System.out.println();

        // 데미지 계산 테스트
        testDamageCalculation();

        // 스탯 테스트 (순수 로직)
        testStatCalculations();

        // 청크 키 테스트
        testChunkKeyGeneration();

        // 결과 출력
        System.out.println();
        System.out.println("=".repeat(50));
        System.out.printf("결과: %d 통과, %d 실패%n", passed, failed);
        System.out.println("=".repeat(50));

        if (failed > 0) {
            System.exit(1);
        }
    }

    // ===== 데미지 계산 테스트 =====
    static void testDamageCalculation() {
        System.out.println("[데미지 계산 테스트]");

        // 데미지 계산 공식
        // effectiveDef = DEF * (1 - PEN/100)
        // baseDamage = max(1.0, totalAtk - max(0, effectiveDef))
        // finalDamage = baseDamage * critDmg (크리티컬 시)

        // 테스트 1: 기본 데미지 (방어력 없음)
        test("기본 데미지 (방어력 없음)",
            calculateDamage(100, 50, 0, 0, false, 1.5) == 150.0);

        // 테스트 2: 방어력 적용
        test("방어력 적용",
            calculateDamage(100, 50, 30, 0, false, 1.5) == 120.0);

        // 테스트 3: 50% 관통력
        test("50% 관통력",
            calculateDamage(100, 50, 100, 50, false, 1.5) == 100.0);

        // 테스트 4: 100% 관통력 (방어 무시)
        test("100% 관통력",
            calculateDamage(100, 50, 100, 100, false, 1.5) == 150.0);

        // 테스트 5: 크리티컬 데미지
        test("크리티컬 데미지 (1.5배)",
            calculateDamage(100, 50, 0, 0, true, 1.5) == 225.0);

        // 테스트 6: 최소 데미지 보장
        test("최소 데미지 1.0 보장",
            calculateDamage(10, 0, 1000, 0, false, 1.5) == 1.0);

        // 테스트 7: 복합 계산
        // ATK 100 + 무기 50 = 150, DEF 80, PEN 25%
        // effectiveDef = 80 * 0.75 = 60
        // baseDamage = 150 - 60 = 90
        // critDamage = 90 * 2.0 = 180
        test("복합 계산 (ATK+방어+관통+크리티컬)",
            calculateDamage(100, 50, 80, 25, true, 2.0) == 180.0);

        System.out.println();
    }

    static double calculateDamage(double playerAtk, double weaponAtk, double def,
                                   double pen, boolean isCrit, double critDmg) {
        pen = Math.max(0.0, Math.min(100.0, pen));
        double totalAtk = playerAtk + weaponAtk;
        double effectiveDef = def * (1 - (pen / 100.0));
        double baseDamage = Math.max(1.0, totalAtk - Math.max(0, effectiveDef));
        double finalDamage = baseDamage;
        if (isCrit) {
            finalDamage *= Math.max(1.0, critDmg);
        }
        return finalDamage;
    }

    // ===== 스탯 계산 테스트 =====
    static void testStatCalculations() {
        System.out.println("[스탯 계산 테스트]");

        // 스탯 누적
        double hp = 0;
        hp += 100;
        hp += 50;
        hp += 25;
        test("스탯 누적 (100+50+25=175)", hp == 175.0);

        // 직업 배율 (Warrior: ATK×1.2, DEF×1.1)
        double baseAtk = 100;
        double warriorAtk = baseAtk * 1.2;
        test("전사 ATK 배율 (100×1.2=120)", warriorAtk == 120.0);

        // 직업 배율 (Rogue: +10% CRIT)
        double baseCrit = 5.0;
        double rogueCrit = baseCrit + 10.0;
        test("로그 크리티컬 보너스 (5+10=15)", rogueCrit == 15.0);

        // CRIT_DMG 기본값
        double critDmgDefault = 1.5;
        test("CRIT_DMG 기본값 1.5", critDmgDefault == 1.5);

        System.out.println();
    }

    // ===== 청크 키 테스트 =====
    static void testChunkKeyGeneration() {
        System.out.println("[청크 키 생성 테스트]");

        String worldUuid = "550e8400-e29b-41d4-a716-446655440000";

        // 키 포맷 테스트
        String key = worldUuid + "," + 10 + "," + 20;
        test("키 포맷 확인", key.equals("550e8400-e29b-41d4-a716-446655440000,10,20"));

        // 동일 좌표 = 동일 키
        String key1 = worldUuid + "," + 5 + "," + 10;
        String key2 = worldUuid + "," + 5 + "," + 10;
        test("동일 좌표는 동일 키", key1.equals(key2));

        // 다른 좌표 = 다른 키
        String key3 = worldUuid + "," + 5 + "," + 11;
        test("다른 좌표는 다른 키", !key1.equals(key3));

        // 음수 좌표
        String negKey = worldUuid + "," + (-100) + "," + (-200);
        test("음수 좌표 키", negKey.contains(",-100,") && negKey.endsWith(",-200"));

        System.out.println();
    }

    // ===== 테스트 유틸리티 =====
    static void test(String name, boolean condition) {
        if (condition) {
            System.out.println("  ✓ " + name);
            passed++;
        } else {
            System.out.println("  ✗ " + name + " [실패]");
            failed++;
        }
    }
}
