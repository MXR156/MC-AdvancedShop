package io.github.mxr156.advancedshop;

import io.github.mxr156.advancedshop.model.ShopItem;
import io.github.mxr156.advancedshop.model.ShopItemType;
import io.github.mxr156.advancedshop.util.Messages;
import io.github.mxr156.advancedshop.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Builds the item stacks players see inside a shop. */
public final class ItemRenderer {

    private final AdvancedShopPlugin plugin;

    public ItemRenderer(AdvancedShopPlugin plugin) {
        this.plugin = plugin;
    }

    public String price(double amount) {
        return amount == 0 ? "Free" : plugin.economy().format(amount);
    }

    /** Units bought with shift-left-click. */
    public static int stackMultiplier(ShopItem item) {
        if (item.type() != ShopItemType.ITEM) return 1;
        return Math.max(1, item.rewardItem().getMaxStackSize() / item.amount());
    }

    public String displayName(ShopItem item) {
        return item.displayName() != null ? Text.plain(Text.mm(item.displayName())) : io.github.mxr156.advancedshop.util.Items.name(item.icon());
    }

    public ItemStack render(ShopItem item, @Nullable Player viewer, boolean editor) {
        Messages msg = plugin.messages();
        ItemStack stack = item.icon();
        if (item.type() == ShopItemType.ITEM) {
            stack.setAmount(Math.max(1, Math.min(item.amount(), stack.getMaxStackSize())));
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        if (item.displayName() != null) meta.displayName(Text.item(item.displayName()));

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        for (String line : item.description()) lore.add(Text.item(line));
        if (!lore.isEmpty()) lore.add(Component.empty());

        switch (item.type()) {
            case ITEM -> lore.add(Text.item(msg.lore("amount"), Placeholder.unparsed("amount", String.valueOf(item.amount()))));
            case PERMISSION -> lore.add(Text.item(msg.lore("permission-item")));
            case COMMAND -> lore.add(Text.item(msg.lore("command-item")));
        }

        boolean owned = viewer != null && item.type() == ShopItemType.PERMISSION
                && !item.permissions().isEmpty()
                && item.permissions().stream().allMatch(viewer::hasPermission);
        if (owned && !editor) {
            lore.add(Text.item(msg.lore("owned")));
        } else if (item.canBuy()) {
            lore.add(Text.item(msg.lore("buy"),
                    Placeholder.unparsed("price", price(item.buyPrice())),
                    Placeholder.unparsed("stack", String.valueOf(stackMultiplier(item)))));
        } else {
            lore.add(Text.item(msg.lore("buy-disabled")));
        }
        if (item.type() == ShopItemType.ITEM) {
            lore.add(item.canSell()
                    ? Text.item(msg.lore("sell"), Placeholder.unparsed("price", price(item.sellPrice())))
                    : Text.item(msg.lore("sell-disabled")));
        }
        if (editor) {
            lore.add(Component.empty());
            lore.add(Text.item(msg.lore("edit-hint")));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
