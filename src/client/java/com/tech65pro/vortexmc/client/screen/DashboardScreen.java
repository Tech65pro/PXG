package com.tech65pro.vortexmc.client.screen;

import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.client.VortexServices;
import com.tech65pro.vortexmc.client.screen.tabs.FpsMonitorTab;
import com.tech65pro.vortexmc.client.screen.tabs.GraphicsTab;
import com.tech65pro.vortexmc.client.screen.tabs.MemoryTab;
import com.tech65pro.vortexmc.client.screen.tabs.NetworkTab;
import com.tech65pro.vortexmc.client.screen.tabs.OverviewTab;
import com.tech65pro.vortexmc.client.screen.tabs.ProfilesTab;
import com.tech65pro.vortexmc.client.screen.tabs.SettingsTab;
import com.tech65pro.vortexmc.client.screen.Ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The VortexMC dashboard.
 *
 * <p>A single screen with a tab strip. Widgets are Minecraft's own, so hover states, sounds and
 * narration all behave the way players expect; everything that is not a widget (graphs, bars,
 * wrapped explanatory text) is drawn by the tabs themselves inside a scissored content area.</p>
 */
public class DashboardScreen extends Screen {

    private static final int SCROLL_STEP = 24;

    private final VortexServices services;
    private final List<DashboardTab> tabs = new ArrayList<>();
    private final List<ButtonWidget> tabButtons = new ArrayList<>();
    private final List<SectionHeader> sectionHeaders = new ArrayList<>();

    private int activeTab;
    private int scrollOffset;
    private int contentHeightUsed;

    public DashboardScreen(VortexServices services) {
        super(Text.literal("VortexMC"));
        this.services = services;
        this.tabs.add(new OverviewTab());
        this.tabs.add(new FpsMonitorTab());
        this.tabs.add(new GraphicsTab());
        this.tabs.add(new MemoryTab());
        this.tabs.add(new NetworkTab());
        this.tabs.add(new ProfilesTab());
        this.tabs.add(new SettingsTab());
    }

    public VortexServices services() {
        return services;
    }

    public TextRenderer font() {
        TextRenderer renderer = client == null ? null : client.textRenderer;
        return renderer == null ? MinecraftClientHolder.font() : renderer;
    }

    public DashboardTab activeTab() {
        return tabs.get(activeTab);
    }

    public int activeTabIndex() {
        return activeTab;
    }

    // ------------------------------------------------------------------ layout

    public int contentX() {
        return Theme.MARGIN;
    }

    public int contentY() {
        return Theme.MARGIN + Theme.TITLE_HEIGHT + Theme.TAB_HEIGHT + Theme.GAP;
    }

    public int contentWidth() {
        return this.width - Theme.MARGIN * 2;
    }

    public int contentHeight() {
        return this.height - contentY() - Theme.FOOTER_HEIGHT - Theme.MARGIN;
    }

    /** Reports how tall the tab's drawn content is, so the scroll limit can be computed. */
    public void reportContentHeight(int height) {
        this.contentHeightUsed = height;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public int maxScroll() {
        return Math.max(0, contentHeightUsed - contentHeight());
    }

    private void clampScroll() {
        int max = maxScroll();
        if (scrollOffset > max) {
            scrollOffset = max;
        }
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void init() {
        tabButtons.clear();
        sectionHeaders.clear();
        buildTabStrip();
        scrollOffset = 0;
        contentHeightUsed = 0;
        tabs.get(activeTab).build(this);
        clampScroll();
    }

    private void buildTabStrip() {
        int x = Theme.MARGIN;
        int y = Theme.MARGIN + Theme.TITLE_HEIGHT + Theme.GAP;
        int width = contentWidth();
        int buttonWidth = Math.max(40, (width - Theme.GAP * (tabs.size() - 1)) / tabs.size());
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            boolean selected = i == activeTab;
            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(tabs.get(i).title()),
                    b -> selectTab(index))
                    .dimensions(x, y, buttonWidth, Theme.TAB_HEIGHT)
                    .build();
            button.active = !selected;
            tabButtons.add(addDrawableChild(button));
            x += buttonWidth + Theme.GAP;
        }
    }

