package me.kall.modernemb.compat.embeddium;

import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.kall.modernemb.ModernEmb;
import me.kall.modernemb.mixin.EmbeddiumVideoOptionsAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Replaces Embeddium's video options screen with the Modern UI screen at the last possible moment.
 * <p>
 * Embeddium's {@code SodiumOptionsGUI} opens an {@link EmbeddiumVideoOptionsScreen} from its own
 * {@code init()}. By the time this event fires, the pages have already been built and all
 * option-construction events have run, so the bridge can reuse that exact model and its real parent
 * screen.
 */
@Mod.EventBusSubscriber(modid = ModernEmb.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EmbeddiumScreenHook {
    private static final Logger LOGGER = LogManager.getLogger("ModernEmb/Embeddium");

    private EmbeddiumScreenHook() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.@NotNull Opening event) {
        if (event.isCanceled()) {
            return;
        }

        Screen next = event.getNewScreen();
        if (!(next instanceof EmbeddiumVideoOptionsScreen videoOptionsScreen)) {
            return;
        }

        try {
            EmbeddiumVideoOptionsAccessor access = (EmbeddiumVideoOptionsAccessor) videoOptionsScreen;
            List<OptionPage> pages = access.modernemb$pages();
            Screen parent = access.modernemb$previousScreen();

            Screen replacement = EmbeddiumMuiScreen.create(parent, pages);
            if (replacement != null) {
                event.setNewScreen(replacement);
            }
        } catch (Throwable throwable) {
            LOGGER.error("Failed to open the Modern UI screen for Embeddium options", throwable);
        }
    }
}