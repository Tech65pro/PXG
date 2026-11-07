package com.tech65pro.vortexmc.client;

import com.tech65pro.vortexmc.core.FrameTimeStats;
import com.tech65pro.vortexmc.core.MetricSeries;
import com.tech65pro.vortexmc.core.PerformanceAnalyzer;
import net.minecraft.client.MinecraftClient;

/**
 * Collects real frame-time data.
 *
 * <p>Sampling happens from a Fabric HUD element, so it runs once per rendered frame and sees the
 * same frame times the player experiences. Nothing is estimated or interpolated: if the game has
 * not rendered a frame yet, there is no data and the GUI says so.</p>
 *
 * <p>The per-frame samples feed a rolling {@link FrameTimeStats} window; a one-hertz rollup feeds
 * {@link MetricSeries} histories that the trend analysis and the dashboard graphs read.</p>
 */
public final class PerformanceMonitor {

    /** Roughly ten seconds at 60 fps, sixty at 10 fps. */
    private static final int FRAME_WINDOW = 600;
    /** Five minutes of one-hertz samples. */
    private static final int HISTORY_WINDOW = 300;

    private final MinecraftClient client;
    private final FrameTimeStats frameStats = new FrameTimeStats(FRAME_WINDOW);
    private final MetricSeries fpsHistory = new MetricSeries(HISTORY_WINDOW);
    private final MetricSeries onePercentHistory = new MetricSeries(HISTORY_WINDOW);
    private final MetricSeries frameTimeHistory = new MetricSeries(HISTORY_WINDOW);

    private long lastFrameNanos;
    private long secondStartNanos;
    private int framesInCurrentSecond;
    private float lastFrameTimeMillis;
    private boolean measuring;

    public PerformanceMonitor(MinecraftClient client) {
        this.client = client;
    }

    /** Called once per rendered frame. Cheap enough to be called from the render path. */
    public void onFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            long elapsed = now - lastFrameNanos;
            // Guard against absurd values from a clock adjustment or a suspended process.
            if (elapsed > 0L && elapsed < 10_000_000_000L) {
                float millis = elapsed / 1_000_000f;
                lastFrameTimeMillis = millis;
                frameStats.add(millis);
                framesInCurrentSecond++;
            }
        }
        lastFrameNanos = now;

        if (secondStartNanos == 0L) {
            secondStartNanos = now;
            return;
        }
        long span = now - secondStartNanos;
        if (span >= 1_000_000_000L) {
            double seconds = span / 1_000_000_000d;
            double fps = framesInCurrentSecond / seconds;
            long timestamp = System.currentTimeMillis();
            fpsHistory.add(timestamp, fps);
            onePercentHistory.add(timestamp, frameStats.onePercentLowFps());
            frameTimeHistory.add(timestamp, frameStats.averageFrameTimeMillis());
            framesInCurrentSecond = 0;
            secondStartNanos = now;
            measuring = true;
        }
    }

    /** Drops everything collected so far, for example after a settings change. */
    public void reset() {
        frameStats.clear();
        fpsHistory.clear();
        onePercentHistory.clear();
        frameTimeHistory.clear();
        lastFrameNanos = 0L;
        secondStartNanos = 0L;
        framesInCurrentSecond = 0;
        lastFrameTimeMillis = 0f;
        measuring = false;
    }

    public boolean hasFrameData() {
        return frameStats.hasSamples();
    }

    /** True once at least one complete one-second rollup has been recorded. */
    public boolean hasHistory() {
        return fpsHistory.hasSamples();
    }

    public int frameSampleCount() {
        return frameStats.sampleCount();
    }

    public int historySampleCount() {
        return fpsHistory.sampleCount();
    }

    public float lastFrameTimeMillis() {
        return lastFrameTimeMillis;
    }

    /** Frame rate implied by the most recent frame. */
    public float instantFps() {
        return frameStats.instantFps();
    }

    /** Average frame rate over the rolling window. */
    public float averageFps() {
        return frameStats.averageFps();
    }

    public float medianFrameTimeMillis() {
        return frameStats.medianFrameTimeMillis();
    }

    public float worstFrameTimeMillis() {
        return frameStats.worstFrameTimeMillis();
    }

    public float frameTimeSpreadMillis() {
        return frameStats.standardDeviationMillis();
    }

    public float onePercentLowFps() {
        return frameStats.onePercentLowFps();
    }

    public float zeroPointOnePercentLowFps() {
        return frameStats.zeroPointOnePercentLowFps();
    }

    /**
     * The frame counter Minecraft itself shows. Shown alongside VortexMC's own measurement so the
     * two can be compared rather than silently disagreeing.
     */
    public int vanillaFps() {
        return client == null ? 0 : client.getCurrentFps();
    }

    public MetricSeries fpsHistory() {
        return fpsHistory;
    }

    public MetricSeries onePercentHistory() {
        return onePercentHistory;
    }

    public MetricSeries frameTimeHistory() {
        return frameTimeHistory;
    }

    /** Signed change in average FPS per second over the history window. */
    public double fpsTrendPerSecond() {
        return fpsHistory.slopePerSecond();
    }

    /** Relative drop in average FPS across the history window, as a positive percentage. */
    public double detectedDropPercent() {
        return PerformanceAnalyzer.detectDrop(fpsHistory, 15.0);
    }

    /** True when the slowest 1% of frames are far below the median, i.e. visible stutter. */
    public boolean isStuttering() {
        return PerformanceAnalyzer.isStuttering(frameStats, 0.7);
    }

    /** Mean average-FPS over the whole retained history. */
    public double sessionAverageFps() {
        return fpsHistory.average();
    }

    public double sessionLowestAverageFps() {
        return fpsHistory.min();
    }

    public double sessionHighestAverageFps() {
        return fpsHistory.max();
    }
}
