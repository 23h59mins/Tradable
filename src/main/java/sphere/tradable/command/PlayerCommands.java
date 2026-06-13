package sphere.tradable.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import sphere.tradable.Tradable;
import sphere.tradable.gui.AuctionMenu;
import sphere.tradable.gui.BaltopMenu;
import sphere.tradable.gui.BountyConfirmMenu;
import sphere.tradable.gui.BountyMenu;
import sphere.tradable.gui.ProvideConfirmMenu;
import sphere.tradable.gui.ShopMenu;
import sphere.tradable.util.MoneyUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PlayerCommands implements CommandExecutor, TabCompleter {
    private static final List<String> EMPTY = Collections.emptyList();
    private static final List<String> MONEY_SUGGESTIONS = List.of("100", "1K", "10K", "100K", "1M");

    private static final String AUCTION_LIMIT_BYPASS_PERMISSION = "tradable.auction.bypasslimit";
    private static final String AUCTION_MAX_LISTINGS_PATH = "auction-house.max-listings-per-player";
    private static final String AUCTION_MINIMUM_PRICE_PATH = "auction-house.minimum-price";
    private static final String AUCTION_ENABLED_PATH = "auction-house.enabled";

    private static final String PREFIX = "&6&lTradable &8» ";
    private static final String SOFT = "&8• ";
    private static final String HIGHLIGHT = "&e";
    private static final String POSITIVE = "&a";
    private static final String NEGATIVE = "&c";
    private static final String INFO = "&7";
    private static final String MUTED = "&8";

    private final Tradable plugin;

    public PlayerCommands(final Tradable plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            final CommandSender sender,
            final Command command,
            final String label,
            final String[] args
    ) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "bal" -> handleBal(sender, args);
            case "baltop" -> handleBaltop(sender, args);
            case "shop" -> handleShop(sender, args);
            case "provide" -> handleProvide(sender, args);
            case "bountyset" -> handleBountySet(sender, args);
            case "bounty" -> handleBounty(sender, args);
            case "ah" -> handleAh(sender, args);
            case "ahsell" -> handleAhSell(sender, args);
            default -> false;
        };
    }

    private boolean handleBal(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length != 0) {
            sendUsage(player, "/bal", "/bal");
            return true;
        }

        final double balance = plugin.getEconomyService().getBalance(player);
        sendSuccess(player, "Your current balance is " + HIGHLIGHT + formatMoney(balance) + POSITIVE + ".");
        sendInfo(player, "Spend wisely, trade smart.");
        return true;
    }

    private boolean handleBaltop(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length != 0) {
            sendUsage(player, "/baltop", "/baltop");
            return true;
        }

        new BaltopMenu(plugin, player).open(player);
        return true;
    }

    private boolean handleShop(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length != 0) {
            sendUsage(player, "/shop", "/shop");
            return true;
        }

        new ShopMenu(plugin, player).open(player);
        return true;
    }

    private boolean handleProvide(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length != 2) {
            sendUsage(player, "/provide <player> <money>", "/provide Notch 2.5K");
            sendMoneyHint(player);
            return true;
        }

        final Player target = findOnlinePlayer(args[0]);
        if (target == null) {
            sendError(player, "That player is not online right now.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            sendError(player, "You cannot send money to yourself.");
            return true;
        }

        final Double amount = parsePositiveAmount(args[1]);
        if (amount == null) {
            sendError(player, "Please enter a valid positive amount.");
            sendMoneyHint(player);
            return true;
        }

        if (plugin.getEconomyService().getBalance(player) < amount) {
            sendError(player, "You do not have enough funds for that transfer.");
            return true;
        }

        new ProvideConfirmMenu(plugin, player, target, amount).open(player);
        return true;
    }

    private boolean handleBountySet(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length != 2) {
            sendUsage(player, "/bountyset <player> <money>", "/bountyset Steve 5K");
            sendMoneyHint(player);
            return true;
        }

        final OfflinePlayer target = resolveKnownPlayer(args[0]);
        if (target == null || target.getName() == null) {
            sendError(player, "That player could not be found.");
            return true;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            sendError(player, "You cannot place a bounty on yourself.");
            return true;
        }

        final Double amount = parsePositiveAmount(args[1]);
        if (amount == null) {
            sendError(player, "Please enter a valid positive bounty amount.");
            sendInfo(player, "Examples: " + HIGHLIGHT + "750" + INFO + ", " + HIGHLIGHT + "2K" + INFO + ", " + HIGHLIGHT + "1.5M");
            return true;
        }

        if (plugin.getEconomyService().getBalance(player) < amount) {
            sendError(player, "You do not have enough funds to place that bounty.");
            return true;
        }

        new BountyConfirmMenu(plugin, player, target, amount).open(player);
        return true;
    }

    private boolean handleBounty(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length != 0) {
            sendUsage(player, "/bounty", "/bounty");
            return true;
        }

        new BountyMenu(plugin, player).open(player);
        return true;
    }

    private boolean handleAh(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length != 0) {
            sendUsage(player, "/ah", "/ah");
            return true;
        }

        if (!isAuctionHouseEnabled()) {
            sendError(player, "The Auction House is currently unavailable.");
            sendInfo(player, "Please try again later.");
            return true;
        }

        new AuctionMenu(plugin, player).open(player);
        return true;
    }

    private boolean handleAhSell(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!isAuctionHouseEnabled()) {
            sendError(player, "The Auction House is currently unavailable.");
            sendInfo(player, "Please try again later.");
            return true;
        }

        if (args.length != 1) {
            sendUsage(player, "/ahsell <money>", "/ahsell 12.5K");
            sendMoneyHint(player);
            return true;
        }

        final Double price = parsePositiveAmount(args[0]);
        if (price == null) {
            sendError(player, "Please enter a valid positive listing price.");
            sendMoneyHint(player);
            return true;
        }

        final double minimumPrice = getMinimumAuctionPrice();
        if (price < minimumPrice) {
            sendError(player, "That listing is below the minimum price.");
            sendInfo(player, "Minimum required: " + HIGHLIGHT + formatMoney(minimumPrice));
            return true;
        }

        final int maxListings = getMaxListingsPerPlayer();
        final int currentListings = getActiveAuctionListings(player);

        if (!player.hasPermission(AUCTION_LIMIT_BYPASS_PERMISSION) && currentListings >= maxListings) {
            sendError(player, "You have reached your Auction House listing limit.");
            sendInfo(player, "Used slots: " + HIGHLIGHT + currentListings + INFO + "/" + HIGHLIGHT + maxListings);
            return true;
        }

        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR || heldItem.getType().isAir()) {
            sendError(player, "Hold the item you want to list in your main hand.");
            return true;
        }

        final ItemStack listedItem = heldItem.clone();
        listedItem.setAmount(1);

        try {
            plugin.getAuctionService().createAuction(player, listedItem, price);
        } catch (final IllegalArgumentException exception) {
            sendError(player, "Listing failed: " + HIGHLIGHT + exception.getMessage());
            return true;
        }

        if (heldItem.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            heldItem.setAmount(heldItem.getAmount() - 1);
            player.getInventory().setItemInMainHand(heldItem);
        }

        final int afterListings = currentListings + 1;

        sendSuccess(player, "Your item has been listed on the Auction House.");
        sendInfo(player, "Item: " + HIGHLIGHT + prettyMaterialName(listedItem.getType()));
        sendInfo(player, "Price: " + HIGHLIGHT + formatMoney(price));

        if (!player.hasPermission(AUCTION_LIMIT_BYPASS_PERMISSION)) {
            sendInfo(player, "Used slots: " + HIGHLIGHT + afterListings + INFO + "/" + HIGHLIGHT + maxListings);
        }

        return true;
    }

    private boolean isAuctionHouseEnabled() {
        return plugin.getConfig().getBoolean(AUCTION_ENABLED_PATH, true);
    }

    private int getMaxListingsPerPlayer() {
        return Math.max(1, plugin.getConfig().getInt(AUCTION_MAX_LISTINGS_PATH, 5));
    }

    private double getMinimumAuctionPrice() {
        return Math.max(0.01D, plugin.getConfig().getDouble(AUCTION_MINIMUM_PRICE_PATH, 1.0D));
    }

    private int getActiveAuctionListings(final Player player) {
        if (player == null) {
            return 0;
        }

        return plugin.getAuctionService().getAuctionsBySeller(player.getUniqueId(), null).size();
    }

    @Override
    public List<String> onTabComplete(
            final CommandSender sender,
            final Command command,
            final String alias,
            final String[] args
    ) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "provide", "bountyset" -> {
                if (args.length == 1) {
                    yield onlinePlayerNames(args[0], sender);
                }
                if (args.length == 2) {
                    yield filterPrefix(MONEY_SUGGESTIONS, args[1]);
                }
                yield EMPTY;
            }
            case "ahsell" -> args.length == 1 ? filterPrefix(MONEY_SUGGESTIONS, args[0]) : EMPTY;
            default -> EMPTY;
        };
    }

    private Player requirePlayer(final CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }

        sender.sendMessage(color(PREFIX + NEGATIVE + "Only players can use this command."));
        return null;
    }

    private Player findOnlinePlayer(final String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        final Player exact = Bukkit.getPlayerExact(input);
        return exact != null ? exact : Bukkit.getPlayer(input);
    }

    private OfflinePlayer resolveKnownPlayer(final String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        final Player online = findOnlinePlayer(input);
        if (online != null) {
            return online;
        }

        for (final OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(input)) {
                return offline;
            }
        }

        return null;
    }

    private List<String> onlinePlayerNames(final String prefix, final CommandSender sender) {
        final List<String> names = new ArrayList<>();

        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player self && online.getUniqueId().equals(self.getUniqueId())) {
                continue;
            }

            names.add(online.getName());
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        return filterPrefix(names, prefix);
    }

    private List<String> filterPrefix(final List<String> source, final String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>(source);
        }

        final String lowered = prefix.toLowerCase(Locale.ROOT);
        final List<String> matches = new ArrayList<>();

        for (final String entry : source) {
            if (entry.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                matches.add(entry);
            }
        }

        return matches;
    }

    private Double parsePositiveAmount(final String raw) {
        try {
            final double value = MoneyUtil.parse(raw);
            return value > 0D ? value : null;
        } catch (final Exception exception) {
            return null;
        }
    }

    private String formatMoney(final double amount) {
        return "$" + MoneyUtil.formatCompact(amount);
    }

    private String prettyMaterialName(final Material material) {
        final String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        final StringBuilder builder = new StringBuilder();

        for (final String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(' ');
        }

        return builder.toString().trim();
    }

    private void sendUsage(final CommandSender sender, final String usage, final String example) {
        sendError(sender, "Usage: " + HIGHLIGHT + usage);

        if (example != null && !example.isBlank()) {
            sendInfo(sender, "Example: " + HIGHLIGHT + example);
        }
    }

    private void sendMoneyHint(final CommandSender sender) {
        sendInfo(sender, "Compact values work too: " + HIGHLIGHT + "1.5K" + INFO + ", " + HIGHLIGHT + "2M" + INFO + ", " + HIGHLIGHT + "3.2B");
    }

    private void sendSuccess(final CommandSender sender, final String message) {
        sender.sendMessage(color(PREFIX + POSITIVE + message));
    }

    private void sendError(final CommandSender sender, final String message) {
        sender.sendMessage(color(PREFIX + NEGATIVE + message));
    }

    private void sendInfo(final CommandSender sender, final String message) {
        sender.sendMessage(color(SOFT + INFO + message));
    }

    private String color(final String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}