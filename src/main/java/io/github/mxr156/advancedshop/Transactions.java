package io.github.mxr156.advancedshop;

import io.github.mxr156.advancedshop.economy.EconomyHook;
import io.github.mxr156.advancedshop.model.ShopItem;
import io.github.mxr156.advancedshop.model.ShopItemType;
import io.github.mxr156.advancedshop.util.Items;
import io.github.mxr156.advancedshop.util.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Buying and selling logic. */
public final class Transactions {

    private final AdvancedShopPlugin plugin;

    public Transactions(AdvancedShopPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return true if anything was bought */
    public boolean buy(Player player, ShopItem item, int units) {
        Messages msg = plugin.messages();
        EconomyHook eco = plugin.economy();
        if (!item.canBuy()) {
            msg.send(player, "cannot-buy");
            return false;
        }
        if (item.type() != ShopItemType.ITEM) units = 1;
        units = Math.max(1, units);

        if (item.type() == ShopItemType.PERMISSION && !item.permissions().isEmpty()
                && item.permissions().stream().allMatch(player::hasPermission)) {
            msg.send(player, "already-owned");
            return false;
        }

        ItemStack reward = item.rewardItem();
        int totalItems = item.amount() * units;
        if (item.type() == ShopItemType.ITEM && Items.freeSpace(player.getInventory(), reward) < totalItems) {
            // Fall back to as many units as fit.
            units = Items.freeSpace(player.getInventory(), reward) / item.amount();
            if (units <= 0) {
                msg.send(player, "not-enough-space");
                return false;
            }
            totalItems = item.amount() * units;
        }

        double cost = item.buyPrice() * units;
        if (cost > 0 && !eco.hasEconomy()) {
            msg.send(player, "no-economy");
            return false;
        }
        if (!eco.has(player, cost)) {
            msg.send(player, "not-enough-money", Placeholder.unparsed("price", plugin.renderer().price(cost)));
            fail(player);
            return false;
        }
        String error = eco.withdraw(player, cost);
        if (error != null) {
            msg.send(player, "transaction-failed", Placeholder.unparsed("error", error));
            fail(player);
            return false;
        }

        switch (item.type()) {
            case ITEM -> Items.give(player, reward, totalItems);
            case PERMISSION -> item.permissions().forEach(node -> grantPermission(player, node));
            case COMMAND -> { /* commands below */ }
        }
        for (int i = 0; i < units; i++) runCommands(player, item);

        String label = (item.type() == ShopItemType.ITEM ? totalItems + "x " : "") + plugin.renderer().displayName(item);
        msg.send(player, "buy-success",
                Placeholder.unparsed("item", label),
                Placeholder.unparsed("price", plugin.renderer().price(cost)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
        return true;
    }

    /** @param all sell every matching item instead of one unit */
    public boolean sell(Player player, ShopItem item, boolean all) {
        Messages msg = plugin.messages();
        if (!item.canSell()) {
            msg.send(player, "cannot-sell");
            return false;
        }
        ItemStack proto = item.rewardItem();
        int have = Items.count(player.getInventory(), proto);
        int units = all ? have / item.amount() : (have >= item.amount() ? 1 : 0);
        if (units <= 0) {
            msg.send(player, "nothing-to-sell", Placeholder.unparsed("item", item.amount() + "x " + Items.name(proto)));
            fail(player);
            return false;
        }
        double earned = item.sellPrice() * units;
        if (earned > 0 && !plugin.economy().hasEconomy()) {
            msg.send(player, "no-economy");
            return false;
        }
        int totalItems = units * item.amount();
        Items.remove(player.getInventory(), proto, totalItems);
        String error = plugin.economy().deposit(player, earned);
        if (error != null) {
            // Refund the items if the payout failed.
            Items.give(player, proto, totalItems);
            msg.send(player, "transaction-failed", Placeholder.unparsed("error", error));
            return false;
        }
        msg.send(player, "sell-success",
                Placeholder.unparsed("item", totalItems + "x " + Items.name(proto)),
                Placeholder.unparsed("price", plugin.renderer().price(earned)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.9f);
        return true;
    }

    private void grantPermission(Player player, String node) {
        String mode = plugin.getConfig().getString("permission-grant.mode", "vault");
        if (mode.equalsIgnoreCase("vault") && plugin.economy().hasPermissionBackend()) {
            if (plugin.economy().addPermission(player, node)) return;
            plugin.getLogger().warning("Vault failed to add " + node + " to " + player.getName() + ", falling back to command.");
        }
        String command = plugin.getConfig().getString("permission-grant.command", "lp user %player% permission set %permission% true");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                command.replace("%player%", player.getName()).replace("%permission%", node));
    }

    private void runCommands(Player player, ShopItem item) {
        for (String command : item.commands()) {
            String cmd = command.startsWith("/") ? command.substring(1) : command;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString()));
        }
    }

    private static void fail(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }
}
