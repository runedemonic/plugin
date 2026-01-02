package com.chzzk.rpg.items;

import com.chzzk.rpg.stats.PlayerStats;
import com.chzzk.rpg.stats.StatType;
import com.chzzk.rpg.utils.RpgKeys;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@Getter
@Setter
public class WeaponData {

    private final ItemStack item;

    private WeaponType weaponType;
    private double baseAtk;
    private double attackSpeed; // APS
    private int enhanceLevel;
    private java.util.UUID ownerUuid;
    private PlayerStats bonusStats;

    private static final Gson gson = new Gson();

    public WeaponData(ItemStack item) {
        this.item = item;
        this.bonusStats = new PlayerStats();
        load();
    }

    public static boolean isWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(RpgKeys.BASE_ATK, PersistentDataType.DOUBLE);
    }

    public void load() {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        this.baseAtk = pdc.getOrDefault(RpgKeys.BASE_ATK, PersistentDataType.DOUBLE, 0.0);
        this.attackSpeed = pdc.getOrDefault(RpgKeys.ATTACK_SPEED, PersistentDataType.DOUBLE, 1.0); // Default 1.0 APS
        this.enhanceLevel = pdc.getOrDefault(RpgKeys.ENHANCE_LEVEL, PersistentDataType.INTEGER, 0);

        String typeStr = pdc.get(RpgKeys.WEAPON_TYPE, PersistentDataType.STRING);
        this.weaponType = typeStr != null ? WeaponType.valueOf(typeStr) : WeaponType.NONE;
        String ownerStr = pdc.get(RpgKeys.OWNER_UUID, PersistentDataType.STRING);
        this.ownerUuid = ownerStr != null ? java.util.UUID.fromString(ownerStr) : null;

        // Load Bonus Stats from JSON if exists, otherwise default
    }

    public void save() {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(RpgKeys.BASE_ATK, PersistentDataType.DOUBLE, baseAtk);
        pdc.set(RpgKeys.ATTACK_SPEED, PersistentDataType.DOUBLE, attackSpeed);
        pdc.set(RpgKeys.ENHANCE_LEVEL, PersistentDataType.INTEGER, enhanceLevel);
        pdc.set(RpgKeys.WEAPON_TYPE, PersistentDataType.STRING, weaponType.name());
        if (ownerUuid != null) {
            pdc.set(RpgKeys.OWNER_UUID, PersistentDataType.STRING, ownerUuid.toString());
        } else {
            pdc.remove(RpgKeys.OWNER_UUID);
        }

        // Update Lore
        updateLore(meta);

        item.setItemMeta(meta);
    }

    private void updateLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(""));

        // Weapon Type
        if (weaponType != WeaponType.NONE) {
            lore.add(Component.text("§f타입: " + weaponType.name())); // Enhance translation later
        }

        String prefix = enhanceLevel > 0 ? "§e[+" + enhanceLevel + "] " : "";
        lore.add(Component.text("§7공격력: §c" + (baseAtk + getEnhanceBonus())));
        if (enhanceLevel > 0) {
            lore.add(Component.text("§8(기본: " + baseAtk + " + 강화: " + getEnhanceBonus() + ")"));
        }

        lore.add(Component.text("§7공격 속도: §f" + attackSpeed + " APS"));
        if (ownerUuid != null) {
            String ownerName = org.bukkit.Bukkit.getOfflinePlayer(ownerUuid).getName();
            String display = ownerName != null ? ownerName : ownerUuid.toString();
            lore.add(Component.text("§7귀속: §f" + display));
        }

        lore.add(Component.text(""));
        // Add Bonus Stats
        for (Map.Entry<StatType, Double> entry : bonusStats.getStats().entrySet()) {
            if (entry.getValue() > 0) {
                lore.add(Component.text("§7" + entry.getKey().getDisplayName() + ": §a+" + entry.getValue()));
            }
        }

        meta.lore(lore);

        // Update Name
        // String name = meta.hasDisplayName() ?
        // ((TextComponent)meta.displayName()).content() : ...;
        // Logic to keep name but add +number is complex with Components, skipping name
        // update for now to avoid reset
    }

    public double getEnhanceBonus() {
        // Simple formula: +2.0 per level
        return enhanceLevel * 2.0;
    }

    public double getTotalAtk() {
        return baseAtk + getEnhanceBonus();
    }

    public int getCooldownTicks() {
        return (int) Math.round(20.0 / attackSpeed);
    }

    public boolean isOwnedBy(java.util.UUID uuid) {
        return ownerUuid == null || ownerUuid.equals(uuid);
    }
}
