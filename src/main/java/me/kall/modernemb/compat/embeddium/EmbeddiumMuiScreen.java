package me.kall.modernemb.compat.embeddium;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.View;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.kall.modernemb.config.ModernEmbConfig;
import me.kall.modernemb.config.ModernEmbSettings;
import me.kall.modernemb.config.NavigationStyle;
import me.kall.narutoui.mui.factory.MuiWidgetFactory;
import me.kall.narutoui.mui.screen.MuiSettingsScreen;
import me.kall.narutoui.mui.shimmer.MuiShimmerProfile;
import me.kall.narutoui.mui.shimmer.MuiShimmerable;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.widget.MuiLogo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a Modern UI screen from Embeddium's own option pages.
 * <p>
 * The pages are not rebuilt: ModernEmb reuses the exact list {@code SodiumOptionsGUI} constructed
 * (plus its own appearance page, added defensively if the construction event did not run), so the
 * bridge keeps Embeddium's option model, bindings, conditions, apply flags and persistence. Only
 * the presentation (navigation rail, section cards, switches, sliders and choice grids) and the
 * apply/undo flow are replaced.
 */
public final class EmbeddiumMuiScreen {
    private static final Logger LOGGER = LogManager.getLogger("ModernEmb/Embeddium");

    private EmbeddiumMuiScreen() {}

    /**
     * Creates the replacement screen.
     *
     * @param parent previous screen
     * @param pages  pages built by Embeddium
     * @return the Modern UI screen, or {@code null} when there is nothing to render
     */
    public static @Nullable Screen create(@Nullable Screen parent, @NotNull List<OptionPage> pages) {
        return create(parent, pages, false);
    }

    /**
     * Creates the replacement screen.
     *
     * @param parent          previous screen
     * @param pages           pages built by Embeddium
     * @param focusAppearance {@code true} to open ModernEmb's appearance page first
     * @return the Modern UI screen, or {@code null} when there is nothing to render
     */
    public static @Nullable Screen create(@Nullable Screen parent,
                                          @NotNull List<OptionPage> pages,
                                          boolean focusAppearance) {
        if (pages.isEmpty()) {
            return null;
        }

        // The Forge event normally registered this page while Embeddium built the GUI. Add it
        // defensively when another mod opens the screen with a hand-built page list.
        List<OptionPage> allPages = new ArrayList<>(pages);
        if (allPages.stream().noneMatch(ModernEmbOptionsPage::isAppearancePage)) {
            allPages.add(ModernEmbOptionsPage.create());
        }

        ModernEmbConfig style = ModernEmbSettings.config();
        ShaderState shaderState = createShaderState();
        OptionPage oculusPlaceholder = shaderState == null ? null : findOculusPlaceholder(allPages);
        EmbeddiumState state = new EmbeddiumState(allPages, shaderState, parent);
        MuiSettingsScreen.Builder builder = MuiSettingsScreen.builder("Embeddium")
                .background(ModernEmbTheme.background(style))
                .widgetFactory(style.glowActive() ? MuiWidgetFactory.shimmer() : MuiWidgetFactory.modern())
                .shimmerProfile(MuiShimmerProfile.create(new ModernEmbShimmer(style)))
                .shimmerNavigation(MuiShimmerable.of(style.glow() && style.glowNavigation()))
                .shimmerTitles(MuiShimmerable.of(style.glow() && style.glowTitles()))
                .actionBar(state::buildActionBar)
                .onCloseRequested(state::onCloseRequested)
                .onCreated(state::onCreated)
                .onDestroyed(fragment -> state.onDestroyed());

        applyNavigation(builder, style);
        MuiTheme theme = ModernEmbTheme.resolve(style);
        if (theme != null) {
            builder.theme(theme);
        }

        for (EmbeddiumPageGroups.Group group : EmbeddiumPageGroups.build(allPages, oculusPlaceholder)) {
            if (group.grouped()) {
                String title = group.displayName();
                String subtitle = Component.translatable("modernemb.embeddium.group.subtitle", group.pages().size()).getString();

                builder.page(title, title, subtitle, ui -> {
                    for (int i = 0; i < group.pages().size(); i++) {
                        OptionPage page = group.pages().get(i);
                        String pageTitle = page.getName().getString();
                        if (pageTitle.isBlank()) {
                            pageTitle = title;
                        }

                        ui.collapsibleSection(pageTitle, EmbeddiumPageBuilder.summary(page), i == 0,
                                section -> new EmbeddiumPageBuilder(state, page).buildInto(section));
                    }
                });
                continue;
            }

            OptionPage page = group.pages().get(0);
            String title = page.getName().getString();
            if (title.isBlank()) {
                title = group.displayName();
            }
            String subtitle = group.modId() != null && !group.displayName().equalsIgnoreCase(title)
                    ? group.displayName()
                    : null;

            builder.page(title, title, subtitle, ui -> new EmbeddiumPageBuilder(state, page).build(ui));
        }

        if (shaderState != null) {
            // Oculus injects an empty page whose only purpose is to open its native screen from
            // SodiumOptionsGUI#setPage. Reuse that page's translated title for the Modern UI page
            // so the player sees one shader entry, not two.
            String shaderTitle = oculusPlaceholder != null
                    ? oculusPlaceholder.getName().getString()
                    : Component.translatable("modernemb.embeddium.shaders").getString();
            builder.page(shaderTitle, shaderTitle, null, shaderState::buildPage);
        }

        if (focusAppearance) {
            for (OptionPage page : allPages) {
                if (ModernEmbOptionsPage.isAppearancePage(page)) {
                    builder.initialPage(page.getName().getString());
                    break;
                }
            }
        }

        return builder.create(parent);
    }

