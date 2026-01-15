package com.chzzk.rpg.hooks;

import com.chzzk.rpg.ChzzkRPG;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    private static final String COMMAND_FLAG_NAME = "chzzk-rpg-commands";

    private final ChzzkRPG plugin;
    private final boolean enabled;
    private StateFlag commandFlag;

    public WorldGuardHook(ChzzkRPG plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard");
        if (enabled) {
            registerFlags();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag(COMMAND_FLAG_NAME, true);
            registry.register(flag);
            commandFlag = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(COMMAND_FLAG_NAME);
            if (existing instanceof StateFlag) {
                commandFlag = (StateFlag) existing;
            } else {
                plugin.getLogger().warning("WorldGuard flag conflict: " + COMMAND_FLAG_NAME);
            }
        }
    }

    public boolean isCommandAllowed(Player player, Location location) {
        if (!enabled || commandFlag == null) {
            return true;
        }
        WorldGuardPlugin wg = WorldGuardPlugin.inst();
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        StateFlag.State state = query.queryState(BukkitAdapter.adapt(location), wg.wrapPlayer(player), commandFlag);
        return state != StateFlag.State.DENY;
    }

    /**
     * Checks if a player can teleport to a location (checks ENTRY and BUILD flags).
     * Returns true if no WorldGuard protection or if allowed.
     */
    public boolean canTeleportTo(Player player, Location location) {
        if (!enabled) {
            return true;
        }
        WorldGuardPlugin wg = WorldGuardPlugin.inst();
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

        // Check if player can enter this region
        StateFlag.State entryState = query.queryState(
            BukkitAdapter.adapt(location),
            wg.wrapPlayer(player),
            Flags.ENTRY
        );
        if (entryState == StateFlag.State.DENY) {
            return false;
        }

        // Also check BUILD flag - if they can't build, likely a protected area
        StateFlag.State buildState = query.queryState(
            BukkitAdapter.adapt(location),
            wg.wrapPlayer(player),
            Flags.BUILD
        );
        return buildState != StateFlag.State.DENY;
    }

    /**
     * Checks if a location is in any WorldGuard region (regardless of flags).
     */
    public boolean isInProtectedRegion(Location location) {
        if (!enabled) {
            return false;
        }
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.getApplicableRegions(BukkitAdapter.adapt(location)).size() > 0;
    }
}
