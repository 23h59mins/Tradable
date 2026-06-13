// File: src/main/java/sphere/tradable/model/SortMode.java
package sphere.tradable.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public enum SortMode {
    HIGHEST,
    LOWEST,
    NEWEST,
    OLDEST;

    public SortMode next() {
        final SortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public boolean isPriceMode() {
        return this == HIGHEST || this == LOWEST;
    }

    public boolean isTimeMode() {
        return this == NEWEST || this == OLDEST;
    }

    public String getDisplayName() {
        return switch (this) {
            case HIGHEST -> "Highest Value";
            case LOWEST -> "Lowest Value";
            case NEWEST -> "Newest Listings";
            case OLDEST -> "Oldest Listings";
        };
    }

    public String getMenuLabel() {
        return switch (this) {
            case HIGHEST -> "Price: High to Low";
            case LOWEST -> "Price: Low to High";
            case NEWEST -> "Date: Newest First";
            case OLDEST -> "Date: Oldest First";
        };
    }

    public String getShortLabel() {
        return switch (this) {
            case HIGHEST -> "High → Low";
            case LOWEST -> "Low → High";
            case NEWEST -> "Newest First";
            case OLDEST -> "Oldest First";
        };
    }

    public Comparator<BountyEntry> bountyComparator() {
        return switch (this) {
            case HIGHEST -> Comparator
                    .comparingDouble(BountyEntry::getAmount).reversed()
                    .thenComparing(Comparator.comparingLong(BountyEntry::getCreatedAt).reversed())
                    .thenComparing(BountyEntry::getTargetIdString, Comparator.nullsLast(String::compareToIgnoreCase));

            case LOWEST -> Comparator
                    .comparingDouble(BountyEntry::getAmount)
                    .thenComparingLong(BountyEntry::getCreatedAt)
                    .thenComparing(BountyEntry::getTargetIdString, Comparator.nullsLast(String::compareToIgnoreCase));

            case NEWEST -> Comparator
                    .comparingLong(BountyEntry::getCreatedAt).reversed()
                    .thenComparing(Comparator.comparingDouble(BountyEntry::getAmount).reversed())
                    .thenComparing(BountyEntry::getTargetIdString, Comparator.nullsLast(String::compareToIgnoreCase));

            case OLDEST -> Comparator
                    .comparingLong(BountyEntry::getCreatedAt)
                    .thenComparing(Comparator.comparingDouble(BountyEntry::getAmount).reversed())
                    .thenComparing(BountyEntry::getTargetIdString, Comparator.nullsLast(String::compareToIgnoreCase));
        };
    }

    public Comparator<AuctionEntry> auctionComparator() {
        return switch (this) {
            case HIGHEST -> Comparator
                    .comparingDouble(AuctionEntry::getPrice).reversed()
                    .thenComparing(Comparator.comparingLong(AuctionEntry::getCreatedAt).reversed())
                    .thenComparing(AuctionEntry::getSellerIdString, Comparator.nullsLast(String::compareToIgnoreCase));

            case LOWEST -> Comparator
                    .comparingDouble(AuctionEntry::getPrice)
                    .thenComparingLong(AuctionEntry::getCreatedAt)
                    .thenComparing(AuctionEntry::getSellerIdString, Comparator.nullsLast(String::compareToIgnoreCase));

            case NEWEST -> Comparator
                    .comparingLong(AuctionEntry::getCreatedAt).reversed()
                    .thenComparing(Comparator.comparingDouble(AuctionEntry::getPrice).reversed())
                    .thenComparing(AuctionEntry::getSellerIdString, Comparator.nullsLast(String::compareToIgnoreCase));

            case OLDEST -> Comparator
                    .comparingLong(AuctionEntry::getCreatedAt)
                    .thenComparing(Comparator.comparingDouble(AuctionEntry::getPrice).reversed())
                    .thenComparing(AuctionEntry::getSellerIdString, Comparator.nullsLast(String::compareToIgnoreCase));
        };
    }

    public static SortMode fromString(final String raw) {
        if (raw == null || raw.isBlank()) {
            return HIGHEST;
        }

        final String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return Arrays.stream(values())
                .filter(mode -> mode.name().equals(normalized))
                .findFirst()
                .orElseGet(() -> switch (normalized) {
                    case "HIGH", "HIGHEST_PRICE", "PRICE_DESC", "AMOUNT_DESC", "DESC" -> HIGHEST;
                    case "LOW", "LOWEST_PRICE", "PRICE_ASC", "AMOUNT_ASC", "ASC" -> LOWEST;
                    case "LATEST" -> NEWEST;
                    case "EARLIEST", "FIRST" -> OLDEST;
                    default -> HIGHEST;
                });
    }
}