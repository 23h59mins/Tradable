// File: src/main/java/sphere/tradable/service/EconomyService.java
package sphere.tradable.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sphere.tradable.storage.BalanceStorage;
import sphere.tradable.util.MoneyUtil;
import sphere.tradable.util.PlayerRefResolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class EconomyService {
    private final BalanceStorage balanceStorage;

    public EconomyService(final BalanceStorage balanceStorage) {
        this.balanceStorage = Objects.requireNonNull(balanceStorage, "balanceStorage");
    }

    public double getBalance(final Player player) {
        return getBalance((Object) player);
    }

    public double getBalance(final OfflinePlayer player) {
        return getBalance((Object) player);
    }

    public double getBalance(final UUID playerId) {
        return getBalance((Object) playerId);
    }

    public double getBalance(final String playerName) {
        return getBalance((Object) playerName);
    }

    public double getBalance(final Object playerRef) {
        return balanceStorage.getBalance(PlayerRefResolver.requireUuid(playerRef));
    }

    public double balance(final Player player) {
        return getBalance(player);
    }

    public double balance(final OfflinePlayer player) {
        return getBalance(player);
    }

    public double balance(final UUID playerId) {
        return getBalance(playerId);
    }

    public double balance(final String playerName) {
        return getBalance(playerName);
    }

    public double getMoney(final Player player) {
        return getBalance(player);
    }

    public double getMoney(final OfflinePlayer player) {
        return getBalance(player);
    }

    public double getMoney(final UUID playerId) {
        return getBalance(playerId);
    }

    public double getMoney(final String playerName) {
        return getBalance(playerName);
    }

    public boolean setBalance(final Player player, final double amount) {
        return setBalance((Object) player, amount);
    }

    public boolean setBalance(final OfflinePlayer player, final double amount) {
        return setBalance((Object) player, amount);
    }

    public boolean setBalance(final UUID playerId, final double amount) {
        return setBalance((Object) playerId, amount);
    }

    public boolean setBalance(final String playerName, final double amount) {
        return setBalance((Object) playerName, amount);
    }

    public boolean setBalance(final Object playerRef, final double amount) {
        final UUID playerId = PlayerRefResolver.requireUuid(playerRef);
        balanceStorage.setBalance(
                playerId,
                PlayerRefResolver.resolveName(playerRef),
                MoneyUtil.sanitize(amount)
        );
        return true;
    }

    public boolean addMoney(final Player player, final double amount) {
        return addMoney((Object) player, amount);
    }

    public boolean addMoney(final OfflinePlayer player, final double amount) {
        return addMoney((Object) player, amount);
    }

    public boolean addMoney(final UUID playerId, final double amount) {
        return addMoney((Object) playerId, amount);
    }

    public boolean addMoney(final String playerName, final double amount) {
        return addMoney((Object) playerName, amount);
    }

    public boolean addMoney(final Object playerRef, final double amount) {
        final UUID playerId = PlayerRefResolver.requireUuid(playerRef);
        balanceStorage.addBalance(
                playerId,
                PlayerRefResolver.resolveName(playerRef),
                MoneyUtil.requirePositive(amount, "Amount")
        );
        return true;
    }

    public boolean deposit(final Player player, final double amount) {
        return addMoney(player, amount);
    }

    public boolean deposit(final OfflinePlayer player, final double amount) {
        return addMoney(player, amount);
    }

    public boolean deposit(final UUID playerId, final double amount) {
        return addMoney(playerId, amount);
    }

    public boolean deposit(final String playerName, final double amount) {
        return addMoney(playerName, amount);
    }

    public boolean removeMoney(final Player player, final double amount) {
        return removeMoney((Object) player, amount);
    }

    public boolean removeMoney(final OfflinePlayer player, final double amount) {
        return removeMoney((Object) player, amount);
    }

    public boolean removeMoney(final UUID playerId, final double amount) {
        return removeMoney((Object) playerId, amount);
    }

    public boolean removeMoney(final String playerName, final double amount) {
        return removeMoney((Object) playerName, amount);
    }

    public boolean removeMoney(final Object playerRef, final double amount) {
        final UUID playerId = PlayerRefResolver.requireUuid(playerRef);
        return balanceStorage.subtractBalance(
                playerId,
                PlayerRefResolver.resolveName(playerRef),
                MoneyUtil.requirePositive(amount, "Amount")
        );
    }

    public boolean withdraw(final Player player, final double amount) {
        return removeMoney(player, amount);
    }

    public boolean withdraw(final OfflinePlayer player, final double amount) {
        return removeMoney(player, amount);
    }

    public boolean withdraw(final UUID playerId, final double amount) {
        return removeMoney(playerId, amount);
    }

    public boolean withdraw(final String playerName, final double amount) {
        return removeMoney(playerName, amount);
    }

    public boolean transfer(final Player from, final Player to, final double amount) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return transfer(from.getUniqueId(), to.getUniqueId(), amount);
    }

    public boolean transfer(final OfflinePlayer from, final OfflinePlayer to, final double amount) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return transfer(from.getUniqueId(), to.getUniqueId(), amount);
    }

    public boolean transfer(final UUID from, final UUID to, final double amount) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        final double value = MoneyUtil.requirePositive(amount, "Amount");

        if (from.equals(to)) {
            return false;
        }

        final OfflinePlayer fromPlayer = Bukkit.getOfflinePlayer(from);
        final OfflinePlayer toPlayer = Bukkit.getOfflinePlayer(to);

        return balanceStorage.transfer(
                from,
                fromPlayer.getName(),
                to,
                toPlayer.getName(),
                value
        );
    }

    public Map<UUID, Double> getTopBalances(final int limit) {
        return new LinkedHashMap<>(balanceStorage.getTopBalances(limit));
    }
}