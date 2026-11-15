package de.example.inmisitemlocks.mixin;

import de.example.inmisitemlocks.BackpackLockIds;
import draylar.inmis.ui.BackpackHandledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ClientSort only asks ItemLocks about player-inventory slots. Teach it about
 * locked Inmis backpack slots so sort / transfer / match-transfer skip them.
 * Pseudo: silently ignored when ClientSort is not installed.
 */
@Pseudo
@Mixin(
        targets = "dev.terminalmc.clientsort.client.compat.itemlocks.ItemLocksCompat",
        remap = false
)
public abstract class ClientSortItemLocksCompatMixin {

    @Shadow
    public static boolean checkStatic(int index) {
        throw new AssertionError();
    }

    @Inject(method = "isLocked", at = @At("HEAD"), cancellable = true, remap = false)
    private static void inmisitemlocks$backpackSlots(
            Slot slot,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (slot.container instanceof Inventory
                || !(Minecraft.getInstance().screen instanceof BackpackHandledScreen)) {
            return;
        }

        cir.setReturnValue(checkStatic(BackpackLockIds.forSlot(slot)));
    }
}
