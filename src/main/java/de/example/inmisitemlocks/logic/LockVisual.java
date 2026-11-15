package de.example.inmisitemlocks.logic;

/** Pure decision which lock icon is drawn on a slot (mirrors ItemLocks' RenderContainer). */
public final class LockVisual {

    public enum Icon { NONE, LOCKED, UNLOCKED, LIFTED_LOCKED, LIFTED_UNLOCKED }

    private LockVisual() {}

    public static Icon icon(
            boolean locked,
            boolean hasItem,
            boolean hovered,
            boolean bypass,
            boolean toggle
    ) {
        // Unlocked slots never show anything, regardless of hover or lift keys.
        if (!locked) return Icon.NONE;

        boolean lifted = toggle && hovered;

        if (bypass && hovered) {
            return lifted ? Icon.LIFTED_UNLOCKED : Icon.UNLOCKED;
        }

        if (hasItem) {
            return lifted ? Icon.LIFTED_LOCKED : Icon.LOCKED;
        }

        return lifted ? Icon.LIFTED_UNLOCKED : Icon.UNLOCKED;
    }
}
