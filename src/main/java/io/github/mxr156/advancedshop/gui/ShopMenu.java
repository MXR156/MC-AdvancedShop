package io.github.mxr156.advancedshop.gui;

import io.github.mxr156.advancedshop.AdvancedShopPlugin;
import io.github.mxr156.advancedshop.ItemRenderer;
import io.github.mxr156.advancedshop.model.Shop;
import io.github.mxr156.advancedshop.model.ShopItem;
import io.github.mxr156.advancedshop.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** The customer-facing shop. */
public final class ShopMenu extends Menu {

    private final AdvancedShopPlugin plugin;
    private final Shop shop;
    private final Player viewer;

    public ShopMenu(AdvancedShopPlugin plugin, Shop shop, Player viewer) {
        this.plugin = plugin;
        this.shop = shop;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, shop.size(),
                Text.mm(shop.title(), Placeholder.unparsed("shop", shop.id())));
    }

    public Shop shop() { return shop; }

    @Override
    public void render() {
        inventory.clear();
        shop.items().forEach((slot, item) -> {
            if (slot < inventory.getSize()) inventory.setItem(slot, plugin.renderer().render(item, viewer, false));
        });
    }

    @Override
    public void onTopClick(InventoryClickEvent event) {
        ShopItem item = shop.item(event.getSlot());
        if (item == null || !(event.getWhoClicked() instanceof Player player)) return;
        boolean changed = switch (event.getClick()) {
            case LEFT -> plugin.transactions().buy(player, item, 1);
            case SHIFT_LEFT -> plugin.transactions().buy(player, item, ItemRenderer.stackMultiplier(item));
            case RIGHT -> plugin.transactions().sell(player, item, false);
            case SHIFT_RIGHT -> plugin.transactions().sell(player, item, true);
            default -> false;
        };
        if (changed) render();
    }
}
