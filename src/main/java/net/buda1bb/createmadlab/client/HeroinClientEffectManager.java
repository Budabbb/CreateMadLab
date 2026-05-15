package net.buda1bb.createmadlab.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.logging.LogUtils;
import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HeroinClientEffectManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String HEROIN_EXTRACT_PROGRAM_NAME = CreateMadLab.MOD_ID + ":heroin/extract_bright";
    private static final String HEROIN_BLUR_PROGRAM_NAME = CreateMadLab.MOD_ID + ":heroin/blur";
    private static final String HEROIN_COMPOSITE_PROGRAM_NAME = CreateMadLab.MOD_ID + ":heroin/composite";
    private static final String BLIT_PROGRAM_NAME = "minecraft:blit";
    private static final String SMOOTH_CAMERA_OWNER = "heroin";
    private static final float FOV_BREATHING_DEGREES = 2.15F;

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
    private static boolean historyPrimed;
    private static int lastWindowWidth = -1;
    private static int lastWindowHeight = -1;
    private static int lastTargetWidth = -1;
    private static int lastTargetHeight = -1;
    private static boolean previousSmoothCamera;
    private static boolean smoothCameraStateCaptured;
    private static boolean cinematicCameraEnabled;

    private HeroinClientEffectManager() {
    }

    public static void activate(int durationTicks) {
        activate(durationTicks, HeroinEffectsManager.getTotalDuration(), 1.0F, true, false);
    }

    public static void activate(int durationTicks, int totalTicks, float visualStrength, boolean cinematicCamera) {
        activate(durationTicks, totalTicks, visualStrength, cinematicCamera, false);
    }

    public static void activate(int durationTicks, int totalTicks, float visualStrength, boolean cinematicCamera, boolean fadeOut) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        int safeTotalTicks = totalTicks > 0 ? totalTicks : HeroinEffectsManager.getTotalDuration();
        int remainingTicks = durationTicks > 0 ? Mth.clamp(durationTicks, 0, safeTotalTicks) : safeTotalTicks;
        if (fadeOut) {
            HeroinTripState.resumeFadeOut(safeTotalTicks, remainingTicks, visualStrength);
        } else if (HeroinTripState.isActive()) {
            HeroinTripState.extend(safeTotalTicks, remainingTicks, visualStrength);
        } else {
            HeroinTripState.resume(safeTotalTicks, remainingTicks, visualStrength);
        }
        cinematicCameraEnabled = cinematicCamera;
        if (cinematicCameraEnabled) {
            enableSmoothCamera(minecraft);
        } else {
            restoreSmoothCamera(minecraft);
        }
        historyPrimed = false;
        lastWindowWidth = minecraft.getWindow().getWidth();
        lastWindowHeight = minecraft.getWindow().getHeight();
        lastTargetWidth = minecraft.getMainRenderTarget().width;
        lastTargetHeight = minecraft.getMainRenderTarget().height;
        clearHistoryTarget();
    }

    public static void deactivate() {
        restoreSmoothCamera(Minecraft.getInstance());
        cinematicCameraEnabled = false;
        HeroinTripState.deactivate();
        closeProcessor();
        historyPrimed = false;
        lastWindowWidth = -1;
        lastWindowHeight = -1;
        lastTargetWidth = -1;
        lastTargetHeight = -1;
    }

    public static boolean isActive() {
        return ClientDrugVisualAuthority.isHeroinAllowed() && HeroinTripState.isActive();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !HeroinTripState.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientDrugVisualAuthority.isHeroinAllowed()) {
            deactivate();
            return;
        }

        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            closeProcessor();
            return;
        }

        if (minecraft.player.isDeadOrDying()) {
            deactivate();
            return;
        }

        if (cinematicCameraEnabled) {
            enableSmoothCamera(minecraft);
        } else {
            restoreSmoothCamera(minecraft);
        }
        HeroinTripState.tick(minecraft);
        if (!HeroinTripState.isActive()) {
            deactivate();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!ClientDrugVisualAuthority.isHeroinAllowed()
                || !HeroinTripState.isActive()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!ensureProcessorReady(minecraft)) {
            return;
        }

        float partialTick = event.getPartialTick();
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

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!ClientDrugVisualAuthority.isHeroinAllowed() || !HeroinTripState.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        float partialTick = minecraft.getFrameTime();
        float intensity = HeroinTripState.getSmoothedIntensity(partialTick);
        if (intensity < 0.28F) {
            return;
        }

        float seconds = HeroinTripState.getTimeSeconds(partialTick);
        float sedationWave = HeroinTripState.getSedationWave(partialTick);
        float gate = smoothstep(0.28F, 0.74F, intensity);
        float breath = Mth.sin(seconds * 0.55F + Mth.sin(seconds * 0.17F + 0.8F) * 0.65F);
        float fovOffset = breath * FOV_BREATHING_DEGREES * gate * (0.82F + 0.38F * sedationWave);
        event.setFOV(event.getFOV() + fovOffset);
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

            extractPass = new PostPass(minecraft.getResourceManager(), HEROIN_EXTRACT_PROGRAM_NAME, mainTarget, highlightsTarget);
            blurHorizontalPass = new PostPass(minecraft.getResourceManager(), HEROIN_BLUR_PROGRAM_NAME, highlightsTarget, blurTargetA);
            blurVerticalPass = new PostPass(minecraft.getResourceManager(), HEROIN_BLUR_PROGRAM_NAME, blurTargetA, blurTargetB);
            compositePass = new PostPass(minecraft.getResourceManager(), HEROIN_COMPOSITE_PROGRAM_NAME, mainTarget, swapTarget);
            compositePass.addAuxAsset("BloomSampler", blurTargetB::getColorTextureId, blurTargetB.width, blurTargetB.height);
            compositePass.addAuxAsset("HistorySampler", historyTarget::getColorTextureId, historyTarget.width, historyTarget.height);
            blitPass = new PostPass(minecraft.getResourceManager(), BLIT_PROGRAM_NAME, swapTarget, mainTarget);
            historyCopyPass = new PostPass(minecraft.getResourceManager(), BLIT_PROGRAM_NAME, swapTarget, historyTarget);

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
            LOGGER.warn("Failed to initialize heroin post processor", exception);
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
        float intensity = HeroinTripState.getSmoothedIntensity(partialTick);
        float seconds = HeroinTripState.getTimeSeconds(partialTick);
        float cameraMotion = HeroinTripState.getCameraMotion(partialTick);
        float sedationWave = HeroinTripState.getSedationWave(partialTick);
        float warmGate = smoothstep(0.06F, 0.48F, intensity);
        float vignetteGate = smoothstep(0.06F, 0.48F, intensity);
        float blurGate = smoothstep(0.20F, 0.70F, intensity);
        float trailGate = smoothstep(0.42F, 0.84F, intensity);
        float bloomGate = smoothstep(0.52F, 0.90F, intensity);
        float fullWaveGate = smoothstep(0.68F, 1.0F, intensity);
        float warmth = warmGate * (0.18F + 0.24F * intensity) * (0.96F + 0.18F * fullWaveGate * sedationWave);
        float blurStrength = blurGate * (0.48F + 0.72F * intensity + 0.38F * fullWaveGate * sedationWave);
        float vignetteStrength = vignetteGate * (0.40F + 0.42F * intensity + 0.16F * fullWaveGate * sedationWave);
        float trailStrength = historyPrimed
                ? trailGate * (0.45F + 0.75F * fullWaveGate * (0.5F + 0.5F * sedationWave))
                : 0.0F;
        float bloomStrength = bloomGate * (0.16F + 0.28F * intensity + 0.12F * fullWaveGate * sedationWave);
        float bloomThreshold = Mth.lerp(bloomGate, 0.68F, 0.50F);
        float bloomKnee = Mth.lerp(bloomGate, 0.20F, 0.34F);
        float bloomBlurScale = 1.0F + bloomGate * (1.25F + 1.15F * fullWaveGate * sedationWave);
        float resolutionX = minecraft.getWindow().getWidth();
        float resolutionY = minecraft.getWindow().getHeight();

        setUniform(extractPass.getEffect(), "Threshold", bloomThreshold);
        setUniform(extractPass.getEffect(), "Knee", bloomKnee);

        setUniform(blurHorizontalPass.getEffect(), "BlurDir", 1.0F, 0.0F);
        setUniform(blurHorizontalPass.getEffect(), "BlurScale", bloomBlurScale);
        setUniform(blurVerticalPass.getEffect(), "BlurDir", 0.0F, 1.0F);
        setUniform(blurVerticalPass.getEffect(), "BlurScale", bloomBlurScale);

        EffectInstance composite = compositePass.getEffect();
        setUniform(composite, "EffectTime", seconds);
        setUniform(composite, "Intensity", intensity);
        setUniform(composite, "Resolution", resolutionX, resolutionY);
        setUniform(composite, "CameraMotion", cameraMotion);
        setUniform(composite, "SedationWave", sedationWave);
        setUniform(composite, "Warmth", warmth);
        setUniform(composite, "BlurStrength", blurStrength);
        setUniform(composite, "VignetteStrength", vignetteStrength);
        setUniform(composite, "TrailStrength", trailStrength);
        setUniform(composite, "BloomStrength", bloomStrength);
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
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static void enableSmoothCamera(Minecraft minecraft) {
        DrugSmoothCameraManager.enable(minecraft, SMOOTH_CAMERA_OWNER);
    }

    private static void restoreSmoothCamera(Minecraft minecraft) {
        DrugSmoothCameraManager.disable(minecraft, SMOOTH_CAMERA_OWNER);
    }
}
