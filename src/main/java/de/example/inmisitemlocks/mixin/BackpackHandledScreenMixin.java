package de.example.inmisitemlocks.mixin;

import com.kirdow.itemlocks.client.LockManager;
import com.kirdow.itemlocks.proxy.Components;
import de.example.inmisitemlocks.BackpackLockIds;
import draylar.inmis.ui.BackpackHandledScreen;
import draylar.inmis.ui.BackpackScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackpackHandledScreen.class)
public abstract class BackpackHandledScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void inmisitemlocks$drawLockMarks(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        BackpackHandledScreen screen = (BackpackHandledScreen) (Object) this;

        BackpackScreenHandler handler = screen.getMenu();
        if (handler == null) return;

        LockManager manager;
        try {
            manager = Components.getComponent(LockManager.class);
        } catch (Throwable ignored) {
            return;
        }
        if (manager == null) return;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int guiLeft = accessor.inmisitemlocks$getLeftPos();
        int guiTop = accessor.inmisitemlocks$getTopPos();

        for (Slot slot : handler.slots) {
            if (slot.container != handler.getBackpackInventory()) continue;
            if (!manager.isLockedSlotRaw(BackpackLockIds.forSlot(slot))) continue;

            int x = guiLeft + slot.x + 10;
            int y = guiTop + slot.y + 1;
            graphics.fill(x, y, x + 6, y + 7, 0xD0202020);
            graphics.drawString(Minecraft.getInstance().font, "L", x + 1, y, 0xFFFFFFFF, false);
        }
    }
}
