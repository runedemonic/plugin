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
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM claims");
                ResultSet rs = ps.executeQuery();
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

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Claim claim = new Claim(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ(), Claim.ClaimType.PERSONAL,
                    player.getUniqueId().toString());

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO claims (world_uuid, chunk_x, chunk_z, owner_type, owner_id) VALUES (?, ?, ?, ?, ?)");
                ps.setString(1, claim.getWorldUuid().toString());
                ps.setInt(2, claim.getChunkX());
                ps.setInt(3, claim.getChunkZ());
                ps.setString(4, claim.getOwnerType().name());
                ps.setString(5, claim.getOwnerId());
                ps.executeUpdate();

                // Reload or just put in cache
                // For simplified flow, just put
                claims.put(getChunkKey(chunk), claim);
                player.sendMessage("§aSuccesfully claimed this chunk for $" + CLAIM_COST);

            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage("§cError saving claim.");
                // Refund if error? complex loop, skip for now
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
            player.sendMessage("§cYou do not own this land.");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM claims WHERE world_uuid=? AND chunk_x=? AND chunk_z=?");
                ps.setString(1, claim.getWorldUuid().toString());
                ps.setInt(2, claim.getChunkX());
                ps.setInt(3, claim.getChunkZ());
                ps.executeUpdate();

                claims.remove(getChunkKey(chunk));
                player.sendMessage("§aUnclaimed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
