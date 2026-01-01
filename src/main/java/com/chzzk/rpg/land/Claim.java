package com.chzzk.rpg.land;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Chunk;

@Getter
@Setter
public class Claim {
    private int id;
    private UUID worldUuid;
    private int chunkX;
    private int chunkZ;

    private ClaimType ownerType;
    private String ownerId; // UUID String or Guild Name

    public enum ClaimType {
        PERSONAL,
        GUILD
    }

    public Claim(UUID worldUuid, int chunkX, int chunkZ, ClaimType ownerType, String ownerId) {
        this.worldUuid = worldUuid;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }

    // Helper to check against Bukkit Chunk
    public boolean isChunk(Chunk chunk) {
        return chunk.getWorld().getUID().equals(worldUuid) && chunk.getX() == chunkX && chunk.getZ() == chunkZ;
    }
}
