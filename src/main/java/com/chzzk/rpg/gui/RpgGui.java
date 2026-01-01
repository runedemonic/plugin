package com.chzzk.rpg.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public interface RpgGui extends InventoryHolder {
    void onOpen(InventoryOpenEvent event);

    void onClick(InventoryClickEvent event);
}
