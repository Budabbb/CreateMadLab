package net.buda1bb.createmadlab.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.logging.LogUtils;
import net.buda1bb.createmadlab.CreateMadLab;
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
public final class LSDClientEffectManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LSD_PROGRAM_NAME = CreateMadLab.MOD_ID + ":lsd/composite";
    private static final String BLIT_PROGRAM_NAME = "minecraft:blit";
    private static final float FOV_BREATHING_DEGREES = 2.4F;

    private static TextureTarget swapTarget;
    private static PostPass lsdPass;
    private static PostPass blitPass;
    private static int lastWindowWidth = -1;
    private static int lastWindowHeight = -1;

    private LSDClientEffectManager() {
    }

    public static void activate(int durationTicks, float strength) {
        activate(durationTicks, durationTicks, strength);
    }

    public static void activate(int remainingTicks, int totalTicks, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        if (LsdTripState.isActive()) {
            LsdTripState.extend(totalTicks, remainingTicks, strength);
        } else {
            LsdTripState.resume(totalTicks, remainingTicks, strength);
        }
        lastWindowWidth = minecraft.getWindow().getWidth();
        lastWindowHeight = minecraft.getWindow().getHeight();
    }

    public static void deactivate() {
        LsdTripState.deactivate();
        closeProcessor();
        lastWindowWidth = -1;
        lastWindowHeight = -1;
    }

    public static boolean isActive() {
        return LsdTripState.isActive();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !LsdTripState.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (minecraft.player.isDeadOrDying()) {
            deactivate();
            return;
        }

        if (minecraft.isPaused()) {
            return;
        }

        LsdTripState.tick(minecraft);
        if (!LsdTripState.isActive()) {
            deactivate();
            return;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!LsdTripState.isActive() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
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
        lsdPass.process(partialTick);
        blitPass.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(true);
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        deactivate();
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            deactivate();
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            deactivate();
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!LsdTripState.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        float partialTick = minecraft.getFrameTime();
        float intensity = LsdTripState.getSmoothedIntensity(partialTick);
        if (intensity < 0.10F) {
            return;
        }

        float seconds = LsdTripState.getTimeSeconds(partialTick);
        float ultraDoseFactor = LsdTripState.getUltraDoseFactor();
        float peakPulse = LsdTripState.getPeakPulse(partialTick);
        float spikeStrength = LsdTripState.getSpikeStrength(partialTick);
        float breath = 0.5F + 0.5F * Mth.sin(seconds * 0.82F + peakPulse * 1.75F);
        float fovOffset = smoothstep(0.10F, 0.45F, intensity) * (0.45F + 0.55F * breath) * (0.75F + 0.25F * peakPulse);
        fovOffset += spikeStrength * (0.38F + 0.22F * Mth.sin(seconds * 5.6F + 0.9F));
        fovOffset += ultraDoseFactor * spikeStrength * (0.18F + 0.10F * Mth.cos(seconds * 7.1F + 0.5F));
        event.setFOV(event.getFOV() + FOV_BREATHING_DEGREES * fovOffset);
    }

    private static void updateUniforms(Minecraft minecraft, float partialTick) {
        if (lsdPass == null) {
            return;
        }

        float intensity = LsdTripState.getSmoothedIntensity(partialTick);
        float phase = LsdTripState.getTripPhase(partialTick);
        float tripTimeSeconds = LsdTripState.getTimeSeconds(partialTick);
        float cameraMotion = LsdTripState.getCameraMotion(partialTick);
        float doseStrength = LsdTripState.getDoseStrength();
        float highDoseFactor = smoothstep(0.90F, 1.55F, doseStrength);
        float veryHighDoseFactor = smoothstep(1.90F, 2.45F, doseStrength);
        float ultraDoseFactor = LsdTripState.getUltraDoseFactor();
        float spikeStrength = LsdTripState.getSpikeStrength(partialTick);
        float peakPulse = LsdTripState.getPeakPulse(partialTick);
        float distortionStrength = intensity
                * (0.72F + 0.28F * peakPulse)
                * (0.92F + 0.48F * highDoseFactor)
                * (1.0F + 0.25F * veryHighDoseFactor + 0.60F * ultraDoseFactor + 0.95F * spikeStrength);
        float trailStrength = smoothstep(0.60F, 1.0F, intensity)
                * (0.30F + 0.70F * peakPulse)
                * (0.20F + 0.80F * cameraMotion)
                * (0.85F + 0.80F * highDoseFactor)
                * (1.0F + 0.20F * veryHighDoseFactor + 0.42F * ultraDoseFactor + 0.65F * spikeStrength);
        float resolutionX = minecraft.getWindow().getWidth();
        float resolutionY = minecraft.getWindow().getHeight();

        EffectInstance effect = lsdPass.getEffect();
        setUniform(effect, "TripTime", tripTimeSeconds);
        setUniform(effect, "Intensity", intensity);
        setUniform(effect, "DoseStrength", doseStrength);
        setUniform(effect, "CameraMotion", cameraMotion);
        setUniform(effect, "TripPhase", phase);
        setUniform(effect, "PeakPulse", peakPulse);
        setUniform(effect, "SpikeStrength", spikeStrength);
        setUniform(effect, "DistortionStrength", distortionStrength);
        setUniform(effect, "TrailStrength", trailStrength);
        setUniform(effect, "Resolution", resolutionX, resolutionY);
    }

    private static boolean ensureProcessorReady(Minecraft minecraft) {
        if (minecraft.level == null) {
            return false;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        int windowWidth = minecraft.getWindow().getWidth();
        int windowHeight = minecraft.getWindow().getHeight();

        if (swapTarget != null && lsdPass != null && blitPass != null
                && windowWidth == lastWindowWidth && windowHeight == lastWindowHeight
                && swapTarget.width == mainTarget.width && swapTarget.height == mainTarget.height) {
            return true;
        }

        closeProcessor();

        try {
            swapTarget = new TextureTarget(mainTarget.width, mainTarget.height, true, Minecraft.ON_OSX);
            swapTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            if (mainTarget.isStencilEnabled()) {
                swapTarget.enableStencil();
            }

            lsdPass = new PostPass(minecraft.getResourceManager(), LSD_PROGRAM_NAME, mainTarget, swapTarget);
            blitPass = new PostPass(minecraft.getResourceManager(), BLIT_PROGRAM_NAME, swapTarget, mainTarget);

            Matrix4f orthoMatrix = new Matrix4f().setOrtho(0.0F, (float) mainTarget.width, 0.0F, (float) mainTarget.height, 0.1F, 1000.0F);
            lsdPass.setOrthoMatrix(orthoMatrix);
            blitPass.setOrthoMatrix(orthoMatrix);

            lastWindowWidth = windowWidth;
            lastWindowHeight = windowHeight;
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Failed to initialize LSD post processor", exception);
            closeProcessor();
            return false;
        }
    }

    private static void closeProcessor() {
        if (lsdPass != null) {
            lsdPass.close();
            lsdPass = null;
        }
        if (blitPass != null) {
            blitPass.close();
            blitPass = null;
        }
        if (swapTarget != null) {
            swapTarget.destroyBuffers();
            swapTarget = null;
        }
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
}
