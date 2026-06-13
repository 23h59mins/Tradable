package sphere.tradable.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import sphere.tradable.Tradable;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class GuiAccess {
    private GuiAccess() {
    }

    public static Collection<?> auctions(final Tradable plugin) {
        if (plugin == null || plugin.getAuctionService() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(plugin.getAuctionService().getAuctions());
    }

    public static Collection<?> bounties(final Tradable plugin) {
        if (plugin == null || plugin.getBountyService() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(plugin.getBountyService().getBounties());
    }

    public static Map<UUID, Double> balances(final Tradable plugin) {
        if (plugin == null || plugin.getEconomyService() == null) {
            return new LinkedHashMap<>();
        }

        return new LinkedHashMap<>(plugin.getEconomyService().getTopBalances(10_000));
    }

    public static double balanceOf(final Tradable plugin, final UUID playerId) {
        if (plugin == null || plugin.getEconomyService() == null || playerId == null) {
            return 0D;
        }

        return plugin.getEconomyService().getBalance(playerId);
    }

    public static boolean withdraw(final Tradable plugin, final UUID playerId, final double amount) {
        if (plugin == null || plugin.getEconomyService() == null || playerId == null || amount <= 0D) {
            return false;
        }

        return plugin.getEconomyService().withdraw(playerId, amount);
    }

    public static boolean deposit(final Tradable plugin, final UUID playerId, final double amount) {
        if (plugin == null || plugin.getEconomyService() == null || playerId == null || amount <= 0D) {
            return false;
        }

        return plugin.getEconomyService().deposit(playerId, amount);
    }

    public static boolean buyAuction(final Tradable plugin, final Player buyer, final Object raw) {
        if (plugin == null || buyer == null || !(raw instanceof sphere.tradable.model.AuctionEntry entry)) {
            return false;
        }

        final boolean ok = plugin.getAuctionService().purchaseAuction(entry, buyer, plugin.getEconomyService());
        if (!ok) {
            return false;
        }

        final ItemStack item = plugin.getAuctionService().getItem(entry);
        if (item != null && !item.getType().isAir()) {
            buyer.getInventory().addItem(item.clone());
        }

        return true;
    }

    public static boolean removeAuction(final Tradable plugin, final Player player, final Object raw) {
        if (plugin == null || player == null || !(raw instanceof sphere.tradable.model.AuctionEntry entry)) {
            return false;
        }

        if (entry.getSellerId() == null || !entry.getSellerId().equals(player.getUniqueId())) {
            return false;
        }

        final boolean removed = plugin.getAuctionService().removeAuction(entry);
        if (!removed) {
            return false;
        }

        final ItemStack item = entry.getItemStack();
        if (item != null && !item.getType().isAir()) {
            player.getInventory().addItem(item.clone());
        }

        return true;
    }

    public static UUID uuid(final Object target, final String... methods) {
        final Object value = invoke(target, methods);

        if (value instanceof UUID uuid) {
            return uuid;
        }

        if (value instanceof String string) {
            try {
                return UUID.fromString(string);
            } catch (final IllegalArgumentException ignored) {
                return null;
            }
        }

        return null;
    }

    public static double number(final Object target, final String... methods) {
        final Object value = invoke(target, methods);
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    public static Instant instant(final Object target, final String... methods) {
        final Object value = invoke(target, methods);

        if (value instanceof Instant instant) {
            return instant;
        }

        if (value instanceof Number number) {
            final long millis = number.longValue();
            return millis <= 0L ? null : Instant.ofEpochMilli(millis);
        }

        return null;
    }

    public static ItemStack item(final Object target, final String... methods) {
        final Object value = invoke(target, methods);
        return value instanceof ItemStack item ? item.clone() : null;
    }

    private static Object invoke(final Object target, final String... methods) {
        if (target == null || methods == null || methods.length == 0) {
            return null;
        }

        for (final String methodName : methods) {
            if (methodName == null || methodName.isBlank()) {
                continue;
            }

            for (final Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    final Object value = method.invoke(target);
                    if (value != null) {
                        return value;
                    }
                } catch (final Throwable ignored) {
                }
            }
        }

        return null;
    }
}