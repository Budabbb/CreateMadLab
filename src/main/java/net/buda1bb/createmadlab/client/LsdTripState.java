package net.buda1bb.createmadlab.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class LsdTripState {
    private static final float TARGET_SMOOTHING = 0.08F;
    private static final float CAMERA_SMOOTHING = 0.15F;
    private static final float CAMERA_DECAY = 0.10F;
    private static final float CAMERA_MOTION_SCALE = 18.0F;
    private static final float SPIKE_SMOOTHING = 0.18F;
    private static final float VERY_HIGH_DOSE_THRESHOLD = 2.10F;
    private static final float ULTRA_HIGH_DOSE_THRESHOLD = 3.10F;
    private static final int MIN_SPIKE_DURATION_TICKS = 28;
    private static final int MAX_SPIKE_DURATION_TICKS = 54;
    private static final int MIN_LONG_SPIKE_DURATION_TICKS = 110;
    private static final int MAX_LONG_SPIKE_DURATION_TICKS = 220;
    private static final int MIN_PEAK_SPIKE_DURATION_TICKS = 150;
    private static final int MAX_PEAK_SPIKE_DURATION_TICKS = 300;
    private static final int MIN_SPIKE_DELAY_TICKS = 140;
    private static final int MAX_SPIKE_DELAY_TICKS = 280;
    private static final int MIN_PEAK_SPIKE_DELAY_TICKS = 4;
    private static final int MAX_PEAK_SPIKE_DELAY_TICKS = 14;
    private static final RandomSource SPIKE_RANDOM = RandomSource.create();

    private static boolean active;
    private static int totalTicks;
    private static int remainingTicks;
    private static float timeTicks;
    private static float previousTimeTicks;
    private static float targetIntensity;
    private static float smoothedIntensity;
    private static float previousSmoothedIntensity;
    private static float smoothedCameraMotion;
    private static float previousCameraMotion;
    private static float previousYaw;
    private static float previousPitch;
    private static float doseStrength = 1.0F;
    private static float potencyScale = 1.0F;
    private static float spikeTargetStrength;
    private static float smoothedSpikeStrength;
    private static float previousSmoothedSpikeStrength;
    private static int spikeTicksRemaining;
    private static int spikeDurationTicks;
    private static int ticksUntilNextSpike;
    private static boolean longUltraSpike;
    private static float spikeRotationBias;
    private static float spikeForwardBias;
    private static float spikeLeftBias;
    private static boolean hasCameraBaseline;

    private LsdTripState() {
    }

    public static void start(int durationTicks, float strength) {
        resume(durationTicks, durationTicks, strength);
    }

    public static void resume(int totalDurationTicks, int remainingDurationTicks, float strength) {
        totalTicks = Math.max(1, totalDurationTicks);
        remainingTicks = Math.max(0, Math.min(remainingDurationTicks, totalTicks));
        active = remainingTicks > 0;
        float elapsed = 1.0F - remainingTicks / (float) totalTicks;
        timeTicks = totalTicks - remainingTicks;
        previousTimeTicks = timeTicks;
        smoothedCameraMotion = 0.0F;
        previousCameraMotion = 0.0F;
        doseStrength = Math.max(0.55F, strength);
        potencyScale = Mth.clamp(0.90F + doseStrength * 0.30F, 0.92F, 1.76F);
        targetIntensity = Mth.clamp(computeEnvelope(elapsed) * potencyScale, 0.0F, 1.0F);
        smoothedIntensity = targetIntensity;
        previousSmoothedIntensity = targetIntensity;
        spikeTargetStrength = 0.0F;
        smoothedSpikeStrength = 0.0F;
        previousSmoothedSpikeStrength = 0.0F;
        spikeTicksRemaining = 0;
        spikeDurationTicks = 0;
        ticksUntilNextSpike = isVeryHighDose() ? sampleNextSpikeDelay(0.0F) : Integer.MAX_VALUE;
        longUltraSpike = false;
        spikeRotationBias = 0.0F;
        spikeForwardBias = 0.0F;
        spikeLeftBias = 0.0F;
        hasCameraBaseline = false;
    }

    public static void deactivate() {
        active = false;
        totalTicks = 0;
        remainingTicks = 0;
        timeTicks = 0.0F;
        previousTimeTicks = 0.0F;
        targetIntensity = 0.0F;
        smoothedIntensity = 0.0F;
        previousSmoothedIntensity = 0.0F;
        smoothedCameraMotion = 0.0F;
        previousCameraMotion = 0.0F;
        doseStrength = 1.0F;
        potencyScale = 1.0F;
        spikeTargetStrength = 0.0F;
        smoothedSpikeStrength = 0.0F;
        previousSmoothedSpikeStrength = 0.0F;
        spikeTicksRemaining = 0;
        spikeDurationTicks = 0;
        ticksUntilNextSpike = 0;
        longUltraSpike = false;
        spikeRotationBias = 0.0F;
        spikeForwardBias = 0.0F;
        spikeLeftBias = 0.0F;
        hasCameraBaseline = false;
    }

    public static void tick(Minecraft minecraft) {
        if (!active) {
            return;
        }

        previousSmoothedIntensity = smoothedIntensity;
        previousCameraMotion = smoothedCameraMotion;
        previousTimeTicks = timeTicks;
        previousSmoothedSpikeStrength = smoothedSpikeStrength;

        if (remainingTicks > 0) {
            remainingTicks--;
        }
        timeTicks += 1.0F;

        float elapsed = 1.0F - remainingTicks / (float) totalTicks;
        targetIntensity = Mth.clamp(computeEnvelope(elapsed) * potencyScale, 0.0F, 1.0F);
        smoothedIntensity += (targetIntensity - smoothedIntensity) * TARGET_SMOOTHING;
        updateSpikeState(elapsed);
        smoothedSpikeStrength += (spikeTargetStrength - smoothedSpikeStrength) * SPIKE_SMOOTHING;

        updateCameraMotion(minecraft);

        if (remainingTicks <= 0 && smoothedIntensity < 0.01F && smoothedSpikeStrength < 0.01F) {
            deactivate();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float getTargetIntensity() {
        return targetIntensity;
    }

    public static float getDoseStrength() {
        return doseStrength;
    }

    public static float getSmoothedIntensity(float partialTick) {
        return Mth.lerp(partialTick, previousSmoothedIntensity, smoothedIntensity);
    }

    public static float getCameraMotion(float partialTick) {
        return Mth.lerp(partialTick, previousCameraMotion, smoothedCameraMotion);
    }

    public static float getSpikeStrength(float partialTick) {
        return Mth.lerp(partialTick, previousSmoothedSpikeStrength, smoothedSpikeStrength);
    }

    public static float getUltraDoseFactor() {
        return smoothstep(ULTRA_HIGH_DOSE_THRESHOLD, 3.45F, doseStrength);
    }

    public static float getSpikeRotationBias() {
        return spikeRotationBias;
    }

    public static float getSpikeForwardBias() {
        return spikeForwardBias;
    }

    public static float getSpikeLeftBias() {
        return spikeLeftBias;
    }

    public static float getTripPhase(float partialTick) {
        if (totalTicks <= 0) {
            return 0.0F;
        }

        float interpolatedRemaining = Math.max(0.0F, remainingTicks - partialTick);
        return Mth.clamp(1.0F - interpolatedRemaining / totalTicks, 0.0F, 1.0F);
    }

    public static float getTimeSeconds(float partialTick) {
        return Mth.lerp(partialTick, previousTimeTicks, timeTicks) / 20.0F;
    }

    public static float getPeakPulse(float partialTick) {
        float intensity = getSmoothedIntensity(partialTick);
        if (intensity < 0.70F) {
            return 0.0F;
        }

        float seconds = getTimeSeconds(partialTick);
        float phase = getTripPhase(partialTick);
        float cycleA = 0.5F + 0.5F * Mth.sin(seconds * 0.54F + Mth.sin(seconds * 0.17F) * 1.15F);
        float cycleB = 0.5F + 0.5F * Mth.sin(seconds * 0.79F + 1.9F);
        float combined = cycleA * 0.68F + cycleB * 0.32F;
        float pulse = smoothstep(0.78F, 0.97F, combined);
        float gate = smoothstep(0.70F, 0.85F, intensity);
        float fadeGate = 1.0F - smoothstep(0.92F, 1.0F, phase);
        return pulse * gate * (0.55F + 0.45F * fadeGate);
    }

    public static void updateCameraMotion(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            hasCameraBaseline = false;
            smoothedCameraMotion += (0.0F - smoothedCameraMotion) * CAMERA_DECAY;
            return;
        }

        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();

        if (!hasCameraBaseline) {
            previousYaw = yaw;
            previousPitch = pitch;
            hasCameraBaseline = true;
            smoothedCameraMotion += (0.0F - smoothedCameraMotion) * CAMERA_DECAY;
            return;
        }

        float deltaYaw = Mth.wrapDegrees(yaw - previousYaw);
        float deltaPitch = pitch - previousPitch;
        previousYaw = yaw;
        previousPitch = pitch;

        float rawMotion = Mth.clamp((float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch) / CAMERA_MOTION_SCALE, 0.0F, 1.0F);
        smoothedCameraMotion += (rawMotion - smoothedCameraMotion) * CAMERA_SMOOTHING;
    }

    private static void updateSpikeState(float elapsed) {
        float veryHighDose = smoothstep(VERY_HIGH_DOSE_THRESHOLD, 2.55F, doseStrength);
        float ultraDose = getUltraDoseFactor();
        float phaseGate = smoothstep(0.20F, 0.32F, elapsed) * (1.0F - smoothstep(0.76F, 0.90F, elapsed));
        float intensityGate = smoothstep(0.58F, 0.86F, targetIntensity);
        float peakSpikeWindow = ultraDose * smoothstep(0.34F, 0.42F, elapsed) * (1.0F - smoothstep(0.60F, 0.72F, elapsed));
        float peakSpikeIntensity = smoothstep(0.70F, 0.92F, targetIntensity);
        float peakSpikeField = peakSpikeWindow * peakSpikeIntensity;
        float availability = Mth.clamp(
                veryHighDose * phaseGate * intensityGate * (0.52F + 0.16F * ultraDose)
                        + peakSpikeField * 0.92F,
                0.0F,
                1.0F
        );

        if (availability < 0.05F) {
            spikeTargetStrength = 0.0F;
            return;
        }

        if (spikeTicksRemaining <= 0) {
            if (peakSpikeField > 0.45F && ultraDose > 0.30F) {
                beginSpike(availability, peakSpikeField);
            } else {
                if (ticksUntilNextSpike > 0) {
                    ticksUntilNextSpike--;
                }
                if (ticksUntilNextSpike <= 0) {
                    beginSpike(availability, peakSpikeField);
                }
            }
        }

        if (spikeTicksRemaining > 0) {
            spikeTicksRemaining--;
            if (peakSpikeField > 0.55F && ultraDose > 0.30F && !longUltraSpike) {
                longUltraSpike = true;
                spikeTicksRemaining = Math.max(spikeTicksRemaining, MIN_PEAK_SPIKE_DURATION_TICKS / 2);
                spikeDurationTicks = Math.max(spikeDurationTicks, spikeTicksRemaining + Mth.floor(55.0F + 48.0F * ultraDose));
            }
            float progress = 1.0F - spikeTicksRemaining / (float) Math.max(1, spikeDurationTicks);
            float envelope = longUltraSpike
                    ? smoothstep(0.0F, 0.10F, progress) * (1.0F - smoothstep(0.82F, 1.0F, progress))
                    : Mth.sin(progress * Mth.PI);
            float tremor = 0.88F + 0.12F * Mth.sin(timeTicks * (0.33F + 0.08F * ultraDose) + spikeRotationBias * 2.4F);
            float spikePotency = 0.72F + 0.18F * ultraDose + 0.58F * peakSpikeField;
            float longSpikeBoost = longUltraSpike ? 1.08F + 0.14F * ultraDose + 0.28F * peakSpikeField : 1.0F;
            spikeTargetStrength = Mth.clamp(envelope * availability * tremor * spikePotency * longSpikeBoost, 0.0F, 1.0F);
            return;
        }

        longUltraSpike = false;
        spikeTargetStrength = 0.0F;
    }

    private static void beginSpike(float availability, float peakSpikeField) {
        float ultraDose = getUltraDoseFactor();
        boolean peakSpikeMode = peakSpikeField > 0.45F && ultraDose > 0.30F;
        longUltraSpike = peakSpikeMode || (ultraDose > 0.45F && SPIKE_RANDOM.nextFloat() < (0.10F + 0.26F * ultraDose));
        int minDuration = MIN_SPIKE_DURATION_TICKS + Mth.floor(8.0F * ultraDose);
        int maxDuration = MAX_SPIKE_DURATION_TICKS + Mth.floor(12.0F * ultraDose);
        if (longUltraSpike) {
            if (peakSpikeMode) {
                minDuration = MIN_PEAK_SPIKE_DURATION_TICKS + Mth.floor(28.0F * ultraDose);
                maxDuration = MAX_PEAK_SPIKE_DURATION_TICKS + Mth.floor(70.0F * ultraDose);
            } else {
                minDuration = MIN_LONG_SPIKE_DURATION_TICKS + Mth.floor(12.0F * ultraDose);
                maxDuration = MAX_LONG_SPIKE_DURATION_TICKS + Mth.floor(28.0F * ultraDose);
            }
        }
        spikeDurationTicks = randomRange(minDuration, maxDuration);
        spikeTicksRemaining = spikeDurationTicks;
        float longSpikeBias = longUltraSpike
                ? (peakSpikeMode ? 0.88F + 0.40F * ultraDose : 0.42F + 0.24F * ultraDose)
                : 0.0F;
        spikeRotationBias = (SPIKE_RANDOM.nextBoolean() ? 1.0F : -1.0F) * (0.95F + SPIKE_RANDOM.nextFloat() * (0.85F + 0.80F * ultraDose + 0.80F * longSpikeBias));
        spikeForwardBias = randomSignedFloat() * (0.18F + 0.10F * availability + 0.14F * ultraDose + 0.16F * longSpikeBias);
        spikeLeftBias = randomSignedFloat() * (0.18F + 0.14F * availability + 0.18F * ultraDose + 0.20F * longSpikeBias);
        ticksUntilNextSpike = sampleNextSpikeDelay(peakSpikeField);
    }

    private static boolean isVeryHighDose() {
        return doseStrength >= VERY_HIGH_DOSE_THRESHOLD;
    }

    private static int sampleNextSpikeDelay(float peakSpikeField) {
        if (peakSpikeField > 0.45F && getUltraDoseFactor() > 0.30F) {
            return randomRange(MIN_PEAK_SPIKE_DELAY_TICKS, MAX_PEAK_SPIKE_DELAY_TICKS);
        }

        float veryHighDose = smoothstep(VERY_HIGH_DOSE_THRESHOLD, 2.55F, doseStrength);
        float ultraDose = getUltraDoseFactor();
        int minDelay = Math.max(90, MIN_SPIKE_DELAY_TICKS - Mth.floor(veryHighDose * 18.0F) - Mth.floor(ultraDose * 24.0F));
        int maxDelay = Math.max(minDelay + 26, MAX_SPIKE_DELAY_TICKS - Mth.floor(veryHighDose * 30.0F) - Mth.floor(ultraDose * 36.0F));
        return randomRange(minDelay, maxDelay);
    }

    private static int randomRange(int min, int max) {
        return min + SPIKE_RANDOM.nextInt(max - min + 1);
    }

    private static float randomSignedFloat() {
        return SPIKE_RANDOM.nextFloat() * 2.0F - 1.0F;
    }

    private static float computeEnvelope(float elapsed) {
        float rampUp = smoothstep(0.0F, 0.25F, elapsed);
        float rampDown = 1.0F - smoothstep(0.75F, 1.0F, elapsed);
        return Mth.clamp(rampUp * rampDown, 0.0F, 1.0F);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
