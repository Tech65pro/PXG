package com.tech65pro.vortexmc.client;

/**
 * The colours and spacing VortexMC's own UI uses.
 *
 * <p>Kept in one place so the overlay and the dashboard cannot drift apart. Colours are packed
 * ARGB the way {@code DrawContext} expects.</p>
 */
public final class Theme {

    private Theme() {
    }

    // ------------------------------------------------------------------ surfaces

    /** Screen backdrop, behind the panels. */
    public static final int BACKDROP = 0xF00B0D14;
    /** Panel body. */
    public static final int PANEL = 0xF0151826;
    /** Raised element inside a panel. */
    public static final int PANEL_RAISED = 0xF01D2436;
    /** Selected or highlighted element. */
    public static final int PANEL_HIGHLIGHT = 0xF0242F47;
    /** Thin divider lines. */
    public static final int DIVIDER = 0xFF2C3A52;

    // ------------------------------------------------------------------ accents

    public static final int ACCENT = 0xFF5B8DEF;
    public static final int ACCENT_SOFT = 0x335B8DEF;
    public static final int ACCENT_ALT = 0xFF9B6BFF;
    public static final int ACCENT_ALT_SOFT = 0x339B6BFF;

    // ------------------------------------------------------------------ text

    public static final int TEXT = 0xFFE9ECF5;
    public static final int TEXT_DIM = 0xFF98A2BC;
    public static final int TEXT_FAINT = 0xFF6B7490;

    // ------------------------------------------------------------------ status

    public static final int GOOD = 0xFF4ECB71;
    public static final int WARN = 0xFFF2C14E;
    public static final int BAD = 0xFFE5566D;

    // ------------------------------------------------------------------ metrics

    public static final int GRAPH_LINE = 0xFF5B8DEF;
    public static final int GRAPH_FILL = 0x405B8DEF;
    public static final int GRAPH_GRID = 0x22FFFFFF;

    // ------------------------------------------------------------------ layout

    public static final int MARGIN = 10;
    public static final int GAP = 6;
    public static final int ROW_HEIGHT = 14;
    public static final int TITLE_HEIGHT = 34;
    public static final int TAB_HEIGHT = 20;
    public static final int FOOTER_HEIGHT = 18;

    /** Slightly transparent panel fill. */
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
