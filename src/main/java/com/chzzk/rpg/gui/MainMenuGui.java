package com.chzzk.rpg.gui;

import com.chzzk.rpg.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MainMenuGui implements RpgGui {

    private final Inventory inventory;

    public MainMenuGui() {
        this.inventory = Bukkit.createInventory(this, 27, Component.text("§0§l[ Chzzk RPG Menu ]"));
        initializeItems();
    }

    private void initializeItems() {
        // Background
        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, bg);
        }

        // Icons
        inventory.setItem(10,
                new ItemBuilder(Material.PLAYER_HEAD).name("§aMy Stats").lore("§7View your RPG stats").build());
        inventory.setItem(11,
                new ItemBuilder(Material.IRON_SWORD).name("§cJobs").lore("§7Manage Combat & Life Jobs").build());
        inventory.setItem(12, new ItemBuilder(Material.GRASS_BLOCK).name("§2Land").lore("§7Manage Claims").build());
        inventory.setItem(13,
                new ItemBuilder(Material.PAPER).name("§6Contracts").lore("§7Construction Contracts").build());
        inventory.setItem(14, new ItemBuilder(Material.SHIELD).name("§9Guild").lore("§7Guild Management").build());
        inventory.setItem(15, new ItemBuilder(Material.ANVIL).name("§bEnhance").lore("§7Enhance Items").build());
        inventory.setItem(16, new ItemBuilder(Material.BOOK).name("§dWiki/Help").lore("§7Plugin Info").build());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        // Update dynamic items if needed (e.g. Head with player skin)
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null)
            return;
        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 10: // Stats
                new StatsGui(player).open();
                break;
            case 11: // Jobs
                player.performCommand("job info"); // Placeholder until JobGUI
                player.closeInventory();
                break;
            case 12: // Land
                player.performCommand("land info"); // Placeholder
                player.closeInventory();
                break;
            case 13: // Contracts
                player.performCommand("contract list"); // Placeholder
                player.closeInventory();
                break;
            case 14: // Guild
                player.performCommand("guild info"); // Placeholder
                player.closeInventory();
                break;
            case 15: // Enhance
                player.performCommand("enhance");
                break;
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
