package io.github.mxr156.advancedshop.economy;

import org.bukkit.OfflinePlayer;

/** Abstraction over the money / permission backend so Vault stays optional. */
public interface EconomyHook {

    boolean hasEconomy();

    boolean hasPermissionBackend();

    boolean has(OfflinePlayer player, double amount);

    /** @return null on success, otherwise an error message */
    String withdraw(OfflinePlayer player, double amount);

    /** @return null on success, otherwise an error message */
    String deposit(OfflinePlayer player, double amount);

    boolean addPermission(OfflinePlayer player, String permission);

    String format(double amount);

    /** Used when Vault isn't installed: only free items work. */
    EconomyHook NONE = new EconomyHook() {
        @Override public boolean hasEconomy() { return false; }
        @Override public boolean hasPermissionBackend() { return false; }
        @Override public boolean has(OfflinePlayer player, double amount) { return amount <= 0; }
        @Override public String withdraw(OfflinePlayer player, double amount) { return amount <= 0 ? null : "no economy"; }
        @Override public String deposit(OfflinePlayer player, double amount) { return amount <= 0 ? null : "no economy"; }
        @Override public boolean addPermission(OfflinePlayer player, String permission) { return false; }
        @Override public String format(double amount) { return String.format("%,.2f", amount); }
    };
}
