package io.github.mxr156.advancedshop.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Item helpers for GUIs and transactions. */
public final class Items {

    private Items() {}

    public static ItemStack button(Material material, String name, String... lore) {
        return button(material, name, List.of(lore));
    }

    public static ItemStack button(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.item(name));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) lines.add(Text.item(line));
        meta.lore(lines);
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack filler() {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.empty());
        meta.setHideTooltip(true);
        stack.setItemMeta(meta);
        return stack;
    }

    public static String name(ItemStack stack) {
        return Text.plain(stack.effectiveName());
    }

    /** How many of {@code proto} still fit into the player's main inventory. */
    public static int freeSpace(PlayerInventory inventory, ItemStack proto) {
        int max = proto.getMaxStackSize();
        int free = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) free += max;
            else if (slot.isSimilar(proto)) free += Math.max(0, max - slot.getAmount());
        }
        return free;
    }

    public static int count(PlayerInventory inventory, ItemStack proto) {
        int count = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot != null && slot.isSimilar(proto)) count += slot.getAmount();
        }
        return count;
    }

    /** Gives {@code amount} of {@code proto}, splitting into stacks; overflow drops at the player's feet. */
    public static void give(org.bukkit.entity.Player player, ItemStack proto, int amount) {
        int max = proto.getMaxStackSize();
        while (amount > 0) {
            ItemStack stack = proto.clone();
            int size = Math.min(max, amount);
            stack.setAmount(size);
            amount -= size;
            player.getInventory().addItem(stack).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    /** Removes {@code amount} of items similar to {@code proto}. Caller must check {@link #count} first. */
    public static void remove(PlayerInventory inventory, ItemStack proto, int amount) {
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack slot = contents[i];
            if (slot == null || !slot.isSimilar(proto)) continue;
            int take = Math.min(slot.getAmount(), amount);
            amount -= take;
            if (take == slot.getAmount()) contents[i] = null;
            else slot.setAmount(slot.getAmount() - take);
        }
        inventory.setStorageContents(contents);
    }
}
