package io.github.mxr156.advancedshop.command;

import io.github.mxr156.advancedshop.AdvancedShopPlugin;
import io.github.mxr156.advancedshop.ShopManager;
import io.github.mxr156.advancedshop.gui.ShopEditorMenu;
import io.github.mxr156.advancedshop.gui.ShopMenu;
import io.github.mxr156.advancedshop.model.Shop;
import io.github.mxr156.advancedshop.util.Messages;
import io.github.mxr156.advancedshop.util.Text;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /ashop create|link|unlink|delete|edit|open|list|settitle|setrows|reload
 */
public final class ShopCommand implements BasicCommand {

    private static final List<String> ADMIN_SUBS =
            List.of("create", "link", "unlink", "delete", "edit", "open", "list", "settitle", "setrows", "reload", "help");

    private final AdvancedShopPlugin plugin;

    public ShopCommand(AdvancedShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        Messages msg = plugin.messages();
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("open")) {
            open(sender, args);
            return;
        }
        if (!sender.hasPermission("ashop.admin")) {
            msg.send(sender, "no-permission");
            return;
        }

        switch (sub) {
            case "create", "link" -> {
                if (args.length < 2) { usage(sender, "/ashop " + sub + " <shop>"); return; }
                if (!ShopManager.isValidId(args[1])) { msg.send(sender, "invalid-name"); return; }
                Block block = targetContainer(sender);
                if (block == null) return;
                Shop shop = plugin.shops().getOrCreate(args[1]);
                plugin.shops().link(block, shop);
                msg.send(sender, sub.equals("create") ? "shop-created" : "shop-linked", Placeholder.unparsed("shop", shop.id()));
            }
            case "unlink" -> {
                Block block = targetContainer(sender);
                if (block == null) return;
                String id = plugin.shops().shopIdAt(block);
                if (id == null) { msg.send(sender, "not-a-shop-chest"); return; }
                plugin.shops().link(block, null);
                msg.send(sender, "shop-unlinked", Placeholder.unparsed("shop", id));
            }
            case "delete" -> {
                Shop shop = shopArg(sender, args, "/ashop delete <shop>");
                if (shop == null) return;
                closeViewers(shop);
                plugin.shops().delete(shop);
                msg.send(sender, "shop-deleted", Placeholder.unparsed("shop", shop.id()));
            }
            case "edit" -> {
                Shop shop = shopArg(sender, args, "/ashop edit <shop>");
                if (shop == null) return;
                if (!(sender instanceof Player player)) { msg.send(sender, "player-only"); return; }
                new ShopEditorMenu(plugin, shop).open(player);
            }
            case "list" -> {
                String names = plugin.shops().all().stream().map(Shop::id).collect(Collectors.joining(", "));
                msg.send(sender, "list-header",
                        Placeholder.unparsed("count", String.valueOf(plugin.shops().all().size())),
                        Placeholder.unparsed("shops", names.isEmpty() ? "-" : names));
            }
            case "settitle" -> {
                Shop shop = shopArg(sender, args, "/ashop settitle <shop> <title...>");
                if (shop == null) return;
                if (args.length < 3) { usage(sender, "/ashop settitle <shop> <title...>"); return; }
                shop.title(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                plugin.shops().save(shop);
                closeViewers(shop);
                msg.send(sender, "title-set", Placeholder.unparsed("shop", shop.id()));
            }
            case "setrows" -> {
                Shop shop = shopArg(sender, args, "/ashop setrows <shop> <1-6>");
                if (shop == null) return;
                try {
                    shop.rows(Integer.parseInt(args[2]));
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                    usage(sender, "/ashop setrows <shop> <1-6>");
                    return;
                }
                plugin.shops().save(shop);
                closeViewers(shop);
                msg.send(sender, "rows-set", Placeholder.unparsed("shop", shop.id()),
                        Placeholder.unparsed("rows", String.valueOf(shop.rows())));
            }
            case "reload" -> {
                plugin.reload();
                msg.send(sender, "reloaded");
            }
            default -> help(sender);
        }
    }

    /** /ashop open <shop> [player] - handy for NPC plugins, signs, menus etc. */
    private void open(CommandSender sender, String[] args) {
        Messages msg = plugin.messages();
        if (!sender.hasPermission("ashop.command.open")) { msg.send(sender, "no-permission"); return; }
        Shop shop = shopArg(sender, args, "/ashop open <shop> [player]");
        if (shop == null) return;
        Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission("ashop.admin") && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                msg.send(sender, "no-permission");
                return;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { usage(sender, "Player " + args[2] + " is not online."); return; }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            msg.send(sender, "player-only");
            return;
        }
        new ShopMenu(plugin, shop, target).open(target);
    }

    private Shop shopArg(CommandSender sender, String[] args, String usage) {
        if (args.length < 2) { usage(sender, usage); return null; }
        Shop shop = plugin.shops().get(args[1]);
        if (shop == null) plugin.messages().send(sender, "shop-not-found", Placeholder.unparsed("shop", args[1]));
        return shop;
    }

    private Block targetContainer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return null;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null || !ShopManager.isShopContainerType(block)) {
            plugin.messages().send(sender, "not-looking-at-chest");
            return null;
        }
        return block;
    }

    private void closeViewers(Shop shop) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof ShopMenu menu && menu.shop() == shop) {
                player.closeInventory();
            }
        }
    }

    private void usage(CommandSender sender, String usage) {
        sender.sendMessage(Text.mm("<red>Usage: <white><usage>", Placeholder.unparsed("usage", usage)));
    }

    private void help(CommandSender sender) {
        String[] lines = {
                "<green><bold>AdvancedShop</bold> <gray>commands",
                "<gray>Rename a chest to <white>[ashop.<name>]</white> in an anvil and place it to make a shop.",
                "<gray>Sneak + right-click a shop chest to edit it.",
                "<white>/ashop create <shop> <gray>- turn the chest you look at into a shop",
                "<white>/ashop link <shop> <gray>- link the chest you look at to an existing shop",
                "<white>/ashop unlink <gray>- turn the shop chest you look at back to normal",
                "<white>/ashop edit <shop> <gray>- open the editor",
                "<white>/ashop open <shop> [player] <gray>- open a shop (for NPCs/console)",
                "<white>/ashop settitle <shop> <title> <gray>- MiniMessage title",
                "<white>/ashop setrows <shop> <1-6>",
                "<white>/ashop delete <shop> <gray>| <white>list <gray>| <white>reload"
        };
        for (String line : lines) sender.sendMessage(Text.mm(line.replace("<shop>", "\\<shop>").replace("<name>", "\\<name>")
                .replace("<title>", "\\<title>").replace("<1-6>", "\\<1-6>")));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        boolean admin = sender.hasPermission("ashop.admin");
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> subs = admin ? ADMIN_SUBS : (sender.hasPermission("ashop.command.open") ? List.of("open") : List.of());
            return subs.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("create", "link", "delete", "edit", "open", "settitle", "setrows").contains(sub)) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.shops().all().stream().map(Shop::id).filter(id -> id.startsWith(prefix)).toList();
        }
        if (args.length == 3 && sub.equals("open") && admin) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        if (args.length == 3 && sub.equals("setrows")) return List.of("1", "2", "3", "4", "5", "6");
        return List.of();
    }
}
