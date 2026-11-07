package com.tech65pro.vortexmc.client;

import com.tech65pro.vortexmc.config.VortexConfig;
import com.tech65pro.vortexmc.core.MetricSeries;
import com.tech65pro.vortexmc.core.PerformanceAnalysis;
import com.tech65pro.vortexmc.core.PerformanceAnalyzer;
import com.tech65pro.vortexmc.core.Recommendation;
import com.tech65pro.vortexmc.core.RecommendationEngine;
import com.tech65pro.vortexmc.core.Setting;
import com.tech65pro.vortexmc.core.SettingChange;
import com.tech65pro.vortexmc.core.SettingsSnapshot;

import java.util.List;

/**
 * Smart Auto: watch, propose, ask, apply, measure, and only then report.
 *
 * <p>The controller is a small state machine. It never changes a setting on its own when approval
 * is required, and it never reports a gain it has not measured. If a measured change turns out to
 * be a regression, the previous values are restored automatically.</p>
 *
 * <pre>
 * IDLE --requestAnalysis()--> AWAITING_APPROVAL --approve()--> MEASURING_BEFORE
 *                                   |                              | enough samples
 *                                   | decline()                    v
 *                                   v                            (apply) -> MEASURING_AFTER
 *                                 IDLE                                              | enough samples
 *                                                                                   v
 *                                                          EVALUATED (or REVERTED)
 * </pre>
 */
public final class SmartAutoController {

    public enum Phase {
        IDLE("Idle"),
        AWAITING_APPROVAL("Waiting for your approval"),
        MEASURING_BEFORE("Measuring current performance"),
        MEASURING_AFTER("Change applied, measuring"),
        EVALUATED("Measured"),
        REVERTED("Change reverted"),
        COOLDOWN("Waiting before the next check");

        private final String label;

