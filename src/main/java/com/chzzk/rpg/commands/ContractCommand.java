package com.chzzk.rpg.commands;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.contracts.Contract;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ContractCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players.");
            return true;
        }

        Player player = (Player) sender;
        var cm = ChzzkRPG.getInstance().getContractManager();

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            List<Contract> list = cm.getOpenContracts();
            if (list.isEmpty()) {
                player.sendMessage("§eNo open contracts.");
            } else {
                player.sendMessage("§6=== Open Contracts ===");
                for (Contract c : list) {
                    player.sendMessage(
                            "§e#" + c.getId() + " §fReward: $" + c.getReward() + " Budget: $" + c.getBudget());
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            // /contract create <reward> <budget>
            if (args.length < 3) {
                player.sendMessage("Usage: /contract create <reward> <budget>");
                return true;
            }
            try {
                double reward = Double.parseDouble(args[1]);
                double budget = Double.parseDouble(args[2]);
                cm.createContract(player, player.getLocation().getChunk(), reward, budget);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid numbers.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /contract accept <id>");
                return true;
            }
            try {
                int id = Integer.parseInt(args[1]);
                cm.acceptContract(player, id);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid ID.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("complete")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /contract complete <id>");
                return true;
            }
            try {
                int id = Integer.parseInt(args[1]);
                cm.completeContract(player, id);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid ID.");
            }
            return true;
        }

        return true;
    }
}
