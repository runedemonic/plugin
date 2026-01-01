package com.chzzk.rpg.commands;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.stats.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RpgAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("rpg.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0)
            return false;

        if (args[0].equalsIgnoreCase("reload")) {
            ChzzkRPG.getInstance().reloadConfig();
            sender.sendMessage("§aConfig reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setpoints")) {
            // /rpgadmin setpoints <player> <amount>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin setpoints <player> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[2]);
                PlayerProfile profile = ChzzkRPG.getInstance().getStatsManager().getProfile(target);
                if (profile != null) {
                    profile.setStatPoints(amount);
                    sender.sendMessage("§aSet " + target.getName() + "'s stat points to " + amount);
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("giveweapon")) {
            // /rpgadmin giveweapon <player> <atk>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin giveweapon <player> <atk>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            try {
                double atk = Double.parseDouble(args[2]);
                org.bukkit.inventory.ItemStack item = com.chzzk.rpg.items.ItemManager
                        .createWeapon(org.bukkit.Material.DIAMOND_SWORD, "§bStarter Sword", atk);
                target.getInventory().addItem(item);
                sender.sendMessage("§aGiven weapon with " + atk + " ATK to " + target.getName());
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("givescroll")) {
            // /rpgadmin givescroll <player> <type>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin givescroll <player> <type>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            String type = args[2].toUpperCase();
            org.bukkit.inventory.ItemStack scroll = com.chzzk.rpg.items.ItemManager
                    .createScroll("§a강화 주문서 (" + type + ")", type);
            target.getInventory().addItem(scroll);
            sender.sendMessage("§aGiven scroll to " + target.getName());
            return true;
        }

        return false;
    }
}
