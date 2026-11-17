package io.github.mxr156.advancedshop.listener;

import io.github.mxr156.advancedshop.gui.Menu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Routes clicks in plugin GUIs and makes sure no items can be taken out of them. */
public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Menu menu)) return;
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        if (clicked.equals(top)) {
            event.setCancelled(true);
            menu.onTopClick(event);
        } else if (event.isShiftClick()) {
            event.setCancelled(true);
            menu.onBottomShiftClick(event);
        } else if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true); // double-click would pull items out of the menu
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Menu menu)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize())) {
            event.setCancelled(true);
            menu.onDrag(event);
        }
    }
}
