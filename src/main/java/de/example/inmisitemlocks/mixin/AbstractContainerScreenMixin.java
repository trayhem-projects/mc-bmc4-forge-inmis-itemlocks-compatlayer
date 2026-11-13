
package de.example.inmisitemlocks.mixin;

import com.kirdow.itemlocks.client.LockManager;
import com.kirdow.itemlocks.client.input.KeyBindings;
import com.kirdow.itemlocks.client.render.RenderContainer;
import com.kirdow.itemlocks.client.render.ELockModifier;
import com.kirdow.itemlocks.proxy.Components;
import com.kirdow.itemlocks.util.locks.InventoryHelper;
import com.kirdow.itemlocks.util.enums.ContainerType;
import de.example.inmisitemlocks.BackpackLockIds;
import draylar.inmis.ui.BackpackHandledScreen;
import draylar.inmis.ui.BackpackScreenHandler;
import draylar.inmis.augment.BackpackInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Unique
    private boolean inmisitemlocks$blockedDrag = false;

    /*
     * Ermittelt die Lock-ID eines Slots im normalen Spielerinventar.
     */
    @Unique
    private int inmisitemlocks$getPlayerLockId(Slot slot) {
        if ((Object) this instanceof BackpackHandledScreen) {
            return slot.getSlotIndex() + 27;
        }

        ContainerType type = ContainerType.match(
                (AbstractContainerScreen<?>) (Object) this
        );

        return InventoryHelper.getSlotFromIncrement(
                type,
                slot.getSlotIndex()
        );
    }

    /*
     * Prüft, ob ein Slot zum Spielerinventar gehört und gesperrt ist.
     */
    @Unique
    private boolean inmisitemlocks$isLockedPlayerSlot(
            Slot slot,
            LockManager manager
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || slot == null) {
            return false;
        }

        if (slot.container != minecraft.player.getInventory()) {
            return false;
        }

        return manager.isLockedSlotRaw(
                inmisitemlocks$getPlayerLockId(slot)
        );
    }

    /*
     * Fängt direkte Klicks und Shift-Klicks ab.
     */
    @Inject(
            method = "slotClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inmisitemlocks$blockLockedSlots(
            Slot slot,
            int slotId,
            int mouseButton,
            ClickType clickType,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || slot == null) return;
        if (KeyBindings.isBypass()) return;

        LockManager manager = Components.getComponent(LockManager.class);
        if (manager == null) return;

        boolean backpackScreen =
                (Object) this instanceof BackpackHandledScreen;

        boolean playerSlot =
                slot.container == minecraft.player.getInventory();

        // Gesperrte Quellslots im Spielerinventar schützen.
        if (playerSlot && inmisitemlocks$isLockedPlayerSlot(slot, manager)) {
            ci.cancel();
            return;
        }

        // Spezielle Inmis-Rucksacklogik.
        if (backpackScreen) {
            BackpackHandledScreen screen =
                    (BackpackHandledScreen) (Object) this;

            BackpackScreenHandler handler = screen.getMenu();
            if (handler == null) return;

            if (!playerSlot) {
                if (manager.isLockedSlotRaw(
                        BackpackLockIds.forSlot(slot))) {
                    ci.cancel();
                    return;
                }

                if (clickType == ClickType.QUICK_MOVE
                        && wouldQuickMoveTouchLockedPlayerSlot(
                                handler, slot, manager)) {
                    ci.cancel();
                }

                return;
            }

            if (clickType == ClickType.QUICK_MOVE
                    && wouldQuickMoveTouchLockedSlot(
                            handler, slot, manager)) {
                ci.cancel();
            }

            return;
        }

        /*
         * Bei normalen Inventarfenstern:
         * Shift-Klick aus einem anderen Container darf keine
         * gesperrten Spielerinventar-Slots als Ziel verwenden.
         */
        if (clickType == ClickType.QUICK_MOVE && !playerSlot) {
            ItemStack source = slot.getItem();

            if (source.isEmpty()) return;

            for (Slot target : ((AbstractContainerScreen<?>)
                    (Object) this).getMenu().slots) {

                if (target.container != minecraft.player.getInventory()) {
                    continue;
                }

                if (!inmisitemlocks$isLockedPlayerSlot(target, manager)) {
                    continue;
                }

                ItemStack existing = target.getItem();

                boolean canReceive =
                        existing.isEmpty()
                                ? target.mayPlace(source)
                                : source.isStackable()
                                && ItemStack.isSameItemSameTags(
                                        source, existing)
                                && target.mayPlace(source)
                                && existing.getCount()
                                < Math.min(
                                        target.getMaxStackSize(existing),
                                        existing.getMaxStackSize()
                                );

                if (canReceive) {
                    ci.cancel();
                    return;
                }
            }
        }
    }

    /*
     * Prüft Shift-Klick aus dem Rucksack in das Spielerinventar.
     */
    @Unique
    private static boolean wouldQuickMoveTouchLockedPlayerSlot(
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
                        || !target.mayPlace(source)) continue;

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
                        target.getSlotIndex() + 27)) {
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
                    target.getSlotIndex() + 27)) {
                return true;
            }

            simulated.set(i, source.copyWithCount(moved));
            source.shrink(moved);
        }

        return false;
    }

    /*
     * Prüft Shift-Klick aus dem Spielerinventar in den Rucksack.
     */
    @Unique
    private static boolean wouldQuickMoveTouchLockedSlot(
            BackpackScreenHandler handler,
            Slot sourceSlot,
            LockManager manager
    ) {
        ItemStack source = sourceSlot.getItem().copy();

        if (source.isEmpty()) return false;
        if (!BackpackInventory.isAllowedStack(source)) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        List<Slot> backpackSlots = new ArrayList<>();

        for (Slot target : handler.slots) {
            if (target.container == minecraft.player.getInventory()) break;
            backpackSlots.add(target);
        }

        List<ItemStack> simulated = new ArrayList<>();

        for (Slot target : backpackSlots) {
            simulated.add(target.getItem().copy());
        }

        if (source.isStackable()) {
            for (int i = 0;
                 i < backpackSlots.size() && !source.isEmpty(); i++) {

                Slot target = backpackSlots.get(i);
                ItemStack existing = simulated.get(i);

                if (existing.isEmpty()
                        || !ItemStack.isSameItemSameTags(source, existing)
                        || !target.mayPlace(source)) continue;

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
                        BackpackLockIds.forSlot(target))) {
                    return true;
                }

                existing.grow(moved);
                source.shrink(moved);
            }
        }

        for (int i = 0;
             i < backpackSlots.size() && !source.isEmpty(); i++) {

            Slot target = backpackSlots.get(i);
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
                    BackpackLockIds.forSlot(target))) {
                return true;
            }

            simulated.set(i, source.copyWithCount(moved));
            source.shrink(moved);
        }

        return false;
    }

    /*
     * Drag-and-Drop:
     * Sobald ein gesperrter Spieler- oder Rucksackslot berührt wird,
     * wird die gesamte Verteilung abgebrochen.
     */
    @Inject(
            method = "mouseDragged",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inmisitemlocks$blockDragAcrossLockedSlots(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || KeyBindings.isBypass()) return;

        if (inmisitemlocks$blockedDrag) {
            cir.setReturnValue(true);
            return;
        }

        LockManager manager = Components.getComponent(LockManager.class);
        if (manager == null) return;

        Slot hovered = ((AbstractContainerScreenAccessor) this)
                .inmisitemlocks$findSlot(mouseX, mouseY);

        if (hovered == null) return;

        boolean backpackScreen =
                (Object) this instanceof BackpackHandledScreen;

        boolean locked;

        if (hovered.container == minecraft.player.getInventory()) {
            locked = inmisitemlocks$isLockedPlayerSlot(hovered, manager);
        } else if (backpackScreen) {
            locked = manager.isLockedSlotRaw(
                    BackpackLockIds.forSlot(hovered)
            );
        } else {
            locked = false;
        }

        if (locked) {
            inmisitemlocks$blockedDrag = true;
            cir.setReturnValue(true);
        }
    }

    /*
     * Beim Loslassen alle gesammelten Drag-Slots verwerfen.
     */
    @Inject(
            method = "mouseReleased",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inmisitemlocks$cancelBlockedDrag(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!inmisitemlocks$blockedDrag) return;

        AbstractContainerScreenAccessor accessor =
                (AbstractContainerScreenAccessor) this;

        accessor.inmisitemlocks$getQuickCraftSlots().clear();
        accessor.inmisitemlocks$setQuickCrafting(false);

        inmisitemlocks$blockedDrag = false;

        cir.setReturnValue(true);
    }

    /*
     * Schlossmarkierungen im Inmis-Rucksack zeichnen.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void inmisitemlocks$drawLockMarks(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!((Object) this instanceof BackpackHandledScreen screen)) return;
        if (Minecraft.getInstance().player == null) return;

        BackpackScreenHandler handler = screen.getMenu();
        if (handler == null) return;

        LockManager manager;

        try {
            manager = Components.getComponent(LockManager.class);
        } catch (Throwable ignored) {
            return;
        }

        if (manager == null) return;

        AbstractContainerScreenAccessor accessor =
                (AbstractContainerScreenAccessor) screen;

        int guiLeft = accessor.inmisitemlocks$getLeftPos();
        int guiTop = accessor.inmisitemlocks$getTopPos();

        Slot hovered = accessor.inmisitemlocks$getSlotUnderMouse();

        boolean toggleActive =
                KeyBindings.isToggle() && !Screen.hasShiftDown();

        for (Slot slot : handler.slots) {
            if (slot.container == Minecraft.getInstance().player.getInventory()) {
                continue;
            }

            if (!manager.isLockedSlotRaw(
                    BackpackLockIds.forSlot(slot))) {
                continue;
            }

            boolean lifted = toggleActive && hovered == slot;

            ELockModifier modifier = lifted
                    ? ELockModifier.LiftedLocked
                    : ELockModifier.Locked;

            RenderContainer.drawLock(
                    graphics,
                    guiLeft + slot.x,
                    guiTop + slot.y,
                    modifier
            );
        }
    }
}
