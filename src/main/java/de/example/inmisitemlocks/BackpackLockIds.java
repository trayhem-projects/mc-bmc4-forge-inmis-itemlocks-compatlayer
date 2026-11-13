package de.example.inmisitemlocks;

import net.minecraft.world.inventory.Slot;

/** Keeps Inmis lock IDs in a range separate from ordinary ItemLocks inventory IDs. */
public final class BackpackLockIds {
    private static final int INMIS_BASE = 1_000_000;

    private BackpackLockIds() {}

    public static int forSlot(Slot slot) {
        return INMIS_BASE + slot.getSlotIndex();
    }
}
