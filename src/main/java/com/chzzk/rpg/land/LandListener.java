package com.chzzk.rpg.land;

import com.chzzk.rpg.ChzzkRPG;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class LandListener implements Listener {

    private final ChzzkRPG plugin;

    public LandListener(ChzzkRPG plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean canModify(Player player, Chunk chunk) {
        if (player.isOp())
            return true;

        Claim claim = plugin.getLandManager().getClaim(chunk);
        if (claim == null)
            return true; // Wilderness (Unclaimed) - Allow or Deny based on config? Assuming Allow for
                         // now.

        // Personal Claim
        if (claim.getOwnerType() == Claim.ClaimType.PERSONAL) {
            if (claim.getOwnerId().equals(player.getUniqueId().toString()))
                return true;
        }

        // Contract Check
        if (plugin.getContractManager() != null) {
            com.chzzk.rpg.contracts.Contract c = plugin.getContractManager().getActiveContract(player, chunk);
            if (c != null)
                return true;
        }

        // Guild Check
        if (claim.getOwnerType() == Claim.ClaimType.GUILD && plugin.getGuildManager() != null) {
            com.chzzk.rpg.guilds.Guild guild = plugin.getGuildManager().getGuild(player);
            if (guild != null && claim.getOwnerId().equals(String.valueOf(guild.getId()))) {
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot build here.");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot build here.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;
        if (!canModify(event.getPlayer(), event.getClickedBlock().getChunk())) {
            // Allow pressure plates / doors? Maybe. For now stricter.
            // event.setCancelled(true);
            // Interaction logic is complex (chest access vs usage).
            // Basic block interaction prevention
            if (event.getAction().isRightClick() && event.getClickedBlock().getType().isInteractable()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cYou cannot interact with this claim.");
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot place liquids here.");
        }
    }

    @EventHandler
    public void onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot take liquids here.");
        }
    }

    @EventHandler
    public void onExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        // Prevent explosion damage to claimed blocks if not configurable
        // Simple Logic: If any block in list is in claimed and owner != source?
        // Too complex for simple logic. For now, disable explosion block breaking
        // globally in claimed lands or let it happen?
        // Let's just remove blocks from the list that are in claimed chunks to be safe.

        event.blockList().removeIf(block -> {
            Claim claim = plugin.getLandManager().getClaim(block.getChunk());
            return claim != null; // Protected
        });
    }
}
