package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FentanylTripState {
    private static final float INTENSITY_SMOOTHING = 0.07F;
    private static final float CAMERA_MOTION_SMOOTHING = 0.12F;
    private static final float CAMERA_LAG_SMOOTHING = 0.16F;
    private static final float CAMERA_MOTION_SCALE = 14.0F;

    private static boolean active;
    private static int totalTicks;
    private static int remainingTicks;
    private static float timeTicks;
    private static float previousTimeTicks;
    private static float targetIntensity;
    private static float smoothedIntensity;
    private static float previousSmoothedIntensity;
    private static float blackoutAlpha;
    private static float previousBlackoutAlpha;
    private static float smoothedCameraMotion;
    private static float previousCameraMotion;
    private static float lagYawOffset;
    private static float previousLagYawOffset;
    private static float lagPitchOffset;
    private static float previousLagPitchOffset;
    private static float previousYaw;
    private static float previousPitch;
    private static boolean fadeOutMode;
    private static float fadeOutStartIntensity;
    private static float fadeOutStartBlackoutAlpha;
    private static float visualStrength = 1.0F;
    private static boolean hasCameraBaseline;

    private FentanylTripState() {
    }

    public static void sync(int totalDurationTicks, int remainingDurationTicks) {
        sync(totalDurationTicks, remainingDurationTicks, 1.0F);
    }

    public static void sync(int totalDurationTicks, int remainingDurationTicks, float syncedVisualStrength) {
        int safeTotalTicks = Math.max(1, totalDurationTicks);
        int safeRemainingTicks = Mth.clamp(remainingDurationTicks, 0, safeTotalTicks);
        float syncedTimeTicks = safeTotalTicks - safeRemainingTicks;
        boolean shouldReset = !active || totalTicks != safeTotalTicks || timeTicks > syncedTimeTicks + 5.0F;

        active = true;
        fadeOutMode = false;
        fadeOutStartIntensity = 0.0F;
        fadeOutStartBlackoutAlpha = 0.0F;
        visualStrength = sanitizeVisualStrength(syncedVisualStrength);
        totalTicks = safeTotalTicks;
        remainingTicks = safeRemainingTicks;
        timeTicks = syncedTimeTicks;
        previousTimeTicks = syncedTimeTicks;
        targetIntensity = computeStackedVisualIntensity(timeTicks);
        blackoutAlpha = FentanylEffectsManager.computeBlackoutAlpha(timeTicks);
        previousBlackoutAlpha = blackoutAlpha;

        if (shouldReset) {
            smoothedIntensity = targetIntensity;
            previousSmoothedIntensity = targetIntensity;
            smoothedCameraMotion = 0.0F;
            previousCameraMotion = 0.0F;
            lagYawOffset = 0.0F;
            previousLagYawOffset = 0.0F;
            lagPitchOffset = 0.0F;
            previousLagPitchOffset = 0.0F;
            hasCameraBaseline = false;
            return;
        }

        previousSmoothedIntensity = smoothedIntensity;
        previousCameraMotion = smoothedCameraMotion;
        previousLagYawOffset = lagYawOffset;
        previousLagPitchOffset = lagPitchOffset;
    }

    public static void syncFadeOut(int totalDurationTicks, int remainingDurationTicks, float startIntensity, float startBlackoutAlpha) {
        syncFadeOut(totalDurationTicks, remainingDurationTicks, startIntensity, startBlackoutAlpha, 1.0F);
    }

    public static void syncFadeOut(int totalDurationTicks, int remainingDurationTicks, float startIntensity,
                                   float startBlackoutAlpha, float syncedVisualStrength) {
        int safeTotalTicks = Math.max(1, totalDurationTicks);
        int safeRemainingTicks = Mth.clamp(remainingDurationTicks, 0, safeTotalTicks);

        active = safeRemainingTicks > 0;
        fadeOutMode = true;
        fadeOutStartIntensity = Mth.clamp(startIntensity, 0.0F, 1.0F);
        fadeOutStartBlackoutAlpha = Mth.clamp(startBlackoutAlpha, 0.0F, 1.0F);
        visualStrength = sanitizeVisualStrength(syncedVisualStrength);
        totalTicks = safeTotalTicks;
        remainingTicks = safeRemainingTicks;
        timeTicks = safeTotalTicks - safeRemainingTicks;
        previousTimeTicks = timeTicks;
        targetIntensity = computeFadeOutTargetIntensity();
        smoothedIntensity = targetIntensity;
        previousSmoothedIntensity = targetIntensity;
        blackoutAlpha = computeFadeOutBlackoutAlpha();
        previousBlackoutAlpha = blackoutAlpha;
        smoothedCameraMotion = 0.0F;
        previousCameraMotion = 0.0F;
        lagYawOffset = 0.0F;
        previousLagYawOffset = 0.0F;
        lagPitchOffset = 0.0F;
        previousLagPitchOffset = 0.0F;
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
        blackoutAlpha = 0.0F;
        previousBlackoutAlpha = 0.0F;
        smoothedCameraMotion = 0.0F;
        previousCameraMotion = 0.0F;
        lagYawOffset = 0.0F;
        previousLagYawOffset = 0.0F;
        lagPitchOffset = 0.0F;
        previousLagPitchOffset = 0.0F;
        fadeOutMode = false;
        fadeOutStartIntensity = 0.0F;
        fadeOutStartBlackoutAlpha = 0.0F;
        visualStrength = 1.0F;
        hasCameraBaseline = false;
    }

    public static void tick(Minecraft minecraft) {
        if (!active) {
            return;
        }

        previousTimeTicks = timeTicks;
        previousSmoothedIntensity = smoothedIntensity;
        previousBlackoutAlpha = blackoutAlpha;
        previousCameraMotion = smoothedCameraMotion;
        previousLagYawOffset = lagYawOffset;
        previousLagPitchOffset = lagPitchOffset;

        if (remainingTicks > 0) {
            remainingTicks--;
        }
        timeTicks = Math.min(totalTicks, timeTicks + 1.0F);

        if (fadeOutMode) {
            if (remainingTicks <= 0) {
                deactivate();
                return;
            }

            targetIntensity = computeFadeOutTargetIntensity();
            smoothedIntensity = targetIntensity;
            blackoutAlpha = computeFadeOutBlackoutAlpha();
            smoothedCameraMotion = 0.0F;
            lagYawOffset = 0.0F;
            lagPitchOffset = 0.0F;
        } else {
            targetIntensity = computeStackedVisualIntensity(timeTicks);
            smoothedIntensity += (targetIntensity - smoothedIntensity) * INTENSITY_SMOOTHING;
            blackoutAlpha = FentanylEffectsManager.computeBlackoutAlpha(timeTicks);
            updateCameraLag(minecraft);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isFadeOutMode() {
        return fadeOutMode;
    }

    public static float getSmoothedIntensity(float partialTick) {
        return Mth.lerp(partialTick, previousSmoothedIntensity, smoothedIntensity);
    }

    public static float getTargetIntensity() {
        return targetIntensity;
    }

    public static float getTimeSeconds(float partialTick) {
        return getElapsedTicks(partialTick) / 20.0F;
    }

    public static float getElapsedTicks(float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, previousTimeTicks, timeTicks), 0.0F, Math.max(1.0F, totalTicks));
    }

    public static float getBlackoutAlpha(float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, previousBlackoutAlpha, blackoutAlpha), 0.0F, 1.0F);
    }

    public static float getFinalFade(float partialTick) {
        return FentanylEffectsManager.computeFinalFade(getElapsedTicks(partialTick));
    }

    public static float getCameraMotion(float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, previousCameraMotion, smoothedCameraMotion), 0.0F, 1.0F);
    }

    public static float getLagYawOffset(float partialTick) {
        return Mth.lerp(partialTick, previousLagYawOffset, lagYawOffset);
    }

    public static float getLagPitchOffset(float partialTick) {
        return Mth.lerp(partialTick, previousLagPitchOffset, lagPitchOffset);
    }

    private static void updateCameraLag(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            hasCameraBaseline = false;
            smoothedCameraMotion += (0.0F - smoothedCameraMotion) * CAMERA_MOTION_SMOOTHING;
            lagYawOffset += (0.0F - lagYawOffset) * CAMERA_LAG_SMOOTHING;
            lagPitchOffset += (0.0F - lagPitchOffset) * CAMERA_LAG_SMOOTHING;
            return;
        }

        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        if (!hasCameraBaseline) {
            previousYaw = yaw;
            previousPitch = pitch;
            hasCameraBaseline = true;
            smoothedCameraMotion += (0.0F - smoothedCameraMotion) * CAMERA_MOTION_SMOOTHING;
            return;
        }

        float deltaYaw = Mth.wrapDegrees(yaw - previousYaw);
        float deltaPitch = pitch - previousPitch;
        previousYaw = yaw;
        previousPitch = pitch;

        float finalFade = FentanylEffectsManager.computeFinalFade(timeTicks);
        float inertiaGate = smoothstep(160.0F, FentanylEffectsManager.FINAL_FADE_START_TICKS, timeTicks) * (1.0F - 0.82F * finalFade);
        float targetYawLag = Mth.clamp(-deltaYaw * 0.22F * inertiaGate, -3.1F, 3.1F);
        float targetPitchLag = Mth.clamp(-deltaPitch * 0.18F * inertiaGate, -2.4F, 2.4F);
        lagYawOffset += (targetYawLag - lagYawOffset) * CAMERA_LAG_SMOOTHING;
        lagPitchOffset += (targetPitchLag - lagPitchOffset) * CAMERA_LAG_SMOOTHING;

        float rawMotion = Mth.clamp((float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch) / CAMERA_MOTION_SCALE, 0.0F, 1.0F);
        smoothedCameraMotion += (rawMotion - smoothedCameraMotion) * CAMERA_MOTION_SMOOTHING;
    }

    private static float computeFadeOutTargetIntensity() {
        return fadeOutStartIntensity * getFadeOutProgress();
    }

    private static float computeStackedVisualIntensity(float elapsedTicks) {
        return FentanylEffectsManager.computeVisualIntensity(elapsedTicks) * visualStrength;
    }

    private static float sanitizeVisualStrength(float strength) {
        return Mth.clamp(strength, 0.0F, 3.0F);
    }

    private static float computeFadeOutBlackoutAlpha() {
        return fadeOutStartBlackoutAlpha * getFadeOutProgress();
    }

    private static float getFadeOutProgress() {
        return totalTicks <= 0 ? 0.0F : Mth.clamp(remainingTicks / (float) totalTicks, 0.0F, 1.0F);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / Math.max(edge1 - edge0, 0.0001F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
