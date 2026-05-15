package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.drug.DrugClass;
import net.buda1bb.createmadlab.drug.DrugStateManager;
import net.buda1bb.createmadlab.drug.DrugType;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.FentanylEffectS2CPacket;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class FentanylEffectsManager {
    private static final String FENTANYL_REMAINING_TICKS_TAG = "FentanylRemainingTicks";
    private static final String FENTANYL_COUNTERED_BY_NALOXONE_TAG = "FentanylCounteredByNaloxone";
    private static final String FENTANYL_COUNTERED_VISUAL_START_INTENSITY_TAG = "FentanylCounteredVisualStartIntensity";
    public static final int TOTAL_DURATION_TICKS = DrugType.FENTANYL.getDurationTicks();
    public static final int EFFECT_FADE_IN_TICKS = 100;
    public static final int FINAL_FADE_START_TICKS = TOTAL_DURATION_TICKS - 280;
    private static final int NALOXONE_COUNTER_DURATION_TICKS = 5 * 20;
    private static final int PEAK_HOLD_TICKS = 900;
    private static final double MOVEMENT_SPEED_PENALTY = -0.65D;
    private static final double ATTACK_SPEED_PENALTY = -0.20D;
    private static final double JUMP_DISTANCE_POWER = 4.0D;
    private static final UUID MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("c3099cb3-c31f-4722-bfc8-5d9ba4aa1f34");
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("1c026ba6-2227-4b2b-9b31-e2296427610d");

    private FentanylEffectsManager() {
    }

    public static void startFentanylEffect(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        if (level.isClientSide) {
            ShaderUtils.activateFentanylShaders(TOTAL_DURATION_TICKS, TOTAL_DURATION_TICKS,
                    false, 0.0F, 0.0F, 1.0F);
            return;
        }

        setRemainingDuration(player, TOTAL_DURATION_TICKS);
        setCounteredByNaloxone(player, false);
        OpiateWithdrawalEffectsManager.clearWithdrawalEffect(player, level);
        if (player instanceof ServerPlayer serverPlayer) {
            syncActiveEffect(serverPlayer);
        }
    }

    public static void extendFentanylEffect(Player player, int remainingTicks) {
        if (player == null || remainingTicks <= 0 || isCounteredByNaloxone(player)) {
            return;
        }

        int safeRemainingTicks = Mth.clamp(remainingTicks, 1, TOTAL_DURATION_TICKS);
        setRemainingDuration(player, safeRemainingTicks);
        setCounteredByNaloxone(player, false);
        if (player instanceof ServerPlayer serverPlayer) {
            syncActiveEffect(serverPlayer);
        }
    }

    public static void syncActiveEffect(ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (!DrugStateManager.hasActiveDrug(player, DrugType.FENTANYL) && !isCounteredByNaloxone(player)) {
            if (getRemainingDurationTicks(player) > 0) {
                cleanupFentanylEffect(player, player.level());
            }
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            cleanupFentanylEffect(player, player.level());
            return;
        }

        if (isCounteredByNaloxone(player)) {
            ModMessages.sendToPlayer(new FentanylEffectS2CPacket(
                    true,
                    remainingTicks,
                    NALOXONE_COUNTER_DURATION_TICKS,
                    true,
                    getCounteredVisualStartIntensity(player),
                    0.0F,
                    1.0F
            ), player);
            return;
        }

        ModMessages.sendToPlayer(new FentanylEffectS2CPacket(
                true,
                remainingTicks,
                TOTAL_DURATION_TICKS,
                false,
                0.0F,
                0.0F,
                getFentanylVisualStrength(player)
        ), player);
    }

    public static void tickActiveEffect(Player player) {
        if (player == null) {
            return;
        }

        if (!DrugStateManager.hasActiveDrug(player, DrugType.FENTANYL) && !isCounteredByNaloxone(player)) {
            if (getRemainingDurationTicks(player) > 0) {
                cleanupFentanylEffect(player, player.level());
            }
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            clearFentanylEffect(player);
            return;
        }

        if (remainingTicks <= 1) {
            clearFentanylEffectAndSync(player, !isCounteredByNaloxone(player));
            return;
        }

        setRemainingDuration(player, remainingTicks - 1);
    }

    public static void updateOngoingGameplayEffects(Player player, Level level) {
        if (player == null || level == null || level.isClientSide || player.isDeadOrDying()) {
            removePenaltyModifiers(player);
            return;
        }

        if (!isFentanylActive(player, level)) {
            removePenaltyModifiers(player);
            return;
        }

        if (isCounteredByNaloxone(player)) {
            applyCounteredPenaltyModifiers(player);
            return;
        }

        ensureModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID,
                "fentanyl_sedation_movement", getScaledMovementPenalty(player));
        ensureModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER_ID,
                "fentanyl_sedation_attack", getScaledAttackSpeedPenalty(player));
    }

    public static void cleanupFentanylEffect(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        clearFentanylEffect(player);
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new FentanylEffectS2CPacket(false, 0, TOTAL_DURATION_TICKS), serverPlayer);
        } else if (level.isClientSide) {
            ShaderUtils.deactivateFentanylShaders();
        }
    }

    public static void counterWithNaloxone(Player player, Level level) {
        if (player == null || level == null || !isFentanylActive(player, level)) {
            return;
        }

        int counteredTicks = NALOXONE_COUNTER_DURATION_TICKS;
        float visualStartIntensity = Mth.clamp(getEffectIntensity(player, level), 0.0F, 1.0F);
        setRemainingDuration(player, counteredTicks);
        setCounteredByNaloxone(player, true);
        setCounteredVisualStartIntensity(player, visualStartIntensity);
        removePenaltyModifiers(player);

        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new FentanylEffectS2CPacket(
                    true,
                    counteredTicks,
                    NALOXONE_COUNTER_DURATION_TICKS,
                    true,
                    visualStartIntensity,
                    0.0F,
                    1.0F
            ), serverPlayer);
        }
    }

    public static boolean isFentanylActive(Player player) {
        return player != null && player.level().isClientSide && ShaderUtils.areFentanylShadersActive();
    }

    public static boolean isFentanylActive(Player player, Level level) {
        return player != null
                && level != null
                && !player.isDeadOrDying()
                && getRemainingDurationTicks(player) > 0
                && (DrugStateManager.hasActiveDrug(player, DrugType.FENTANYL) || isCounteredByNaloxone(player));
    }

    public static int getRemainingDurationTicks(Player player) {
        if (player == null) {
            return 0;
        }

        return Math.max(0, getPersistedData(player).getInt(FENTANYL_REMAINING_TICKS_TAG));
    }

    public static float getEffectIntensity(Player player, Level level) {
        if (player == null || level == null || !isFentanylActive(player, level)) {
            return 0.0F;
        }

        if (isCounteredByNaloxone(player)) {
            return getCounteredVisualStartIntensity(player) * getCounteredRemainingProgress(player);
        }

        int remainingTicks = getRemainingDurationTicks(player);
        return computeVisualIntensity(TOTAL_DURATION_TICKS - remainingTicks) * getFentanylVisualStrength(player);
    }

    public static float getFentanylVisualStrength(Player player) {
        if (player == null) {
            return 0.0F;
        }

        if (isCounteredByNaloxone(player)) {
            return getCounteredVisualStartIntensity(player) * getCounteredRemainingProgress(player);
        }

        float strength = DrugStateManager.getDrugVisualStrength(player, DrugType.FENTANYL);
        return Mth.clamp(strength, 0.0F, 3.0F);
    }

    public static boolean isCounteredByNaloxone(Player player) {
        return player != null && getPersistedData(player).getBoolean(FENTANYL_COUNTERED_BY_NALOXONE_TAG);
    }

    public static double getHorizontalJumpDistanceMultiplier(Player player) {
        double movementSpeedMultiplier = Math.max(0.0D, 1.0D + getScaledMovementPenalty(player));
        return Math.pow(movementSpeedMultiplier, JUMP_DISTANCE_POWER);
    }

    public static double getJumpVelocityMultiplier(Player player) {
        float intensity = Mth.clamp(getFentanylVisualStrength(player), 0.0F, 2.5F) / 2.5F;
        return Mth.clamp(1.0D - 0.35D * intensity, 0.45D, 1.0D);
    }

    public static int getCooldownDuration() {
        return TOTAL_DURATION_TICKS;
    }

    public static int getTotalDurationTicks() {
        return TOTAL_DURATION_TICKS;
    }

    public static double getMovementPenalty(int elapsedTicks) {
        float intensity = computeVisualIntensity(elapsedTicks);
        return MOVEMENT_SPEED_PENALTY * intensity;
    }

    public static float computeVisualIntensity(float elapsedTicks) {
        float elapsed = Mth.clamp(elapsedTicks, 0.0F, TOTAL_DURATION_TICKS);
        float fadeInEnd = Math.min(EFFECT_FADE_IN_TICKS, TOTAL_DURATION_TICKS);
        float peakEnd = Math.min(fadeInEnd + PEAK_HOLD_TICKS, TOTAL_DURATION_TICKS);

        if (elapsed <= fadeInEnd) {
            return smoothstep(0.0F, fadeInEnd, elapsed);
        }
        if (elapsed <= peakEnd) {
            return 1.0F;
        }
        return 1.0F - smoothstep(peakEnd, TOTAL_DURATION_TICKS, elapsed);
    }

    public static float computeFinalFade(float elapsedTicks) {
        return 0.0F;
    }

    public static float computeEffectFadeIn(float elapsedTicks) {
        return smoothstep(0.0F, EFFECT_FADE_IN_TICKS, elapsedTicks);
    }

    public static float computeBlackoutAlpha(float elapsedTicks) {
        return 0.0F;
    }

    public static void startFentanylOverdose(Player player, Level level) {
        DrugStateManager.addDrug(player, DrugType.FENTANYL, 1.0F);
    }

    public static boolean isFentanylOverdoseActive(Player player) {
        return UniversalOverdoseHandler.isOpioidOverdoseActive(player);
    }

    public static boolean isFentanylOverdoseActive(Player player, Level level) {
        return UniversalOverdoseHandler.isOpioidOverdoseActive(player, level);
    }

    public static boolean hasFentanylOverdoseState(Player player) {
        return UniversalOverdoseHandler.hasOpioidOverdoseState(player);
    }

    public static int getElapsedTicks(Player player) {
        return UniversalOverdoseHandler.getElapsedTicks(player);
    }

    public static void clearFentanylOverdose(Player player) {
        UniversalOverdoseHandler.clearOpioidOverdose(player);
    }

    private static void clearFentanylEffectAndSync(Player player, boolean startWithdrawal) {
        clearFentanylEffect(player);
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new FentanylEffectS2CPacket(false, 0, TOTAL_DURATION_TICKS), serverPlayer);
            if (startWithdrawal
                    && !serverPlayer.isDeadOrDying()
                    && !DrugStateManager.hasActiveDrugClass(serverPlayer, DrugClass.OPIOID)
                    && !OpiateWithdrawalEffectsManager.isWithdrawalActive(serverPlayer, serverPlayer.level())) {
                OpiateWithdrawalEffectsManager.startWithdrawalForOpioidDangerLoad(
                        serverPlayer,
                        serverPlayer.level(),
                        DrugType.FENTANYL.getOpioidLoad()
                );
            }
        }
    }

    private static void clearFentanylEffect(Player player) {
        if (player == null) {
            return;
        }

        removePenaltyModifiers(player);
        CompoundTag persistedData = getPersistedData(player);
        persistedData.remove(FENTANYL_REMAINING_TICKS_TAG);
        persistedData.remove(FENTANYL_COUNTERED_BY_NALOXONE_TAG);
        persistedData.remove(FENTANYL_COUNTERED_VISUAL_START_INTENSITY_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static void setRemainingDuration(Player player, int remainingTicks) {
        if (remainingTicks <= 0) {
            clearFentanylEffect(player);
            return;
        }

        CompoundTag persistedData = getPersistedData(player);
        persistedData.putInt(FENTANYL_REMAINING_TICKS_TAG, remainingTicks);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static double getScaledMovementPenalty(Player player) {
        return Mth.clamp(MOVEMENT_SPEED_PENALTY * getFentanylVisualStrength(player), -0.95D, 0.0D);
    }

    private static double getScaledAttackSpeedPenalty(Player player) {
        return Mth.clamp(ATTACK_SPEED_PENALTY * getFentanylVisualStrength(player), -0.90D, 0.0D);
    }

    private static void applyCounteredPenaltyModifiers(Player player) {
        float progress = getCounteredRemainingProgress(player);
        float startIntensity = getCounteredVisualStartIntensity(player);
        ensureModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID,
                "fentanyl_countered_sedation_movement", MOVEMENT_SPEED_PENALTY * startIntensity * progress);
        ensureModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER_ID,
                "fentanyl_countered_sedation_attack", ATTACK_SPEED_PENALTY * startIntensity * progress);
    }

    private static float getCounteredRemainingProgress(Player player) {
        return Mth.clamp(getRemainingDurationTicks(player) / (float) NALOXONE_COUNTER_DURATION_TICKS, 0.0F, 1.0F);
    }

    private static void setCounteredByNaloxone(Player player, boolean countered) {
        CompoundTag persistedData = getPersistedData(player);
        if (countered) {
            persistedData.putBoolean(FENTANYL_COUNTERED_BY_NALOXONE_TAG, true);
        } else {
            persistedData.remove(FENTANYL_COUNTERED_BY_NALOXONE_TAG);
            persistedData.remove(FENTANYL_COUNTERED_VISUAL_START_INTENSITY_TAG);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static void setCounteredVisualStartIntensity(Player player, float startIntensity) {
        CompoundTag persistedData = getPersistedData(player);
        persistedData.putFloat(FENTANYL_COUNTERED_VISUAL_START_INTENSITY_TAG, Mth.clamp(startIntensity, 0.0F, 1.0F));
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static float getCounteredVisualStartIntensity(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(FENTANYL_COUNTERED_VISUAL_START_INTENSITY_TAG)
                ? Mth.clamp(persistedData.getFloat(FENTANYL_COUNTERED_VISUAL_START_INTENSITY_TAG), 0.0F, 1.0F)
                : 0.0F;
    }

    private static void ensureModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) {
            return;
        }

        if (Math.abs(amount) < 0.0001D) {
            removeModifier(attribute, id);
            return;
        }

        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null && Math.abs(existing.getAmount() - amount) < 0.0001D) {
            return;
        }

        if (existing != null) {
            attribute.removeModifier(existing);
        }

        attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removePenaltyModifiers(Player player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID);
        removeModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER_ID);
    }

    private static void removeModifier(AttributeInstance attribute, UUID id) {
        if (attribute == null) {
            return;
        }

        AttributeModifier existing = attribute.getModifier(id);
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
