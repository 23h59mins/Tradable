package sphere.tradable.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sphere.tradable.Tradable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopMenu extends BaseMenu {
    private static final String LABEL = "&8";
    private static final String VALUE = "&f";
    private static final String ACCENT = "&6";
    private static final String ACTION = "&e";
    private static final String MUTED = "&7";
    private static final String DIVIDER = "&8&m------------------------";

    private static final double MIN_PRICE = 0.000001D;

    private static final Set<String> WARNED_INVALID_PATHS = ConcurrentHashMap.newKeySet();

    private ViewMode viewMode;
    private String selectedCategoryId;

    private final List<Category> categoryCache = new ArrayList<>();
    private final List<ShopEntry> itemCache = new ArrayList<>();

    public ShopMenu(final Tradable plugin, final Player viewer) {
        this(plugin, viewer, null, 0, GuiSort.PRICE_ASC);
    }

    public ShopMenu(
            final Tradable plugin,
            final Player viewer,
            final String selectedCategoryId,
            final int page,
            final GuiSort sort
    ) {
        super(plugin, viewer.getUniqueId(), "&2&lMarketplace", sort == null ? GuiSort.PRICE_ASC : sort);
        this.selectedCategoryId = selectedCategoryId;
        this.viewMode = selectedCategoryId == null ? ViewMode.CATEGORIES : ViewMode.ITEMS;
        this.page = Math.max(0, page);
    }

    @Override
    protected void drawControls() {
        setControl(
                45,
                page > 0 ? Material.ARROW : Material.AIR,
                page > 0 ? "&6&l← Previous Page" : "",
                page > 0 ? List.of("&7Return to the previous page.") : List.of()
        );

        setControl(
                47,
                Material.SUNFLOWER,
                "&a&lRefresh View",
                List.of(
                        "&7Reload the current page.",
                        "&8Useful after config or economy updates."
                )
        );

        if (viewMode == ViewMode.CATEGORIES) {
            final int totalCategories = categories().size();

            setControl(
                    49,
                    Material.CHEST,
                    "&6&lBrowse Categories",
                    List.of(
                            DIVIDER,
                            LABEL + "Available Sections " + VALUE + totalCategories,
                            LABEL + "Page " + VALUE + (page + 1) + "/" + maxCategoryPages(),
                            "",
                            MUTED + "Choose a section to explore the market.",
                            DIVIDER
                    )
            );
        } else {
            setControl(
                    49,
                    Material.HOPPER,
                    "&6&lSort Listings",
                    List.of(
                            DIVIDER,
                            LABEL + "Current Order " + VALUE + sort.label(),
                            LABEL + "Page " + VALUE + (page + 1) + "/" + maxItemPages(),
                            "",
                            ACTION + "Click to switch sorting order.",
                            DIVIDER
                    )
            );
        }

        setControl(
                51,
                hasNextPage() ? Material.ARROW : Material.AIR,
                hasNextPage() ? "&6&lNext Page →" : "",
                hasNextPage() ? List.of("&7Continue to the next page.") : List.of()
        );

        if (viewMode == ViewMode.ITEMS) {
            setControl(
                    48,
                    Material.OAK_DOOR,
                    "&6&lBack to Categories",
                    List.of(
                            "&7Return to the category overview.",
                            "&8Browse a different section."
                    )
            );
        } else {
            setControl(48, Material.AIR, "", List.of());
        }

        final double balance = GuiAccess.balanceOf(plugin, viewerId);
        setControl(
                50,
                Material.GOLD_INGOT,
                "&6&lYour Balance",
                List.of(
                        DIVIDER,
                        LABEL + "Available Funds " + ACCENT + GuiUtil.money(balance),
                        "",
                        MUTED + "Ready for your next purchase.",
                        DIVIDER
                )
        );

        setControl(
                53,
                Material.BARRIER,
                "&c&lClose Menu",
                List.of("&7Close the marketplace.")
        );
    }

    @Override
    protected void drawContent() {
        if (viewMode == ViewMode.CATEGORIES) {
            drawCategories();
        } else {
            drawItems();
        }
    }

    @Override
    protected boolean hasNextPage() {
        if (viewMode == ViewMode.CATEGORIES) {
            return page + 1 < maxCategoryPages();
        }

        return page + 1 < maxItemPages();
    }

    @Override
    protected void cycleSort() {
        if (viewMode == ViewMode.ITEMS) {
            sort = sort == GuiSort.PRICE_ASC ? GuiSort.PRICE_DESC : GuiSort.PRICE_ASC;
        }
    }

    @Override
    public void handleClick(final Player player, final InventoryClickEvent event) {
        if (player == null || event == null) {
            return;
        }

        if (!viewerId.equals(player.getUniqueId())) {
            GuiUtil.fail(player, "That menu belongs to another player.");
            player.closeInventory();
            return;
        }

        final int rawSlot = event.getRawSlot();

        if (rawSlot < 0 || rawSlot >= inventory.getSize()) {
            return;
        }

        switch (rawSlot) {
            case 45 -> {
                if (page > 0) {
                    page--;
                    redraw();
                    GuiUtil.click(player);
                } else {
                    GuiUtil.failSound(player);
                }
            }
            case 47 -> {
                redraw();
                GuiUtil.click(player);
            }
            case 49 -> {
                if (viewMode == ViewMode.ITEMS) {
                    cycleSort();
                    redraw();
                    GuiUtil.click(player);
                } else {
                    GuiUtil.failSound(player);
                }
            }
            case 51 -> {
                if (hasNextPage()) {
                    page++;
                    redraw();
                    GuiUtil.click(player);
                } else {
                    GuiUtil.failSound(player);
                }
            }
            case 53 -> {
                GuiUtil.click(player);
                player.closeInventory();
            }
            default -> handleCustomClick(player, event);
        }
    }

    @Override
    protected void handleCustomClick(final Player player, final InventoryClickEvent event) {
        if (player == null || event == null) {
            return;
        }

        if (viewMode == ViewMode.ITEMS && event.getRawSlot() == 48) {
            selectedCategoryId = null;
            viewMode = ViewMode.CATEGORIES;
            page = 0;
            redraw();
            GuiUtil.click(player);
            return;
        }

        final int clickedSlot = event.getRawSlot();

        if (viewMode == ViewMode.CATEGORIES) {
            final Category category = categoryAtDisplaySlot(clickedSlot);

            if (category == null) {
                return;
            }

            selectedCategoryId = category.id();
            viewMode = ViewMode.ITEMS;
            page = 0;
            redraw();
            GuiUtil.click(player);
            return;
        }

        final ShopEntry entry = itemAtDisplaySlot(clickedSlot);

        if (entry == null) {
            return;
        }

        new ShopConfirmMenu(plugin, player, entry, selectedCategoryId, page, sort).open(player);
        GuiUtil.click(player);
    }

    private void drawCategories() {
        categoryCache.clear();
        categoryCache.addAll(categories());

        final List<Category> pageCategories = categoriesForPage(page);

        if (pageCategories.isEmpty()) {
            inventory.setItem(
                    22,
                    GuiUtil.item(
                            Material.BOOK,
                            "&6&lMarketplace Unavailable",
                            List.of(
                                    "&7No categories are available on this page.",
                                    "&7Add entries under &fshop.categories &7in the config.",
                                    "",
                                    "&8Once categories are configured, they will appear here."
                            )
                    )
            );
            return;
        }

        for (final Category category : pageCategories) {
            final int localSlot = toLocalContentSlot(category.slot());

            if (localSlot < 0 || localSlot >= CONTENT_SLOTS.length) {
                continue;
            }

            inventory.setItem(
                    CONTENT_SLOTS[localSlot],
                    GuiUtil.item(
                            category.icon(),
                            category.displayName(),
                            List.of(
                                    DIVIDER,
                                    LABEL + "Available Listings " + VALUE + category.items().size(),
                                    LABEL + "Category Slot " + VALUE + category.absoluteSlot(),
                                    "",
                                    ACTION + "Click to browse this section.",
                                    DIVIDER
                            )
                    )
            );
        }
    }

    private void drawItems() {
        itemCache.clear();
        itemCache.addAll(items());

        final List<ShopEntry> pageItems = itemsForPage(page);

        if (pageItems.isEmpty()) {
            inventory.setItem(
                    22,
                    GuiUtil.item(
                            Material.CHEST,
                            "&6&lNothing Listed",
                            List.of(
                                    "&7This section does not contain any valid listings on this page.",
                                    "",
                                    "&8Try another category or check back later."
                            )
                    )
            );
            return;
        }

        for (final ShopEntry entry : pageItems) {
            final int localSlot = toLocalContentSlot(entry.slot());

            if (localSlot < 0 || localSlot >= CONTENT_SLOTS.length) {
                continue;
            }

            final ItemStack display = entry.item().clone();
            final ItemMeta meta = display.getItemMeta();

            if (meta != null) {
                final List<String> lore = new ArrayList<>();

                if (meta.hasLore() && meta.getLore() != null && !meta.getLore().isEmpty()) {
                    lore.addAll(meta.getLore());
                    lore.add("");
                }

                lore.add(GuiUtil.color(DIVIDER));
                lore.add(GuiUtil.color("&6&lListing Details"));
                lore.add(GuiUtil.color(LABEL + "Price " + ACCENT + GuiUtil.money(entry.price())));
                lore.add(GuiUtil.color(LABEL + "Bundle " + VALUE + formatBundle(entry.item())));
                lore.add(GuiUtil.color(LABEL + "Listing Slot " + VALUE + entry.absoluteSlot()));
                lore.add("");
                lore.add(GuiUtil.color(ACTION + "Click to continue to checkout."));
                lore.add(GuiUtil.color(DIVIDER));

                meta.setLore(lore);
                display.setItemMeta(meta);
            }

            inventory.setItem(CONTENT_SLOTS[localSlot], display);
        }
    }

    private Category categoryAtDisplaySlot(final int rawSlot) {
        final int local = indexOfContentSlot(rawSlot);

        if (local < 0) {
            return null;
        }

        final int pageSlot = local + 1;

        for (final Category category : categoriesForPage(page)) {
            if (category.slot() == pageSlot) {
                return category;
            }
        }

        return null;
    }

    private ShopEntry itemAtDisplaySlot(final int rawSlot) {
        final int local = indexOfContentSlot(rawSlot);

        if (local < 0) {
            return null;
        }

        final int pageSlot = local + 1;

        for (final ShopEntry entry : itemsForPage(page)) {
            if (entry.slot() == pageSlot) {
                return entry;
            }
        }

        return null;
    }

    private List<Category> categories() {
        final ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop.categories");

        if (section == null) {
            return Collections.emptyList();
        }

        final List<Category> out = new ArrayList<>();
        final Set<Integer> usedSlots = new HashSet<>();
        int fallbackSlot = 1;

        for (final String id : section.getKeys(false)) {
            final ConfigurationSection categorySection = section.getConfigurationSection(id);

            if (categorySection == null) {
                warnOnce(section.getCurrentPath() + "." + id, "Skipping invalid shop category '" + id + "': not a configuration section.");
                continue;
            }

            final Material icon = safeMaterial(
                    categorySection.getString("material", categorySection.getString("icon", "CHEST")),
                    Material.CHEST
            );

            int absoluteSlot = categorySection.getInt("absolute-slot", safeParseInt(id, -1));

            if (absoluteSlot < 1) {
                absoluteSlot = fallbackSlot;
            }

            while (usedSlots.contains(absoluteSlot)) {
                absoluteSlot++;
            }

            usedSlots.add(absoluteSlot);
            fallbackSlot = absoluteSlot + 1;

            final String displayName = categorySection.getString(
                    "display-name",
                    categorySection.getString("name", "&f" + GuiUtil.prettify(id))
            );

            final int derivedPage = pageFromAbsoluteSlot(absoluteSlot);
            final int derivedSlot = slotFromAbsoluteSlot(absoluteSlot);
            final int configuredPage = sanitizePage(categorySection.getInt("page", derivedPage), derivedPage);
            final int configuredSlot = sanitizeSlot(categorySection.getInt("slot", derivedSlot), derivedSlot);
            final List<ShopEntry> items = parseItems(categorySection.getConfigurationSection("items"));

            out.add(new Category(id, icon, displayName, absoluteSlot, configuredPage, configuredSlot, items));
        }

        out.sort(
                Comparator.comparingInt(Category::page)
                        .thenComparingInt(Category::slot)
                        .thenComparingInt(Category::absoluteSlot)
                        .thenComparing(Category::displayName, String.CASE_INSENSITIVE_ORDER)
        );

        return out;
    }

    private List<ShopEntry> items() {
        final Category category = categories().stream()
                .filter(entry -> Objects.equals(entry.id(), selectedCategoryId))
                .findFirst()
                .orElse(null);

        if (category == null) {
            return Collections.emptyList();
        }

        final List<ShopEntry> out = new ArrayList<>(category.items());

        out.sort(
                Comparator.comparingInt(ShopEntry::page)
                        .thenComparingInt(ShopEntry::slot)
                        .thenComparing(
                                sort == GuiSort.PRICE_DESC
                                        ? Comparator.comparingDouble(ShopEntry::price).reversed()
                                        : Comparator.comparingDouble(ShopEntry::price)
                        )
                        .thenComparingInt(ShopEntry::absoluteSlot)
        );

        return out;
    }

    private List<Category> categoriesForPage(final int zeroBasedPage) {
        final int configPage = zeroBasedPage + 1;
        final List<Category> out = new ArrayList<>();

        for (final Category category : categoryCache.isEmpty() ? categories() : categoryCache) {
            if (category.page() == configPage) {
                out.add(category);
            }
        }

        out.sort(Comparator.comparingInt(Category::slot).thenComparingInt(Category::absoluteSlot));
        return out;
    }

    private List<ShopEntry> itemsForPage(final int zeroBasedPage) {
        final int configPage = zeroBasedPage + 1;
        final List<ShopEntry> out = new ArrayList<>();

        for (final ShopEntry entry : itemCache.isEmpty() ? items() : itemCache) {
            if (entry.page() == configPage) {
                out.add(entry);
            }
        }

        out.sort(
                Comparator.comparingInt(ShopEntry::slot)
                        .thenComparing(
                                sort == GuiSort.PRICE_DESC
                                        ? Comparator.comparingDouble(ShopEntry::price).reversed()
                                        : Comparator.comparingDouble(ShopEntry::price)
                        )
                        .thenComparingInt(ShopEntry::absoluteSlot)
        );

        return out;
    }

    private int maxCategoryPages() {
        int max = 1;

        for (final Category category : categories()) {
            max = Math.max(max, category.page());
        }

        return max;
    }

    private int maxItemPages() {
        int max = 1;

        for (final ShopEntry entry : items()) {
            max = Math.max(max, entry.page());
        }

        return max;
    }

    private List<ShopEntry> parseItems(final ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyList();
        }

        final List<ShopEntry> out = new ArrayList<>();
        final Set<Integer> usedSlots = new HashSet<>();
        int fallbackSlot = 1;

        for (final String id : section.getKeys(false)) {
            final ConfigurationSection itemSection = section.getConfigurationSection(id);
            final String path = section.getCurrentPath() + "." + id;

            if (itemSection == null) {
                warnOnce(path, "Skipping invalid shop item '" + id + "': not a configuration section.");
                continue;
            }

            final Material material = parseShopMaterial(itemSection.getString("material", id));

            if (material == null || material.isAir()) {
                warnOnce(path, "Skipping invalid shop item '" + id + "': invalid material.");
                continue;
            }

            final int maxStack = Math.max(1, material.getMaxStackSize());
            final int amount = itemSection.getInt("amount", 1);

            if (amount < 1 || amount > maxStack) {
                warnOnce(path, "Skipping invalid shop item '" + id + "': invalid amount " + amount + " for " + material.name() + ".");
                continue;
            }

            final double price = itemSection.getDouble("price", Double.NaN);

            if (!Double.isFinite(price) || price < MIN_PRICE) {
                warnOnce(path, "Skipping invalid shop item '" + id + "': invalid or missing price.");
                continue;
            }

            int absoluteSlot = itemSection.getInt("absolute-slot", safeParseInt(id, -1));

            if (absoluteSlot < 1) {
                absoluteSlot = fallbackSlot;
            }

            while (usedSlots.contains(absoluteSlot)) {
                absoluteSlot++;
            }

            usedSlots.add(absoluteSlot);
            fallbackSlot = absoluteSlot + 1;

            final int derivedPage = pageFromAbsoluteSlot(absoluteSlot);
            final int derivedSlot = slotFromAbsoluteSlot(absoluteSlot);
            final int configuredPage = sanitizePage(itemSection.getInt("page", derivedPage), derivedPage);
            final int configuredSlot = sanitizeSlot(itemSection.getInt("slot", derivedSlot), derivedSlot);

            final ItemStack stack = new ItemStack(material, amount);
            final ItemMeta meta = stack.getItemMeta();

            if (meta != null) {
                final String originalDisplayName = resolveOriginalDisplayName(itemSection);
                final List<String> originalLore = resolveOriginalLore(itemSection);

                if (originalDisplayName != null && !originalDisplayName.isBlank()) {
                    meta.setDisplayName(GuiUtil.color(originalDisplayName));
                }

                if (!originalLore.isEmpty()) {
                    meta.setLore(GuiUtil.colorLines(cleanLore(originalLore)));
                }

                stack.setItemMeta(meta);
            }

            out.add(new ShopEntry(id, stack, price, absoluteSlot, configuredPage, configuredSlot));
        }

        out.sort(
                Comparator.comparingInt(ShopEntry::page)
                        .thenComparingInt(ShopEntry::slot)
                        .thenComparingInt(ShopEntry::absoluteSlot)
        );

        return out;
    }

    private String resolveOriginalDisplayName(final ConfigurationSection itemSection) {
        final String original = itemSection.getString("original-display-name");
        if (original == null || original.isBlank()) {
            return null;
        }

        return original;
    }

    private List<String> resolveOriginalLore(final ConfigurationSection itemSection) {
        final List<String> originalLore = itemSection.getStringList("original-lore");
        return originalLore.isEmpty() ? Collections.emptyList() : cleanLore(originalLore);
    }

    private Material parseShopMaterial(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        return Material.matchMaterial(raw.trim());
    }

    private Material safeMaterial(final String raw, final Material fallback) {
        final Material material = parseShopMaterial(raw);
        return material == null || material.isAir() ? fallback : material;
    }

    private int sanitizePage(final int configuredPage, final int fallback) {
        return configuredPage > 0 ? configuredPage : Math.max(1, fallback);
    }

    private int sanitizeSlot(final int configuredSlot, final int fallback) {
        return configuredSlot >= 1 && configuredSlot <= PAGE_SIZE
                ? configuredSlot
                : Math.max(1, Math.min(PAGE_SIZE, fallback));
    }

    private void warnOnce(final String key, final String message) {
        if (key == null || key.isBlank()) {
            return;
        }

        if (WARNED_INVALID_PATHS.add(key)) {
            plugin.getLogger().warning(message);
        }
    }

    private int safeParseInt(final String value, final int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (final Exception ignored) {
            return fallback;
        }
    }

    private int pageFromAbsoluteSlot(final int absoluteSlot) {
        return ((Math.max(1, absoluteSlot) - 1) / PAGE_SIZE) + 1;
    }

    private int slotFromAbsoluteSlot(final int absoluteSlot) {
        return ((Math.max(1, absoluteSlot) - 1) % PAGE_SIZE) + 1;
    }

    private int toLocalContentSlot(final int oneBasedPageSlot) {
        return oneBasedPageSlot - 1;
    }

    private List<String> cleanLore(final List<String> lore) {
        final List<String> cleaned = new ArrayList<>();
        boolean previousBlank = false;

        for (final String line : lore) {
            final String value = line == null ? "" : line.trim();
            final boolean blank = value.isEmpty();

            if (blank && previousBlank) {
                continue;
            }

            cleaned.add(value);
            previousBlank = blank;
        }

        while (!cleaned.isEmpty() && cleaned.get(0).isEmpty()) {
            cleaned.remove(0);
        }

        while (!cleaned.isEmpty() && cleaned.get(cleaned.size() - 1).isEmpty()) {
            cleaned.remove(cleaned.size() - 1);
        }

        return cleaned;
    }

    static String readable(final ItemStack stack) {
        if (stack == null) {
            return "Unknown";
        }

        final ItemMeta meta = stack.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return ChatStrip.strip(meta.getDisplayName());
        }

        return GuiUtil.prettify(stack.getType().name());
    }

    static String formatBundle(final ItemStack stack) {
        if (stack == null) {
            return "0x Unknown";
        }

        return stack.getAmount() + "x " + readable(stack);
    }

    private enum ViewMode {
        CATEGORIES,
        ITEMS
    }

    private record Category(
            String id,
            Material icon,
            String displayName,
            int absoluteSlot,
            int page,
            int slot,
            List<ShopEntry> items
    ) {
    }

    public record ShopEntry(
            String id,
            ItemStack item,
            double price,
            int absoluteSlot,
            int page,
            int slot
    ) {
    }
}

final class ChatStrip {
    private ChatStrip() {
    }

    static String strip(final String text) {
        return text == null
                ? ""
                : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }
}