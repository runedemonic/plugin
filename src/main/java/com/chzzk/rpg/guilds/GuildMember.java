package com.chzzk.rpg.guilds;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuildMember {
    private final UUID uuid;
    private Role role;

    public enum Role {
        LEADER,
        OFFICER,
        MEMBER
    }

    public GuildMember(UUID uuid, Role role) {
        this.uuid = uuid;
        this.role = role;
    }
}
