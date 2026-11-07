package com.tech65pro.vortexmc.config;

/**
 * A complete set of graphics settings that VortexMC can apply.
 *
 * <p>Profiles are deliberately expressed in <em>semantic</em> terms rather than in Minecraft
 * types so that the whole profile model lives in the platform-independent part of the mod and
 * can be unit-tested without a game running. The client-side layer translates the semantic
 * values into the concrete Minecraft option values.</p>
 *
 * <p>Any field left at its "unset" sentinel means "do not touch this option", which lets a
 * profile change one thing without silently resetting the player's other preferences.</p>
 */
public final class OptimizationProfile {

    /** Sentinel meaning "leave the option exactly as the user configured it". */
    public static final int UNSET = Integer.MIN_VALUE;
    public static final double UNSET_DOUBLE = Double.NaN;

    /** Particle density levels, named to match Minecraft's own option values. */
    public enum ParticleLevel { ALL, DECREASED, MINIMAL }

    /** Cloud rendering levels, named to match Minecraft's own option values. */
    public enum CloudLevel { OFF, FAST, FANCY }

    private String id = "custom";
    private String displayName = "Custom";
    private boolean builtIn;

    private ParticleLevel particles;
    private CloudLevel clouds;

    private int renderDistance = UNSET;
    private int simulationDistance = UNSET;
    private double entityDistanceScaling = UNSET_DOUBLE;
    private int maxFps = UNSET;
    private Boolean smoothLighting;
    private Boolean vsync;
    private int mipmapLevels = UNSET;
    private int biomeBlendRadius = UNSET;
    private int guiScale = UNSET;
    private double gamma = UNSET_DOUBLE;
    private int cloudRenderDistance = UNSET;
    private String inactivityFpsLimit = null;

    public OptimizationProfile() {
    }

    public OptimizationProfile(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    // ------------------------------------------------------------------ accessors

    public String id() {
        return id;
    }

    public OptimizationProfile id(String value) {
        this.id = value;
        return this;
    }

    public String displayName() {
        return displayName;
    }

    public OptimizationProfile displayName(String value) {
        this.displayName = value;
        return this;
    }

    public boolean builtIn() {
        return builtIn;
    }

    public OptimizationProfile builtIn(boolean value) {
        this.builtIn = value;
        return this;
    }

    public ParticleLevel particles() {
        return particles;
    }

    public OptimizationProfile particles(ParticleLevel value) {
        this.particles = value;
        return this;
    }

    public CloudLevel clouds() {
        return clouds;
    }

    public OptimizationProfile clouds(CloudLevel value) {
        this.clouds = value;
        return this;
    }

    public int renderDistance() {
        return renderDistance;
    }

    public OptimizationProfile renderDistance(int value) {
        this.renderDistance = value;
        return this;
    }

    public int simulationDistance() {
        return simulationDistance;
    }

    public OptimizationProfile simulationDistance(int value) {
        this.simulationDistance = value;
        return this;
    }

    public double entityDistanceScaling() {
        return entityDistanceScaling;
    }

    public OptimizationProfile entityDistanceScaling(double value) {
        this.entityDistanceScaling = value;
        return this;
    }

    public int maxFps() {
        return maxFps;
    }

    public OptimizationProfile maxFps(int value) {
        this.maxFps = value;
        return this;
    }

    public Boolean smoothLighting() {
        return smoothLighting;
    }

    public OptimizationProfile smoothLighting(Boolean value) {
        this.smoothLighting = value;
        return this;
    }

    public boolean hasSmoothLighting() {
        return smoothLighting != null;
    }

    public Boolean vsync() {
        return vsync;
    }

    public OptimizationProfile vsync(Boolean value) {
        this.vsync = value;
        return this;
    }

    public boolean hasVsync() {
        return vsync != null;
    }

    public int mipmapLevels() {
        return mipmapLevels;
    }

    public OptimizationProfile mipmapLevels(int value) {
        this.mipmapLevels = value;
        return this;
    }

    public int biomeBlendRadius() {
        return biomeBlendRadius;
    }

    public OptimizationProfile biomeBlendRadius(int value) {
        this.biomeBlendRadius = value;
        return this;
    }

    public int guiScale() {
        return guiScale;
    }

    public OptimizationProfile guiScale(int value) {
        this.guiScale = value;
        return this;
    }

    public double gamma() {
        return gamma;
    }

    public OptimizationProfile gamma(double value) {
        this.gamma = value;
        return this;
    }

    public int cloudRenderDistance() {
        return cloudRenderDistance;
    }

    public OptimizationProfile cloudRenderDistance(int value) {
        this.cloudRenderDistance = value;
        return this;
    }

    public String inactivityFpsLimit() {
        return inactivityFpsLimit;
    }

    public OptimizationProfile inactivityFpsLimit(String value) {
        this.inactivityFpsLimit = value;
        return this;
    }

    // ------------------------------------------------------------------ helpers

    public boolean hasRenderDistance() {
        return renderDistance != UNSET;
    }

    public boolean hasSimulationDistance() {
        return simulationDistance != UNSET;
    }

    public boolean hasEntityDistanceScaling() {
        return !Double.isNaN(entityDistanceScaling);
    }

    public boolean hasMaxFps() {
        return maxFps != UNSET;
    }

    public boolean hasMipmapLevels() {
        return mipmapLevels != UNSET;
    }

    public boolean hasBiomeBlendRadius() {
        return biomeBlendRadius != UNSET;
    }

    public boolean hasGuiScale() {
        return guiScale != UNSET;
    }

    public boolean hasGamma() {
        return !Double.isNaN(gamma);
    }

    public boolean hasCloudRenderDistance() {
        return cloudRenderDistance != UNSET;
    }

    public boolean hasInactivityFpsLimit() {
        return inactivityFpsLimit != null;
    }

    /** Number of settings this profile would actually change. Used by the GUI summary. */
    public int definedSettingCount() {
        int count = 0;
        if (hasSmoothLighting()) count++;
        if (hasVsync()) count++;
        if (particles != null) count++;
        if (clouds != null) count++;
        if (hasRenderDistance()) count++;
        if (hasSimulationDistance()) count++;
        if (hasEntityDistanceScaling()) count++;
        if (hasMaxFps()) count++;
        if (hasMipmapLevels()) count++;
        if (hasBiomeBlendRadius()) count++;
        if (hasGuiScale()) count++;
        if (hasGamma()) count++;
        if (hasCloudRenderDistance()) count++;
        if (hasInactivityFpsLimit()) count++;
        return count;
    }

    public OptimizationProfile copy() {
        OptimizationProfile copy = new OptimizationProfile(id, displayName);
        copy.builtIn = builtIn;
        copy.particles = particles;
        copy.clouds = clouds;
        copy.renderDistance = renderDistance;
        copy.simulationDistance = simulationDistance;
        copy.entityDistanceScaling = entityDistanceScaling;
        copy.maxFps = maxFps;
        copy.smoothLighting = smoothLighting;
        copy.vsync = vsync;
        copy.mipmapLevels = mipmapLevels;
        copy.biomeBlendRadius = biomeBlendRadius;
        copy.guiScale = guiScale;
        copy.gamma = gamma;
        copy.cloudRenderDistance = cloudRenderDistance;
        copy.inactivityFpsLimit = inactivityFpsLimit;
        return copy;
    }

    @Override
    public String toString() {
        return "OptimizationProfile[" + id + " (" + displayName + "), " + definedSettingCount() + " settings]";
    }
}
