// File: src/main/java/sphere/tradable/storage/BalanceStorage.java
package sphere.tradable.storage;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BalanceStorage extends YamlDataFile {
    private static final String ROOT = "balances";
    private static final double EPSILON = 0.000001D;

    public BalanceStorage(final JavaPlugin plugin) {
        super(plugin, "balances.yml");
    }

    public synchronized double getBalance(final UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return sanitize(config().getDouble(path(playerId) + ".amount", 0D));
    }

    public synchronized double getBalance(final String playerId) {
        return getBalance(parseUuid(playerId));
    }

    public synchronized boolean hasAccount(final UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return config().isConfigurationSection(path(playerId));
    }

    public synchronized boolean hasBalance(final UUID playerId, final double amount) {
        return hasEnough(getBalance(playerId), sanitizePositive(amount));
    }

    public synchronized void setBalance(final UUID playerId, final double amount) {
        setBalance(playerId, null, amount);
    }

    public synchronized void setBalance(final String playerId, final double amount) {
        setBalance(parseUuid(playerId), amount);
    }

    public synchronized void setBalance(final OfflinePlayer player, final double amount) {
        Objects.requireNonNull(player, "player");
        setBalance(player.getUniqueId(), player.getName(), amount);
    }

    public synchronized void setBalance(final UUID playerId, final String playerName, final double amount) {
        writeBalance(playerId, playerName, amount);
        save();
    }

    public synchronized double addBalance(final UUID playerId, final double amount) {
        return addBalance(playerId, null, amount);
    }

    public synchronized double addBalance(final String playerId, final double amount) {
        return addBalance(parseUuid(playerId), amount);
    }

    public synchronized double addBalance(final OfflinePlayer player, final double amount) {
        Objects.requireNonNull(player, "player");
        return addBalance(player.getUniqueId(), player.getName(), amount);
    }

    public synchronized double addBalance(final UUID playerId, final String playerName, final double amount) {
        final double value = sanitizePositive(amount);
        final double updated = sanitize(getBalance(playerId) + value);

        writeBalance(playerId, playerName, updated);
        save();

        return updated;
    }

    public synchronized boolean subtractBalance(final UUID playerId, final double amount) {
        return subtractBalance(playerId, null, amount);
    }

    public synchronized boolean subtractBalance(final String playerId, final double amount) {
        return subtractBalance(parseUuid(playerId), amount);
    }

    public synchronized boolean subtractBalance(final OfflinePlayer player, final double amount) {
        Objects.requireNonNull(player, "player");
        return subtractBalance(player.getUniqueId(), player.getName(), amount);
    }

    public synchronized boolean subtractBalance(final UUID playerId, final String playerName, final double amount) {
        Objects.requireNonNull(playerId, "playerId");

        final double value = sanitizePositive(amount);
        final double current = getBalance(playerId);

        if (!hasEnough(current, value)) {
            return false;
        }

        writeBalance(playerId, playerName, current - value);
        save();

        return true;
    }

    public synchronized boolean transfer(final UUID from, final UUID to, final double amount) {
        return transfer(from, null, to, null, amount);
    }

    public synchronized boolean transfer(
            final UUID from,
            final String fromName,
            final UUID to,
            final String toName,
            final double amount
    ) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        final double value = sanitizePositive(amount);

        if (from.equals(to)) {
            return false;
        }

        final double fromBalance = getBalance(from);
        if (!hasEnough(fromBalance, value)) {
            return false;
        }

        final double toBalance = getBalance(to);

        writeBalance(from, fromName, fromBalance - value);
        writeBalance(to, toName, toBalance + value);
        save();

        return true;
    }

    public synchronized void deleteBalance(final UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        remove(path(playerId));
        save();
    }

    public synchronized String getLastKnownName(final UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return config().getString(path(playerId) + ".last-known-name");
    }

    public synchronized Map<UUID, Double> getAllBalances() {
        final ConfigurationSection root = config().getConfigurationSection(ROOT);
        if (root == null) {
            return Collections.emptyMap();
        }

        final Map<UUID, Double> balances = new LinkedHashMap<>();

        for (final String key : root.getKeys(false)) {
            try {
                final UUID playerId = UUID.fromString(key);
                final double amount = sanitize(root.getDouble(key + ".amount", 0D));
                balances.put(playerId, amount);
            } catch (final IllegalArgumentException ignored) {
            }
        }

        return balances;
    }

    public synchronized Map<UUID, Double> getTopBalances(final int limit) {
        if (limit <= 0) {
            return Collections.emptyMap();
        }

        return getAllBalances()
                .entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private void writeBalance(final UUID playerId, final String playerName, final double amount) {
        Objects.requireNonNull(playerId, "playerId");

        final double value = sanitize(amount);
        final String base = path(playerId);

        config().set(base + ".amount", value);
        config().set(base + ".updated-at", System.currentTimeMillis());

        if (playerName != null && !playerName.isBlank()) {
            config().set(base + ".last-known-name", playerName);
        }
    }

    private String path(final UUID playerId) {
        return ROOT + "." + playerId;
    }

    private UUID parseUuid(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UUID cannot be blank.");
        }

        return UUID.fromString(value);
    }

    private boolean hasEnough(final double current, final double required) {
        return current + EPSILON >= required;
    }

    private double sanitize(final double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return 0D;
        }

        return BigDecimal.valueOf(Math.max(0D, amount))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double sanitizePositive(final double amount) {
        final double value = sanitize(amount);

        if (value <= 0D) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        return value;
    }
}