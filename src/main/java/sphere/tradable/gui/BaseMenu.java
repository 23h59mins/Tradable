// src/main/java/sphere/tradable/gui/BaseMenu.java
package sphere.tradable.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import sphere.tradable.Tradable;

import java.util.List;
import java.util.UUID;

public abstract class BaseMenu {

    public static final int ROWS = 6;
    public static final int SIZE = ROWS * 9;

    /*
     * Content uses the full top 5 rows.
     *
     * Command slot 1  -> raw slot 0, top-left
     * Command slot 9  -> raw slot 8, top-right
     * Command slot 10 -> raw slot 9, second row left
     * Command slot 45 -> raw slot 44, fifth row right
     * Command slot 45 -> raw slot 44, fifth row right
     *
     * Bottom row raw slots 45-53 are reserved for controls.
     */
    public static final int[] CONTENT_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    public static final int PAGE_SIZE = CONTENT_SLOTS.length;

    private static final String PREVIOUS_NAME = "&6&l← Previous Page";
    private static final String NEXT_NAME = "&6&lNext Page →";
    private static final String SORT_NAME = "&6&lBrowse Options";

    protected final Tradable plugin;
    protected final UUID viewerId;
    protected final String title;
    protected final MenuHolder holder;

    protected Inventory inventory;
    protected int page;
    protected GuiSort sort;

    protected BaseMenu(final Tradable plugin, final UUID viewerId, final String title, final GuiSort sort) {
        this.plugin = plugin;
        this.viewerId = viewerId;
        this.title = GuiUtil.color(title);
        this.sort = sort;
        this.page = 0;
        this.holder = new MenuHolder(this);
        this.inventory = Bukkit.createInventory(holder, SIZE, this.title);
    }

    public final Inventory getInventory() {
        return inventory;
    }

    public final UUID getViewerId() {
        return viewerId;
    }

    public final int getPage() {
        return page;
    }

    public final GuiSort getSort() {
        return sort;
    }

    public final void open() {
        final Player viewer = Bukkit.getPlayer(viewerId);

        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        redraw();
        viewer.openInventory(inventory);
    }

    public final void open(final Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        if (!viewer.getUniqueId().equals(viewerId)) {
            return;
        }

        redraw();
        viewer.openInventory(inventory);
    }

    public final void redraw() {
        inventory.clear();
        drawFrame();
        drawContent();
        drawControls();
    }

    /*
     * No filler panes.
     * Empty slots remain empty.
     */
    protected void drawFrame() {
    }

    protected void drawControls() {
        final boolean hasPrevious = page > 0;
        final boolean hasNext = hasNextPage();

        setControl(
                45,
                hasPrevious ? Material.ARROW : Material.AIR,
                hasPrevious ? PREVIOUS_NAME : "",
                hasPrevious ? List.of(
                        "&7Return to the previous page.",
                        "&8Page " + page
                ) : List.of()
        );

        setControl(
                49,
                Material.COMPASS,
                SORT_NAME,
                List.of(
                        "&7Current Order: &f" + (sort == null ? "None" : sort.displayName()),
                        "",
                        "&eClick to cycle sorting modes.",
                        "&8Find exactly what you want faster."
                )
        );

        setControl(
                53,
                hasNext ? Material.ARROW : Material.AIR,
                hasNext ? NEXT_NAME : "",
                hasNext ? List.of(
                        "&7Continue to the next page.",
                        "&8Page " + (page + 2)
                ) : List.of()
        );
    }

    protected final void setControl(final int slot, final Material material, final String name, final List<String> lore) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (material == null || material == Material.AIR) {
            inventory.setItem(slot, null);
            return;
        }

        inventory.setItem(slot, GuiUtil.item(material, name, lore == null ? List.of() : lore));
    }

    protected final boolean isContentSlot(final int slot) {
        for (final int contentSlot : CONTENT_SLOTS) {
            if (contentSlot == slot) {
                return true;
            }
        }

        return false;
    }

    protected final boolean isReservedControlSlot(final int slot) {
        return slot >= 45 && slot <= 53;
    }

    protected final int indexOfContentSlot(final int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                return i;
            }
        }

        return -1;
    }

    public void handleClick(final Player player, final InventoryClickEvent event) {
        if (player == null || event == null) {
            return;
        }

        if (!viewerId.equals(player.getUniqueId())) {
            GuiUtil.fail(player, "&cThat menu belongs to another player.");
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
            case 49 -> {
                cycleSort();
                page = 0;
                redraw();
                GuiUtil.click(player);
            }
            case 53 -> {
                if (hasNextPage()) {
                    page++;
                    redraw();
                    GuiUtil.click(player);
                } else {
                    GuiUtil.failSound(player);
                }
            }
            default -> handleCustomClick(player, event);
        }
    }

    protected void handleCustomClick(final Player player, final InventoryClickEvent event) {
    }

    public void onClose(final Player player) {
    }

    protected abstract void drawContent();

    protected abstract boolean hasNextPage();

    protected abstract void cycleSort();
}