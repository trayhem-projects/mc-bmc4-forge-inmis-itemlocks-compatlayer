
package de.example.inmisitemlocks.lock;

import com.kirdow.itemlocks.client.LockManager;
import de.example.inmisitemlocks.BackpackLockIds;
import de.example.inmisitemlocks.logic.LockIds;
import draylar.inmis.ui.BackpackScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BackpackLockHandler {

    private BackpackLockHandler() {
    }

    public static boolean isLocked(Slot slot, LockManager manager) {
        return manager.isLockedSlotRaw(BackpackLockIds.forSlot(slot));
    }

    public static boolean wouldQuickMoveTouchLockedPlayerSlot(
            BackpackScreenHandler handler,
            Slot sourceSlot,
            LockManager manager
    ) {
        ItemStack source = sourceSlot.getItem().copy();

        if (source.isEmpty()) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        List<Slot> playerSlots = new ArrayList<>();

        for (Slot target : handler.slots) {
            if (target.container == minecraft.player.getInventory()) {
                playerSlots.add(target);
            }
        }

        List<ItemStack> simulated = new ArrayList<>();

        for (Slot target : playerSlots) {
            simulated.add(target.getItem().copy());
        }

        if (source.isStackable()) {
            for (int i = playerSlots.size() - 1;
                 i >= 0 && !source.isEmpty(); i--) {

                Slot target = playerSlots.get(i);
                ItemStack existing = simulated.get(i);

                if (existing.isEmpty()
                        || !ItemStack.isSameItemSameTags(source, existing)
                        || !target.mayPlace(source)) {
                    continue;
                }

                int max = Math.min(
                        target.getMaxStackSize(existing),
                        existing.getMaxStackSize()
                );

                int moved = Math.min(
                        max - existing.getCount(),
                        source.getCount()
                );

                if (moved <= 0) continue;

                if (manager.isLockedSlotRaw(
                        LockIds.player(target.getSlotIndex()))) {
                    return true;
                }

                existing.grow(moved);
                source.shrink(moved);
            }
        }

        for (int i = playerSlots.size() - 1;
             i >= 0 && !source.isEmpty(); i--) {

            Slot target = playerSlots.get(i);
            ItemStack existing = simulated.get(i);

            if (!existing.isEmpty() || !target.mayPlace(source)) {
                continue;
            }

            int max = Math.min(
                    target.getMaxStackSize(source),
                    source.getMaxStackSize()
            );

            int moved = Math.min(max, source.getCount());

            if (moved <= 0) continue;

            if (manager.isLockedSlotRaw(
                    LockIds.player(target.getSlotIndex()))) {
                return true;
            }

            simulated.set(i, source.copyWithCount(moved));
            source.shrink(moved);
        }

        return false;
    }
}
