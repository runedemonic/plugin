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
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerProfile profile = new PlayerProfile(uuid, name);

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Load basic info
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
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
                            loadLifeJobs(conn, profile);

                        } else {
                            // Create new
                            createNewProfile(conn, profile);
                        }
                    }
                }

                // Recalculate generic stats
                profile.recalculateStats();

                profiles.put(uuid, profile);

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load profile for " + name);
                e.printStackTrace();
            }
        });
    }

    private void createNewProfile(Connection conn, PlayerProfile profile) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO players (uuid, name, combat_job, life_job, stat_points) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, profile.getUuid().toString());
            ps.setString(2, profile.getName());
            ps.setString(3, profile.getCombatJob().name());
            ps.setString(4, profile.getLifeJob().name());
            ps.setInt(5, profile.getStatPoints());
            ps.executeUpdate();
        }

        try (PreparedStatement psStat = conn.prepareStatement(
                "INSERT INTO player_stats (uuid) VALUES (?)")) {
            psStat.setString(1, profile.getUuid().toString());
            psStat.executeUpdate();
        }

        try (PreparedStatement psLife = conn.prepareStatement(
                "INSERT INTO life_jobs (uuid, blacksmith_lv, blacksmith_exp, chef_lv, chef_exp, builder_lv, builder_exp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            psLife.setString(1, profile.getUuid().toString());
            psLife.setInt(2, profile.getBlacksmithLevel());
            psLife.setDouble(3, profile.getBlacksmithExp());
            psLife.setInt(4, profile.getChefLevel());
            psLife.setDouble(5, profile.getChefExp());
            psLife.setInt(6, profile.getBuilderLevel());
            psLife.setDouble(7, profile.getBuilderExp());
            psLife.executeUpdate();
        }
    }

    private void loadStats(Connection conn, PlayerProfile profile) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_stats WHERE uuid = ?")) {
            ps.setString(1, profile.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
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
        }
    }

    private void loadLifeJobs(Connection conn, PlayerProfile profile) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM life_jobs WHERE uuid = ?")) {
            ps.setString(1, profile.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    profile.setBlacksmithLevel(rs.getInt("blacksmith_lv"));
                    profile.setBlacksmithExp(rs.getDouble("blacksmith_exp"));
                    profile.setChefLevel(rs.getInt("chef_lv"));
                    profile.setChefExp(rs.getDouble("chef_exp"));
                    profile.setBuilderLevel(rs.getInt("builder_lv"));
                    profile.setBuilderExp(rs.getDouble("builder_exp"));
                    return;
                }
            }
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO life_jobs (uuid, blacksmith_lv, blacksmith_exp, chef_lv, chef_exp, builder_lv, builder_exp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, profile.getUuid().toString());
            insert.setInt(2, profile.getBlacksmithLevel());
            insert.setDouble(3, profile.getBlacksmithExp());
            insert.setInt(4, profile.getChefLevel());
            insert.setDouble(5, profile.getChefExp());
            insert.setInt(6, profile.getBuilderLevel());
            insert.setDouble(7, profile.getBuilderExp());
            insert.executeUpdate();
        }
    }

    public void saveProfile(UUID uuid) {
        PlayerProfile profile = profiles.get(uuid);
        if (profile == null)
            return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE players SET stat_points=?, combat_job=?, life_job=? WHERE uuid=?")) {
                    ps.setInt(1, profile.getStatPoints());
                    ps.setString(2, profile.getCombatJob().name());
                    ps.setString(3, profile.getLifeJob().name());
                    ps.setString(4, uuid.toString());
                    ps.executeUpdate();
                }

                try (PreparedStatement psStats = conn.prepareStatement(
                        "UPDATE player_stats SET atk=?, def=?, hp=?, crit=?, crit_dmg=?, pen=? WHERE uuid=?")) {
                    PlayerStats stats = profile.getBaseStats();
                    psStats.setDouble(1, stats.get(StatType.ATK));
                    psStats.setDouble(2, stats.get(StatType.DEF));
                    psStats.setDouble(3, stats.get(StatType.HP));
                    psStats.setDouble(4, stats.get(StatType.CRIT));
                    psStats.setDouble(5, stats.get(StatType.CRIT_DMG));
                    psStats.setDouble(6, stats.get(StatType.PEN));
                    psStats.setString(7, uuid.toString());
                    psStats.executeUpdate();
                }

                try (PreparedStatement psLife = conn.prepareStatement(
                        "UPDATE life_jobs SET blacksmith_lv=?, blacksmith_exp=?, chef_lv=?, chef_exp=?, builder_lv=?, builder_exp=? WHERE uuid=?")) {
                    psLife.setInt(1, profile.getBlacksmithLevel());
                    psLife.setDouble(2, profile.getBlacksmithExp());
                    psLife.setInt(3, profile.getChefLevel());
                    psLife.setDouble(4, profile.getChefExp());
                    psLife.setInt(5, profile.getBuilderLevel());
                    psLife.setDouble(6, profile.getBuilderExp());
                    psLife.setString(7, uuid.toString());
                    psLife.executeUpdate();
                }

    public void saveAllProfiles() {
        saveAllProfiles(true);
    }

    public void saveAllProfiles(boolean async) {
        Runnable task = () -> {
            int savedCount = 0;
            for (Map.Entry<UUID, PlayerProfile> entry : profiles.entrySet()) {
                saveProfileSync(entry.getKey(), entry.getValue());
                savedCount++;
            }
            plugin.getLogger().info("Saved " + savedCount + " profiles.");
        };

        if (async) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            task.run();
        }
    }

    private void saveProfileSync(UUID uuid, PlayerProfile profile) {
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

            PreparedStatement psLife = conn.prepareStatement(
                    "UPDATE life_jobs SET blacksmith_lv=?, blacksmith_exp=?, chef_lv=?, chef_exp=?, builder_lv=?, builder_exp=? WHERE uuid=?");
            psLife.setInt(1, profile.getBlacksmithLevel());
            psLife.setDouble(2, profile.getBlacksmithExp());
            psLife.setInt(3, profile.getChefLevel());
            psLife.setDouble(4, profile.getChefExp());
            psLife.setInt(5, profile.getBuilderLevel());
            psLife.setDouble(6, profile.getBuilderExp());
            psLife.setString(7, uuid.toString());
            psLife.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save profile for " + uuid);
            e.printStackTrace();
        }
    }
}
