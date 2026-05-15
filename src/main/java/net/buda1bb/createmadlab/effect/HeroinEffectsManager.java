package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.HeroinEffectS2CPacket;
import net.buda1bb.createmadlab.drug.DrugClass;
import net.buda1bb.createmadlab.drug.DrugStateManager;
import net.buda1bb.createmadlab.drug.DrugType;
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

public final class HeroinEffectsManager {
    private static final String HEROIN_REMAINING_TICKS_TAG = "HeroinRemainingTicks";
    private static final String HEROIN_LAST_DAMAGE_TICK_TAG = "HeroinLastDamageTick";
    private static final String HEROIN_COUNTERED_BY_NALOXONE_TAG = "HeroinCounteredByNaloxone";
    private static final String HEROIN_COUNTERED_VISUAL_START_INTENSITY_TAG = "HeroinCounteredVisualStartIntensity";
    private static final int TOTAL_DURATION = 185 * 20;
    private static final int NALOXONE_COUNTER_DURATION_TICKS = 5 * 20;
    private static final int FADE_IN_TICKS = 5 * 20;
    private static final int PEAK_HOLD_TICKS = 60 * 20;
    private static final double GROUP_AGGRO_RADIUS = 12.0D;
    private static final int GROUP_AGGRO_DURATION_TICKS = 200;
    private static final float DAMAGE_MULTIPLIER = 0.50F;
    private static final double MOVEMENT_SPEED_PENALTY = -0.50D;
    private static final double JUMP_DISTANCE_POWER = 3.0D;
    private static final float MINING_SPEED_MULTIPLIER = 0.90F;
    private static final double ATTACK_SPEED_PENALTY = -0.15D;
    private static final int COMFORT_RECOVERY_DELAY_TICKS = 120;
    private static final int COMFORT_HEAL_INTERVAL_TICKS = 80;
    private static final float COMFORT_HEAL_AMOUNT = 0.5F;
    private static final float COMFORT_MIN_INTENSITY = 0.35F;
    private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("createmadlab", "heroin_sedation_movement");
    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("createmadlab", "heroin_sedation_attack");

    private HeroinEffectsManager() {
    }

