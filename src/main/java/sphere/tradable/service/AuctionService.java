package sphere.tradable.service;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import sphere.tradable.Tradable;
import sphere.tradable.model.AuctionEntry;
import sphere.tradable.model.SortMode;
import sphere.tradable.storage.AuctionStorage;
import sphere.tradable.util.MoneyUtil;
import sphere.tradable.util.PlayerRefResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AuctionService {
    private static final String MAX_LISTINGS_PATH = "auction-house.max-listings-per-player";

    private final Tradable plugin;
    private final AuctionStorage auctionStorage;

    public AuctionService(final Tradable plugin, final AuctionStorage auctionStorage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.auctionStorage = Objects.requireNonNull(auctionStorage, "auctionStorage");
    }

    public AuctionEntry createAuction(final Player seller, final ItemStack item, final double price) {
        return createAuction((Object) seller, item, price);
    }

    public AuctionEntry createAuction(final OfflinePlayer seller, final ItemStack item, final double price) {
        return createAuction((Object) seller, item, price);
    }

    public AuctionEntry createAuction(final UUID sellerId, final ItemStack item, final double price) {
        return createAuction((Object) sellerId, item, price);
    }

    public AuctionEntry createAuction(final String sellerName, final ItemStack item, final double price) {
        return createAuction((Object) sellerName, item, price);
    }

    public AuctionEntry createAuction(final Object sellerRef, final ItemStack item, final double price) {
        final UUID sellerId = PlayerRefResolver.requireUuid(sellerRef);
        final ItemStack itemCopy = requireItem(item);
        final double value = MoneyUtil.requirePositive(price, "Price");

        enforceSellerListingLimit(sellerId);

        final AuctionEntry entry = new AuctionEntry(sellerId, itemCopy, value, System.currentTimeMillis());
        auctionStorage.addEntry(entry);
        return entry;
    }

    public AuctionEntry listAuction(final Player seller, final ItemStack item, final double price) {
        return createAuction(seller, item, price);
    }

    public AuctionEntry listAuction(final UUID sellerId, final ItemStack item, final double price) {
        return createAuction(sellerId, item, price);
    }

    public AuctionEntry addAuction(final Player seller, final ItemStack item, final double price) {
        return createAuction(seller, item, price);
    }

    public AuctionEntry addAuction(final UUID sellerId, final ItemStack item, final double price) {
        return createAuction(sellerId, item, price);
    }

    public AuctionEntry listItem(final Player seller, final ItemStack item, final double price) {
        return createAuction(seller, item, price);
    }

    public AuctionEntry sellItem(final Player seller, final ItemStack item, final double price) {
        return createAuction(seller, item, price);
    }

    public List<AuctionEntry> getAuctions() {
        return getAuctions(SortMode.HIGHEST);
    }

    public List<AuctionEntry> getAuctions(final SortMode sortMode) {
        final SortMode effectiveSort = sortMode == null ? SortMode.HIGHEST : sortMode;
        final List<AuctionEntry> entries = new ArrayList<>(auctionStorage.getEntries());
        entries.sort(effectiveSort.auctionComparator());
        return entries;
    }

    public List<AuctionEntry> getEntries() {
        return getAuctions();
    }

    public List<AuctionEntry> getEntries(final SortMode sortMode) {
        return getAuctions(sortMode);
    }

    public List<AuctionEntry> getAuctions(final String rawSortMode) {
        return getAuctions(SortMode.fromString(rawSortMode));
    }

    public List<AuctionEntry> getAuctionsBySeller(final OfflinePlayer seller, final SortMode sortMode) {
        Objects.requireNonNull(seller, "seller");
        return getAuctionsBySeller(seller.getUniqueId(), sortMode);
    }

    public List<AuctionEntry> getAuctionsBySeller(final UUID sellerId, final SortMode sortMode) {
        Objects.requireNonNull(sellerId, "sellerId");

        final SortMode effectiveSort = sortMode == null ? SortMode.HIGHEST : sortMode;
        final List<AuctionEntry> result = new ArrayList<>();

        for (final AuctionEntry entry : auctionStorage.getEntries()) {
            if (sellerId.equals(entry.getSellerId())) {
                result.add(entry);
            }
        }

        result.sort(effectiveSort.auctionComparator());
        return result;
    }

    public boolean removeAuction(final AuctionEntry target) {
        if (target == null) {
            return false;
        }

        final List<AuctionEntry> entries = new ArrayList<>(auctionStorage.getEntries());
        final boolean removed = entries.remove(target);

        if (!removed) {
            return false;
        }

        auctionStorage.setEntries(entries);
        return true;
    }

    public boolean purchaseAuction(final AuctionEntry entry, final Player buyer, final EconomyService economyService) {
        return purchaseAuction(entry, (Object) buyer, economyService);
    }

    public boolean purchaseAuction(final AuctionEntry entry, final OfflinePlayer buyer, final EconomyService economyService) {
        return purchaseAuction(entry, (Object) buyer, economyService);
    }

    public boolean purchaseAuction(final AuctionEntry entry, final UUID buyerId, final EconomyService economyService) {
        return purchaseAuction(entry, (Object) buyerId, economyService);
    }

    public boolean purchaseAuction(final AuctionEntry entry, final Object buyerRef, final EconomyService economyService) {
        if (entry == null || economyService == null) {
            return false;
        }

        final UUID buyerId = PlayerRefResolver.requireUuid(buyerRef);
        final UUID sellerId = entry.getSellerId();
        final double price = MoneyUtil.sanitize(entry.getPrice());

        if (sellerId == null || sellerId.equals(buyerId) || price <= 0D) {
            return false;
        }

        if (!economyService.transfer(buyerId, sellerId, price)) {
            return false;
        }

        return removeAuction(entry);
    }

    public ItemStack getItem(final AuctionEntry entry) {
        return entry == null ? null : entry.getItemStack();
    }

    private void enforceSellerListingLimit(final UUID sellerId) {
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller could not be resolved.");
        }

        final int maxListings = Math.max(1, plugin.getConfig().getInt(MAX_LISTINGS_PATH, 5));
        final int currentListings = getAuctionsBySeller(sellerId, null).size();

        if (currentListings >= maxListings) {
            throw new IllegalArgumentException(
                    "You have reached your auction listing limit (" +
                            currentListings + "/" + maxListings + ")."
            );
        }
    }

    private ItemStack requireItem(final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("You must provide a valid item to list.");
        }

        return item.clone();
    }
}