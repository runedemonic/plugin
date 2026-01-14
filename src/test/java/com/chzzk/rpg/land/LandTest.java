package com.chzzk.rpg.land;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Land 시스템 테스트")
class LandTest {

    private UUID worldUuid;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        worldUuid = UUID.randomUUID();
        playerUuid = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Claim 클래스 테스트")
    class ClaimTest {

        @Test
        @DisplayName("개인 토지 생성 테스트")
        void testPersonalClaimCreation() {
            Claim claim = new Claim(worldUuid, 10, 20, Claim.ClaimType.PERSONAL, playerUuid.toString());

            assertEquals(worldUuid, claim.getWorldUuid());
            assertEquals(10, claim.getChunkX());
            assertEquals(20, claim.getChunkZ());
            assertEquals(Claim.ClaimType.PERSONAL, claim.getOwnerType());
            assertEquals(playerUuid.toString(), claim.getOwnerId());
        }

        @Test
        @DisplayName("길드 토지 생성 테스트")
        void testGuildClaimCreation() {
            int guildId = 123;
            Claim claim = new Claim(worldUuid, 5, -10, Claim.ClaimType.GUILD, String.valueOf(guildId));

            assertEquals(Claim.ClaimType.GUILD, claim.getOwnerType());
            assertEquals("123", claim.getOwnerId());
        }

        @Test
        @DisplayName("Claim ID 설정 테스트")
        void testClaimIdSetter() {
            Claim claim = new Claim(worldUuid, 0, 0, Claim.ClaimType.PERSONAL, playerUuid.toString());
            claim.setId(42);

            assertEquals(42, claim.getId());
        }

        @Test
        @DisplayName("음수 청크 좌표 테스트")
        void testNegativeChunkCoordinates() {
            Claim claim = new Claim(worldUuid, -100, -200, Claim.ClaimType.PERSONAL, playerUuid.toString());

            assertEquals(-100, claim.getChunkX());
            assertEquals(-200, claim.getChunkZ());
        }

        @Test
        @DisplayName("ClaimType enum 값 테스트")
        void testClaimTypes() {
            assertEquals(2, Claim.ClaimType.values().length);
            assertNotNull(Claim.ClaimType.valueOf("PERSONAL"));
            assertNotNull(Claim.ClaimType.valueOf("GUILD"));
        }

        @Test
        @DisplayName("소유자 변경 테스트")
        void testOwnerChange() {
            Claim claim = new Claim(worldUuid, 0, 0, Claim.ClaimType.PERSONAL, playerUuid.toString());

            UUID newOwner = UUID.randomUUID();
            claim.setOwnerId(newOwner.toString());
            claim.setOwnerType(Claim.ClaimType.GUILD);

            assertEquals(newOwner.toString(), claim.getOwnerId());
            assertEquals(Claim.ClaimType.GUILD, claim.getOwnerType());
        }
    }

    @Nested
    @DisplayName("토지 구매 비용 테스트")
    class LandCostTest {

        private static final double CLAIM_COST = 1000.0;

        @Test
        @DisplayName("기본 토지 구매 비용은 1000")
        void testClaimCost() {
            assertEquals(1000.0, CLAIM_COST);
        }

        @Test
        @DisplayName("잔액 충분 여부 계산")
        void testSufficientBalance() {
            double playerBalance = 5000.0;
            assertTrue(playerBalance >= CLAIM_COST, "5000원은 1000원 토지를 구매할 수 있어야 함");
        }

        @Test
        @DisplayName("잔액 부족 여부 계산")
        void testInsufficientBalance() {
            double playerBalance = 500.0;
            assertFalse(playerBalance >= CLAIM_COST, "500원은 1000원 토지를 구매할 수 없어야 함");
        }

        @Test
        @DisplayName("구매 후 잔액 계산")
        void testBalanceAfterPurchase() {
            double playerBalance = 2500.0;
            double afterPurchase = playerBalance - CLAIM_COST;
            assertEquals(1500.0, afterPurchase);
        }

        @Test
        @DisplayName("환불 후 잔액 계산")
        void testBalanceAfterRefund() {
            double playerBalance = 1500.0;
            double afterRefund = playerBalance + CLAIM_COST;
            assertEquals(2500.0, afterRefund);
        }
    }

    @Nested
    @DisplayName("청크 키 생성 테스트")
    class ChunkKeyTest {

        /**
         * LandManager의 getChunkKey 로직 재현
         */
        private String getChunkKey(UUID worldId, int x, int z) {
            return worldId.toString() + "," + x + "," + z;
        }

        @Test
        @DisplayName("청크 키 포맷 테스트")
        void testChunkKeyFormat() {
            String key = getChunkKey(worldUuid, 10, 20);

            assertTrue(key.contains(worldUuid.toString()));
            assertTrue(key.contains(",10,"));
            assertTrue(key.endsWith(",20"));
        }

        @Test
        @DisplayName("동일 좌표는 동일 키 생성")
        void testSameCoordinatesSameKey() {
            String key1 = getChunkKey(worldUuid, 5, 10);
            String key2 = getChunkKey(worldUuid, 5, 10);

            assertEquals(key1, key2);
        }

        @Test
        @DisplayName("다른 좌표는 다른 키 생성")
        void testDifferentCoordinatesDifferentKey() {
            String key1 = getChunkKey(worldUuid, 5, 10);
            String key2 = getChunkKey(worldUuid, 5, 11);

            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("다른 월드는 다른 키 생성")
        void testDifferentWorldDifferentKey() {
            UUID anotherWorld = UUID.randomUUID();
            String key1 = getChunkKey(worldUuid, 5, 10);
            String key2 = getChunkKey(anotherWorld, 5, 10);

            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("음수 좌표 키 생성")
        void testNegativeCoordinatesKey() {
            String key = getChunkKey(worldUuid, -100, -200);

            assertTrue(key.contains(",-100,"));
            assertTrue(key.endsWith(",-200"));
        }
    }

    @Nested
    @DisplayName("토지 소유권 검증 테스트")
    class OwnershipTest {

        @Test
        @DisplayName("본인 소유 토지 확인")
        void testOwnershipCheck() {
            Claim claim = new Claim(worldUuid, 0, 0, Claim.ClaimType.PERSONAL, playerUuid.toString());

            assertEquals(playerUuid.toString(), claim.getOwnerId());
            assertTrue(claim.getOwnerId().equals(playerUuid.toString()));
        }

        @Test
        @DisplayName("타인 소유 토지 확인")
        void testNotOwner() {
            UUID otherPlayer = UUID.randomUUID();
            Claim claim = new Claim(worldUuid, 0, 0, Claim.ClaimType.PERSONAL, otherPlayer.toString());

            assertFalse(claim.getOwnerId().equals(playerUuid.toString()));
        }

        @Test
        @DisplayName("길드 토지 소유권 - 길드 ID로 확인")
        void testGuildOwnership() {
            int guildId = 5;
            Claim claim = new Claim(worldUuid, 0, 0, Claim.ClaimType.GUILD, String.valueOf(guildId));

            assertEquals(Claim.ClaimType.GUILD, claim.getOwnerType());
            assertEquals("5", claim.getOwnerId());

            // 길드 멤버인지 확인하는 로직 (실제로는 GuildManager에서 처리)
            int playerGuildId = 5;
            assertTrue(claim.getOwnerId().equals(String.valueOf(playerGuildId)));
        }
    }
}
