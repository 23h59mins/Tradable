package sphere.tradable.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ProvideConfirmHolder implements InventoryHolder {
    private final ProvideConfirmMenu menu;

    public ProvideConfirmHolder(final ProvideConfirmMenu menu) {
        this.menu = menu;
    }

    public ProvideConfirmMenu menu() {
        return menu;
    }

    @Override
    public Inventory getInventory() {
        return menu == null ? null : menu.getInventory();
    }
}