package sphere.tradable.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

public final class CommandUtil {
    private CommandUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean hasPermission(final CommandSender sender, final String permission) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(permission, "permission");

        if (sender.hasPermission(permission)) {
            return true;
        }

        MessageUtil.sendKey(sender, "general.no-permission");
        return false;
    }

    public static Player requirePlayer(final CommandSender sender) {
        Objects.requireNonNull(sender, "sender");

        if (sender instanceof Player player) {
            return player;
        }

        MessageUtil.sendKey(sender, "general.player-only");
        return null;
    }

    public static Player findOnlinePlayer(final CommandSender sender, final String name) {
        Objects.requireNonNull(sender, "sender");

        if (name == null || name.isBlank()) {
            MessageUtil.sendKey(sender, "general.invalid-player");
            return null;
        }

        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            target = Bukkit.getPlayer(name);
        }

        if (target == null || !target.isOnline()) {
            MessageUtil.sendKey(sender, "general.player-not-found", Map.of("player", name));
            return null;
        }

        return target;
    }

    public static BigDecimal parseAmount(final String input) {
        if (input == null) {
            return null;
        }

        try {
            return MoneyUtil.parseBigDecimal(input).stripTrailingZeros();
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    public static BigDecimal parsePositiveAmount(final CommandSender sender, final String input) {
        Objects.requireNonNull(sender, "sender");

        BigDecimal amount = parseAmount(input);
        if (amount == null) {
            MessageUtil.sendKey(sender, "general.invalid-amount", Map.of("input", String.valueOf(input)));
            return null;
        }

        if (amount.scale() > 2) {
            amount = amount.setScale(2, RoundingMode.DOWN).stripTrailingZeros();
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            MessageUtil.sendKey(sender, "general.amount-must-be-positive");
            return null;
        }

        return amount;
    }

    public static boolean ensureMinArgs(final CommandSender sender, final String[] args, final int minArgs, final String usageKey) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(usageKey, "usageKey");

        if (args.length >= minArgs) {
            return true;
        }

        MessageUtil.sendKey(sender, usageKey);
        return false;
    }
}
