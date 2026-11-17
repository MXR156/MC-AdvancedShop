package io.github.mxr156.advancedshop.listener;

import io.github.mxr156.advancedshop.AdvancedShopPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** One-shot chat prompts used by the editor GUIs (prices, permissions, commands...). */
public final class ChatInput implements Listener {

    private record Pending(Consumer<String> onInput, Runnable onCancel, BukkitTask timeout) {}

    private final AdvancedShopPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatInput(AdvancedShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void ask(Player player, String prompt, Consumer<String> onInput, Runnable onCancel) {
        long ticks = Math.max(5, plugin.getConfig().getLong("chat-input-timeout", 60)) * 20L;
        UUID id = player.getUniqueId();
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Pending expired = pending.remove(id);
            if (expired != null && player.isOnline()) {
                plugin.messages().send(player, "chat-expired");
                expired.onCancel().run();
            }
        }, ticks);
        Pending old = pending.put(id, new Pending(onInput, onCancel, timeout));
        if (old != null) old.timeout().cancel();
        plugin.messages().send(player, "chat-prompt", Placeholder.unparsed("prompt", prompt));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Pending input = pending.remove(event.getPlayer().getUniqueId());
        if (input == null) return;
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            input.timeout().cancel();
            if (!player.isOnline()) return;
            if (text.trim().equalsIgnoreCase("cancel")) {
                plugin.messages().send(player, "chat-cancelled");
                input.onCancel().run();
            } else {
                input.onInput().accept(text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Pending input = pending.remove(event.getPlayer().getUniqueId());
        if (input != null) input.timeout().cancel();
    }
}
