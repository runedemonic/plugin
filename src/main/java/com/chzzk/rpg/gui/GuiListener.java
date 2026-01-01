package com.chzzk.rpg.gui;

import com.chzzk.rpg.ChzzkRPG;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {
    public GuiListener(ChzzkRPG plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof RpgGui) {
            event.setCancelled(true); // Default cancel
            ((RpgGui) holder).onClick(event);
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof RpgGui) {
            ((RpgGui) holder).onOpen(event);
        }
    }
}
