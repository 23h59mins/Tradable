package sphere.tradable.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class PlayerRefResolver {
    private PlayerRefResolver() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static UUID requireUuid(final Object ref) {
        final UUID uuid = resolveUuid(ref);
        if (uuid == null) {
            throw new IllegalArgumentException("Could not resolve a player UUID from: " + ref);
        }

        return uuid;
    }

    public static UUID resolveUuid(final Object ref) {
        return switch (ref) {
            case null -> null;
            case UUID uuid -> uuid;
            case Player player -> player.getUniqueId();
            case OfflinePlayer offlinePlayer -> offlinePlayer.getUniqueId();
            case String string -> resolveStringRef(string);
            default -> null;
        };
    }

    public static String resolveName(final Object ref) {
        return switch (ref) {
            case null -> null;
            case Player player -> player.getName();
            case OfflinePlayer offlinePlayer -> offlinePlayer.getName();
            case UUID uuid -> Bukkit.getOfflinePlayer(uuid).getName();
            case String string -> {
                final String trimmed = string.trim();
                yield trimmed.isEmpty() ? null : trimmed;
            }
            default -> Objects.toString(ref, null);
        };
    }

    public static OfflinePlayer resolveOfflinePlayer(final Object ref) {
        final UUID uuid = resolveUuid(ref);
        return uuid == null ? null : Bukkit.getOfflinePlayer(uuid);
    }

    private static UUID resolveStringRef(final String input) {
        final String raw = input == null ? "" : input.trim();
        if (raw.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(raw);
        } catch (final IllegalArgumentException ignored) {
        }

        final Player exact = Bukkit.getPlayerExact(raw);
        if (exact != null) {
            return exact.getUniqueId();
        }

        final Player partial = Bukkit.getPlayer(raw);
        if (partial != null) {
            return partial.getUniqueId();
        }

        final String lowered = raw.toLowerCase(Locale.ROOT);
        for (final OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            final String name = offlinePlayer.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).equals(lowered)) {
                return offlinePlayer.getUniqueId();
            }
        }

        return null;
    }
}
