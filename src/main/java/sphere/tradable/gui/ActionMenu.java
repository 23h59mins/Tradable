package sphere.tradable.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface ActionMenu {
    Inventory getInventory();

    void handleClick(Player player, InventoryClickEvent event);

    default void onClose(final Player player) {
    }
}