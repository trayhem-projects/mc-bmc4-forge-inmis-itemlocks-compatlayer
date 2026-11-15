package de.example.inmisitemlocks;

import de.example.inmisitemlocks.logic.LockIds;
import net.minecraft.world.inventory.Slot;

/** Keeps Inmis lock IDs in a range separate from ordinary ItemLocks inventory IDs. */
public final class BackpackLockIds {
    private BackpackLockIds() {}

    public static int forSlot(Slot slot) {
        return LockIds.backpack(slot.getSlotIndex());
    }
}
