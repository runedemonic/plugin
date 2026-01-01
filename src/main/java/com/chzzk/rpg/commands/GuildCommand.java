package com.chzzk.rpg.commands;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.guilds.Guild;
import com.chzzk.rpg.guilds.GuildManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuildCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players.");
            return true;
        }

        Player player = (Player) sender;
        GuildManager gm = ChzzkRPG.getInstance().getGuildManager();

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            Guild guild = gm.getGuild(player);
            if (guild == null) {
                player.sendMessage("§cYou are not in a guild.");
            } else {
                player.sendMessage("§6=== " + guild.getName() + " ===");
                player.sendMessage("§eLevel: " + guild.getLevel() + " (Exp: " + guild.getExp() + ")");
                player.sendMessage("§eMembers: " + guild.getMembers().size());
                player.sendMessage("§eLeader: " + Bukkit.getOfflinePlayer(guild.getLeaderUuid()).getName());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /guild create <name>");
                return true;
            }
            gm.createGuild(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("invite")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /guild invite <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cPlayer not found.");
                return true;
            }
            gm.invitePlayer(player, target);
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            gm.joinGuild(player);
            return true;
        }

        return true;
    }
}
