package de.example.inmisitemlocks.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.kirdow.itemlocks.util.enums.ContainerType;
import com.kirdow.itemlocks.util.locks.InventoryHelper;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LockIdsTest {

    @Test
    void playerIdsMatchItemLocksMapping() {
        for (int i = 0; i < 36; i++) {
            assertEquals(
                    InventoryHelper.getSlotFromIncrement(ContainerType.PLAYER_INVENTORY, i),
                    LockIds.player(i),
                    "slot " + i);
        }
    }

    @Test
    void hotbarAndInventoryDoNotCollide() {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < 36; i++) assertEquals(true, ids.add(LockIds.player(i)));
    }

    @Test
    void backpackIdsAreSeparateFromPlayerIds() {
        for (int i = 0; i < 200; i++) {
            assertNotEquals(LockIds.player(i % 36), LockIds.backpack(i));
        }
        assertEquals(1_000_005, LockIds.backpack(5));
    }
}
