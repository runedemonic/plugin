package com.chzzk.rpg.combat;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.items.WeaponData;
import com.chzzk.rpg.stats.PlayerProfile;
import com.chzzk.rpg.stats.StatType;
import java.util.Random;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DamageListener implements Listener {

    private final ChzzkRPG plugin;
    private final Random random = new Random();

    public DamageListener(ChzzkRPG plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        LivingEntity defender = (LivingEntity) event.getEntity();
        Player attackerPlayer = null;

        // Resolve Attacker
        if (event.getDamager() instanceof Player) {
            attackerPlayer = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attackerPlayer = (Player) proj.getShooter();
            }
        }

        // Handle Mob -> Player damage (DEF application)
        if (attackerPlayer == null && defender instanceof Player) {
            handleMobToPlayerDamage(event, (Player) defender);
            return;
        }

        if (attackerPlayer == null)
            return;

        // 0. Cooldown (Attack Speed) Check and Weapon Resolution
        WeaponData wd = null;
        double weaponAtk = 0;

        // Check if RPG Projectile
        if (event.getDamager() instanceof Projectile && event.getDamager().getPersistentDataContainer()
                .has(com.chzzk.rpg.utils.RpgKeys.BASE_ATK, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
            org.bukkit.persistence.PersistentDataContainer pdc = event.getDamager().getPersistentDataContainer();
            double baseAtk = pdc.getOrDefault(com.chzzk.rpg.utils.RpgKeys.BASE_ATK,
                    org.bukkit.persistence.PersistentDataType.DOUBLE, 0.0);
            int enhanceLevel = pdc.getOrDefault(com.chzzk.rpg.utils.RpgKeys.ENHANCE_LEVEL,
                    org.bukkit.persistence.PersistentDataType.INTEGER, 0);
            weaponAtk = baseAtk + (enhanceLevel * 2.0);
            // No cooldown check for duplicate projectile hit events, assume controlled by
            // launch

        } else {
            // Melee Logic
            int cooldownTicks = 20;
            ItemStack hand = attackerPlayer.getInventory().getItemInMainHand();

            if (WeaponData.isWeapon(hand)) {
                wd = new WeaponData(hand);
                if (wd.getOwnerUuid() != null && !wd.getOwnerUuid().equals(attackerPlayer.getUniqueId())) {
                    attackerPlayer.sendMessage("§c이 장비는 귀속되어 있습니다.");
                    event.setCancelled(true);
                    return;
                }
                cooldownTicks = wd.getCooldownTicks();
                weaponAtk = wd.getTotalAtk();
            }
            // Cooldown check
            if (!plugin.getCooldownManager().canAttack(attackerPlayer, cooldownTicks)) {
                event.setCancelled(true);
                return;
            }
            plugin.getCooldownManager().updateAttack(attackerPlayer);
        }

        // 1. Calculate Attacker Stats
        PlayerProfile attackerProfile = plugin.getStatsManager().getProfile(attackerPlayer);
        if (attackerProfile == null)
            return;

        double playerAtk = attackerProfile.getTotalStats().get(StatType.ATK);
        // double weaponAtk already set
        double critRate = attackerProfile.getTotalStats().get(StatType.CRIT);
        double critDmg = Math.max(1.0, attackerProfile.getTotalStats().get(StatType.CRIT_DMG));
        double pen = attackerProfile.getTotalStats().get(StatType.PEN);
        pen = Math.max(0.0, Math.min(100.0, pen));

        // Total Attack
        double totalAtk = playerAtk + weaponAtk;

        // 2. Calculate Defender Stats
        double def = 0;
        if (defender instanceof Player) {
            PlayerProfile defenderProfile = plugin.getStatsManager().getProfile((Player) defender);
            if (defenderProfile != null) {
                def = defenderProfile.getTotalStats().get(StatType.DEF);
            }
        } else {
            // Mobs: maybe custom logic later
            def = 0;
        }

        // 3. Apply Penetration
        // Effective Def = Def * (1 - PEN/100) - PEN_FLAT (Assuming 0 flat for now or
        // add to stats)
        double effectiveDef = def * (1 - (pen / 100.0)); // TODO: Add PEN_FLAT

        // 4. Calculate Base Damage
        // base = max(minDamage, A - max(0, effectiveDef))
        double baseDamage = Math.max(1.0, totalAtk - Math.max(0, effectiveDef));

        // 5. Critical
        boolean isCrit = false;
        double finalDamage = baseDamage;
        if (random.nextDouble() * 100 < critRate) {
            isCrit = true;
            finalDamage *= critDmg;
        }

        // 6. Set Final Damage
        event.setDamage(finalDamage);

        // 7. Indicators
        if (isCrit) {
            attackerPlayer.sendActionBar(Component.text("§cCRITICAL! §f" + String.format("%.1f", finalDamage)));
        } else {
            attackerPlayer.sendActionBar(Component.text("§7Damage: §f" + String.format("%.1f", finalDamage)));
        }
    }

    /**
     * Handle damage from mobs (including MythicMobs) to players.
     * Applies player's DEF stat to reduce incoming damage.
     */
    private void handleMobToPlayerDamage(EntityDamageByEntityEvent event, Player defender) {
        PlayerProfile defenderProfile = plugin.getStatsManager().getProfile(defender);
        if (defenderProfile == null)
            return;

        double def = defenderProfile.getTotalStats().get(StatType.DEF);
        if (def <= 0)
            return;

        double originalDamage = event.getDamage();

        // DEF reduces damage: finalDamage = originalDamage * (100 / (100 + DEF))
        // This formula ensures DEF has diminishing returns and never reduces damage to 0
        double damageReduction = 100.0 / (100.0 + def);
        double finalDamage = Math.max(1.0, originalDamage * damageReduction);

        event.setDamage(finalDamage);

        // Show damage reduction indicator
        double reduced = originalDamage - finalDamage;
        if (reduced > 0.1) {
            defender.sendActionBar(Component.text(
                    "§7Received: §c" + String.format("%.1f", finalDamage) +
                            " §8(§a-" + String.format("%.1f", reduced) + " DEF§8)"));
        }
    }
}
