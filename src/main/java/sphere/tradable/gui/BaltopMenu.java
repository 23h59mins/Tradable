// src/main/java/sphere/tradable/gui/BaltopMenu.java
package sphere.tradable.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sphere.tradable.Tradable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BaltopMenu extends BaseMenu {
    private static final String MENU_TITLE = "&6&lWealth Leaderboard";
    private static final String DIVIDER = "&8&m------------------------";
    private static final String EMPTY_TITLE = "&6&lNo Ranking Data";
    private static final List<String> EMPTY_LORE = List.of(
            "&7No balance records are available yet.",
            "&7Once players build their wealth,",
            "&7the leaderboard will appear here.",
            "",
            "&8Come back after the economy heats up."
    );

    public BaltopMenu(final Tradable plugin, final Player viewer) {
        super(plugin, viewer.getUniqueId(), MENU_TITLE, GuiSort.BALANCE_DESC);
    }

    @Override
    protected void drawContent() {
        final List<Entry> entries = entries();

        if (entries.isEmpty()) {
            inventory.setItem(22, GuiUtil.item(Material.BOOK, EMPTY_TITLE, EMPTY_LORE));
            return;
        }

        final int from = page * PAGE_SIZE;
        final int to = Math.min(entries.size(), from + PAGE_SIZE);

        for (int i = from; i < to; i++) {
            final Entry entry = entries.get(i);
            final OfflinePlayer target = Bukkit.getOfflinePlayer(entry.uuid());
            final int slot = CONTENT_SLOTS[i - from];

            inventory.setItem(slot, GuiUtil.head(
                    target,
                    rankName(i + 1, target),
                    buildLore(entry, target)
            ));
        }
    }

    @Override
    protected boolean hasNextPage() {
        return (page + 1) * PAGE_SIZE < entries().size();
    }

    @Override
    protected void cycleSort() {
        sort = sort == GuiSort.BALANCE_DESC ? GuiSort.BALANCE_ASC : GuiSort.BALANCE_DESC;
    }

    private String rankName(final int rank, final OfflinePlayer target) {
        return switch (rank) {
            case 1 -> "&6&l♔ #1 &f" + GuiUtil.name(target);
            case 2 -> "&7&l✦ #2 &f" + GuiUtil.name(target);
            case 3 -> "&c&l✦ #3 &f" + GuiUtil.name(target);
            default -> "&e#" + rank + " &f" + GuiUtil.name(target);
        };
    }

    private List<String> buildLore(final Entry entry, final OfflinePlayer target) {
        final List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("&6&lWealth Details");
        lore.add("&7Player: &f" + GuiUtil.name(target));
        lore.add("&7Net Worth: &a" + GuiUtil.money(entry.balance()));
        lore.add("&7Compact View: &e" + GuiUtil.compact(entry.balance()));
        lore.add("");
        lore.add("&8UUID: " + entry.uuid());
        lore.add(DIVIDER);
        return lore;
    }

    private List<Entry> entries() {
        final Map<UUID, Double> balances = GuiAccess.balances(plugin);
        final List<Entry> out = new ArrayList<>(balances.size());

        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            out.add(new Entry(entry.getKey(), entry.getValue()));
        }

        final Comparator<Entry> comparator = Comparator.comparingDouble(Entry::balance)
                .thenComparing(entry -> GuiUtil.name(Bukkit.getOfflinePlayer(entry.uuid())).toLowerCase());

        out.sort(sort == GuiSort.BALANCE_DESC ? comparator.reversed() : comparator);
        return out;
    }

    private record Entry(UUID uuid, double balance) {
    }
}