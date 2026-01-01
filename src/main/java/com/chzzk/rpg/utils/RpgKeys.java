package com.chzzk.rpg.utils;

import com.chzzk.rpg.ChzzkRPG;
import org.bukkit.NamespacedKey;

public class RpgKeys {

    public static NamespacedKey ITEM_TYPE; // WEAPON, SCROLL, CONSUMABLE
    public static NamespacedKey WEAPON_DATA;

    // Scroll Stats
    public static NamespacedKey SCROLL_TYPE; // NORMAL, PROTECTION

    // Individual stats if not using JSON
    public static NamespacedKey WEAPON_TYPE;
    public static NamespacedKey BASE_ATK;
    public static NamespacedKey ATTACK_SPEED;
    public static NamespacedKey ENHANCE_LEVEL;
    public static NamespacedKey OWNER_UUID;

    private RpgKeys() {
    }

    public static void init(ChzzkRPG plugin) {
        ITEM_TYPE = new NamespacedKey(plugin, "item_type");
        WEAPON_DATA = new NamespacedKey(plugin, "weapon_data");
        SCROLL_TYPE = new NamespacedKey(plugin, "scroll_type");
        WEAPON_TYPE = new NamespacedKey(plugin, "weapon_type");
        BASE_ATK = new NamespacedKey(plugin, "base_atk");
        ATTACK_SPEED = new NamespacedKey(plugin, "attack_speed");
        ENHANCE_LEVEL = new NamespacedKey(plugin, "enhance_level");
        OWNER_UUID = new NamespacedKey(plugin, "owner_uuid");
    }
}
