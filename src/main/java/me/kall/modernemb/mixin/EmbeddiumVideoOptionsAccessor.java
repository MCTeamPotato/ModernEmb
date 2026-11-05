package me.kall.modernemb.mixin;

import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import net.minecraft.client.gui.screens.Screen;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Reads the already-constructed option pages and the real parent screen from Embeddium's video
 * options screen. Reusing that list keeps the bridge on Embeddium's own option model, including
 * the groups and options that other mods register during page construction.
 */
@Mixin(EmbeddiumVideoOptionsScreen.class)
public interface EmbeddiumVideoOptionsAccessor {
    /**
     * @return the pages built by {@code SodiumOptionsGUI}
     */
    @Accessor("pages")
    List<OptionPage> modernemb$pages();

    /**
     * @return the screen that opened the Embeddium options screen
     */
    @Accessor("prevScreen")
    Screen modernemb$previousScreen();
}