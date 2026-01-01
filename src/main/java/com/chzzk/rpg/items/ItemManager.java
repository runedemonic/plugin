package com.chzzk.rpg.items;

import com.chzzk.rpg.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemManager {

    public static ItemStack createWeapon(Material material, String name, double baseAtk) {
        return createWeapon(material, name, baseAtk, null);
    }

    public static ItemStack createWeapon(Material material, String name, double baseAtk, java.util.UUID ownerUuid) {
        ItemStack item = new ItemBuilder(material)
                .name(name)
                .build();

        WeaponData weaponData = new WeaponData(item);
        weaponData.setWeaponType(WeaponType.SWORD);
        weaponData.setAttackSpeed(1.0);
        weaponData.setBaseAtk(baseAtk);
        weaponData.setEnhanceLevel(0);
        weaponData.setOwnerUuid(ownerUuid);
        weaponData.save();

        return weaponData.getItem();
    }

    public static ItemStack createScroll(String name, String type) {
        ItemStack item = new ItemBuilder(Material.PAPER)
                .name(name)
                .lore("§7강화 주문서 입니다.", "§e우클릭으로 사용하거나 강화 UI에서 사용하세요.")
                .build();

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(com.chzzk.rpg.utils.RpgKeys.ITEM_TYPE, org.bukkit.persistence.PersistentDataType.STRING, "SCROLL");
        pdc.set(com.chzzk.rpg.utils.RpgKeys.SCROLL_TYPE, org.bukkit.persistence.PersistentDataType.STRING, type);
        item.setItemMeta(meta);

        return item;
    }
}
