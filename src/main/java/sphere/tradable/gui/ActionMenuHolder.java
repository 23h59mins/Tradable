package sphere.tradable.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ActionMenuHolder implements InventoryHolder {
    private final ActionMenu menu;

    public ActionMenuHolder(final ActionMenu menu) {
        this.menu = menu;
    }

    public ActionMenu menu() {
        return menu;
    }

    @Override
    public Inventory getInventory() {
        return menu == null ? null : menu.getInventory();
    }
}