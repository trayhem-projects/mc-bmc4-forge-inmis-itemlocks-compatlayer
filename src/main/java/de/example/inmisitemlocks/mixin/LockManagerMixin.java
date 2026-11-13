package de.example.inmisitemlocks.mixin;

import com.kirdow.itemlocks.client.LockManager;
import com.kirdow.itemlocks.client.input.KeyBindings;
import com.kirdow.itemlocks.common.KeyboardEvent;
import com.kirdow.itemlocks.common.MouseEvent;
import de.example.inmisitemlocks.BackpackLockIds;
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

        if (!(minecraft.screen instanceof BackpackHandledScreen screen)
                || minecraft.player == null
                || event.button == null) {
            return;
        }

        Slot slot = ((AbstractContainerScreenAccessor) screen)
                .inmisitemlocks$getSlotUnderMouse();

        if (slot == null) {
            cir.setReturnValue(false);
            return;
        }

        boolean backpackSlot =
                slot.container != minecraft.player.getInventory();

        // Exakt dieselbe ID-Zuordnung wie ItemLocks verwenden.
        int lockId = backpackSlot
                ? BackpackLockIds.forSlot(slot)
                : slot.getSlotIndex() + 27;

        boolean locked = isLockedSlotRaw(lockId);

        // Shift-Klick darf den Sperrstatus nicht verändern.
        if (Screen.hasShiftDown()) {
            cir.setReturnValue(
                    locked && !KeyBindings.isBypass()
                            && !event.button.isMiddle
            );
            return;
        }

        if (KeyBindings.isToggle()) {
            boolean cursorEmpty = screen.getMenu().getCarried().isEmpty();

            boolean toggleNow = cursorEmpty
                    ? event.action.isPress
                    : event.action.isRelease;

            if (toggleNow) {
                setLockedSlotRaw(!locked, lockId);
            }

            cir.setReturnValue(true);
            return;
        }

        if (locked
                && !KeyBindings.isBypass()
                && !event.button.isMiddle) {
            cir.setReturnValue(true);
            return;
        }

        // Original-ItemLocks-Methode nicht mit einer falschen ID ausführen.
        cir.setReturnValue(false);
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