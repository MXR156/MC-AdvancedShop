package io.github.mxr156.advancedshop.gui;

import io.github.mxr156.advancedshop.AdvancedShopPlugin;
import io.github.mxr156.advancedshop.model.Shop;
import io.github.mxr156.advancedshop.model.ShopItem;
import io.github.mxr156.advancedshop.util.Items;
import io.github.mxr156.advancedshop.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the shop layout with one item "picked up". Click a free (highlighted) slot to put it down there;
 * occupied slots are refused. Clicking the item itself cancels the move.
 */
public final class MoveItemMenu extends Menu {

    private final AdvancedShopPlugin plugin;
    private final Shop shop;
    private final int from;

    public MoveItemMenu(AdvancedShopPlugin plugin, Shop shop, int from) {
        this.plugin = plugin;
        this.shop = shop;
        this.from = from;
        this.inventory = Bukkit.createInventory(this, shop.size(),
                Text.mm("<dark_red>Move item <gray>- pick a free slot"));
    }

    @Override
    public void render() {
        inventory.clear();
        ItemStack free = Items.button(Material.LIME_STAINED_GLASS_PANE, "<green>Place here");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ShopItem item = shop.item(slot);
            if (item == null) {
                inventory.setItem(slot, free);
            } else if (slot == from) {
                inventory.setItem(slot, moving(item));
            } else {
                inventory.setItem(slot, plugin.renderer().render(item, null, false));
            }
        }
    }

    private ItemStack moving(ShopItem item) {
        ItemStack stack = plugin.renderer().render(item, null, false);
        ItemMeta meta = stack.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(Text.item("<gold>» Moving this item"));
        lore.add(Text.item("<gray>Click a <green>green</green> slot to place it"));
        lore.add(Text.item("<gray>Click this item to cancel"));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public void onTopClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int target = event.getSlot();
        ShopItem moving = shop.item(from);
        if (moving == null) { // removed meanwhile by another admin
            new ShopEditorMenu(plugin, shop).open(player);
            return;
        }
        if (target == from) {
            new ItemEditorMenu(plugin, shop, from).open(player);
            return;
        }
        if (shop.item(target) != null) {
            plugin.messages().send(player, "move-occupied");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return;
        }
        shop.setItem(from, null);
        shop.setItem(target, moving);
        plugin.shops().save(shop);
        plugin.shops().refreshViewers(shop);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
        plugin.messages().send(player, "item-moved", Placeholder.unparsed("slot", String.valueOf(target)));
        new ItemEditorMenu(plugin, shop, target).open(player);
    }
}
