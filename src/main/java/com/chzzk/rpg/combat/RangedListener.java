package com.chzzk.rpg.combat;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.items.WeaponData;
import com.chzzk.rpg.items.WeaponType;
import com.chzzk.rpg.utils.RpgKeys;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class RangedListener implements Listener {

    private final ChzzkRPG plugin;

    public RangedListener(ChzzkRPG plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR)
            return;

        // Check if RPG Weapon
        if (!WeaponData.isWeapon(item))
            return;

        WeaponData wd = new WeaponData(item);
        if (wd.getWeaponType() != WeaponType.RANGED)
            return;
        if (wd.getOwnerUuid() != null && !wd.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§c이 장비는 귀속되어 있습니다.");
            return;
        }

        event.setCancelled(true); // Prevent vanilla interaction (e.g. blocking, bow draw)

        // Cooldown Check
        if (!plugin.getCooldownManager().canAttack(player, wd.getCooldownTicks())) {
            return;
        }
        plugin.getCooldownManager().updateAttack(player);

        // Shoot Projectile
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setShooter(player);
        arrow.setCritical(true); // Visual crit (particles)

        // Velocity (Optional: fast projectiles)
        arrow.setVelocity(player.getLocation().getDirection().multiply(2.0));

        // Store Weapon Data Snapshot on Projectile
        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        pdc.set(RpgKeys.BASE_ATK, PersistentDataType.DOUBLE, wd.getBaseAtk());
        pdc.set(RpgKeys.ENHANCE_LEVEL, PersistentDataType.INTEGER, wd.getEnhanceLevel());
        pdc.set(RpgKeys.WEAPON_TYPE, PersistentDataType.STRING, WeaponType.RANGED.name());

        // TODO: Store bonus stats if needed
    }
}
