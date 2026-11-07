package com.tech65pro.vortexmc.client;

/**
 * Heap diagnostics.
 *
 * <p>VortexMC deliberately never calls {@link System#gc()}. Forcing a collection to make a number
 * look tidier stalls the game for exactly as long as the collector needs, which is the opposite
 * of what a performance mod is for. Everything reported here is read straight from
 * {@link Runtime}.</p>
 */
public final class MemoryDiagnostics {

    public static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private final Runtime runtime = Runtime.getRuntime();

    public long maxHeapBytes() {
        return runtime.maxMemory();
    }

    public long committedHeapBytes() {
        return runtime.totalMemory();
    }

    public long freeHeapBytes() {
        return runtime.freeMemory();
    }

    /** Heap currently holding live objects: committed minus free. */
    public long usedHeapBytes() {
        long used = runtime.totalMemory() - runtime.freeMemory();
        return Math.max(0L, used);
    }

    /** Used heap as a percentage of the maximum heap, 0..100. */
    public double usedPercent() {
        long max = maxHeapBytes();
        if (max <= 0L) {
            return 0d;
        }
        return usedHeapBytes() * 100d / max;
    }

    /** Used heap as a percentage of what the JVM has currently committed. */
    public double usedOfCommittedPercent() {
        long committed = committedHeapBytes();
        if (committed <= 0L) {
            return 0d;
        }
        return usedHeapBytes() * 100d / committed;
    }

    /** How much of the maximum heap is not even committed yet. */
    public long uncommittedHeapBytes() {
        long uncommitted = maxHeapBytes() - committedHeapBytes();
        return Math.max(0L, uncommitted);
    }

    public int availableProcessors() {
        return runtime.availableProcessors();
    }

    /** A short human-readable assessment. */
    public String assessment() {
        double used = usedPercent();
        if (used < 55d) {
            return "Heap usage is comfortable.";
        }
        if (used < 75d) {
            return "Heap usage is moderate. No action needed.";
        }
        if (used < 90d) {
            return "Heap usage is high. Consider a larger -Xmx if you have spare RAM.";
        }
        return "Heap usage is critical. The JVM will spend increasing time collecting.";
    }

    /**
     * Whether the current allocation looks like a vanilla-style heap. Used only to show a
     * suggestion; VortexMC never changes the allocation itself.
     */
    public boolean looksUndersized() {
        long max = maxHeapBytes();
        return max > 0L && max < 2L * BYTES_PER_MEGABYTE;
    }

    public static double toMegabytes(long bytes) {
        return bytes / (double) BYTES_PER_MEGABYTE;
    }

    public static String formatMegabytes(long bytes) {
        return String.format("%.0f MB", toMegabytes(bytes));
    }
}
