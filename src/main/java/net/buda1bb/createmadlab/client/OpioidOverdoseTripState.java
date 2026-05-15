package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.effect.UniversalOverdoseHandler;
import net.minecraft.util.Mth;

public final class OpioidOverdoseTripState {
    private static final float INTENSITY_SMOOTHING = 0.07F;

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
    private static boolean fadeOutMode;
    private static float fadeOutStartIntensity;
    private static float fadeOutStartBlackoutAlpha;

    private OpioidOverdoseTripState() {
    }

    public static void sync(int totalDurationTicks, int remainingDurationTicks) {
        int safeTotalTicks = Math.max(1, totalDurationTicks);
        int safeRemainingTicks = Mth.clamp(remainingDurationTicks, 0, safeTotalTicks);
        float syncedTimeTicks = safeTotalTicks - safeRemainingTicks;
        boolean shouldReset = !active || totalTicks != safeTotalTicks || timeTicks > syncedTimeTicks + 5.0F;

        active = true;
        fadeOutMode = false;
        fadeOutStartIntensity = 0.0F;
        fadeOutStartBlackoutAlpha = 0.0F;
        totalTicks = safeTotalTicks;
        remainingTicks = safeRemainingTicks;
        timeTicks = syncedTimeTicks;
        previousTimeTicks = syncedTimeTicks;
        targetIntensity = UniversalOverdoseHandler.computeVisualIntensity(timeTicks);
        blackoutAlpha = UniversalOverdoseHandler.computeBlackoutAlpha(timeTicks);
        previousBlackoutAlpha = blackoutAlpha;

        if (shouldReset) {
            smoothedIntensity = targetIntensity;
            previousSmoothedIntensity = targetIntensity;
            return;
        }

        previousSmoothedIntensity = smoothedIntensity;
    }

    public static void syncFadeOut(int totalDurationTicks, int remainingDurationTicks, float startIntensity, float startBlackoutAlpha) {
        int safeTotalTicks = Math.max(1, totalDurationTicks);
        int safeRemainingTicks = Mth.clamp(remainingDurationTicks, 0, safeTotalTicks);

        active = safeRemainingTicks > 0;
        fadeOutMode = true;
        fadeOutStartIntensity = Mth.clamp(startIntensity, 0.0F, 1.0F);
        fadeOutStartBlackoutAlpha = Mth.clamp(startBlackoutAlpha, 0.0F, 1.0F);
        totalTicks = safeTotalTicks;
        remainingTicks = safeRemainingTicks;
        timeTicks = safeTotalTicks - safeRemainingTicks;
        previousTimeTicks = timeTicks;
        targetIntensity = computeFadeOutTargetIntensity();
        smoothedIntensity = targetIntensity;
        previousSmoothedIntensity = targetIntensity;
        blackoutAlpha = computeFadeOutBlackoutAlpha();
        previousBlackoutAlpha = blackoutAlpha;
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
        fadeOutMode = false;
        fadeOutStartIntensity = 0.0F;
        fadeOutStartBlackoutAlpha = 0.0F;
    }

    public static void tick() {
        if (!active) {
            return;
        }

        previousTimeTicks = timeTicks;
        previousSmoothedIntensity = smoothedIntensity;
        previousBlackoutAlpha = blackoutAlpha;

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
        } else {
            targetIntensity = UniversalOverdoseHandler.computeVisualIntensity(timeTicks);
            smoothedIntensity += (targetIntensity - smoothedIntensity) * INTENSITY_SMOOTHING;
            blackoutAlpha = UniversalOverdoseHandler.computeBlackoutAlpha(timeTicks);
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
        return UniversalOverdoseHandler.computeFinalFade(getElapsedTicks(partialTick));
    }

    private static float computeFadeOutTargetIntensity() {
        return fadeOutStartIntensity * getFadeOutProgress();
    }

    private static float computeFadeOutBlackoutAlpha() {
        return fadeOutStartBlackoutAlpha * getFadeOutProgress();
    }

    private static float getFadeOutProgress() {
        return totalTicks <= 0 ? 0.0F : Mth.clamp(remainingTicks / (float) totalTicks, 0.0F, 1.0F);
    }
}