        Phase(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final long SAMPLE_INTERVAL_MILLIS = 1000L;

    private final SettingMapper mapper;
    private final PerformanceMonitor monitor;
    private final VortexConfig config;

    private final MetricSeries beforeSeries = new MetricSeries(600);
    private final MetricSeries afterSeries = new MetricSeries(600);

    private Phase phase = Phase.IDLE;
    private Recommendation pending;
    private SettingsSnapshot beforeSnapshot;
    private long cooldownUntilMillis;
    private long lastSampleMillis;
    private int samplesInCurrentPhase;
    private PerformanceAnalysis lastAnalysis;
    private String lastMessage = "Smart Auto is idle.";
    private Setting lastAppliedSetting;

    /** The proposal the player has been asked about and has not yet answered. */
    private Recommendation inflight;

    public SmartAutoController(SettingMapper mapper, PerformanceMonitor monitor, VortexConfig config) {
        this.mapper = mapper;
        this.monitor = monitor;
        this.config = config;
    }

    public Phase phase() {
        return phase;
    }

    public String lastMessage() {
        return lastMessage;
    }

    public Recommendation pending() {
        return pending;
    }

    public PerformanceAnalysis lastAnalysis() {
        return lastAnalysis;
    }

    /** 0..1 progress through the current measurement phase. */
    public double progress() {
        int target = Math.max(1, config.measurementSeconds());
        return Math.min(1.0, samplesInCurrentPhase / (double) target);
    }

    public int samplesInCurrentPhase() {
        return samplesInCurrentPhase;
    }

    public int measurementTarget() {
        return config.measurementSeconds();
    }

    public boolean isBusy() {
        return phase == Phase.MEASURING_BEFORE || phase == Phase.MEASURING_AFTER;
    }

    public boolean isInCooldown(long nowMillis) {
        return phase == Phase.COOLDOWN && nowMillis < cooldownUntilMillis;
    }

    public long cooldownRemainingMillis(long nowMillis) {
        if (phase != Phase.COOLDOWN) {
            return 0L;
        }
        return Math.max(0L, cooldownUntilMillis - nowMillis);
    }

    /**
     * Looks at the current metrics and decides whether to propose a change.
     *
     * @return the proposal, or {@code null} when nothing should be proposed
     */
    public Recommendation requestAnalysis() {
        if (isBusy()) {
            lastMessage = "A measurement is already running.";
            return null;
        }
        if (!monitor.hasHistory()) {
            lastMessage = "Not enough performance data yet. Play for a few seconds and try again.";
            return null;
        }
        if (!mapper.isAvailable()) {
            lastMessage = "Game options are not available yet.";
            return null;
        }

        Recommendation suggestion = RecommendationEngine.suggest(
                mapper.currentAsProfile(),
                monitor.sessionAverageFps(),
                monitor.onePercentLowFps(),
                monitor.isStuttering(),
                config.minFpsBeforeActing());

        if (suggestion == null) {
            lastMessage = "Nothing to change. The game is running within the target frame rate, or "
                    + "every supported setting is already at its most conservative value.";
            phase = Phase.IDLE;
            return null;
        }

        pending = suggestion;
        phase = Phase.AWAITING_APPROVAL;
        lastMessage = "Proposed: " + suggestion.summary() + ". Waiting for your approval.";
        return suggestion;
    }

    /** The player declined the proposal. */
    public void decline() {
        pending = null;
        phase = Phase.IDLE;
        lastMessage = "Proposal declined. No settings were changed.";
        enterCooldown();
    }

    /**
     * The player approved the proposal. Starts the measurement cycle: a baseline window first, then
     * the change, then a second window.
     */
    public boolean approve() {
        if (pending == null) {
            return false;
        }
        // Move the proposal out of `pending` and into `inflight`. `pending` is what the GUI
        // shows an Approve button for, so leaving it set would let the player approve a
        // second time while the first measurement is still running, which would overwrite
        // the baseline snapshot taken before any change was made.
        inflight = pending;
        pending = null;
        beforeSnapshot = mapper.snapshot();
        config.lastKnownGood(beforeSnapshot);
        beforeSeries.clear();
        afterSeries.clear();
        samplesInCurrentPhase = 0;
        lastSampleMillis = 0L;
        lastAnalysis = null;
        phase = Phase.MEASURING_BEFORE;
        lastMessage = "Measuring the current frame rate for " + config.measurementSeconds() + " seconds.";
        return true;
    }

    /** Reverts the most recent change Smart Auto made, restoring the values captured before it. */
    public boolean revertLast() {
        if (beforeSnapshot == null || lastAppliedSetting == null) {
            lastMessage = "There is nothing to undo.";
            return false;
        }
        int restored = mapper.restore(beforeSnapshot);
        markLastReverted();
        lastAppliedSetting = null;
        phase = Phase.REVERTED;
        lastMessage = restored > 0
                ? "Restored " + restored + " setting(s) to their previous values."
                : "No settings needed restoring.";
        enterCooldown();
        return restored > 0;
    }

    /** Restores the snapshot saved before Smart Auto first changed anything this session. */
    public boolean restoreLastKnownGood() {
        SettingsSnapshot snapshot = config.lastKnownGood();
        if (snapshot.isEmpty()) {
            lastMessage = "No saved settings to restore.";
            return false;
        }
        int restored = mapper.restore(snapshot);
        markLastReverted();
        lastAppliedSetting = null;
        lastMessage = restored > 0
                ? "Restored " + restored + " setting(s) from the last known-good snapshot."
                : "The saved snapshot no longer matches any supported setting.";
        enterCooldown();
        return restored > 0;
    }

    /**
     * Advances the state machine. Called once per client tick with the current wall-clock time.
     */
    public void tick(long nowMillis) {
        switch (phase) {
            case MEASURING_BEFORE:
            case MEASURING_AFTER:
                sample(nowMillis);
                break;
            case COOLDOWN:
                if (nowMillis >= cooldownUntilMillis) {
                    phase = Phase.IDLE;
                }
                break;
            default:
                break;
        }
    }

    private void sample(long nowMillis) {
        if (lastSampleMillis == 0L) {
            lastSampleMillis = nowMillis;
        }
        if (nowMillis - lastSampleMillis < SAMPLE_INTERVAL_MILLIS) {
            return;
        }
        lastSampleMillis = nowMillis;

        double fps = monitor.averageFps();
        if (fps <= 0d) {
            return;
        }
        MetricSeries target = phase == Phase.MEASURING_BEFORE ? beforeSeries : afterSeries;
        target.add(nowMillis, fps);
        samplesInCurrentPhase++;

        if (samplesInCurrentPhase >= config.measurementSeconds()) {
            finishPhase();
        }
    }

    private void finishPhase() {
        if (phase == Phase.MEASURING_BEFORE) {
            applyPending();
            return;
        }
        evaluate();
    }

    private void applyPending() {
        Recommendation suggestion = inflight;
        if (suggestion == null) {
            phase = Phase.IDLE;
            return;
        }
        String before = mapper.read(suggestion.setting());
        boolean applied = mapper.write(suggestion.setting(), suggestion.proposedValue());
        if (!applied) {
            phase = Phase.IDLE;
            lastMessage = "Could not apply the change. The option refused the value.";
            inflight = null;
            return;
        }
        String after = mapper.read(suggestion.setting());
        lastAppliedSetting = suggestion.setting();
        config.recordChange(new SettingChange(suggestion.setting(),
                before == null ? "?" : before,
                after == null ? suggestion.proposedValue() : after,
                SettingChange.Source.SMART_AUTO,
                System.currentTimeMillis()));
        mapper.saveOptions();
        afterSeries.clear();
        samplesInCurrentPhase = 0;
        lastSampleMillis = 0L;
        phase = Phase.MEASURING_AFTER;
        lastMessage = "Applied. Measuring for " + config.measurementSeconds() + " seconds.";
    }

    private void evaluate() {
        PerformanceAnalysis analysis = PerformanceAnalyzer.compare(beforeSeries, afterSeries);
        lastAnalysis = analysis;

        if (analysis.isMeasuredRegression() && config.autoRevertFailedChanges()) {
            int restored = mapper.restore(beforeSnapshot);
            markLastReverted();
            phase = Phase.REVERTED;
            lastMessage = "The change made things worse (" + analysis.describe() + "). "
                    + restored + " setting(s) restored.";
        } else if (analysis.isMeasuredImprovement()) {
            phase = Phase.EVALUATED;
            lastMessage = "Measured improvement: " + analysis.describe() + ".";
        } else {
            phase = Phase.EVALUATED;
            lastMessage = "No measurable change (" + analysis.describe() + "). "
                    + "The setting is left as you approved it; you can undo it at any time.";
        }
        pending = null;
        inflight = null;
        enterCooldown();
    }

    private void markLastReverted() {
        List<SettingChange> history = config.changeHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            SettingChange change = history.get(i);
            if (!change.reverted()) {
                change.markReverted();
                return;
            }
        }
    }

    private void enterCooldown() {
        cooldownUntilMillis = System.currentTimeMillis() + config.cooldownSeconds() * 1000L;
        if (phase != Phase.REVERTED) {
            phase = Phase.COOLDOWN;
        }
    }

    /** The setting most recently changed by Smart Auto, for display. */
    public Setting lastAppliedSetting() {
        return lastAppliedSetting;
    }
}
