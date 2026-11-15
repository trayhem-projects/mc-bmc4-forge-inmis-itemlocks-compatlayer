package de.example.inmisitemlocks.mixin;

import com.kirdow.itemlocks.client.LockManager;
import com.kirdow.itemlocks.client.input.KeyBindings;
import com.kirdow.itemlocks.client.render.ELockModifier;
import com.kirdow.itemlocks.client.render.RenderContainer;
import com.kirdow.itemlocks.config.state.ConfigStates;
import com.kirdow.itemlocks.proxy.Components;
import de.example.inmisitemlocks.BackpackLockIds;
import de.example.inmisitemlocks.lock.BackpackLockHandler;
import de.example.inmisitemlocks.lock.PlayerSlotLockHandler;
import de.example.inmisitemlocks.transfer.BackpackTransferHandler;
import de.example.inmisitemlocks.transfer.PlayerTransferHandler;
import draylar.inmis.ui.BackpackHandledScreen;
import draylar.inmis.ui.BackpackScreenHandler;
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

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Unique
    private boolean inmisitemlocks$blockedDrag = false;

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void inmisitemlocks$handleSlotClick(
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

        AbstractContainerScreen<?> screen =
                (AbstractContainerScreen<?>) (Object) this;

        boolean backpackScreen = screen instanceof BackpackHandledScreen;
        boolean playerSlot =
                slot.container == minecraft.player.getInventory();

        if (playerSlot
                && PlayerSlotLockHandler.isLocked(screen, slot, manager)) {
            ci.cancel();
            return;
        }

        if (!backpackScreen) {
            if (clickType == ClickType.QUICK_MOVE && !playerSlot) {
                ItemStack source = slot.getItem();

                if (source.isEmpty()) return;

                for (Slot target : screen.getMenu().slots) {
                    if (target.container != minecraft.player.getInventory()) {
                        continue;
                    }

                    if (!PlayerSlotLockHandler.isLocked(
                            screen, target, manager)) {
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

            return;
        }

        BackpackScreenHandler handler =
                ((BackpackHandledScreen) screen).getMenu();

        if (handler == null) return;

        if (playerSlot) {
            if (clickType == ClickType.QUICK_MOVE) {
                BackpackTransferHandler.transferIntoUnlockedSlots(
                        minecraft, handler, slot, manager
                );

                ci.cancel();
            }

            return;
        }

        if (BackpackLockHandler.isLocked(slot, manager)) {
            ci.cancel();
            return;
        }

        // Rücktransfer aus dem Backpack ins Spielerinventar.
        if (clickType == ClickType.QUICK_MOVE) {
            PlayerTransferHandler.transferIntoUnlockedSlots(
                    minecraft, screen, handler, slot, manager
            );

            ci.cancel();
        }
    }

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

        AbstractContainerScreen<?> screen =
                (AbstractContainerScreen<?>) (Object) this;

        boolean locked;

        if (hovered.container == minecraft.player.getInventory()) {
            locked = PlayerSlotLockHandler.isLocked(
                    screen, hovered, manager
            );
        } else if (screen instanceof BackpackHandledScreen) {
            locked = BackpackLockHandler.isLocked(hovered, manager);
        } else {
            locked = false;
        }

        if (locked) {
            inmisitemlocks$blockedDrag = true;
            cir.setReturnValue(true);
        }
    }

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
}