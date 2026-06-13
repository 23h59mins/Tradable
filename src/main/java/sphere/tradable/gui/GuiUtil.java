package sphere.tradable.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import sphere.tradable.util.MoneyUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class GuiUtil {
    private static final String PREFIX_OK = "&6&lTradable &8» &a";
    private static final String PREFIX_FAIL = "&6&lTradable &8» &c";
    private static final String PREFIX_INFO = "&6&lTradable &8» &7";
    private static final String DIVIDER = "&8&m------------------------";
    private static final String UNKNOWN_PLAYER = "Unknown Trader";
    private static final String UNKNOWN_TIME = "Recently";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static final List<ItemFlag> DEFAULT_FLAGS = List.of(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_DESTROYS,
            ItemFlag.HIDE_PLACED_ON,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP
    );

    private GuiUtil() {
    }

    public static String color(final String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> colorize(final List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }

        final List<String> out = new ArrayList<>(lines.size());
        for (final String line : lines) {
            out.add(color(line));
        }
        return out;
    }

    public static List<String> colorLines(final List<String> lines) {
        return colorize(lines);
    }

    public static List<String> cleanLore(final List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }

        final List<String> cleaned = new ArrayList<>();
        boolean previousBlank = false;

        for (final String line : lines) {
            final String value = line == null ? "" : line.trim();
            final boolean blank = value.isEmpty();

            if (blank && previousBlank) {
                continue;
            }

            cleaned.add(value);
            previousBlank = blank;
        }

        while (!cleaned.isEmpty() && cleaned.get(0).isEmpty()) {
            cleaned.remove(0);
        }

        while (!cleaned.isEmpty() && cleaned.get(cleaned.size() - 1).isEmpty()) {
            cleaned.remove(cleaned.size() - 1);
        }

        return cleaned;
    }

    public static List<String> lore(final String... lines) {
        if (lines == null || lines.length == 0) {
            return new ArrayList<>();
        }

        final List<String> out = new ArrayList<>(lines.length);
        Collections.addAll(out, lines);
        return out;
    }

    public static List<String> section(final String title, final List<String> body) {
        final List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("&6&l" + strip(title));
        if (body != null && !body.isEmpty()) {
            lore.addAll(body);
        }
        lore.add(DIVIDER);
        return lore;
    }

    public static void ok(final CommandSender sender, final String message) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }

        sender.sendMessage(color(PREFIX_OK + stripLeadingColor(message)));
    }

    public static void fail(final CommandSender sender, final String message) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }

        sender.sendMessage(color(PREFIX_FAIL + stripLeadingColor(message)));
    }

    public static void info(final CommandSender sender, final String message) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }

        sender.sendMessage(color(PREFIX_INFO + stripLeadingColor(message)));
    }

    public static void click(final Player player) {
        if (player == null) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.65F, 1.05F);
    }

    public static void successSound(final Player player) {
        if (player == null) {
            return;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.70F, 1.15F);
    }

    public static void failSound(final Player player) {
        if (player == null) {
            return;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.60F, 1.00F);
    }

    public static ItemStack item(final Material material, final String name, final List<String> lore) {
        return item(material, 1, name, lore);
    }

    public static ItemStack item(final Material material, final int amount, final String name, final List<String> lore) {
        final Material safeMaterial = material == null ? Material.STONE : material;
        final int safeAmount = Math.max(1, amount);

        final ItemStack item = new ItemStack(safeMaterial, safeAmount);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        applyMeta(meta, name, lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack button(final Material material, final String name, final String... lore) {
        return item(material, name, lore(lore));
    }

    public static ItemStack head(final OfflinePlayer player, final String name, final List<String> lore) {
        final ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        final ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item(materialOrHeadFallback(), name, lore);
        }

        if (player != null) {
            meta.setOwningPlayer(player);
        }

        applyMeta(meta, name, lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void applyMeta(final ItemMeta meta, final String name, final List<String> lore) {
        if (meta == null) {
            return;
        }

        if (name != null && !name.isBlank()) {
            meta.setDisplayName(color(name));
        }

        final List<String> cleanedLore = cleanLore(lore);
        if (!cleanedLore.isEmpty()) {
            meta.setLore(colorize(cleanedLore));
        }

        for (final ItemFlag flag : DEFAULT_FLAGS) {
            meta.addItemFlags(flag);
        }
    }

    public static String formatInstant(final Instant instant) {
        return instant == null ? UNKNOWN_TIME : DATE_TIME.format(instant);
    }

    public static String money(final double amount) {
        return "$" + MoneyUtil.formatCompact(amount);
    }

    public static String compact(final double amount) {
        return MoneyUtil.formatCompact(amount);
    }

    public static String moneyFull(final double amount) {
        return MoneyUtil.format(amount);
    }

    public static String name(final OfflinePlayer player) {
        if (player == null) {
            return UNKNOWN_PLAYER;
        }

        final String name = player.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }

        final UUID uuid = player.getUniqueId();
        return uuid == null ? UNKNOWN_PLAYER : uuid.toString();
    }

    public static OfflinePlayer offlinePlayer(final String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    public static Material material(final String raw, final Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return fallback;
        }
    }

    public static String prettify(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        final String[] parts = raw.toLowerCase(Locale.ROOT).split("[_\\-\\s]+");
        final StringBuilder builder = new StringBuilder();

        for (final String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }

    public static String strip(final String text) {
        if (text == null) {
            return "";
        }

        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }

    public static boolean fits(final Inventory inventory, final ItemStack stack) {
        if (inventory == null || stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return false;
        }

        final ItemStack[] contents = inventory.getStorageContents().clone();
        int remaining = stack.getAmount();

        for (final ItemStack slot : contents) {
            if (slot == null || slot.getType().isAir()) {
                continue;
            }

            if (!slot.isSimilar(stack)) {
                continue;
            }

            final int max = Math.max(1, slot.getMaxStackSize());
            final int free = Math.max(0, max - slot.getAmount());
            if (free <= 0) {
                continue;
            }

            remaining -= free;
            if (remaining <= 0) {
                return true;
            }
        }

        final int maxStack = Math.max(1, stack.getMaxStackSize());
        for (final ItemStack slot : contents) {
            if (slot == null || slot.getType().isAir()) {
                remaining -= maxStack;
                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return remaining <= 0;
    }

    public static boolean fitsAll(final Inventory inventory, final List<ItemStack> stacks) {
        if (inventory == null || stacks == null || stacks.isEmpty()) {
            return false;
        }

        final ItemStack[] contents = inventory.getStorageContents().clone();

        for (final ItemStack original : stacks) {
            if (original == null || original.getType().isAir() || original.getAmount() <= 0) {
                continue;
            }

            final ItemStack probe = original.clone();
            int remaining = probe.getAmount();

            for (final ItemStack slot : contents) {
                if (slot == null || slot.getType().isAir()) {
                    continue;
                }

                if (!slot.isSimilar(probe)) {
                    continue;
                }

                final int free = Math.max(0, slot.getMaxStackSize() - slot.getAmount());
                if (free <= 0) {
                    continue;
                }

                final int moved = Math.min(free, remaining);
                slot.setAmount(slot.getAmount() + moved);
                remaining -= moved;

                if (remaining <= 0) {
                    break;
                }
            }

            while (remaining > 0) {
                final int emptySlot = firstEmpty(contents);
                if (emptySlot < 0) {
                    return false;
                }

                final int placed = Math.min(probe.getMaxStackSize(), remaining);
                final ItemStack clone = probe.clone();
                clone.setAmount(placed);
                contents[emptySlot] = clone;
                remaining -= placed;
            }
        }

        return true;
    }

    private static int firstEmpty(final ItemStack[] contents) {
        for (int i = 0; i < contents.length; i++) {
            final ItemStack slot = contents[i];
            if (slot == null || slot.getType().isAir()) {
                return i;
            }
        }
        return -1;
    }

    private static String stripLeadingColor(final String text) {
        return text == null ? "" : text.replaceFirst("^(?:&[0-9a-fk-or])+","").trim();
    }

    private static Material materialOrHeadFallback() {
        return Material.matchMaterial("PLAYER_HEAD") != null ? Material.PLAYER_HEAD : Material.STONE;
    }
}