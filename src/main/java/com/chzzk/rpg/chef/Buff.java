package com.chzzk.rpg.chef;

import com.chzzk.rpg.stats.PlayerStats;
import lombok.Getter;

@Getter
public class Buff {
    private final String name;
    private final PlayerStats stats;
    private final long expireTime; // System.currentTimeMillis()

    public Buff(String name, PlayerStats stats, int durationSeconds) {
        this.name = name;
        this.stats = stats;
        this.expireTime = System.currentTimeMillis() + (durationSeconds * 1000L);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
}
