package de.example.inmisitemlocks.logic;

/**
 * Pure decision logic for slot locks.
 *
 * <ul>
 *   <li>temporary lift key ("bypass"): locked slots behave as unlocked, nothing is toggled</li>
 *   <li>permanent lift key ("toggle"): click flips the lock state of any slot</li>
 *   <li>otherwise: a locked slot holding an item cannot be interacted with
 *       (an empty locked slot still accepts items, like ItemLocks does)</li>
 * </ul>
 */
public final class LockInteraction {

    public enum Action { PASS, CANCEL, TOGGLE }

    private LockInteraction() {}

    public static boolean blocksClick(boolean locked, boolean empty, boolean bypass, boolean middle) {
        return locked && !empty && !bypass && !middle;
    }

    public static Action onMouse(
            boolean locked,
            boolean empty,
            boolean bypass,
            boolean toggleKey,
            boolean shift,
            boolean middle,
            boolean press,
            boolean cursorEmpty
    ) {
        if (toggleKey && !bypass && !shift) {
            boolean toggleNow = cursorEmpty ? press : !press;
            return toggleNow ? Action.TOGGLE : Action.CANCEL;
        }

        return blocksClick(locked, empty, bypass, middle) ? Action.CANCEL : Action.PASS;
    }
}
