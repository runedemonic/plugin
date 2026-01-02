package com.chzzk.rpg;

import org.bukkit.plugin.java.JavaPlugin;
import lombok.Getter;

import com.chzzk.rpg.data.DatabaseManager;
import com.chzzk.rpg.stats.StatsManager;

public class ChzzkRPG extends JavaPlugin {

    @Getter
    private static ChzzkRPG instance;

    @Getter
    private DatabaseManager databaseManager;

    @Getter
    private StatsManager statsManager;

    @Getter
    private com.chzzk.rpg.combat.CooldownManager cooldownManager;

    @Getter
    private com.chzzk.rpg.enhance.EnhanceManager enhanceManager;

    @Getter
    private com.chzzk.rpg.hooks.VaultHook vaultHook;

    @Getter
    private com.chzzk.rpg.jobs.life.BuilderManager builderManager;

    @Getter
    private com.chzzk.rpg.land.LandManager landManager;

    @Getter
    private com.chzzk.rpg.contracts.ContractManager contractManager;

    @Getter
    private com.chzzk.rpg.guilds.GuildManager guildManager;

    @Getter
    private com.chzzk.rpg.items.SoulboundListener soulboundListener;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        com.chzzk.rpg.utils.RpgKeys.init(this);

        // Initialize Database
        this.databaseManager = new DatabaseManager(this);

        // Initialize Modules
        this.vaultHook = new com.chzzk.rpg.hooks.VaultHook(this); // Init Vault first for Economy
        this.statsManager = new StatsManager(this);
        this.cooldownManager = new com.chzzk.rpg.combat.CooldownManager();
        this.enhanceManager = new com.chzzk.rpg.enhance.EnhanceManager(this);
        this.builderManager = new com.chzzk.rpg.jobs.life.BuilderManager(this);
        this.landManager = new com.chzzk.rpg.land.LandManager(this);
        this.contractManager = new com.chzzk.rpg.contracts.ContractManager(this);
        this.guildManager = new com.chzzk.rpg.guilds.GuildManager(this);

        new com.chzzk.rpg.gui.GuiListener(this);
        new com.chzzk.rpg.combat.DamageListener(this);
        new com.chzzk.rpg.combat.RangedListener(this);
        new com.chzzk.rpg.land.LandListener(this);
        new com.chzzk.rpg.chef.ChefListener(this);
        this.soulboundListener = new com.chzzk.rpg.items.SoulboundListener(this);

        // Buff Cleanup Task (Every 1s)
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
                com.chzzk.rpg.stats.PlayerProfile profile = statsManager.getProfile(p);
                if (profile != null) {
                    profile.cleanupBuffs();
                }
            }
        }, 20L, 20L);

        getCommand("rpg").setExecutor(new com.chzzk.rpg.commands.MainCommand());
        getCommand("stats").setExecutor(new com.chzzk.rpg.commands.StatsCommand());
        getCommand("rpgadmin").setExecutor(new com.chzzk.rpg.commands.RpgAdminCommand());
        getCommand("enhance").setExecutor(new com.chzzk.rpg.commands.EnhanceCommand());
        getCommand("job").setExecutor(new com.chzzk.rpg.commands.JobCommand());
        getCommand("builder").setExecutor(new com.chzzk.rpg.commands.BuilderCommand());
        getCommand("land").setExecutor(new com.chzzk.rpg.commands.LandCommand());
        getCommand("contract").setExecutor(new com.chzzk.rpg.commands.ContractCommand());
        getCommand("repair").setExecutor(new com.chzzk.rpg.commands.RepairCommand());
        getCommand("guild").setExecutor(new com.chzzk.rpg.commands.GuildCommand());

        getLogger().info("ChzzkRPG has been enabled!");
    }

    @Override
    public void onDisable() {
        // Shutdown logic
        if (soulboundListener != null) {
            soulboundListener.flushPendingReturns();
        }
        if (statsManager != null) {
            statsManager.saveAllProfiles(false);
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("ChzzkRPG has been disabled!");
    }
}
