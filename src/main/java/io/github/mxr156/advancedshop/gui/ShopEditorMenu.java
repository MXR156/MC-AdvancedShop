package io.github.mxr156.advancedshop.gui;

import io.github.mxr156.advancedshop.AdvancedShopPlugin;
import io.github.mxr156.advancedshop.model.Shop;
import io.github.mxr156.advancedshop.model.ShopItem;
import io.github.mxr156.advancedshop.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Admin view of a shop. Drop/drag an item into an empty slot to add it,
 * click an existing entry to configure it.
 */
public final class ShopEditorMenu extends Menu {

    private final AdvancedShopPlugin plugin;
    private final Shop shop;

    public ShopEditorMenu(AdvancedShopPlugin plugin, Shop shop) {
        this.plugin = plugin;
        this.shop = shop;
        this.inventory = Bukkit.createInventory(this, shop.size(),
                Text.mm("<dark_red>Editing: <gray><shop>", Placeholder.unparsed("shop", shop.id())));
    }

    @Override
    public void render() {
        inventory.clear();
        shop.items().forEach((slot, item) -> {
            if (slot < inventory.getSize()) inventory.setItem(slot, plugin.renderer().render(item, null, true));
        });
    }

    @Override
    public void onTopClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        ShopItem existing = shop.item(slot);
        if (existing != null) {
            new ItemEditorMenu(plugin, shop, slot).open(player);
            return;
        }
        ItemStack cursor = event.getCursor();
        if (!cursor.getType().isAir()) add(player, slot, cursor);
    }

    @Override
    public void onBottomShiftClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (shop.item(slot) == null) {
                add((Player) event.getWhoClicked(), slot, current);
                return;
            }
        }
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        var topSlots = event.getRawSlots().stream().filter(s -> s < inventory.getSize()).toList();
        if (topSlots.size() != 1) return;
        int slot = topSlots.getFirst();
        if (shop.item(slot) == null) add((Player) event.getWhoClicked(), slot, event.getOldCursor());
    }

    /** Adds a copy of {@code stack} as a new entry; the admin keeps their item. */
    private void add(Player player, int slot, ItemStack stack) {
        shop.setItem(slot, new ShopItem(stack));
        plugin.shops().save(shop);
        plugin.shops().refreshViewers(shop);
        render();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
        plugin.messages().send(player, "item-added", Placeholder.unparsed("slot", String.valueOf(slot)));
    }
}
