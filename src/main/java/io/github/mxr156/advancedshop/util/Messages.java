package io.github.mxr156.advancedshop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Reads messages and lore templates from config.yml. */
public final class Messages {

    private final FileConfiguration config;

    public Messages(FileConfiguration config) {
        this.config = config;
    }

    public String raw(String key) {
        ConfigurationSection section = config.getConfigurationSection("messages");
        String value = section == null ? null : section.getString(key);
        return value == null ? "<red>Missing message: " + key : value;
    }

    public Component get(String key, TagResolver... resolvers) {
        return Text.mm(raw("prefix") + raw(key), resolvers);
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        sender.sendMessage(get(key, resolvers));
    }

    public String lore(String key) {
        return config.getString("lore." + key, "");
    }
}
