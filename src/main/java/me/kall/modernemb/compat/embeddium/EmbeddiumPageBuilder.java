package me.kall.modernemb.compat.embeddium;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.screen.MuiSettingsSection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders one Embeddium option page as Modern UI section cards.
 */
final class EmbeddiumPageBuilder {
    private final EmbeddiumState state;
    private final OptionPage page;

    EmbeddiumPageBuilder(@NotNull EmbeddiumState state, @NotNull OptionPage page) {
        this.state = state;
        this.page = page;
    }

    /**
     * Appends the page content.
     *
     * @param ui page builder
     */
    void build(@NotNull MuiSettingsPage ui) {
        boolean rendered = false;

        for (List<Option<?>> visible : this.visibleGroups()) {
            rendered = true;
            ui.section(section -> {
                for (Option<?> option : visible) {
                    EmbeddiumOptionRows.add(section, this.state, option);
                }
            });
        }

        if (!rendered) {
            ui.text(Component.translatable("modernemb.embeddium.empty").getString());
        }
    }

    /**
     * Appends this page to a collapsible card body.
     * <p>
     * {@link MuiSettingsSection} inserts an automatic divider before every row, so no extra
     * divider is added between option groups here; doing that produced two lines at group
     * boundaries.
     *
     * @param section target section, usually the body of a collapsible page card
     */
    void buildInto(@NotNull MuiSettingsSection section) {
        boolean rendered = false;

        for (List<Option<?>> visible : this.visibleGroups()) {
            for (Option<?> option : visible) {
                EmbeddiumOptionRows.add(section, this.state, option);
            }
            rendered = true;
        }

        if (!rendered) {
            section.text(Component.translatable("modernemb.embeddium.empty").getString());
        }
    }

    private @NotNull List<List<Option<?>>> visibleGroups() {
        List<List<Option<?>>> groups = new ArrayList<>();
        for (OptionGroup group : this.page.getGroups()) {
            List<Option<?>> visible = new ArrayList<>();
            for (Option<?> option : group.getOptions()) {
                if (EmbeddiumOptionRows.supports(option)) {
                    visible.add(option);
                }
            }
            if (!visible.isEmpty()) {
                groups.add(visible);
            }
        }
        return groups;
    }

    /**
     * Returns the summary shown on a collapsible page card.
     *
     * @param page Embeddium page
     * @return localized option count, or an empty string when nothing is renderable
     */
    static @NotNull String summary(@NotNull OptionPage page) {
        int count = 0;
        for (OptionGroup group : page.getGroups()) {
            for (Option<?> option : group.getOptions()) {
                if (EmbeddiumOptionRows.supports(option)) count++;
            }
        }
        return count == 0 ? "" : Component.translatable("modernemb.embeddium.group.options", count).getString();
    }
}