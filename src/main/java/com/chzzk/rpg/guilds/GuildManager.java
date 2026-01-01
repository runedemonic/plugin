package com.chzzk.rpg.guilds;

import com.chzzk.rpg.ChzzkRPG;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class GuildManager {

    private final ChzzkRPG plugin;
    private final Map<Integer, Guild> guildIdMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerGuildMap = new ConcurrentHashMap<>();

    public GuildManager(ChzzkRPG plugin) {
        this.plugin = plugin;
        loadGuilds();
    }

    private void loadGuilds() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // 1. Load Guilds
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM guilds");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    UUID leader = UUID.fromString(rs.getString("leader_uuid"));
                    Guild guild = new Guild(id, name, leader);
                    guild.setLevel(rs.getInt("level"));
                    guild.setExp(rs.getDouble("exp"));
                    guildIdMap.put(id, guild);
                }

                // 2. Load Members
                ps = conn.prepareStatement("SELECT * FROM guild_members");
                rs = ps.executeQuery();
                while (rs.next()) {
                    int guildId = rs.getInt("guild_id");
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    GuildMember.Role role = GuildMember.Role.valueOf(rs.getString("role"));

                    Guild guild = guildIdMap.get(guildId);
                    if (guild != null) {
                        guild.addMember(new GuildMember(uuid, role));
                        playerGuildMap.put(uuid, guildId);
                    }
                }
                plugin.getLogger().info("Loaded " + guildIdMap.size() + " guilds.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Guild getGuild(Player player) {
        return getGuild(player.getUniqueId());
    }

    public Guild getGuild(UUID uuid) {
        Integer id = playerGuildMap.get(uuid);
        return id != null ? guildIdMap.get(id) : null;
    }

    public Guild getGuildById(int id) {
        return guildIdMap.get(id);
    }

    public void createGuild(Player leader, String name) {
        if (getGuild(leader) != null) {
            leader.sendMessage("§cYou are already in a guild.");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO guilds (name, leader_uuid) VALUES (?, ?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setString(2, leader.getUniqueId().toString());
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    Guild guild = new Guild(id, name, leader.getUniqueId());
                    guild.addMember(new GuildMember(leader.getUniqueId(), GuildMember.Role.LEADER));

                    guildIdMap.put(id, guild);
                    playerGuildMap.put(leader.getUniqueId(), id);

                    // Add leader to members table
                    addMemberToDB(id, leader.getUniqueId(), GuildMember.Role.LEADER);

                    leader.sendMessage("§aGuild '" + name + "' created!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                leader.sendMessage("§cFailed to create guild (Name taken?).");
            }
        });
    }

    private final Map<UUID, Integer> pendingInvites = new ConcurrentHashMap<>();

    public void invitePlayer(Player inviter, Player target) {
        Guild guild = getGuild(inviter);
        if (guild == null) {
            inviter.sendMessage("§cYou are not in a guild.");
            return;
        }
        // Check permissions (Leader/Officer)
        GuildMember member = guild.getMember(inviter.getUniqueId());
        if (member.getRole() == GuildMember.Role.MEMBER) {
            inviter.sendMessage("§cOnly officers can invite.");
            return;
        }

        if (getGuild(target) != null) {
            inviter.sendMessage("§cTarget is already in a guild.");
            return;
        }

        pendingInvites.put(target.getUniqueId(), guild.getId());
        target.sendMessage("§aYou have been invited to join §e" + guild.getName());
        target.sendMessage("§aType §6/guild join §ato accept.");
        inviter.sendMessage("§aInvited " + target.getName());
    }

    public void joinGuild(Player player) {
        Integer guildId = pendingInvites.remove(player.getUniqueId());
        if (guildId == null) {
            player.sendMessage("§cNo pending invites.");
            return;
        }

        Guild guild = guildIdMap.get(guildId);
        if (guild == null) {
            player.sendMessage("§cGuild no longer exists.");
            return;
        }

        // Add to memory
        guild.addMember(new GuildMember(player.getUniqueId(), GuildMember.Role.MEMBER));
        playerGuildMap.put(player.getUniqueId(), guildId);

        // Add to DB
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            addMemberToDB(guildId, player.getUniqueId(), GuildMember.Role.MEMBER);
        });

        player.sendMessage("§aJoined Guild: " + guild.getName());
    }

    private void addMemberToDB(int guildId, UUID uuid, GuildMember.Role role) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement ps = conn
                    .prepareStatement("INSERT INTO guild_members (guild_id, player_uuid, role) VALUES (?, ?, ?)");
            ps.setInt(1, guildId);
            ps.setString(2, uuid.toString());
            ps.setString(3, role.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
