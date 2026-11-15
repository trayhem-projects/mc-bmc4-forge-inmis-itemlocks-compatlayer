package de.example.inmisitemlocks.mixin;

import com.kirdow.itemlocks.client.LockManager;
import com.kirdow.itemlocks.client.input.KeyBindings;
import com.kirdow.itemlocks.common.KeyboardEvent;
import com.kirdow.itemlocks.common.MouseEvent;
import com.kirdow.itemlocks.config.state.ConfigStates;
import com.kirdow.itemlocks.util.enums.ContainerType;
import com.kirdow.itemlocks.util.locks.InventoryHelper;
import de.example.inmisitemlocks.BackpackLockIds;
import de.example.inmisitemlocks.logic.LockIds;
import de.example.inmisitemlocks.logic.LockInteraction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Unique;
import draylar.inmis.ui.BackpackHandledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LockManager.class, remap = false)
public abstract class LockManagerMixin {

    @Shadow
    public abstract void setLockedSlotRaw(boolean locked, int slot);

    @Shadow
    public abstract boolean isLockedSlotRaw(int slot);

    @Shadow
    private boolean isVanillaDropActive(KeyboardEvent event) {
        throw new AssertionError();
    }

    @Shadow
    private boolean isVanillaSwapActive(KeyboardEvent event) {
        throw new AssertionError();
    }

    @Inject(
            method = "mouseEvent",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void inmisitemlocks$handleBackpackMouse(
            MouseEvent event,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || event.button == null
                || !(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        Slot slot = ((AbstractContainerScreenAccessor) screen)
                .inmisitemlocks$getSlotUnderMouse();

        boolean backpackScreen = screen instanceof BackpackHandledScreen;

        if (!backpackScreen) {
            inmisitemlocks$lockEmptyPlayerSlot(minecraft, screen, slot, event, cir);
            return;
        }

        if (slot == null) {
            cir.setReturnValue(false);
            return;
        }

        boolean backpackSlot =
                slot.container != minecraft.player.getInventory();

        int lockId = backpackSlot
                ? BackpackLockIds.forSlot(slot)
                : LockIds.player(slot.getSlotIndex());

        LockInteraction.Action action = LockInteraction.onMouse(
                isLockedSlotRaw(lockId),
                !slot.hasItem(),
                KeyBindings.isBypass() || ConfigStates.Action.bypassAll(),
                KeyBindings.isToggle(),
                Screen.hasShiftDown(),
                event.button.isMiddle,
                event.action.isPress,
                screen.getMenu().getCarried().isEmpty()
        );

        switch (action) {
            case TOGGLE -> {
                setLockedSlotRaw(!isLockedSlotRaw(lockId), lockId);
                cir.setReturnValue(true);
            }
            case CANCEL -> cir.setReturnValue(true);
            default -> cir.setReturnValue(false);
        }
    }

    /**
     * Plain ItemLocks cannot lock an empty player slot (toggle on an empty,
     * unlocked slot does nothing). Make that work as well.
     */
    @Unique
    private void inmisitemlocks$lockEmptyPlayerSlot(
            Minecraft minecraft,
            AbstractContainerScreen<?> screen,
            Slot slot,
            MouseEvent event,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (slot == null
                || slot.container != minecraft.player.getInventory()
                || slot.hasItem()
                || !KeyBindings.isToggle()
                || Screen.hasShiftDown()) {
            return;
        }

        int lockId = InventoryHelper.getSlotFromIncrement(
                ContainerType.match(screen),
                slot.getSlotIndex()
        );

        if (isLockedSlotRaw(lockId)) {
            return; // ItemLocks unlocks it itself.
        }

        boolean cursorEmpty = screen.getMenu().getCarried().isEmpty();
        boolean toggleNow = cursorEmpty ? event.action.isPress : event.action.isRelease;

        if (toggleNow) {
            setLockedSlotRaw(true, lockId);
        }

        cir.setReturnValue(true);
    }

    @Inject(
            method = "keyboardEvent",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void inmisitemlocks$handleBackpackKeyboard(
            KeyboardEvent event,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Slot slot = getHoveredBackpackSlot();

        if (slot == null
                || !isLockedSlotRaw(BackpackLockIds.forSlot(slot))
                || KeyBindings.isBypass()) {
            return;
        }

        if (isVanillaDropActive(event) || isVanillaSwapActive(event)) {
            cir.setReturnValue(true);
        }
    }

    private static Slot getHoveredBackpackSlot() {
        Minecraft minecraft = Minecraft.getInstance();

        if (!(minecraft.screen instanceof BackpackHandledScreen screen)) {
            return null;
        }

        if (minecraft.player == null) {
            return null;
        }

        Slot slot = ((AbstractContainerScreenAccessor) screen)
                .inmisitemlocks$getSlotUnderMouse();

        if (slot == null
                || slot.container == minecraft.player.getInventory()) {
            return null;
        }

        return slot;
    }
}