    public static void startHeroinEffect(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        if (level.isClientSide) {
            ShaderUtils.activateHeroinShaders(TOTAL_DURATION);
            return;
        }

        setRemainingDuration(player, TOTAL_DURATION);
        setCounteredByNaloxone(player, false);
        OpiateWithdrawalEffectsManager.clearWithdrawalEffect(player, level);
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new HeroinEffectS2CPacket(true, TOTAL_DURATION, TOTAL_DURATION,
                    getHeroinVisualStrength(player), true, false), serverPlayer);
        }
    }

    public static void syncActiveEffect(ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (!DrugStateManager.hasActiveDrug(player, DrugType.HEROIN) && !isCounteredByNaloxone(player)) {
            if (getRemainingDurationTicks(player) > 0) {
                cleanupHeroinEffect(player, player.level());
            }
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            cleanupHeroinEffect(player, player.level());
            return;
        }

        if (isCounteredByNaloxone(player)) {
            ModMessages.sendToPlayer(new HeroinEffectS2CPacket(
                    true,
                    remainingTicks,
                    NALOXONE_COUNTER_DURATION_TICKS,
                    getCounteredVisualStartIntensity(player),
                    false,
                    true
            ), player);
            return;
        }

        ModMessages.sendToPlayer(new HeroinEffectS2CPacket(true, remainingTicks, TOTAL_DURATION,
                getHeroinVisualStrength(player), true, false), player);
    }

    public static void extendHeroinEffect(Player player, int remainingTicks) {
        if (player == null || remainingTicks <= 0 || isCounteredByNaloxone(player)) {
            return;
        }

        int safeRemainingTicks = Mth.clamp(remainingTicks, 1, TOTAL_DURATION);
        setRemainingDuration(player, safeRemainingTicks);
        setCounteredByNaloxone(player, false);
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new HeroinEffectS2CPacket(true, safeRemainingTicks, TOTAL_DURATION,
                    getHeroinVisualStrength(player), true, false), serverPlayer);
        }
    }

    public static void tickActiveEffect(Player player) {
        if (player == null) {
            return;
        }

        if (!DrugStateManager.hasActiveDrug(player, DrugType.HEROIN) && !isCounteredByNaloxone(player)) {
            if (getRemainingDurationTicks(player) > 0) {
                cleanupHeroinEffect(player, player.level());
            }
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            clearHeroinEffect(player);
            return;
        }

        if (remainingTicks <= 1) {
            clearHeroinEffectAndSync(player, true);
            return;
        }

        setRemainingDuration(player, remainingTicks - 1);
    }

    public static void updateOngoingGameplayEffects(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        if (!isHeroinActive(player, level) || player.isDeadOrDying()) {
            removePenaltyModifiers(player);
            return;
        }

        if (isCounteredByNaloxone(player)) {
            applyCounteredPenaltyModifiers(player);
            return;
        }

        applyFullPenaltyModifiers(player);
        maybeApplyComfortHealing(player, level);
    }

    public static void recordDamageTaken(Player player, Level level) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }

        CompoundTag persistedData = getPersistedData(player);
        persistedData.putLong(HEROIN_LAST_DAMAGE_TICK_TAG, level.getGameTime());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    public static float getEffectIntensity(Player player, Level level) {
        if (player == null || level == null) {
            return 0.0F;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            return 0.0F;
        }

        if (!DrugStateManager.hasActiveDrug(player, DrugType.HEROIN) && !isCounteredByNaloxone(player)) {
            return 0.0F;
        }

        return computeEffectIntensity(TOTAL_DURATION, TOTAL_DURATION - remainingTicks) * getHeroinVisualStrength(player);
    }

    public static void cleanupHeroinEffect(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        clearHeroinEffect(player);
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new HeroinEffectS2CPacket(false, 0), serverPlayer);
        } else if (level.isClientSide) {
            ShaderUtils.deactivateHeroinShaders();
        }
    }

    public static void counterWithNaloxone(Player player, Level level) {
        if (player == null || level == null || !isHeroinActive(player, level)) {
            return;
        }

        int counteredTicks = NALOXONE_COUNTER_DURATION_TICKS;
        float visualStartIntensity = Mth.clamp(getEffectIntensity(player, level), 0.0F, 1.0F);
        setRemainingDuration(player, counteredTicks);
        setCounteredByNaloxone(player, true);
        setCounteredVisualStartIntensity(player, visualStartIntensity);
        removePenaltyModifiers(player);

        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new HeroinEffectS2CPacket(
                    true,
                    counteredTicks,
                    NALOXONE_COUNTER_DURATION_TICKS,
                    visualStartIntensity,
                    false,
                    true
            ), serverPlayer);
        }
    }

    public static boolean isHeroinActive(Player player) {
        return player != null && player.level().isClientSide && ShaderUtils.areHeroinShadersActive();
    }

    public static boolean isHeroinActive(Player player, Level level) {
        return player != null
                && level != null
                && !player.isDeadOrDying()
                && getRemainingDurationTicks(player) > 0
                && (DrugStateManager.hasActiveDrug(player, DrugType.HEROIN) || isCounteredByNaloxone(player));
    }

    public static int getRemainingDurationTicks(Player player) {
        if (player == null) {
            return 0;
        }

        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(HEROIN_REMAINING_TICKS_TAG)
                ? Math.max(0, persistedData.getInt(HEROIN_REMAINING_TICKS_TAG))
                : 0;
    }

    public static float getMiningSpeedMultiplier() {
        return MINING_SPEED_MULTIPLIER;
    }

    public static float getMiningSpeedMultiplier(Player player) {
        if (!isCounteredByNaloxone(player)) {
            return scaleBelowOneMultiplier(MINING_SPEED_MULTIPLIER, getHeroinGameplayStrength(player));
        }

        float remainingProgress = getCounteredRemainingProgress(player);
        return 1.0F - (1.0F - MINING_SPEED_MULTIPLIER) * remainingProgress;
    }

    public static float getDamageMultiplier() {
        return DAMAGE_MULTIPLIER;
    }

    public static float getDamageMultiplier(Player player) {
        if (!isCounteredByNaloxone(player)) {
            return scaleBelowOneMultiplier(DAMAGE_MULTIPLIER, getHeroinGameplayStrength(player));
        }

        float remainingProgress = getCounteredRemainingProgress(player);
        return 1.0F - (1.0F - DAMAGE_MULTIPLIER) * remainingProgress;
    }

    public static double getJumpDistanceMultiplier() {
        return getJumpDistanceMultiplierForMovementPenalty(MOVEMENT_SPEED_PENALTY);
    }

    public static double getJumpDistanceMultiplier(Player player) {
        if (!isCounteredByNaloxone(player)) {
            return getJumpDistanceMultiplierForMovementPenalty(getScaledMovementPenalty(player));
        }

        float remainingProgress = getCounteredRemainingProgress(player);
        return getJumpDistanceMultiplierForMovementPenalty(MOVEMENT_SPEED_PENALTY * remainingProgress);
    }

    public static double getGroupAggroRadius() {
        return GROUP_AGGRO_RADIUS;
    }

    public static int getGroupAggroDurationTicks() {
        return GROUP_AGGRO_DURATION_TICKS;
    }

    public static float getHeroinVisualStrength(Player player) {
        if (player == null) {
            return 0.0F;
        }

        float strength = DrugStateManager.getDrugVisualStrength(player, DrugType.HEROIN);
        return Mth.clamp(strength, 0.0F, 3.0F);
    }

    public static boolean shouldPacifyMobs(Player player) {
        return player != null && !isCounteredByNaloxone(player);
    }

    public static boolean isCounteredByNaloxone(Player player) {
        return player != null && getPersistedData(player).getBoolean(HEROIN_COUNTERED_BY_NALOXONE_TAG);
    }

    public static void clearHeroinEffect(Player player) {
        if (player == null) {
            return;
        }

        removePenaltyModifiers(player);

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedData.remove(HEROIN_REMAINING_TICKS_TAG);
        persistedData.remove(HEROIN_LAST_DAMAGE_TICK_TAG);
        persistedData.remove(HEROIN_COUNTERED_BY_NALOXONE_TAG);
        persistedData.remove(HEROIN_COUNTERED_VISUAL_START_INTENSITY_TAG);
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    public static int getTotalDuration() {
        return TOTAL_DURATION;
    }

    public static int getCooldownDuration() {
        return TOTAL_DURATION;
    }

    public static float computeEffectIntensity(int totalTicks, float elapsedTicks) {
        int safeTotalTicks = Math.max(1, totalTicks);
        float clampedElapsedTicks = Mth.clamp(elapsedTicks, 0.0F, safeTotalTicks);
        float fadeInEnd = Math.min(FADE_IN_TICKS, safeTotalTicks);
        float peakEnd = Math.min(fadeInEnd + PEAK_HOLD_TICKS, safeTotalTicks);

        if (clampedElapsedTicks <= fadeInEnd) {
            return smoothstep(0.0F, fadeInEnd, clampedElapsedTicks);
        }

        if (clampedElapsedTicks <= peakEnd) {
            return 1.0F;
        }

        if (peakEnd >= safeTotalTicks) {
            return 1.0F;
        }

        return 1.0F - smoothstep(peakEnd, safeTotalTicks, clampedElapsedTicks);
    }

    private static void maybeApplyComfortHealing(Player player, Level level) {
        if (getEffectIntensity(player, level) < COMFORT_MIN_INTENSITY) {
            return;
        }

        long lastDamageTick = getLastDamageTick(player);
        if (level.getGameTime() - lastDamageTick < COMFORT_RECOVERY_DELAY_TICKS) {
            return;
        }

        if (player.getHealth() >= player.getMaxHealth()) {
            return;
        }

        if (Math.floorMod(player.tickCount + player.getId(), COMFORT_HEAL_INTERVAL_TICKS) != 0) {
            return;
        }

        player.heal(COMFORT_HEAL_AMOUNT);
    }

    private static long getLastDamageTick(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(HEROIN_LAST_DAMAGE_TICK_TAG)
                ? persistedData.getLong(HEROIN_LAST_DAMAGE_TICK_TAG)
                : Long.MIN_VALUE / 4L;
    }

    private static void applyFullPenaltyModifiers(Player player) {
        ensureModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID,
                "heroin_sedation_movement", getScaledMovementPenalty(player));
        ensureModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER_ID,
                "heroin_sedation_attack", getScaledAttackSpeedPenalty(player));
    }

    private static void applyCounteredPenaltyModifiers(Player player) {
        float remainingProgress = getCounteredRemainingProgress(player);
        ensureModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID,
                "heroin_countered_sedation_movement", MOVEMENT_SPEED_PENALTY * remainingProgress);
        ensureModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER_ID,
                "heroin_countered_sedation_attack", ATTACK_SPEED_PENALTY * remainingProgress);
    }

    private static void removePenaltyModifiers(Player player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID);
        removeModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER_ID);
    }

    private static void ensureModifier(AttributeInstance attribute, ResourceLocation id, String name, double amount) {
        if (attribute == null) {
            return;
        }

        if (Math.abs(amount) < 0.0001D) {
            removeModifier(attribute, id);
            return;
        }

        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 0.0001D) {
            return;
        }

        if (existing != null) {
            attribute.removeModifier(existing);
        }

        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute == null) {
            return;
        }

        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null) {
            attribute.removeModifier(existing);
        }
    }

    private static double getJumpDistanceMultiplierForMovementPenalty(double movementPenalty) {
        double movementSpeedMultiplier = Math.max(0.0D, 1.0D + movementPenalty);
        return Math.pow(movementSpeedMultiplier, JUMP_DISTANCE_POWER);
    }

    private static float getHeroinGameplayStrength(Player player) {
        return Mth.clamp(getHeroinVisualStrength(player), 0.0F, 3.0F);
    }

    private static double getScaledMovementPenalty(Player player) {
        return Mth.clamp(MOVEMENT_SPEED_PENALTY * getHeroinGameplayStrength(player), -0.95D, 0.0D);
    }

    private static double getScaledAttackSpeedPenalty(Player player) {
        return Mth.clamp(ATTACK_SPEED_PENALTY * getHeroinGameplayStrength(player), -0.90D, 0.0D);
    }

    private static float scaleBelowOneMultiplier(float baseMultiplier, float strength) {
        float penalty = 1.0F - baseMultiplier;
        return Mth.clamp(1.0F - penalty * Math.max(0.0F, strength), 0.05F, 1.0F);
    }

    private static void setRemainingDuration(Player player, int remainingTicks) {
        if (remainingTicks <= 0) {
            clearHeroinEffect(player);
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedData.putInt(HEROIN_REMAINING_TICKS_TAG, remainingTicks);
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static float getCounteredRemainingProgress(Player player) {
        return Mth.clamp(getRemainingDurationTicks(player) / (float) NALOXONE_COUNTER_DURATION_TICKS, 0.0F, 1.0F);
    }

    private static void setCounteredByNaloxone(Player player, boolean countered) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        if (countered) {
            persistedData.putBoolean(HEROIN_COUNTERED_BY_NALOXONE_TAG, true);
        } else {
            persistedData.remove(HEROIN_COUNTERED_BY_NALOXONE_TAG);
        }
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static void setCounteredVisualStartIntensity(Player player, float startIntensity) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedData.putFloat(HEROIN_COUNTERED_VISUAL_START_INTENSITY_TAG, Mth.clamp(startIntensity, 0.0F, 1.0F));
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static float getCounteredVisualStartIntensity(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(HEROIN_COUNTERED_VISUAL_START_INTENSITY_TAG)
                ? Mth.clamp(persistedData.getFloat(HEROIN_COUNTERED_VISUAL_START_INTENSITY_TAG), 0.0F, 1.0F)
                : 0.0F;
    }

    private static void clearHeroinEffectAndSync(Player player, boolean startWithdrawal) {
        clearHeroinEffect(player);
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new HeroinEffectS2CPacket(false, 0), serverPlayer);
            if (startWithdrawal
                    && !serverPlayer.isDeadOrDying()
                    && !DrugStateManager.hasActiveDrugClass(serverPlayer, DrugClass.OPIOID)
                    && !OpiateWithdrawalEffectsManager.isWithdrawalActive(serverPlayer, serverPlayer.level())) {
                OpiateWithdrawalEffectsManager.startHeroinWithdrawal(serverPlayer, serverPlayer.level());
            }
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
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
