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
        return MM.deserialize(legacyToMini(input), resolvers);
    }

    /** Parses MiniMessage for item names / lore (no default italics). */
    public static Component item(String input, TagResolver... resolvers) {
        return MM.deserialize(legacyToMini(input), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static final java.util.regex.Pattern LEGACY =
            java.util.regex.Pattern.compile("[&\u00A7](#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR])");

    /**
     * Converts legacy colour codes ({@code &6}, {@code &l}, {@code &#ff8800}) into MiniMessage tags,
     * so both styles can be used (and mixed) everywhere text is configured.
     */
    public static String legacyToMini(String input) {
        if (input.indexOf('&') < 0 && input.indexOf('\u00A7') < 0) return input;
        java.util.regex.Matcher m = LEGACY.matcher(input);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String code = m.group(1).toLowerCase(java.util.Locale.ROOT);
            String tag = code.startsWith("#") ? "<reset><" + code + ">" : switch (code.charAt(0)) {
                case '0' -> "<reset><black>";
                case '1' -> "<reset><dark_blue>";
                case '2' -> "<reset><dark_green>";
                case '3' -> "<reset><dark_aqua>";
                case '4' -> "<reset><dark_red>";
                case '5' -> "<reset><dark_purple>";
                case '6' -> "<reset><gold>";
                case '7' -> "<reset><gray>";
                case '8' -> "<reset><dark_gray>";
                case '9' -> "<reset><blue>";
                case 'a' -> "<reset><green>";
                case 'b' -> "<reset><aqua>";
                case 'c' -> "<reset><red>";
                case 'd' -> "<reset><light_purple>";
                case 'e' -> "<reset><yellow>";
                case 'f' -> "<reset><white>";
                case 'k' -> "<obfuscated>";
                case 'l' -> "<bold>";
                case 'm' -> "<strikethrough>";
                case 'n' -> "<underlined>";
                case 'o' -> "<italic>";
                default -> "<reset>";
            };
            m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(tag));
        }
        m.appendTail(out);
        return out.toString();
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
