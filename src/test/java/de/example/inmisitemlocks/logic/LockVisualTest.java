package de.example.inmisitemlocks.logic;

import static de.example.inmisitemlocks.logic.LockVisual.Icon.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LockVisualTest {

    // locked, hasItem, hovered, bypass, toggle
    @Test
    void unlockedSlot_neverShowsAnything() {
        for (boolean item : new boolean[]{false, true})
            for (boolean hover : new boolean[]{false, true})
                for (boolean bypass : new boolean[]{false, true})
                    for (boolean toggle : new boolean[]{false, true})
                        assertEquals(NONE, LockVisual.icon(false, item, hover, bypass, toggle));
    }

    @Test
    void tempLift_hoverLockedSlot_showsOpenLock() {
        assertEquals(UNLOCKED, LockVisual.icon(true, true, true, true, false));
    }

    @Test
    void tempLift_otherLockedSlotsStayClosed() {
        assertEquals(LOCKED, LockVisual.icon(true, true, false, true, false));
    }

    @Test
    void lockedSlotWithItem_showsClosedLock() {
        assertEquals(LOCKED, LockVisual.icon(true, true, false, false, false));
        assertEquals(LOCKED, LockVisual.icon(true, true, true, false, false));
    }

    @Test
    void permLift_hoverLockedSlot_showsLiftedLock() {
        assertEquals(LIFTED_LOCKED, LockVisual.icon(true, true, true, false, true));
    }

    @Test
    void emptyLockedSlot_showsOpenLock() {
        assertEquals(UNLOCKED, LockVisual.icon(true, false, false, false, false));
        assertEquals(LIFTED_UNLOCKED, LockVisual.icon(true, false, true, false, true));
    }
}
