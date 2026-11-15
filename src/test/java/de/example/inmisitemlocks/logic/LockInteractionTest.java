package de.example.inmisitemlocks.logic;

import static de.example.inmisitemlocks.logic.LockInteraction.Action.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Spec matrix for temporary ("bypass") and permanent ("toggle") lift key. */
class LockInteractionTest {

    // locked, empty, bypass, toggle, shift, middle, press, cursorEmpty
    private static LockInteraction.Action click(
            boolean locked, boolean empty, boolean bypass, boolean toggle, boolean shift) {
        return LockInteraction.onMouse(locked, empty, bypass, toggle, shift, false, true, true);
    }

    @Test
    void tempLift_clickUnlockedSlot_passesThrough() {
        assertEquals(PASS, click(false, false, true, false, false));
        assertEquals(PASS, click(false, true, true, false, false));
    }

    @Test
    void tempLift_clickLockedSlot_allowsMovingIncludingShiftClick() {
        assertEquals(PASS, click(true, false, true, false, false));
        assertEquals(PASS, click(true, false, true, false, true));
    }

    @Test
    void noKey_lockedSlotWithItem_isBlocked_unlocked_passes() {
        assertEquals(CANCEL, click(true, false, false, false, false));
        assertEquals(CANCEL, click(true, false, false, false, true));
        assertEquals(PASS, click(false, false, false, false, false));
    }

    @Test
    void noKey_emptyLockedSlot_stillAcceptsItems() {
        assertEquals(PASS, click(true, true, false, false, false));
    }

    @Test
    void middleClickIsNeverBlocked() {
        assertEquals(PASS, LockInteraction.onMouse(true, false, false, false, false, true, true, true));
    }

    @Test
    void permLift_clickUnlockedSlot_locks() {
        assertEquals(TOGGLE, click(false, false, false, true, false));
    }

    @Test
    void permLift_clickEmptyUnlockedSlot_locks() {
        assertEquals(TOGGLE, click(false, true, false, true, false));
    }

    @Test
    void permLift_clickLockedSlot_unlocks() {
        assertEquals(TOGGLE, click(true, false, false, true, false));
        assertEquals(TOGGLE, click(true, true, false, true, false));
    }

    @Test
    void permLift_releaseWithEmptyCursor_isSwallowedWithoutSecondToggle() {
        assertEquals(CANCEL,
                LockInteraction.onMouse(true, false, false, true, false, false, false, true));
    }

    @Test
    void permLift_withCarriedItem_togglesOnReleaseOnly() {
        assertEquals(CANCEL,
                LockInteraction.onMouse(false, false, false, true, false, false, true, false));
        assertEquals(TOGGLE,
                LockInteraction.onMouse(false, false, false, true, false, false, false, false));
    }

    @Test
    void permLift_withShift_doesNotToggle() {
        assertEquals(CANCEL, click(true, false, false, true, true));
        assertEquals(PASS, click(false, false, false, true, true));
    }

    @Test
    void bypassWinsOverToggle() {
        assertEquals(PASS, click(true, false, true, true, false));
    }
}
