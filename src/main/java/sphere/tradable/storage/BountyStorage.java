// File: src/main/java/sphere/tradable/storage/BountyStorage.java
package sphere.tradable.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import sphere.tradable.model.BountyEntry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BountyStorage extends YamlDataFile {
    private static final String ROOT = "entries";

    public BountyStorage(final JavaPlugin plugin) {
        super(plugin, "bounties.yml");
    }

    public synchronized List<BountyEntry> getEntries() {
        final ConfigurationSection root = config().getConfigurationSection(ROOT);
        if (root == null) {
            return Collections.emptyList();
        }

        final List<BountyEntry> entries = new ArrayList<>();
        for (final String key : root.getKeys(false)) {
            final BountyEntry entry = readEntry(root.getConfigurationSection(key));
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    public synchronized List<BountyEntry> getBounties() {
        return getEntries();
    }

    public synchronized void setEntries(final Collection<BountyEntry> entries) {
        remove(ROOT);
        section(ROOT);

        for (final BountyEntry entry : entries) {
            writeEntry(entry);
        }

        save();
    }

    public synchronized void setBounties(final Collection<BountyEntry> entries) {
        setEntries(entries);
    }

    public synchronized void addEntry(final BountyEntry entry) {
        writeEntry(entry);
        save();
    }

    public synchronized void addBounty(final BountyEntry entry) {
        addEntry(entry);
    }

    public synchronized int removeByTarget(final UUID targetId) {
        final List<BountyEntry> kept = new ArrayList<>();
        int removed = 0;

        for (final BountyEntry entry : getEntries()) {
            final UUID current = readUuid(entry, "getTarget", "getTargetId", "target", "targetId");
            if (targetId.equals(current)) {
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

    public synchronized double getTotalBounty(final UUID targetId) {
        double total = 0D;

        for (final BountyEntry entry : getEntries()) {
            final UUID current = readUuid(entry, "getTarget", "getTargetId", "target", "targetId");
            if (targetId.equals(current)) {
                total += readAmount(entry);
            }
        }

        return sanitize(total);
    }

    public synchronized boolean hasBounty(final UUID targetId) {
        return getTotalBounty(targetId) > 0D;
    }

    private void writeEntry(final BountyEntry entry) {
        final String id = UUID.randomUUID().toString();
        final String base = ROOT + "." + id;

        final UUID setterId = readUuid(entry, "getSetter", "getSetterId", "setter", "setterId");
        final UUID targetId = readUuid(entry, "getTarget", "getTargetId", "target", "targetId");
        final double amount = readAmount(entry);
        final long createdAt = readCreatedAt(entry);
        final long updatedAt = readUpdatedAt(entry);

        if (setterId == null || targetId == null) {
            throw new IllegalArgumentException("BountyEntry must expose setter and target UUIDs.");
        }

        config().set(base + ".setter-uuid", setterId.toString());
        config().set(base + ".target-uuid", targetId.toString());
        config().set(base + ".amount", amount);
        config().set(base + ".created-at", createdAt <= 0L ? System.currentTimeMillis() : createdAt);
        config().set(base + ".updated-at", updatedAt <= 0L ? System.currentTimeMillis() : updatedAt);
    }

    private BountyEntry readEntry(final ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        final String setterRaw = section.getString("setter-uuid");
        final String targetRaw = section.getString("target-uuid");
        final double amount = sanitize(section.getDouble("amount", 0D));
        final long createdAt = section.getLong("created-at", 0L);
        final long updatedAt = section.getLong("updated-at", createdAt);

        if (setterRaw == null || targetRaw == null) {
            return null;
        }

        try {
            final UUID setterId = UUID.fromString(setterRaw);
            final UUID targetId = UUID.fromString(targetRaw);
            return newEntry(setterId, targetId, amount, createdAt, updatedAt);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    private BountyEntry newEntry(
            final UUID setterId,
            final UUID targetId,
            final double amount,
            final long createdAt,
            final long updatedAt
    ) {
        try {
            for (final Constructor<?> constructor : BountyEntry.class.getConstructors()) {
                final Class<?>[] types = constructor.getParameterTypes();

                if (types.length == 5) {
                    return (BountyEntry) constructor.newInstance(
                            convert(types[0], setterId),
                            convert(types[1], targetId),
                            convert(types[2], amount),
                            convert(types[3], createdAt),
                            convert(types[4], updatedAt)
                    );
                }

                if (types.length == 4) {
                    return (BountyEntry) constructor.newInstance(
                            convert(types[0], setterId),
                            convert(types[1], targetId),
                            convert(types[2], amount),
                            convert(types[3], createdAt)
                    );
                }

                if (types.length == 3) {
                    return (BountyEntry) constructor.newInstance(
                            convert(types[0], setterId),
                            convert(types[1], targetId),
                            convert(types[2], amount)
                    );
                }

                if (types.length == 0) {
                    final BountyEntry entry = (BountyEntry) constructor.newInstance();
                    write(entry, setterId, "setSetter", "setSetterId");
                    write(entry, targetId, "setTarget", "setTargetId");
                    write(entry, amount, "setAmount", "setPrice", "setValue");
                    write(entry, createdAt, "setCreatedAt", "setTimestamp", "setTime");
                    write(entry, updatedAt, "setUpdatedAt");
                    return entry;
                }
            }
        } catch (final Throwable ignored) {
        }

        throw new IllegalStateException(
                "Unable to instantiate BountyEntry. Match the expected constructors or setters."
        );
    }

    private UUID readUuid(final BountyEntry entry, final String... methods) {
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

    private double readAmount(final BountyEntry entry) {
        final Object value = invoke(entry, new String[]{"getAmount", "amount", "getPrice", "getValue"});
        return value instanceof Number number ? sanitize(number.doubleValue()) : 0D;
    }

    private long readCreatedAt(final BountyEntry entry) {
        final Object value = invoke(entry, new String[]{"getCreatedAt", "getTimestamp", "getTime", "createdAt"});
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private long readUpdatedAt(final BountyEntry entry) {
        final Object value = invoke(entry, new String[]{"getUpdatedAt", "updatedAt", "getLastUpdated", "lastUpdated"});
        return value instanceof Number number ? number.longValue() : 0L;
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