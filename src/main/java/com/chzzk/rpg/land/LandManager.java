package com.chzzk.rpg.land;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.hooks.VaultHook;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class LandManager {

    private final ChzzkRPG plugin;
    // Cache: "WorldUUID,X,Z" -> Claim
    private final Map<String, Claim> claims = new ConcurrentHashMap<>();

    private final double CLAIM_COST = 1000.0;

    public LandManager(ChzzkRPG plugin) {
        this.plugin = plugin;
        loadClaims();
    }

    private String getChunkKey(UUID worldId, int x, int z) {
        return worldId.toString() + "," + x + "," + z;
    }

    private String getChunkKey(Chunk chunk) {
        return getChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public void loadClaims() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM claims");
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID worldId = UUID.fromString(rs.getString("world_uuid"));
                        int x = rs.getInt("chunk_x");
                        int z = rs.getInt("chunk_z");
                        Claim.ClaimType type = Claim.ClaimType.valueOf(rs.getString("owner_type"));
                        String ownerId = rs.getString("owner_id");

                        Claim claim = new Claim(worldId, x, z, type, ownerId);
                        claim.setId(rs.getInt("id"));

                        claims.put(getChunkKey(worldId, x, z), claim);
                    }
                }
                plugin.getLogger().info("Loaded " + claims.size() + " claims.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Claim getClaim(Chunk chunk) {
        return claims.get(getChunkKey(chunk));
    }

    public void buyClaim(Player player, Chunk chunk) {
        buyClaim(player, chunk, Claim.ClaimType.PERSONAL, player.getUniqueId().toString());
    }

    public void buyGuildClaim(Player player, Chunk chunk, int guildId) {
        if (plugin.getGuildManager() == null || plugin.getGuildManager().getGuildById(guildId) == null) {
            player.sendMessage("§cGuild not found.");
            return;
        }
        buyClaim(player, chunk, Claim.ClaimType.GUILD, String.valueOf(guildId));
    }

    private void buyClaim(Player player, Chunk chunk, Claim.ClaimType claimType, String ownerId) {
        if (getClaim(chunk) != null) {
            player.sendMessage("§cThis land is already claimed.");
            return;
        }

        VaultHook economy = plugin.getVaultHook();
        if (economy != null && !economy.hasMoney(player, CLAIM_COST)) {
            player.sendMessage("§cNot enough money! Need $" + CLAIM_COST);
            return;
        }

        if (economy != null)
            economy.withdraw(player, CLAIM_COST);

        UUID playerId = player.getUniqueId();
        UUID worldId = chunk.getWorld().getUID();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Claim claim = new Claim(worldId, chunkX, chunkZ, claimType, ownerId);

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO claims (world_uuid, chunk_x, chunk_z, owner_type, owner_id) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setString(1, claim.getWorldUuid().toString());
                    ps.setInt(2, claim.getChunkX());
                    ps.setInt(3, claim.getChunkZ());
                    ps.setString(4, claim.getOwnerType().name());
                    ps.setString(5, claim.getOwnerId());
                    ps.executeUpdate();
                }

                // Reload or just put in cache
                // For simplified flow, just put
                claims.put(getChunkKey(worldId, chunkX, chunkZ), claim);
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> {
                            Player onlinePlayer = plugin.getServer().getPlayer(playerId);
                            if (onlinePlayer != null) {
                                onlinePlayer.sendMessage("§aSuccesfully claimed this chunk for $" + CLAIM_COST);
                            }
                        });

            } catch (SQLException e) {
                e.printStackTrace();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player onlinePlayer = plugin.getServer().getPlayer(playerId);
                    if (onlinePlayer != null) {
                        onlinePlayer.sendMessage("§cError saving claim.");
                        if (economy != null) {
                            economy.deposit(onlinePlayer, CLAIM_COST);
                            onlinePlayer.sendMessage("§aRefunded $" + CLAIM_COST);
                        }
                    }
                });
            }
        });
    }

    public void unclaim(Player player, Chunk chunk) {
        Claim claim = getClaim(chunk);
        if (claim == null) {
            player.sendMessage("§cThis land is not claimed.");
            return;
        }

        if (!claim.getOwnerId().equals(player.getUniqueId().toString()) && !player.isOp()) {
            if (claim.getOwnerType() == Claim.ClaimType.GUILD && plugin.getGuildManager() != null) {
                com.chzzk.rpg.guilds.Guild guild = plugin.getGuildManager().getGuild(player);
                if (guild != null && claim.getOwnerId().equals(String.valueOf(guild.getId()))) {
                    com.chzzk.rpg.guilds.GuildMember member = guild.getMember(player.getUniqueId());
                    if (member != null && member.getRole() != com.chzzk.rpg.guilds.GuildMember.Role.MEMBER) {
                        // Guild officer/leader can unclaim
                    } else {
                        player.sendMessage("§cOnly guild officers can unclaim.");
                        return;
                    }
                } else {
                    player.sendMessage("§cYou do not own this land.");
                    return;
                }
            } else {
                player.sendMessage("§cYou do not own this land.");
                return;
            }
        }

        UUID playerId = player.getUniqueId();
        UUID worldId = claim.getWorldUuid();
        int chunkX = claim.getChunkX();
        int chunkZ = claim.getChunkZ();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM claims WHERE world_uuid=? AND chunk_x=? AND chunk_z=?")) {
                    ps.setString(1, worldId.toString());
                    ps.setInt(2, chunkX);
                    ps.setInt(3, chunkZ);
                    ps.executeUpdate();
                }

                claims.remove(getChunkKey(worldId, chunkX, chunkZ));
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player onlinePlayer = plugin.getServer().getPlayer(playerId);
                    if (onlinePlayer != null) {
                        onlinePlayer.sendMessage("§aUnclaimed.");
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
