package com.chzzk.rpg.data;

import com.chzzk.rpg.ChzzkRPG;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.Getter;

public class DatabaseManager {

    private final ChzzkRPG plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ChzzkRPG plugin) {
        this.plugin = plugin;
        connect();
        initSchema();
    }

    private void connect() {
        String type = plugin.getConfig().getString("database.type", "sqlite");
        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("mysql")) {
            String host = plugin.getConfig().getString("database.host");
            int port = plugin.getConfig().getInt("database.port");
            String db = plugin.getConfig().getString("database.database");
            String user = plugin.getConfig().getString("database.username");
            String pass = plugin.getConfig().getString("database.password");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true");
            config.setUsername(user);
            config.setPassword(pass);
        } else {
            // SQLite
            File file = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.file", "database.db"));
            // Ensure directory exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        }

        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setConnectionTimeout(plugin.getConfig().getLong("database.connection-timeout", 30000));

        this.dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Database connected: " + type);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void initSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Initialize Tables based on plan.md

            // 1. Players
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "combat_job VARCHAR(32), " +
                    "life_job VARCHAR(32), " +
                    "stat_points INT DEFAULT 0, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 2. Player Stats
            stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "atk DOUBLE DEFAULT 0, " +
                    "def DOUBLE DEFAULT 0, " +
                    "hp DOUBLE DEFAULT 0, " +
                    "crit DOUBLE DEFAULT 0, " +
                    "crit_dmg DOUBLE DEFAULT 0, " +
                    "pen DOUBLE DEFAULT 0, " +
                    "FOREIGN KEY (uuid) REFERENCES players(uuid)" +
                    ");");

            // 3. Life Jobs
            stmt.execute("CREATE TABLE IF NOT EXISTS life_jobs (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "blacksmith_lv INT DEFAULT 1, " +
                    "blacksmith_exp DOUBLE DEFAULT 0, " +
                    "chef_lv INT DEFAULT 1, " +
                    "chef_exp DOUBLE DEFAULT 0, " +
                    "builder_lv INT DEFAULT 1, " +
                    "builder_exp DOUBLE DEFAULT 0, " +
                    "FOREIGN KEY (uuid) REFERENCES players(uuid)" +
                    ");");

            // 4. Claims
            stmt.execute("CREATE TABLE IF NOT EXISTS claims (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " + // SQLite specific, Use AUTO_INCREMENT for MySQL
                                                               // compatibility adjustment later if needed
                    "world_uuid VARCHAR(36), " +
                    "chunk_x INT, " +
                    "chunk_z INT, " +
                    "owner_type VARCHAR(16), " + // PERSONAL, GUILD
                    "owner_id VARCHAR(36), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(world_uuid, chunk_x, chunk_z)" +
                    ");");

            // 5. Contracts
            stmt.execute("CREATE TABLE IF NOT EXISTS contracts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "employer_uuid VARCHAR(36), " +
                    "contractor_uuid VARCHAR(36), " +
                    "world_uuid VARCHAR(36), " +
                    "chunk_x INT, " +
                    "chunk_z INT, " +
                    "reward DOUBLE DEFAULT 0, " +
                    "budget DOUBLE DEFAULT 0, " +
                    "current_budget DOUBLE DEFAULT 0, " +
                    "status VARCHAR(16), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 6. Guilds
            stmt.execute("CREATE TABLE IF NOT EXISTS guilds (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(32) UNIQUE, " +
                    "leader_uuid VARCHAR(36), " +
                    "level INT DEFAULT 1, " +
                    "exp DOUBLE DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 7. Guild Members
            stmt.execute("CREATE TABLE IF NOT EXISTS guild_members (" +
                    "guild_id INTEGER, " +
                    "player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "role VARCHAR(16), " + // LEADER, OFFICER, MEMBER
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
