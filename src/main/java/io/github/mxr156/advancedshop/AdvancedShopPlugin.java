package io.github.mxr156.advancedshop;

import io.github.mxr156.advancedshop.command.ShopCommand;
import io.github.mxr156.advancedshop.economy.EconomyHook;
import io.github.mxr156.advancedshop.economy.VaultHook;
import io.github.mxr156.advancedshop.gui.Menu;
import io.github.mxr156.advancedshop.listener.ChatInput;
import io.github.mxr156.advancedshop.listener.ChestListener;
import io.github.mxr156.advancedshop.listener.MenuListener;
import io.github.mxr156.advancedshop.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class AdvancedShopPlugin extends JavaPlugin {

    private ShopManager shops;
    private Messages messages;
    private EconomyHook economy = EconomyHook.NONE;
    private ChatInput chatInput;
    private Transactions transactions;
    private ItemRenderer renderer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(getConfig());
        shops = new ShopManager(this);
        shops.loadAll();
        chatInput = new ChatInput(this);
        transactions = new Transactions(this);
        renderer = new ItemRenderer(this);

        getServer().getPluginManager().registerEvents(new ChestListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(chatInput, this);

        registerCommand("ashop", "AdvancedShop admin and utility commands", List.of("shop", "advancedshop"), new ShopCommand(this));

        // Economy plugins register their Vault provider during their own enable, so hook one tick later.
        getServer().getScheduler().runTask(this, this::hookEconomy);
    }

    @Override
    public void onDisable() {
        // Close our GUIs so nobody keeps a stale menu across reloads.
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
                player.closeInventory();
            }
        }
        if (shops != null) shops.saveAll();
    }

    private void hookEconomy() {
        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
        } catch (ClassNotFoundException e) {
            getLogger().warning("Vault not found - only free shop items will work and bought permissions use the command mode.");
            economy = EconomyHook.NONE;
            return;
        }
        economy = VaultHook.create();
        getLogger().info("Vault hooked. Economy: " + (economy.hasEconomy() ? "yes" : "NO")
                + ", permissions: " + (economy.hasPermissionBackend() ? "yes" : "no"));
    }

    public void reload() {
        reloadConfig();
        messages = new Messages(getConfig());
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
                player.closeInventory();
            }
        }
        shops.loadAll();
        hookEconomy();
    }

    public ShopManager shops() { return shops; }
    public Messages messages() { return messages; }
    public EconomyHook economy() { return economy; }
    public ChatInput chatInput() { return chatInput; }
    public Transactions transactions() { return transactions; }
    public ItemRenderer renderer() { return renderer; }
}
