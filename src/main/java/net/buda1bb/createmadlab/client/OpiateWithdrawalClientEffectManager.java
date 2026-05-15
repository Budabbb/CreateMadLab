package net.buda1bb.createmadlab.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.IOException;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID, value = Dist.CLIENT)
public final class OpiateWithdrawalClientEffectManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WITHDRAWAL_COMPOSITE_PROGRAM_NAME = CreateMadLab.MOD_ID + ":withdrawal/composite";
    private static final String BLIT_PROGRAM_NAME = "minecraft:blit";
    private static final int WITHDRAWAL_FADE_IN_TICKS = 5 * 20;
    private static final int WITHDRAWAL_FADE_OUT_TICKS = 7 * 20;

    // Tweak: cold blue-gray color cast strength.
    private static final float TINT_STRENGTH = 0.46F;
    // Tweak: saturation loss; higher values drain more color.
    private static final float DESATURATION_STRENGTH = 0.58F;
    // Tweak: dark edge pressure around the screen.
    private static final float VIGNETTE_STRENGTH = 0.26F;
    // Tweak: speed of the restless breathing/pulsing.
    private static final float PULSE_SPEED = 1.15F;
    // Tweak: very small UV shiver amount; keep this subtle.
    private static final float JITTER_AMOUNT = 1.85F;
    // Tweak: maximum blur spike strength.
    private static final float BLUR_STRENGTH = 0.72F;
    // Tweak: seconds between the primary soft blur pulses.
    private static final float BLUR_PULSE_PERIOD_SECONDS = 11.5F;
    // Tweak: first-person hand/held item shake offset.
    private static final float HAND_SHAKE_TRANSLATION = 0.018F;
    // Tweak: first-person hand/held item shake rotation in degrees.
    private static final float HAND_SHAKE_ROTATION_DEGREES = 2.7F;
    // Tweak: third-person arm-only shake pitch rotation in radians.
    private static final float PLAYER_ARM_SHAKE_X_ROT = 0.105F;
    // Tweak: third-person arm-only shake side rotation in radians.
    private static final float PLAYER_ARM_SHAKE_Y_ROT = 0.055F;
    // Tweak: third-person arm-only shake roll rotation in radians.
    private static final float PLAYER_ARM_SHAKE_Z_ROT = 0.115F;
    // Tweak: camera yaw shake in degrees.
    private static final float CAMERA_SHAKE_YAW_DEGREES = 0.08F;
    // Tweak: camera pitch shake in degrees.
    private static final float CAMERA_SHAKE_PITCH_DEGREES = 0.05F;
    // Tweak: camera roll shake in degrees.
    private static final float CAMERA_SHAKE_ROLL_DEGREES = 0.10F;

    private static TextureTarget swapTarget;
    private static PostPass compositePass;
    private static PostPass blitPass;
    private static int lastWindowWidth = -1;
    private static int lastWindowHeight = -1;
    private static int lastTargetWidth = -1;
    private static int lastTargetHeight = -1;
    private static boolean active;
    private static int totalTicks;
    private static int remainingTicks;
    private static float timeTicks;
    private static float previousTimeTicks;
    private static float maxIntensity;
    private static float withdrawalIntensity;
    private static float previousWithdrawalIntensity;
    private static float blurPulse;
    private static float previousBlurPulse;

    private OpiateWithdrawalClientEffectManager() {
    }

    public static void activate(int remainingDurationTicks, int totalDurationTicks, float intensity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        totalTicks = Math.max(1, totalDurationTicks);
        remainingTicks = Mth.clamp(remainingDurationTicks, 0, totalTicks);
        maxIntensity = Mth.clamp(intensity, 0.0F, 1.0F);
        active = remainingTicks > 0 && maxIntensity > 0.0F;
        timeTicks = totalTicks - remainingTicks;
        previousTimeTicks = timeTicks;
        setWithdrawalIntensity(maxIntensity * computeDurationEnvelope());
        previousWithdrawalIntensity = withdrawalIntensity;
        blurPulse = computeBlurPulse(timeTicks / 20.0F) * withdrawalIntensity;
        previousBlurPulse = blurPulse;

        lastWindowWidth = minecraft.getWindow().getWidth();
        lastWindowHeight = minecraft.getWindow().getHeight();
        lastTargetWidth = minecraft.getMainRenderTarget().width;
        lastTargetHeight = minecraft.getMainRenderTarget().height;
    }

    public static void deactivate() {
        active = false;
        totalTicks = 0;
        remainingTicks = 0;
        timeTicks = 0.0F;
        previousTimeTicks = 0.0F;
        maxIntensity = 0.0F;
        withdrawalIntensity = 0.0F;
        previousWithdrawalIntensity = 0.0F;
        blurPulse = 0.0F;
        previousBlurPulse = 0.0F;
        closeProcessor();
        lastWindowWidth = -1;
        lastWindowHeight = -1;
        lastTargetWidth = -1;
        lastTargetHeight = -1;
    }

    public static boolean isActive() {
        return active;
    }

    public static void setWithdrawalIntensity(float intensity) {
        boolean wasActive = active;
        withdrawalIntensity = Mth.clamp(intensity, 0.0F, 1.0F);
        if (!wasActive && withdrawalIntensity > 0.0F) {
            active = true;
            totalTicks = Integer.MAX_VALUE / 4;
            remainingTicks = totalTicks;
            maxIntensity = withdrawalIntensity;
            previousWithdrawalIntensity = withdrawalIntensity;
        }
    }

    public static float getWithdrawalIntensity() {
        return withdrawalIntensity;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
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

        previousTimeTicks = timeTicks;
        previousWithdrawalIntensity = withdrawalIntensity;
        previousBlurPulse = blurPulse;

        if (remainingTicks > 0) {
            remainingTicks--;
        }
        timeTicks += 1.0F;

        if (remainingTicks <= 0) {
            deactivate();
            return;
        }

        setWithdrawalIntensity(maxIntensity * computeDurationEnvelope());
        blurPulse = computeBlurPulse(timeTicks / 20.0F) * withdrawalIntensity;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!active || withdrawalIntensity <= 0.001F || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
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
        compositePass.process(partialTick);
        blitPass.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!shouldApplyVisualShake()) {
            return;
        }

        float partialTick = event.getPartialTick();
        float intensity = getInterpolatedWithdrawalIntensity(partialTick);
        if (intensity <= 0.001F) {
            return;
        }

        float seconds = getInterpolatedTimeTicks(partialTick) / 20.0F;
        float amount = getRestlessShakeAmount(seconds, intensity);
        float fastShake = getFastShake(seconds);
        float sideShake = getSideShake(seconds);
        float depthShake = getDepthShake(seconds);
        float rollShake = getRollShake(seconds);

        PoseStack poseStack = event.getPoseStack();
        poseStack.translate(
                sideShake * HAND_SHAKE_TRANSLATION * amount,
                fastShake * HAND_SHAKE_TRANSLATION * 0.65F * amount,
                depthShake * HAND_SHAKE_TRANSLATION * 0.35F * amount
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(fastShake * HAND_SHAKE_ROTATION_DEGREES * 0.55F * amount));
        poseStack.mulPose(Axis.YP.rotationDegrees(sideShake * HAND_SHAKE_ROTATION_DEGREES * 0.45F * amount));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollShake * HAND_SHAKE_ROTATION_DEGREES * amount));
    }

    public static void applyPlayerArmWithdrawalShake(PlayerModel<?> model, LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldApplyVisualShake() || minecraft == null || entity != minecraft.player) {
            return;
        }

        float intensity = withdrawalIntensity;
        if (intensity <= 0.001F) {
            return;
        }

        float seconds = timeTicks / 20.0F;
        float amount = getRestlessShakeAmount(seconds, intensity);
        float fastShake = getFastShake(seconds);
        float sideShake = getSideShake(seconds);
        float rollShake = getRollShake(seconds);
        float leftOffset = Mth.sin(seconds * 39.0F + 2.1F);
        float rightOffset = Mth.sin(seconds * 41.0F + 0.3F);

        model.rightArm.xRot += (fastShake * 0.65F + rightOffset * 0.35F) * PLAYER_ARM_SHAKE_X_ROT * amount;
        model.rightArm.yRot += sideShake * PLAYER_ARM_SHAKE_Y_ROT * amount;
        model.rightArm.zRot += rollShake * PLAYER_ARM_SHAKE_Z_ROT * amount;

        model.leftArm.xRot += (leftOffset * 0.70F - fastShake * 0.30F) * PLAYER_ARM_SHAKE_X_ROT * amount;
        model.leftArm.yRot -= sideShake * PLAYER_ARM_SHAKE_Y_ROT * amount;
        model.leftArm.zRot -= rollShake * PLAYER_ARM_SHAKE_Z_ROT * amount;

        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldApplyVisualShake() || minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }

        float partialTick = (float) event.getPartialTick();
        float intensity = getInterpolatedWithdrawalIntensity(partialTick);
        if (intensity <= 0.001F) {
            return;
        }

        float seconds = getInterpolatedTimeTicks(partialTick) / 20.0F;
        float amount = getRestlessShakeAmount(seconds, intensity);
        event.setYaw(event.getYaw() + getSideShake(seconds) * CAMERA_SHAKE_YAW_DEGREES * amount);
        event.setPitch(event.getPitch() + getFastShake(seconds) * CAMERA_SHAKE_PITCH_DEGREES * amount);
        event.setRoll(event.getRoll() + getRollShake(seconds) * CAMERA_SHAKE_ROLL_DEGREES * amount);
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
        if (swapTarget != null && compositePass != null && blitPass != null
                && windowWidth == lastWindowWidth && windowHeight == lastWindowHeight
                && lastTargetWidth == mainTarget.width && lastTargetHeight == mainTarget.height) {
            return true;
        }

        closeProcessor();

        try {
            swapTarget = new TextureTarget(mainTarget.width, mainTarget.height, true, Minecraft.ON_OSX);
            swapTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            if (mainTarget.isStencilEnabled()) {
                swapTarget.enableStencil();
            }

            compositePass = new PostPass(minecraft.getResourceManager(), WITHDRAWAL_COMPOSITE_PROGRAM_NAME, mainTarget, swapTarget, false);
            blitPass = new PostPass(minecraft.getResourceManager(), BLIT_PROGRAM_NAME, swapTarget, mainTarget, false);

            Matrix4f orthoMatrix = new Matrix4f().setOrtho(0.0F, (float) mainTarget.width, 0.0F, (float) mainTarget.height, 0.1F, 1000.0F);
            compositePass.setOrthoMatrix(orthoMatrix);
            blitPass.setOrthoMatrix(orthoMatrix);

            lastWindowWidth = windowWidth;
            lastWindowHeight = windowHeight;
            lastTargetWidth = mainTarget.width;
            lastTargetHeight = mainTarget.height;
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Failed to initialize opiate withdrawal post processor", exception);
            closeProcessor();
            return false;
        }
    }

    private static void updateUniforms(Minecraft minecraft, float partialTick) {
        float intensity = getInterpolatedWithdrawalIntensity(partialTick);
        float seconds = getInterpolatedTimeTicks(partialTick) / 20.0F;
        float pulse = 0.5F + 0.5F * Mth.sin(seconds * PULSE_SPEED + Mth.sin(seconds * 0.21F) * 0.55F);
        float jitterDrift = 0.72F + 0.28F * Mth.sin(seconds * 7.2F + Mth.sin(seconds * 1.7F));
        float blur = Mth.lerp(partialTick, previousBlurPulse, blurPulse);
        float resolutionX = minecraft.getWindow().getWidth();
        float resolutionY = minecraft.getWindow().getHeight();

        EffectInstance composite = compositePass.getEffect();
        setUniform(composite, "Time", seconds);
        setUniform(composite, "Intensity", intensity);
        setUniform(composite, "Resolution", resolutionX, resolutionY);
        setUniform(composite, "TintStrength", TINT_STRENGTH * intensity);
        setUniform(composite, "DesaturationStrength", DESATURATION_STRENGTH * intensity);
        setUniform(composite, "VignetteStrength", VIGNETTE_STRENGTH * intensity * (0.84F + 0.16F * pulse));
        setUniform(composite, "PulseStrength", intensity * pulse);
        setUniform(composite, "JitterStrength", JITTER_AMOUNT * intensity * jitterDrift);
        setUniform(composite, "BlurStrength", BLUR_STRENGTH * blur);
    }

    private static boolean shouldApplyVisualShake() {
        return active && withdrawalIntensity > 0.001F;
    }

    private static float getInterpolatedWithdrawalIntensity(float partialTick) {
        return Mth.lerp(partialTick, previousWithdrawalIntensity, withdrawalIntensity);
    }

    private static float getInterpolatedTimeTicks(float partialTick) {
        return Mth.lerp(partialTick, previousTimeTicks, timeTicks);
    }

    private static float getRestlessShakeAmount(float seconds, float intensity) {
        return intensity * (0.68F + 0.32F * Mth.sin(seconds * 2.4F + 0.4F));
    }

    private static float getFastShake(float seconds) {
        return Mth.sin(seconds * 36.0F + Mth.sin(seconds * 5.2F) * 0.8F);
    }

    private static float getSideShake(float seconds) {
        return Mth.sin(seconds * 23.0F + 1.7F);
    }

    private static float getDepthShake(float seconds) {
        return Mth.sin(seconds * 19.0F + 0.6F);
    }

    private static float getRollShake(float seconds) {
        return Mth.sin(seconds * 31.0F + 0.9F);
    }

    private static float computeDurationEnvelope() {
        float elapsedTicks = totalTicks - remainingTicks;
        float fadeIn = smoothstep(0.0F, Math.min(WITHDRAWAL_FADE_IN_TICKS, totalTicks), elapsedTicks);
        float fadeOut = 1.0F - smoothstep(Math.max(0.0F, totalTicks - WITHDRAWAL_FADE_OUT_TICKS), totalTicks, elapsedTicks);
        return Mth.clamp(fadeIn * fadeOut, 0.0F, 1.0F);
    }

    private static float computeBlurPulse(float seconds) {
        float primary = pulseWindow(seconds, BLUR_PULSE_PERIOD_SECONDS, 0.72F);
        float secondary = pulseWindow(seconds + 4.3F, 17.0F, 0.54F) * 0.58F;
        return Mth.clamp(Math.max(primary, secondary), 0.0F, 1.0F);
    }

    private static float pulseWindow(float seconds, float period, float width) {
        float local = seconds - Mth.floor(seconds / period) * period;
        if (local > width) {
            return 0.0F;
        }

        float peak = width * 0.36F;
        if (local <= peak) {
            return smoothstep(0.0F, peak, local);
        }
        return 1.0F - smoothstep(peak, width, local);
    }

    private static void closeProcessor() {
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
