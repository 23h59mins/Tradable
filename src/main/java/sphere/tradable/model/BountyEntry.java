package sphere.tradable.model;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SerializableAs("BountyEntry")
public final class BountyEntry implements ConfigurationSerializable {
    private UUID setterId;
    private UUID targetId;
    private double amount;
    private long createdAt;
    private long updatedAt;

    public BountyEntry() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public BountyEntry(UUID setterId, UUID targetId, double amount) {
        this(setterId, targetId, amount, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public BountyEntry(UUID setterId, UUID targetId, double amount, long createdAt) {
        this(setterId, targetId, amount, createdAt, createdAt);
    }

    public BountyEntry(UUID setterId, UUID targetId, double amount, long createdAt, long updatedAt) {
        this.setterId = requireUuid(setterId, "setterId");
        this.targetId = requireUuid(targetId, "targetId");
        if (setterId.equals(targetId)) {
            throw new IllegalArgumentException("setterId cannot equal targetId");
        }
        this.amount = requirePositiveMoney(amount, "amount");
        this.createdAt = requireTimestamp(createdAt, "createdAt");
        this.updatedAt = requireTimestamp(updatedAt, "updatedAt");
    }

    public BountyEntry(String setterId, String targetId, double amount) {
        this(parseUuid(setterId, "setterId"), parseUuid(targetId, "targetId"), amount);
    }

    public BountyEntry(String setterId, String targetId, double amount, long createdAt) {
        this(parseUuid(setterId, "setterId"), parseUuid(targetId, "targetId"), amount, createdAt);
    }

    public BountyEntry(String setterId, String targetId, double amount, long createdAt, long updatedAt) {
        this(parseUuid(setterId, "setterId"), parseUuid(targetId, "targetId"), amount, createdAt, updatedAt);
    }

    public UUID getSetter() {
        return setterId;
    }

    public UUID getSetterId() {
        return setterId;
    }

    public UUID getSetterUuid() {
        return setterId;
    }

    public String getSetterIdString() {
        return setterId == null ? null : setterId.toString();
    }

    public UUID getTarget() {
        return targetId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public UUID getTargetUuid() {
        return targetId;
    }

    public String getTargetIdString() {
        return targetId == null ? null : targetId.toString();
    }

    public double getAmount() {
        return amount;
    }

    public double getPrice() {
        return amount;
    }

    public double getValue() {
        return amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getTimestamp() {
        return createdAt;
    }

    public long getTime() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public OfflinePlayer getSetterOfflinePlayer() {
        return setterId == null ? null : Bukkit.getOfflinePlayer(setterId);
    }

    public OfflinePlayer getTargetOfflinePlayer() {
        return targetId == null ? null : Bukkit.getOfflinePlayer(targetId);
    }

    public String getSetterName() {
        OfflinePlayer player = getSetterOfflinePlayer();
        return player == null ? null : player.getName();
    }

    public String getTargetName() {
        OfflinePlayer player = getTargetOfflinePlayer();
        return player == null ? null : player.getName();
    }

    public boolean isTarget(UUID playerId) {
        return playerId != null && playerId.equals(targetId);
    }

    public void setSetter(UUID setterId) {
        this.setterId = requireUuid(setterId, "setterId");
    }

    public void setSetterId(UUID setterId) {
        setSetter(setterId);
    }

    public void setSetter(String setterId) {
        setSetter(parseUuid(setterId, "setterId"));
    }

    public void setTarget(UUID targetId) {
        this.targetId = requireUuid(targetId, "targetId");
    }

    public void setTargetId(UUID targetId) {
        setTarget(targetId);
    }

    public void setTarget(String targetId) {
        setTarget(parseUuid(targetId, "targetId"));
    }

    public void setAmount(double amount) {
        this.amount = requirePositiveMoney(amount, "amount");
        this.updatedAt = System.currentTimeMillis();
    }

    public void setPrice(double amount) {
        setAmount(amount);
    }

    public void setValue(double amount) {
        setAmount(amount);
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = requireTimestamp(createdAt, "createdAt");
    }

    public void setTimestamp(long createdAt) {
        setCreatedAt(createdAt);
    }

    public void setTime(long createdAt) {
        setCreatedAt(createdAt);
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = requireTimestamp(updatedAt, "updatedAt");
    }

    public BountyEntry copy() {
        return new BountyEntry(setterId, targetId, amount, createdAt, updatedAt);
    }

    public BountyEntry withAmount(double newAmount) {
        return new BountyEntry(setterId, targetId, newAmount, createdAt, System.currentTimeMillis());
    }

    public static BountyEntry deserialize(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");

        UUID setterId = readUuid(map, "setterId", "setter", "setterUuid");
        UUID targetId = readUuid(map, "targetId", "target", "targetUuid");
        double amount = readDouble(map, "amount", "price", "value", "money");
        long createdAt = readLong(map, "createdAt", "timestamp", "time");
        long updatedAt = readLong(map, "updatedAt", "lastUpdated");

        return new BountyEntry(setterId, targetId, amount, createdAt, updatedAt <= 0L ? createdAt : updatedAt);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("setterId", setterId.toString());
        map.put("targetId", targetId.toString());
        map.put("amount", amount);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    private static UUID requireUuid(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " cannot be null");
        }
        return value;
    }

    private static UUID parseUuid(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " must be a valid UUID", exception);
        }
    }

    private static double requirePositiveMoney(double value, String field) {
        double sanitized = sanitizeMoney(value);
        if (sanitized <= 0D) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return sanitized;
    }

    private static long requireTimestamp(long value, String field) {
        if (value < 0L) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }

    private static double sanitizeMoney(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("money value must be finite");
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static UUID readUuid(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof UUID uuid) {
                return uuid;
            }
            if (value instanceof String string && !string.isBlank()) {
                return parseUuid(string, key);
            }
        }
        throw new IllegalArgumentException("Missing UUID field in serialized BountyEntry");
    }

    private static double readDouble(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String string && !string.isBlank()) {
                return Double.parseDouble(string);
            }
        }
        throw new IllegalArgumentException("Missing amount field in serialized BountyEntry");
    }

    private static long readLong(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String string && !string.isBlank()) {
                return Long.parseLong(string);
            }
        }
        return System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BountyEntry that)) {
            return false;
        }
        return Double.compare(that.amount, amount) == 0
                && createdAt == that.createdAt
                && updatedAt == that.updatedAt
                && Objects.equals(setterId, that.setterId)
                && Objects.equals(targetId, that.targetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(setterId, targetId, amount, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "BountyEntry{" +
                "setterId=" + setterId +
                ", targetId=" + targetId +
                ", amount=" + amount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}