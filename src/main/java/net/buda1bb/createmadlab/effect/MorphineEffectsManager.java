package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.MorphineEffectS2CPacket;
import net.buda1bb.createmadlab.drug.DrugClass;
import net.buda1bb.createmadlab.drug.DrugStateManager;
import net.buda1bb.createmadlab.drug.DrugType;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class MorphineEffectsManager {
    private static final String MORPHINE_START_TIME_TAG = "MorphineStartTime";
    private static final String MORPHINE_ACTIVE_TAG = "MorphineActive";
    private static final String MORPHINE_UNSTABLE_HP_TAG = "MorphineUnstableHp";
    private static final String MORPHINE_COUNTERED_BY_NALOXONE_TAG = "MorphineCounteredByNaloxone";
    private static final String MORPHINE_COUNTERED_REMAINING_TICKS_TAG = "MorphineCounteredRemainingTicks";
    private static final String MORPHINE_COUNTERED_VISUAL_START_INTENSITY_TAG = "MorphineCounteredVisualStartIntensity";
    private static final String MORPHINE_COUNTERED_DEBT_START_INTENSITY_TAG = "MorphineCounteredDebtStartIntensity";
    private static final String MORPHINE_COUNTERED_MOVEMENT_START_PENALTY_TAG = "MorphineCounteredMovementStartPenalty";
    private static final String MORPHINE_COUNTERED_ATTACK_START_PENALTY_TAG = "MorphineCounteredAttackStartPenalty";
    private static final int COMEUP_DURATION_TICKS = 5 * 20;
    private static final int PEAK_DURATION_TICKS = 55 * 20;
    private static final int COMEDOWN_DURATION_TICKS = 60 * 20;
    private static final int PEAK_END_TICKS = COMEUP_DURATION_TICKS + PEAK_DURATION_TICKS;
    private static final int TOTAL_DURATION = PEAK_END_TICKS + COMEDOWN_DURATION_TICKS;
    private static final int NALOXONE_FADE_DURATION_TICKS = 5 * 20;
    private static final int COOLDOWN_DURATION = TOTAL_DURATION;
    private static final int ACTIVE_DRAIN_INTERVAL_TICKS = 20;
    private static final int DEBT_DRAIN_INTERVAL_TICKS = 20;
    private static final int PEAK_SETTLE_TICKS = 6 * 20;
    private static final float PEAK_INTENSITY = 0.92F;
    private static final int MILD_DEBT_THRESHOLD = 8;
    private static final int SEVERE_DEBT_THRESHOLD = 12;
    private static final double MILD_DEBT_MOVEMENT_PENALTY = -0.12D;
    private static final double SEVERE_DEBT_MOVEMENT_PENALTY = -0.28D;
    private static final double MILD_DEBT_ATTACK_DAMAGE_PENALTY = -2.0D;
    private static final double SEVERE_DEBT_ATTACK_DAMAGE_PENALTY = -5.0D;
    private static final ResourceLocation DEBT_MOVEMENT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("createmadlab", "morphine_unstable_speed");
    private static final ResourceLocation DEBT_ATTACK_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("createmadlab", "morphine_unstable_weakness");
    private static final ThreadLocal<Boolean> APPLYING_UNSTABLE_DRAIN = ThreadLocal.withInitial(() -> false);

    private MorphineEffectsManager() {
    }

    public static void startMorphineEffect(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        if (level.isClientSide) {
            ShaderUtils.activateMorphineShaders(TOTAL_DURATION, TOTAL_DURATION, 0);
            return;
        }

        CompoundTag persistedData = getPersistedData(player);
        persistedData.putLong(MORPHINE_START_TIME_TAG, level.getGameTime());
        persistedData.putBoolean(MORPHINE_ACTIVE_TAG, true);
        clearNaloxoneFadeData(persistedData);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
        OpiateWithdrawalEffectsManager.clearWithdrawalEffect(player, level);

        if (player instanceof ServerPlayer serverPlayer) {
            syncActiveEffect(serverPlayer);
        }
    }

    public static void syncActiveEffect(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Level level = player.level();
        if (isCounteredByNaloxone(player)) {
            if (getCounteredRemainingDurationTicks(player) <= 0) {
                finishNaloxoneFade(player, level);
                return;
            }

            sendEffectState(player, false, false);
            return;
        }

        if (hasActiveMorphineWindow(player) && getRemainingDurationTicks(player, level) <= 0) {
            endMorphineEffect(player, level);
            return;
        }

        sendEffectState(player, false, false);
    }

    public static void extendMorphineEffect(Player player, Level level, int remainingTicks) {
        if (player == null || level == null || level.isClientSide || remainingTicks <= 0 || isCounteredByNaloxone(player)) {
            return;
        }

        int safeRemainingTicks = Mth.clamp(remainingTicks, 1, TOTAL_DURATION);
        long adjustedStartTime = level.getGameTime() - (TOTAL_DURATION - safeRemainingTicks);
        CompoundTag persistedData = getPersistedData(player);
        persistedData.putLong(MORPHINE_START_TIME_TAG, adjustedStartTime);
        persistedData.putBoolean(MORPHINE_ACTIVE_TAG, true);
        clearNaloxoneFadeData(persistedData);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);

        if (player instanceof ServerPlayer serverPlayer) {
            syncActiveEffect(serverPlayer);
        }
    }

    public static void handleMorphineEffectTicks(Player player, Level level, long elapsedTicks) {
        if (player == null || level == null) {
            return;
        }

        if (isCounteredByNaloxone(player)) {
            return;
        }

        if (elapsedTicks >= TOTAL_DURATION) {
            endMorphineEffect(player, level);
        }
    }

    public static void tickNaloxoneFade(Player player, Level level) {
        if (player == null || level == null || level.isClientSide || !isCounteredByNaloxone(player)) {
            return;
        }

        int remainingTicks = getCounteredRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            finishNaloxoneFade(player, level);
            return;
        }

        setCounteredRemainingDuration(player, remainingTicks - 1);
        if (remainingTicks - 1 <= 0) {
            finishNaloxoneFade(player, level);
        }
    }

    public static int convertIncomingDamage(ServerPlayer player, DamageSource source, float damageAmount) {
        if (isCounteredByNaloxone(player)) {
            return -1;
        }

        if (!shouldTrackMorphineHit(player, source, damageAmount)) {
            return -1;
        }

        int incomingHp = Math.max(1, Mth.ceil(damageAmount));
        if (shouldSpendUnstableHeartsOnHit(player)) {
            consumeUnstableHpFromHit(player, incomingHp);
            return incomingHp;
        }

        if (!shouldConvertIncomingDamage(player, source, damageAmount)) {
            return -1;
        }

        setUnstableHp(player, getUnstableHp(player) + incomingHp, false);
        sendEffectState(player, false, true);

        // The vanilla hurt pipeline has already queued hit feedback by this stage.
        // Clearing invulnerability here prevents full-conversion morphine from creating accidental iframes.
        player.invulnerableTime = 0;
        return 0;
    }

    public static void tickUnstableHp(Player player, Level level) {
        if (!(player instanceof ServerPlayer serverPlayer) || level == null || level.isClientSide || player.isDeadOrDying()) {
            return;
        }

        if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) {
            return;
        }

        int unstableHp = getUnstableHp(player);
        if (unstableHp <= 0) {
            return;
        }

        int interval = isMorphineActive(player, level) ? ACTIVE_DRAIN_INTERVAL_TICKS : DEBT_DRAIN_INTERVAL_TICKS;
        if (interval <= 0 || level.getGameTime() % interval != 0L) {
            return;
        }

        drainUnstableHp(serverPlayer, 1);
    }

    public static void updateOngoingGameplayEffects(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        if (player.isDeadOrDying()) {
            removeAllModifiers(player);
            return;
        }

        if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) {
            removeAllModifiers(player);
            return;
        }

        if (isCounteredByNaloxone(player)) {
            applyNaloxoneFadeModifiers(player);
            return;
        }

        int unstableHp = getUnstableHp(player);
        if (unstableHp >= SEVERE_DEBT_THRESHOLD) {
            applyDebtModifiers(player, SEVERE_DEBT_MOVEMENT_PENALTY, SEVERE_DEBT_ATTACK_DAMAGE_PENALTY);
            return;
        }

        if (unstableHp >= MILD_DEBT_THRESHOLD) {
            applyDebtModifiers(player, MILD_DEBT_MOVEMENT_PENALTY, MILD_DEBT_ATTACK_DAMAGE_PENALTY);
            return;
        }

        removeAllModifiers(player);
    }

    private static void applyDebtModifiers(Player player, double movementPenalty, double attackPenalty) {
        ensureModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), DEBT_MOVEMENT_SPEED_MODIFIER_ID,
                "morphine_unstable_speed", movementPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        ensureModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DEBT_ATTACK_DAMAGE_MODIFIER_ID,
                "morphine_unstable_weakness", attackPenalty, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyNaloxoneFadeModifiers(Player player) {
        float progress = getCounteredRemainingProgress(player);
        ensureModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), DEBT_MOVEMENT_SPEED_MODIFIER_ID,
                "morphine_naloxone_fade_speed", getCounteredMovementStartPenalty(player) * progress,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        ensureModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DEBT_ATTACK_DAMAGE_MODIFIER_ID,
                "morphine_naloxone_fade_weakness", getCounteredAttackStartPenalty(player) * progress,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void removeAllModifiers(Player player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), DEBT_MOVEMENT_SPEED_MODIFIER_ID);
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DEBT_ATTACK_DAMAGE_MODIFIER_ID);
    }

    public static void endMorphineEffect(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        boolean startWithdrawal = !level.isClientSide
                && hasActiveMorphineWindow(player)
                && !player.isDeadOrDying()
                && !DrugStateManager.hasActiveDrugClass(player, DrugClass.OPIOID)
                && !OpiateWithdrawalEffectsManager.isWithdrawalActive(player, level);
        removeActiveMorphineNBTData(player);
        removeAllModifiers(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendEffectState(serverPlayer, false, false);
            if (startWithdrawal) {
                OpiateWithdrawalEffectsManager.startMorphineWithdrawal(serverPlayer, level);
            }
        } else if (level.isClientSide && getUnstableHp(player) <= 0) {
            ShaderUtils.deactivateMorphineShaders();
        }
    }

    public static void cleanupMorphineEffects(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        removeAllModifiers(player);
        cleanupMorphineNBTData(player, true);

        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new MorphineEffectS2CPacket(0, 0, 0, serverPlayer.getHealth(), false, false), serverPlayer);
        } else if (level.isClientSide) {
            ShaderUtils.deactivateMorphineShaders();
        }
    }

    public static void counterWithNaloxone(Player player, Level level) {
        if (player == null || level == null || !hasMorphineState(player)) {
            return;
        }

        float visualStartIntensity = Mth.clamp(getEffectIntensity(player, level), 0.0F, 1.0F);
        int unstableHp = getUnstableHp(player);
        float debtStartIntensity = computeDebtIntensity(unstableHp);
        double movementStartPenalty = getDebtMovementPenalty(unstableHp);
        double attackStartPenalty = getDebtAttackPenalty(unstableHp);

        CompoundTag persistedData = getPersistedData(player);
        persistedData.remove(MORPHINE_START_TIME_TAG);
        persistedData.remove(MORPHINE_ACTIVE_TAG);
        persistedData.putBoolean(MORPHINE_COUNTERED_BY_NALOXONE_TAG, true);
        persistedData.putInt(MORPHINE_COUNTERED_REMAINING_TICKS_TAG, NALOXONE_FADE_DURATION_TICKS);
        persistedData.putFloat(MORPHINE_COUNTERED_VISUAL_START_INTENSITY_TAG, visualStartIntensity);
        persistedData.putFloat(MORPHINE_COUNTERED_DEBT_START_INTENSITY_TAG, debtStartIntensity);
        persistedData.putDouble(MORPHINE_COUNTERED_MOVEMENT_START_PENALTY_TAG, movementStartPenalty);
        persistedData.putDouble(MORPHINE_COUNTERED_ATTACK_START_PENALTY_TAG, attackStartPenalty);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);

        removeAllModifiers(player);
        if (player instanceof ServerPlayer serverPlayer) {
            sendEffectState(serverPlayer, false, false);
        }
    }

    private static void finishNaloxoneFade(Player player, Level level) {
        removeAllModifiers(player);
        cleanupMorphineNBTData(player, false);
        if (player instanceof ServerPlayer serverPlayer) {
            sendEffectState(serverPlayer, false, false);
        } else if (level.isClientSide && getUnstableHp(player) <= 0) {
            ShaderUtils.deactivateMorphineShaders();
        }

    }

    private static void cleanupMorphineNBTData(Player player, boolean clearUnstableHp) {
        CompoundTag persistedData = getPersistedData(player);
        persistedData.remove(MORPHINE_START_TIME_TAG);
        persistedData.remove(MORPHINE_ACTIVE_TAG);
        if (clearUnstableHp) {
            persistedData.remove(MORPHINE_UNSTABLE_HP_TAG);
        }
        clearNaloxoneFadeData(persistedData);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    public static boolean isMorphineActive(Player player) {
        return player != null && player.level().isClientSide && ShaderUtils.areMorphineShadersActive();
    }

    public static boolean isMorphineActive(Player player, Level level) {
        if (player == null || level == null || player.isDeadOrDying()) {
            return false;
        }

        if (isCounteredByNaloxone(player)) {
            return getCounteredRemainingDurationTicks(player) > 0;
        }

        CompoundTag persistedData = getPersistedData(player);
        return persistedData.getBoolean(MORPHINE_ACTIVE_TAG) && getRemainingDurationTicks(player, level) > 0;
    }

    public static boolean hasActiveMorphineWindow(Player player) {
        if (player == null) {
            return false;
        }

        CompoundTag persistedData = getPersistedData(player);
        return persistedData.getBoolean(MORPHINE_ACTIVE_TAG) && persistedData.contains(MORPHINE_START_TIME_TAG);
    }

    public static long getMorphineStartTime(Player player) {
        if (player == null) {
            return 0L;
        }

        return getPersistedData(player).getLong(MORPHINE_START_TIME_TAG);
    }

    public static int getRemainingDurationTicks(Player player, Level level) {
        if (player == null || level == null) {
            return 0;
        }

        CompoundTag persistedData = getPersistedData(player);
        if (persistedData.getBoolean(MORPHINE_COUNTERED_BY_NALOXONE_TAG)) {
            return getCounteredRemainingDurationTicks(player);
        }

        if (!persistedData.getBoolean(MORPHINE_ACTIVE_TAG) || !persistedData.contains(MORPHINE_START_TIME_TAG)) {
            return 0;
        }

        long elapsedTicks = level.getGameTime() - persistedData.getLong(MORPHINE_START_TIME_TAG);
        return Math.max(0, TOTAL_DURATION - (int) Math.min(elapsedTicks, Integer.MAX_VALUE));
    }

    public static float getEffectIntensity(Player player, Level level) {
        if (player == null || level == null) {
            return 0.0F;
        }

        int remainingTicks = getRemainingDurationTicks(player, level);
        if (remainingTicks <= 0) {
            return 0.0F;
        }

        if (isCounteredByNaloxone(player)) {
            return getCounteredVisualStartIntensity(player) * getCounteredRemainingProgress(player);
        }

        return computeEffectIntensity(TOTAL_DURATION, TOTAL_DURATION - remainingTicks) * getMorphineVisualStrength(player);
    }

    public static boolean hasMorphineState(Player player) {
        if (player == null) {
            return false;
        }

        CompoundTag persistedData = getPersistedData(player);
        return persistedData.getBoolean(MORPHINE_ACTIVE_TAG)
                || persistedData.getBoolean(MORPHINE_COUNTERED_BY_NALOXONE_TAG)
                || getUnstableHp(player) > 0;
    }

    public static int getUnstableHp(Player player) {
        if (player == null) {
            return 0;
        }

        return Math.max(0, getPersistedData(player).getInt(MORPHINE_UNSTABLE_HP_TAG));
    }

    public static int getTotalDuration() {
        return TOTAL_DURATION;
    }

    public static float getComeupEndPhase() {
        return COMEUP_DURATION_TICKS / (float) TOTAL_DURATION;
    }

    public static float getPeakEndPhase() {
        return PEAK_END_TICKS / (float) TOTAL_DURATION;
    }

    public static int getCooldownDuration() {
        return COOLDOWN_DURATION;
    }

    public static float computeDebtIntensity(int unstableHp) {
        return Mth.clamp(unstableHp / 12.0F, 0.0F, 1.0F);
    }

    public static float getMorphineVisualStrength(Player player) {
        if (player == null) {
            return 0.0F;
        }

        float strength = DrugStateManager.getDrugVisualStrength(player, DrugType.MORPHINE);
        if (strength <= 0.0F && player.level() != null && isMorphineActive(player, player.level())) {
            return 1.0F;
        }
        return Mth.clamp(strength, 0.0F, 3.0F);
    }

    public static float computeEffectIntensity(int totalTicks, float elapsedTicks) {
        int safeTotalTicks = Math.max(1, totalTicks);
        float clampedElapsedTicks = Mth.clamp(elapsedTicks, 0.0F, safeTotalTicks);
        float safeComeupEnd = Math.min(COMEUP_DURATION_TICKS, safeTotalTicks);
        float safePeakEnd = Math.min(PEAK_END_TICKS, safeTotalTicks);
        float settleEnd = Math.min(COMEUP_DURATION_TICKS + PEAK_SETTLE_TICKS, safePeakEnd);

        if (clampedElapsedTicks <= safeComeupEnd) {
            return smoothstep(0.0F, Math.max(1.0F, safeComeupEnd), clampedElapsedTicks);
        }

        if (clampedElapsedTicks <= settleEnd) {
            float settleProgress = smoothstep(safeComeupEnd, Math.max(safeComeupEnd + 1.0F, settleEnd), clampedElapsedTicks);
            return Mth.lerp(settleProgress, 1.0F, PEAK_INTENSITY);
        }

        if (clampedElapsedTicks <= safePeakEnd) {
            return PEAK_INTENSITY;
        }

        if (safePeakEnd >= safeTotalTicks) {
            return PEAK_INTENSITY;
        }

        return PEAK_INTENSITY * (1.0F - smoothstep(safePeakEnd, safeTotalTicks, clampedElapsedTicks));
    }

    private static boolean isCounteredByNaloxone(Player player) {
        return player != null && getPersistedData(player).getBoolean(MORPHINE_COUNTERED_BY_NALOXONE_TAG);
    }

    private static int getCounteredRemainingDurationTicks(Player player) {
        if (player == null) {
            return 0;
        }

        return Mth.clamp(getPersistedData(player).getInt(MORPHINE_COUNTERED_REMAINING_TICKS_TAG),
                0, NALOXONE_FADE_DURATION_TICKS);
    }

    private static void setCounteredRemainingDuration(Player player, int remainingTicks) {
        CompoundTag persistedData = getPersistedData(player);
        persistedData.putBoolean(MORPHINE_COUNTERED_BY_NALOXONE_TAG, true);
        persistedData.putInt(MORPHINE_COUNTERED_REMAINING_TICKS_TAG,
                Mth.clamp(remainingTicks, 0, NALOXONE_FADE_DURATION_TICKS));
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static float getCounteredRemainingProgress(Player player) {
        return Mth.clamp(getCounteredRemainingDurationTicks(player) / (float) NALOXONE_FADE_DURATION_TICKS, 0.0F, 1.0F);
    }

    private static float getCounteredVisualStartIntensity(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(MORPHINE_COUNTERED_VISUAL_START_INTENSITY_TAG)
                ? Mth.clamp(persistedData.getFloat(MORPHINE_COUNTERED_VISUAL_START_INTENSITY_TAG), 0.0F, 1.0F)
                : 0.0F;
    }

    private static float getCounteredDebtStartIntensity(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(MORPHINE_COUNTERED_DEBT_START_INTENSITY_TAG)
                ? Mth.clamp(persistedData.getFloat(MORPHINE_COUNTERED_DEBT_START_INTENSITY_TAG), 0.0F, 1.0F)
                : 0.0F;
    }

    private static double getCounteredMovementStartPenalty(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(MORPHINE_COUNTERED_MOVEMENT_START_PENALTY_TAG)
                ? Mth.clamp(persistedData.getDouble(MORPHINE_COUNTERED_MOVEMENT_START_PENALTY_TAG), SEVERE_DEBT_MOVEMENT_PENALTY, 0.0D)
                : 0.0D;
    }

    private static double getCounteredAttackStartPenalty(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(MORPHINE_COUNTERED_ATTACK_START_PENALTY_TAG)
                ? Mth.clamp(persistedData.getDouble(MORPHINE_COUNTERED_ATTACK_START_PENALTY_TAG), SEVERE_DEBT_ATTACK_DAMAGE_PENALTY, 0.0D)
                : 0.0D;
    }

    private static double getDebtMovementPenalty(int unstableHp) {
        if (unstableHp >= SEVERE_DEBT_THRESHOLD) {
            return SEVERE_DEBT_MOVEMENT_PENALTY;
        }
        if (unstableHp >= MILD_DEBT_THRESHOLD) {
            return MILD_DEBT_MOVEMENT_PENALTY;
        }
        return 0.0D;
    }

    private static double getDebtAttackPenalty(int unstableHp) {
        if (unstableHp >= SEVERE_DEBT_THRESHOLD) {
            return SEVERE_DEBT_ATTACK_DAMAGE_PENALTY;
        }
        if (unstableHp >= MILD_DEBT_THRESHOLD) {
            return MILD_DEBT_ATTACK_DAMAGE_PENALTY;
        }
        return 0.0D;
    }

    private static void clearNaloxoneFadeData(CompoundTag persistedData) {
        persistedData.remove(MORPHINE_COUNTERED_BY_NALOXONE_TAG);
        persistedData.remove(MORPHINE_COUNTERED_REMAINING_TICKS_TAG);
        persistedData.remove(MORPHINE_COUNTERED_VISUAL_START_INTENSITY_TAG);
        persistedData.remove(MORPHINE_COUNTERED_DEBT_START_INTENSITY_TAG);
        persistedData.remove(MORPHINE_COUNTERED_MOVEMENT_START_PENALTY_TAG);
        persistedData.remove(MORPHINE_COUNTERED_ATTACK_START_PENALTY_TAG);
    }

    private static CompoundTag getPersistedData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            persistentData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistentData.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static long getElapsedTicks(Player player, Level level) {
        return Math.max(0L, level.getGameTime() - getMorphineStartTime(player));
    }

    private static boolean shouldConvertIncomingDamage(ServerPlayer player, DamageSource source, float damageAmount) {
        if (!shouldTrackMorphineHit(player, source, damageAmount)) {
            return false;
        }

        if (isCounteredByNaloxone(player)
                || !isMorphineActive(player, player.level())
                || player.isCreative()
                || player.isSpectator()
                || player.getAbilities().invulnerable) {
            return false;
        }

        return !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && !source.is(DamageTypes.GENERIC_KILL)
                && !source.isCreativePlayer();
    }

    private static boolean shouldTrackMorphineHit(ServerPlayer player, DamageSource source, float damageAmount) {
        return player != null
                && source != null
                && damageAmount > 0.0F
                && !APPLYING_UNSTABLE_DRAIN.get()
                && !player.isCreative()
                && !player.isSpectator()
                && !player.getAbilities().invulnerable
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && !source.is(DamageTypes.GENERIC_KILL)
                && !source.isCreativePlayer();
    }

    private static boolean shouldSpendUnstableHeartsOnHit(ServerPlayer player) {
        int unstableHp = getUnstableHp(player);
        int currentHealthHp = getCurrentHealthHp(player);
        return unstableHp > 0 && currentHealthHp > 0 && unstableHp >= currentHealthHp;
    }

    private static void consumeUnstableHpFromHit(ServerPlayer player, int damageHp) {
        int currentUnstableHp = getUnstableHp(player);
        int currentHealthHp = getCurrentHealthHp(player);
        int unstableHpToConsume = Math.min(Math.max(0, damageHp), Math.min(currentUnstableHp, currentHealthHp));
        if (unstableHpToConsume <= 0) {
            return;
        }

        setUnstableHp(player, currentUnstableHp - unstableHpToConsume, false);
        sendEffectState(player, false, true);
    }

    private static void drainUnstableHp(ServerPlayer player, int hpToDrain) {
        int currentDebt = getUnstableHp(player);
        int actualDrain = Math.min(currentDebt, Math.max(0, hpToDrain));
        if (actualDrain <= 0) {
            return;
        }

        setUnstableHp(player, currentDebt - actualDrain, false);
        APPLYING_UNSTABLE_DRAIN.set(true);
        try {
            // Debt drain bypasses the normal hurt pipeline so it stays silent and does not re-run hit reactions.
            float remainingHealth = player.getHealth() - actualDrain;
            if (remainingHealth > 0.0F) {
                sendEffectState(player, remainingHealth, true, false);
                player.setHealth(remainingHealth);
            } else {
                player.setHealth(0.0F);
                player.die(player.damageSources().generic());
            }
        } finally {
            APPLYING_UNSTABLE_DRAIN.set(false);
        }

        if (!player.isDeadOrDying() && player.getHealth() <= 0.0F) {
            sendEffectState(player, false, false);
        }
    }

    private static void setUnstableHp(Player player, int unstableHp, boolean syncClient) {
        CompoundTag persistedData = getPersistedData(player);
        int clampedUnstableHp = Math.max(0, unstableHp);
        if (clampedUnstableHp > 0) {
            persistedData.putInt(MORPHINE_UNSTABLE_HP_TAG, clampedUnstableHp);
        } else {
            persistedData.remove(MORPHINE_UNSTABLE_HP_TAG);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);

        if (syncClient && player instanceof ServerPlayer serverPlayer) {
            sendEffectState(serverPlayer, false, false);
        }
    }

    private static void removeActiveMorphineNBTData(Player player) {
        cleanupMorphineNBTData(player, false);
    }

    private static int getCurrentHealthHp(Player player) {
        if (player == null) {
            return 0;
        }

        return Math.max(0, Mth.ceil(player.getHealth()));
    }

    private static void sendEffectState(ServerPlayer player, boolean silentDecay, boolean convertedDamage) {
        sendEffectState(player, player.getHealth(), silentDecay, convertedDamage);
    }

    private static void sendEffectState(ServerPlayer player, float syncedHealth, boolean silentDecay, boolean convertedDamage) {
        if (!isCounteredByNaloxone(player) && OpiateWithdrawalEffectsManager.isWithdrawalActive(player, player.level())) {
            int unstableHp = getUnstableHp(player);
            ModMessages.sendToPlayer(new MorphineEffectS2CPacket(
                    0,
                    NALOXONE_FADE_DURATION_TICKS,
                    unstableHp,
                    syncedHealth,
                    silentDecay,
                    convertedDamage,
                    unstableHp > 0 || silentDecay,
                    0.0F,
                    0.0F,
                    getMorphineVisualStrength(player)
            ), player);
            return;
        }

        if (isCounteredByNaloxone(player)) {
            ModMessages.sendToPlayer(new MorphineEffectS2CPacket(
                    getCounteredRemainingDurationTicks(player),
                    NALOXONE_FADE_DURATION_TICKS,
                    getUnstableHp(player),
                    syncedHealth,
                    silentDecay,
                    convertedDamage,
                    true,
                    getCounteredVisualStartIntensity(player),
                    getCounteredDebtStartIntensity(player),
                    getMorphineVisualStrength(player)
            ), player);
            return;
        }

        int unstableHp = getUnstableHp(player);
        int remainingTicks = getRemainingDurationTicks(player, player.level());
        if (remainingTicks <= 0 && unstableHp <= 0) {
            ModMessages.sendToPlayer(new MorphineEffectS2CPacket(0, 0, 0, syncedHealth, false, false), player);
            return;
        }

        ModMessages.sendToPlayer(new MorphineEffectS2CPacket(
                Math.max(0, remainingTicks),
                TOTAL_DURATION,
                unstableHp,
                syncedHealth,
                silentDecay,
                convertedDamage,
                false,
                0.0F,
                0.0F,
                getMorphineVisualStrength(player)
        ), player);
    }

    private static void ensureModifier(AttributeInstance attribute, ResourceLocation id, String name, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }

        if (Math.abs(amount) < 0.0001D) {
            removeModifier(attribute, id);
            return;
        }

        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 0.0001D && existing.operation() == operation) {
            return;
        }

        if (existing != null) {
            attribute.removeModifier(existing);
        }

        attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
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

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / Math.max(edge1 - edge0, 0.0001F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
