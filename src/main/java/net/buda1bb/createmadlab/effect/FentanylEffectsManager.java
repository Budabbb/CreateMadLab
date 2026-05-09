package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.FentanylEffectS2CPacket;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class FentanylEffectsManager {
    public static final int TOTAL_DURATION_TICKS = 1900;
    public static final int FINAL_FADE_START_TICKS = 1620;
    public static final int EFFECT_FADE_IN_TICKS = 100;

    public static final int BLINK_1_START = 240;
    public static final int BLINK_1_DURATION = 8;
    public static final int BLINK_2_START = 420;
    public static final int BLINK_2_DURATION = 12;
    public static final int BLINK_3_START = 600;
    public static final int BLINK_3_DURATION = 18;
    public static final int BLINK_4_START = 780;
    public static final int BLINK_4_DURATION = 28;
    public static final int BLINK_5_START = 980;
    public static final int BLINK_5_DURATION = 40;
    public static final int BLINK_6_START = 1180;
    public static final int BLINK_6_DURATION = 52;
    public static final int BLINK_7_START = 1360;
    public static final int BLINK_7_DURATION = 64;
    public static final int BLINK_8_START = 1500;
    public static final int BLINK_8_DURATION = 72;

    public static final double MAX_MOVEMENT_SLOW = -0.86D;

    private static final String FENTANYL_ACTIVE_TAG = "FentanylOverdoseActive";
    private static final String FENTANYL_ELAPSED_TICKS_TAG = "FentanylOverdoseElapsedTicks";
    private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "fentanyl_sedation_movement");

    private FentanylEffectsManager() {
    }

    public static void startFentanylOverdose(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        if (level.isClientSide) {
            ShaderUtils.activateFentanylOverdoseShaders(TOTAL_DURATION_TICKS, TOTAL_DURATION_TICKS);
            return;
        }

        CompoundTag persistedData = getPersistedData(player);
        persistedData.putBoolean(FENTANYL_ACTIVE_TAG, true);
        persistedData.putInt(FENTANYL_ELAPSED_TICKS_TAG, 0);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);

        if (player instanceof ServerPlayer serverPlayer) {
            syncActiveEffect(serverPlayer);
        }
    }

    public static void syncActiveEffect(ServerPlayer player) {
        if (player == null) {
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            clearFentanylOverdose(player);
            return;
        }

        ModMessages.sendToPlayer(new FentanylEffectS2CPacket(true, remainingTicks, TOTAL_DURATION_TICKS), player);
    }

    public static void tickActiveEffect(Player player, Level level) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }

        if (!hasActiveOverdoseData(player)) {
            removeMovementModifier(player);
            return;
        }

        if (player.isDeadOrDying()) {
            clearFentanylOverdoseData(player);
            removeMovementModifier(player);
            return;
        }

        int elapsedTicks = getElapsedTicks(player);
        if (elapsedTicks >= TOTAL_DURATION_TICKS) {
            completeOverdoseDeath(player);
            return;
        }

        elapsedTicks++;
        setElapsedTicks(player, elapsedTicks);

        if (elapsedTicks >= TOTAL_DURATION_TICKS) {
            completeOverdoseDeath(player);
        }
    }

    public static void updateOngoingGameplayEffects(Player player, Level level) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }

        if (!isFentanylOverdoseActive(player, level) || player.isDeadOrDying()) {
            removeMovementModifier(player);
            return;
        }

        int elapsedTicks = getElapsedTicks(player);
        applyMovementModifier(player, getMovementPenalty(elapsedTicks));

    }

    public static boolean isFentanylOverdoseActive(Player player) {
        return player != null && player.level().isClientSide && ShaderUtils.areFentanylOverdoseShadersActive();
    }

    public static boolean isFentanylOverdoseActive(Player player, Level level) {
        return player != null
                && level != null
                && !player.isDeadOrDying()
                && hasActiveOverdoseData(player)
                && getElapsedTicks(player) < TOTAL_DURATION_TICKS;
    }

    public static boolean hasFentanylOverdoseState(Player player) {
        return hasActiveOverdoseData(player);
    }

    public static int getElapsedTicks(Player player) {
        if (player == null) {
            return 0;
        }

        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(FENTANYL_ELAPSED_TICKS_TAG)
                ? Mth.clamp(persistedData.getInt(FENTANYL_ELAPSED_TICKS_TAG), 0, TOTAL_DURATION_TICKS)
                : 0;
    }

    public static int getRemainingDurationTicks(Player player) {
        if (player == null || !hasActiveOverdoseData(player)) {
            return 0;
        }

        return Math.max(0, TOTAL_DURATION_TICKS - getElapsedTicks(player));
    }

    public static int getCooldownDuration() {
        return TOTAL_DURATION_TICKS;
    }

    public static double getMovementPenalty(int elapsedTicks) {
        float slowdownProgress = smoothstep(0.0F, FINAL_FADE_START_TICKS, elapsedTicks);
        return MAX_MOVEMENT_SLOW * slowdownProgress;
    }

    public static double getHorizontalJumpDistanceMultiplier(int elapsedTicks) {
        double movementSpeedMultiplier = Math.max(0.0D, 1.0D + getMovementPenalty(elapsedTicks));
        return movementSpeedMultiplier * movementSpeedMultiplier;
    }

    public static double getJumpVelocityMultiplier(int elapsedTicks) {
        float collapseProgress = smoothstep(200.0F, FINAL_FADE_START_TICKS, elapsedTicks);
        return Math.max(0.0D, 1.0D - collapseProgress);
    }

    public static void clearFentanylOverdose(Player player) {
        if (player == null) {
            return;
        }

        removeMovementModifier(player);
        clearFentanylOverdoseData(player);

        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new FentanylEffectS2CPacket(false, 0, TOTAL_DURATION_TICKS), serverPlayer);
        } else if (player.level().isClientSide) {
            ShaderUtils.deactivateFentanylOverdoseShaders();
        }
    }

    public static float computeVisualIntensity(float elapsedTicks) {
        float elapsed = Mth.clamp(elapsedTicks, 0.0F, TOTAL_DURATION_TICKS);
        if (elapsed <= 200.0F) {
            return smoothstep(0.0F, 200.0F, elapsed) * 0.26F;
        }
        if (elapsed <= 700.0F) {
            return Mth.lerp(smoothstep(200.0F, 700.0F, elapsed), 0.26F, 0.68F);
        }
        if (elapsed <= FINAL_FADE_START_TICKS) {
            return Mth.lerp(smoothstep(700.0F, FINAL_FADE_START_TICKS, elapsed), 0.68F, 1.0F);
        }
        return 1.0F;
    }

    public static float computeFinalFade(float elapsedTicks) {
        return smoothstep(FINAL_FADE_START_TICKS, TOTAL_DURATION_TICKS, elapsedTicks);
    }

    public static float computeEffectFadeIn(float elapsedTicks) {
        return smoothstep(0.0F, EFFECT_FADE_IN_TICKS, elapsedTicks);
    }

    public static float computeBlackoutAlpha(float elapsedTicks) {
        float blinkAlpha = 0.0F;
        blinkAlpha = Math.max(blinkAlpha, computeBlinkAlpha(elapsedTicks, BLINK_1_START, BLINK_1_DURATION));
        blinkAlpha = Math.max(blinkAlpha, computeBlinkAlpha(elapsedTicks, BLINK_2_START, BLINK_2_DURATION));
        blinkAlpha = Math.max(blinkAlpha, computeBlinkAlpha(elapsedTicks, BLINK_3_START, BLINK_3_DURATION));
        blinkAlpha = Math.max(blinkAlpha, computeBlinkAlpha(elapsedTicks, BLINK_4_START, BLINK_4_DURATION));
        blinkAlpha = Math.max(blinkAlpha, computeBlinkAlpha(elapsedTicks, BLINK_5_START, BLINK_5_DURATION));
        blinkAlpha = Math.max(blinkAlpha, computeBlinkAlpha(elapsedTicks, BLINK_6_START, BLINK_6_DURATION));
        blinkAlpha = Math.max(blinkAlpha, computeBlinkAlpha(elapsedTicks, BLINK_7_START, BLINK_7_DURATION));
        blinkAlpha = Math.max(blinkAlpha, computeBlinkAlpha(elapsedTicks, BLINK_8_START, BLINK_8_DURATION));

        return Math.max(blinkAlpha, computeFinalFade(elapsedTicks));
    }

    private static float computeBlinkAlpha(float elapsedTicks, int startTick, int durationTicks) {
        float localTicks = elapsedTicks - startTick;
        if (localTicks < 0.0F || localTicks > durationTicks) {
            return 0.0F;
        }

        float fadeTicks = Mth.clamp(durationTicks / 3.0F, 2.0F, 12.0F);
        if (localTicks < fadeTicks) {
            return smoothstep(0.0F, fadeTicks, localTicks);
        }
        if (localTicks > durationTicks - fadeTicks) {
            return 1.0F - smoothstep(durationTicks - fadeTicks, durationTicks, localTicks);
        }
        return 1.0F;
    }

    private static void completeOverdoseDeath(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.isDeadOrDying()) {
            return;
        }

        removeMovementModifier(serverPlayer);
        serverPlayer.setSprinting(false);
        serverPlayer.setHealth(0.0F);
        serverPlayer.die(serverPlayer.damageSources().generic());
    }

    private static boolean hasActiveOverdoseData(Player player) {
        if (player == null) {
            return false;
        }

        CompoundTag persistedData = getPersistedData(player);
        return persistedData.getBoolean(FENTANYL_ACTIVE_TAG);
    }

    private static void setElapsedTicks(Player player, int elapsedTicks) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedData.putBoolean(FENTANYL_ACTIVE_TAG, true);
        persistedData.putInt(FENTANYL_ELAPSED_TICKS_TAG, Mth.clamp(elapsedTicks, 0, TOTAL_DURATION_TICKS));
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static void clearFentanylOverdoseData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedData.remove(FENTANYL_ACTIVE_TAG);
        persistedData.remove(FENTANYL_ELAPSED_TICKS_TAG);
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static void applyMovementModifier(Player player, double amount) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        if (Math.abs(amount) < 0.0001D) {
            removeMovementModifier(player);
            return;
        }

        AttributeModifier existing = attribute.getModifier(MOVEMENT_SPEED_MODIFIER_ID);
        if (existing != null && existing.amount() == amount) {
            return;
        }

        if (existing != null) {
            attribute.removeModifier(existing);
        }

        attribute.addTransientModifier(new AttributeModifier(
                MOVEMENT_SPEED_MODIFIER_ID,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void removeMovementModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        AttributeModifier existing = attribute.getModifier(MOVEMENT_SPEED_MODIFIER_ID);
        if (existing != null) {
            attribute.removeModifier(existing);
        }
    }

    private static CompoundTag getPersistedData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            persistentData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistentData.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / Math.max(edge1 - edge0, 0.0001F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