    public void selectTab(int index) {
        if (index < 0 || index >= tabs.size() || index == activeTab) {
            return;
        }
        activeTab = index;
        scrollOffset = 0;
        contentHeightUsed = 0;
        tabs.get(activeTab).onSelected();
        rebuild();
    }

    /** Clears and rebuilds the current tab, keeping the scroll offset. */
    public void rebuild() {
        int savedScroll = scrollOffset;
        clearAndInit();
        scrollOffset = savedScroll;
        clampScroll();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        services.save();
        super.close();
    }

    @Override
    public void removed() {
        services.save();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackdrop(context);
        renderHeader(context);
        renderTabStrip(context);
        renderContent(context, mouseX, mouseY, deltaTicks);
        renderFooter(context);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderBackdrop(DrawContext context) {
        context.fill(0, 0, this.width, this.height, Theme.BACKDROP);
        context.fillGradient(0, 0, this.width, Theme.MARGIN + Theme.TITLE_HEIGHT,
                Theme.withAlpha(Theme.ACCENT, 40), Theme.withAlpha(Theme.ACCENT_ALT, 12));
    }

    private void renderHeader(DrawContext context) {
        TextRenderer font = font();
        int x = Theme.MARGIN;
        int y = Theme.MARGIN;
        context.drawTextWithShadow(font, Text.literal("VortexMC"), x, y + 4, Theme.TEXT);
        String subtitle = "Minecraft " + services.config.schemaVersion()
                + "  |  " + services.compatibility.interactionNote();
        Ui.wrapped(context, font, subtitle, x + 78, y + 5, this.width - x * 2 - 78,
                Theme.TEXT_FAINT, 10);
        context.fill(x, y + Theme.TITLE_HEIGHT, this.width - Theme.MARGIN,
                y + Theme.TITLE_HEIGHT + 1, Theme.DIVIDER);
    }

    private void renderTabStrip(DrawContext context) {
        TextRenderer font = font();
        int y = Theme.MARGIN + Theme.TITLE_HEIGHT + Theme.GAP;
        for (int i = 0; i < tabs.size(); i++) {
            ButtonWidget button = tabButtons.get(i);
            if (button == null) {
                continue;
            }
            int bx = button.getX();
            int by = button.getY();
            int bw = button.getWidth();
            boolean selected = i == activeTab;
            if (selected) {
                context.fill(bx, by, bx + bw, by + Theme.TAB_HEIGHT, Theme.ACCENT_SOFT);
                context.fill(bx, by, bx + bw, by + 1, Theme.ACCENT);
            } else if (isOver(bx, by, bw, Theme.TAB_HEIGHT)) {
                context.fill(bx, by, bx + bw, by + Theme.TAB_HEIGHT, Theme.PANEL_RAISED);
            }
            String label = Ui.clip(font, tabs.get(i).title(), bw - 8);
            int textX = bx + (bw - font.getWidth(label)) / 2;
            context.drawTextWithShadow(font, Text.literal(label), textX, by + 6,
                    selected ? Theme.TEXT : Theme.TEXT_DIM);
        }
    }

    private void renderContent(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int x = contentX();
        int y = contentY();
        int width = contentWidth();
        int height = contentHeight();
        if (height <= 4) {
            return;
        }
        context.enableScissor(x, y, x + width, y + height);
        context.fill(x, y, x + width, y + height, Theme.PANEL);
        int translatedY = y - scrollOffset;
        context.enableScissor(x, y, x + width, y + height);
        DashboardTab tab = tabs.get(activeTab);
        for (SectionHeader header : sectionHeaders) {
            Ui.panel(context, header.x(), header.y() - scrollOffset, header.width(), 15);
            context.drawTextWithShadow(font(), Text.literal(header.label()),
                    header.x() + 6, header.y() - scrollOffset + 3, Theme.TEXT);
        }
        tab.render(this, context, mouseX, mouseY - translatedY, deltaTicks);
        context.disableScissor();
        if (maxScroll() > 0) {
            renderScrollbar(context, x, y, width, height);
        }
        context.disableScissor();
    }

    private void renderScrollbar(DrawContext context, int x, int y, int width, int height) {
        int trackX = x + width - 4;
        context.fill(trackX, y + 2, trackX + 2, y + height - 2, Theme.PANEL_RAISED);
        int total = contentHeightUsed;
        if (total <= 0) {
            return;
        }
        int thumbHeight = Math.max(12, (int) ((double) height / total * (height - 4)));
        int travel = height - 4 - thumbHeight;
        int thumbY = y + 2 + (int) ((double) scrollOffset / maxScroll() * travel);
        context.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, Theme.ACCENT);
    }

