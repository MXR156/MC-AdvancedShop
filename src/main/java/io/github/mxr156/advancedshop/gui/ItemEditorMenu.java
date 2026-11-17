package io.github.mxr156.advancedshop.gui;

import io.github.mxr156.advancedshop.AdvancedShopPlugin;
import io.github.mxr156.advancedshop.model.Shop;
import io.github.mxr156.advancedshop.model.ShopItem;
import io.github.mxr156.advancedshop.model.ShopItemType;
import io.github.mxr156.advancedshop.util.Items;
import io.github.mxr156.advancedshop.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Configures a single shop entry: type, prices, amount, reward item, permissions, commands... */
public final class ItemEditorMenu extends Menu {

    private static final int PREVIEW = 4;
    private static final int TYPE = 10, BUY = 12, SELL = 14, AMOUNT = 16;
    private static final int REWARD = 28, ICON = 30, PERMISSIONS = 32, COMMANDS = 34;
    private static final int NAME = 38, DESCRIPTION = 40, MOVE = 42;
    private static final int BACK = 45, DELETE = 53;

    private final AdvancedShopPlugin plugin;
    private final Shop shop;
    private final int slot;

    public ItemEditorMenu(AdvancedShopPlugin plugin, Shop shop, int slot) {
        this.plugin = plugin;
        this.shop = shop;
        this.slot = slot;
        this.inventory = Bukkit.createInventory(this, 54,
                Text.mm("<dark_red>Edit item <gray>(<shop> #<slot>)",
                        Placeholder.unparsed("shop", shop.id()),
                        Placeholder.unparsed("slot", String.valueOf(slot))));
    }

    private ShopItem item() {
        return shop.item(slot);
    }

    @Override
    public void render() {
        ShopItem item = item();
        if (item == null) return;
        ItemStack filler = Items.filler();
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        inventory.setItem(PREVIEW, plugin.renderer().render(item, null, false));

        inventory.setItem(TYPE, Items.button(switch (item.type()) {
                    case ITEM -> Material.CHEST;
                    case PERMISSION -> Material.NAME_TAG;
                    case COMMAND -> Material.COMMAND_BLOCK;
                }, "<gold>Type: <white>" + item.type().name(),
                "<gray>ITEM <dark_gray>- gives/buys back the reward item",
                "<gray>PERMISSION <dark_gray>- grants permission node(s)",
                "<gray>COMMAND <dark_gray>- only runs the commands",
                "", "<yellow>Click to cycle"));

        inventory.setItem(BUY, Items.button(Material.EMERALD,
                "<green>Buy price: <white>" + (item.canBuy() ? plugin.renderer().price(item.buyPrice()) : "disabled"),
                "<yellow>Left-click <gray>to set in chat", "<yellow>Right-click <gray>to disable buying"));

        inventory.setItem(SELL, Items.button(item.type() == ShopItemType.ITEM ? Material.GOLD_INGOT : Material.BARRIER,
                "<gold>Sell price: <white>" + (item.sellPrice() >= 0 ? plugin.renderer().price(item.sellPrice()) : "disabled"),
                item.type() == ShopItemType.ITEM ? "<gray>Players sell the reward item back" : "<red>Only ITEM entries can be sold",
                "<yellow>Left-click <gray>to set in chat", "<yellow>Right-click <gray>to disable selling"));

        ItemStack amount = Items.button(Material.HOPPER, "<aqua>Amount per purchase: <white>" + item.amount(),
                "<yellow>Left/Right <gray>+1 / -1", "<yellow>Shift-Left/Right <gray>+16 / -16",
                "<yellow>Middle/Drop (Q) <gray>type a value");
        inventory.setItem(AMOUNT, amount);

        ItemStack reward = item.rewardItem();
        List<String> rewardLore = new ArrayList<>();
        rewardLore.add("<gray>Current: <white>" + Items.name(reward) + (item.hasCustomReward() ? "" : " <dark_gray>(same as icon)"));
        rewardLore.add("");
        rewardLore.add("<yellow>Click with an item <gray>on your cursor to set");
        rewardLore.add("<yellow>Right-click <gray>(empty cursor) to reset to icon");
        rewardLore.add("<yellow>Shift-click <gray>to get a copy");
        inventory.setItem(REWARD, Items.button(reward.getType(), "<green>Reward item", rewardLore));

        inventory.setItem(ICON, Items.button(item.icon().getType(), "<green>Display icon",
                "<gray>The item shown in the shop.", "",
                "<yellow>Click with an item <gray>on your cursor to change"));

        inventory.setItem(PERMISSIONS, listButton(Material.NAME_TAG, "<aqua>Permissions", item.permissions(),
                "<gray>Granted on purchase (PERMISSION type)."));
        inventory.setItem(COMMANDS, listButton(Material.COMMAND_BLOCK, "<light_purple>Commands", item.commands(),
                "<gray>Run by console on purchase. Use <white>%player%"));

        inventory.setItem(NAME, Items.button(Material.OAK_SIGN, "<white>Display name",
                "<gray>Current: " + (item.displayName() == null ? "<dark_gray>(item name)" : "<reset>" + item.displayName()),
                "", "<yellow>Left-click <gray>to set (MiniMessage)", "<yellow>Right-click <gray>to reset"));
        inventory.setItem(DESCRIPTION, listButton(Material.WRITABLE_BOOK, "<white>Description", item.description(),
                "<gray>Extra lore lines (MiniMessage)."));

        inventory.setItem(MOVE, Items.button(Material.COMPASS, "<white>Move to slot",
                "<gray>Current slot: <white>" + slot, "<yellow>Click <gray>to pick it up and place it in a free slot"));

        inventory.setItem(BACK, Items.button(Material.ARROW, "<gray>« Back to shop"));
        inventory.setItem(DELETE, Items.button(Material.LAVA_BUCKET, "<red>Delete item", "<yellow>Shift-click <gray>to confirm"));
    }

