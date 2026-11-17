package io.github.mxr156.advancedshop.model;

/** What a shop entry gives the buyer. */
public enum ShopItemType {
    /** Gives (and buys back) a physical item. */
    ITEM,
    /** Grants one or more permission nodes. Cannot be sold. */
    PERMISSION,
    /** Only runs the configured console commands. Cannot be sold. */
    COMMAND;

    public ShopItemType next() {
        ShopItemType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
