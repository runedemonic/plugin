package com.chzzk.rpg.commands;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.jobs.CombatJob;
import com.chzzk.rpg.jobs.LifeJob;
import com.chzzk.rpg.stats.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JobCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
        PlayerProfile profile = ChzzkRPG.getInstance().getStatsManager().getProfile(player);

        if (profile == null) {
            sender.sendMessage("§cProfile error.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            showInfo(player, profile);
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /job join <jobName>");
                return true;
            }

            String jobName = args[1].toUpperCase();

            // Try Combat Job
            try {
                CombatJob cJob = CombatJob.valueOf(jobName);
                profile.setCombatJob(cJob);
                profile.recalculateStats();
                sender.sendMessage("§aYou are now a " + cJob.getDisplayName() + "!");
                return true;
            } catch (IllegalArgumentException ignored) {
            }

            // Try Life Job
            try {
                LifeJob lJob = LifeJob.valueOf(jobName);
                profile.setLifeJob(lJob);
                sender.sendMessage("§aYou are now a " + lJob.getDisplayName() + "!");
                return true;
            } catch (IllegalArgumentException ignored) {
            }

            sender.sendMessage("§cJob not found: " + jobName);
            return true;
        }

        return true;
    }

    private void showInfo(Player player, PlayerProfile profile) {
        player.sendMessage("§6=== [ Your Jobs ] ===");
        player.sendMessage("§fCombat: §e" + profile.getCombatJob().getDisplayName());
        player.sendMessage("§fLife: §e" + profile.getLifeJob().getDisplayName());
        player.sendMessage("§7Type /job join <job> to switch.");
    }
}
