// File: src/main/java/sphere/tradable/command/AdminCommands.java
package sphere.tradable.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sphere.tradable.Tradable;
import sphere.tradable.gui.BaseMenu;
import sphere.tradable.util.MoneyUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class AdminCommands implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "tradable.admin";
    private static final String SHOP_ROOT = "shop.categories";

    private static final int CONTENT_PAGE_SIZE = BaseMenu.PAGE_SIZE;
    private static final double MIN_PRICE = 0.000001D;

    private static final String BORDER_STRONG = "&8&m━━━━━━━━━━━━━━━━━━━━";
    private static final String BORDER_SOFT = "&8&m--------------------";
    private static final String ACCENT = "&6✦";
    private static final String PRICE_TAG = "&e&lPrice";
    private static final String BUY_HINT = "&7Click to trade this item.";
    private static final String CATEGORY_HINT = "&7Browse this collection.";
    private static final String CATEGORY_SUBTITLE = "&8Curated goods ready for trade";
    private static final String ITEM_FOOTER = "&8Tradable Shop";
    private static final String CATEGORY_FOOTER = "&8Tradable Collection";

    private static final String HELP_PREFIX = "&6&lTradable &8» ";
    private static final String HELP_DIVIDER = "&8&m----------------------------------------";
    private static final String HELP_SECTION = "&6&l";
    private static final String HELP_ENTRY = "&8▪ &f";
    private static final String HELP_DESC = " &8- &7";
    private static final String HELP_EXAMPLE = "&8   ↳ &e";
    private static final String HELP_NOTE = "&8• &7";

    private static final String NEGATIVE = "&c";
    private static final String POSITIVE = "&a";
    private static final String INFO = "&7";
    private static final String HIGHLIGHT = "&e";

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "addmoney",
            "minusmoney",
            "addcategory",
            "renamecategory",
            "removecategory",
            "removeallcategory",
            "additem",
            "removeitem",
            "setprice",
            "seticoncategory",
            "reload"
    );

    private static final List<String> MONEY_SUGGESTIONS = List.of("100", "1K", "10K", "100K", "1M");
    private static final List<String> SLOT_SUGGESTIONS = List.of("1", "2", "3", "4", "5", "10", "20", "45");

    private final Tradable plugin;

    public AdminCommands(final Tradable plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            final CommandSender sender,
            final Command command,
            final String label,
            final String[] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sendError(sender, "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        final String subcommand = args[0].toLowerCase(Locale.ROOT);

        return switch (subcommand) {
            case "addmoney" -> handleAddMoney(sender, args);
            case "minusmoney" -> handleMinusMoney(sender, args);
            case "addcategory" -> handleAddCategory(sender, args);
            case "renamecategory" -> handleRenameCategory(sender, args);
            case "removecategory" -> handleRemoveCategory(sender, args);
            case "removeallcategory" -> handleRemoveAllCategory(sender, args);
            case "additem" -> handleAddItem(sender, args);
            case "removeitem" -> handleRemoveItem(sender, args);
            case "setprice" -> handleSetPrice(sender, args);
            case "seticoncategory" -> handleSetIconCategory(sender, args);
            case "reload" -> handleReload(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleReload(final CommandSender sender, final String[] args) {
        if (args.length != 1) {
            sendCompactUsage(sender, "/tradable reload", "/tradable reload");
            return true;
        }

        try {
            plugin.reloadPlugin();
            sendSuccess(sender, "Tradable has been reloaded successfully.");
        } catch (final Exception exception) {
            sendError(sender, "Failed to reload Tradable. Check console logs.");
            plugin.getLogger().log(Level.SEVERE, "Failed to reload Tradable.", exception);
        }

        return true;
    }

    private boolean handleAddMoney(final CommandSender sender, final String[] args) {
        final TargetAndAmount parsed = parseTargetAndAmount(
                sender,
                args,
                "/tradable addmoney [player] <money>",
                "/tradable addmoney Notch 25K"
        );
        if (parsed == null) {
            return true;
        }

        plugin.getEconomyService().deposit(parsed.target(), parsed.amount());

        sendSuccess(sender, "Added " + HIGHLIGHT + formatMoney(parsed.amount()) + POSITIVE + " to " + HIGHLIGHT + safeName(parsed.target()) + POSITIVE + ".");
        notifyTarget(parsed.target(), sender, "&aYour balance increased by &e" + formatMoney(parsed.amount()) + "&a.");
        return true;
    }

    private boolean handleMinusMoney(final CommandSender sender, final String[] args) {
        final TargetAndAmount parsed = parseTargetAndAmount(
                sender,
                args,
                "/tradable minusmoney [player] <money>",
                "/tradable minusmoney Notch 5K"
        );
        if (parsed == null) {
            return true;
        }

        final boolean removed = plugin.getEconomyService().withdraw(parsed.target(), parsed.amount());
        if (!removed) {
            sendError(sender, "Failed to remove money.");
            return true;
        }

        sendSuccess(sender, "Removed " + HIGHLIGHT + formatMoney(parsed.amount()) + POSITIVE + " from " + HIGHLIGHT + safeName(parsed.target()) + POSITIVE + ".");
        notifyTarget(parsed.target(), sender, "&cYour balance decreased by &e" + formatMoney(parsed.amount()) + "&c.");
        return true;
    }

    private boolean handleAddCategory(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sendCompactUsage(sender, "/tradable addcategory <name> <slot>", "/tradable addcategory Mob Drops 2");
            return true;
        }

        final Integer absoluteSlot = parsePositiveInteger(args[args.length - 1]);
        if (absoluteSlot == null) {
            sendError(sender, "Slot must be a positive integer starting from 1.");
            sendInfo(sender, "Example: " + HIGHLIGHT + "/tradable addcategory Mob Drops 2");
            return true;
        }

        final String categoryName = joinArgs(args, 1, args.length - 1).trim();
        if (categoryName.isEmpty()) {
            sendError(sender, "Category name cannot be empty.");
            return true;
        }

        ensureShopRoot();

        String categoryKey = findCategoryKeyByName(categoryName);
        final boolean updatingExisting = categoryKey != null;

        if (categoryKey == null) {
            categoryKey = createUniqueCategoryKey(categoryName);
        }

        final String occupiedBy = findCategoryKeyByAbsoluteSlot(absoluteSlot);
        if (occupiedBy != null && !occupiedBy.equalsIgnoreCase(categoryKey)) {
            sendError(sender, "That category slot is already used by " + HIGHLIGHT + stripColor(getCategoryDisplayName(occupiedBy)) + NEGATIVE + ".");
            return true;
        }

        final SlotPosition position = SlotPosition.fromAbsolute(absoluteSlot);
        final String basePath = SHOP_ROOT + "." + categoryKey;
        final FileConfiguration config = plugin.getConfig();

        config.set(basePath + ".name", categoryName);
        config.set(basePath + ".display-name", buildCategoryDisplayName(categoryName));
        config.set(basePath + ".lore", buildCategoryLore(categoryName));

        if (!config.contains(basePath + ".material")) {
            config.set(basePath + ".material", Material.CHEST.name());
        }

        config.set(basePath + ".absolute-slot", absoluteSlot);
        config.set(basePath + ".page", position.page());
        config.set(basePath + ".slot", position.oneBasedSlot());

        if (!saveAndRefreshShopConfig(sender)) {
            return true;
        }

        if (!plugin.getConfig().isConfigurationSection(basePath)) {
            sendError(sender, "Failed to save category. Check config.yml and console logs.");
            return true;
        }

        sendSuccess(
                sender,
                (updatingExisting ? "Updated category " : "Added category ")
                        + HIGHLIGHT + categoryName
                        + POSITIVE + " at absolute slot "
                        + HIGHLIGHT + absoluteSlot
                        + POSITIVE + " (page "
                        + HIGHLIGHT + position.page()
                        + POSITIVE + ", slot "
                        + HIGHLIGHT + position.oneBasedSlot()
                        + POSITIVE + ")."
        );

        return true;
    }

    private boolean handleRenameCategory(final CommandSender sender, final String[] args) {
        ensureShopRoot();

        final RenameCategoryArguments parsed = parseRenameCategoryArguments(args);
        if (parsed == null) {
            sendCompactUsage(
                    sender,
                    "/tradable renamecategory <current name> <new name>",
                    "/tradable renamecategory Mob Drops Rare Drops"
            );
            sendInfo(sender, "Both the current name and the new name may contain spaces.");
            sendCategorySuggestions(sender, "");
            return true;
        }

        final String currentKey = parsed.currentCategoryKey();
        final String currentName = parsed.currentCategoryName();
        final String newName = parsed.newCategoryName().trim();

        if (currentKey == null || !plugin.getConfig().isConfigurationSection(SHOP_ROOT + "." + currentKey)) {
            sendError(sender, "That category does not exist.");
            sendInfo(sender, "Tried to find: " + HIGHLIGHT + currentName);
            sendCategorySuggestions(sender, currentName);
            return true;
        }

        if (newName.isBlank()) {
            sendError(sender, "The new category name cannot be empty.");
            return true;
        }

        final String existingTargetKey = findCategoryKeyByName(newName);
        if (existingTargetKey != null && !existingTargetKey.equalsIgnoreCase(currentKey)) {
            sendError(sender, "A category with that name already exists.");
            sendInfo(sender, "Existing category: " + HIGHLIGHT + stripColor(getCategoryDisplayName(existingTargetKey)));
            return true;
        }

        final String oldBasePath = SHOP_ROOT + "." + currentKey;
        final String targetKey = createRenameTargetKey(currentKey, newName);
        final String newBasePath = SHOP_ROOT + "." + targetKey;
        final String oldDisplayName = stripColor(getCategoryDisplayName(currentKey));

        final ConfigurationSection oldSection = plugin.getConfig().getConfigurationSection(oldBasePath);
        if (oldSection == null) {
            sendError(sender, "The category data could not be loaded.");
            return true;
        }

        if (!targetKey.equalsIgnoreCase(currentKey)) {
            plugin.getConfig().set(newBasePath, null);
            copySection(oldSection, newBasePath);
            plugin.getConfig().set(oldBasePath, null);
        }

        plugin.getConfig().set(newBasePath + ".name", newName);
        plugin.getConfig().set(newBasePath + ".display-name", buildCategoryDisplayName(newName));
        plugin.getConfig().set(newBasePath + ".lore", buildCategoryLore(newName));

        if (!saveAndRefreshShopConfig(sender)) {
            return true;
        }

        if (!plugin.getConfig().isConfigurationSection(newBasePath)) {
            sendError(sender, "Failed to rename category. Check config.yml and console logs.");
            return true;
        }

        sendSuccess(
                sender,
                "Renamed category "
                        + HIGHLIGHT + oldDisplayName
                        + POSITIVE + " to "
                        + HIGHLIGHT + newName
                        + POSITIVE + "."
        );

        return true;
    }

    private boolean handleRemoveCategory(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sendCompactUsage(sender, "/tradable removecategory <name>", "/tradable removecategory Mob Drops");
            return true;
        }

        ensureShopRoot();

        final String categoryName = joinArgs(args, 1, args.length).trim();
        final String categoryKey = findCategoryKeyByName(categoryName);

        if (categoryKey == null) {
            sendError(sender, "That category does not exist.");
            sendInfo(sender, "Tried to find: " + HIGHLIGHT + categoryName);
            sendCategorySuggestions(sender, categoryName);
            return true;
        }

        final String displayName = getCategoryDisplayName(categoryKey);
        final String basePath = SHOP_ROOT + "." + categoryKey;

        plugin.getConfig().set(basePath, null);

        if (!saveAndRefreshShopConfig(sender)) {
            return true;
        }

        if (plugin.getConfig().contains(basePath)) {
            sendError(sender, "Failed to remove category. Check config.yml and console logs.");
            return true;
        }

        sendSuccess(sender, "Removed category " + HIGHLIGHT + stripColor(displayName) + POSITIVE + ".");
        return true;
    }

    private boolean handleRemoveAllCategory(final CommandSender sender, final String[] args) {
        if (args.length != 1) {
            sendCompactUsage(sender, "/tradable removeallcategory", "/tradable removeallcategory");
            return true;
        }

        ensureShopRoot();

        final ConfigurationSection categories = getCategoriesSection();
        final int removedCount = categories == null ? 0 : categories.getKeys(false).size();

        plugin.getConfig().set(SHOP_ROOT, null);
        plugin.getConfig().createSection(SHOP_ROOT);

        if (!saveAndRefreshShopConfig(sender)) {
            return true;
        }

        sendSuccess(sender, "Removed " + HIGHLIGHT + removedCount + POSITIVE + " shop categor" + (removedCount == 1 ? "y" : "ies") + POSITIVE + ".");
        return true;
    }

    private boolean handleAddItem(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        ensureShopRoot();

        final AddItemArguments parsedArgs = parseAddItemArguments(args);
        if (parsedArgs == null) {
            sendCompactUsage(player, "/tradable additem <category> <slot> <price>", "/tradable additem Blocks 1 1.5K");
            sendInfo(player, "Example: " + HIGHLIGHT + "/tradable additem Mob Drops 1 250");
            sendInfo(player, "When replacing an existing item, price may be omitted to keep the current price.");
            sendCategorySuggestions(player, "");
            return true;
        }

        final String categoryKey = parsedArgs.categoryKey();
        if (categoryKey == null || !plugin.getConfig().isConfigurationSection(SHOP_ROOT + "." + categoryKey)) {
            sendError(player, "That category does not exist.");
            sendInfo(player, "Tried to find: " + HIGHLIGHT + parsedArgs.categoryName());
            sendCategorySuggestions(player, parsedArgs.categoryName());
            return true;
        }

        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (isAir(heldItem)) {
            sendError(player, "Hold the item in your main hand.");
            return true;
        }

        final String categoryPath = SHOP_ROOT + "." + categoryKey;
        final SlotPosition position = SlotPosition.fromAbsolute(parsedArgs.absoluteSlot());
        final String itemPath = categoryPath + ".items." + parsedArgs.absoluteSlot();
        final boolean replacingExisting = plugin.getConfig().isConfigurationSection(itemPath);

        final Double price;

        if (parsedArgs.price() != null) {
            price = parsedArgs.price();

            if (!isValidPrice(price)) {
                sendError(player, "Price must be a positive number.");
                return true;
            }
        } else if (replacingExisting) {
            final double currentPrice = plugin.getConfig().getDouble(itemPath + ".price", Double.NaN);

            if (!isValidPrice(currentPrice)) {
                sendError(player, "The existing item has no valid price.");
                sendInfo(player, "Use: " + HIGHLIGHT + "/tradable additem " + parsedArgs.categoryName() + " " + parsedArgs.absoluteSlot() + " <price>");
                return true;
            }

            price = currentPrice;
        } else {
            sendError(player, "New shop items must have a positive price.");
            sendInfo(player, "Example: " + HIGHLIGHT + "/tradable additem " + parsedArgs.categoryName() + " " + parsedArgs.absoluteSlot() + " 100");
            return true;
        }

        writeShopItem(itemPath, heldItem, price);
        plugin.getConfig().set(itemPath + ".absolute-slot", parsedArgs.absoluteSlot());
        plugin.getConfig().set(itemPath + ".page", position.page());
        plugin.getConfig().set(itemPath + ".slot", position.oneBasedSlot());

        if (!saveAndRefreshShopConfig(player)) {
            return true;
        }

        if (!plugin.getConfig().contains(itemPath + ".material")) {
            sendError(player, "Failed to save item. Check config.yml and console logs.");
            return true;
        }

        sendSuccess(
                player,
                (replacingExisting ? "Replaced item in " : "Added held item to ")
                        + HIGHLIGHT + stripColor(getCategoryDisplayName(categoryKey))
                        + POSITIVE + " at absolute slot "
                        + HIGHLIGHT + parsedArgs.absoluteSlot()
                        + POSITIVE + " (page "
                        + HIGHLIGHT + position.page()
                        + POSITIVE + ", slot "
                        + HIGHLIGHT + position.oneBasedSlot()
                        + POSITIVE + ")."
        );
        sendInfo(player, "Price: " + HIGHLIGHT + formatMoney(price));
        return true;
    }

    private boolean handleSetPrice(final CommandSender sender, final String[] args) {
        ensureShopRoot();

        final CategorySlotPriceArguments slotArgs = parseCategorySlotPriceArguments(args);
        if (slotArgs != null) {
            return handleSetPriceBySlot(sender, slotArgs);
        }

        final CategoryHeldPriceArguments heldArgs = parseCategoryHeldPriceArguments(args);
        if (heldArgs != null) {
            return handleSetPriceByHeldItem(sender, heldArgs);
        }

        sendError(sender, "Invalid setprice syntax.");
        sendInfo(sender, "Usage: " + HIGHLIGHT + "/tradable setprice <category> <price>");
        sendInfo(sender, "Hold the target shop item in your main hand.");
        sendInfo(sender, "Example: " + HIGHLIGHT + "/tradable setprice Mob Drops 2.5K");
        sendInfo(sender, "Or: " + HIGHLIGHT + "/tradable setprice <category> <slot> <price>");
        sendInfo(sender, "Example: " + HIGHLIGHT + "/tradable setprice Mob Drops 1 250");
        sendCategorySuggestions(sender, "");
        return true;
    }

    private boolean handleSetPriceBySlot(final CommandSender sender, final CategorySlotPriceArguments parsedArgs) {
        final String categoryKey = parsedArgs.categoryKey();

        if (categoryKey == null || !plugin.getConfig().isConfigurationSection(SHOP_ROOT + "." + categoryKey)) {
            sendError(sender, "That category does not exist.");
            sendInfo(sender, "Tried to find: " + HIGHLIGHT + parsedArgs.categoryName());
            sendCategorySuggestions(sender, parsedArgs.categoryName());
            return true;
        }

        final String itemPath = SHOP_ROOT + "." + categoryKey + ".items." + parsedArgs.absoluteSlot();

        if (!plugin.getConfig().isConfigurationSection(itemPath)) {
            sendError(sender, "No shop item exists in that category slot.");
            return true;
        }

        plugin.getConfig().set(itemPath + ".price", parsedArgs.price());
        refreshDecoratedItemPresentation(itemPath);

        if (!saveAndRefreshShopConfig(sender)) {
            return true;
        }

        sendSuccess(
                sender,
                "Set price of item in "
                        + HIGHLIGHT + stripColor(getCategoryDisplayName(categoryKey))
                        + POSITIVE + " slot "
                        + HIGHLIGHT + parsedArgs.absoluteSlot()
                        + POSITIVE + " to "
                        + HIGHLIGHT + formatMoney(parsedArgs.price())
                        + POSITIVE + "."
        );
        return true;
    }

    private boolean handleSetPriceByHeldItem(final CommandSender sender, final CategoryHeldPriceArguments parsedArgs) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        final String categoryKey = parsedArgs.categoryKey();

        if (categoryKey == null || !plugin.getConfig().isConfigurationSection(SHOP_ROOT + "." + categoryKey)) {
            sendError(player, "That category does not exist.");
            sendInfo(player, "Tried to find: " + HIGHLIGHT + parsedArgs.categoryName());
            sendCategorySuggestions(player, parsedArgs.categoryName());
            return true;
        }

        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (isAir(heldItem)) {
            sendError(player, "Hold the target shop item in your main hand.");
            return true;
        }

        final String itemPath = findMatchingItemPathInCategory(categoryKey, heldItem);

        if (itemPath == null) {
            sendError(player, "No matching shop item was found in category " + HIGHLIGHT + stripColor(getCategoryDisplayName(categoryKey)) + NEGATIVE + ".");
            sendInfo(player, "Tip: hold the exact item you added to the shop.");
            return true;
        }

        plugin.getConfig().set(itemPath + ".price", parsedArgs.price());
        refreshDecoratedItemPresentation(itemPath);

        if (!saveAndRefreshShopConfig(player)) {
            return true;
        }

        sendSuccess(
                player,
                "Set price of held item in "
                        + HIGHLIGHT + stripColor(getCategoryDisplayName(categoryKey))
                        + POSITIVE + " to "
                        + HIGHLIGHT + formatMoney(parsedArgs.price())
                        + POSITIVE + "."
        );
        return true;
    }

    private boolean handleRemoveItem(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length != 1) {
            sendCompactUsage(player, "/tradable removeitem", "/tradable removeitem");
            return true;
        }

        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (isAir(heldItem)) {
            sendError(player, "Hold the item in your main hand.");
            return true;
        }

        final ConfigurationSection categories = getCategoriesSection();
        if (categories == null) {
            sendError(player, "There are no categories configured.");
            return true;
        }

        int removed = 0;

        for (final String categoryKey : categories.getKeys(false)) {
            final String itemsPath = SHOP_ROOT + "." + categoryKey + ".items";
            final ConfigurationSection items = plugin.getConfig().getConfigurationSection(itemsPath);

            if (items == null) {
                continue;
            }

            final List<String> itemKeys = new ArrayList<>(items.getKeys(false));

            for (final String itemKey : itemKeys) {
                final String itemPath = itemsPath + "." + itemKey;

                if (matchesStoredShopItem(itemPath, heldItem)) {
                    plugin.getConfig().set(itemPath, null);
                    removed++;
                }
            }
        }

        if (removed == 0) {
            sendError(player, "No matching shop item was found.");
            return true;
        }

        if (!saveAndRefreshShopConfig(player)) {
            return true;
        }

        sendSuccess(player, "Removed " + HIGHLIGHT + removed + POSITIVE + " matching shop entr" + (removed == 1 ? "y" : "ies") + POSITIVE + ".");
        return true;
    }

    private boolean handleSetIconCategory(final CommandSender sender, final String[] args) {
        final Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 2) {
            sendCompactUsage(player, "/tradable seticoncategory <category>", "/tradable seticoncategory Mob Drops");
            return true;
        }

        ensureShopRoot();

        final String categoryName = joinArgs(args, 1, args.length).trim();
        final String categoryKey = findCategoryKeyByName(categoryName);

        if (categoryKey == null) {
            sendError(player, "That category does not exist.");
            sendInfo(player, "Tried to find: " + HIGHLIGHT + categoryName);
            sendCategorySuggestions(player, categoryName);
            return true;
        }

        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (isAir(heldItem)) {
            sendError(player, "Hold the item in your main hand.");
            return true;
        }

        final String categoryPath = SHOP_ROOT + "." + categoryKey;

        plugin.getConfig().set(categoryPath + ".material", heldItem.getType().name());

        final String rawCategoryName = plugin.getConfig().getString(
                categoryPath + ".name",
                stripColor(getCategoryDisplayName(categoryKey))
        );
        plugin.getConfig().set(categoryPath + ".display-name", buildCategoryDisplayName(rawCategoryName));
        plugin.getConfig().set(categoryPath + ".lore", buildCategoryLore(rawCategoryName));

        if (!saveAndRefreshShopConfig(player)) {
            return true;
        }

        if (!plugin.getConfig().contains(categoryPath + ".material")) {
            sendError(player, "Failed to save category icon. Check config.yml and console logs.");
            return true;
        }

        sendSuccess(player, "Set icon for category " + HIGHLIGHT + stripColor(getCategoryDisplayName(categoryKey)) + POSITIVE + ".");
        return true;
    }

    private AddItemArguments parseAddItemArguments(final String[] args) {
        if (args.length < 3) {
            return null;
        }

        final CategoryArgumentMatch match = findCategoryArgumentMatch(args, 1);
        if (match == null) {
            return null;
        }

        final int remaining = args.length - match.endExclusive();

        if (remaining != 1 && remaining != 2) {
            return null;
        }

        final Integer slot = parsePositiveInteger(args[match.endExclusive()]);
        if (slot == null) {
            return null;
        }

        if (remaining == 1) {
            return new AddItemArguments(match.categoryName(), match.categoryKey(), slot, null);
        }

        final Double price = parsePositiveAmount(args[match.endExclusive() + 1]);
        if (price == null || !isValidPrice(price)) {
            return null;
        }

        return new AddItemArguments(match.categoryName(), match.categoryKey(), slot, price);
    }

    private RenameCategoryArguments parseRenameCategoryArguments(final String[] args) {
        if (args.length < 3) {
            return null;
        }

        final CategoryArgumentMatch match = findCategoryArgumentMatch(args, 1);
        if (match == null) {
            return null;
        }

        final String newName = joinArgs(args, match.endExclusive(), args.length).trim();
        if (newName.isEmpty()) {
            return null;
        }

        return new RenameCategoryArguments(
                match.categoryName(),
                match.categoryKey(),
                newName
        );
    }

    private CategorySlotPriceArguments parseCategorySlotPriceArguments(final String[] args) {
        if (args.length < 4) {
            return null;
        }

        final CategoryArgumentMatch match = findCategoryArgumentMatch(args, 1);
        if (match == null) {
            return null;
        }

        final int remaining = args.length - match.endExclusive();
        if (remaining != 2) {
            return null;
        }

        final Integer slot = parsePositiveInteger(args[match.endExclusive()]);
        final Double price = parsePositiveAmount(args[match.endExclusive() + 1]);

        if (slot == null || price == null || !isValidPrice(price)) {
            return null;
        }

        return new CategorySlotPriceArguments(match.categoryName(), match.categoryKey(), slot, price);
    }

    private CategoryHeldPriceArguments parseCategoryHeldPriceArguments(final String[] args) {
        if (args.length < 3) {
            return null;
        }

        final CategoryArgumentMatch match = findCategoryArgumentMatch(args, 1);
        if (match == null) {
            return null;
        }

        final int remaining = args.length - match.endExclusive();
        if (remaining != 1) {
            return null;
        }

        final Double price = parsePositiveAmount(args[match.endExclusive()]);
        if (price == null || !isValidPrice(price)) {
            return null;
        }

        return new CategoryHeldPriceArguments(match.categoryName(), match.categoryKey(), price);
    }

    private CategoryArgumentMatch findCategoryArgumentMatch(final String[] args, final int startInclusive) {
        final ConfigurationSection categories = getCategoriesSection();

        if (categories == null || args == null || startInclusive >= args.length) {
            return null;
        }

        CategoryArgumentMatch best = null;

        for (int endExclusive = startInclusive + 1; endExclusive <= args.length; endExclusive++) {
            final String possibleName = joinArgs(args, startInclusive, endExclusive).trim();

            if (possibleName.isEmpty()) {
                continue;
            }

            final String key = findCategoryKeyByName(possibleName);
            if (key == null) {
                continue;
            }

            best = new CategoryArgumentMatch(possibleName, key, endExclusive);
        }

        return best;
    }

    private String findMatchingItemPathInCategory(final String categoryKey, final ItemStack heldItem) {
        if (categoryKey == null || heldItem == null || heldItem.getType().isAir()) {
            return null;
        }

        final String itemsPath = SHOP_ROOT + "." + categoryKey + ".items";
        final ConfigurationSection items = plugin.getConfig().getConfigurationSection(itemsPath);

        if (items == null) {
            return null;
        }

        for (final String itemKey : items.getKeys(false)) {
            final String itemPath = itemsPath + "." + itemKey;

            if (matchesStoredShopItem(itemPath, heldItem)) {
                return itemPath;
            }
        }

        return null;
    }

    private boolean saveAndRefreshShopConfig(final CommandSender sender) {
        try {
            plugin.saveAndRefreshShopConfig();
            return true;
        } catch (final Exception exception) {
            sendError(sender, "Failed to save or refresh the shop config. Check console logs.");
            plugin.getLogger().log(Level.SEVERE, "Failed to save or refresh shop config.", exception);
            return false;
        }
    }

    private void ensureShopRoot() {
        final FileConfiguration config = plugin.getConfig();

        if (!config.isConfigurationSection("shop")) {
            config.createSection("shop");
        }

        if (!config.isConfigurationSection(SHOP_ROOT)) {
            config.createSection(SHOP_ROOT);
        }
    }

    private void writeShopItem(final String itemPath, final ItemStack stack, final double price) {
        plugin.getConfig().set(itemPath + ".material", stack.getType().name());
        plugin.getConfig().set(itemPath + ".amount", Math.max(1, Math.min(stack.getType().getMaxStackSize(), stack.getAmount())));
        plugin.getConfig().set(itemPath + ".price", price);

        final ItemMeta meta = stack.getItemMeta();
        final String originalDisplayName = extractOriginalDisplayName(meta);
        final List<String> originalLore = extractOriginalLore(meta);

        plugin.getConfig().set(itemPath + ".original-display-name", originalDisplayName);
        plugin.getConfig().set(itemPath + ".original-lore", originalLore.isEmpty() ? null : new ArrayList<>(originalLore));

        plugin.getConfig().set(itemPath + ".display-name", buildDecoratedItemName(stack, originalDisplayName));
        plugin.getConfig().set(itemPath + ".lore", buildDecoratedItemLore(stack, originalLore, price));
    }

    private void refreshDecoratedItemPresentation(final String itemPath) {
        final Material material = Material.matchMaterial(plugin.getConfig().getString(itemPath + ".material", ""));
        final double price = plugin.getConfig().getDouble(itemPath + ".price", 0D);
        final String originalName = plugin.getConfig().getString(itemPath + ".original-display-name");
        final List<String> originalLore = plugin.getConfig().getStringList(itemPath + ".original-lore");

        plugin.getConfig().set(itemPath + ".display-name", buildDecoratedItemName(material, originalName));
        plugin.getConfig().set(itemPath + ".lore", buildDecoratedItemLore(material, originalLore, price));
    }

    private boolean matchesStoredShopItem(final String itemPath, final ItemStack heldItem) {
        final Material storedMaterial = Material.matchMaterial(plugin.getConfig().getString(itemPath + ".material", ""));

        if (storedMaterial != heldItem.getType()) {
            return false;
        }

        final ItemMeta heldMeta = heldItem.getItemMeta();

        final String heldName = heldMeta != null && heldMeta.hasDisplayName()
                ? heldMeta.getDisplayName()
                : null;

        final List<String> heldLore = heldMeta != null && heldMeta.hasLore() && heldMeta.getLore() != null
                ? new ArrayList<>(heldMeta.getLore())
                : Collections.emptyList();

        final String storedOriginalName = plugin.getConfig().getString(itemPath + ".original-display-name");
        final List<String> storedOriginalLore = plugin.getConfig().getStringList(itemPath + ".original-lore");

        final List<String> effectiveStoredLore = storedOriginalLore.isEmpty()
                ? Collections.emptyList()
                : storedOriginalLore;

        final boolean nameMatches =
                (storedOriginalName == null && heldName == null)
                        || (storedOriginalName != null && storedOriginalName.equals(heldName));

        final boolean loreMatches = effectiveStoredLore.equals(heldLore);

        return nameMatches && loreMatches;
    }

    private TargetAndAmount parseTargetAndAmount(
            final CommandSender sender,
            final String[] args,
            final String usage,
            final String example
    ) {
        if (args.length != 2 && args.length != 3) {
            sendCompactUsage(sender, usage, example);
            return null;
        }

        final OfflinePlayer target;
        final String amountRaw;

        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                sendError(sender, "Console must specify a player.");
                return null;
            }

            target = player;
            amountRaw = args[1];
        } else {
            target = Bukkit.getOfflinePlayer(args[1]);

            if (target.getName() == null) {
                sendError(sender, "That player was not found.");
                return null;
            }

            amountRaw = args[2];
        }

        final Double amount = parsePositiveAmount(amountRaw);

        if (amount == null) {
            sendError(sender, "Money must be a positive number.");
            sendMoneyHint(sender);
            return null;
        }

        return new TargetAndAmount(target, amount);
    }

    @Override
    public List<String> onTabComplete(
            final CommandSender sender,
            final Command command,
            final String alias,
            final String[] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }

        final String subcommand = args[0].toLowerCase(Locale.ROOT);

        return switch (subcommand) {
            case "addmoney", "minusmoney" -> tabCompleteMoneyCommands(args);
            case "addcategory" -> tabCompleteAddCategory(args);
            case "renamecategory" -> tabCompleteRenameCategory(args);
            case "removecategory" -> args.length >= 2 ? categoryNameSuggestions(args, 1) : Collections.emptyList();
            case "additem" -> tabCompleteAddItem(args);
            case "setprice" -> tabCompleteSetPrice(args);
            case "seticoncategory" -> args.length >= 2 ? categoryNameSuggestions(args, 1) : Collections.emptyList();
            case "reload", "removeallcategory", "removeitem" -> Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    private List<String> tabCompleteMoneyCommands(final String[] args) {
        if (args.length == 2) {
            final List<String> combined = new ArrayList<>(onlinePlayerNames(args[1]));
            combined.addAll(filterPrefix(MONEY_SUGGESTIONS, args[1]));
            return unique(combined);
        }

        if (args.length == 3) {
            return filterPrefix(MONEY_SUGGESTIONS, args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> tabCompleteAddCategory(final String[] args) {
        if (args.length <= 2) {
            return Collections.emptyList();
        }

        return filterPrefix(SLOT_SUGGESTIONS, args[args.length - 1]);
    }

    private List<String> tabCompleteRenameCategory(final String[] args) {
        final CategoryArgumentMatch match = findCategoryArgumentMatch(args, 1);

        if (match == null) {
            return args.length >= 2 ? categoryNameSuggestions(args, 1) : Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private List<String> tabCompleteAddItem(final String[] args) {
        final CategoryArgumentMatch match = findCategoryArgumentMatch(args, 1);

        if (match == null) {
            return args.length >= 2 ? categoryNameSuggestions(args, 1) : Collections.emptyList();
        }

        final int remaining = args.length - match.endExclusive();

        if (remaining == 0) {
            return SLOT_SUGGESTIONS;
        }

        if (remaining == 1) {
            return filterPrefix(SLOT_SUGGESTIONS, args[args.length - 1]);
        }

        if (remaining == 2) {
            return filterPrefix(MONEY_SUGGESTIONS, args[args.length - 1]);
        }

        return Collections.emptyList();
    }

    private List<String> tabCompleteSetPrice(final String[] args) {
        final CategoryArgumentMatch match = findCategoryArgumentMatch(args, 1);

        if (match == null) {
            return args.length >= 2 ? categoryNameSuggestions(args, 1) : Collections.emptyList();
        }

        final int remaining = args.length - match.endExclusive();

        if (remaining == 0) {
            final List<String> suggestions = new ArrayList<>(MONEY_SUGGESTIONS);
            suggestions.addAll(SLOT_SUGGESTIONS);
            return unique(suggestions);
        }

        if (remaining == 1) {
            final String current = args[args.length - 1];
            final List<String> suggestions = new ArrayList<>(filterPrefix(MONEY_SUGGESTIONS, current));
            suggestions.addAll(filterPrefix(SLOT_SUGGESTIONS, current));
            return unique(suggestions);
        }

        if (remaining == 2) {
            return filterPrefix(MONEY_SUGGESTIONS, args[args.length - 1]);
        }

        return Collections.emptyList();
    }

    private void sendUsage(final CommandSender sender) {
        final List<String> playerCommands = List.of(
                helpEntry("/bal", "Check your current balance."),
                helpEntry("/baltop", "Open the balance leaderboard."),
                helpEntry("/shop", "Browse the marketplace."),
                helpEntry("/provide <player> <money>", "Send money to another online player."),
                helpEntry("/bounty", "Open the bounty board."),
                helpEntry("/bountyset <player> <money>", "Place a bounty on a player."),
                helpEntry("/ah", "Open the auction house."),
                helpEntry("/ahsell <money>", "List the item in your main hand on the auction house.")
        );

        final List<String> playerExamples = List.of(
                "/bal",
                "/shop",
                "/provide Notch 5K",
                "/bountyset Steve 25K",
                "/ahsell 100K"
        );

        final List<String> adminCommands = List.of(
                helpEntry("/tradable addmoney [player] <money>", "Add money to yourself or another player."),
                helpEntry("/tradable minusmoney [player] <money>", "Remove money from yourself or another player."),
                helpEntry("/tradable addcategory <name> <slot>", "Create a category or move an existing one."),
                helpEntry("/tradable renamecategory <current name> <new name>", "Rename an existing shop category."),
                helpEntry("/tradable removecategory <name>", "Delete a category."),
                helpEntry("/tradable removeallcategory", "Delete every shop category."),
                helpEntry("/tradable additem <category> <slot> <price>", "Add the held item to a shop category slot."),
                helpEntry("/tradable removeitem", "Remove matching shop entries for the held item."),
                helpEntry("/tradable setprice <category> <price>", "Set price by matching the held item."),
                helpEntry("/tradable setprice <category> <slot> <price>", "Set price directly by category slot."),
                helpEntry("/tradable seticoncategory <category>", "Use the held item as the category icon."),
                helpEntry("/tradable reload", "Reload config, messages, and storage files.")
        );

        final List<String> adminExamples = List.of(
                "/tradable addmoney 2.5K",
                "/tradable addmoney Notch 25K",
                "/tradable addcategory Blocks 1",
                "/tradable renamecategory Mob Drops Rare Drops",
                "/tradable additem Mob Drops 1 250",
                "/tradable setprice Mob Drops 2.5K",
                "/tradable reload"
        );

        sender.sendMessage(color(HELP_DIVIDER));
        sender.sendMessage(color(HELP_PREFIX + "&fCommand Overview"));
        sender.sendMessage(color("&7Tradable includes commands for regular players and admin management tools."));
        sender.sendMessage(color(""));

        sendHelpSection(sender, "Available to All Players", playerCommands, playerExamples);
        sender.sendMessage(color("&8&m----------------------------------------"));
        sender.sendMessage(color(""));
        sendHelpSection(sender, "Admin Tools", adminCommands, adminExamples);

        sender.sendMessage(color(HELP_NOTE + "Money supports compact values like &f1.5K&7, &f2M&7, &f3.25B&7."));
        sender.sendMessage(color(HELP_NOTE + "Category names may contain spaces."));
        sender.sendMessage(color(HELP_NOTE + "For shop item commands, hold the target item in your main hand."));
        sender.sendMessage(color(HELP_DIVIDER));
    }

    private void sendHelpSection(
            final CommandSender sender,
            final String title,
            final List<String> entries,
            final List<String> examples
    ) {
        sender.sendMessage(color(HELP_SECTION + title));

        for (final String entry : entries) {
            sender.sendMessage(color(entry));
        }

        if (examples != null && !examples.isEmpty()) {
            sender.sendMessage(color("&8  &m------------------------------"));
            sender.sendMessage(color("&8  &7Examples:"));

            for (final String example : examples) {
                sender.sendMessage(color(HELP_EXAMPLE + example));
            }
        }

        sender.sendMessage(color(""));
    }

    private String helpEntry(final String syntax, final String description) {
        return HELP_ENTRY + syntax + HELP_DESC + description;
    }

    private Player requirePlayer(final CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }

        sendError(sender, "Only players can use this command.");
        return null;
    }

    private void notifyTarget(final OfflinePlayer target, final CommandSender actor, final String message) {
        if (!target.isOnline() || target.getPlayer() == null || target.getPlayer().equals(actor)) {
            return;
        }

        target.getPlayer().sendMessage(color(message));
    }

    private ConfigurationSection getCategoriesSection() {
        return plugin.getConfig().getConfigurationSection(SHOP_ROOT);
    }

    private void sendCategorySuggestions(final CommandSender sender, final String attemptedName) {
        final List<String> categories = categoryNames("");

        if (categories.isEmpty()) {
            sendInfo(sender, "No categories currently exist.");
            return;
        }

        sendInfo(sender, "Existing categories: " + HIGHLIGHT + String.join(INFO + ", " + HIGHLIGHT, categories));

        final String normalizedAttempt = normalizeCategoryKey(attemptedName);

        for (final String category : categories) {
            final String normalizedCategory = normalizeCategoryKey(category);

            if (normalizedCategory.contains(normalizedAttempt)
                    || normalizedAttempt.contains(normalizedCategory)) {
                sendInfo(sender, "Did you mean: " + HIGHLIGHT + category + INFO + "?");
                return;
            }
        }
    }

    private List<String> onlinePlayerNames(final String prefix) {
        final List<String> names = new ArrayList<>();

        for (final Player online : Bukkit.getOnlinePlayers()) {
            names.add(online.getName());
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        return filterPrefix(names, prefix);
    }

    private List<String> categoryNames(final String prefix) {
        final ConfigurationSection categories = getCategoriesSection();

        if (categories == null) {
            return Collections.emptyList();
        }

        final List<String> names = new ArrayList<>();

        for (final String key : categories.getKeys(false)) {
            names.add(stripColor(getCategoryDisplayName(key)));
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        return filterPrefix(names, prefix);
    }

    private List<String> categoryNameSuggestions(final String[] args, final int startInclusive) {
        final String typed = joinArgs(args, startInclusive, args.length).trim();

        if (typed.isEmpty()) {
            return categoryNames("");
        }

        final List<String> categories = categoryNames("");
        final List<String> matches = new ArrayList<>();
        final String normalizedTyped = normalizeCategoryKey(typed);

        for (final String category : categories) {
            final String normalizedCategory = normalizeCategoryKey(category);

            if (normalizedCategory.startsWith(normalizedTyped)
                    || normalizedCategory.contains(normalizedTyped)
                    || category.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT))) {
                matches.add(category);
            }
        }

        return matches;
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

    private List<String> unique(final List<String> source) {
        final List<String> out = new ArrayList<>();

        for (final String entry : source) {
            if (entry == null || entry.isBlank() || out.contains(entry)) {
                continue;
            }
            out.add(entry);
        }

        return out;
    }

    private String findCategoryKeyByName(final String input) {
        final ConfigurationSection categories = getCategoriesSection();

        if (categories == null || input == null || input.isBlank()) {
            return null;
        }

        final String normalized = normalizeCategoryKey(input);
        final String strippedInput = stripColor(input);
        final String compactInput = strippedInput.replace(" ", "_");

        for (final String key : categories.getKeys(false)) {
            final String displayName = stripColor(getCategoryDisplayName(key));
            final String rawName = plugin.getConfig().getString(SHOP_ROOT + "." + key + ".name", "");
            final String normalizedDisplay = normalizeCategoryKey(displayName);
            final String normalizedRaw = normalizeCategoryKey(rawName);

            if (key.equalsIgnoreCase(normalized)
                    || key.equalsIgnoreCase(compactInput)
                    || displayName.equalsIgnoreCase(strippedInput)
                    || stripColor(rawName).equalsIgnoreCase(strippedInput)
                    || normalizedDisplay.equalsIgnoreCase(normalized)
                    || normalizedRaw.equalsIgnoreCase(normalized)) {
                return key;
            }
        }

        return null;
    }

    private String findCategoryKeyByAbsoluteSlot(final int absoluteSlot) {
        final ConfigurationSection categories = getCategoriesSection();

        if (categories == null) {
            return null;
        }

        for (final String key : categories.getKeys(false)) {
            final int configuredSlot = plugin.getConfig().getInt(SHOP_ROOT + "." + key + ".absolute-slot", -1);

            if (configuredSlot == absoluteSlot) {
                return key;
            }
        }

        return null;
    }

    private String getCategoryDisplayName(final String key) {
        final String path = SHOP_ROOT + "." + key;
        return plugin.getConfig().getString(path + ".display-name", buildCategoryDisplayName(prettify(key)));
    }

    private String createUniqueCategoryKey(final String name) {
        final String base = normalizeCategoryKey(name);
        String candidate = base;
        int index = 2;

        while (plugin.getConfig().isConfigurationSection(SHOP_ROOT + "." + candidate)) {
            candidate = base + "_" + index;
            index++;
        }

        return candidate;
    }

    private String createRenameTargetKey(final String currentKey, final String newName) {
        final String normalized = normalizeCategoryKey(newName);

        if (normalized.equalsIgnoreCase(currentKey)) {
            return currentKey;
        }

        if (!plugin.getConfig().isConfigurationSection(SHOP_ROOT + "." + normalized)) {
            return normalized;
        }

        String candidate = normalized;
        int index = 2;

        while (plugin.getConfig().isConfigurationSection(SHOP_ROOT + "." + candidate)
                && !candidate.equalsIgnoreCase(currentKey)) {
            candidate = normalized + "_" + index;
            index++;
        }

        return candidate;
    }

    private void copySection(final ConfigurationSection source, final String destinationPath) {
        final FileConfiguration config = plugin.getConfig();

        for (final String key : source.getKeys(false)) {
            final String childSourcePath = source.getCurrentPath() + "." + key;
            final String childDestinationPath = destinationPath + "." + key;

            if (source.isConfigurationSection(key)) {
                config.createSection(childDestinationPath);
                final ConfigurationSection childSection = source.getConfigurationSection(key);
                if (childSection != null) {
                    copySection(childSection, childDestinationPath);
                }
                continue;
            }

            config.set(childDestinationPath, config.get(childSourcePath));
        }
    }

    private String normalizeCategoryKey(final String input) {
        final String normalized = stripColor(input)
                .toLowerCase(Locale.ROOT)
                .trim()
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_\\-]", "")
                .replaceAll("_+", "_");

        return normalized.isEmpty() ? "category" : normalized;
    }

    private String prettify(final String input) {
        final String[] parts = input.replace('_', ' ').split(" ");
        final StringBuilder builder = new StringBuilder();

        for (final String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));

            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }

        return builder.toString();
    }

    private String joinArgs(final String[] args, final int startInclusive, final int endExclusive) {
        if (args == null || startInclusive < 0 || endExclusive > args.length || startInclusive >= endExclusive) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();

        for (int i = startInclusive; i < endExclusive; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(args[i]);
        }

        return builder.toString();
    }

    private Integer parsePositiveInteger(final String raw) {
        try {
            final int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (final Exception exception) {
            return null;
        }
    }

    private boolean isValidPrice(final double price) {
        return Double.isFinite(price) && price >= MIN_PRICE;
    }

    private Double parsePositiveAmount(final String raw) {
        try {
            final double value = MoneyUtil.parse(raw);
            return value > 0D ? value : null;
        } catch (final Exception exception) {
            return null;
        }
    }

    private boolean isAir(final ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getType().isAir();
    }

    private String formatMoney(final double amount) {
        return "$" + MoneyUtil.formatCompact(amount);
    }

    private String safeName(final OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    private String stripColor(final String text) {
        return text == null
                ? ""
                : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }

    private String color(final String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void sendCompactUsage(final CommandSender sender, final String usage, final String example) {
        sendError(sender, "Usage: " + HIGHLIGHT + usage);
        if (example != null && !example.isBlank()) {
            sendInfo(sender, "Example: " + HIGHLIGHT + example);
        }
    }

    private void sendMoneyHint(final CommandSender sender) {
        sendInfo(sender, "Compact values work too: " + HIGHLIGHT + "1.5K" + INFO + ", " + HIGHLIGHT + "2M" + INFO + ", " + HIGHLIGHT + "3.25B");
    }

    private void sendSuccess(final CommandSender sender, final String message) {
        sender.sendMessage(color(HELP_PREFIX + POSITIVE + message));
    }

    private void sendError(final CommandSender sender, final String message) {
        sender.sendMessage(color(HELP_PREFIX + NEGATIVE + message));
    }

    private void sendInfo(final CommandSender sender, final String message) {
        sender.sendMessage(color("&8• " + INFO + message));
    }

    private String extractOriginalDisplayName(final ItemMeta meta) {
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }

        return null;
    }

    private List<String> extractOriginalLore(final ItemMeta meta) {
        if (meta != null && meta.hasLore() && meta.getLore() != null && !meta.getLore().isEmpty()) {
            return new ArrayList<>(meta.getLore());
        }

        return Collections.emptyList();
    }

    private String buildCategoryDisplayName(final String categoryName) {
        return "&6&l" + stripColor(categoryName);
    }

    private List<String> buildCategoryLore(final String categoryName) {
        final List<String> lore = new ArrayList<>();
        lore.add(BORDER_SOFT);
        lore.add("&7" + CATEGORY_SUBTITLE);
        lore.add("&7Collection: &f" + stripColor(categoryName));
        lore.add("");
        lore.add("&e" + CATEGORY_HINT);
        lore.add("&8" + CATEGORY_FOOTER);
        lore.add(BORDER_SOFT);
        return lore;
    }

    private String buildDecoratedItemName(final ItemStack stack, final String originalDisplayName) {
        return buildDecoratedItemName(stack.getType(), originalDisplayName);
    }

    private String buildDecoratedItemName(final Material material, final String originalDisplayName) {
        final String baseName;

        if (originalDisplayName != null && !stripColor(originalDisplayName).isBlank()) {
            baseName = stripColor(originalDisplayName);
        } else if (material != null) {
            baseName = prettify(material.name());
        } else {
            baseName = "Unknown Item";
        }

        return "&6" + ACCENT + " &f" + baseName + " &6" + ACCENT;
    }

    private List<String> buildDecoratedItemLore(final ItemStack stack, final List<String> originalLore, final double price) {
        return buildDecoratedItemLore(stack.getType(), originalLore, price);
    }

    private List<String> buildDecoratedItemLore(final Material material, final List<String> originalLore, final double price) {
        final List<String> lore = new ArrayList<>();
        lore.add(BORDER_STRONG);

        if (originalLore != null && !originalLore.isEmpty()) {
            lore.addAll(originalLore);
            lore.add("");
        } else {
            lore.add("&7A premium listing from the market.");
            lore.add("&7Item: &f" + prettify(material.name()));
            lore.add("");
        }

        lore.add("&6" + ACCENT + " " + PRICE_TAG + ": &a" + formatMoney(price));
        lore.add("&7Value shown in compact format.");
        lore.add("");
        lore.add("&e" + BUY_HINT);
        lore.add("&8" + ITEM_FOOTER);
        lore.add(BORDER_STRONG);

        return lore;
    }

    private record TargetAndAmount(OfflinePlayer target, double amount) {
    }

    private record AddItemArguments(String categoryName, String categoryKey, int absoluteSlot, Double price) {
    }

    private record RenameCategoryArguments(String currentCategoryName, String currentCategoryKey, String newCategoryName) {
    }

    private record CategorySlotPriceArguments(String categoryName, String categoryKey, int absoluteSlot, double price) {
    }

    private record CategoryHeldPriceArguments(String categoryName, String categoryKey, double price) {
    }

    private record CategoryArgumentMatch(String categoryName, String categoryKey, int endExclusive) {
    }

    private record SlotPosition(int absoluteSlot, int page, int oneBasedSlot) {
        private static SlotPosition fromAbsolute(final int absoluteSlot) {
            final int zeroBased = absoluteSlot - 1;
            final int page = (zeroBased / CONTENT_PAGE_SIZE) + 1;
            final int slot = (zeroBased % CONTENT_PAGE_SIZE) + 1;

            return new SlotPosition(absoluteSlot, page, slot);
        }
    }
}