package sphere.tradable.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import sphere.tradable.Tradable;

import java.util.ArrayList;
import java.util.List;

public final class ShopConfirmMenu extends BaseMenu {
    private static final String LABEL = "&8";
    private static final String VALUE = "&f";
    private static final String ACCENT = "&6";
    private static final String ACTION = "&e";
    private static final String SUCCESS = "&a";
    private static final String WARNING = "&c";
    private static final String DIVIDER = "&8&m------------------------";

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 64;
    private static final double MIN_PRICE = 0.000001D;

    private static final int SLOT_PREVIEW = 13;

    private static final int SLOT_MINUS_TEN = 20;
    private static final int SLOT_MINUS_ONE = 21;
    private static final int SLOT_QUANTITY = 22;
    private static final int SLOT_PLUS_ONE = 23;
    private static final int SLOT_PLUS_TEN = 24;

    private static final int SLOT_TOTAL = 31;
    private static final int SLOT_BALANCE = 32;

    private static final int SLOT_BACK = 48;
    private static final int SLOT_CANCEL = 49;
    private static final int SLOT_CONFIRM = 50;

    private final ShopMenu.ShopEntry entry;
    private final String categoryId;
    private final int returnPage;
    private final GuiSort returnSort;

    private int quantity = 1;

    public ShopConfirmMenu(
            final Tradable plugin,
            final Player viewer,
            final ShopMenu.ShopEntry entry,
            final String categoryId,
            final int returnPage,
            final GuiSort returnSort
    ) {
        super(plugin, viewer.getUniqueId(), "&6&lConfirm Order", GuiSort.PRICE_ASC);
        this.entry = entry;
        this.categoryId = categoryId;
        this.returnPage = Math.max(0, returnPage);
        this.returnSort = returnSort == null ? GuiSort.PRICE_ASC : returnSort;
    }

    @Override
    protected void drawFrame() {
    }

    @Override
    protected void drawControls() {
        setControl(
                SLOT_BACK,
                Material.OAK_DOOR,
                "&6&lBack to Shop",
                List.of(
                        "&7Return to the item listing.",
                        "&8Keep browsing available goods."
                )
        );

        setControl(
                SLOT_CANCEL,
                Material.BARRIER,
                "&c&lCancel Order",
                List.of(
                        "&7Close this purchase menu.",
                        "&8No items or money will be used."
                )
        );

        final boolean purchasable = isPurchasable();
        final boolean affordable = canAfford();

        setControl(
                SLOT_CONFIRM,
                purchasable && affordable ? Material.EMERALD : Material.REDSTONE,
                purchasable && affordable ? "&a&lComplete Purchase" : "&c&lPurchase Unavailable",
                List.of(
                        DIVIDER,
                        LABEL + "Order Total " + ACCENT + GuiUtil.money(safeTotalPrice()),
                        LABEL + "Quantity " + VALUE + quantity + " bundle" + (quantity == 1 ? "" : "s"),
                        "",
                        !purchasable
                                ? "&cThis listing is currently misconfigured."
                                : affordable
                                  ? ACTION + "Click to complete this order."
                                  : "&cYou do not have enough balance.",
                        DIVIDER
                )
        );
    }

    @Override
    protected void drawContent() {
        inventory.setItem(SLOT_PREVIEW, previewItem());
        inventory.setItem(SLOT_MINUS_TEN, adjustButton(-10));
        inventory.setItem(SLOT_MINUS_ONE, adjustButton(-1));
        inventory.setItem(SLOT_QUANTITY, quantityDisplay());
        inventory.setItem(SLOT_PLUS_ONE, adjustButton(1));
        inventory.setItem(SLOT_PLUS_TEN, adjustButton(10));
        inventory.setItem(SLOT_TOTAL, totalDisplay());
        inventory.setItem(SLOT_BALANCE, balanceDisplay());
    }

    @Override
    protected boolean hasNextPage() {
        return false;
    }

