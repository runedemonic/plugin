package com.chzzk.rpg.items;

import com.chzzk.rpg.grade.BonusStat;
import com.chzzk.rpg.grade.WeaponGrade;
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

    // Grade System
    private WeaponGrade grade;
    private List<BonusStat> bonusStatList;

    private static final Gson gson = new Gson();

    public WeaponData(ItemStack item) {
        this.item = item;
        this.bonusStats = new PlayerStats();
        this.grade = WeaponGrade.COMMON;
        this.bonusStatList = new ArrayList<>();
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

        // Load Grade
        String gradeStr = pdc.get(RpgKeys.WEAPON_GRADE, PersistentDataType.STRING);
        this.grade = gradeStr != null ? WeaponGrade.valueOf(gradeStr) : WeaponGrade.COMMON;

        // Load Bonus Stats from JSON
        String statsJson = pdc.get(RpgKeys.BONUS_STATS, PersistentDataType.STRING);
        this.bonusStatList = new ArrayList<>();
        if (statsJson != null && !statsJson.isEmpty()) {
            List<String> statStrings = gson.fromJson(statsJson, new TypeToken<List<String>>(){}.getType());
            for (String s : statStrings) {
                try {
                    bonusStatList.add(BonusStat.fromStorageString(s));
                } catch (Exception ignored) {
                }
            }
        }
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

        // Save Grade
        pdc.set(RpgKeys.WEAPON_GRADE, PersistentDataType.STRING, grade.name());

        // Save Bonus Stats as JSON
        List<String> statStrings = new ArrayList<>();
        for (BonusStat bs : bonusStatList) {
            statStrings.add(bs.toStorageString());
        }
        pdc.set(RpgKeys.BONUS_STATS, PersistentDataType.STRING, gson.toJson(statStrings));

        // Update Lore
        updateLore(meta);

        item.setItemMeta(meta);
    }

    private void updateLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(""));

        // Grade Display
        lore.add(Component.text("§7등급: " + grade.getColoredName()));

        // Weapon Type
        if (weaponType != WeaponType.NONE) {
            lore.add(Component.text("§7타입: §f" + weaponType.name()));
        }

        lore.add(Component.text(""));
        lore.add(Component.text("§7공격력: §c" + String.format("%.1f", baseAtk + getEnhanceBonus())));
        if (enhanceLevel > 0) {
            lore.add(Component.text("§8 (기본: " + baseAtk + " + 강화: +" + enhanceLevel + ")"));
        }

        lore.add(Component.text("§7공격 속도: §f" + attackSpeed + " APS"));

        if (ownerUuid != null) {
            String ownerName = org.bukkit.Bukkit.getOfflinePlayer(ownerUuid).getName();
            String display = ownerName != null ? ownerName : ownerUuid.toString();
            lore.add(Component.text("§7귀속: §f" + display));
        }

        // Bonus Stats from Grade System
        if (!bonusStatList.isEmpty()) {
            lore.add(Component.text(""));
            lore.add(Component.text("§d추가 옵션:"));
            for (BonusStat bs : bonusStatList) {
                lore.add(Component.text("§7 " + bs.getDisplayString()));
            }
        }

        meta.lore(lore);
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

    public PlayerStats getBonusStatsAsPlayerStats() {
        PlayerStats ps = new PlayerStats();
        for (BonusStat bs : bonusStatList) {
            ps.add(bs.getType(), bs.getValue());
        }
        return ps;
    }
}
