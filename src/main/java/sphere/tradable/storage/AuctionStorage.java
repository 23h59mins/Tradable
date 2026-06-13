// File: src/main/java/sphere/tradable/storage/AuctionStorage.java
package sphere.tradable.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import sphere.tradable.model.AuctionEntry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class AuctionStorage extends YamlDataFile {
    private static final String ROOT = "entries";

    public AuctionStorage(final JavaPlugin plugin) {
        super(plugin, "auctions.yml");
    }

    public synchronized List<AuctionEntry> getEntries() {
        final ConfigurationSection root = config().getConfigurationSection(ROOT);
        if (root == null) {
            return Collections.emptyList();
        }

        final List<AuctionEntry> entries = new ArrayList<>();
        for (final String key : root.getKeys(false)) {
            final AuctionEntry entry = readEntry(root.getConfigurationSection(key));
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    public synchronized List<AuctionEntry> getAuctions() {
        return getEntries();
    }

    public synchronized void setEntries(final Collection<AuctionEntry> entries) {
        remove(ROOT);
        section(ROOT);

        for (final AuctionEntry entry : entries) {
            writeEntry(entry);
        }

        save();
    }

    public synchronized void setAuctions(final Collection<AuctionEntry> entries) {
        setEntries(entries);
    }

    public synchronized void addEntry(final AuctionEntry entry) {
        writeEntry(entry);
        save();
    }

    public synchronized void addAuction(final AuctionEntry entry) {
        addEntry(entry);
    }

    public synchronized int removeBySeller(final UUID sellerId) {
        final List<AuctionEntry> kept = new ArrayList<>();
        int removed = 0;

        for (final AuctionEntry entry : getEntries()) {
            final UUID current = readUuid(entry, "getSeller", "getSellerId", "seller", "sellerId");
            if (sellerId.equals(current)) {
                removed++;
            } else {
                kept.add(entry);
            }
        }

        if (removed > 0) {
            setEntries(kept);
        }

        return removed;
    }

    private void writeEntry(final AuctionEntry entry) {
        final String id = UUID.randomUUID().toString();
        final String base = ROOT + "." + id;

        final UUID sellerId = readUuid(entry, "getSeller", "getSellerId", "seller", "sellerId");
        final double price = readPrice(entry);
        final long createdAt = readCreatedAt(entry);
        final ItemStack item = readItem(entry);

        if (sellerId == null || item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("AuctionEntry must expose a seller UUID and a valid item.");
        }

        config().set(base + ".seller-uuid", sellerId.toString());
        config().set(base + ".price", price);
        config().set(base + ".created-at", createdAt <= 0L ? System.currentTimeMillis() : createdAt);
        config().set(base + ".item", item.clone());
    }

    private AuctionEntry readEntry(final ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        final String sellerRaw = section.getString("seller-uuid");
        final double price = sanitize(section.getDouble("price", 0D));
        final long createdAt = section.getLong("created-at", 0L);
        final ItemStack item = section.getItemStack("item");

        if (sellerRaw == null || item == null || item.getType().isAir()) {
            return null;
        }

        try {
            final UUID sellerId = UUID.fromString(sellerRaw);
            return newEntry(sellerId, item.clone(), price, createdAt);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    private AuctionEntry newEntry(final UUID sellerId, final ItemStack item, final double price, final long createdAt) {
        try {
            for (final Constructor<?> constructor : AuctionEntry.class.getConstructors()) {
                final Class<?>[] types = constructor.getParameterTypes();

                if (types.length == 4) {
                    return (AuctionEntry) constructor.newInstance(
                            convert(types[0], sellerId),
                            convert(types[1], item),
                            convert(types[2], price),
                            convert(types[3], createdAt)
                    );
                }

                if (types.length == 3) {
                    return (AuctionEntry) constructor.newInstance(
                            convert(types[0], sellerId),
                            convert(types[1], item),
                            convert(types[2], price)
                    );
                }

                if (types.length == 0) {
                    final AuctionEntry entry = (AuctionEntry) constructor.newInstance();
                    write(entry, sellerId, "setSeller", "setSellerId");
                    write(entry, item, "setItem", "setItemStack");
                    write(entry, price, "setPrice", "setAmount", "setValue");
                    write(entry, createdAt, "setCreatedAt", "setTimestamp", "setTime");
                    return entry;
                }
            }
        } catch (final Throwable ignored) {
        }

        throw new IllegalStateException(
                "Unable to instantiate AuctionEntry. Match the expected constructors or setters."
        );
    }

    private UUID readUuid(final AuctionEntry entry, final String... methods) {
        final Object value = invoke(entry, methods);

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

    private double readPrice(final AuctionEntry entry) {
        final Object value = invoke(entry, new String[]{"getPrice", "price", "getAmount", "getValue"});
        return value instanceof Number number ? sanitize(number.doubleValue()) : 0D;
    }

    private long readCreatedAt(final AuctionEntry entry) {
        final Object value = invoke(entry, new String[]{"getCreatedAt", "getTimestamp", "getTime", "createdAt"});
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private ItemStack readItem(final AuctionEntry entry) {
        final Object value = invoke(entry, new String[]{"getItem", "getItemStack", "item"});
        return value instanceof ItemStack item ? item.clone() : null;
    }

    private void write(final Object target, final Object value, final String... methods) {
        invoke(target, methods, value);
    }

    private Object invoke(final Object target, final String[] methods, final Object... args) {
        for (final String method : methods) {
            final Object result = invoke(target, method, args);
            if (result != InvocationFailure.INSTANCE) {
                return result;
            }
        }

        return InvocationFailure.INSTANCE;
    }

    private Object invoke(final Object target, final String methodName, final Object... args) {
        if (target == null) {
            return InvocationFailure.INSTANCE;
        }

        for (final Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }

            try {
                final Object[] converted = new Object[args.length];
                final Class<?>[] types = method.getParameterTypes();

                for (int i = 0; i < args.length; i++) {
                    converted[i] = convert(types[i], args[i]);
                }

                method.setAccessible(true);
                return method.invoke(target, converted);
            } catch (final Throwable ignored) {
            }
        }

        return InvocationFailure.INSTANCE;
    }

    private Object convert(final Class<?> targetType, final Object value) {
        if (value == null) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class && value instanceof UUID uuid) {
            return uuid.toString();
        }

        if ((targetType == double.class || targetType == Double.class) && value instanceof Number number) {
            return number.doubleValue();
        }

        if ((targetType == long.class || targetType == Long.class) && value instanceof Number number) {
            return number.longValue();
        }

        return value;
    }

    private double sanitize(final double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return 0D;
        }

        return BigDecimal.valueOf(Math.max(0D, amount))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private enum InvocationFailure {
        INSTANCE
    }
}