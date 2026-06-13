package sphere.tradable.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sphere.tradable.Tradable;
import sphere.tradable.model.AuctionEntry;
import sphere.tradable.model.BountyEntry;
import sphere.tradable.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TradablePlaceholderExpansion extends PlaceholderExpansion {
    private static final String ZERO_COMPACT = "0";
    private static final String ZERO_FULL = "0.00";
    private static final String EMPTY = "";

    private final Tradable plugin;

    public TradablePlaceholderExpansion(final Tradable plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tradable";
    }

    @Override
    public @NotNull String getAuthor() {
        final List<String> authors = plugin.getDescription().getAuthors();
        return authors.isEmpty() ? "Unknown" : String.join(", ", authors);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(final OfflinePlayer player, @NotNull final String params) {
        final String key = params.trim();
        if (key.isEmpty()) {
            return null;
        }

        final String lowered = key.toLowerCase(Locale.ROOT);

        return switch (lowered) {
            case "balance", "balance_compact" -> formatCompact(balanceOf(player));
            case "balance_full" -> formatFull(balanceOf(player));
            case "balance_raw" -> formatRaw(balanceOf(player));

            case "auction_count" -> player == null ? "0" : String.valueOf(playerAuctionCount(player.getUniqueId()));
            case "auction_total_value" -> player == null ? ZERO_COMPACT : formatCompact(playerAuctionTotalValue(player.getUniqueId()));
            case "auction_total_value_full" -> player == null ? ZERO_FULL : formatFull(playerAuctionTotalValue(player.getUniqueId()));
            case "auction_total_value_raw" -> player == null ? ZERO_FULL : formatRaw(playerAuctionTotalValue(player.getUniqueId()));

            case "bounty_target_count" -> player == null ? "0" : String.valueOf(playerBountyCount(player.getUniqueId()));
            case "bounty_target_total" -> player == null ? ZERO_COMPACT : formatCompact(playerBountyTotal(player.getUniqueId()));
            case "bounty_target_total_full" -> player == null ? ZERO_FULL : formatFull(playerBountyTotal(player.getUniqueId()));
            case "bounty_target_total_raw" -> player == null ? ZERO_FULL : formatRaw(playerBountyTotal(player.getUniqueId()));

            case "auction_total_listings" -> String.valueOf(totalAuctionListings());
            case "auction_total_value_all" -> formatCompact(globalAuctionValue());
            case "auction_total_value_all_full" -> formatFull(globalAuctionValue());
            case "auction_total_value_all_raw" -> formatRaw(globalAuctionValue());

            case "bounty_total_entries" -> String.valueOf(totalBountyEntries());
            case "bounty_total_value_all" -> formatCompact(globalBountyValue());
            case "bounty_total_value_all_full" -> formatFull(globalBountyValue());
            case "bounty_total_value_all_raw" -> formatRaw(globalBountyValue());

            case "baltop_position" -> player == null ? "0" : String.valueOf(balanceTopPosition(player.getUniqueId()));
            case "baltop_balance" -> formatCompact(balanceOf(player));
            case "baltop_balance_full" -> formatFull(balanceOf(player));
            case "baltop_balance_raw" -> formatRaw(balanceOf(player));

            case "shop_enabled" -> String.valueOf(plugin.getConfig().getBoolean("shop.enabled", true));
            case "shop_category_count" -> String.valueOf(shopCategoryCount());

            case "viewer_name" -> player == null || player.getName() == null ? EMPTY : player.getName();
            case "viewer_uuid" -> player == null || player.getUniqueId() == null ? EMPTY : player.getUniqueId().toString();
            default -> handleDynamicPlaceholders(lowered);
        };
    }

    private @Nullable String handleDynamicPlaceholders(final String key) {
        if (key.startsWith("player_balance_compact_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_balance_compact_".length()));
            return target == null ? ZERO_COMPACT : formatCompact(balanceOf(target));
        }

        if (key.startsWith("player_balance_full_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_balance_full_".length()));
            return target == null ? ZERO_FULL : formatFull(balanceOf(target));
        }

        if (key.startsWith("player_balance_raw_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_balance_raw_".length()));
            return target == null ? ZERO_FULL : formatRaw(balanceOf(target));
        }

        if (key.startsWith("player_balance_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_balance_".length()));
            return target == null ? ZERO_COMPACT : formatCompact(balanceOf(target));
        }

        if (key.startsWith("player_auction_count_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_auction_count_".length()));
            return target == null ? "0" : String.valueOf(playerAuctionCount(target.getUniqueId()));
        }

        if (key.startsWith("player_auction_total_value_full_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_auction_total_value_full_".length()));
            return target == null ? ZERO_FULL : formatFull(playerAuctionTotalValue(target.getUniqueId()));
        }

        if (key.startsWith("player_auction_total_value_raw_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_auction_total_value_raw_".length()));
            return target == null ? ZERO_FULL : formatRaw(playerAuctionTotalValue(target.getUniqueId()));
        }

        if (key.startsWith("player_auction_total_value_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_auction_total_value_".length()));
            return target == null ? ZERO_COMPACT : formatCompact(playerAuctionTotalValue(target.getUniqueId()));
        }

        if (key.startsWith("player_bounty_count_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_bounty_count_".length()));
            return target == null ? "0" : String.valueOf(playerBountyCount(target.getUniqueId()));
        }

        if (key.startsWith("player_bounty_total_full_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_bounty_total_full_".length()));
            return target == null ? ZERO_FULL : formatFull(playerBountyTotal(target.getUniqueId()));
        }

        if (key.startsWith("player_bounty_total_raw_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_bounty_total_raw_".length()));
            return target == null ? ZERO_FULL : formatRaw(playerBountyTotal(target.getUniqueId()));
        }

        if (key.startsWith("player_bounty_total_")) {
            final OfflinePlayer target = resolvePlayerToken(key.substring("player_bounty_total_".length()));
            return target == null ? ZERO_COMPACT : formatCompact(playerBountyTotal(target.getUniqueId()));
        }

        if (key.startsWith("top_balance_name_")) {
            final Integer rank = parsePositiveInt(key.substring("top_balance_name_".length()));
            return rank == null ? null : topBalanceName(rank);
        }

        if (key.startsWith("top_balance_value_full_")) {
            final Integer rank = parsePositiveInt(key.substring("top_balance_value_full_".length()));
            return rank == null ? null : topBalanceValue(rank, ValueFormat.FULL);
        }

        if (key.startsWith("top_balance_value_raw_")) {
            final Integer rank = parsePositiveInt(key.substring("top_balance_value_raw_".length()));
            return rank == null ? null : topBalanceValue(rank, ValueFormat.RAW);
        }

        if (key.startsWith("top_balance_value_")) {
            final Integer rank = parsePositiveInt(key.substring("top_balance_value_".length()));
            return rank == null ? null : topBalanceValue(rank, ValueFormat.COMPACT);
        }

        if (key.startsWith("top_balance_uuid_")) {
            final Integer rank = parsePositiveInt(key.substring("top_balance_uuid_".length()));
            return rank == null ? null : topBalanceUuid(rank);
        }

        return null;
    }

    private double balanceOf(final OfflinePlayer player) {
        return player == null ? 0D : safe(plugin.getEconomyService().getBalance(player));
    }

    private OfflinePlayer resolvePlayerToken(final String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        final String raw = token.trim();

        try {
            final UUID uuid = UUID.fromString(raw);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (final IllegalArgumentException ignored) {
        }

        final org.bukkit.entity.Player online = Bukkit.getPlayerExact(raw);
        if (online != null) {
            return online;
        }

        for (final OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(raw)) {
                return offline;
            }
        }

        return null;
    }

    private int playerAuctionCount(final UUID playerId) {
        if (playerId == null) {
            return 0;
        }

        return plugin.getAuctionService().getAuctionsBySeller(playerId, null).size();
    }

    private double playerAuctionTotalValue(final UUID playerId) {
        if (playerId == null) {
            return 0D;
        }

        double total = 0D;

        for (final AuctionEntry entry : plugin.getAuctionService().getAuctionsBySeller(playerId, null)) {
            if (entry != null) {
                total += safe(entry.getPrice());
            }
        }

        return safe(total);
    }

    private int playerBountyCount(final UUID targetId) {
        if (targetId == null) {
            return 0;
        }

        return plugin.getBountyService().getBountiesForTarget(targetId, null).size();
    }

    private double playerBountyTotal(final UUID targetId) {
        if (targetId == null) {
            return 0D;
        }

        return safe(plugin.getBountyService().getTotalBounty(targetId));
    }

    private int totalAuctionListings() {
        return plugin.getAuctionService().getAuctions().size();
    }

    private double globalAuctionValue() {
        double total = 0D;

        for (final AuctionEntry entry : plugin.getAuctionService().getAuctions()) {
            if (entry != null) {
                total += safe(entry.getPrice());
            }
        }

        return safe(total);
    }

    private int totalBountyEntries() {
        return plugin.getBountyService().getBounties().size();
    }

    private double globalBountyValue() {
        double total = 0D;

        for (final BountyEntry entry : plugin.getBountyService().getBounties()) {
            if (entry != null) {
                total += safe(entry.getAmount());
            }
        }

        return safe(total);
    }

    private int balanceTopPosition(final UUID playerId) {
        if (playerId == null) {
            return 0;
        }

        int position = 1;

        for (final UUID id : plugin.getEconomyService().getTopBalances(10_000).keySet()) {
            if (playerId.equals(id)) {
                return position;
            }
            position++;
        }

        return 0;
    }

    private int shopCategoryCount() {
        final org.bukkit.configuration.ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("shop.categories");
        return section == null ? 0 : section.getKeys(false).size();
    }

    private String topBalanceName(final int rank) {
        final UUID uuid = topBalanceUuidInternal(rank);
        if (uuid == null) {
            return EMPTY;
        }

        final OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString() : player.getName();
    }

    private String topBalanceValue(final int rank, final ValueFormat format) {
        final Double value = topBalanceAmount(rank);
        if (value == null) {
            return format == ValueFormat.COMPACT ? ZERO_COMPACT : ZERO_FULL;
        }

        return switch (format) {
            case COMPACT -> formatCompact(value);
            case FULL -> formatFull(value);
            case RAW -> formatRaw(value);
        };
    }

    private @Nullable Double topBalanceAmount(final int rank) {
        if (rank <= 0) {
            return null;
        }

        final Map<UUID, Double> top = plugin.getEconomyService().getTopBalances(rank);
        if (top.isEmpty() || top.size() < rank) {
            return null;
        }

        int index = 1;
        for (final Double value : top.values()) {
            if (index == rank) {
                return safe(value);
            }
            index++;
        }

        return null;
    }

    private String topBalanceUuid(final int rank) {
        final UUID uuid = topBalanceUuidInternal(rank);
        return uuid == null ? EMPTY : uuid.toString();
    }

    private UUID topBalanceUuidInternal(final int rank) {
        if (rank <= 0) {
            return null;
        }

        final Map<UUID, Double> top = plugin.getEconomyService().getTopBalances(rank);
        if (top.isEmpty() || top.size() < rank) {
            return null;
        }

        int index = 1;
        for (final UUID uuid : top.keySet()) {
            if (index == rank) {
                return uuid;
            }
            index++;
        }

        return null;
    }

    private Integer parsePositiveInt(final String raw) {
        try {
            final int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (final Exception exception) {
            return null;
        }
    }

    private String formatCompact(final double value) {
        return MoneyUtil.formatCompact(safe(value));
    }

    private String formatFull(final double value) {
        return MoneyUtil.format(safe(value));
    }

    private String formatRaw(final double value) {
        final BigDecimal decimal = BigDecimal.valueOf(safe(value)).stripTrailingZeros();
        return decimal.scale() < 0 ? decimal.setScale(0).toPlainString() : decimal.toPlainString();
    }

    private double safe(final double value) {
        return MoneyUtil.sanitize(value);
    }

    private enum ValueFormat {
        COMPACT,
        FULL,
        RAW
    }
}