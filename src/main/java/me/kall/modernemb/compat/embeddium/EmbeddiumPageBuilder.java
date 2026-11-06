package me.kall.modernemb.compat.embeddium;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.screen.MuiSettingsSection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        for (GroupView group : this.visibleGroups()) {
            rendered = true;
            String title = this.groupTitle(group.group());
            if (title != null) {
                ui.section(title, section -> {
                    for (Option<?> option : group.options()) {
                        EmbeddiumOptionRows.add(section, this.state, option);
                    }
                });
            } else {
                ui.section(section -> {
                    for (Option<?> option : group.options()) {
                        EmbeddiumOptionRows.add(section, this.state, option);
                    }
                });
            }
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

        for (GroupView group : this.visibleGroups()) {
            for (Option<?> option : group.options()) {
                EmbeddiumOptionRows.add(section, this.state, option);
            }
            rendered = true;
        }

        if (!rendered) {
            section.text(Component.translatable("modernemb.embeddium.empty").getString());
        }
    }

    private @Nullable String groupTitle(@NotNull OptionGroup group) {
        if (!ModernEmbOptionsPage.isAppearancePage(this.page)) {
            return null;
        }
        return ModernEmbOptionsPage.groupTitle(group.getId());
    }

    private @NotNull List<GroupView> visibleGroups() {
        List<GroupView> groups = new ArrayList<>();
        for (OptionGroup group : this.page.getGroups()) {
            List<Option<?>> visible = new ArrayList<>();
            for (Option<?> option : group.getOptions()) {
                if (EmbeddiumOptionRows.supports(option)) {
                    visible.add(option);
                }
            }
            if (!visible.isEmpty()) {
                groups.add(new GroupView(group, List.copyOf(visible)));
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

    /**
     * One visible group: the source group (for its title) and the options this bridge can render.
     *
     * @param group   source option group
     * @param options renderable options
     */
    private record GroupView(@NotNull OptionGroup group, @NotNull List<Option<?>> options) {}
}