package net.buda1bb.createmadlab.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.logging.LogUtils;
import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
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
import java.lang.reflect.Field;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID, value = Dist.CLIENT)
public final class MorphineClientEffectManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MORPHINE_EXTRACT_PROGRAM_NAME = CreateMadLab.MOD_ID + ":morphine/extract_bright";
    private static final String MORPHINE_BLUR_PROGRAM_NAME = CreateMadLab.MOD_ID + ":morphine/blur";
    private static final String MORPHINE_COMPOSITE_PROGRAM_NAME = CreateMadLab.MOD_ID + ":morphine/composite";
    private static final String BLIT_PROGRAM_NAME = "minecraft:blit";
    private static final float FOV_PULSE_DEGREES = 1.45F;

    private static TextureTarget swapTarget;
    private static TextureTarget highlightsTarget;
    private static TextureTarget blurTargetA;
    private static TextureTarget blurTargetB;
    private static PostPass extractPass;
    private static PostPass blurHorizontalPass;
    private static PostPass blurVerticalPass;
    private static PostPass compositePass;
    private static PostPass blitPass;
    private static int lastWindowWidth = -1;
    private static int lastWindowHeight = -1;
    private static int lastTargetWidth = -1;
    private static int lastTargetHeight = -1;
    private static final int OBSERVED_DECAY_MATCH_WINDOW_TICKS = 2;
    private static boolean pendingSilentHealthSync;
    private static float pendingSilentHealthTarget = Float.NaN;
    private static int recentConvertedDamageTicks;
    private static float lastObservedHealth = Float.NaN;
    private static int lastObservedUnstableHp = -1;
    private static float pendingObservedHealthDrop;
    private static int pendingObservedDebtDrop;
    private static int pendingObservedDecayMatchTicks;
    private static final Field GUI_HEALTH_BLINK_TIME_FIELD = findGuiField("healthBlinkTime");
    private static final Field GUI_LAST_HEALTH_TIME_FIELD = findGuiField("lastHealthTime");
    private static final Field GUI_LAST_HEALTH_FIELD = findGuiField("lastHealth");
    private static final Field GUI_DISPLAY_HEALTH_FIELD = findGuiField("displayHealth");
    private static final Field LOCAL_PLAYER_FLASH_ON_SET_HEALTH_FIELD = findLocalPlayerField("flashOnSetHealth");

    private MorphineClientEffectManager() {
    }

    public static void activate(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay, boolean convertedDamage) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        int safeTotalTicks = totalTicks > 0 ? totalTicks : MorphineEffectsManager.getTotalDuration();
        int safeRemainingTicks = Mth.clamp(remainingTicks, 0, safeTotalTicks);
        MorphineTripState.sync(safeTotalTicks, safeRemainingTicks, Math.max(0, unstableHp));
        if (convertedDamage) {
            recentConvertedDamageTicks = Math.max(recentConvertedDamageTicks, 3);
        }
        if (silentDecay) {
            queueSilentDecaySuppression(minecraft, health);
        }
        lastWindowWidth = minecraft.getWindow().getWidth();
        lastWindowHeight = minecraft.getWindow().getHeight();
        lastTargetWidth = minecraft.getMainRenderTarget().width;
        lastTargetHeight = minecraft.getMainRenderTarget().height;
    }

    public static void deactivate() {
        MorphineTripState.deactivate();
        closeProcessor();
        lastWindowWidth = -1;
        lastWindowHeight = -1;
        lastTargetWidth = -1;
        lastTargetHeight = -1;
        pendingSilentHealthSync = false;
        pendingSilentHealthTarget = Float.NaN;
        recentConvertedDamageTicks = 0;
        lastObservedHealth = Float.NaN;
        lastObservedUnstableHp = -1;
        pendingObservedHealthDrop = 0.0F;
        pendingObservedDebtDrop = 0;
        pendingObservedDecayMatchTicks = 0;
    }

    public static boolean isActive() {
        return MorphineTripState.isActive();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!MorphineTripState.isActive()) {
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

        if (minecraft.isPaused()) {
            return;
        }

        MorphineTripState.tick();
        if (recentConvertedDamageTicks > 0) {
            recentConvertedDamageTicks--;
        }
        detectSilentDecayTransition(minecraft);
        suppressSilentDecayFeedback(minecraft);
        softenHurtFeedback(minecraft);
        if (!MorphineTripState.isActive()) {
            deactivate();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!MorphineTripState.isActive() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
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
        minecraft.getMainRenderTarget().bindWrite(true);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            return;
        }

        suppressSilentDecayFeedback(Minecraft.getInstance());
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
        if (!MorphineTripState.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        float partialTick = (float) event.getPartialTick();
        float phaseIntensity = MorphineTripState.getSmoothedIntensity(partialTick);
        float debtIntensity = MorphineTripState.getSmoothedDebtIntensity(partialTick);
        float healthFactor = getMissingHealthFactor(minecraft.player);
        float intensity = computeFinalIntensity(phaseIntensity, healthFactor);
        if (Math.max(intensity, debtIntensity * 0.68F) < 0.08F) {
            return;
        }

        float seconds = MorphineTripState.getTimeSeconds(partialTick);
        float phase = MorphineTripState.getTripPhase(partialTick);
        float peakEndPhase = MorphineEffectsManager.getPeakEndPhase();
        float breathingPhase = seconds * 0.82F + 0.35F * Mth.sin(seconds * 0.31F);
        float breathingCycle = Mth.sin(breathingPhase);
        float breathingWave = 0.5F + 0.5F * breathingCycle;
        float pulseFade = 1.0F - smoothstep(peakEndPhase, Math.min(1.0F, peakEndPhase + 0.20F), phase);
        float morphineGate = smoothstep(0.06F, 0.32F, intensity);
        float debtGate = smoothstep(0.12F, 0.56F, debtIntensity);
        float fovScale = Mth.clamp(morphineGate * (0.58F + 0.42F * pulseFade) + debtGate * 0.48F, 0.0F, 1.0F);
        float healthPressure = 1.0F + healthFactor * 0.70F;
        float fovOffset = fovScale * breathingCycle * (0.45F + 0.55F * breathingWave) * healthPressure;
        event.setFOV(event.getFOV() + FOV_PULSE_DEGREES * fovOffset);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!MorphineTripState.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.player.hurtTime <= 0) {
            return;
        }

        float intensity = MorphineTripState.getSmoothedIntensity((float) event.getPartialTick());
        if (intensity <= 0.08F) {
            return;
        }

        float suppression = smoothstep(0.18F, 0.82F, intensity);
        float rollScale = Mth.lerp(suppression, 0.78F, 0.28F);
        event.setRoll(event.getRoll() * rollScale);
    }

    private static boolean ensureProcessorReady(Minecraft minecraft) {
        if (minecraft.level == null) {
            return false;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        int windowWidth = minecraft.getWindow().getWidth();
        int windowHeight = minecraft.getWindow().getHeight();
        if (swapTarget != null && highlightsTarget != null && blurTargetA != null && blurTargetB != null
                && extractPass != null && blurHorizontalPass != null && blurVerticalPass != null
                && compositePass != null && blitPass != null && windowWidth == lastWindowWidth
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

            extractPass = new PostPass(minecraft.getResourceManager(), MORPHINE_EXTRACT_PROGRAM_NAME, mainTarget, highlightsTarget, false);
            blurHorizontalPass = new PostPass(minecraft.getResourceManager(), MORPHINE_BLUR_PROGRAM_NAME, highlightsTarget, blurTargetA, false);
            blurVerticalPass = new PostPass(minecraft.getResourceManager(), MORPHINE_BLUR_PROGRAM_NAME, blurTargetA, blurTargetB, false);
            compositePass = new PostPass(minecraft.getResourceManager(), MORPHINE_COMPOSITE_PROGRAM_NAME, mainTarget, swapTarget, false);
            compositePass.addAuxAsset("BloomSampler", blurTargetB::getColorTextureId, blurTargetB.width, blurTargetB.height);
            blitPass = new PostPass(minecraft.getResourceManager(), BLIT_PROGRAM_NAME, swapTarget, mainTarget, false);

            Matrix4f orthoMatrix = new Matrix4f().setOrtho(0.0F, (float) mainTarget.width, 0.0F, (float) mainTarget.height, 0.1F, 1000.0F);
            extractPass.setOrthoMatrix(orthoMatrix);
            blurHorizontalPass.setOrthoMatrix(orthoMatrix);
            blurVerticalPass.setOrthoMatrix(orthoMatrix);
            compositePass.setOrthoMatrix(orthoMatrix);
            blitPass.setOrthoMatrix(orthoMatrix);

            lastWindowWidth = windowWidth;
            lastWindowHeight = windowHeight;
            lastTargetWidth = mainTarget.width;
            lastTargetHeight = mainTarget.height;
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Failed to initialize morphine post processor", exception);
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
        float phaseIntensity = MorphineTripState.getSmoothedIntensity(partialTick);
        float debtIntensity = MorphineTripState.getSmoothedDebtIntensity(partialTick);
        float phase = MorphineTripState.getTripPhase(partialTick);
        float seconds = MorphineTripState.getTimeSeconds(partialTick);
        float rushPulse = MorphineTripState.getHeartPulse(partialTick);
        float healthFactor = getMissingHealthFactor(minecraft.player);
        float intensity = computeFinalIntensity(phaseIntensity, healthFactor);
        float comeupEndPhase = MorphineEffectsManager.getComeupEndPhase();
        float peakEndPhase = MorphineEffectsManager.getPeakEndPhase();
        float onset = 1.0F - smoothstep(comeupEndPhase * 0.40F, comeupEndPhase, phase);
        float breathingPhase = seconds * 0.82F + 0.35F * Mth.sin(seconds * 0.31F);
        float breathingWave = 0.5F + 0.5F * Mth.sin(breathingPhase);
        float pulseFade = 1.0F - smoothstep(peakEndPhase, Math.min(1.0F, peakEndPhase + 0.22F), phase);
        float bloomFade = 1.0F - smoothstep(peakEndPhase + 0.06F, Math.min(1.0F, peakEndPhase + 0.34F), phase);
        float edgeFade = 1.0F - smoothstep(peakEndPhase + 0.12F, Math.min(1.0F, peakEndPhase + 0.42F), phase);
        float colorFade = 1.0F - smoothstep(peakEndPhase + 0.20F, 1.0F, phase);
        float morphineGate = smoothstep(0.03F, 0.26F, intensity);
        float debtGate = smoothstep(0.08F, 0.52F, debtIntensity);
        float presence = Mth.clamp(Math.max(morphineGate, debtGate * 0.88F), 0.0F, 1.0F);
        float injuryTunnel = smoothstep(0.18F, 0.85F, healthFactor);
        float healthBoost = 1.0F + healthFactor * 0.88F;
        float surgeStrength = Mth.clamp(
                (0.24F + 0.76F * rushPulse) * Mth.clamp(
                        morphineGate * (0.56F + 0.44F * pulseFade) * (1.0F + 0.58F * healthFactor)
                                + debtGate * 0.72F,
                        0.0F,
                        1.0F
                ),
                0.0F,
                1.0F
        );
        float pulseStrength = Mth.clamp(
                (0.28F + 0.72F * breathingWave) * Mth.clamp(
                        morphineGate * (0.58F + 0.42F * pulseFade) * (1.0F + 0.52F * healthFactor)
                                + debtGate * 0.76F,
                        0.0F,
                        1.0F
                ),
                0.0F,
                1.0F
        );
        float rushStrength = onset * morphineGate * (0.88F + 0.32F * rushPulse);

        float warmth = Mth.clamp(
                colorFade * morphineGate * (0.34F + 0.28F * phaseIntensity + 0.08F * healthFactor)
                        + debtGate * 0.08F
                        + rushStrength * 0.20F
                        + surgeStrength * 0.08F,
                0.0F, 1.0F
        );
        float blurStrength = Mth.clamp(
                edgeFade * presence * (0.16F + 0.22F * phaseIntensity + 0.16F * injuryTunnel + 0.14F * pulseStrength) * healthBoost
                        + debtGate * 0.12F,
                0.0F, 1.0F
        );
        float vignetteStrength = Mth.clamp(
                edgeFade * presence * (0.24F + 0.30F * phaseIntensity + 0.20F * injuryTunnel) * (1.0F + 0.55F * healthFactor)
                        + pulseStrength * (0.16F + 0.08F * healthFactor)
                        + debtGate * 0.14F,
                0.0F, 1.0F
        );
        float bloomStrength = Mth.clamp(
                bloomFade * presence * (0.14F + 0.18F * phaseIntensity + 0.10F * pulseStrength + 0.16F * surgeStrength + 0.10F * rushStrength),
                0.0F, 1.0F
        );
        float hazeStrength = Mth.clamp(
                bloomFade * presence * (0.10F + 0.14F * phaseIntensity + 0.18F * healthFactor + 0.14F * pulseStrength + 0.10F * surgeStrength)
                        + debtGate * 0.10F,
                0.0F, 1.0F
        );
        float contrastStrength = Mth.clamp(
                presence * (0.08F + 0.12F * phaseIntensity + 0.26F * rushStrength) * (0.86F + 0.14F * colorFade),
                0.0F, 1.0F
        );

        float bloomDriver = Mth.clamp(rushStrength + surgeStrength * 0.72F + pulseStrength * 0.30F, 0.0F, 1.0F);
        float bloomThreshold = Mth.lerp(bloomDriver, 0.72F, 0.50F);
        float bloomKnee = Mth.lerp(bloomDriver, 0.18F, 0.34F);
        float bloomBlurScale = 1.10F + bloomStrength * 1.60F + pulseStrength * 0.50F;
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
        setUniform(composite, "Intensity", intensity);
        setUniform(composite, "HealthFactor", healthFactor);
        setUniform(composite, "Pulse", pulseStrength);
        setUniform(composite, "Resolution", resolutionX, resolutionY);
        setUniform(composite, "Warmth", warmth);
        setUniform(composite, "VignetteStrength", vignetteStrength);
        setUniform(composite, "BlurStrength", blurStrength);
        setUniform(composite, "BloomStrength", bloomStrength);
        setUniform(composite, "HazeStrength", hazeStrength);
        setUniform(composite, "ContrastStrength", contrastStrength);
        setUniform(composite, "RushStrength", rushStrength);
        setUniform(composite, "DebtIntensity", debtIntensity);
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
    }

    private static void detectSilentDecayTransition(Minecraft minecraft) {
        if (minecraft.player == null) {
            resetObservedDecayTracking();
            return;
        }

        float currentHealth = minecraft.player.getHealth();
        int currentUnstableHp = MorphineTripState.getUnstableHp();
        if (Float.isNaN(lastObservedHealth) || lastObservedUnstableHp < 0) {
            lastObservedHealth = currentHealth;
            lastObservedUnstableHp = currentUnstableHp;
            return;
        }

        if (pendingObservedDecayMatchTicks > 0) {
            pendingObservedDecayMatchTicks--;
            if (pendingObservedDecayMatchTicks <= 0) {
                pendingObservedHealthDrop = 0.0F;
                pendingObservedDebtDrop = 0;
            }
        }

        float healthDrop = lastObservedHealth - currentHealth;
        int unstableHpDrop = lastObservedUnstableHp - currentUnstableHp;
        if (healthDrop > 0.05F && recentConvertedDamageTicks <= 0 && (currentUnstableHp > 0 || lastObservedUnstableHp > 0)) {
            pendingObservedHealthDrop = healthDrop;
            pendingObservedDecayMatchTicks = OBSERVED_DECAY_MATCH_WINDOW_TICKS;
        }
        if (unstableHpDrop > 0) {
            pendingObservedDebtDrop = unstableHpDrop;
            pendingObservedDecayMatchTicks = OBSERVED_DECAY_MATCH_WINDOW_TICKS;
        }

        if (pendingObservedHealthDrop > 0.05F
                && pendingObservedDebtDrop > 0
                && Math.abs(pendingObservedHealthDrop - pendingObservedDebtDrop) <= 0.15F) {
            queueSilentDecaySuppression(minecraft, currentHealth);
            pendingObservedHealthDrop = 0.0F;
            pendingObservedDebtDrop = 0;
            pendingObservedDecayMatchTicks = 0;
        }

        lastObservedHealth = currentHealth;
        lastObservedUnstableHp = currentUnstableHp;
    }

    private static void suppressSilentDecayFeedback(Minecraft minecraft) {
        if (minecraft.player == null || !pendingSilentHealthSync || Float.isNaN(pendingSilentHealthTarget)) {
            return;
        }

        if (recentConvertedDamageTicks <= 0) {
            suppressLocalPlayerHealthFlash(minecraft);
            clearPlayerHurtState(minecraft);
            resetGuiHealthBlink(minecraft, pendingSilentHealthTarget);
        }

        if (minecraft.player.getHealth() <= pendingSilentHealthTarget + 0.05F) {
            pendingSilentHealthSync = false;
            pendingSilentHealthTarget = Float.NaN;
        }
    }

    private static void queueSilentDecaySuppression(Minecraft minecraft, float targetHealth) {
        pendingSilentHealthSync = true;
        pendingSilentHealthTarget = targetHealth;
        if (recentConvertedDamageTicks <= 0) {
            silentlyApplyDecayHealth(minecraft, targetHealth);
            suppressLocalPlayerHealthFlash(minecraft);
            clearPlayerHurtState(minecraft);
            resetGuiHealthBlink(minecraft, targetHealth);
        }
    }

    private static void silentlyApplyDecayHealth(Minecraft minecraft, float targetHealth) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        if (minecraft.player.getHealth() > targetHealth + 0.05F) {
            minecraft.player.setHealth(targetHealth);
        }

        lastObservedHealth = minecraft.player.getHealth();
        lastObservedUnstableHp = MorphineTripState.getUnstableHp();
        pendingObservedHealthDrop = 0.0F;
        pendingObservedDebtDrop = 0;
        pendingObservedDecayMatchTicks = 0;
    }

    private static void clearPlayerHurtState(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        minecraft.player.hurtTime = 0;
        minecraft.player.hurtDuration = 0;
        minecraft.player.invulnerableTime = 0;
    }

    private static void suppressLocalPlayerHealthFlash(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || LOCAL_PLAYER_FLASH_ON_SET_HEALTH_FIELD == null) {
            return;
        }

        LocalPlayer localPlayer = minecraft.player;
        try {
            LOCAL_PLAYER_FLASH_ON_SET_HEALTH_FIELD.setBoolean(localPlayer, false);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static void resetGuiHealthBlink(Minecraft minecraft, float targetHealth) {
        if (minecraft == null || minecraft.gui == null || minecraft.player == null) {
            return;
        }

        try {
            int currentHealth = Mth.ceil(Math.max(0.0F, targetHealth));
            long now = Util.getMillis();
            setNumericGuiField(minecraft.gui, GUI_HEALTH_BLINK_TIME_FIELD, 0L);
            setNumericGuiField(minecraft.gui, GUI_LAST_HEALTH_TIME_FIELD, now);
            setNumericGuiField(minecraft.gui, GUI_LAST_HEALTH_FIELD, currentHealth);
            setNumericGuiField(minecraft.gui, GUI_DISPLAY_HEALTH_FIELD, currentHealth);
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
        }
    }

    private static void setNumericGuiField(Gui gui, Field field, Number value) throws IllegalAccessException {
        if (gui == null || field == null || value == null) {
            return;
        }

        Class<?> type = field.getType();
        if (type == long.class || type == Long.class) {
            field.setLong(gui, value.longValue());
            return;
        }
        if (type == int.class || type == Integer.class) {
            field.setInt(gui, value.intValue());
            return;
        }
        if (type == float.class || type == Float.class) {
            field.setFloat(gui, value.floatValue());
            return;
        }
        if (type == double.class || type == Double.class) {
            field.setDouble(gui, value.doubleValue());
            return;
        }

        field.set(gui, value);
    }

    private static Field findGuiField(String name) {
        try {
            Field field = Gui.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            return null;
        }
    }

    private static Field findLocalPlayerField(String name) {
        try {
            Field field = LocalPlayer.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            return null;
        }
    }

    private static void resetObservedDecayTracking() {
        lastObservedHealth = Float.NaN;
        lastObservedUnstableHp = -1;
        pendingObservedHealthDrop = 0.0F;
        pendingObservedDebtDrop = 0;
        pendingObservedDecayMatchTicks = 0;
    }

    private static void softenHurtFeedback(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.player.hurtTime <= 0) {
            return;
        }

        float intensity = MorphineTripState.getTargetIntensity();
        if (intensity <= 0.08F) {
            return;
        }

        int cappedHurtTime = intensity >= 0.52F ? 1 : intensity >= 0.24F ? 2 : 3;
        if (minecraft.player.hurtTime > cappedHurtTime) {
            minecraft.player.hurtTime = cappedHurtTime;
        }
        if (intensity > 0.22F && minecraft.player.hurtTime > 1) {
            minecraft.player.hurtTime--;
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
        float t = Mth.clamp((value - edge0) / Math.max(edge1 - edge0, 0.0001F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float computeFinalIntensity(float phaseIntensity, float healthFactor) {
        return Mth.clamp(phaseIntensity * (1.08F + healthFactor * 0.82F), 0.0F, 1.9F);
    }

    private static float getMissingHealthFactor(LocalPlayer player) {
        if (player == null || player.getMaxHealth() <= 0.0F) {
            return 0.0F;
        }

        return Mth.clamp(1.0F - (player.getHealth() / player.getMaxHealth()), 0.0F, 1.0F);
    }
}
