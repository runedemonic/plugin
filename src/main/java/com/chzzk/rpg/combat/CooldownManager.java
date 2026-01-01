package com.chzzk.rpg.combat;

import java.util.UUID;
import java.util.WeakHashMap;
import org.bukkit.entity.Player;

public class CooldownManager {

    // Store last attack tick (server tick)
    private final WeakHashMap<UUID, Long> lastAttack = new WeakHashMap<>();

    public boolean canAttack(Player player, int cooldownTicks) {
        long current = player.getWorld().getFullTime();
        long last = lastAttack.getOrDefault(player.getUniqueId(), 0L);

        // If cooldownTicks == 0, always true (or based on policy)
        return (current - last) >= cooldownTicks;
    }

    public void updateAttack(Player player) {
        lastAttack.put(player.getUniqueId(), player.getWorld().getFullTime());
    }

    public long getRemainingTicks(Player player, int cooldownTicks) {
        long current = player.getWorld().getFullTime();
        long last = lastAttack.getOrDefault(player.getUniqueId(), 0L);
        long diff = current - last;
        return Math.max(0, cooldownTicks - diff);
    }
}
