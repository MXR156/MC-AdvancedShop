package io.github.mxr156.advancedshop.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Base for all plugin GUIs. The holder identifies our inventories; {@code MenuListener}
 * cancels every click in the top inventory before calling into the menu.
 */
public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /** (Re)draws the inventory contents. */
    public abstract void render();

    public void open(Player player) {
        render();
        player.openInventory(inventory);
    }

    /** Click inside the top (menu) inventory. The event is already cancelled. */
    public abstract void onTopClick(InventoryClickEvent event);

    /** Shift-click from the player's own inventory. The event is already cancelled. */
    public void onBottomShiftClick(InventoryClickEvent event) {}

    /** Drag touching the top inventory. The event is already cancelled. */
    public void onDrag(InventoryDragEvent event) {}
}
