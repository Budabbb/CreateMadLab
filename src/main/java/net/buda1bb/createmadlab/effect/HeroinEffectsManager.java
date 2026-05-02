package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.HeroinEffectS2CPacket;
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

public final class HeroinEffectsManager {
    private static final String HEROIN_REMAINING_TICKS_TAG = "HeroinRemainingTicks";
    private static final String HEROIN_LAST_DAMAGE_TICK_TAG = "HeroinLastDamageTick";
    private static final int TOTAL_DURATION = 185 * 20;
    private static final int FADE_IN_TICKS = 5 * 20;
    private static final int PEAK_HOLD_TICKS = 60 * 20;
    private static final double GROUP_AGGRO_RADIUS = 12.0D;
    private static final int GROUP_AGGRO_DURATION_TICKS = 200;
    private static final float DAMAGE_MULTIPLIER = 0.50F;
    private static final double MOVEMENT_SPEED_PENALTY = -0.50D;
    private static final double JUMP_DISTANCE_MULTIPLIER = 0.40D;
    private static final float MINING_SPEED_MULTIPLIER = 0.90F;
    private static final double ATTACK_SPEED_PENALTY = -0.15D;
    private static final int COMFORT_RECOVERY_DELAY_TICKS = 120;
    private static final int COMFORT_HEAL_INTERVAL_TICKS = 80;
    private static final float COMFORT_HEAL_AMOUNT = 0.5F;
    private static final float COMFORT_MIN_INTENSITY = 0.35F;
    private static final UUID MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("8eec8b5d-250c-4f52-b1c4-a0b4a7c4bf5d");
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("3d979a18-5177-4a40-bd41-cff16f35ba50");

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
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new HeroinEffectS2CPacket(true, TOTAL_DURATION), serverPlayer);
        }
    }

    public static void syncActiveEffect(ServerPlayer player) {
        if (player == null) {
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            clearHeroinEffect(player);
            return;
        }

        ModMessages.sendToPlayer(new HeroinEffectS2CPacket(true, remainingTicks), player);
    }

    public static void tickActiveEffect(Player player) {
        int remainingTicks = getRemainingDurationTicks(player);
        if (remainingTicks <= 0) {
            clearHeroinEffect(player);
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

        applyPenaltyModifiers(player);
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

        return computeEffectIntensity(TOTAL_DURATION, TOTAL_DURATION - remainingTicks);
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

    public static boolean isHeroinActive(Player player) {
        return player != null && player.level().isClientSide && ShaderUtils.areHeroinShadersActive();
    }

    public static boolean isHeroinActive(Player player, Level level) {
        return player != null && level != null && getRemainingDurationTicks(player) > 0;
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

    public static float getDamageMultiplier() {
        return DAMAGE_MULTIPLIER;
    }

    public static double getJumpDistanceMultiplier() {
        return JUMP_DISTANCE_MULTIPLIER;
    }

    public static double getGroupAggroRadius() {
        return GROUP_AGGRO_RADIUS;
    }

    public static int getGroupAggroDurationTicks() {
        return GROUP_AGGRO_DURATION_TICKS;
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

    private static void applyPenaltyModifiers(Player player) {
        ensureModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID,
                "heroin_sedation_movement", MOVEMENT_SPEED_PENALTY);
        ensureModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER_ID,
                "heroin_sedation_attack", ATTACK_SPEED_PENALTY);
    }

    private static void removePenaltyModifiers(Player player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID);
        removeModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER_ID);
    }

    private static void ensureModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) {
            return;
        }

        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null) {
            return;
        }

        attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
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
