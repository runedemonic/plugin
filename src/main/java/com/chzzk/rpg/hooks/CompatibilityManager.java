package com.chzzk.rpg.hooks;

import com.chzzk.rpg.ChzzkRPG;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CompatibilityManager {

    private final ChzzkRPG plugin;
    private final Object worldGuardHook;
    private final boolean worldGuardEnabled;
    private Method worldGuardIsEnabled;
    private Method worldGuardIsCommandAllowed;

    @Getter
    private final boolean luckPermsEnabled;
    @Getter
    private final boolean multiverseEnabled;

    private final Set<String> landClaimWorlds = new HashSet<>();
    private final Set<String> rtpWorlds = new HashSet<>();
    private final Set<String> restrictedCommands = new HashSet<>();

    public CompatibilityManager(ChzzkRPG plugin) {
        this.plugin = plugin;
        this.luckPermsEnabled = plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms");
        this.multiverseEnabled = plugin.getServer().getPluginManager().isPluginEnabled("Multiverse-Core");
        this.worldGuardHook = initWorldGuardHook();
        this.worldGuardEnabled = worldGuardHook != null;
        loadConfig();
        logDetectedPlugins();
    }

    private Object initWorldGuardHook() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            return null;
        }
        try {
            Class<?> hookClass = Class.forName("com.chzzk.rpg.hooks.WorldGuardHook");
            Object hook = hookClass.getConstructor(ChzzkRPG.class).newInstance(plugin);
            worldGuardIsEnabled = hookClass.getMethod("isEnabled");
            worldGuardIsCommandAllowed = hookClass.getMethod("isCommandAllowed", Player.class, Location.class);
            return hook;
        } catch (ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException e) {
            plugin.getLogger().warning("WorldGuard hook failed to initialize: " + e.getMessage());
            return null;
        }
    }

    private void logDetectedPlugins() {
        if (luckPermsEnabled) {
            plugin.getLogger().info("LuckPerms detected. Using LuckPerms for permissions.");
        }
        if (multiverseEnabled) {
            plugin.getLogger().info("Multiverse-Core detected. Land claims can be scoped by world.");
        }
        if (worldGuardEnabled && isWorldGuardHookEnabled()) {
            plugin.getLogger().info("WorldGuard detected. Command restrictions are enabled.");
        }
    }

    private void loadConfig() {
        landClaimWorlds.clear();
        rtpWorlds.clear();
        restrictedCommands.clear();
        List<String> worldList = plugin.getConfig().getStringList("compatibility.multiverse.land-claim-worlds");
        for (String world : worldList) {
            landClaimWorlds.add(world.toLowerCase(Locale.ROOT));
        }
        List<String> rtpWorldList = plugin.getConfig().getStringList("compatibility.multiverse.rtp-worlds");
        for (String world : rtpWorldList) {
            rtpWorlds.add(world.toLowerCase(Locale.ROOT));
        }
        List<String> commandList = plugin.getConfig().getStringList("compatibility.worldguard.restricted-commands");
        for (String command : commandList) {
            restrictedCommands.add(command.toLowerCase(Locale.ROOT));
        }
    }

    public boolean isLandClaimAllowed(World world) {
        if (landClaimWorlds.isEmpty()) {
            return true;
        }
        String worldName = world.getName().toLowerCase(Locale.ROOT);
        return landClaimWorlds.contains(worldName);
    }

    public boolean isCommandRestricted(String command) {
        if (restrictedCommands.isEmpty()) {
            return false;
        }
        return restrictedCommands.contains(command.toLowerCase(Locale.ROOT));
    }

    public boolean isRtpAllowed(World world) {
        if (rtpWorlds.isEmpty()) {
            return true;
        }
        String worldName = world.getName().toLowerCase(Locale.ROOT);
        return rtpWorlds.contains(worldName);
    }

    public boolean isCommandAllowed(Player player) {
        if (!worldGuardEnabled || worldGuardHook == null || worldGuardIsCommandAllowed == null) {
            return true;
        }
        try {
            Object result = worldGuardIsCommandAllowed.invoke(worldGuardHook, player, player.getLocation());
            return result instanceof Boolean && (Boolean) result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("WorldGuard command check failed: " + e.getMessage());
            return true;
        }
    }

    private boolean isWorldGuardHookEnabled() {
        if (!worldGuardEnabled || worldGuardHook == null || worldGuardIsEnabled == null) {
            return false;
        }
        try {
            Object result = worldGuardIsEnabled.invoke(worldGuardHook);
            return result instanceof Boolean && (Boolean) result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("WorldGuard status check failed: " + e.getMessage());
            return false;
        }
    }
}
