package com.chzzk.rpg.jobs.life;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.hooks.VaultHook;
import com.chzzk.rpg.jobs.LifeJob;
import com.chzzk.rpg.stats.PlayerProfile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BuilderManager implements Listener {

    private final ChzzkRPG plugin;
    private final Set<UUID> activeBuilders = new HashSet<>();
    private final Map<UUID, GameMode> previousGameModes = new ConcurrentHashMap<>();

    // Simple Cost Map (Should be config)
    private final double DEFAULT_COST = 10.0;

    public BuilderManager(ChzzkRPG plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void toggleBuilderMode(Player player) {
        PlayerProfile profile = plugin.getStatsManager().getProfile(player);
        if (profile == null || profile.getLifeJob() != LifeJob.BUILDER) {
            player.sendMessage("§cYou are not a Builder!");
            return;
        }

        if (activeBuilders.contains(player.getUniqueId())) {
            disableBuilderMode(player);
        } else {
            enableBuilderMode(player);
        }
    }

    public void enableBuilderMode(Player player) {
        activeBuilders.add(player.getUniqueId());
        previousGameModes.putIfAbsent(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.CREATIVE);
        player.sendMessage("§aBuilder Mode ON. Blocks will cost money.");
    }

    public void disableBuilderMode(Player player) {
        activeBuilders.remove(player.getUniqueId());
        GameMode previousMode = previousGameModes.remove(player.getUniqueId());
        player.setGameMode(previousMode != null ? previousMode : GameMode.SURVIVAL);
        player.sendMessage("§cBuilder Mode OFF.");
    }

    public boolean isBuilderMode(Player player) {
        return activeBuilders.contains(player.getUniqueId());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isBuilderMode(player))
            return;

        // Cost Calculation
        double cost = getBlockCost(event.getBlock().getType());

        // 1. Check for Active Contract
        if (plugin.getContractManager() != null) {
            com.chzzk.rpg.contracts.Contract contract = plugin.getContractManager().getActiveContract(player,
                    event.getBlock().getChunk());
            if (contract != null) {
                // Deduct from Contract Budget
                if (plugin.getContractManager().spendBudget(contract, cost)) {
                    player.sendActionBar(Component.text("§aContract Build: " + event.getBlock().getType()
                            + " (Budget: $" + contract.getCurrentBudget() + ")"));
                    return; // Success, paid by budget
                } else {
                    player.sendActionBar(Component.text("§cContract Budget Exceeded! Need $" + cost));
                    // Fallthrough to personal wallet? Or block?
                    // Strict: Block
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 2. Personal Wallet (Fallback)
        VaultHook economy = plugin.getVaultHook();
        if (economy != null) {
            if (!economy.hasMoney(player, cost)) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("§cNot enough money! Need $" + cost));
                return;
            }

            economy.withdraw(player, cost);
            player.sendActionBar(Component.text("§aPlaced " + event.getBlock().getType() + " (-$" + cost + ")"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isBuilderMode(player))
            return;

        // Prevent drops in builder mode to avoid duplicating items
        event.setDropItems(false);
        event.setExpToDrop(0);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (activeBuilders.contains(event.getPlayer().getUniqueId())) {
            disableBuilderMode(event.getPlayer());
        }
    }

    private double getBlockCost(Material material) {
        // TODO: Load from config
        if (material == Material.DIAMOND_BLOCK)
            return 500.0;
        if (material == Material.GOLD_BLOCK)
            return 200.0;
        if (material == Material.IRON_BLOCK)
            return 100.0;
        return DEFAULT_COST;
    }
}
