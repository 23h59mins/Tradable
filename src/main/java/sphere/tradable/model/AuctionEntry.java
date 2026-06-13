package sphere.tradable.model;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SerializableAs("AuctionEntry")
public final class AuctionEntry implements ConfigurationSerializable {
    private UUID sellerId;
    private ItemStack item;
    private double price;
    private long createdAt;

    public AuctionEntry() {
        this.createdAt = System.currentTimeMillis();
    }

    public AuctionEntry(UUID sellerId, ItemStack item, double price) {
        this(sellerId, item, price, System.currentTimeMillis());
    }

    public AuctionEntry(UUID sellerId, ItemStack item, double price, long createdAt) {
        this.sellerId = requireUuid(sellerId, "sellerId");
        this.item = requireItem(item);
        this.price = requirePositiveMoney(price, "price");
        this.createdAt = requireTimestamp(createdAt);
    }

    public AuctionEntry(String sellerId, ItemStack item, double price) {
        this(parseUuid(sellerId, "sellerId"), item, price);
    }

    public AuctionEntry(String sellerId, ItemStack item, double price, long createdAt) {
        this(parseUuid(sellerId, "sellerId"), item, price, createdAt);
    }

    public UUID getSeller() {
        return sellerId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSellerIdString() {
        return sellerId == null ? null : sellerId.toString();
    }

    public UUID getSellerUuid() {
        return sellerId;
    }

    public ItemStack getItem() {
        return item == null ? null : item.clone();
    }

    public ItemStack getItemStack() {
        return getItem();
    }

    public double getPrice() {
        return price;
    }

    public double getAmount() {
        return price;
    }

    public double getValue() {
        return price;
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

    public OfflinePlayer getSellerOfflinePlayer() {
        return sellerId == null ? null : Bukkit.getOfflinePlayer(sellerId);
    }

    public String getSellerName() {
        OfflinePlayer player = getSellerOfflinePlayer();
        return player == null ? null : player.getName();
    }

    public boolean isSeller(UUID playerId) {
        return playerId != null && playerId.equals(sellerId);
    }

    public void setSeller(UUID sellerId) {
        this.sellerId = requireUuid(sellerId, "sellerId");
    }

    public void setSellerId(UUID sellerId) {
        setSeller(sellerId);
    }

    public void setSeller(String sellerId) {
        setSeller(parseUuid(sellerId, "sellerId"));
    }

    public void setSellerId(String sellerId) {
        setSeller(sellerId);
    }

    public void setItem(ItemStack item) {
        this.item = requireItem(item);
    }

    public void setItemStack(ItemStack item) {
        setItem(item);
    }

    public void setPrice(double price) {
        this.price = requirePositiveMoney(price, "price");
    }

    public void setAmount(double amount) {
        setPrice(amount);
    }

    public void setValue(double amount) {
        setPrice(amount);
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = requireTimestamp(createdAt);
    }

    public void setTimestamp(long createdAt) {
        setCreatedAt(createdAt);
    }

    public void setTime(long createdAt) {
        setCreatedAt(createdAt);
    }

    public AuctionEntry copy() {
        return new AuctionEntry(sellerId, getItem(), price, createdAt);
    }

    public AuctionEntry withPrice(double newPrice) {
        return new AuctionEntry(sellerId, getItem(), newPrice, createdAt);
    }

    public static AuctionEntry deserialize(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");

        UUID sellerId = readUuid(map, "sellerId", "seller", "sellerUniqueId", "sellerUuid");
        ItemStack item = readItem(map, "item", "itemStack");
        double price = readDouble(map, "price", "amount", "value", "money");
        long createdAt = readLong(map, "createdAt", "timestamp", "time");

        return new AuctionEntry(sellerId, item, price, createdAt);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sellerId", sellerId.toString());
        map.put("item", getItem());
        map.put("price", price);
        map.put("createdAt", createdAt);
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

    private static ItemStack requireItem(ItemStack value) {
        if (value == null || value.getType().isAir()) {
            throw new IllegalArgumentException("item cannot be null or air");
        }
        return value.clone();
    }

    private static double requirePositiveMoney(double value, String field) {
        double sanitized = sanitizeMoney(value);
        if (sanitized <= 0D) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return sanitized;
    }

    private static long requireTimestamp(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException("createdAt cannot be negative");
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
        throw new IllegalArgumentException("Missing UUID field in serialized AuctionEntry");
    }

    private static ItemStack readItem(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof ItemStack itemStack) {
                return itemStack.clone();
            }
        }
        throw new IllegalArgumentException("Missing item field in serialized AuctionEntry");
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
        throw new IllegalArgumentException("Missing price field in serialized AuctionEntry");
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
        if (!(o instanceof AuctionEntry that)) {
            return false;
        }
        return Double.compare(that.price, price) == 0
                && createdAt == that.createdAt
                && Objects.equals(sellerId, that.sellerId)
                && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sellerId, item, price, createdAt);
    }

    @Override
    public String toString() {
        return "AuctionEntry{" +
                "sellerId=" + sellerId +
                ", item=" + item +
                ", price=" + price +
                ", createdAt=" + createdAt +
                '}';
    }
}