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
                player.sendMessage("§eOwner: §f" + claim.getOwnerId()); // Need to resolve Name from UUID really
                player.sendMessage("§eType: §f" + claim.getOwnerType());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("claim")) {
            ChzzkRPG.getInstance().getLandManager().buyClaim(player, chunk);
            return true;
        }

        if (args[0].equalsIgnoreCase("unclaim")) {
            ChzzkRPG.getInstance().getLandManager().unclaim(player, chunk);
            return true;
        }

        return true;
    }
}
