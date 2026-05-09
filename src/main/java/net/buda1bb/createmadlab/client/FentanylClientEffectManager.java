package net.buda1bb.createmadlab.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.logging.LogUtils;
import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.IOException;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID, value = Dist.CLIENT)
public final class FentanylClientEffectManager {
    public static final float BASE_DIM_STRENGTH = 0.12F;
    public static final float MAX_DIM_STRENGTH = 0.86F;
    public static final float GREEN_AQUA_TINT_STRENGTH = 0.62F;
    public static final float VIGNETTE_STRENGTH = 1.62F;
    public static final float BLUR_STRENGTH = 1.42F;
    public static final float BLOOM_HAZE_STRENGTH = 0.66F;
    public static final float CAMERA_DRIFT_STRENGTH = 1.25F;
    public static final float CAMERA_INERTIA_STRENGTH = 1.0F;
    public static final float CAMERA_SWAY_STRENGTH = 0.88F;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FENTANYL_EXTRACT_PROGRAM_NAME = CreateMadLab.MOD_ID + ":fentanyl/extract_bright";
    private static final String FENTANYL_BLUR_PROGRAM_NAME = CreateMadLab.MOD_ID + ":fentanyl/blur";
    private static final String FENTANYL_COMPOSITE_PROGRAM_NAME = CreateMadLab.MOD_ID + ":fentanyl/composite";
    private static final String BLIT_PROGRAM_NAME = "minecraft:blit";

    private static TextureTarget swapTarget;
    private static TextureTarget highlightsTarget;
    private static TextureTarget blurTargetA;
    private static TextureTarget blurTargetB;
    private static TextureTarget historyTarget;
    private static PostPass extractPass;
    private static PostPass blurHorizontalPass;
    private static PostPass blurVerticalPass;
    private static PostPass compositePass;
    private static PostPass blitPass;
    private static PostPass historyCopyPass;
    private static int lastWindowWidth = -1;
    private static int lastWindowHeight = -1;
    private static int lastTargetWidth = -1;
    private static int lastTargetHeight = -1;
    private static boolean historyPrimed;
    private static boolean previousSmoothCamera;
    private static boolean smoothCameraStateCaptured;

    private FentanylClientEffectManager() {
    }

    public static void activate(int remainingTicks, int totalTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        int safeTotalTicks = totalTicks > 0 ? totalTicks : FentanylEffectsManager.TOTAL_DURATION_TICKS;
        int safeRemainingTicks = Mth.clamp(remainingTicks, 0, safeTotalTicks);
        FentanylTripState.sync(safeTotalTicks, safeRemainingTicks);
        enableSmoothCamera(minecraft);
        lastWindowWidth = minecraft.getWindow().getWidth();
        lastWindowHeight = minecraft.getWindow().getHeight();
        lastTargetWidth = minecraft.getMainRenderTarget().width;
        lastTargetHeight = minecraft.getMainRenderTarget().height;
        historyPrimed = false;
        clearHistoryTarget();
    }

    public static void deactivate() {
        restoreSmoothCamera(Minecraft.getInstance());
        FentanylTripState.deactivate();
        closeProcessor();
        lastWindowWidth = -1;
        lastWindowHeight = -1;
        lastTargetWidth = -1;
        lastTargetHeight = -1;
        historyPrimed = false;
    }

    public static boolean isActive() {
        return FentanylTripState.isActive();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!FentanylTripState.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            closeProcessor();
            return;
        }

        if (minecraft.player.isDeadOrDying()) {
            deactivate();
            return;
        }

        enableSmoothCamera(minecraft);
        if (!minecraft.isPaused()) {
            FentanylTripState.tick(minecraft);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!FentanylTripState.isActive() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!ensureProcessorReady(minecraft)) {
            return;
        }

