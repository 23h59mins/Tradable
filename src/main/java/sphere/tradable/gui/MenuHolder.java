// File: src/main/java/sphere/tradable/gui/MenuHolder.java
package sphere.tradable.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

public final class MenuHolder implements InventoryHolder {

    private final BaseMenu menu;

    public MenuHolder(final BaseMenu menu) {
        this.menu = Objects.requireNonNull(menu, "menu");
    }

    public BaseMenu menu() {
        return menu;
    }

    public BaseMenu getMenu() {
        return menu;
    }

    @Override
    public Inventory getInventory() {
        return menu.getInventory();
    }
}