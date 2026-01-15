package com.chzzk.rpg.commands;

import com.chzzk.rpg.ChzzkRPG;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RtpCommand implements CommandExecutor {

    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("rpg.rtp")) {
            player.sendMessage("§cNo permission.");
            return true;
        }
        ChzzkRPG plugin = ChzzkRPG.getInstance();
        World world = player.getWorld();

        if (!plugin.getCompatibilityManager().isRtpAllowed(world)) {
            player.sendMessage("§c이 월드에서는 랜덤 텔레포트를 사용할 수 없습니다.");
            return true;
        }

        int cooldownSeconds = plugin.getConfig().getInt("rtp.cooldown-seconds", 60);
        if (cooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            long lastUse = COOLDOWNS.getOrDefault(player.getUniqueId(), 0L);
            long remainingMs = (lastUse + cooldownSeconds * 1000L) - now;
            if (remainingMs > 0) {
                long remainingSeconds = (remainingMs + 999) / 1000;
                player.sendMessage("§c쿨다운: " + remainingSeconds + "초 남았습니다.");
                return true;
            }
        }

        int minRadius = plugin.getConfig().getInt("rtp.radius-min", 200);
        int maxRadius = plugin.getConfig().getInt("rtp.radius-max", 1000);
        int maxAttempts = plugin.getConfig().getInt("rtp.max-attempts", 10);
        if (minRadius < 0 || maxRadius < 0 || maxRadius < minRadius) {
            player.sendMessage("§cRTP 설정이 잘못되었습니다.");
            return true;
        }

        Location origin = player.getLocation();
        Location target = findSafeLocation(player, world, origin, minRadius, maxRadius, maxAttempts);
        if (target == null) {
            player.sendMessage("§c안전한 위치를 찾지 못했습니다. 다시 시도해주세요.");
            return true;
        }

        player.teleport(target);
        COOLDOWNS.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage("§a랜덤 텔레포트 완료!");
        return true;
    }

    private Location findSafeLocation(Player player, World world, Location origin, int minRadius, int maxRadius, int attempts) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        org.bukkit.WorldBorder border = world.getWorldBorder();
        ChzzkRPG plugin = ChzzkRPG.getInstance();

        for (int i = 0; i < attempts; i++) {
            double distance = random.nextDouble(minRadius, maxRadius + 1.0);
            double angle = random.nextDouble(0, Math.PI * 2);
            int x = origin.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);

            Location center = new Location(world, x + 0.5, world.getMinHeight(), z + 0.5);
            if (!border.isInside(center)) {
                continue;
            }

            int y = world.getHighestBlockYAt(x, z);
            if (y <= world.getMinHeight()) {
                continue;
            }

            Block ground = world.getBlockAt(x, y - 1, z);
            Block target = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (!isSafeBlock(ground, target, head)) {
                continue;
            }

            Location targetLocation = new Location(world, x + 0.5, y, z + 0.5);

            // Check WorldGuard protected regions
            if (plugin.getCompatibilityManager().isInProtectedRegion(targetLocation)) {
                continue;
            }

            // Check if player can teleport to this location
            if (!plugin.getCompatibilityManager().canTeleportTo(player, targetLocation)) {
                continue;
            }

            return targetLocation;
        }
        return null;
    }

    private boolean isSafeBlock(Block ground, Block target, Block head) {
        if (!ground.getType().isSolid()) {
            return false;
        }
        if (ground.isLiquid()) {
            return false;
        }
        return target.getType().isAir() && head.getType().isAir();
    }
}