    private void renderFooter(DrawContext context) {
        TextRenderer font = font();
        int y = this.height - Theme.FOOTER_HEIGHT - Theme.MARGIN + 2;
        String left = services.smartAuto.phase().label() + "  -  " + services.smartAuto.lastMessage();
        Ui.wrapped(context, font, left, Theme.MARGIN, y, this.width - Theme.MARGIN * 2,
                Theme.TEXT_DIM, 9);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= contentX() && mouseX <= contentX() + contentWidth()
                && mouseY >= contentY() && mouseY <= contentY() + contentHeight()) {
            int before = scrollOffset;
            scrollOffset += (int) Math.round(verticalAmount * SCROLL_STEP);
            clampScroll();
            if (scrollOffset != before) {
                activeTab().scroll(this, verticalAmount);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean isOver(int x, int y, int width, int height) {
        return lastMouseX >= x && lastMouseX <= x + width && lastMouseY >= y && lastMouseY <= y + height;
    }

    private double lastMouseX;
    private double lastMouseY;

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    /**
     * Registers a section header to be drawn inside the content area. Headers are recorded during
     * {@code build()} and painted during {@code render()}, because no draw context exists yet when
     * the widgets are created.
     */
    public void addSectionHeader(int x, int y, int width, String label) {
        sectionHeaders.add(new SectionHeader(x, y, width, label));
    }

    private record SectionHeader(int x, int y, int width, String label) {
    }

    // ------------------------------------------------------------------ widget helpers

    /** Adds a button inside the content area, offset by the current scroll position. */
    public ButtonWidget addButton(int x, int y, int width, int height, String label,
                                  ButtonWidget.PressAction action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(label), action)
                .dimensions(contentX() + x, contentY() + y - scrollOffset, width, height)
                .build());
    }

    /** Adds an integer slider inside the content area. */
    public SliderWidget addSlider(int x, int y, int width, int height, String label,
                                  int min, int max, int value, IntConsumer onChange) {
        int span = Math.max(1, max - min);
        int clamped = Math.max(min, Math.min(max, value));
        double fraction = (clamped - min) / (double) span;
        VortexSlider slider = new VortexSlider(contentX() + x, contentY() + y - scrollOffset,
                width, height, label, min, max, onChange);
        slider.setSliderValue(fraction);
        return addDrawableChild(slider);
    }

    /**
     * Adds an arbitrary widget to this screen. {@code Screen.addDrawableChild} is protected, so
     * tabs reach it through here.
     */
    public <T extends Element & Drawable & Selectable> void addChild(T child) {
        addDrawableChild(child);
    }

    /** Vertical position of a content row, already adjusted for scrolling. */
    public int rowY(int y) {
        return y - scrollOffset;
    }

    private static final class VortexSlider extends SliderWidget {

        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer onChange;

        private VortexSlider(int x, int y, int width, int height, String label,
                             int min, int max, IntConsumer onChange) {
            super(x, y, width, height, Text.literal(label), 0d);
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        /**
         * SliderWidget.setValue is protected, so callers outside this subclass go through here.
         * {@code fraction} is a 0..1 position, matching SliderWidget's own value scale.
         */
        private void setSliderValue(double fraction) {
            setValue(fraction);
        }

        private int currentValue() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + currentValue()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(currentValue());
        }
    }

    /** Holder that keeps the static font lookup out of the instance path. */
    private static final class MinecraftClientHolder {
        private static TextRenderer font() {
            return net.minecraft.client.MinecraftClient.getInstance() == null
                    ? null : net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        }
    }
}
