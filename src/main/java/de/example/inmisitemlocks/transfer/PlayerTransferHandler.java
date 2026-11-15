package de.example.inmisitemlocks.transfer;

import com.kirdow.itemlocks.client.LockManager;
import de.example.inmisitemlocks.lock.PlayerSlotLockHandler;
import draylar.inmis.ui.BackpackScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PlayerTransferHandler {

    private PlayerTransferHandler() {
    }

    public static void transferIntoUnlockedSlots(
            Minecraft minecraft,
            AbstractContainerScreen<?> screen,
            BackpackScreenHandler handler,
            Slot sourceSlot,
            LockManager manager
    ) {
        Player player = minecraft.player;

        if (player == null || minecraft.gameMode == null) return;
        if (!handler.getCarried().isEmpty()) return;
        if (sourceSlot.getItem().isEmpty()) return;
        if (!sourceSlot.mayPickup(player)) return;

        ItemStack source = sourceSlot.getItem().copy();

        List<Slot> playerSlots = new ArrayList<>();

        for (Slot target : handler.slots) {
            if (target.container == player.getInventory()) {
                playerSlots.add(target);
            }
        }

        List<ItemStack> simulated = new ArrayList<>();

        for (Slot target : playerSlots) {
            simulated.add(target.getItem().copy());
        }

        List<Integer> targetSlotIds = new ArrayList<>();

        // Zuerst bereits vorhandene passende Stacks auffüllen.
        if (source.isStackable()) {
            for (int i = 0; i < playerSlots.size(); i++) {
                Slot target = playerSlots.get(i);
                ItemStack existing = simulated.get(i);

                if (PlayerSlotLockHandler.isLocked(screen, target, manager)) {
                    continue;
                }

                if (existing.isEmpty()) continue;

                if (!ItemStack.isSameItemSameTags(source, existing)) {
                    continue;
                }

                if (!target.mayPlace(source)) continue;

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

                if (source.isEmpty()) break;
            }
        }

        // Danach freie, nicht gesperrte Slots verwenden.
        for (int i = 0; i < playerSlots.size(); i++) {
            if (source.isEmpty()) break;

            Slot target = playerSlots.get(i);
            ItemStack existing = simulated.get(i);

            if (PlayerSlotLockHandler.isLocked(screen, target, manager)) {
                continue;
            }

            if (!existing.isEmpty()) continue;
            if (!target.mayPlace(source)) continue;

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

        if (targetSlotIds.isEmpty()) return;

        int sourceSlotId = handler.slots.indexOf(sourceSlot);

        if (sourceSlotId < 0) return;

        // Gegenstand aufnehmen, verteilen und Rest zurücklegen.
        click(minecraft, handler, player, sourceSlotId);

        for (int targetSlotId : targetSlotIds) {
            click(minecraft, handler, player, targetSlotId);
        }

        click(minecraft, handler, player, sourceSlotId);
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