package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.drug.DrugType;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.OpiateWithdrawalEffectS2CPacket;
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

public final class OpiateWithdrawalEffectsManager {
    private static final String WITHDRAWAL_REMAINING_TICKS_TAG = "OpiateWithdrawalRemainingTicks";
    private static final String WITHDRAWAL_TOTAL_TICKS_TAG = "OpiateWithdrawalTotalTicks";
    private static final String WITHDRAWAL_INTENSITY_TAG = "OpiateWithdrawalIntensity";
    private static final float MORPHINE_LOAD = DrugType.MORPHINE.getOpioidLoad();
    private static final float HEROIN_LOAD = DrugType.HEROIN.getOpioidLoad();
    private static final float FENTANYL_LOAD = DrugType.FENTANYL.getOpioidLoad();
    private static final int TRACE_WITHDRAWAL_DURATION_TICKS = 25 * 20;
    private static final int MORPHINE_WITHDRAWAL_DURATION_TICKS = 60 * 20;
    private static final int HEROIN_WITHDRAWAL_DURATION_TICKS = 95 * 20;
    private static final int FENTANYL_WITHDRAWAL_DURATION_TICKS = 150 * 20;
    private static final int WITHDRAWAL_FADE_IN_TICKS = 5 * 20;
    private static final int WITHDRAWAL_FADE_OUT_TICKS = 7 * 20;
    private static final float TRACE_WITHDRAWAL_INTENSITY = 0.14F;
    private static final float MORPHINE_WITHDRAWAL_INTENSITY = 0.34F;
    private static final float HEROIN_WITHDRAWAL_INTENSITY = 0.56F;
    private static final float FENTANYL_WITHDRAWAL_INTENSITY = 0.82F;
    private static final double MOVEMENT_SPEED_PENALTY = -0.24D;
    private static final double JUMP_DISTANCE_POWER = 3.0D;
    private static final double ATTACK_DAMAGE_PENALTY = -0.32D;
    private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("createmadlab", "opiate_withdrawal_movement");
    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("createmadlab", "opiate_withdrawal_attack_damage");

    private OpiateWithdrawalEffectsManager() {
    }

    public static void startMorphineWithdrawal(Player player, Level level) {
        startWithdrawalForOpioidDangerLoad(player, level, DrugType.MORPHINE.getOpioidLoad());
    }

    public static void startHeroinWithdrawal(Player player, Level level) {
        startWithdrawalForOpioidDangerLoad(player, level, DrugType.HEROIN.getOpioidLoad());
    }

    public static void startWithdrawalForOpioidLoad(Player player, Level level, float opioidLoad) {
        startWithdrawalForOpioidDangerLoad(player, level, opioidLoad);
    }

    public static void startWithdrawalForOpioidDangerLoad(Player player, Level level, float opioidDangerLoad) {
        if (opioidDangerLoad <= 0.0F) {
            return;
        }

        int durationTicks = getWithdrawalDurationForDangerLoad(opioidDangerLoad);
        float intensity = getWithdrawalIntensityForDangerLoad(opioidDangerLoad);
        startWithdrawal(player, level, durationTicks, intensity);
    }

    public static void startWithdrawal(Player player, Level level, int durationTicks, float intensity) {
        if (player == null || level == null || player.isDeadOrDying()) {
            return;
        }

        int safeDurationTicks = Math.max(1, durationTicks);
        float clampedIntensity = Mth.clamp(intensity, 0.0F, 1.0F);
        if (clampedIntensity <= 0.0F) {
            clearWithdrawalEffect(player, level);
            return;
        }

        if (level.isClientSide) {
            ShaderUtils.activateOpiateWithdrawalShaders(safeDurationTicks, safeDurationTicks, clampedIntensity);
            return;
        }

        CompoundTag persistedData = getPersistedData(player);
        persistedData.putInt(WITHDRAWAL_REMAINING_TICKS_TAG, safeDurationTicks);
        persistedData.putInt(WITHDRAWAL_TOTAL_TICKS_TAG, safeDurationTicks);
        persistedData.putFloat(WITHDRAWAL_INTENSITY_TAG, clampedIntensity);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);

