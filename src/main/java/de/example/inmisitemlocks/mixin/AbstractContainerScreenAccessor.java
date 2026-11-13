package de.example.inmisitemlocks.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Invoker("getSlotUnderMouse")
    Slot inmisitemlocks$getSlotUnderMouse();

    @Invoker("findSlot")
    Slot inmisitemlocks$findSlot(double mouseX, double mouseY);

    @Accessor("leftPos")
    int inmisitemlocks$getLeftPos();

    @Accessor("topPos")
    int inmisitemlocks$getTopPos();

    @Accessor("quickCraftSlots")
    Set<Slot> inmisitemlocks$getQuickCraftSlots();

    @Accessor("isQuickCrafting")
    void inmisitemlocks$setQuickCrafting(boolean value);
}