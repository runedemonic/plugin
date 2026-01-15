package com.chzzk.rpg.items;

import com.chzzk.rpg.cube.CubeType;
import com.chzzk.rpg.utils.ItemBuilder;
import com.chzzk.rpg.utils.RpgKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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

    public static ItemStack createGradeStone(int amount) {
        ItemStack item = new ItemBuilder(Material.AMETHYST_SHARD)
                .name("§d등급석")
                .lore(
                    "§7무기 등급 승급에 사용됩니다.",
                    "",
                    "§7대장장이 전용"
                )
                .build();

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(RpgKeys.GRADE_STONE, PersistentDataType.STRING, "GRADE_STONE");
        item.setItemMeta(meta);
        item.setAmount(amount);

        return item;
    }

    public static ItemStack createCube(CubeType type, int amount) {
        Material material = type == CubeType.ADVANCED ? Material.ENDER_EYE : Material.ENDER_PEARL;

        ItemStack item = new ItemBuilder(material)
                .name(type.getColoredName())
                .lore(
                    "§7무기의 추가 옵션을 재설정합니다.",
                    "",
                    type.isCanRestore()
                        ? "§6이전 옵션으로 복원 가능"
                        : "§7되돌릴 수 없음",
                    "",
                    "§7사용 비용: §e" + type.getGoldCost() + "원"
                )
                .build();

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(RpgKeys.CUBE_TYPE, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        item.setAmount(amount);

        return item;
    }
}
