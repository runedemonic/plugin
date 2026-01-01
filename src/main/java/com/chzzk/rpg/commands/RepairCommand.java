package com.chzzk.rpg.commands;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.jobs.LifeJob;
import com.chzzk.rpg.stats.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class RepairCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
        PlayerProfile profile = ChzzkRPG.getInstance().getStatsManager().getProfile(player);

        if (profile == null || profile.getLifeJob() != LifeJob.BLACKSMITH) {
            player.sendMessage("§cOnly Blacksmiths can repair items.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cHold an item.");
            return true;
        }
        if (com.chzzk.rpg.items.WeaponData.isWeapon(item)) {
            com.chzzk.rpg.items.WeaponData weaponData = new com.chzzk.rpg.items.WeaponData(item);
            if (!weaponData.isOwnedBy(player.getUniqueId())) {
                player.sendMessage("§c이 장비는 귀속되어 있습니다.");
                return true;
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            if (!damageable.hasDamage()) {
                player.sendMessage("§aItem is already fully repaired.");
                return true;
            }

            damageable.setDamage(0);
            item.setItemMeta(meta);
            player.sendMessage("§aItem repaired!");
        } else {
            player.sendMessage("§cThis item cannot be repaired.");
        }

        return true;
    }
}
