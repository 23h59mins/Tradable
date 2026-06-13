package sphere.tradable.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(final InventoryClickEvent event) {
        final Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null) {
            return;
        }

        final InventoryHolder holder = topInventory.getHolder();
        if (!isSupportedHolder(holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
            return;
        }

        if (isBlockedClick(event)) {
            return;
        }

        try {
            if (holder instanceof MenuHolder menuHolder) {
                final BaseMenu menu = menuHolder.menu();
                if (menu != null) {
                    menu.handleClick(player, event);
                }
                return;
            }

            if (holder instanceof ActionMenuHolder actionHolder) {
                final ActionMenu menu = actionHolder.menu();
                if (menu != null) {
                    menu.handleClick(player, event);
                }
            }
        } catch (final Exception exception) {
            GuiUtil.fail(player, "Something went wrong while handling that menu.");
            GuiUtil.failSound(player);
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(final InventoryDragEvent event) {
        final Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null || !isSupportedHolder(topInventory.getHolder())) {
            return;
        }

        for (final int rawSlot : event.getRawSlots()) {
            if (rawSlot < topInventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClose(final InventoryCloseEvent event) {
        final Inventory topInventory = event.getInventory();
        if (topInventory == null) {
            return;
        }

        final InventoryHolder holder = topInventory.getHolder();
        if (!isSupportedHolder(holder)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        try {
            if (holder instanceof MenuHolder menuHolder) {
                final BaseMenu menu = menuHolder.menu();
                if (menu != null) {
                    menu.onClose(player);
                }
                return;
            }

            if (holder instanceof ActionMenuHolder actionHolder) {
                final ActionMenu menu = actionHolder.menu();
                if (menu != null) {
                    menu.onClose(player);
                }
            }
        } catch (final Exception ignored) {
        }
    }

    private boolean isSupportedHolder(final InventoryHolder holder) {
        return holder instanceof MenuHolder || holder instanceof ActionMenuHolder;
    }

    private boolean isBlockedClick(final InventoryClickEvent event) {
        final ClickType click = event.getClick();
        final InventoryAction action = event.getAction();

        return click == ClickType.DOUBLE_CLICK
                || click == ClickType.NUMBER_KEY
                || click == ClickType.SWAP_OFFHAND
                || click == ClickType.CREATIVE
                || click == ClickType.CONTROL_DROP
                || click == ClickType.DROP
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.CLONE_STACK
                || action == InventoryAction.UNKNOWN;
    }
}