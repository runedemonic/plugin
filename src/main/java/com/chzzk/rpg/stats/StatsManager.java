package com.chzzk.rpg.stats;

import com.chzzk.rpg.ChzzkRPG;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StatsManager implements Listener {

    private final ChzzkRPG plugin;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    public StatsManager(ChzzkRPG plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    public PlayerProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadProfile(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveProfile(event.getPlayer().getUniqueId());
        profiles.remove(event.getPlayer().getUniqueId());
    }

    public void loadProfile(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = player.getUniqueId();
            PlayerProfile profile = new PlayerProfile(uuid, player.getName());

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Load basic info
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    profile.setStatPoints(rs.getInt("stat_points"));

                    try {
                        String combatJobStr = rs.getString("combat_job");
                        profile.setCombatJob(com.chzzk.rpg.jobs.CombatJob.valueOf(combatJobStr));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        profile.setCombatJob(com.chzzk.rpg.jobs.CombatJob.NONE);
                    }

                    try {
                        String lifeJobStr = rs.getString("life_job");
                        profile.setLifeJob(com.chzzk.rpg.jobs.LifeJob.valueOf(lifeJobStr));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        profile.setLifeJob(com.chzzk.rpg.jobs.LifeJob.NONE);
                    }

                    // Load Stats
                    loadStats(conn, profile);

                } else {
                    // Create new
                    createNewProfile(conn, profile);
                }

                // Recalculate generic stats
                profile.recalculateStats();

                profiles.put(uuid, profile);

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load profile for " + player.getName());
                e.printStackTrace();
            }
        });
    }

    private void createNewProfile(Connection conn, PlayerProfile profile) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO players (uuid, name, combat_job, life_job, stat_points) VALUES (?, ?, ?, ?, ?)");
        ps.setString(1, profile.getUuid().toString());
        ps.setString(2, profile.getName());
        ps.setString(3, profile.getCombatJob().name());
        ps.setString(4, profile.getLifeJob().name());
        ps.setInt(5, profile.getStatPoints());
        ps.executeUpdate();

        PreparedStatement psStat = conn.prepareStatement(
                "INSERT INTO player_stats (uuid) VALUES (?)");
        psStat.setString(1, profile.getUuid().toString());
        psStat.executeUpdate();
    }

    private void loadStats(Connection conn, PlayerProfile profile) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_stats WHERE uuid = ?");
        ps.setString(1, profile.getUuid().toString());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            PlayerStats stats = profile.getBaseStats();
            stats.set(StatType.ATK, rs.getDouble("atk"));
            stats.set(StatType.DEF, rs.getDouble("def"));
            stats.set(StatType.HP, rs.getDouble("hp"));
            stats.set(StatType.CRIT, rs.getDouble("crit"));
            stats.set(StatType.CRIT_DMG, rs.getDouble("crit_dmg"));
            stats.set(StatType.PEN, rs.getDouble("pen"));
        }
    }

    public void saveProfile(UUID uuid) {
        PlayerProfile profile = profiles.get(uuid);
        if (profile == null)
            return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE players SET stat_points=?, combat_job=?, life_job=? WHERE uuid=?");
                ps.setInt(1, profile.getStatPoints());
                ps.setString(2, profile.getCombatJob().name());
                ps.setString(3, profile.getLifeJob().name());
                ps.setString(4, uuid.toString());
                ps.executeUpdate();

                PreparedStatement psStats = conn.prepareStatement(
                        "UPDATE player_stats SET atk=?, def=?, hp=?, crit=?, crit_dmg=?, pen=? WHERE uuid=?");
                PlayerStats stats = profile.getBaseStats();
                psStats.setDouble(1, stats.get(StatType.ATK));
                psStats.setDouble(2, stats.get(StatType.DEF));
                psStats.setDouble(3, stats.get(StatType.HP));
                psStats.setDouble(4, stats.get(StatType.CRIT));
                psStats.setDouble(5, stats.get(StatType.CRIT_DMG));
                psStats.setDouble(6, stats.get(StatType.PEN));
                psStats.setString(7, uuid.toString());
                psStats.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save profile for " + uuid);
                e.printStackTrace();
            }
        });
    }
}
