package de.example.inmisitemlocks.logic;

/**
 * Pure ID mapping. Player IDs are identical to the ItemLocks mapping for
 * {@code ContainerType.PLAYER_INVENTORY} (hotbar 27..35, main inventory 0..26),
 * so that locks set in the backpack screen match locks in the inventory screen.
 */
public final class LockIds {
    public static final int INMIS_BASE = 1_000_000;

    private LockIds() {}

    public static int player(int inventoryIndex) {
        if (inventoryIndex >= 0 && inventoryIndex <= 8) return inventoryIndex + 27;
        if (inventoryIndex >= 9 && inventoryIndex <= 35) return inventoryIndex - 9;
        return inventoryIndex;
    }

    public static int backpack(int containerIndex) {
        return INMIS_BASE + containerIndex;
    }
}
