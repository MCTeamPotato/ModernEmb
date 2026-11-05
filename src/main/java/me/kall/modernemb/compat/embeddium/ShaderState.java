package me.kall.modernemb.compat.embeddium;

import me.kall.narutoui.mui.screen.MuiSettingsPage;
import org.jetbrains.annotations.NotNull;

/**
 * Optional shader-pack editing session that shares the Embeddium screen's action bar.
 * <p>
 * Implementations are loaded only when the corresponding shader mod is present; the interface
 * itself deliberately has no shader-mod types so Embeddium's screen can stay optional-safe.
 */
public interface ShaderState {
    /**
     * @return whether the draft differs from the active shader configuration
     */
    boolean hasChanges();

    /**
     * Applies the draft. Called on the Minecraft render thread because Iris reloads GL state.
     */
    void applyOnRenderThread();

    /**
     * Restores the draft from the active shader configuration.
     */
    void reset();

    /**
     * Registers a callback invoked after a user edit changes the draft. The host uses this to
     * refresh the shared action bar immediately.
     *
     * @param listener change listener, called on the Modern UI thread
     */
    void setChangeListener(@NotNull Runnable listener);

    /**
     * Appends the shader page content.
     *
     * @param ui page builder
     */
    void buildPage(@NotNull MuiSettingsPage ui);
}