package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class HeroinTripState {
    private static final float TARGET_SMOOTHING = 0.06F;
    private static final float CAMERA_SMOOTHING = 0.10F;
    private static final float CAMERA_DECAY = 0.10F;
    private static final float CAMERA_MOTION_SCALE = 18.0F;
    private static final float WAVE_SMOOTHING = 0.08F;

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
    private static float sedationWave;
    private static float previousSedationWave;
    private static boolean hasCameraBaseline;

    private HeroinTripState() {
    }

    public static void start(int durationTicks) {
        resume(durationTicks, durationTicks);
    }

    public static void resume(int totalDurationTicks, int remainingDurationTicks) {
        totalTicks = Math.max(1, totalDurationTicks);
        remainingTicks = Mth.clamp(remainingDurationTicks, 0, totalTicks);
        active = remainingTicks > 0;

        timeTicks = totalTicks - remainingTicks;
        previousTimeTicks = timeTicks;
        targetIntensity = HeroinEffectsManager.computeEffectIntensity(totalTicks, timeTicks);
        smoothedIntensity = targetIntensity;
        previousSmoothedIntensity = targetIntensity;
        smoothedCameraMotion = 0.0F;
        previousCameraMotion = 0.0F;
        sedationWave = computeSedationWave(timeTicks / 20.0F, smoothedIntensity);
        previousSedationWave = sedationWave;
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
        sedationWave = 0.0F;
        previousSedationWave = 0.0F;
        hasCameraBaseline = false;
    }

    public static void tick(Minecraft minecraft) {
        if (!active) {
            return;
        }

        previousSmoothedIntensity = smoothedIntensity;
        previousCameraMotion = smoothedCameraMotion;
        previousTimeTicks = timeTicks;
        previousSedationWave = sedationWave;

        if (remainingTicks > 0) {
            remainingTicks--;
        }
        timeTicks += 1.0F;

        targetIntensity = HeroinEffectsManager.computeEffectIntensity(totalTicks, timeTicks);
        smoothedIntensity += (targetIntensity - smoothedIntensity) * TARGET_SMOOTHING;
        updateCameraMotion(minecraft);

        float waveTarget = computeSedationWave(timeTicks / 20.0F, smoothedIntensity);
        sedationWave += (waveTarget - sedationWave) * WAVE_SMOOTHING;

        if (remainingTicks <= 0 && smoothedIntensity < 0.01F && sedationWave < 0.01F && smoothedCameraMotion < 0.01F) {
            deactivate();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float getTargetIntensity() {
        return targetIntensity;
    }

    public static float getSmoothedIntensity(float partialTick) {
        return Mth.lerp(partialTick, previousSmoothedIntensity, smoothedIntensity);
    }

    public static float getTimeSeconds(float partialTick) {
        return Mth.lerp(partialTick, previousTimeTicks, timeTicks) / 20.0F;
    }

    public static float getCameraMotion(float partialTick) {
        return Mth.lerp(partialTick, previousCameraMotion, smoothedCameraMotion);
    }

    public static float getSedationWave(float partialTick) {
        return Mth.lerp(partialTick, previousSedationWave, sedationWave);
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

    private static float computeSedationWave(float seconds, float intensity) {
        float primary = 0.5F + 0.5F * Mth.sin(seconds * 0.45F + Mth.sin(seconds * 0.12F + 0.9F) * 0.8F);
        float secondary = 0.5F + 0.5F * Mth.sin(seconds * 0.21F + 1.4F);
        float combined = primary * 0.72F + secondary * 0.28F;
        float wave = smoothstep(0.18F, 1.0F, combined);
        float gate = smoothstep(0.55F, 0.80F, intensity);
        return wave * gate;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
