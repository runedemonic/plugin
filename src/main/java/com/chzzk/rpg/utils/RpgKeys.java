package com.chzzk.rpg.utils;

import com.chzzk.rpg.ChzzkRPG;
import org.bukkit.NamespacedKey;

public class RpgKeys {

    public static final NamespacedKey ITEM_TYPE = new NamespacedKey(ChzzkRPG.getInstance(), "item_type"); // WEAPON,
                                                                                                          // SCROLL,
                                                                                                          // CONSUMABLE
    public static final NamespacedKey WEAPON_DATA = new NamespacedKey(ChzzkRPG.getInstance(), "weapon_data");

    // Scroll Stats
    public static final NamespacedKey SCROLL_TYPE = new NamespacedKey(ChzzkRPG.getInstance(), "scroll_type"); // NORMAL,
                                                                                                              // PROTECTION

    // Individual stats if not using JSON
    public static final NamespacedKey WEAPON_TYPE = new NamespacedKey(ChzzkRPG.getInstance(), "weapon_type");
    public static final NamespacedKey BASE_ATK = new NamespacedKey(ChzzkRPG.getInstance(), "base_atk");
    public static final NamespacedKey ATTACK_SPEED = new NamespacedKey(ChzzkRPG.getInstance(), "attack_speed");
    public static final NamespacedKey ENHANCE_LEVEL = new NamespacedKey(ChzzkRPG.getInstance(), "enhance_level");
}
