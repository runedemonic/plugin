package com.chzzk.rpg.guilds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Guild 테스트")
class GuildTest {

    private Guild guild;
    private UUID leaderUuid;
    private UUID memberUuid;
    private UUID officerUuid;

    @BeforeEach
    void setUp() {
        leaderUuid = UUID.randomUUID();
        memberUuid = UUID.randomUUID();
        officerUuid = UUID.randomUUID();
        guild = new Guild(1, "TestGuild", leaderUuid);
    }

    @Test
    @DisplayName("길드 생성 테스트")
    void testGuildCreation() {
        assertEquals(1, guild.getId());
        assertEquals("TestGuild", guild.getName());
        assertEquals(leaderUuid, guild.getLeaderUuid());
        assertEquals(1, guild.getLevel(), "초기 레벨은 1이어야 함");
        assertEquals(0.0, guild.getExp(), "초기 경험치는 0이어야 함");
        assertTrue(guild.getMembers().isEmpty(), "초기 멤버 리스트는 비어있어야 함");
    }

    @Test
    @DisplayName("멤버 추가 테스트")
    void testAddMember() {
        GuildMember member = new GuildMember(memberUuid, GuildMember.Role.MEMBER);
        guild.addMember(member);

        assertEquals(1, guild.getMembers().size());
        assertTrue(guild.isMember(memberUuid));
    }

    @Test
    @DisplayName("멤버 삭제 테스트")
    void testRemoveMember() {
        GuildMember member = new GuildMember(memberUuid, GuildMember.Role.MEMBER);
        guild.addMember(member);
        assertTrue(guild.isMember(memberUuid));

        guild.removeMember(memberUuid);
        assertFalse(guild.isMember(memberUuid));
        assertEquals(0, guild.getMembers().size());
    }

    @Test
    @DisplayName("멤버 조회 테스트")
    void testGetMember() {
        GuildMember member = new GuildMember(memberUuid, GuildMember.Role.MEMBER);
        guild.addMember(member);

        GuildMember found = guild.getMember(memberUuid);
        assertNotNull(found);
        assertEquals(memberUuid, found.getUuid());
        assertEquals(GuildMember.Role.MEMBER, found.getRole());
    }

    @Test
    @DisplayName("존재하지 않는 멤버 조회 시 null 반환")
    void testGetNonExistentMember() {
        UUID randomUuid = UUID.randomUUID();
        assertNull(guild.getMember(randomUuid));
        assertFalse(guild.isMember(randomUuid));
    }

    @Test
    @DisplayName("여러 멤버 추가 및 관리 테스트")
    void testMultipleMembers() {
        GuildMember leader = new GuildMember(leaderUuid, GuildMember.Role.LEADER);
        GuildMember officer = new GuildMember(officerUuid, GuildMember.Role.OFFICER);
        GuildMember member = new GuildMember(memberUuid, GuildMember.Role.MEMBER);

        guild.addMember(leader);
        guild.addMember(officer);
        guild.addMember(member);

        assertEquals(3, guild.getMembers().size());
        assertTrue(guild.isMember(leaderUuid));
        assertTrue(guild.isMember(officerUuid));
        assertTrue(guild.isMember(memberUuid));
    }

    @Test
    @DisplayName("길드 레벨/경험치 변경 테스트")
    void testLevelAndExp() {
        guild.setLevel(5);
        guild.setExp(1500.0);

        assertEquals(5, guild.getLevel());
        assertEquals(1500.0, guild.getExp());
    }

    @Test
    @DisplayName("길드 이름 변경 테스트")
    void testNameChange() {
        guild.setName("NewGuildName");
        assertEquals("NewGuildName", guild.getName());
    }

    @Test
    @DisplayName("GuildMember Role 테스트")
    void testGuildMemberRoles() {
        GuildMember leader = new GuildMember(leaderUuid, GuildMember.Role.LEADER);
        GuildMember officer = new GuildMember(officerUuid, GuildMember.Role.OFFICER);
        GuildMember member = new GuildMember(memberUuid, GuildMember.Role.MEMBER);

        assertEquals(GuildMember.Role.LEADER, leader.getRole());
        assertEquals(GuildMember.Role.OFFICER, officer.getRole());
        assertEquals(GuildMember.Role.MEMBER, member.getRole());

        // Role 변경 테스트
        member.setRole(GuildMember.Role.OFFICER);
        assertEquals(GuildMember.Role.OFFICER, member.getRole());
    }
}
