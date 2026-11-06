package me.kall.modernemb.config;

import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Adapter between Forge's client config and Embeddium's option storage.
 * <p>
 * All values already live in {@link ModernEmbConfig}'s {@link net.minecraftforge.common.ForgeConfigSpec};
 * this class only gives the bridge the data object and save entry point it expects.
 */
public final class ModernEmbSettings {
    private static final Logger LOGGER = LogManager.getLogger("ModernEmb/Settings");
    private static final OptionStorage<ModernEmbConfig> STORAGE = new Storage();

    private ModernEmbSettings() {}

    /**
     * Returns the live appearance config.
     *
     * @return the config facade
     */
    public static @NotNull ModernEmbConfig config() {
        return ModernEmbConfig.get();
    }

    /**
     * Persists the Forge config.
     */
    public static void save() {
        try {
            ModernEmbConfig.save();
        } catch (Throwable throwable) {
            LOGGER.error("Failed to save the ModernEmb client config", throwable);
        }
    }

    /**
     * Returns the Embeddium storage used by the appearance options.
     *
     * @return the storage
     */
    public static @NotNull OptionStorage<ModernEmbConfig> storage() {
        return STORAGE;
    }

    /**
     * Embeddium storage adapter over the singleton config facade.
     */
    private static final class Storage implements OptionStorage<ModernEmbConfig> {
        @Override
        public ModernEmbConfig getData() {
            return ModernEmbConfig.get();
        }

        @Override
        public void save() {
            ModernEmbSettings.save();
        }
    }
}