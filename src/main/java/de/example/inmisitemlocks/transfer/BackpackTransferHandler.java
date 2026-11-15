
package de.example.inmisitemlocks.transfer;

import com.kirdow.itemlocks.client.LockManager;
import de.example.inmisitemlocks.BackpackLockIds;
import draylar.inmis.augment.BackpackInventory;
import draylar.inmis.ui.BackpackScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BackpackTransferHandler {

    private BackpackTransferHandler() {
    }

    public static void transferIntoUnlockedSlots(
            Minecraft minecraft,
            BackpackScreenHandler handler,
            Slot sourceSlot,
            LockManager manager
    ) {
        Player player = minecraft.player;

        if (player == null || minecraft.gameMode == null) return;
        if (handler.getCarried().isEmpty() == false) return;
        if (sourceSlot.getItem().isEmpty()) return;
        if (!sourceSlot.mayPickup(player)) return;

        ItemStack source = sourceSlot.getItem().copy();

        if (!BackpackInventory.isAllowedStack(source)) return;

        List<Slot> backpackSlots = new ArrayList<>();

        for (Slot target : handler.slots) {
            if (target.container == player.getInventory()) {
                break;
            }

            backpackSlots.add(target);
        }

        List<ItemStack> simulated = new ArrayList<>();

        for (Slot target : backpackSlots) {
            simulated.add(target.getItem().copy());
        }

        List<Integer> targetSlotIds = new ArrayList<>();

        // Phase 1: passende vorhandene Stapel auffüllen.
        if (source.isStackable()) {
            for (int i = 0;
                 i < backpackSlots.size() && !source.isEmpty(); i++) {

                Slot target = backpackSlots.get(i);
                ItemStack existing = simulated.get(i);

                if (BackpackLockIdsLocked(target, manager)) {
                    continue;
                }

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

                existing.grow(moved);
                source.shrink(moved);

                targetSlotIds.add(handler.slots.indexOf(target));
            }
        }

        // Phase 2: freie, entsperrte Slots befüllen.
        for (int i = 0;
             i < backpackSlots.size() && !source.isEmpty(); i++) {

            Slot target = backpackSlots.get(i);
            ItemStack existing = simulated.get(i);

            if (BackpackLockIdsLocked(target, manager)) {
                continue;
            }

            if (!existing.isEmpty() || !target.mayPlace(source)) {
                continue;
            }

            int max = Math.min(
                    target.getMaxStackSize(source),
                    source.getMaxStackSize()
            );

            int moved = Math.min(max, source.getCount());

            if (moved <= 0) continue;

            simulated.set(i, source.copyWithCount(moved));
            source.shrink(moved);

            targetSlotIds.add(handler.slots.indexOf(target));
        }

        // Nichts passt: keine Klicks senden.
        if (targetSlotIds.isEmpty()) return;

        int sourceSlotId = handler.slots.indexOf(sourceSlot);

        if (sourceSlotId < 0) return;

        // Quellstapel aufnehmen.
        click(minecraft, handler, player, sourceSlotId);

        // Nur die zuvor berechneten entsperrten Slots befüllen.
        for (int targetSlotId : targetSlotIds) {
            click(minecraft, handler, player, targetSlotId);
        }

        // Eventuellen Rest in den Quellslot zurücklegen.
        click(minecraft, handler, player, sourceSlotId);
    }

    private static boolean BackpackLockIdsLocked(
            Slot slot,
            LockManager manager
    ) {
        return manager.isLockedSlotRaw(BackpackLockIds.forSlot(slot));
    }

    private static void click(
            Minecraft minecraft,
            BackpackScreenHandler handler,
            Player player,
            int slotId
    ) {
        minecraft.gameMode.handleInventoryMouseClick(
                handler.containerId,
                slotId,
                0,
                ClickType.PICKUP,
                player
        );
    }
}
