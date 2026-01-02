package com.chzzk.rpg.commands;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.land.Claim;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LandCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players.");
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            Claim claim = ChzzkRPG.getInstance().getLandManager().getClaim(chunk);
            if (claim == null) {
                player.sendMessage("§aThis land is WILDERNESS (Unclaimed).");
            } else {
                player.sendMessage("§6--- Land Info ---");
                if (claim.getOwnerType() == Claim.ClaimType.GUILD) {
                    com.chzzk.rpg.guilds.GuildManager gm = ChzzkRPG.getInstance().getGuildManager();
                    if (gm == null) {
                        player.sendMessage("§eOwner: §fUnknown Guild");
                        player.sendMessage("§eType: §f" + claim.getOwnerType());
                        return true;
                    }
                    try {
                        int guildId = Integer.parseInt(claim.getOwnerId());
                        com.chzzk.rpg.guilds.Guild guild = gm.getGuildById(guildId);
                        String guildName = guild != null ? guild.getName() : "Unknown Guild";
                        player.sendMessage("§eOwner: §f" + guildName + " (#" + guildId + ")");
                    } catch (NumberFormatException e) {
                        player.sendMessage("§eOwner: §f" + claim.getOwnerId());
                    }
                } else {
                    try {
                        java.util.UUID ownerUuid = java.util.UUID.fromString(claim.getOwnerId());
                        String ownerName = org.bukkit.Bukkit.getOfflinePlayer(ownerUuid).getName();
                        if (ownerName != null) {
                            player.sendMessage("§eOwner: §f" + ownerName);
                        } else {
                            player.sendMessage("§eOwner: §f" + claim.getOwnerId());
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§eOwner: §f" + claim.getOwnerId());
                    }
                }
                player.sendMessage("§eType: §f" + claim.getOwnerType());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("claim")) {
            ChzzkRPG.getInstance().getLandManager().buyClaim(player, chunk);
            return true;
        }

        if (args[0].equalsIgnoreCase("guildclaim")) {
            if (!player.hasPermission("rpg.land.guildclaim")) {
                player.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("Usage: /land guildclaim <guildId>");
                return true;
            }
            try {
                int guildId = Integer.parseInt(args[1]);
                com.chzzk.rpg.guilds.GuildManager gm = ChzzkRPG.getInstance().getGuildManager();
                if (gm == null) {
                    player.sendMessage("§cGuild system not available.");
                    return true;
                }
                com.chzzk.rpg.guilds.Guild guild = gm.getGuild(player);
                if (guild == null || guild.getId() != guildId) {
                    player.sendMessage("§cYou are not a member of that guild.");
                    return true;
                }
                com.chzzk.rpg.guilds.GuildMember member = guild.getMember(player.getUniqueId());
                if (member == null || member.getRole() == com.chzzk.rpg.guilds.GuildMember.Role.MEMBER) {
                    player.sendMessage("§cOnly guild officers can claim land.");
                    return true;
                }
                if (gm.getGuildById(guildId) == null) {
                    player.sendMessage("§cGuild not found.");
                    return true;
                }
                ChzzkRPG.getInstance().getLandManager().buyGuildClaim(player, chunk, guildId);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid guild ID.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("unclaim")) {
            ChzzkRPG.getInstance().getLandManager().unclaim(player, chunk);
            return true;
        }

        return true;
    }
}