        if (player instanceof ServerPlayer serverPlayer) {
            syncActiveEffect(serverPlayer);
        }
    }

    public static void tickActiveEffect(Player player, Level level) {
        if (player == null || level == null || level.isClientSide || !hasWithdrawalState(player)) {
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            clearWithdrawalEffect(player, level);
            return;
        }

        setRemainingDuration(player, remainingTicks - 1);
        if (remainingTicks - 1 <= 0) {
            clearWithdrawalEffect(player, level);
        }
    }

    public static void updateOngoingGameplayEffects(Player player, Level level) {
        if (player == null || level == null || level.isClientSide || player.isDeadOrDying()) {
            removeWithdrawalModifiers(player);
            return;
        }

        if (!isWithdrawalActive(player, level)) {
            removeWithdrawalModifiers(player);
            return;
        }

        float intensity = getCurrentWithdrawalIntensity(player);
        ensureModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID,
                "opiate_withdrawal_movement", MOVEMENT_SPEED_PENALTY * intensity, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        ensureModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_MODIFIER_ID,
                "opiate_withdrawal_attack_damage", ATTACK_DAMAGE_PENALTY * intensity, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void syncActiveEffect(ServerPlayer player) {
        if (player == null) {
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        int totalTicks = getTotalDurationTicks(player);
        float intensity = getWithdrawalIntensity(player);
        if (remainingTicks <= 0 || totalTicks <= 0 || intensity <= 0.0F) {
            clearWithdrawalEffect(player, player.level());
            return;
        }

        ModMessages.sendToPlayer(new OpiateWithdrawalEffectS2CPacket(true, remainingTicks, totalTicks, intensity), player);
    }

    public static void clearWithdrawalEffect(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        CompoundTag persistedData = getPersistedData(player);
        persistedData.remove(WITHDRAWAL_REMAINING_TICKS_TAG);
        persistedData.remove(WITHDRAWAL_TOTAL_TICKS_TAG);
        persistedData.remove(WITHDRAWAL_INTENSITY_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
        removeWithdrawalModifiers(player);

        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new OpiateWithdrawalEffectS2CPacket(false, 0, 0, 0.0F), serverPlayer);
        } else if (level.isClientSide) {
            ShaderUtils.deactivateOpiateWithdrawalShaders();
        }
    }

    public static boolean isWithdrawalActive(Player player, Level level) {
        return player != null
                && level != null
                && !player.isDeadOrDying()
                && getRemainingDurationTicks(player) > 0
                && getWithdrawalIntensity(player) > 0.0F;
    }

    public static float getCurrentWithdrawalIntensity(Player player) {
        if (player == null) {
            return 0.0F;
        }

        int totalTicks = getTotalDurationTicks(player);
        int remainingTicks = getRemainingDurationTicks(player);
        if (totalTicks <= 0 || remainingTicks <= 0) {
            return 0.0F;
        }

        return getWithdrawalIntensity(player) * computeDurationEnvelope(totalTicks, remainingTicks);
    }

    public static double getJumpDistanceMultiplier(Player player) {
        double movementSpeedMultiplier = Math.max(0.0D, 1.0D + MOVEMENT_SPEED_PENALTY * getCurrentWithdrawalIntensity(player));
        return Math.pow(movementSpeedMultiplier, JUMP_DISTANCE_POWER);
    }

    private static boolean hasWithdrawalState(Player player) {
        return player != null && getPersistedData(player).contains(WITHDRAWAL_REMAINING_TICKS_TAG);
    }

    private static int getRemainingDurationTicks(Player player) {
        if (player == null) {
            return 0;
        }

        return Math.max(0, getPersistedData(player).getInt(WITHDRAWAL_REMAINING_TICKS_TAG));
    }

    private static int getTotalDurationTicks(Player player) {
        if (player == null) {
            return 0;
        }

        return Math.max(0, getPersistedData(player).getInt(WITHDRAWAL_TOTAL_TICKS_TAG));
    }

    private static float getWithdrawalIntensity(Player player) {
        if (player == null) {
            return 0.0F;
        }

        return Mth.clamp(getPersistedData(player).getFloat(WITHDRAWAL_INTENSITY_TAG), 0.0F, 1.0F);
    }

    private static void setRemainingDuration(Player player, int remainingTicks) {
        CompoundTag persistedData = getPersistedData(player);
        persistedData.putInt(WITHDRAWAL_REMAINING_TICKS_TAG, Math.max(0, remainingTicks));
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static float computeDurationEnvelope(int totalTicks, int remainingTicks) {
        float elapsedTicks = totalTicks - remainingTicks;
        float fadeIn = smoothstep(0.0F, Math.min(WITHDRAWAL_FADE_IN_TICKS, totalTicks), elapsedTicks);
        float fadeOutStart = Math.max(0.0F, totalTicks - WITHDRAWAL_FADE_OUT_TICKS);
        float fadeOut = 1.0F - smoothstep(fadeOutStart, totalTicks, elapsedTicks);
        return Mth.clamp(fadeIn * fadeOut, 0.0F, 1.0F);
    }

    private static int getWithdrawalDurationForDangerLoad(float opioidDangerLoad) {
        return Mth.ceil(interpolateByOpioidLoad(
                opioidDangerLoad,
                TRACE_WITHDRAWAL_DURATION_TICKS,
                MORPHINE_WITHDRAWAL_DURATION_TICKS,
                HEROIN_WITHDRAWAL_DURATION_TICKS,
                FENTANYL_WITHDRAWAL_DURATION_TICKS
        ));
    }

    private static float getWithdrawalIntensityForDangerLoad(float opioidDangerLoad) {
        return interpolateByOpioidLoad(
                opioidDangerLoad,
                TRACE_WITHDRAWAL_INTENSITY,
                MORPHINE_WITHDRAWAL_INTENSITY,
                HEROIN_WITHDRAWAL_INTENSITY,
                FENTANYL_WITHDRAWAL_INTENSITY
        );
    }

    private static float interpolateByOpioidLoad(float opioidDangerLoad, float traceValue,
                                                 float morphineValue, float heroinValue, float fentanylValue) {
        float load = Math.max(0.0F, opioidDangerLoad);
        if (load <= MORPHINE_LOAD) {
            return Mth.lerp(load / Math.max(MORPHINE_LOAD, 0.0001F), traceValue, morphineValue);
        }
        if (load <= HEROIN_LOAD) {
            return Mth.lerp((load - MORPHINE_LOAD) / Math.max(HEROIN_LOAD - MORPHINE_LOAD, 0.0001F),
                    morphineValue, heroinValue);
        }
        if (load <= FENTANYL_LOAD) {
            return Mth.lerp((load - HEROIN_LOAD) / Math.max(FENTANYL_LOAD - HEROIN_LOAD, 0.0001F),
                    heroinValue, fentanylValue);
        }
        return fentanylValue;
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

    private static void removeWithdrawalModifiers(Player player) {
        if (player == null) {
            return;
        }

        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID);
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_MODIFIER_ID);
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