    private static void applyNavigation(@NotNull MuiSettingsScreen.Builder builder, @NotNull ModernEmbConfig style) {
        if (style.navigationStyle() == NavigationStyle.TOP) {
            builder.topNavigation();
            return;
        }

        builder.leftNavigation();
        if (style.navigationStyle() == NavigationStyle.SIDEBAR) {
            builder.railHeader(EmbeddiumMuiScreen::railLogo);
        }
    }

    private static @NotNull View railLogo(@NotNull Context context) {
        MuiTheme theme = ModernEmbTheme.resolve(context);
        return MuiLogo.create(context, theme, "π");
    }

    /**
     * Finds Oculus' legacy empty shader page. Oculus adds it in {@code SodiumOptionsGUI#init} and
     * opens {@code ShaderPackScreen} from {@code setPage}; our bridge replaces that page with the
     * Modern UI shader page instead of showing both.
     *
     * @param pages pages built by Embeddium
     * @return the placeholder page, or {@code null}
     */
    private static @Nullable OptionPage findOculusPlaceholder(@NotNull List<OptionPage> pages) {
        String key = "options.iris.shaderPackSelection";
        String translated = Component.translatable(key).getString();
        OptionPage firstEmpty = null;

        for (OptionPage page : pages) {
            if (!page.getGroups().isEmpty()) {
                continue;
            }

            if (firstEmpty == null) {
                firstEmpty = page;
            }

            String name = page.getName().getString();
            if (name.equals(key) || name.equals(translated)) {
                return page;
            }
        }

        // Oculus' placeholder is the empty page; use it as a fallback if its display name was
        // changed by a resource pack or a different Iris build.
        return firstEmpty;
    }

    /**
     * Loads the Oculus/Iris shader session lazily. The factory class is resolved by name so a
     * client without Oculus never links any Oculus class through this screen.
     *
     * @return the shader session, or {@code null} when no supported shader mod is installed
     */
    private static @Nullable ShaderState createShaderState() {
        if (!ModList.get().isLoaded("oculus") && !ModList.get().isLoaded("iris")) {
            return null;
        }

        try {
            Class<?> factory = Class.forName("me.kall.modernemb.compat.oculus.OculusShaderFactory");
            return (ShaderState) factory.getMethod("create").invoke(null);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to create the Oculus shader session", throwable);
            return null;
        }
    }
}