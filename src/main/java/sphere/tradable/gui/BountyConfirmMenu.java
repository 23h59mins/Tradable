package sphere.tradable.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import sphere.tradable.Tradable;
import sphere.tradable.util.MoneyUtil;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class BountyConfirmMenu implements ActionMenu {
    private static final int SIZE = 27;

    private static final int SLOT_HEADER = 4;
    private static final int SLOT_SETTER = 10;
    private static final int SLOT_AMOUNT = 13;
    private static final int SLOT_TARGET = 16;
    private static final int SLOT_CONFIRM = 21;
    private static final int SLOT_CANCEL = 23;

    private final Tradable plugin;
    private final UUID setterId;
    private final UUID targetId;
    private final double amount;

    private final ActionMenuHolder holder;
    private final Inventory inventory;

    public BountyConfirmMenu(
            final Tradable plugin,
            final Player setter,
            final OfflinePlayer target,
            final double amount
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.setterId = Objects.requireNonNull(setter, "setter").getUniqueId();
        this.targetId = Objects.requireNonNull(target, "target").getUniqueId();
        this.amount = MoneyUtil.requirePositive(amount, "amount");
        this.holder = new ActionMenuHolder(this);
        this.inventory = Bukkit.createInventory(holder, SIZE, GuiUtil.color("&4&lConfirm Bounty"));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(final Player viewer) {
        if (viewer == null || !viewer.isOnline() || !viewer.getUniqueId().equals(setterId)) {
            return;
        }

        redraw();
        viewer.openInventory(inventory);
    }

    public void redraw() {
        inventory.clear();
        drawFrame();
        drawContent();
    }

    @Override
    public void handleClick(final Player player, final InventoryClickEvent event) {
        if (player == null || event == null) {
            return;
        }

        if (!setterId.equals(player.getUniqueId())) {
            GuiUtil.fail(player, "That confirmation menu is not yours.");
            player.closeInventory();
            return;
        }

        final int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) {
            return;
        }

        switch (rawSlot) {
            case SLOT_CONFIRM -> confirm(player);
            case SLOT_CANCEL -> {
                GuiUtil.click(player);
                player.closeInventory();
                GuiUtil.fail(player, "Bounty placement cancelled.");
            }
            default -> GuiUtil.failSound(player);
        }
    }

    private void drawFrame() {
        final int[] fillerSlots = {
                0, 1, 2, 3, 5, 6, 7, 8,
                9, 11, 12, 14, 15, 17,
                18, 19, 20, 22, 24, 25, 26
        };

        for (final int slot : fillerSlots) {
            inventory.setItem(slot, GuiUtil.item(Material.GRAY_STAINED_GLASS_PANE, "&8", List.of()));
        }
    }

    private void drawContent() {
        final OfflinePlayer setter = Bukkit.getOfflinePlayer(setterId);
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);

        final double balance = plugin.getEconomyService().getBalance(setterId);
        final double remaining = balance - amount;
        final boolean affordable = remaining >= 0D;

        inventory.setItem(
                SLOT_HEADER,
                GuiUtil.item(
                        Material.BOOK,
                        "&f&lBounty Confirmation",
                        List.of(
                                "&7Review this bounty before placing it.",
                                "",
                                "&8A clear final check before spending."
                        )
                )
        );

        inventory.setItem(
                SLOT_SETTER,
                GuiUtil.head(
                        setter,
                        "&6&lPlaced By",
                        List.of(
                                "&7Setter: &f" + safeName(setter),
                                "&7Current Balance: &a" + formatMoney(balance),
                                "&7After Placement: " + (affordable ? "&a" : "&c") + formatMoney(Math.max(remaining, 0D))
                        )
                )
        );

        inventory.setItem(
                SLOT_AMOUNT,
                GuiUtil.item(
                        Material.GOLD_INGOT,
                        "&e&lBounty Value",
                        List.of(
                                "&7Amount: &6" + formatMoney(amount),
                                "",
                                affordable
                                        ? "&8You can afford this bounty."
                                        : "&cYou do not have enough funds."
                        )
                )
        );

        inventory.setItem(
                SLOT_TARGET,
                GuiUtil.head(
                        target,
                        "&6&lTarget",
                        List.of(
                                "&7Player: &f" + safeName(target),
                                "",
                                target.isOnline()
                                        ? "&aCurrently online."
                                        : "&8Currently offline."
                        )
                )
        );

        inventory.setItem(
                SLOT_CONFIRM,
                GuiUtil.item(
                        affordable ? Material.EMERALD : Material.REDSTONE,
                        affordable ? "&a&lConfirm Bounty" : "&c&lCannot Confirm",
                        List.of(
                                "&7Place a bounty of &6" + formatMoney(amount),
                                "&7on &f" + safeName(target),
                                "",
                                affordable
                                        ? "&aClick to place this bounty."
                                        : "&cYou need more funds first."
                        )
                )
        );

        inventory.setItem(
                SLOT_CANCEL,
                GuiUtil.item(
                        Material.BARRIER,
                        "&c&lCancel",
                        List.of(
                                "&7Close this menu",
                                "&7without placing the bounty."
                        )
                )
        );
    }

    private void confirm(final Player setter) {
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);

        if (target.getUniqueId().equals(setter.getUniqueId())) {
            GuiUtil.fail(setter, "You cannot place a bounty on yourself.");
            GuiUtil.failSound(setter);
            setter.closeInventory();
            return;
        }

        final double balance = plugin.getEconomyService().getBalance(setter);
        if (balance < amount) {
            GuiUtil.fail(setter, "You do not have enough funds to place that bounty.");
            GuiUtil.failSound(setter);
            redraw();
            return;
        }

        if (!plugin.getEconomyService().withdraw(setter, amount)) {
            GuiUtil.fail(setter, "Bounty placement failed while withdrawing your funds.");
            GuiUtil.failSound(setter);
            redraw();
            return;
        }

        try {
            plugin.getBountyService().addBounty(setter, target, amount);
        } catch (final IllegalArgumentException exception) {
            plugin.getEconomyService().deposit(setter, amount);
            GuiUtil.fail(setter, "Bounty creation failed: " + exception.getMessage());
            GuiUtil.failSound(setter);
            redraw();
            return;
        }

        GuiUtil.ok(setter, "Placed a bounty of " + formatMoney(amount) + " on " + safeName(target) + ".");
        playSuccess(setter);

        if (target.isOnline() && target.getPlayer() != null) {
            GuiUtil.fail(target.getPlayer(), "A bounty has been placed on you.");
            GuiUtil.ok(target.getPlayer(), "Bounty value: " + formatMoney(amount) + ".");
            playSuccess(target.getPlayer());
        }

        setter.closeInventory();
    }

    private void playSuccess(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.05F);
    }

    private String safeName(final OfflinePlayer player) {
        return player != null && player.getName() != null ? player.getName() : "Unknown";
    }

    private String formatMoney(final double value) {
        return "$" + MoneyUtil.formatCompact(value);
    }
}