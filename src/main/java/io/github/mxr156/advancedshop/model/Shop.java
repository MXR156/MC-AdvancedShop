package io.github.mxr156.advancedshop.model;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/** A named shop: a title, a size and a set of items keyed by GUI slot. */
public final class Shop {

    private final String id;
    private String title;
    private int rows;
    private final Map<Integer, ShopItem> items = new TreeMap<>();

    public Shop(String id, String title, int rows) {
        this.id = id;
        this.title = title;
        rows(rows);
    }

    public String id() { return id; }

    public String title() { return title; }
    public void title(String title) { this.title = title; }

    public int rows() { return rows; }
    public void rows(int rows) { this.rows = Math.max(1, Math.min(6, rows)); }
    public int size() { return rows * 9; }

    public Map<Integer, ShopItem> items() { return items; }
    public @Nullable ShopItem item(int slot) { return items.get(slot); }
    public void setItem(int slot, @Nullable ShopItem item) {
        if (item == null) items.remove(slot); else items.put(slot, item);
    }
}
