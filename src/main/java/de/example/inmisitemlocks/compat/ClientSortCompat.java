package de.example.inmisitemlocks.compat;

import de.example.inmisitemlocks.InmisItemLocksCompat;
import draylar.inmis.ui.BackpackHandledScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Makes ClientSort show its buttons on the Inmis backpack screen. */
@Mod.EventBusSubscriber(
        modid = InmisItemLocksCompat.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientSortCompat {

    private ClientSortCompat() {}

    @SubscribeEvent
    public static void beforeInit(ScreenEvent.Init.Pre event) {
        if (!(event.getScreen() instanceof BackpackHandledScreen)
                || !ModList.get().isLoaded("clientsort")) {
            return;
        }

        try {
            ClientSortPolicy.ensureRegistered();
        } catch (Throwable t) {
            // ClientSort API changed - buttons just won't show.
        }
    }
}
