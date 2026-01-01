package com.chzzk.rpg.contracts;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Chunk;

@Getter
@Setter
public class Contract {
    private int id;
    private UUID employerUuid;
    private UUID contractorUuid; // Nullable

    // Area (Single Chunk for MVP)
    private UUID worldUuid;
    private int chunkX;
    private int chunkZ;

    private double reward;
    private double budget;
    private double currentBudget;

    private ContractStatus status;

    public enum ContractStatus {
        OPEN, // Waiting for builder
        ACTIVE, // Builder accepted, working
        COMPLETED, // Finished
        CANCELLED // Cancelled by employer or other
    }

    public Contract(UUID employerUuid, UUID worldUuid, int chunkX, int chunkZ, double reward, double budget) {
        this.employerUuid = employerUuid;
        this.worldUuid = worldUuid;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.reward = reward;
        this.budget = budget;
        this.currentBudget = budget;
        this.status = ContractStatus.OPEN;
    }

    public boolean isChunk(Chunk chunk) {
        return chunk.getWorld().getUID().equals(worldUuid) && chunk.getX() == chunkX && chunk.getZ() == chunkZ;
    }
}
