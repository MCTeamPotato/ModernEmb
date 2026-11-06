package me.kall.modernemb.compat.embeddium;

import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.kall.modernemb.ModernEmb;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Adds ModernEmb's appearance page through Embeddium's public GUI construction event.
 * <p>
 * The event is posted while {@code SodiumOptionsGUI} builds its page list. Adding the page there
 * means it participates in Embeddium's normal tab order, search and native screen as well, instead
 * of being a Modern-UI-only attachment. The event is also forwarded to the Forge bus by Embeddium,
 * so a regular client event subscriber is enough.
 */
@Mod.EventBusSubscriber(modid = ModernEmb.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModernEmbOptionRegistration {
    private static final Logger LOGGER = LogManager.getLogger("ModernEmb/Settings");

    private ModernEmbOptionRegistration() {}

    /**
     * Appends the appearance page once per options GUI construction.
     *
     * @param event Embeddium page construction event
     */
    @SubscribeEvent
    public static void onOptionsConstruction(@NotNull OptionGUIConstructionEvent event) {
        try {
            for (OptionPage page : event.getPages()) {
                if (ModernEmbOptionsPage.isAppearancePage(page)) {
                    return;
                }
            }
            event.addPage(ModernEmbOptionsPage.create());
        } catch (Throwable throwable) {
            LOGGER.error("Failed to register the ModernEmb appearance page", throwable);
        }
    }
}