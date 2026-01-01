package com.chzzk.rpg.guilds;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Guild {
    private int id;
    private String name;
    private UUID leaderUuid;
    private int level;
    private double exp;

    private final List<GuildMember> members = new ArrayList<>();

    public Guild(int id, String name, UUID leaderUuid) {
        this.id = id;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.level = 1;
        this.exp = 0;
    }

    public void addMember(GuildMember member) {
        members.add(member);
    }

    public void removeMember(UUID uuid) {
        members.removeIf(m -> m.getUuid().equals(uuid));
    }

    public boolean isMember(UUID uuid) {
        return members.stream().anyMatch(m -> m.getUuid().equals(uuid));
    }

    public GuildMember getMember(UUID uuid) {
        return members.stream().filter(m -> m.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