    @Override
    protected void cycleSort() {
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
            case SLOT_MINUS_TEN -> adjustQuantity(player, -10);
            case SLOT_MINUS_ONE -> adjustQuantity(player, -1);
            case SLOT_PLUS_ONE -> adjustQuantity(player, 1);
            case SLOT_PLUS_TEN -> adjustQuantity(player, 10);
            case SLOT_BACK -> {
                GuiUtil.click(player);
                reopenShop(player);
            }
            case SLOT_CANCEL -> {
                GuiUtil.click(player);
                player.closeInventory();
            }
            case SLOT_CONFIRM -> confirmPurchase(player);
            default -> GuiUtil.failSound(player);
        }
    }

    private void adjustQuantity(final Player player, final int delta) {
        final int next = Math.max(MIN_QUANTITY, Math.min(MAX_QUANTITY, quantity + delta));

        if (next == quantity) {
            GuiUtil.failSound(player);
            return;
        }

        quantity = next;
        redraw();
        GuiUtil.click(player);
    }

    private void confirmPurchase(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        final String validationError = validatePurchase();

        if (validationError != null) {
            GuiUtil.fail(player, validationError);
            GuiUtil.failSound(player);
            redraw();
            return;
        }

        final double totalPrice = safeTotalPrice();
        final double balance = GuiAccess.balanceOf(plugin, player.getUniqueId());

        if (!Double.isFinite(balance) || balance < totalPrice) {
            GuiUtil.fail(
                    player,
                    "You need " + GuiUtil.money(totalPrice) +
                            ", but only have " + GuiUtil.money(Math.max(0D, balance)) + "."
            );
            GuiUtil.failSound(player);
            redraw();
            return;
        }

        final List<ItemStack> delivery = splitStacks(entry.item(), quantity);

        if (delivery.isEmpty() || !canFitAll(player.getInventory(), delivery)) {
            GuiUtil.fail(player, "You do not have enough inventory space for this order.");
            GuiUtil.failSound(player);
            return;
        }

        final boolean taken = GuiAccess.withdraw(plugin, player.getUniqueId(), totalPrice);

        if (!taken) {
            final double currentBalance = GuiAccess.balanceOf(plugin, player.getUniqueId());

            if (Double.isFinite(currentBalance) && currentBalance < totalPrice) {
                GuiUtil.fail(
                        player,
                        "You need " + GuiUtil.money(totalPrice) +
                                ", but only have " + GuiUtil.money(Math.max(0D, currentBalance)) + "."
                );
            } else {
                GuiUtil.fail(player, "The purchase could not be completed. Please contact an admin.");
                plugin.getLogger().warning(
                        "Shop purchase failed: economy withdraw returned false for " +
                                player.getName() +
                                " (" + player.getUniqueId() + "), amount=" + totalPrice +
                                ", balance=" + currentBalance +
                                ", item=" + ShopMenu.readable(entry.item()) +
                                ", shopEntryId=" + entry.id()
                );
            }

            GuiUtil.failSound(player);
            redraw();
            return;
        }

        for (final ItemStack stack : delivery) {
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                player.getInventory().addItem(stack.clone());
            }
        }

        GuiUtil.ok(
                player,
                "Purchased " + totalItemCount() + "x " +
                        ShopMenu.readable(entry.item()) +
                        " for " + GuiUtil.money(totalPrice) + "."
        );
        GuiUtil.successSound(player);

        plugin.getServer().getScheduler().runTask(plugin, () -> reopenShop(player));
    }

    private ItemStack previewItem() {
        if (entry == null || entry.item() == null) {
            return GuiUtil.item(
                    Material.BARRIER,
                    "&c&lUnavailable Listing",
                    List.of(
                            "&7This shop item could not be loaded.",
                            "&8Please contact an admin if this persists."
                    )
            );
        }

        final ItemStack display = entry.item().clone();
        final ItemMeta meta = display.getItemMeta();

        if (meta != null) {
            final List<String> lore = new ArrayList<>();

            if (meta.hasLore() && meta.getLore() != null && !meta.getLore().isEmpty()) {
                lore.addAll(meta.getLore());
                lore.add("");
            }

            lore.add(DIVIDER);
            lore.add("&6&lListing Preview");
            lore.add(LABEL + "Bundle Size " + VALUE + ShopMenu.formatBundle(entry.item()));
            lore.add(LABEL + "Price Per Bundle " + ACCENT + GuiUtil.money(entry.price()));
            lore.add("");
            lore.add(ACTION + "Adjust your order using the controls below.");
            lore.add(DIVIDER);

            meta.setLore(GuiUtil.colorLines(lore));
            display.setItemMeta(meta);
        }

        return display;
    }

    private ItemStack quantityDisplay() {
        return GuiUtil.item(
                Material.PAPER,
                "&6&lOrder Quantity",
                List.of(
                        DIVIDER,
                        LABEL + "Bundles " + VALUE + quantity,
                        LABEL + "Total Items " + VALUE + totalItemCount(),
                        "",
                        "&7Use the buttons on each side to adjust your order.",
                        DIVIDER
                )
        );
    }

    private ItemStack totalDisplay() {
        return GuiUtil.item(
                Material.GOLD_INGOT,
                "&6&lOrder Summary",
                List.of(
                        DIVIDER,
                        LABEL + "Item " + VALUE + (entry == null ? "Unknown Listing" : ShopMenu.readable(entry.item())),
                        LABEL + "Items Per Bundle " + VALUE + (entry == null || entry.item() == null ? 0 : entry.item().getAmount()),
                        LABEL + "Bundles " + VALUE + quantity,
                        LABEL + "Total Items " + VALUE + totalItemCount(),
                        LABEL + "Order Total " + ACCENT + GuiUtil.money(safeTotalPrice()),
                        DIVIDER
                )
        );
    }

    private ItemStack balanceDisplay() {
        final double balance = GuiAccess.balanceOf(plugin, viewerId);
        final double remaining = balance - safeTotalPrice();
        final String remainingColor = remaining >= 0D ? SUCCESS : WARNING;

        return GuiUtil.item(
                Material.EMERALD,
                "&6&lBalance Check",
                List.of(
                        DIVIDER,
                        LABEL + "Current Balance " + ACCENT + GuiUtil.money(balance),
                        LABEL + "Balance After Purchase " + remainingColor + GuiUtil.money(remaining),
                        "",
                        remaining >= 0D
                                ? "&aYou can afford this order."
                                : "&cYou cannot afford this order.",
                        DIVIDER
                )
        );
    }

    private ItemStack adjustButton(final int delta) {
        final boolean positive = delta > 0;
        final boolean enabled = positive ? quantity < MAX_QUANTITY : quantity > MIN_QUANTITY;

        final Material material;
        final String name;
        final String lore;

        if (positive) {
            material = enabled ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            name = enabled ? "&a&l+" + delta : "&8&l+" + delta;
            lore = enabled
                    ? "&7Increase by " + delta + " bundle" + (delta == 1 ? "" : "s") + "."
                    : "&7You have reached the maximum quantity.";
        } else {
            final int amount = Math.abs(delta);
            material = enabled ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            name = enabled ? "&c&l-" + amount : "&8&l-" + amount;
            lore = enabled
                    ? "&7Decrease by " + amount + " bundle" + (amount == 1 ? "" : "s") + "."
                    : "&7You are already at the minimum quantity.";
        }

        return GuiUtil.item(material, name, List.of(lore));
    }

    private boolean canAfford() {
        return isPurchasable() && GuiAccess.balanceOf(plugin, viewerId) >= safeTotalPrice();
    }

    private boolean isPurchasable() {
        return validatePurchase() == null;
    }

    private String validatePurchase() {
        if (plugin == null || entry == null || entry.item() == null || entry.item().getType().isAir()) {
            return "This shop listing is currently unavailable.";
        }

        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            return "That quantity is outside the allowed range.";
        }

        if (!Double.isFinite(entry.price()) || entry.price() < MIN_PRICE) {
            return "This listing has an invalid price. Please contact an admin.";
        }

        final double total = entry.price() * quantity;

        if (!Double.isFinite(total) || total < MIN_PRICE) {
            return "This order total is invalid. Please contact an admin.";
        }

        if (entry.item().getAmount() <= 0 || entry.item().getAmount() > entry.item().getMaxStackSize()) {
            return "This listing has an invalid bundle size. Please contact an admin.";
        }

        return null;
    }

    private double safeTotalPrice() {
        final double total = entry == null ? 0D : entry.price() * quantity;
        return Double.isFinite(total) ? Math.max(0D, total) : Double.MAX_VALUE;
    }

    private int totalItemCount() {
        if (entry == null || entry.item() == null) {
            return 0;
        }

        return Math.max(0, entry.item().getAmount()) * Math.max(0, quantity);
    }

    private void reopenShop(final Player player) {
        new ShopMenu(plugin, player, categoryId, returnPage, returnSort).open(player);
    }

    private static List<ItemStack> splitStacks(final ItemStack base, final int bundles) {
        final List<ItemStack> out = new ArrayList<>();

        if (base == null || base.getType().isAir() || base.getAmount() <= 0 || bundles <= 0) {
            return out;
        }

        final int totalAmount = base.getAmount() * bundles;
        final int maxStack = Math.max(1, base.getMaxStackSize());

        int remaining = totalAmount;

        while (remaining > 0) {
            final int amount = Math.min(maxStack, remaining);
            final ItemStack stack = base.clone();
            stack.setAmount(amount);
            out.add(stack);
            remaining -= amount;
        }

        return out;
    }

    private static boolean canFitAll(final PlayerInventory inventory, final List<ItemStack> stacks) {
        if (inventory == null || stacks == null || stacks.isEmpty()) {
            return false;
        }

        final ItemStack[] contents = inventory.getStorageContents().clone();

        for (final ItemStack incomingOriginal : stacks) {
            if (incomingOriginal == null || incomingOriginal.getType().isAir() || incomingOriginal.getAmount() <= 0) {
                continue;
            }

            final ItemStack incoming = incomingOriginal.clone();
            int remaining = incoming.getAmount();

            for (final ItemStack existing : contents) {
                if (existing == null || existing.getType().isAir()) {
                    continue;
                }

                if (!existing.isSimilar(incoming)) {
                    continue;
                }

                final int max = existing.getMaxStackSize();
                final int free = max - existing.getAmount();

                if (free <= 0) {
                    continue;
                }

                final int move = Math.min(free, remaining);
                existing.setAmount(existing.getAmount() + move);
                remaining -= move;

                if (remaining <= 0) {
                    break;
                }
            }

            while (remaining > 0) {
                final int emptySlot = firstEmpty(contents);

                if (emptySlot < 0) {
                    return false;
                }

                final int placed = Math.min(incoming.getMaxStackSize(), remaining);
                final ItemStack clone = incoming.clone();
                clone.setAmount(placed);
                contents[emptySlot] = clone;
                remaining -= placed;
            }
        }

        return true;
    }

    private static int firstEmpty(final ItemStack[] contents) {
        for (int i = 0; i < contents.length; i++) {
            final ItemStack stack = contents[i];

            if (stack == null || stack.getType().isAir()) {
                return i;
            }
        }

        return -1;
    }
}