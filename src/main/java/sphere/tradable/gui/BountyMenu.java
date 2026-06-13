// src/main/java/sphere/tradable/gui/BountyMenu.java
package sphere.tradable.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sphere.tradable.Tradable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class BountyMenu extends BaseMenu {
    private static final String MENU_TITLE = "&4&lBounty Board";
    private static final String DIVIDER = "&8&m------------------------";
    private static final String EMPTY_TITLE = "&6&lNo Active Contracts";
    private static final List<String> EMPTY_LORE = List.of(
            "&7The board is currently quiet.",
            "&7No hunters have posted any targets yet.",
            "",
            "&8Check back later for fresh contracts."
    );

    public BountyMenu(final Tradable plugin, final Player viewer) {
        super(plugin, viewer.getUniqueId(), MENU_TITLE, GuiSort.BOUNTY_DESC);
    }

    @Override
    protected void drawContent() {
        final List<Entry> entries = entries();

        if (entries.isEmpty()) {
            inventory.setItem(22, GuiUtil.item(Material.WRITABLE_BOOK, EMPTY_TITLE, EMPTY_LORE));
            return;
        }

        final int from = page * PAGE_SIZE;
        final int to = Math.min(entries.size(), from + PAGE_SIZE);

        for (int i = from; i < to; i++) {
            final Entry entry = entries.get(i);
            final OfflinePlayer target = Bukkit.getOfflinePlayer(entry.targetUuid());
            final OfflinePlayer setter = entry.setterUuid() == null ? null : Bukkit.getOfflinePlayer(entry.setterUuid());
            final int slot = CONTENT_SLOTS[i - from];

            inventory.setItem(slot, GuiUtil.head(
                    target,
                    buildTitle(i + 1, target, entry.amount()),
                    buildLore(target, setter, entry)
            ));
        }
    }

    @Override
    protected boolean hasNextPage() {
        return (page + 1) * PAGE_SIZE < entries().size();
    }

    @Override
    protected void cycleSort() {
        sort = switch (sort) {
            case BOUNTY_DESC -> GuiSort.BOUNTY_ASC;
            case BOUNTY_ASC -> GuiSort.NEWEST;
            case NEWEST -> GuiSort.OLDEST;
            default -> GuiSort.BOUNTY_DESC;
        };
    }

    private String buildTitle(final int rank, final OfflinePlayer target, final double amount) {
        return "&c&l#" + rank + " &f" + GuiUtil.name(target) + " &8• &6" + GuiUtil.money(amount);
    }

    private List<String> buildLore(final OfflinePlayer target, final OfflinePlayer setter, final Entry entry) {
        final List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("&4&lContract Details");
        lore.add("&7Target: &f" + GuiUtil.name(target));
        lore.add("&7Reward: &6" + GuiUtil.money(entry.amount()));
        lore.add("&7Placed by: &f" + setterName(setter));
        lore.add("&7Issued: &f" + formatTime(entry.createdAt(), "Recently"));
        lore.add("&7Last Updated: &f" + formatTime(entry.updatedAt(), "No updates"));
        lore.add("");
        lore.add("&eUse the sort button below to change the board order.");
        lore.add(DIVIDER);
        return lore;
    }

    private String setterName(final OfflinePlayer setter) {
        return setter == null ? "Unknown Patron" : GuiUtil.name(setter);
    }

    private String formatTime(final Instant instant, final String fallback) {
        return instant == null ? fallback : GuiUtil.formatInstant(instant);
    }

    private List<Entry> entries() {
        final List<Entry> out = new ArrayList<>();

        for (Object raw : GuiAccess.bounties(plugin)) {
            final UUID targetUuid = GuiAccess.uuid(raw, "getTargetUuid", "targetUuid", "getTarget", "target", "getPlayerUuid", "playerUuid");
            if (targetUuid == null) {
                continue;
            }

            final UUID setterUuid = GuiAccess.uuid(raw, "getSetterUuid", "setterUuid", "getPlacedBy", "placedBy", "getCreatorUuid", "creatorUuid", "getSetter", "setter");
            final double amount = GuiAccess.number(raw, "getAmount", "amount", "getMoney", "money", "getValue", "value");
            final Instant createdAt = GuiAccess.instant(raw, "getCreatedAt", "createdAt", "getTimestamp", "timestamp", "getCreationTime", "creationTime");
            final Instant updatedAt = GuiAccess.instant(raw, "getUpdatedAt", "updatedAt", "getLastUpdated", "lastUpdated");

            out.add(new Entry(targetUuid, setterUuid, amount, createdAt, updatedAt));
        }

        out.sort(comparator());
        return out;
    }

    private Comparator<Entry> comparator() {
        return switch (sort) {
            case BOUNTY_ASC -> Comparator.comparingDouble(Entry::amount)
                    .thenComparing(entry -> GuiUtil.name(Bukkit.getOfflinePlayer(entry.targetUuid())).toLowerCase());
            case NEWEST -> Comparator.comparing(Entry::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case OLDEST -> Comparator.comparing(Entry::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparingDouble(Entry::amount).reversed()
                    .thenComparing(entry -> GuiUtil.name(Bukkit.getOfflinePlayer(entry.targetUuid())).toLowerCase());
        };
    }

    private record Entry(UUID targetUuid, UUID setterUuid, double amount, Instant createdAt, Instant updatedAt) {
    }
}