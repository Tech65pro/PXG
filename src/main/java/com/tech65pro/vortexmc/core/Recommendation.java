package com.tech65pro.vortexmc.core;

/**
 * A change Smart Auto proposes to the player.
 *
 * <p>A recommendation describes <em>what</em> would change and <em>why</em> that is expected to
 * reduce the amount of work the renderer does. It deliberately carries no promised frame-rate
 * figure: the effect is measured after the fact by {@link PerformanceAnalyzer} and only then
 * reported.</p>
 */
public final class Recommendation {

    /** How visible the change is likely to be to the player. */
    public enum Risk {
        SAFE("Barely noticeable"),
        MODERATE("Visible in some situations"),
        NOTICEABLE("Clearly visible");

        private final String label;

        Risk(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Setting setting;
    private final String title;
    private final String reason;
    private final String currentValue;
    private final String proposedValue;
    private final Risk risk;

    public Recommendation(Setting setting,
                          String title,
                          String reason,
                          String currentValue,
                          String proposedValue,
                          Risk risk) {
        this.setting = setting;
        this.title = title;
        this.reason = reason;
        this.currentValue = currentValue;
        this.proposedValue = proposedValue;
        this.risk = risk;
    }

    public Setting setting() {
        return setting;
    }

    public String title() {
        return title;
    }

    public String reason() {
        return reason;
    }

    public String currentValue() {
        return currentValue;
    }

    public String proposedValue() {
        return proposedValue;
    }

    public Risk risk() {
        return risk;
    }

    /** One-line summary used in the dashboard list. */
    public String summary() {
        return title + ": " + currentValue + " -> " + proposedValue;
    }

    @Override
    public String toString() {
        return "Recommendation[" + summary() + ", " + risk.label() + "]";
    }
}
