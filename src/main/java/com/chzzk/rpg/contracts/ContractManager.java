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
                try (PreparedStatement ps = conn
                        .prepareStatement("SELECT * FROM contracts WHERE status IN ('OPEN', 'ACTIVE')");
                        ResultSet rs = ps.executeQuery()) {
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
        UUID employerId = employer.getUniqueId();
        UUID worldId = chunk.getWorld().getUID();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Contract contract = new Contract(employerId, worldId, chunkX, chunkZ, reward, budget);

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO contracts (employer_uuid, world_uuid, chunk_x, chunk_z, reward, budget, current_budget, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, employerId.toString());
                    ps.setString(2, worldId.toString());
                    ps.setInt(3, chunkX);
                    ps.setInt(4, chunkZ);
                    ps.setDouble(5, contract.getReward());
                    ps.setDouble(6, contract.getBudget());
                    ps.setDouble(7, contract.getCurrentBudget());
                    ps.setString(8, contract.getStatus().name());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            contract.setId(rs.getInt(1));
                            contracts.put(contract.getId(), contract);
                            plugin.getServer().getScheduler().runTask(plugin,
                                    () -> {
                                        Player onlineEmployer = plugin.getServer().getPlayer(employerId);
                                        if (onlineEmployer != null) {
                                            onlineEmployer.sendMessage("§aContract #" + contract.getId() + " created!");
                                        }
                                    });
                        }
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
                if (economy != null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        Player onlineEmployer = plugin.getServer().getPlayer(employerId);
                        if (onlineEmployer != null) {
                            economy.deposit(onlineEmployer, totalCost);
                            onlineEmployer.sendMessage("§cContract creation failed. Refunded $" + totalCost);
                        }
                    });
                }
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

        updateContractAcceptance(contract, builder.getUniqueId());
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

        if (contract.getContractorUuid() == null) {
            employer.sendMessage("§cContractor not set.");
            return;
        }

        contract.setStatus(Contract.ContractStatus.COMPLETED);
        updateContractCompletion(contract, employer.getUniqueId());
    }

    private void updateContractAcceptance(Contract contract, UUID builderId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE contracts SET contractor_uuid=?, status=? WHERE id=? AND status=?")) {
                    ps.setString(1, builderId.toString());
                    ps.setString(2, Contract.ContractStatus.ACTIVE.name());
                    ps.setInt(3, contract.getId());
                    ps.setString(4, Contract.ContractStatus.OPEN.name());
                    success = ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            boolean updateSuccess = success;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player builder = plugin.getServer().getPlayer(builderId);
                if (updateSuccess) {
                    if (builder != null) {
                        builder.sendMessage("§aYou accepted Contract #" + contract.getId());
                    }
                } else {
                    contract.setStatus(Contract.ContractStatus.OPEN);
                    contract.setContractorUuid(null);
                    if (builder != null) {
                        builder.sendMessage("§cContract unavailable.");
                    }
                }
            });
        });
    }

    private void updateContractCompletion(Contract contract, UUID employerId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE contracts SET status=? WHERE id=? AND status=?")) {
                    ps.setString(1, Contract.ContractStatus.COMPLETED.name());
                    ps.setInt(2, contract.getId());
                    ps.setString(3, Contract.ContractStatus.ACTIVE.name());
                    success = ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            boolean updateSuccess = success;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player employer = plugin.getServer().getPlayer(employerId);
                if (!updateSuccess) {
                    contract.setStatus(Contract.ContractStatus.ACTIVE);
                    if (employer != null) {
                        employer.sendMessage("§cCannot complete this contract.");
                    }
                    return;
                }

                VaultHook economy = plugin.getVaultHook();
                if (economy != null) {
                    org.bukkit.OfflinePlayer builder = plugin.getServer().getOfflinePlayer(contract.getContractorUuid());
                    economy.deposit(builder, contract.getReward());
                    if (contract.getCurrentBudget() > 0) {
                        economy.deposit(employer, contract.getCurrentBudget());
                    }
                }

                if (employer != null) {
                    employer.sendMessage("§aContract #" + contract.getId() + " completed.");
                }
                contracts.remove(contract.getId());
            });
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
        if (contract.getCurrentBudget() < amount) {
            return false;
        }

        contract.setCurrentBudget(contract.getCurrentBudget() - amount);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE contracts SET current_budget = current_budget - ? WHERE id = ? AND current_budget >= ?")) {
                ps.setDouble(1, amount);
                ps.setInt(2, contract.getId());
                ps.setDouble(3, amount);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    plugin.getLogger().warning("Contract budget update failed for id=" + contract.getId());
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        contract.setCurrentBudget(contract.getCurrentBudget() + amount);
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    contract.setCurrentBudget(contract.getCurrentBudget() + amount);
                });
            }
        });
        return true;
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
