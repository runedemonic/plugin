package com.chzzk.rpg.contracts;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.hooks.VaultHook;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class ContractManager {

    private final ChzzkRPG plugin;
    // Cache: Contract ID -> Contract
    private final Map<Integer, Contract> contracts = new ConcurrentHashMap<>();

    public ContractManager(ChzzkRPG plugin) {
        this.plugin = plugin;
        loadContracts();
    }

    public void loadContracts() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement ps = conn
                        .prepareStatement("SELECT * FROM contracts WHERE status IN ('OPEN', 'ACTIVE')");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID employer = UUID.fromString(rs.getString("employer_uuid"));
                    UUID world = UUID.fromString(rs.getString("world_uuid"));
                    int x = rs.getInt("chunk_x");
                    int z = rs.getInt("chunk_z");
                    double reward = rs.getDouble("reward");
                    double budget = rs.getDouble("budget");

                    Contract contract = new Contract(employer, world, x, z, reward, budget);
                    contract.setId(rs.getInt("id"));
                    contract.setCurrentBudget(rs.getDouble("current_budget"));
                    contract.setStatus(Contract.ContractStatus.valueOf(rs.getString("status")));

                    String contractorStr = rs.getString("contractor_uuid");
                    if (contractorStr != null) {
                        contract.setContractorUuid(UUID.fromString(contractorStr));
                    }

                    contracts.put(contract.getId(), contract);
                }
                plugin.getLogger().info("Loaded " + contracts.size() + " active contracts.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void createContract(Player employer, Chunk chunk, double reward, double budget) {
        // 1. Check Land Ownership
        com.chzzk.rpg.land.Claim claim = plugin.getLandManager().getClaim(chunk);
        if (claim == null || !claim.getOwnerId().equals(employer.getUniqueId().toString())) {
            employer.sendMessage("§cYou must own the land to create a contract.");
            return;
        }

        if (reward < 0 || budget < 0) {
            employer.sendMessage("§cValues cannot be negative.");
            return;
        }

        // 2. Escrow Check
        double totalCost = reward + budget;
        VaultHook economy = plugin.getVaultHook();
        if (economy != null) {
            if (!economy.hasMoney(employer, totalCost)) {
                employer.sendMessage("§cNot enough money. Need $" + totalCost);
                return;
            }
            economy.withdraw(employer, totalCost);
        }

        // 3. Create & Save
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Contract contract = new Contract(employer.getUniqueId(), chunk.getWorld().getUID(), chunk.getX(),
                    chunk.getZ(), reward, budget);

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO contracts (employer_uuid, world_uuid, chunk_x, chunk_z, reward, budget, current_budget, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, contract.getEmployerUuid().toString());
                ps.setString(2, contract.getWorldUuid().toString());
                ps.setInt(3, contract.getChunkX());
                ps.setInt(4, contract.getChunkZ());
                ps.setDouble(5, contract.getReward());
                ps.setDouble(6, contract.getBudget());
                ps.setDouble(7, contract.getCurrentBudget());
                ps.setString(8, contract.getStatus().name());
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    contract.setId(rs.getInt(1));
                    contracts.put(contract.getId(), contract);
                    employer.sendMessage("§aContract #" + contract.getId() + " created!");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                // Refund logic ideally here
            }
        });
    }

    public void acceptContract(Player builder, int contractId) {
        Contract contract = contracts.get(contractId);
        if (contract == null || contract.getStatus() != Contract.ContractStatus.OPEN) {
            builder.sendMessage("§cContract unavailable.");
            return;
        }

        if (contract.getEmployerUuid().equals(builder.getUniqueId())) {
            builder.sendMessage("§cYou cannot accept your own contract.");
            return;
        }

        contract.setStatus(Contract.ContractStatus.ACTIVE);
        contract.setContractorUuid(builder.getUniqueId());

        updateContractDB(contract);
        builder.sendMessage("§aYou accepted Contract #" + contractId);
    }

    public void completeContract(Player employer, int contractId) {
        Contract contract = contracts.get(contractId);
        if (contract == null || contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            employer.sendMessage("§cCannot complete this contract.");
            return;
        }

        if (!contract.getEmployerUuid().equals(employer.getUniqueId()) && !employer.isOp()) {
            employer.sendMessage("§cNot your contract.");
            return;
        }

        contract.setStatus(Contract.ContractStatus.COMPLETED);

        // Payout to Builder
        VaultHook economy = plugin.getVaultHook();
        if (economy != null) {
            org.bukkit.OfflinePlayer builder = plugin.getServer().getOfflinePlayer(contract.getContractorUuid());
            economy.deposit(builder, contract.getReward()); // Pay Labor

            // Refund remaining budget
            if (contract.getCurrentBudget() > 0) {
                economy.deposit(employer, contract.getCurrentBudget());
            }
        }

        updateContractDB(contract);
        employer.sendMessage("§aContract #" + contractId + " completed.");
        contracts.remove(contractId); // Remove from active cache
    }

    private void updateContractDB(Contract contract) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE contracts SET contractor_uuid=?, status=?, current_budget=? WHERE id=?");
                ps.setString(1, contract.getContractorUuid() == null ? null : contract.getContractorUuid().toString());
                ps.setString(2, contract.getStatus().name());
                ps.setDouble(3, contract.getCurrentBudget());
                ps.setInt(4, contract.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Check if a player interacts with a chunk that has an active contract for them
    public Contract getActiveContract(Player contractor, Chunk chunk) {
        for (Contract c : contracts.values()) {
            if (c.getStatus() == Contract.ContractStatus.ACTIVE &&
                    c.getContractorUuid().equals(contractor.getUniqueId()) &&
                    c.isChunk(chunk)) {
                return c;
            }
        }
        return null;
    }

    // Spend from budget
    public boolean spendBudget(Contract contract, double amount) {
        if (contract.getCurrentBudget() >= amount) {
            contract.setCurrentBudget(contract.getCurrentBudget() - amount);
            // Async update? For now just cache update, DB update on completion or periodic
            return true;
        }
        return false;
    }

    public List<Contract> getOpenContracts() {
        List<Contract> list = new ArrayList<>();
        for (Contract c : contracts.values()) {
            if (c.getStatus() == Contract.ContractStatus.OPEN) {
                list.add(c);
            }
        }
        return list;
    }
}
