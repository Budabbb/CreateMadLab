package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.client.ShaderFileSwapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class MorphineEffectsManager {
    private static final String MORPHINE_START_TIME_TAG = "MorphineStartTime";
    private static final String MORPHINE_ACTIVE_TAG = "MorphineActive";
    private static final String MORPHINE_PROTECTION_TAG = "MorphineProtectionActive";
    private static final String MORPHINE_DAMAGE_BLOCKED_TAG = "MorphineDamageBlocked";
    private static final int SPEED_DURATION = 30 * 20;
    private static final int DAMAGE_PROTECTION_DURATION = 30 * 20;
    private static final int SHADER_DURATION = 30 * 20;
    private static final int WITHDRAWAL_START = 30 * 20;
    private static final int WITHDRAWAL_DURATION = 60 * 20;
    private static final int COOLDOWN_DURATION = 35 * 20;
    private static final float MAX_DAMAGE_BLOCKED = 40.0f;

    public static void startMorphineEffect(Player player, Level level) {
        if (!CreateMadLab.isShaderpackEnabled()) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        compoundtag.putLong(MORPHINE_START_TIME_TAG, level.getGameTime());
        compoundtag.putBoolean(MORPHINE_ACTIVE_TAG, true);
        compoundtag.putBoolean(MORPHINE_PROTECTION_TAG, true);
        compoundtag.putFloat(MORPHINE_DAMAGE_BLOCKED_TAG, 0.0f);
        persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);

        if (level.isClientSide) {
            activateMorphineShaders();
        }

        if (!level.isClientSide) {
            applyMorphineEffects(player);
        }
    }

    public static void activateMorphineShaders() {
        ShaderFileSwapper.activateMorphineShaders();
    }

    private static void applyMorphineEffects(Player player) {
        if (player == null) return;

        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.MOVEMENT_SPEED,
                SPEED_DURATION,
                0
        ));
    }

    public static void handleMorphineEffectTicks(Player player, Level level, long elapsedTicks) {
        if (player == null) return;

        if (elapsedTicks >= SHADER_DURATION) {
            endMorphineShaders(level);
        }
        if (elapsedTicks >= DAMAGE_PROTECTION_DURATION) {
            endDamageProtection(player);
        }
        if (elapsedTicks == WITHDRAWAL_START) {
            applyWithdrawalEffects(player);
        }
    }

    private static void endMorphineShaders(Level level) {
        if (level.isClientSide && ShaderFileSwapper.areMorphineShadersActive()) {
            ShaderFileSwapper.deactivateShaders();
        }
    }

    public static boolean hasDamageProtection(Player player) {
        if (player == null) return false;
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            return false;
        }
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        return compoundtag.getBoolean(MORPHINE_PROTECTION_TAG);
    }

    public static boolean handleDamage(Player player, float damageAmount) {
        if (!hasDamageProtection(player)) return false;
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        float damageBlocked = compoundtag.getFloat(MORPHINE_DAMAGE_BLOCKED_TAG);
        if (damageBlocked >= MAX_DAMAGE_BLOCKED) {
            return false;
        }

        float currentHealth = player.getHealth();
        float newHealth = currentHealth - damageAmount;
        if (newHealth < 1.0f) {
            float allowedDamage = currentHealth - 1.0f;
            float actualDamage = Math.min(damageAmount, allowedDamage);
            float damageToBlock = damageAmount - actualDamage;
            if (damageBlocked + damageToBlock > MAX_DAMAGE_BLOCKED) {
                damageToBlock = MAX_DAMAGE_BLOCKED - damageBlocked;
                actualDamage = damageAmount - damageToBlock;
            }
            if (actualDamage > 0) {
                player.setHealth(currentHealth - actualDamage);
            } else {
                player.setHealth(1.0f);
            }
            compoundtag.putFloat(MORPHINE_DAMAGE_BLOCKED_TAG, damageBlocked + damageToBlock);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
            return true;
        }
        return false;
    }

    private static void endDamageProtection(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        compoundtag.putBoolean(MORPHINE_PROTECTION_TAG, false);
        persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
    }

    private static void applyWithdrawalEffects(Player player) {
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.MOVEMENT_SLOWDOWN,
                WITHDRAWAL_DURATION,
                2
        ));
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.WEAKNESS,
                WITHDRAWAL_DURATION,
                1
        ));
    }

    private static MobEffectInstance createNonRemovableHiddenEffect(net.minecraft.world.effect.MobEffect effect, int duration, int amplifier) {
        return new MobEffectInstance(
                effect, duration, amplifier, false, false, false
        ) {
            @Override
            public boolean isCurativeItem(net.minecraft.world.item.ItemStack curativeItem) {
                return false;
            }
        };
    }

    public static void cleanupMorphineEffects(Player player, Level level) {
        if (player == null) return;
        player.getActiveEffects().removeIf(effect ->
                (effect.getEffect() == MobEffects.MOVEMENT_SPEED && effect.getAmplifier() == 0) ||
                        (effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN && effect.getAmplifier() == 2) ||
                        (effect.getEffect() == MobEffects.WEAKNESS && effect.getAmplifier() == 1)
        );
        if (level.isClientSide && ShaderFileSwapper.areMorphineShadersActive()) {
            ShaderFileSwapper.deactivateShaders();
        }
        cleanupMorphineNBTData(player);
    }

    private static void cleanupMorphineNBTData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        compoundtag.remove(MORPHINE_START_TIME_TAG);
        compoundtag.remove(MORPHINE_ACTIVE_TAG);
        compoundtag.remove(MORPHINE_PROTECTION_TAG);
        compoundtag.remove(MORPHINE_DAMAGE_BLOCKED_TAG);
        persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
    }

    public static boolean isMorphineActive(Player player) {
        if (player == null || player.isDeadOrDying()) return false;
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            return false;
        }
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        return compoundtag.getBoolean(MORPHINE_ACTIVE_TAG);
    }

    public static long getMorphineStartTime(Player player) {
        if (player == null) return 0;
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            return 0;
        }
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        return compoundtag.getLong(MORPHINE_START_TIME_TAG);
    }

    public static float getDamageBlocked(Player player) {
        if (player == null) return 0;
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            return 0;
        }
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        return compoundtag.getFloat(MORPHINE_DAMAGE_BLOCKED_TAG);
    }

    public static int getTotalDuration() {
        return WITHDRAWAL_START + WITHDRAWAL_DURATION;
    }
    public static int getCooldownDuration() {
        return COOLDOWN_DURATION;
    }
}