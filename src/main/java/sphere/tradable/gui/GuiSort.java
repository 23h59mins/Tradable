package sphere.tradable.gui;

public enum GuiSort {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    PRICE_DESC("Highest Price"),
    PRICE_ASC("Lowest Price"),
    BOUNTY_DESC("Highest Bounty"),
    BOUNTY_ASC("Lowest Bounty"),
    BALANCE_DESC("Highest Balance"),
    BALANCE_ASC("Lowest Balance");

    private final String displayName;

    GuiSort(final String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String label() {
        return displayName;
    }
}