        float partialTick = ClientRenderTime.partialTick(event.getPartialTick());
        updateUniforms(minecraft, partialTick);
        extractPass.process(partialTick);
        blurHorizontalPass.process(partialTick);
        blurVerticalPass.process(partialTick);
        compositePass.process(partialTick);
        blitPass.process(partialTick);
        historyCopyPass.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(true);
        historyPrimed = true;
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!FentanylTripState.isActive()) {
            return;
        }

        float alpha = FentanylTripState.getBlackoutAlpha(ClientRenderTime.partialTick(event.getPartialTick()));
        if (alpha <= 0.01F) {
            return;
        }

        int packedAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        event.getGuiGraphics().fill(
                RenderType.guiOverlay(),
                0,
                0,
                event.getGuiGraphics().guiWidth(),
                event.getGuiGraphics().guiHeight(),
                1000,
                packedAlpha << 24
        );
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!FentanylTripState.isActive()) {
            return;
        }

        float partialTick = (float) event.getPartialTick();
        float elapsedTicks = FentanylTripState.getElapsedTicks(partialTick);
        float seconds = FentanylTripState.getTimeSeconds(partialTick);
        float intensity = FentanylTripState.getSmoothedIntensity(partialTick);
        float finalFade = FentanylTripState.getFinalFade(partialTick);
        float driftGate = smoothstep(80.0F, FentanylEffectsManager.FINAL_FADE_START_TICKS - 180.0F, elapsedTicks) * (1.0F - 0.86F * finalFade);
        float swayGate = smoothstep(180.0F, FentanylEffectsManager.FINAL_FADE_START_TICKS - 240.0F, elapsedTicks) * (1.0F - 0.90F * finalFade);
        float droopGate = smoothstep(240.0F, FentanylEffectsManager.FINAL_FADE_START_TICKS, elapsedTicks);
        float lagYaw = FentanylTripState.getLagYawOffset(partialTick) * CAMERA_INERTIA_STRENGTH;
        float lagPitch = FentanylTripState.getLagPitchOffset(partialTick) * CAMERA_INERTIA_STRENGTH;
        float yawDrift = CAMERA_DRIFT_STRENGTH * driftGate
                * (Mth.sin(seconds * 0.16F + 0.7F) * 0.72F + Mth.sin(seconds * 0.055F + 1.9F) * 0.42F);
        float pitchSway = CAMERA_SWAY_STRENGTH * swayGate
                * (Mth.sin(seconds * 0.20F + 1.4F) * 0.34F + Mth.sin(seconds * 0.075F) * 0.22F);
        float pitchDroop = droopGate * (0.65F + 4.85F * intensity) * (1.0F - 0.30F * finalFade);
        float roll = CAMERA_SWAY_STRENGTH * swayGate
                * (Mth.sin(seconds * 0.13F + 2.2F) * 0.58F + Mth.sin(seconds * 0.045F) * 0.22F);

        event.setYaw(event.getYaw() + lagYaw + yawDrift);
        event.setPitch(Mth.clamp(event.getPitch() + lagPitch + pitchDroop + pitchSway, -90.0F, 90.0F));
        event.setRoll(event.getRoll() + roll);
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        deactivate();
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && event.getEntity() == minecraft.player) {
            deactivate();
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && event.getEntity() == minecraft.player) {
            deactivate();
        }
    }

    private static boolean ensureProcessorReady(Minecraft minecraft) {
        if (minecraft.level == null) {
            return false;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        int windowWidth = minecraft.getWindow().getWidth();
        int windowHeight = minecraft.getWindow().getHeight();
        if (swapTarget != null && highlightsTarget != null && blurTargetA != null && blurTargetB != null
                && historyTarget != null && extractPass != null && blurHorizontalPass != null
                && blurVerticalPass != null && compositePass != null && blitPass != null
                && historyCopyPass != null && windowWidth == lastWindowWidth
                && windowHeight == lastWindowHeight && lastTargetWidth == mainTarget.width
                && lastTargetHeight == mainTarget.height) {
            return true;
        }

        closeProcessor();

        try {
            swapTarget = createCompatibleTarget(mainTarget);
            highlightsTarget = createCompatibleTarget(mainTarget);
            blurTargetA = createCompatibleTarget(mainTarget);
            blurTargetB = createCompatibleTarget(mainTarget);
            historyTarget = createCompatibleTarget(mainTarget);

            extractPass = new PostPass(minecraft.getResourceManager(), FENTANYL_EXTRACT_PROGRAM_NAME, mainTarget, highlightsTarget, false);
            blurHorizontalPass = new PostPass(minecraft.getResourceManager(), FENTANYL_BLUR_PROGRAM_NAME, highlightsTarget, blurTargetA, false);
            blurVerticalPass = new PostPass(minecraft.getResourceManager(), FENTANYL_BLUR_PROGRAM_NAME, blurTargetA, blurTargetB, false);
            compositePass = new PostPass(minecraft.getResourceManager(), FENTANYL_COMPOSITE_PROGRAM_NAME, mainTarget, swapTarget, false);
            compositePass.addAuxAsset("BloomSampler", blurTargetB::getColorTextureId, blurTargetB.width, blurTargetB.height);
            compositePass.addAuxAsset("HistorySampler", historyTarget::getColorTextureId, historyTarget.width, historyTarget.height);
            blitPass = new PostPass(minecraft.getResourceManager(), BLIT_PROGRAM_NAME, swapTarget, mainTarget, false);
            historyCopyPass = new PostPass(minecraft.getResourceManager(), BLIT_PROGRAM_NAME, swapTarget, historyTarget, false);

            Matrix4f orthoMatrix = new Matrix4f().setOrtho(0.0F, (float) mainTarget.width, 0.0F, (float) mainTarget.height, 0.1F, 1000.0F);
            extractPass.setOrthoMatrix(orthoMatrix);
            blurHorizontalPass.setOrthoMatrix(orthoMatrix);
            blurVerticalPass.setOrthoMatrix(orthoMatrix);
            compositePass.setOrthoMatrix(orthoMatrix);
            blitPass.setOrthoMatrix(orthoMatrix);
            historyCopyPass.setOrthoMatrix(orthoMatrix);

            lastWindowWidth = windowWidth;
            lastWindowHeight = windowHeight;
            lastTargetWidth = mainTarget.width;
            lastTargetHeight = mainTarget.height;
            historyPrimed = false;
            clearHistoryTarget();
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Failed to initialize fentanyl overdose post processor", exception);
            closeProcessor();
            return false;
        }
    }

    private static TextureTarget createCompatibleTarget(RenderTarget mainTarget) {
        TextureTarget target = new TextureTarget(mainTarget.width, mainTarget.height, true, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        if (mainTarget.isStencilEnabled()) {
            target.enableStencil();
        }
        return target;
    }

    private static void updateUniforms(Minecraft minecraft, float partialTick) {
        float elapsedTicks = FentanylTripState.getElapsedTicks(partialTick);
        float seconds = FentanylTripState.getTimeSeconds(partialTick);
        float intensity = FentanylTripState.getSmoothedIntensity(partialTick);
        float finalFade = FentanylTripState.getFinalFade(partialTick);
        float blackout = FentanylTripState.getBlackoutAlpha(partialTick);
        float cameraMotion = FentanylTripState.getCameraMotion(partialTick);
        float fadeIn = FentanylEffectsManager.computeEffectFadeIn(elapsedTicks);
        float fadedIntensity = intensity * fadeIn;
        float stageTwo = smoothstep(200.0F, 700.0F, elapsedTicks);
        float stageThree = smoothstep(700.0F, FentanylEffectsManager.FINAL_FADE_START_TICKS, elapsedTicks);
        float dimStrength = fadeIn * Mth.clamp(BASE_DIM_STRENGTH + (MAX_DIM_STRENGTH - BASE_DIM_STRENGTH)
                * Mth.clamp(fadedIntensity * 0.86F + finalFade * 0.24F, 0.0F, 1.0F), 0.0F, 1.0F);
        float tintStrength = fadeIn * Mth.clamp(GREEN_AQUA_TINT_STRENGTH * (0.12F + 0.66F * fadedIntensity), 0.0F, 0.76F);
        float desaturation = fadeIn * Mth.clamp(0.10F + 0.48F * fadedIntensity + 0.16F * stageThree + 0.12F * finalFade, 0.0F, 0.88F);
        float vignetteStrength = fadeIn * Mth.clamp(VIGNETTE_STRENGTH * (0.32F + 0.66F * fadedIntensity + 0.34F * stageThree + 0.40F * finalFade), 0.0F, 1.85F);
        float blurStrength = fadeIn * Mth.clamp(BLUR_STRENGTH * (0.10F + 0.50F * fadedIntensity + 0.25F * stageTwo + 0.28F * stageThree), 0.0F, 1.78F);
        float bloomStrength = fadeIn * Mth.clamp(BLOOM_HAZE_STRENGTH * (0.10F + 0.36F * fadedIntensity + 0.14F * stageThree) * (1.0F - 0.38F * finalFade), 0.0F, 0.78F);
        float hazeStrength = fadeIn * Mth.clamp(0.12F + 0.48F * fadedIntensity + 0.22F * stageThree, 0.0F, 0.92F) * (1.0F - 0.28F * finalFade);
        float distortionStrength = fadeIn * Mth.clamp(0.04F + 0.14F * fadedIntensity + 0.08F * cameraMotion, 0.0F, 0.22F) * (1.0F - 0.72F * finalFade);
        float bloomDriver = Mth.clamp(bloomStrength + hazeStrength * 0.18F, 0.0F, 1.0F);
        float bloomThreshold = Mth.lerp(bloomDriver, 0.72F, 0.48F);
        float bloomKnee = Mth.lerp(bloomDriver, 0.16F, 0.34F);
        float bloomBlurScale = 1.10F + blurStrength * 1.85F + hazeStrength * 0.48F;
        float resolutionX = minecraft.getWindow().getWidth();
        float resolutionY = minecraft.getWindow().getHeight();

        setUniform(extractPass.getEffect(), "Threshold", bloomThreshold);
        setUniform(extractPass.getEffect(), "Knee", bloomKnee);

        setUniform(blurHorizontalPass.getEffect(), "BlurDir", 1.0F, 0.0F);
        setUniform(blurHorizontalPass.getEffect(), "BlurScale", bloomBlurScale);
        setUniform(blurVerticalPass.getEffect(), "BlurDir", 0.0F, 1.0F);
        setUniform(blurVerticalPass.getEffect(), "BlurScale", bloomBlurScale);

        EffectInstance composite = compositePass.getEffect();
        setUniform(composite, "Time", seconds);
        setUniform(composite, "Intensity", fadedIntensity);
        setUniform(composite, "Resolution", resolutionX, resolutionY);
        setUniform(composite, "CameraMotion", cameraMotion);
        setUniform(composite, "TintStrength", tintStrength);
        setUniform(composite, "DimStrength", dimStrength);
        setUniform(composite, "DesaturationStrength", desaturation);
        setUniform(composite, "BlurStrength", blurStrength);
        setUniform(composite, "VignetteStrength", vignetteStrength);
        setUniform(composite, "BloomStrength", bloomStrength);
        setUniform(composite, "HazeStrength", hazeStrength);
        setUniform(composite, "DistortionStrength", distortionStrength);
        setUniform(composite, "BlackoutStrength", blackout);
        setUniform(composite, "FinalFade", finalFade);
        setUniform(composite, "HistoryStrength", historyPrimed ? fadeIn * Mth.clamp((0.05F + 0.18F * fadedIntensity) * (1.0F - 0.55F * finalFade), 0.0F, 0.24F) : 0.0F);
    }

    private static void clearHistoryTarget() {
        if (historyTarget == null) {
            return;
        }

        historyTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        historyTarget.clear(Minecraft.ON_OSX);
    }

    private static void closeProcessor() {
        if (extractPass != null) {
            extractPass.close();
            extractPass = null;
        }
        if (blurHorizontalPass != null) {
            blurHorizontalPass.close();
            blurHorizontalPass = null;
        }
        if (blurVerticalPass != null) {
            blurVerticalPass.close();
            blurVerticalPass = null;
        }
        if (compositePass != null) {
            compositePass.close();
            compositePass = null;
        }
        if (blitPass != null) {
            blitPass.close();
            blitPass = null;
        }
        if (historyCopyPass != null) {
            historyCopyPass.close();
            historyCopyPass = null;
        }
        if (swapTarget != null) {
            swapTarget.destroyBuffers();
            swapTarget = null;
        }
        if (highlightsTarget != null) {
            highlightsTarget.destroyBuffers();
            highlightsTarget = null;
        }
        if (blurTargetA != null) {
            blurTargetA.destroyBuffers();
            blurTargetA = null;
        }
        if (blurTargetB != null) {
            blurTargetB.destroyBuffers();
            blurTargetB = null;
        }
        if (historyTarget != null) {
            historyTarget.destroyBuffers();
            historyTarget = null;
        }
        historyPrimed = false;
    }

    private static void enableSmoothCamera(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }

        if (!smoothCameraStateCaptured) {
            previousSmoothCamera = minecraft.options.smoothCamera;
            smoothCameraStateCaptured = true;
        }

        minecraft.options.smoothCamera = true;
    }

    private static void restoreSmoothCamera(Minecraft minecraft) {
        if (minecraft != null && smoothCameraStateCaptured) {
            minecraft.options.smoothCamera = previousSmoothCamera;
        }

        smoothCameraStateCaptured = false;
    }

    private static void setUniform(EffectInstance effect, String name, float value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setUniform(EffectInstance effect, String name, float x, float y) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / Math.max(edge1 - edge0, 0.0001F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
