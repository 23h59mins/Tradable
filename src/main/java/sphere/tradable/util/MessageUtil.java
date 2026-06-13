package sphere.tradable.util;


import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import sphere.tradable.Tradable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MessageUtil {
    private static final String PREFIX_PATH = "prefix";

    private MessageUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String colorize(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<String> colorize(final List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> out = new ArrayList<>(lines.size());
        for (final String line : lines) {
            out.add(colorize(line));
        }
        return out;
    }

    public static void send(final CommandSender sender, final String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }

        sender.sendMessage(colorize(message));
    }

    public static void send(final CommandSender sender, final String message, final Map<String, String> placeholders) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }

        sender.sendMessage(applyPlaceholders(message, placeholders));
    }

    public static void sendList(final CommandSender sender, final List<String> messages) {
        if (sender == null || messages == null || messages.isEmpty()) {
            return;
        }

        for (final String message : messages) {
            send(sender, message);
        }
    }

    public static void sendList(final CommandSender sender, final List<String> messages, final Map<String, String> placeholders) {
        if (sender == null || messages == null || messages.isEmpty()) {
            return;
        }

        for (final String message : messages) {
            send(sender, message, placeholders);
        }
    }

    public static String getMessage(final String path) {
        return getMessage(getPlugin(), path);
    }

    public static String getMessage(final JavaPlugin plugin, final String path) {
        return getMessage(plugin, path, Collections.emptyMap());
    }

    public static String getMessage(final String path, final Map<String, String> placeholders) {
        return getMessage(getPlugin(), path, placeholders);
    }

    public static String getMessage(final JavaPlugin plugin, final String path, final Map<String, String> placeholders) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(path, "path");

        final FileConfiguration messages = resolveMessagesConfig(plugin);
        final String raw = messages.getString(path);

        if (raw == null) {
            return colorize("&cMissing message: " + path);
        }

        return applyPlaceholders(plugin, raw, placeholders);
    }

    public static List<String> getMessageList(final String path) {
        return getMessageList(getPlugin(), path);
    }

    public static List<String> getMessageList(final JavaPlugin plugin, final String path) {
        return getMessageList(plugin, path, Collections.emptyMap());
    }

    public static List<String> getMessageList(final String path, final Map<String, String> placeholders) {
        return getMessageList(getPlugin(), path, placeholders);
    }

    public static List<String> getMessageList(final JavaPlugin plugin, final String path, final Map<String, String> placeholders) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(path, "path");

        final FileConfiguration messages = resolveMessagesConfig(plugin);
        final List<String> raw = messages.getStringList(path);

        if (raw == null || raw.isEmpty()) {
            return Collections.singletonList(colorize("&cMissing message list: " + path));
        }

        final List<String> out = new ArrayList<>(raw.size());
        for (final String line : raw) {
            out.add(applyPlaceholders(plugin, line, placeholders));
        }
        return out;
    }

    public static void sendKey(final CommandSender sender, final String path) {
        sendKey(getPlugin(), sender, path, Collections.emptyMap());
    }

    public static void sendKey(final CommandSender sender, final String path, final Map<String, String> placeholders) {
        sendKey(getPlugin(), sender, path, placeholders);
    }

    public static void sendKey(final JavaPlugin plugin, final CommandSender sender, final String path) {
        sendKey(plugin, sender, path, Collections.emptyMap());
    }

    public static void sendKey(
            final JavaPlugin plugin,
            final CommandSender sender,
            final String path,
            final Map<String, String> placeholders
    ) {
        if (sender == null) {
            return;
        }

        send(sender, getMessage(plugin, path, placeholders));
    }

    public static void sendKeyList(final CommandSender sender, final String path) {
        sendKeyList(getPlugin(), sender, path, Collections.emptyMap());
    }

    public static void sendKeyList(final CommandSender sender, final String path, final Map<String, String> placeholders) {
        sendKeyList(getPlugin(), sender, path, placeholders);
    }

    public static void sendKeyList(final JavaPlugin plugin, final CommandSender sender, final String path) {
        sendKeyList(plugin, sender, path, Collections.emptyMap());
    }

    public static void sendKeyList(
            final JavaPlugin plugin,
            final CommandSender sender,
            final String path,
            final Map<String, String> placeholders
    ) {
        if (sender == null) {
            return;
        }

        sendList(sender, getMessageList(plugin, path, placeholders));
    }

    public static String applyPlaceholders(final String message, final Map<String, String> placeholders) {
        return applyPlaceholders(getPlugin(), message, placeholders);
    }

    public static String applyPlaceholders(
            final JavaPlugin plugin,
            final String message,
            final Map<String, String> placeholders
    ) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String result = message;
        final Map<String, String> merged = new LinkedHashMap<>();
        merged.put("prefix", getPrefix(plugin));

        if (placeholders != null && !placeholders.isEmpty()) {
            merged.putAll(placeholders);
        }

        for (final Map.Entry<String, String> entry : merged.entrySet()) {
            result = result.replace(
                    "%" + entry.getKey() + "%",
                    entry.getValue() == null ? "" : entry.getValue()
            );
        }

        return colorize(result);
    }

    public static String getPrefix() {
        return getPrefix(getPlugin());
    }

    public static String getPrefix(final JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        final FileConfiguration messages = resolveMessagesConfig(plugin);
        final String raw = messages.getString(PREFIX_PATH, "&6&lTradable &8» ");
        return colorize(raw);
    }

    public static Map<String, String> placeholders(final String key, final Object value) {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(key, stringify(value));
        return map;
    }

    public static Map<String, String> placeholders(
            final String key1, final Object value1,
            final String key2, final Object value2
    ) {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(key1, stringify(value1));
        map.put(key2, stringify(value2));
        return map;
    }

    public static Map<String, String> placeholders(
            final String key1, final Object value1,
            final String key2, final Object value2,
            final String key3, final Object value3
    ) {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(key1, stringify(value1));
        map.put(key2, stringify(value2));
        map.put(key3, stringify(value3));
        return map;
    }

    public static Map<String, String> placeholders(
            final String key1, final Object value1,
            final String key2, final Object value2,
            final String key3, final Object value3,
            final String key4, final Object value4
    ) {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(key1, stringify(value1));
        map.put(key2, stringify(value2));
        map.put(key3, stringify(value3));
        map.put(key4, stringify(value4));
        return map;
    }

    public static String formatMoney(final BigDecimal amount) {
        return MoneyUtil.formatCompact(amount);
    }

    public static String formatMoney(final double amount) {
        return MoneyUtil.formatCompact(amount);
    }

    public static String formatMoneyFull(final BigDecimal amount) {
        return MoneyUtil.toPlainString(amount);
    }

    public static String formatMoneyFull(final double amount) {
        return MoneyUtil.format(amount);
    }

    public static String toPlainString(final BigDecimal value) {
        return MoneyUtil.toPlainString(value);
    }

    public static String stringify(final Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof BigDecimal decimal) {
            return toPlainString(decimal);
        }

        if (value instanceof Double number) {
            return formatMoney(number);
        }

        if (value instanceof Float number) {
            return formatMoney(number.doubleValue());
        }

        return String.valueOf(value);
    }

    private static Tradable getPlugin() {
        return JavaPlugin.getPlugin(Tradable.class);
    }

    private static FileConfiguration resolveMessagesConfig(final JavaPlugin plugin) {
        if (plugin instanceof Tradable tradable && tradable.getMessages() != null) {
            return tradable.getMessages();
        }

        return plugin.getConfig();
    }
}