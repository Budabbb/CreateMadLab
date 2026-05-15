package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.drug.DrugStateManager;
import net.buda1bb.createmadlab.drug.PlayerDrugState;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.OpioidOverdoseEffectS2CPacket;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class UniversalOverdoseHandler {
    public static final float WARNING_THRESHOLD = 1.5F;
    public static final float ACTIVE_THRESHOLD = 1.5F;
    public static final float SEVERE_THRESHOLD = 2.4F;
    public static final float LETHAL_THRESHOLD = 2.8F;

    public static final int TOTAL_DURATION_TICKS = 1900;
    public static final int FINAL_FADE_START_TICKS = TOTAL_DURATION_TICKS - 100;
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

    private static final int WARNING_STAGE_CAP_TICKS = 200;
    private static final int RECOVERY_TICKS_PER_TICK = 2;
    private static final int NALOXONE_COUNTER_DURATION_TICKS = 5 * 20;
    private static final int SYNC_INTERVAL_TICKS = 20;

    private static final String OPIOID_OVERDOSE_ACTIVE_TAG = "OpioidOverdoseActive";
    private static final String OPIOID_OVERDOSE_ELAPSED_TICKS_TAG = "OpioidOverdoseElapsedTicks";
    private static final String OPIOID_OVERDOSE_IRREVERSIBLE_TAG = "OpioidOverdoseIrreversible";
    private static final String OPIOID_OVERDOSE_COUNTERED_BY_NALOXONE_TAG = "OpioidOverdoseCounteredByNaloxone";
    private static final String OPIOID_OVERDOSE_COUNTERED_REMAINING_TICKS_TAG = "OpioidOverdoseCounteredRemainingTicks";
    private static final String OPIOID_OVERDOSE_COUNTERED_VISUAL_START_INTENSITY_TAG = "OpioidOverdoseCounteredVisualStartIntensity";
    private static final String OPIOID_OVERDOSE_COUNTERED_BLACKOUT_START_ALPHA_TAG = "OpioidOverdoseCounteredBlackoutStartAlpha";
    private static final String OPIOID_OVERDOSE_COUNTERED_MOVEMENT_START_PENALTY_TAG = "OpioidOverdoseCounteredMovementStartPenalty";
    private static final String OPIOID_OVERDOSE_COUNTERED_JUMP_VELOCITY_START_MULTIPLIER_TAG = "OpioidOverdoseCounteredJumpVelocityStartMultiplier";
    private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "opioid_overdose_movement");
    private static final ResourceKey<DamageType> OPIOID_OVERDOSE_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "opioid_overdose")
    );

    private UniversalOverdoseHandler() {
    }

    public static void evaluateAndSync(Player player, Level level) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }

        PlayerDrugState state = DrugStateManager.calculateMixedDrugState(player);
        if (!hasActiveOverdoseData(player) && state.opioidDangerLoad >= WARNING_THRESHOLD) {
            startOpioidOverdose(player, level);
        }
    }

    public static void startOpioidOverdose(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        if (level.isClientSide) {
            ShaderUtils.activateOpioidOverdoseShaders(TOTAL_DURATION_TICKS, TOTAL_DURATION_TICKS);
            return;
        }

        CompoundTag persistedData = getPersistedData(player);
        if (!persistedData.getBoolean(OPIOID_OVERDOSE_ACTIVE_TAG)) {
            persistedData.putInt(OPIOID_OVERDOSE_ELAPSED_TICKS_TAG, 0);
        }
        persistedData.putBoolean(OPIOID_OVERDOSE_ACTIVE_TAG, true);
        persistedData.remove(OPIOID_OVERDOSE_IRREVERSIBLE_TAG);
        clearCounteredData(persistedData);
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
            clearOpioidOverdose(player);
            return;
        }

        int totalTicks = isCounteredByNaloxone(player) ? NALOXONE_COUNTER_DURATION_TICKS : TOTAL_DURATION_TICKS;
        if (isCounteredByNaloxone(player)) {
            ModMessages.sendToPlayer(new OpioidOverdoseEffectS2CPacket(
                    true,
                    remainingTicks,
                    totalTicks,
                    true,
                    getCounteredVisualStartIntensity(player),
                    getCounteredBlackoutStartAlpha(player)
            ), player);
            return;
        }

        ModMessages.sendToPlayer(new OpioidOverdoseEffectS2CPacket(true, remainingTicks, totalTicks), player);
    }

    public static void tickActiveEffect(Player player, Level level) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }

        if (player.isDeadOrDying()) {
            clearOpioidOverdoseData(player);
            removeMovementModifier(player);
            return;
        }

        PlayerDrugState state = DrugStateManager.calculateMixedDrugState(player);
        if (!hasActiveOverdoseData(player)) {
            removeMovementModifier(player);
            if (state.opioidDangerLoad >= WARNING_THRESHOLD) {
                startOpioidOverdose(player, level);
            }
            return;
        }

        if (isCounteredByNaloxone(player)) {
            tickNaloxoneCounter(player);
            return;
        }

        int elapsedTicks = getElapsedTicks(player);
        boolean irreversible = isIrreversible(player)
                || state.opioidDangerLoad >= LETHAL_THRESHOLD
                || elapsedTicks >= FINAL_FADE_START_TICKS;
        int targetElapsedTicks = getTargetElapsedTicks(state.opioidDangerLoad, irreversible);

        if (irreversible) {
            setIrreversible(player, true);
        }

        if (targetElapsedTicks > elapsedTicks) {
            elapsedTicks++;
        } else if (!irreversible && targetElapsedTicks < elapsedTicks) {
            elapsedTicks = Math.max(targetElapsedTicks, elapsedTicks - RECOVERY_TICKS_PER_TICK);
        }

        if (!irreversible && elapsedTicks >= FINAL_FADE_START_TICKS) {
            irreversible = true;
            setIrreversible(player, true);
        }

        if (!irreversible && state.opioidDangerLoad < WARNING_THRESHOLD && elapsedTicks <= 0) {
            clearOpioidOverdose(player);
            return;
        }

        setElapsedTicks(player, elapsedTicks);
        if (elapsedTicks >= TOTAL_DURATION_TICKS) {
            completeOverdoseDeath(player);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer
                && (elapsedTicks <= 1 || player.tickCount % SYNC_INTERVAL_TICKS == 0)) {
            syncActiveEffect(serverPlayer);
        }
    }

    public static void updateOngoingGameplayEffects(Player player, Level level) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }

        if (!isOpioidOverdoseActive(player, level) || player.isDeadOrDying()) {
            removeMovementModifier(player);
            return;
        }

        removeMovementModifier(player);
    }

    public static boolean isOpioidOverdoseActive(Player player) {
        return player != null && player.level().isClientSide && ShaderUtils.areOpioidOverdoseShadersActive();
    }

    public static boolean isOpioidOverdoseActive(Player player, Level level) {
        return player != null
                && level != null
                && !player.isDeadOrDying()
                && hasActiveOverdoseData(player)
                && (isCounteredByNaloxone(player)
                ? getCounteredRemainingDurationTicks(player) > 0
                : getElapsedTicks(player) < TOTAL_DURATION_TICKS);
    }

    public static boolean hasOpioidOverdoseState(Player player) {
        return hasActiveOverdoseData(player);
    }

    public static boolean isOverdosing(Player player) {
        return player != null && hasActiveOverdoseData(player) && getElapsedTicks(player) > WARNING_STAGE_CAP_TICKS;
    }

    public static float getOverdoseProgress(Player player) {
        if (player == null || !hasActiveOverdoseData(player)) {
            return 0.0F;
        }
        return Mth.clamp(getElapsedTicks(player) / (float) TOTAL_DURATION_TICKS, 0.0F, 1.0F);
    }

    public static int getElapsedTicks(Player player) {
        if (player == null) {
            return 0;
        }

        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(OPIOID_OVERDOSE_ELAPSED_TICKS_TAG)
                ? Mth.clamp(persistedData.getInt(OPIOID_OVERDOSE_ELAPSED_TICKS_TAG), 0, TOTAL_DURATION_TICKS)
                : 0;
    }

    public static int getRemainingDurationTicks(Player player) {
        if (player == null || !hasActiveOverdoseData(player)) {
            return 0;
        }

        if (isCounteredByNaloxone(player)) {
            return getCounteredRemainingDurationTicks(player);
        }

        return Math.max(0, TOTAL_DURATION_TICKS - getElapsedTicks(player));
    }

    public static int getCooldownDuration() {
        return TOTAL_DURATION_TICKS;
    }

    public static double getMovementPenalty(int elapsedTicks) {
        return 0.0D;
    }

    public static double getHorizontalJumpDistanceMultiplier(int elapsedTicks) {
        return 1.0D;
    }

    public static double getHorizontalJumpDistanceMultiplier(Player player) {
        return 1.0D;
    }

    public static double getJumpVelocityMultiplier(int elapsedTicks) {
        return 1.0D;
    }

    public static double getJumpVelocityMultiplier(Player player) {
        return 1.0D;
    }

    public static void counterWithNaloxone(Player player, Level level) {
        if (player == null || level == null || !hasActiveOverdoseData(player)) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = getPersistedData(player);
        int elapsedTicks = getElapsedTicks(player);
        float visualStartIntensity = computeVisualIntensity(elapsedTicks);
        float blackoutStartAlpha = computeBlackoutAlpha(elapsedTicks);
        double movementStartPenalty = 0.0D;
        double jumpVelocityStartMultiplier = 1.0D;
        persistedData.putBoolean(OPIOID_OVERDOSE_ACTIVE_TAG, true);
        persistedData.putBoolean(OPIOID_OVERDOSE_COUNTERED_BY_NALOXONE_TAG, true);
        persistedData.putInt(OPIOID_OVERDOSE_COUNTERED_REMAINING_TICKS_TAG, NALOXONE_COUNTER_DURATION_TICKS);
        persistedData.putFloat(OPIOID_OVERDOSE_COUNTERED_VISUAL_START_INTENSITY_TAG, Mth.clamp(visualStartIntensity, 0.0F, 1.0F));
        persistedData.putFloat(OPIOID_OVERDOSE_COUNTERED_BLACKOUT_START_ALPHA_TAG, Mth.clamp(blackoutStartAlpha, 0.0F, 1.0F));
        persistedData.putDouble(OPIOID_OVERDOSE_COUNTERED_MOVEMENT_START_PENALTY_TAG, movementStartPenalty);
        persistedData.putDouble(OPIOID_OVERDOSE_COUNTERED_JUMP_VELOCITY_START_MULTIPLIER_TAG,
                Mth.clamp(jumpVelocityStartMultiplier, 0.0D, 1.0D));
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);

        removeMovementModifier(player);
        if (player instanceof ServerPlayer serverPlayer) {
            syncActiveEffect(serverPlayer);
        }
    }

    public static void clearOpioidOverdose(Player player) {
        if (player == null) {
            return;
        }

        removeMovementModifier(player);
        clearOpioidOverdoseData(player);

        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new OpioidOverdoseEffectS2CPacket(false, 0, TOTAL_DURATION_TICKS), serverPlayer);
        } else if (player.level().isClientSide) {
            ShaderUtils.deactivateOpioidOverdoseShaders();
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

    private static int getTargetElapsedTicks(float opioidDangerLoad, boolean irreversible) {
        if (irreversible || opioidDangerLoad >= ACTIVE_THRESHOLD) {
            return TOTAL_DURATION_TICKS;
        }
        if (opioidDangerLoad >= WARNING_THRESHOLD) {
            return WARNING_STAGE_CAP_TICKS;
        }
        return 0;
    }

    private static void tickNaloxoneCounter(Player player) {
        int remainingTicks = getCounteredRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            clearOpioidOverdose(player);
            return;
        }

        setCounteredRemainingDuration(player, remainingTicks - 1);
        if (remainingTicks - 1 <= 0) {
            clearOpioidOverdose(player);
        } else if (player instanceof ServerPlayer serverPlayer && player.tickCount % SYNC_INTERVAL_TICKS == 0) {
            syncActiveEffect(serverPlayer);
        }
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
        serverPlayer.die(getOpioidOverdoseDamageSource(serverPlayer));
    }

    private static DamageSource getOpioidOverdoseDamageSource(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(OPIOID_OVERDOSE_DAMAGE_TYPE));
    }

    private static boolean hasActiveOverdoseData(Player player) {
        if (player == null) {
            return false;
        }

        CompoundTag persistedData = getPersistedData(player);
        return persistedData.getBoolean(OPIOID_OVERDOSE_ACTIVE_TAG);
    }

    private static boolean isIrreversible(Player player) {
        return player != null && getPersistedData(player).getBoolean(OPIOID_OVERDOSE_IRREVERSIBLE_TAG);
    }

    private static void setIrreversible(Player player, boolean irreversible) {
        CompoundTag persistedData = getPersistedData(player);
        if (irreversible) {
            persistedData.putBoolean(OPIOID_OVERDOSE_IRREVERSIBLE_TAG, true);
        } else {
            persistedData.remove(OPIOID_OVERDOSE_IRREVERSIBLE_TAG);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static void setElapsedTicks(Player player, int elapsedTicks) {
        CompoundTag persistedData = getPersistedData(player);
        persistedData.putBoolean(OPIOID_OVERDOSE_ACTIVE_TAG, true);
        persistedData.putInt(OPIOID_OVERDOSE_ELAPSED_TICKS_TAG, Mth.clamp(elapsedTicks, 0, TOTAL_DURATION_TICKS));
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static void clearOpioidOverdoseData(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        persistedData.remove(OPIOID_OVERDOSE_ACTIVE_TAG);
        persistedData.remove(OPIOID_OVERDOSE_ELAPSED_TICKS_TAG);
        persistedData.remove(OPIOID_OVERDOSE_IRREVERSIBLE_TAG);
        clearCounteredData(persistedData);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static void clearCounteredData(CompoundTag persistedData) {
        persistedData.remove(OPIOID_OVERDOSE_COUNTERED_BY_NALOXONE_TAG);
        persistedData.remove(OPIOID_OVERDOSE_COUNTERED_REMAINING_TICKS_TAG);
        persistedData.remove(OPIOID_OVERDOSE_COUNTERED_VISUAL_START_INTENSITY_TAG);
        persistedData.remove(OPIOID_OVERDOSE_COUNTERED_BLACKOUT_START_ALPHA_TAG);
        persistedData.remove(OPIOID_OVERDOSE_COUNTERED_MOVEMENT_START_PENALTY_TAG);
        persistedData.remove(OPIOID_OVERDOSE_COUNTERED_JUMP_VELOCITY_START_MULTIPLIER_TAG);
    }

    public static boolean isCounteredByNaloxone(Player player) {
        return player != null && getPersistedData(player).getBoolean(OPIOID_OVERDOSE_COUNTERED_BY_NALOXONE_TAG);
    }

    private static int getCounteredRemainingDurationTicks(Player player) {
        if (player == null) {
            return 0;
        }

        return Mth.clamp(getPersistedData(player).getInt(OPIOID_OVERDOSE_COUNTERED_REMAINING_TICKS_TAG),
                0, NALOXONE_COUNTER_DURATION_TICKS);
    }

    private static float getCounteredVisualStartIntensity(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(OPIOID_OVERDOSE_COUNTERED_VISUAL_START_INTENSITY_TAG)
                ? Mth.clamp(persistedData.getFloat(OPIOID_OVERDOSE_COUNTERED_VISUAL_START_INTENSITY_TAG), 0.0F, 1.0F)
                : 0.0F;
    }

    private static float getCounteredBlackoutStartAlpha(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(OPIOID_OVERDOSE_COUNTERED_BLACKOUT_START_ALPHA_TAG)
                ? Mth.clamp(persistedData.getFloat(OPIOID_OVERDOSE_COUNTERED_BLACKOUT_START_ALPHA_TAG), 0.0F, 1.0F)
                : 0.0F;
    }

    private static void setCounteredRemainingDuration(Player player, int remainingTicks) {
        CompoundTag persistedData = getPersistedData(player);
        persistedData.putBoolean(OPIOID_OVERDOSE_ACTIVE_TAG, true);
        persistedData.putBoolean(OPIOID_OVERDOSE_COUNTERED_BY_NALOXONE_TAG, true);
        persistedData.putInt(OPIOID_OVERDOSE_COUNTERED_REMAINING_TICKS_TAG,
                Mth.clamp(remainingTicks, 0, NALOXONE_COUNTER_DURATION_TICKS));
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static double getCounteredMovementPenalty(Player player) {
        return getCounteredMovementStartPenalty(player) * getCounteredRemainingProgress(player);
    }

    private static double getCounteredMovementStartPenalty(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(OPIOID_OVERDOSE_COUNTERED_MOVEMENT_START_PENALTY_TAG)
                ? Mth.clamp(persistedData.getDouble(OPIOID_OVERDOSE_COUNTERED_MOVEMENT_START_PENALTY_TAG), MAX_MOVEMENT_SLOW, 0.0D)
                : 0.0D;
    }

    private static double getCounteredJumpVelocityStartMultiplier(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(OPIOID_OVERDOSE_COUNTERED_JUMP_VELOCITY_START_MULTIPLIER_TAG)
                ? Mth.clamp(persistedData.getDouble(OPIOID_OVERDOSE_COUNTERED_JUMP_VELOCITY_START_MULTIPLIER_TAG), 0.0D, 1.0D)
                : 1.0D;
    }

    private static float getCounteredRemainingProgress(Player player) {
        return Mth.clamp(getCounteredRemainingDurationTicks(player) / (float) NALOXONE_COUNTER_DURATION_TICKS, 0.0F, 1.0F);
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
