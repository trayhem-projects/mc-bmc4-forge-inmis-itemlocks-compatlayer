package de.example.inmisitemlocks.mixin;

import com.kirdow.itemlocks.client.LockManager;
import com.kirdow.itemlocks.client.input.KeyBindings;
import com.kirdow.itemlocks.client.render.ELockModifier;
import com.kirdow.itemlocks.client.render.RenderContainer;
import com.kirdow.itemlocks.config.state.ConfigStates;
import com.kirdow.itemlocks.proxy.Components;
import de.example.inmisitemlocks.BackpackLockIds;
import de.example.inmisitemlocks.lock.BackpackLockHandler;
import de.example.inmisitemlocks.logic.LockIds;
import de.example.inmisitemlocks.logic.LockInteraction;
import de.example.inmisitemlocks.logic.LockVisual;
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

    @Inject(
            method = "slotClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inmisitemlocks$handleSlotClick(
            Slot slot,
            int slotId,
            int mouseButton,
            ClickType clickType,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || slot == null) return;
        boolean bypass =
                KeyBindings.isBypass() || ConfigStates.Action.bypassAll();

        if (bypass) return;

        LockManager manager =
                Components.getComponent(LockManager.class);

        if (manager == null) return;

        AbstractContainerScreen<?> screen =
                (AbstractContainerScreen<?>) (Object) this;

        boolean backpackScreen = screen instanceof BackpackHandledScreen;
        boolean playerSlot =
                slot.container == minecraft.player.getInventory();
        boolean middle = clickType == ClickType.CLONE;

        if (playerSlot
                && LockInteraction.blocksClick(
                        PlayerSlotLockHandler.isLocked(screen, slot, manager),
                        !slot.hasItem(), false, middle)) {
            ci.cancel();
            return;
        }

        if (backpackScreen
                && inmisitemlocks$touchesLockedStack(
                        minecraft, screen, slot, mouseButton, clickType, manager)) {
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
                        minecraft,
                        handler,
                        slot,
                        manager
                );

                ci.cancel();
            }

            return;
        }

        if (LockInteraction.blocksClick(
                BackpackLockHandler.isLocked(slot, manager),
                !slot.hasItem(), false, middle)) {
            ci.cancel();
            return;
        }

        if (clickType == ClickType.QUICK_MOVE) {
            PlayerTransferHandler.transferIntoUnlockedSlots(
                    minecraft,
                    screen,
                    handler,
                    slot,
                    manager
            );

            ci.cancel();
        }
    }

    /**
     * Number-key swaps with a locked hotbar slot and double-click "collect all"
     * would pull items out of locked slots without clicking on them.
     */
    @Unique
    private static boolean inmisitemlocks$touchesLockedStack(
            Minecraft minecraft,
            AbstractContainerScreen<?> screen,
            Slot clicked,
            int mouseButton,
            ClickType clickType,
            LockManager manager
    ) {
        if (clickType == ClickType.SWAP) {
            int hotbar = mouseButton;

            if (hotbar >= 0 && hotbar <= 8) {
                return LockInteraction.blocksClick(
                        manager.isLockedSlotRaw(LockIds.player(hotbar)),
                        minecraft.player.getInventory().getItem(hotbar).isEmpty(),
                        false, false);
            }

            return false;
        }

        if (clickType == ClickType.PICKUP_ALL) {
            ItemStack carried = screen.getMenu().getCarried();

            if (carried.isEmpty()) return false;

            for (Slot other : screen.getMenu().slots) {
                if (!other.hasItem()
                        || !ItemStack.isSameItemSameTags(carried, other.getItem())) {
                    continue;
                }

                boolean locked = other.container == minecraft.player.getInventory()
                        ? PlayerSlotLockHandler.isLocked(screen, other, manager)
                        : BackpackLockHandler.isLocked(other, manager);

                if (locked) return true;
            }
        }

        return false;
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

        if (minecraft.player == null
                || KeyBindings.isBypass()
                || ConfigStates.Action.bypassAll()) return;

        if (inmisitemlocks$blockedDrag) {
            cir.setReturnValue(true);
            return;
        }

        LockManager manager =
                Components.getComponent(LockManager.class);

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

        locked = LockInteraction.blocksClick(
                locked, !hovered.hasItem(), false, false);

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

    @Inject(method = "render", at = @At("TAIL"))
    private void inmisitemlocks$drawLockMarks(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!((Object) this instanceof BackpackHandledScreen screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) return;

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

        boolean bypassVisual =
                KeyBindings.isBypass()
                        || ConfigStates.Action.bypassAll()
                        || (!handler.getCarried().isEmpty()
                        && ConfigStates.Action.bypassHeld());

        for (Slot slot : handler.slots) {
            if (slot.container == minecraft.player.getInventory()) {
                continue;
            }

            LockVisual.Icon icon = LockVisual.icon(
                    manager.isLockedSlotRaw(BackpackLockIds.forSlot(slot)),
                    slot.hasItem(),
                    hovered == slot,
                    bypassVisual,
                    toggleActive
            );

            ELockModifier modifier = switch (icon) {
                case NONE -> null;
                case LOCKED -> ELockModifier.Locked;
                case UNLOCKED -> ELockModifier.Unlocked;
                case LIFTED_LOCKED -> ELockModifier.LiftedLocked;
                case LIFTED_UNLOCKED -> ELockModifier.LiftedUnlocked;
            };

            if (modifier == null) continue;

            RenderContainer.drawLock(
                    graphics,
                    guiLeft + slot.x,
                    guiTop + slot.y,
                    modifier
            );
        }
    }
}
