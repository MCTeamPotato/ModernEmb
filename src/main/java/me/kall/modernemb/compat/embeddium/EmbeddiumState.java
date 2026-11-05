package me.kall.modernemb.compat.embeddium;

import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.screen.MuiConfigFragment;
import me.kall.narutoui.mui.screen.MuiDialog;
import me.kall.narutoui.mui.screen.MuiScreenHost;
import me.kall.narutoui.mui.theme.MuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Editing session shared by the Modern UI pages and the action bar.
 * <p>
 * Values are staged by Embeddium's own {@link Option} implementations: rows call
 * {@link Option#setValue(Object)}, Apply calls {@link Option#applyChanges()} and Undo calls
 * {@link Option#reset()}. This keeps dirty tracking, option flags and storage persistence identical
 * to Embeddium's screen while the presentation is Modern UI.
 */
final class EmbeddiumState {
    private static final Logger LOGGER = LogManager.getLogger("ModernEmb/Embeddium");

    private final List<Option<?>> options;
    private final @Nullable ShaderState shaderState;

    private @Nullable MuiScreenHost host;
    private @Nullable Button applyButton;
    private @Nullable Button undoButton;
    private @Nullable Button doneButton;
    private @Nullable TextView statusView;
    private boolean closeDialogOpen;
    private boolean applying;

    EmbeddiumState(@NotNull List<OptionPage> pages, @Nullable ShaderState shaderState) {
        this.shaderState = shaderState;
        List<Option<?>> all = new ArrayList<>();
        for (OptionPage page : pages) {
            for (OptionGroup group : page.getGroups()) {
                all.addAll(group.getOptions());
            }
        }
        this.options = List.copyOf(all);

        if (shaderState != null) {
            shaderState.setChangeListener(this::markEdited);
        }
    }

    /**
     * Receives the created screen host.
     *
     * @param fragment the screen fragment
     */
    void onCreated(@NotNull MuiConfigFragment fragment) {
        this.host = fragment;
        this.refreshActions();
    }

    /**
     * Drops the host reference when the screen view is destroyed.
     */
    void onDestroyed() {
        this.host = null;
    }

    /**
     * Builds the fixed action bar.
     *
     * @param context screen context
     * @return the action bar
     */
    @NotNull View buildActionBar(@NotNull Context context) {
        MuiTheme theme = MuiTheme.modern(context);
        LinearLayout bar = MuiControls.row(context);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1.0F));

        this.statusView = MuiControls.text(context, theme, "", 12, theme.onSurfaceVariant());
        LinearLayout.LayoutParams statusParams = MuiControls.wrap();
        statusParams.rightMargin = MuiControls.dp(context, 8);
        bar.addView(this.statusView, statusParams);

        this.undoButton = MuiControls.button(context, theme, label("modernemb.embeddium.undo"), true, view -> this.undo());
        this.addButton(context, bar, this.undoButton);

        this.applyButton = MuiControls.button(context, theme, label("modernemb.embeddium.apply"), true, view -> this.apply());
        this.addButton(context, bar, this.applyButton);

        this.doneButton = MuiControls.button(context, theme, label("modernemb.embeddium.done"), false, view -> this.finish());
        this.addButton(context, bar, this.doneButton);

        this.refreshActions();
        return bar;
    }

    private void addButton(@NotNull Context context, @NotNull LinearLayout bar, @NotNull Button button) {
        LinearLayout.LayoutParams params = MuiControls.wrap();
        params.leftMargin = MuiControls.dp(context, 8);
        params.gravity = Gravity.CENTER_VERTICAL;
        bar.addView(button, params);
    }

    /**
     * @return {@code true} when the screen may close immediately
     */
    boolean onCloseRequested() {
        if (this.applying) {
            return false;
        }
        if (!this.hasChanges()) {
            return true;
        }

        MuiScreenHost current = this.host;
        if (current == null) {
            return true;
        }

        // Modern UI calls the close guard from the client thread when Done/Escape goes through
        // UIManager#onBackPressed. ViewRoot mutations must happen on the Modern UI thread.
        if (Core.isOnUiThread()) {
            this.showCloseDialog(current);
        } else {
            MuiModApi.postToUiThread(() -> this.showCloseDialog(current));
        }
        return false;
    }

    private void showCloseDialog(@NotNull MuiScreenHost host) {
        if (this.closeDialogOpen || this.host != host) {
            return;
        }

        LOGGER.info("Close requested with pending changes; embeddium={}, shaders={}",
                this.hasEmbeddiumChanges(), this.shaderState != null && this.shaderState.hasChanges());
        this.closeDialogOpen = true;
        MuiDialog.confirm(host,
                label("modernemb.embeddium.unsaved_title"),
                label("modernemb.embeddium.unsaved_message"),
                label("modernemb.embeddium.discard"),
                () -> {
                    this.closeDialogOpen = false;
                    host.close();
                },
                label("modernemb.embeddium.keep_editing"),
                () -> this.closeDialogOpen = false);
    }

    /**
     * Refreshes the action buttons after a row changed.
     */
    void markEdited() {
        this.refreshActions();
    }

    /**
     * @return whether any option holds a value that has not been applied
     */
    boolean hasChanges() {
        for (Option<?> option : this.options) {
            if (option.hasChanged()) {
                return true;
            }
        }
        return this.shaderState != null && this.shaderState.hasChanges();
    }

    private boolean hasEmbeddiumChanges() {
        for (Option<?> option : this.options) {
            if (option.hasChanged()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Done: close immediately when clean, otherwise apply first and close on success. This avoids
     * the old "Apply, then Done, then an unsaved-changes dialog" loop.
     */
    private void finish() {
        if (this.applying) {
            return;
        }
        if (!this.hasChanges()) {
            this.closeNow();
            return;
        }
        this.apply(true);
    }

    private void closeNow() {
        MuiScreenHost current = this.host;
        if (current != null) {
            current.close();
        }
    }

    private void refreshActions() {
        boolean dirty = this.hasChanges();

        if (this.applyButton != null) {
            this.applyButton.setEnabled(!this.applying && dirty);
            this.applyButton.setTooltipText(label("modernemb.embeddium.apply_hint"));
        }
        if (this.undoButton != null) {
            this.undoButton.setEnabled(!this.applying && dirty);
            this.undoButton.setTooltipText(label("modernemb.embeddium.undo_hint"));
        }
        if (this.doneButton != null) {
            this.doneButton.setEnabled(!this.applying);
            this.doneButton.setText(label(dirty
                    ? "modernemb.embeddium.done_apply"
                    : "modernemb.embeddium.done"));
            this.doneButton.setTooltipText(label(dirty
                    ? "modernemb.embeddium.done_apply_hint"
                    : "modernemb.embeddium.done_close_hint"));
        }
        if (this.statusView != null) {
            this.statusView.setText(this.applying
                    ? label("modernemb.embeddium.status_applying")
                    : dirty ? label("modernemb.embeddium.status_pending") : "");
        }
    }

    private void showAppliedStatus() {
        if (this.statusView == null) {
            return;
        }
        this.statusView.setText(label("modernemb.embeddium.status_applied"));
        this.statusView.postDelayed(this::refreshActions, 1600L);
    }

    /**
     * Applies every changed option on the Minecraft render thread. Modern UI dispatches button
     * clicks on its own UI thread, while several Embeddium bindings call render-thread-only APIs
     * (GUI scale calls {@code Minecraft.resizeDisplay()}, fullscreen calls the window, renderer
     * flags touch the level renderer). Doing that work from the UI thread crashes with
     * "Rendersystem called from wrong thread".
     */
    void apply() {
        this.apply(false);
    }

    private void apply(boolean closeAfter) {
        if (this.applying) {
            return;
        }
        if (!this.hasChanges()) {
            if (closeAfter) {
                this.closeNow();
            }
            return;
        }

        Minecraft client = Minecraft.getInstance();
        this.applying = true;

        if (client.isSameThread()) {
            this.applyOnRenderThread(closeAfter);
            return;
        }

        this.refreshActions();
        client.execute(() -> this.applyOnRenderThread(closeAfter));
    }

    private void applyOnRenderThread(boolean closeAfter) {
        boolean restart = false;
        boolean shaderChanged = this.shaderState != null && this.shaderState.hasChanges();
        Throwable failure = null;

        try {
            Set<OptionStorage<?>> dirtyStorages = new LinkedHashSet<>();
            EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

            for (Option<?> option : this.options) {
                if (!option.hasChanged()) {
                    continue;
                }

                option.applyChanges();
                flags.addAll(option.getFlags());
                dirtyStorages.add(option.getStorage());
            }

            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
                    client.levelRenderer.allChanged();
                } else if (flags.contains(OptionFlag.REQUIRES_RENDERER_UPDATE)) {
                    client.levelRenderer.needsUpdate();
                }
            }

            if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
                client.updateMaxMipLevel(client.options.mipmapLevels().get());
                client.delayTextureReload();
            }

            for (OptionStorage<?> storage : dirtyStorages) {
                try {
                    storage.save();
                } catch (Throwable throwable) {
                    LOGGER.error("Failed to save an Embeddium option storage", throwable);
                }
            }

            if (this.shaderState != null && this.shaderState.hasChanges()) {
                LOGGER.info("Applying pending Oculus shader changes");
                this.shaderState.applyOnRenderThread();
            }

            restart = flags.contains(OptionFlag.REQUIRES_GAME_RESTART);
        } catch (Throwable throwable) {
            failure = throwable;
            LOGGER.error("Failed to apply Embeddium options", throwable);
        }

        Throwable error = failure;
        boolean showRestart = restart;
        boolean reloadPages = shaderChanged;
        MuiModApi.postToUiThread(() -> {
            this.applying = false;
            this.refreshActions();

            MuiScreenHost current = this.host;
            if (error != null) {
                if (current != null) {
                    MuiDialog.notice(current,
                            label("modernemb.embeddium.apply_failed"),
                            applyFailureMessage(error),
                            label("modernemb.embeddium.ok"),
                            () -> {});
                }
                return;
            }

            if (closeAfter) {
                if (showRestart && current != null) {
                    MuiDialog.notice(current,
                            label("modernemb.embeddium.restart_title"),
                            label("modernemb.embeddium.restart_message"),
                            label("modernemb.embeddium.ok"),
                            current::close);
                } else {
                    this.closeNow();
                }
                return;
            }

            if (reloadPages && current != null) {
                current.reloadAllPages();
            }

            this.showAppliedStatus();
            if (showRestart && current != null) {
                MuiDialog.notice(current,
                        label("modernemb.embeddium.restart_title"),
                        label("modernemb.embeddium.restart_message"),
                        label("modernemb.embeddium.ok"),
                        () -> {});
            }
        });
    }

    private static @NotNull String applyFailureMessage(@NotNull Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? label("modernemb.embeddium.apply_failed_message")
                : message;
    }

    /**
     * Restores every staged value from its storage and rebuilds the visible page. Resetting reads
     * window/options state, so it runs on the render thread; rebuilding the page is posted back to
     * the Modern UI thread.
     */
    void undo() {
        if (this.applying) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.isSameThread()) {
            this.undoOnRenderThread();
            return;
        }

        if (this.undoButton != null) {
            this.undoButton.setEnabled(false);
        }
        client.execute(this::undoOnRenderThread);
    }

    private void undoOnRenderThread() {
        try {
            if (this.shaderState != null) {
                this.shaderState.reset();
            }
            for (Option<?> option : this.options) {
                option.reset();
            }
        } catch (Throwable throwable) {
            LOGGER.error("Failed to reset Embeddium options", throwable);
        }

        MuiModApi.postToUiThread(() -> {
            MuiScreenHost current = this.host;
            if (current != null) {
                current.reloadAllPages();
            }
            this.refreshActions();
        });
    }

    private static @NotNull String label(@NotNull String key) {
        return Component.translatable(key).getString();
    }
}