
package de.example.inmisitemlocks.lock;

import com.kirdow.itemlocks.client.LockManager;
import com.kirdow.itemlocks.util.enums.ContainerType;
import com.kirdow.itemlocks.util.locks.InventoryHelper;
import de.example.inmisitemlocks.logic.LockIds;
import draylar.inmis.ui.BackpackHandledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

public final class PlayerSlotLockHandler {

    private PlayerSlotLockHandler() {
    }

    public static int getLockId(
            AbstractContainerScreen<?> screen,
            Slot slot
    ) {
        if (screen instanceof BackpackHandledScreen) {
            return LockIds.player(slot.getSlotIndex());
        }

        ContainerType type = ContainerType.match(screen);

        return InventoryHelper.getSlotFromIncrement(
                type,
                slot.getSlotIndex()
        );
    }

    public static boolean isLocked(
            AbstractContainerScreen<?> screen,
            Slot slot,
            LockManager manager
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || slot == null) return false;

        if (slot.container != minecraft.player.getInventory()) {
            return false;
        }

        return manager.isLockedSlotRaw(getLockId(screen, slot));
    }
}
