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

public final class ProvideConfirmMenu implements ActionMenu {
    private static final int SIZE = 27;

    private static final int SLOT_HEADER = 4;
    private static final int SLOT_SENDER = 10;
    private static final int SLOT_AMOUNT = 13;
    private static final int SLOT_TARGET = 16;
    private static final int SLOT_CONFIRM = 21;
    private static final int SLOT_CANCEL = 23;

    private final Tradable plugin;
    private final UUID senderId;
    private final UUID targetId;
    private final double amount;

    private final ActionMenuHolder holder;
    private final Inventory inventory;

    public ProvideConfirmMenu(
            final Tradable plugin,
            final Player sender,
            final Player target,
            final double amount
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.senderId = Objects.requireNonNull(sender, "sender").getUniqueId();
        this.targetId = Objects.requireNonNull(target, "target").getUniqueId();
        this.amount = MoneyUtil.requirePositive(amount, "amount");
        this.holder = new ActionMenuHolder(this);
        this.inventory = Bukkit.createInventory(holder, SIZE, GuiUtil.color("&2&lConfirm Transfer"));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(final Player viewer) {
        if (viewer == null || !viewer.isOnline() || !viewer.getUniqueId().equals(senderId)) {
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

        if (!senderId.equals(player.getUniqueId())) {
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
                GuiUtil.fail(player, "Transfer cancelled.");
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
        final OfflinePlayer sender = Bukkit.getOfflinePlayer(senderId);
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        final Player liveTarget = Bukkit.getPlayer(targetId);

        final double balance = plugin.getEconomyService().getBalance(senderId);
        final double remaining = balance - amount;
        final boolean affordable = remaining >= 0D;
        final boolean targetOnline = liveTarget != null && liveTarget.isOnline();

        inventory.setItem(
                SLOT_HEADER,
                GuiUtil.item(
                        Material.PAPER,
                        "&f&lTransfer Confirmation",
                        List.of(
                                "&7Review this payment before sending it.",
                                "",
                                "&8Minimal, clear, and safe."
                        )
                )
        );

        inventory.setItem(
                SLOT_SENDER,
                GuiUtil.head(
                        sender,
                        "&6&lFrom",
                        List.of(
                                "&7Sender: &f" + safeName(sender),
                                "&7Current Balance: &a" + formatMoney(balance),
                                "&7After Transfer: " + (affordable ? "&a" : "&c") + formatMoney(Math.max(remaining, 0D))
                        )
                )
        );

        inventory.setItem(
                SLOT_AMOUNT,
                GuiUtil.item(
                        Material.GOLD_INGOT,
                        "&e&lAmount",
                        List.of(
                                "&7Transfer Value: &6" + formatMoney(amount),
                                "",
                                affordable
                                        ? "&8You can afford this payment."
                                        : "&cYou do not have enough funds."
                        )
                )
        );

        inventory.setItem(
                SLOT_TARGET,
                GuiUtil.head(
                        target,
                        "&6&lTo",
                        List.of(
                                "&7Recipient: &f" + safeName(target),
                                "",
                                targetOnline
                                        ? "&aCurrently online and ready to receive."
                                        : "&cRecipient is no longer online."
                        )
                )
        );

        inventory.setItem(
                SLOT_CONFIRM,
                GuiUtil.item(
                        affordable && targetOnline ? Material.EMERALD : Material.REDSTONE,
                        affordable && targetOnline ? "&a&lConfirm Transfer" : "&c&lCannot Confirm",
                        List.of(
                                "&7Send &6" + formatMoney(amount) + " &7to &f" + safeName(target),
                                "",
                                affordable && targetOnline
                                        ? "&aClick to complete this transfer."
                                        : "&cResolve the issue shown above first."
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
                                "&7without sending anything."
                        )
                )
        );
    }

    private void confirm(final Player sender) {
        final Player target = Bukkit.getPlayer(targetId);

        if (target == null || !target.isOnline()) {
            GuiUtil.fail(sender, "That player is no longer online.");
            GuiUtil.failSound(sender);
            sender.closeInventory();
            return;
        }

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            GuiUtil.fail(sender, "You cannot send money to yourself.");
            GuiUtil.failSound(sender);
            sender.closeInventory();
            return;
        }

        final double balance = plugin.getEconomyService().getBalance(sender);
        if (balance < amount) {
            GuiUtil.fail(sender, "You do not have enough funds for that transfer.");
            GuiUtil.failSound(sender);
            redraw();
            return;
        }

        if (!plugin.getEconomyService().withdraw(sender, amount)) {
            GuiUtil.fail(sender, "Transfer failed while withdrawing your funds.");
            GuiUtil.failSound(sender);
            redraw();
            return;
        }

        plugin.getEconomyService().deposit(target, amount);

        GuiUtil.ok(sender, "Sent " + formatMoney(amount) + " to " + target.getName() + ".");
        GuiUtil.ok(target, "You received " + formatMoney(amount) + " from " + sender.getName() + ".");

        playSuccess(sender);
        playSuccess(target);

        sender.closeInventory();
    }

    private void playSuccess(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.1F);
    }

    private String safeName(final OfflinePlayer player) {
        return player != null && player.getName() != null ? player.getName() : "Unknown";
    }

    private String formatMoney(final double value) {
        return "$" + MoneyUtil.formatCompact(value);
    }
}