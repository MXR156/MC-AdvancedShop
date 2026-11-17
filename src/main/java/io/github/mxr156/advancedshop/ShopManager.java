package io.github.mxr156.advancedshop;

import io.github.mxr156.advancedshop.gui.ShopMenu;
import io.github.mxr156.advancedshop.model.Shop;
import io.github.mxr156.advancedshop.model.ShopItem;
import io.github.mxr156.advancedshop.model.ShopItemType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

/** Loads, saves and looks up shops, and links containers to shops. */
public final class ShopManager {

    public static final Pattern VALID_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private final AdvancedShopPlugin plugin;
    private final File folder;
    private final NamespacedKey shopKey;
    private final Map<String, Shop> shops = new LinkedHashMap<>();

    public ShopManager(AdvancedShopPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "shops");
        this.shopKey = new NamespacedKey(plugin, "shop");
    }

    // ------------------------------------------------------------------ lookup

    public @Nullable Shop get(String id) {
        return shops.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Shop> all() {
        return Collections.unmodifiableCollection(shops.values());
    }

    public Shop getOrCreate(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        Shop shop = shops.get(key);
        if (shop == null) {
            shop = new Shop(key,
                    plugin.getConfig().getString("defaults.title", "<dark_green>Shop").replace("<shop>", key),
                    plugin.getConfig().getInt("defaults.rows", 6));
            shops.put(key, shop);
            save(shop);
        }
        return shop;
    }

    public void delete(Shop shop) {
        shops.remove(shop.id());
        File file = file(shop.id());
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete " + file);
        }
    }

    public static boolean isValidId(String id) {
        return VALID_ID.matcher(id.toLowerCase(Locale.ROOT)).matches();
    }

    // ------------------------------------------------------------------ chest links

    public static boolean isShopContainerType(Block block) {
        return switch (block.getType()) {
            case CHEST, TRAPPED_CHEST, BARREL -> true;
            default -> false;
        };
    }

    /** @return the shop id stored on this block, or null if it isn't a shop container. */
    public @Nullable String shopIdAt(Block block) {
        if (!isShopContainerType(block)) return null;
        BlockState state = block.getState(false);
        if (!(state instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(shopKey, PersistentDataType.STRING);
    }

    public @Nullable Shop shopAt(Block block) {
        String id = shopIdAt(block);
        return id == null ? null : get(id);
    }

    public void link(Block block, @Nullable Shop shop) {
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return;
        if (shop == null) tile.getPersistentDataContainer().remove(shopKey);
        else tile.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, shop.id());
        tile.update(true, false);
    }

    // ------------------------------------------------------------------ live refresh

    /** Re-renders the shop for every player currently looking at it. */
    public void refreshViewers(Shop shop) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof ShopMenu menu
                    && menu.shop() == shop) {
                menu.render();
            }
        }
    }

    // ------------------------------------------------------------------ persistence

    private File file(String id) {
        return new File(folder, id + ".yml");
    }

    public void loadAll() {
        shops.clear();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create " + folder);
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            try {
                shops.put(id, load(id, YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load shop " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Loaded " + shops.size() + " shop(s).");
    }

    private Shop load(String id, YamlConfiguration yaml) {
        Shop shop = new Shop(id, yaml.getString("title", id), yaml.getInt("rows", 6));
        shop.permission(yaml.getString("permission"));
        ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items == null) return shop;
        for (String key : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(key);
            if (s == null) continue;
            try {
                int slot = Integer.parseInt(key);
                ItemStack icon = decode(s.getString("icon"));
                if (icon == null) continue;
                ShopItem item = new ShopItem(icon);
                item.reward(decode(s.getString("reward")));
                item.type(ShopItemType.valueOf(s.getString("type", "ITEM").toUpperCase(Locale.ROOT)));
                item.amount(s.getInt("amount", 1));
                item.buyPrice(s.getDouble("buy", -1));
                item.sellPrice(s.getDouble("sell", -1));
                item.permissions().addAll(s.getStringList("permissions"));
                item.commands().addAll(s.getStringList("commands"));
                item.displayName(s.getString("name"));
                item.description().addAll(s.getStringList("description"));
                shop.setItem(slot, item);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Skipping broken item " + key + " in shop " + id, e);
            }
        }
        return shop;
    }

    public void save(Shop shop) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("title", shop.title());
        yaml.set("rows", shop.rows());
        yaml.set("permission", shop.permission());
        for (Map.Entry<Integer, ShopItem> entry : shop.items().entrySet()) {
            ShopItem item = entry.getValue();
            String path = "items." + entry.getKey() + ".";
            yaml.set(path + "type", item.type().name());
            yaml.set(path + "amount", item.amount());
            yaml.set(path + "buy", item.buyPrice());
            yaml.set(path + "sell", item.sellPrice());
            yaml.set(path + "name", item.displayName());
            yaml.set(path + "description", item.description());
            yaml.set(path + "permissions", item.permissions());
            yaml.set(path + "commands", item.commands());
            yaml.set(path + "icon", encode(item.icon()));
            yaml.set(path + "reward", encode(item.rawReward()));
        }
        try {
            if (!folder.exists()) folder.mkdirs();
            yaml.save(file(shop.id()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save shop " + shop.id(), e);
        }
    }

    public void saveAll() {
        shops.values().forEach(this::save);
    }

    /** Items are stored as Paper's version-safe byte format (upgraded automatically across MC versions). */
    private static @Nullable String encode(@Nullable ItemStack stack) {
        return stack == null ? null : Base64.getEncoder().encodeToString(stack.serializeAsBytes());
    }

    private static @Nullable ItemStack decode(@Nullable String data) {
        return data == null || data.isBlank() ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
    }
}
