package com.chzzk.rpg.hooks;

import com.chzzk.rpg.ChzzkRPG;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private final ChzzkRPG plugin;
    private Economy economy = null;

    public VaultHook(ChzzkRPG plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isEconomyEnabled() {
        return economy != null;
    }

    public boolean hasMoney(org.bukkit.OfflinePlayer player, double amount) {
        if (economy == null) {
            // If no economy plugin, allow the action (for testing)
            return true;
        }
        return economy.has(player, amount);
    }

    public void withdraw(org.bukkit.OfflinePlayer player, double amount) {
        if (economy == null)
            return;
        economy.withdrawPlayer(player, amount);
    }

    public void deposit(org.bukkit.OfflinePlayer player, double amount) {
        if (economy == null)
            return;
        economy.depositPlayer(player, amount);
    }
}