    private static ItemStack listButton(Material material, String name, List<String> values, String hint) {
        List<String> lore = new ArrayList<>();
        lore.add(hint);
        lore.add("");
        if (values.isEmpty()) lore.add("<dark_gray>(none)");
        for (String value : values) lore.add("<gray>- <white>" + value.replace("<", "\\<"));
        lore.add("");
        lore.add("<yellow>Left-click <gray>to add");
        lore.add("<yellow>Right-click <gray>to remove last");
        lore.add("<yellow>Shift-right-click <gray>to clear");
        return Items.button(material, name, lore);
    }

    @Override
    public void onTopClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ShopItem item = item();
        if (item == null) {
            new ShopEditorMenu(plugin, shop).open(player);
            return;
        }
        ClickType click = event.getClick();
        ItemStack cursor = event.getCursor();

        switch (event.getSlot()) {
            case TYPE -> {
                item.type(item.type().next());
                changed(player);
            }
            case BUY -> {
                if (click.isRightClick()) {
                    item.buyPrice(-1);
                    changed(player);
                } else {
                    prompt(player, "Type the BUY price (or 'off'):", input -> item.buyPrice(parsePrice(player, input, item.buyPrice())));
                }
            }
            case SELL -> {
                if (click.isRightClick()) {
                    item.sellPrice(-1);
                    changed(player);
                } else {
                    prompt(player, "Type the SELL price (or 'off'):", input -> item.sellPrice(parsePrice(player, input, item.sellPrice())));
                }
            }
            case AMOUNT -> {
                switch (click) {
                    case LEFT -> item.amount(item.amount() + 1);
                    case RIGHT -> item.amount(item.amount() - 1);
                    case SHIFT_LEFT -> item.amount(item.amount() + 16);
                    case SHIFT_RIGHT -> item.amount(item.amount() - 16);
                    default -> {
                        prompt(player, "Type the amount per purchase:", input -> {
                            try {
                                item.amount(Integer.parseInt(input.trim()));
                            } catch (NumberFormatException e) {
                                plugin.messages().send(player, "invalid-number");
                            }
                        });
                        return;
                    }
                }
                changed(player);
            }
            case REWARD -> {
                if (!cursor.getType().isAir()) {
                    item.reward(cursor);
                    changed(player);
                } else if (click.isShiftClick()) {
                    Items.give(player, item.rewardItem(), 1);
                } else if (click.isRightClick()) {
                    item.reward(null);
                    changed(player);
                }
            }
            case ICON -> {
                if (!cursor.getType().isAir()) {
                    item.icon(cursor);
                    changed(player);
                }
            }
            case PERMISSIONS -> editList(player, click, item.permissions(), "Type a permission node to add:");
            case COMMANDS -> editList(player, click, item.commands(), "Type a console command (without /, use %player%):");
            case DESCRIPTION -> editList(player, click, item.description(), "Type a description line (MiniMessage):");
            case NAME -> {
                if (click.isRightClick()) {
                    item.displayName(null);
                    changed(player);
                } else {
                    prompt(player, "Type the display name (MiniMessage, e.g. <gold>VIP Rank):", item::displayName);
                }
            }
            case MOVE -> new MoveItemMenu(plugin, shop, slot).open(player);
            case BACK -> new ShopEditorMenu(plugin, shop).open(player);
            case DELETE -> {
                if (click.isShiftClick()) {
                    shop.setItem(slot, null);
                    save();
                    plugin.messages().send(player, "item-deleted");
                    new ShopEditorMenu(plugin, shop).open(player);
                }
            }
            default -> { }
        }
    }

    private void editList(Player player, ClickType click, List<String> list, String prompt) {
        if (click == ClickType.SHIFT_RIGHT) {
            list.clear();
            changed(player);
        } else if (click.isRightClick()) {
            if (!list.isEmpty()) list.removeLast();
            changed(player);
        } else {
            prompt(player, prompt, input -> {
                String value = input.trim();
                if (!value.isEmpty()) list.add(value);
            });
        }
    }

    private double parsePrice(Player player, String input, double fallback) {
        String value = input.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("$", "");
        if (value.equals("off") || value.equals("disable") || value.equals("-1")) return -1;
        try {
            double price = Double.parseDouble(value);
            if (price < 0 || Double.isNaN(price) || Double.isInfinite(price)) throw new NumberFormatException();
            return price;
        } catch (NumberFormatException e) {
            plugin.messages().send(player, "invalid-number");
            return fallback;
        }
    }

    /** Closes the GUI, asks for chat input, applies it and reopens the editor. */
    private void prompt(Player player, String text, Consumer<String> apply) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            plugin.chatInput().ask(player, text, input -> {
                if (item() != null) {
                    apply.accept(input);
                    save();
                }
                reopen(player);
            }, () -> reopen(player));
        });
    }

    private void reopen(Player player) {
        if (item() == null) new ShopEditorMenu(plugin, shop).open(player);
        else new ItemEditorMenu(plugin, shop, slot).open(player);
    }

    private void changed(Player player) {
        save();
        render();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    private void save() {
        plugin.shops().save(shop);
        plugin.shops().refreshViewers(shop);
    }
}
