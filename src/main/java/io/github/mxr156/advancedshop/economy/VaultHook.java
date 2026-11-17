package io.github.mxr156.advancedshop.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Vault-backed implementation. Only loaded when Vault (or VaultUnlocked) is present. */
public final class VaultHook implements EconomyHook {

    private final Economy economy;
    private final Permission permission;

    private VaultHook(Economy economy, Permission permission) {
        this.economy = economy;
        this.permission = permission;
    }

    public static EconomyHook create() {
        RegisteredServiceProvider<Economy> eco = Bukkit.getServicesManager().getRegistration(Economy.class);
        RegisteredServiceProvider<Permission> perm = Bukkit.getServicesManager().getRegistration(Permission.class);
        return new VaultHook(eco == null ? null : eco.getProvider(), perm == null ? null : perm.getProvider());
    }

    @Override public boolean hasEconomy() { return economy != null; }

    @Override public boolean hasPermissionBackend() { return permission != null; }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;
        return economy != null && economy.has(player, amount);
    }

    @Override
    public String withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) return null;
        if (economy == null) return "no economy";
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess() ? null : response.errorMessage;
    }

    @Override
    public String deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return null;
        if (economy == null) return "no economy";
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess() ? null : response.errorMessage;
    }

    @Override
    public boolean addPermission(OfflinePlayer player, String node) {
        return permission != null && permission.playerAdd(null, player, node);
    }

    @Override
    public String format(double amount) {
        return economy != null ? economy.format(amount) : String.format("%,.2f", amount);
    }
}
