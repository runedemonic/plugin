package com.chzzk.rpg.chef;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.stats.PlayerProfile;
import com.chzzk.rpg.stats.PlayerStats;
import com.chzzk.rpg.stats.StatType;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ChefListener implements Listener {

    private final ChzzkRPG plugin;
    // Keys
    private final NamespacedKey FOOD_KEY;
    private final NamespacedKey STAT_KEY; // "ATK:10"
    private final NamespacedKey DURATION_KEY; // Seconds

    public ChefListener(ChzzkRPG plugin) {
        this.plugin = plugin;
        this.FOOD_KEY = new NamespacedKey(plugin, "rpg_food");
        this.STAT_KEY = new NamespacedKey(plugin, "food_stat");
        this.DURATION_KEY = new NamespacedKey(plugin, "food_duration");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!item.hasItemMeta())
            return;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(FOOD_KEY, PersistentDataType.STRING))
            return;

        String statParams = pdc.get(STAT_KEY, PersistentDataType.STRING); // e.g. "ATK:10"
        Integer duration = pdc.get(DURATION_KEY, PersistentDataType.INTEGER);

        if (statParams != null && duration != null) {
            PlayerProfile profile = plugin.getStatsManager().getProfile(event.getPlayer());
            if (profile != null) {
                PlayerStats buffStats = new PlayerStats();
                String[] parts = statParams.split(":");
                try {
                    StatType type = StatType.valueOf(parts[0]);
                    double val = Double.parseDouble(parts[1]);
                    buffStats.set(type, val);

                    profile.addBuff(new Buff(item.getType().name(), buffStats, duration));
                    event.getPlayer().sendMessage("§aBuff Applied: " + type + " +" + val + " (" + duration + "s)");
                } catch (Exception e) {
                    // Invalid format
                }
            }
        }
    }
}
