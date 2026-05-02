package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.minecraft.util.Mth;

public final class MorphineTripState {
    private static final float TARGET_SMOOTHING = 0.08F;
    private static final float DEBT_SMOOTHING = 0.10F;
    private static final float PULSE_SMOOTHING = 0.12F;

    private static boolean active;
    private static int totalTicks;
    private static int remainingTicks;
    private static int unstableHp;
    private static float timeTicks;
    private static float previousTimeTicks;
    private static float targetIntensity;
    private static float smoothedIntensity;
    private static float previousSmoothedIntensity;
    private static float targetDebtIntensity;
    private static float smoothedDebtIntensity;
    private static float previousSmoothedDebtIntensity;
    private static float heartPulse;
    private static float previousHeartPulse;

    private MorphineTripState() {
    }

    public static void sync(int totalDurationTicks, int remainingDurationTicks, int syncedUnstableHp) {
        totalTicks = Math.max(1, totalDurationTicks);
        remainingTicks = Mth.clamp(remainingDurationTicks, 0, totalTicks);
        unstableHp = Math.max(0, syncedUnstableHp);
        active = remainingTicks > 0 || unstableHp > 0;

        float syncedTimeTicks = totalTicks - remainingTicks;
        boolean shouldReset = previousTimeTicks == 0.0F && timeTicks == 0.0F && smoothedIntensity == 0.0F && smoothedDebtIntensity == 0.0F;
        if (!active) {
            deactivate();
            return;
        }

        if (shouldReset) {
            timeTicks = syncedTimeTicks;
            previousTimeTicks = syncedTimeTicks;
            targetIntensity = MorphineEffectsManager.computeEffectIntensity(totalTicks, syncedTimeTicks);
            smoothedIntensity = targetIntensity;
            previousSmoothedIntensity = targetIntensity;
            targetDebtIntensity = MorphineEffectsManager.computeDebtIntensity(unstableHp);
            smoothedDebtIntensity = targetDebtIntensity;
            previousSmoothedDebtIntensity = targetDebtIntensity;
            heartPulse = computeHeartPulse(syncedTimeTicks / 20.0F, smoothedIntensity, targetDebtIntensity, computeTripPhase());
            previousHeartPulse = heartPulse;
            return;
        }

        timeTicks = syncedTimeTicks;
        previousTimeTicks = syncedTimeTicks;
        targetIntensity = MorphineEffectsManager.computeEffectIntensity(totalTicks, syncedTimeTicks);
        targetDebtIntensity = MorphineEffectsManager.computeDebtIntensity(unstableHp);
    }

    public static void deactivate() {
        active = false;
        totalTicks = 0;
        remainingTicks = 0;
        unstableHp = 0;
        timeTicks = 0.0F;
        previousTimeTicks = 0.0F;
        targetIntensity = 0.0F;
        smoothedIntensity = 0.0F;
        previousSmoothedIntensity = 0.0F;
        targetDebtIntensity = 0.0F;
        smoothedDebtIntensity = 0.0F;
        previousSmoothedDebtIntensity = 0.0F;
        heartPulse = 0.0F;
        previousHeartPulse = 0.0F;
    }

    public static void tick() {
        if (!active) {
            return;
        }

        previousTimeTicks = timeTicks;
        previousSmoothedIntensity = smoothedIntensity;
        previousSmoothedDebtIntensity = smoothedDebtIntensity;
        previousHeartPulse = heartPulse;

        if (remainingTicks > 0) {
            remainingTicks--;
        }
        timeTicks += 1.0F;

        targetIntensity = MorphineEffectsManager.computeEffectIntensity(totalTicks, timeTicks);
        targetDebtIntensity = MorphineEffectsManager.computeDebtIntensity(unstableHp);
        smoothedIntensity += (targetIntensity - smoothedIntensity) * TARGET_SMOOTHING;
        smoothedDebtIntensity += (targetDebtIntensity - smoothedDebtIntensity) * DEBT_SMOOTHING;

        float pulseTarget = computeHeartPulse(timeTicks / 20.0F, smoothedIntensity, smoothedDebtIntensity, computeTripPhase());
        heartPulse += (pulseTarget - heartPulse) * PULSE_SMOOTHING;

        active = remainingTicks > 0 || unstableHp > 0 || smoothedIntensity >= 0.01F || smoothedDebtIntensity >= 0.01F || heartPulse >= 0.01F;
        if (!active) {
            deactivate();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float getTargetIntensity() {
        return targetIntensity;
    }

    public static float getTargetDebtIntensity() {
        return targetDebtIntensity;
    }

    public static int getUnstableHp() {
        return unstableHp;
    }

    public static float getSmoothedIntensity(float partialTick) {
        return Mth.lerp(partialTick, previousSmoothedIntensity, smoothedIntensity);
    }

    public static float getSmoothedDebtIntensity(float partialTick) {
        return Mth.lerp(partialTick, previousSmoothedDebtIntensity, smoothedDebtIntensity);
    }

    public static float getTimeSeconds(float partialTick) {
        return Mth.lerp(partialTick, previousTimeTicks, timeTicks) / 20.0F;
    }

    public static float getHeartPulse(float partialTick) {
        return Mth.lerp(partialTick, previousHeartPulse, heartPulse);
    }

    public static boolean hasActiveMorphineWindow() {
        return remainingTicks > 0;
    }

    public static float getTripPhase(float partialTick) {
        if (totalTicks <= 0) {
            return 0.0F;
        }

        float interpolatedRemaining = Math.max(0.0F, remainingTicks - partialTick);
        return Mth.clamp(1.0F - interpolatedRemaining / totalTicks, 0.0F, 1.0F);
    }

    private static float computeTripPhase() {
        if (totalTicks <= 0) {
            return 0.0F;
        }

        return Mth.clamp(1.0F - remainingTicks / (float) totalTicks, 0.0F, 1.0F);
    }

    private static float computeHeartPulse(float seconds, float intensity, float debtIntensity, float phase) {
        float gate = smoothstep(0.06F, 0.24F, intensity);
        float debtGate = smoothstep(0.10F, 0.56F, debtIntensity);
        float comeupEndPhase = MorphineEffectsManager.getComeupEndPhase();
        float peakEndPhase = MorphineEffectsManager.getPeakEndPhase();
        float onset = 1.0F - smoothstep(comeupEndPhase * 0.42F, comeupEndPhase, phase);
        float pulseFade = 1.0F - smoothstep(peakEndPhase, Math.min(1.0F, peakEndPhase + 0.22F), phase);
        float rushWave = 0.5F + 0.5F * Mth.sin(seconds * 1.95F - 0.35F);
        float undertow = 0.5F + 0.5F * Mth.sin(seconds * 0.92F + 0.8F);
        float breathing = 0.5F + 0.5F * Mth.sin(seconds * 0.60F + undertow * 0.85F);
        float waveform = Mth.clamp(rushWave * (0.70F + 0.30F * breathing), 0.0F, 1.0F);
        waveform = 0.30F + 0.70F * waveform;
        float rushBoost = onset * gate * (0.22F + 0.30F * rushWave);
        float morphinePulse = waveform * gate * (0.62F + 0.38F * pulseFade);
        float debtPulse = waveform * debtGate * 0.82F;
        return Mth.clamp(Math.max(morphinePulse + rushBoost, debtPulse), 0.0F, 1.0F);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / Math.max(edge1 - edge0, 0.0001F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
