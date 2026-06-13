// src/main/java/sphere/tradable/gui/AuctionMenu.java
package sphere.tradable.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sphere.tradable.Tradable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class AuctionMenu extends BaseMenu {
    private static final String MENU_TITLE = "&6&lAuction House";
    private static final String DIVIDER = "&8&m------------------------";
    private static final String EMPTY_TITLE = "&6&lMarket Quiet";
    private static final List<String> EMPTY_LORE = List.of(
            "&7There are no active listings right now.",
            "&7Check back soon for fresh deals.",
            "",
            "&8Be the first to list something valuable."
    );

    private final List<Entry> cache = new ArrayList<>();

    public AuctionMenu(final Tradable plugin, final Player viewer) {
        super(plugin, viewer.getUniqueId(), MENU_TITLE, GuiSort.NEWEST);
    }

    @Override
    protected void drawContent() {
        cache.clear();
        cache.addAll(entries());

        if (cache.isEmpty()) {
            inventory.setItem(22, GuiUtil.item(Material.CHEST, EMPTY_TITLE, EMPTY_LORE));
            return;
        }

        final int from = page * PAGE_SIZE;
        final int to = Math.min(cache.size(), from + PAGE_SIZE);

        for (int i = from; i < to; i++) {
            final Entry entry = cache.get(i);
            final int slot = CONTENT_SLOTS[i - from];
            final ItemStack display = entry.item().clone();
            final ItemMeta meta = display.getItemMeta();
            final OfflinePlayer seller = entry.sellerUuid() == null ? null : Bukkit.getOfflinePlayer(entry.sellerUuid());

            if (meta != null) {
                meta.setLore(buildLore(meta, seller, entry, entry.sellerUuid() != null && entry.sellerUuid().equals(viewerId)));
                display.setItemMeta(meta);
            }

            inventory.setItem(slot, display);
        }
    }

    @Override
    protected boolean hasNextPage() {
        return (page + 1) * PAGE_SIZE < entries().size();
    }

    @Override
    protected void cycleSort() {
        sort = switch (sort) {
            case NEWEST -> GuiSort.OLDEST;
            case OLDEST -> GuiSort.PRICE_DESC;
            case PRICE_DESC -> GuiSort.PRICE_ASC;
            default -> GuiSort.NEWEST;
        };
    }

    @Override
    protected void handleCustomClick(final Player player, final InventoryClickEvent event) {
        final int local = indexOfContentSlot(event.getRawSlot());
        if (local < 0) {
            return;
        }

        final int index = page * PAGE_SIZE + local;
        if (index < 0 || index >= cache.size()) {
            return;
        }

        final Entry entry = cache.get(index);

        if (event.isLeftClick()) {
            if (entry.sellerUuid() != null && entry.sellerUuid().equals(player.getUniqueId())) {
                GuiUtil.fail(player, "&cYou cannot purchase your own listing.");
                return;
            }

            final boolean ok = GuiAccess.buyAuction(plugin, player, entry.raw());
            if (!ok) {
                GuiUtil.fail(player, "&cThat purchase could not be completed.");
                return;
            }

            GuiUtil.ok(player, "&aPurchase successful! &7You bought this item for &e" + GuiUtil.money(entry.price()) + "&7.");
            redraw();
            return;
        }

        if (event.isRightClick() && entry.sellerUuid() != null && entry.sellerUuid().equals(player.getUniqueId())) {
            final boolean ok = GuiAccess.removeAuction(plugin, player, entry.raw());
            if (!ok) {
                GuiUtil.fail(player, "&cYour listing could not be removed.");
                return;
            }

            GuiUtil.ok(player, "&aListing removed successfully.");
            redraw();
        }
    }

    private List<String> buildLore(
            final ItemMeta meta,
            final OfflinePlayer seller,
            final Entry entry,
            final boolean ownedByViewer
    ) {
        final List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        if (!lore.isEmpty()) {
            lore.add("");
        }

        lore.add(GuiUtil.color(DIVIDER));
        lore.add(GuiUtil.color("&6&lListing Details"));
        lore.add(GuiUtil.color("&7Seller: &f" + sellerName(seller)));
        lore.add(GuiUtil.color("&7Price: &6" + GuiUtil.money(entry.price())));
        lore.add(GuiUtil.color("&7Listed: &f" + listedAt(entry.createdAt())));
        lore.add(GuiUtil.color(DIVIDER));
        lore.add("");

        if (ownedByViewer) {
            lore.add(GuiUtil.color("&eRight-Click to remove your listing"));
            lore.add(GuiUtil.color("&8Manage your auction directly from here."));
        } else {
            lore.add(GuiUtil.color("&aLeft-Click to purchase"));
            lore.add(GuiUtil.color("&8Secure this listing before someone else does."));
        }

        return lore;
    }

    private String sellerName(final OfflinePlayer seller) {
        return seller == null ? "Unknown Trader" : GuiUtil.name(seller);
    }

    private String listedAt(final Instant createdAt) {
        return createdAt == null ? "Recently" : GuiUtil.formatInstant(createdAt);
    }

    private List<Entry> entries() {
        final List<Entry> out = new ArrayList<>();

        for (Object raw : GuiAccess.auctions(plugin)) {
            final UUID sellerUuid = GuiAccess.uuid(raw, "getSellerUuid", "sellerUuid", "getSeller", "seller", "getOwnerUuid", "ownerUuid");
            final double price = GuiAccess.number(raw, "getPrice", "price", "getAmount", "amount", "getMoney", "money");
            final Instant createdAt = GuiAccess.instant(raw, "getCreatedAt", "createdAt", "getListedAt", "listedAt", "getTimestamp", "timestamp");
            final ItemStack item = GuiAccess.item(raw, "getItem", "item", "getItemStack", "itemStack", "getDisplayedItem", "displayedItem");

            if (item == null || item.getType().isAir()) {
                continue;
            }

            out.add(new Entry(raw, sellerUuid, item, price, createdAt));
        }

        out.sort(comparator());
        return out;
    }

    private Comparator<Entry> comparator() {
        return switch (sort) {
            case OLDEST -> Comparator.comparing(Entry::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case PRICE_DESC -> Comparator.comparingDouble(Entry::price).reversed();
            case PRICE_ASC -> Comparator.comparingDouble(Entry::price);
            default -> Comparator.comparing(Entry::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private record Entry(Object raw, UUID sellerUuid, ItemStack item, double price, Instant createdAt) {
    }
}