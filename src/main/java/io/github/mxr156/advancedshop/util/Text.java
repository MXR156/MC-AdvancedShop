package io.github.mxr156.advancedshop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Small MiniMessage helpers. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {}

    /** Parses MiniMessage. */
    public static Component mm(String input, TagResolver... resolvers) {
        return MM.deserialize(input, resolvers);
    }

    /** Parses MiniMessage for item names / lore (no default italics). */
    public static Component item(String input, TagResolver... resolvers) {
        return MM.deserialize(input, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
