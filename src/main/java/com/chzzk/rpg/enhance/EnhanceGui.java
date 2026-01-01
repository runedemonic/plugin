package com.chzzk.rpg.enhance;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.gui.RpgGui;
import com.chzzk.rpg.items.WeaponData;
import com.chzzk.rpg.utils.ItemBuilder;
import com.chzzk.rpg.utils.RpgKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class EnhanceGui implements RpgGui {

    private final ChzzkRPG plugin;
    private final Inventory inventory;

    // Slot Indices
    private static final int SLOT_WEAPON = 11;
    private static final int SLOT_SCROLL = 15;
    private static final int SLOT_BUTTON = 13;

    public EnhanceGui() {
        this.plugin = ChzzkRPG.getInstance();
        this.inventory = Bukkit.createInventory(this, 27, "§0강화 대장간"); // 3 Rows
        initialize();
    }

    private void initialize() {
        // Fill background
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            if (i == SLOT_WEAPON || i == SLOT_SCROLL || i == SLOT_BUTTON)
                continue;
            inventory.setItem(i, filler);
        }

        updateButton();
    }

    private void updateButton() {
        inventory.setItem(SLOT_BUTTON, new ItemBuilder(Material.ANVIL)
                .name("§a강화하기")
                .lore("§7무기와 강화 주문서를 넣고 클릭하세요.")
                .build());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        // Nothing special on open, empty slots
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true); // Default cancel

        int slot = event.getRawSlot();
        if (slot >= 27) { // Player inventory
            event.setCancelled(false); // Allow moving items in player inventory

            // Allow shift-click into GUI? (Complex, skip for now or implementing simpler
            // move)
            if (event.isShiftClick()) {
                event.setCancelled(true); // Simplify, drag and drop only
            }
            return;
        }

        // Allow interaction with Weapon/Scroll slots
        if (slot == SLOT_WEAPON || slot == SLOT_SCROLL) {
            event.setCancelled(false);
            // Schedule update button? simple check on clicking button is safer
            return;
        }

        if (slot == SLOT_BUTTON) {
            Player player = (Player) event.getWhoClicked();
            ItemStack weapon = inventory.getItem(SLOT_WEAPON);
            ItemStack scroll = inventory.getItem(SLOT_SCROLL);

            // Validation
            if (weapon == null || !WeaponData.isWeapon(weapon)) {
                player.sendMessage("§c무기를 올려주세요.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                return;
            }
            if (scroll == null || !isScroll(scroll)) {
                player.sendMessage("§c강화 주문서를 올려주세요.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                return;
            }

            // Execute Enhance
            plugin.getEnhanceManager().enhance(player, weapon, scroll);

            // Effect handled in Manager, but we must update GUI if item destroyed or
            // changed
            // Also need to handle scroll deduction if not done by amount logic
            // Manager does scroll.setAmount(scroll.getAmount() - 1);

            // If weapon destroyed (amount 0), remove it
            if (weapon.getAmount() <= 0) {
                inventory.setItem(SLOT_WEAPON, null);
            }

            if (scroll.getAmount() <= 0) {
                inventory.setItem(SLOT_SCROLL, null);
            }

            // Refresh visuals? (Manager saves PDC/Lore, but item stack object in inventory
            // is same reference usually, so it updates)
            // Just force update to be sure?
            // Bukkit sometimes needs explicit setItem for visual refresh
        }
    }

    private boolean isScroll(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(RpgKeys.SCROLL_TYPE, PersistentDataType.STRING);
    }
}
