package io.github.mxr156.advancedshop.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** A single configurable entry (slot) in a shop. */
public final class ShopItem {

    private ItemStack icon;
    private @Nullable ItemStack reward;
    private ShopItemType type = ShopItemType.ITEM;
    private double buyPrice = -1;
    private double sellPrice = -1;
    private int amount;
    private final List<String> permissions = new ArrayList<>();
    private final List<String> commands = new ArrayList<>();
    private @Nullable String displayName;
    private final List<String> description = new ArrayList<>();

    public ShopItem(ItemStack icon) {
        this.icon = icon.clone();
        this.amount = Math.max(1, icon.getAmount());
        this.icon.setAmount(1);
    }

    /** The icon shown in the shop GUI (amount 1). */
    public ItemStack icon() { return icon.clone(); }

    public void icon(ItemStack icon) {
        this.icon = icon.clone();
        this.icon.setAmount(1);
    }

    /** The item given to buyers / accepted from sellers (amount 1). Defaults to the icon. */
    public ItemStack rewardItem() {
        ItemStack stack = (reward != null ? reward : icon).clone();
        stack.setAmount(1);
        return stack;
    }

    public boolean hasCustomReward() { return reward != null; }

    public void reward(@Nullable ItemStack reward) {
        if (reward == null) {
            this.reward = null;
            return;
        }
        this.reward = reward.clone();
        this.reward.setAmount(1);
    }

    public @Nullable ItemStack rawReward() { return reward == null ? null : reward.clone(); }

    public ShopItemType type() { return type; }
    public void type(ShopItemType type) { this.type = type; }

    public double buyPrice() { return buyPrice; }
    public void buyPrice(double buyPrice) { this.buyPrice = buyPrice; }
    public boolean canBuy() { return buyPrice >= 0; }

    public double sellPrice() { return sellPrice; }
    public void sellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public boolean canSell() { return sellPrice >= 0 && type == ShopItemType.ITEM; }

    public int amount() { return amount; }
    public void amount(int amount) { this.amount = Math.max(1, Math.min(amount, 64 * 36)); }

    public List<String> permissions() { return permissions; }
    public List<String> commands() { return commands; }
    public List<String> description() { return description; }

    public @Nullable String displayName() { return displayName; }
    public void displayName(@Nullable String displayName) { this.displayName = displayName; }
}
