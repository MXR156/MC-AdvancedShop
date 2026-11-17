package io.github.mxr156.advancedshop.listener;

import io.github.mxr156.advancedshop.AdvancedShopPlugin;
import io.github.mxr156.advancedshop.ShopManager;
import io.github.mxr156.advancedshop.gui.ShopEditorMenu;
import io.github.mxr156.advancedshop.gui.ShopMenu;
import io.github.mxr156.advancedshop.model.Shop;
import io.github.mxr156.advancedshop.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Creating shops by placing a renamed chest, opening them, and protecting them. */
public final class ChestListener implements Listener {

    /** Matches a chest renamed (in an anvil) to e.g. "[ashop.tools]". */
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\s*\\[ashop\\.([A-Za-z0-9_-]{1,32})]\\s*$");
    private static final List<BlockFace> SIDES = List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    private final AdvancedShopPlugin plugin;

    public ChestListener(AdvancedShopPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- creation

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!ShopManager.isShopContainerType(block)) return;

        String shopName = shopNameFromItem(event.getItemInHand());
        Player player = event.getPlayer();
        if (shopName != null && player.hasPermission("ashop.admin")) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!ShopManager.isShopContainerType(block)) return;
                Shop shop = plugin.shops().getOrCreate(shopName);
                plugin.shops().link(block, shop);
                splitDoubleChests(block);
                plugin.messages().send(player, "shop-created", Placeholder.unparsed("shop", shop.id()));
            });
        } else {
            // Stop normal chests merging with a shop chest into a double chest.
            plugin.getServer().getScheduler().runTask(plugin, () -> splitDoubleChests(block));
        }
    }

    private static String shopNameFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        Component name = meta.displayName();
        if (name == null) return null;
        Matcher matcher = NAME_PATTERN.matcher(Text.plain(name));
        return matcher.matches() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
    }

    /** Shop chests must stay single chests; un-merge {@code block} from any neighbour if a shop is involved. */
    private void splitDoubleChests(Block block) {
        if (!(block.getBlockData() instanceof Chest data) || data.getType() == Chest.Type.SINGLE) return;
        for (BlockFace face : SIDES) {
            Block other = block.getRelative(face);
            if (!(other.getBlockData() instanceof Chest otherData) || otherData.getType() == Chest.Type.SINGLE) continue;
            if (other.getType() != block.getType()) continue;
            if (plugin.shops().shopIdAt(block) == null && plugin.shops().shopIdAt(other) == null) continue;
            data.setType(Chest.Type.SINGLE);
            otherData.setType(Chest.Type.SINGLE);
            block.setBlockData(data, false);
            other.setBlockData(otherData, false);
        }
    }

    // ---------------------------------------------------------------- opening

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        String id = plugin.shops().shopIdAt(event.getClickedBlock());
        if (id == null) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (event.getHand() != EquipmentSlot.HAND) return; // only handle the main hand once

        Player player = event.getPlayer();
        Shop shop = plugin.shops().get(id);
        if (shop == null) {
            // Shop was deleted; turn this back into a normal container for admins.
            if (player.hasPermission("ashop.admin")) {
                plugin.shops().link(event.getClickedBlock(), null);
                plugin.messages().send(player, "shop-not-found", Placeholder.unparsed("shop", id));
            }
            return;
        }
        if (player.isSneaking() && player.hasPermission("ashop.admin")) {
            new ShopEditorMenu(plugin, shop).open(player);
        } else if (player.hasPermission("ashop.use")) {
            new ShopMenu(plugin, shop, player).open(player);
        } else {
            plugin.messages().send(player, "no-permission");
        }
    }

    // ---------------------------------------------------------------- protection

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String id = plugin.shops().shopIdAt(event.getBlock());
        if (id == null) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("ashop.admin")) {
            event.setCancelled(true);
            plugin.messages().send(player, "cannot-break");
            return;
        }
        plugin.messages().send(player, "shop-chest-broken", Placeholder.unparsed("shop", id));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.shops().shopIdAt(block) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.shops().shopIdAt(block) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        if (isShop(event.getSource().getLocation()) || isShop(event.getDestination().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean isShop(org.bukkit.Location location) {
        return location != null && plugin.shops().shopIdAt(location.getBlock()) != null;
    }

}
