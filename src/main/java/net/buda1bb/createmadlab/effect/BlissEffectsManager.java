package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.client.ShaderFileSwapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class BlissEffectsManager {
    private static final String BLISS_START_TIME_TAG = "BlissStartTime";
    private static final String BLISS_ACTIVE_TAG = "BlissActive";
    private static final int TOTAL_DURATION = 185 * 20;
    private static final int PHASE1_DURATION = 5 * 20;
    private static final int PHASE2_DURATION = 55 * 20;
    private static final int SLOWNESS_DURATION = 4 * 60 * 20;
    private static final int RESISTANCE_DURATION = 60 * 20;
    private static final int ABSORPTION_DURATION = 2 * 60 * 20;
    private static final int WITHDRAWAL_START = 3 * 60 * 20;
    private static final int HUNGER_DURATION = 20 * 20;
    private static final int WEAKNESS_DURATION = 60 * 20;

    public static void startBlissEffect(Player player, Level level) {
        if (!CreateMadLab.isShaderpackEnabled()) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        compoundtag.putLong(BLISS_START_TIME_TAG, level.getGameTime());
        compoundtag.putBoolean(BLISS_ACTIVE_TAG, true);
        persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);

        if (level.isClientSide) {
            ShaderFileSwapper.activateHeroinShaders();
        }

        if (!level.isClientSide) {
            applyBlissEffects(player, level);
        }
    }

    public static void handleBlissEffects(Player player, Level level) {
        if (!isBlissActive(player) || player.isDeadOrDying()) {
            return;
        }
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        long startTime = compoundtag.getLong(BLISS_START_TIME_TAG);
        long currentTime = level.getGameTime();
        long elapsedTicks = currentTime - startTime;

        if (!level.isClientSide) {
            handleBlissEffectTicks(player, level, elapsedTicks);
        }

        if (elapsedTicks >= TOTAL_DURATION) {
            endBlissEffect(player, level);
        }
    }

    public static void cleanupBlissEffect(Player player, Level level) {
        cleanupBlissNBTData(player, level);
        if (player.isDeadOrDying() || !player.isAlive()) {
            cleanupBlissEffects(player);
        }
    }

    public static void handlePlayerLogin(Player player, Level level) {
        if (hasBlissData(player)) {
            cleanupBlissEffect(player, level);
        }
    }

    public static void endBlissEffect(Player player, Level level) {
        if (level.isClientSide && ShaderFileSwapper.areHeroinShadersActive()) {
            ShaderFileSwapper.deactivateShaders();
        }
        cleanupBlissNBTData(player, level);
    }

    private static void cleanupBlissNBTData(Player player, Level level) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        compoundtag.remove(BLISS_START_TIME_TAG);
        compoundtag.remove(BLISS_ACTIVE_TAG);
        persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);

        if (level.isClientSide && ShaderFileSwapper.areHeroinShadersActive()) {
            ShaderFileSwapper.deactivateShaders();
        }
    }

    public static boolean isBlissActive(Player player) {
        if (player == null || player.isDeadOrDying()) return false;
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            return false;
        }
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        if (!compoundtag.contains(BLISS_START_TIME_TAG) || !compoundtag.contains(BLISS_ACTIVE_TAG)) {
            return false;
        }
        return compoundtag.getBoolean(BLISS_ACTIVE_TAG);
    }

    private static boolean hasBlissData(Player player) {
        if (player == null) return false;
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            return false;
        }
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        return compoundtag.contains(BLISS_START_TIME_TAG) || compoundtag.contains(BLISS_ACTIVE_TAG);
    }

    public static void applyBlissEffects(Player player, Level level) {
        if (player == null || level.isClientSide()) return;

        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.MOVEMENT_SLOWDOWN,
                SLOWNESS_DURATION,
                2
        ));
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.BLINDNESS,
                PHASE1_DURATION,
                0
        ));
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.REGENERATION,
                PHASE1_DURATION,
                2
        ));
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.REGENERATION,
                PHASE2_DURATION,
                0
        ));
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.DAMAGE_RESISTANCE,
                RESISTANCE_DURATION,
                1
        ));
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.ABSORPTION,
                ABSORPTION_DURATION,
                1
        ));
    }

    public static void handleBlissEffectTicks(Player player, Level level, long elapsedTicks) {
        if (player == null || level.isClientSide()) return;

        if (elapsedTicks == WITHDRAWAL_START) {
            applyWithdrawalEffects(player);
        }
        handlePhase2Regeneration(player, elapsedTicks);
    }

    private static void handlePhase2Regeneration(Player player, long elapsedTicks) {
        if (elapsedTicks >= PHASE1_DURATION && elapsedTicks < (PHASE1_DURATION + PHASE2_DURATION)) {
            boolean hasRegen = player.hasEffect(MobEffects.REGENERATION) &&
                    player.getEffect(MobEffects.REGENERATION).getAmplifier() == 0;
            if (!hasRegen) {
                player.addEffect(createNonRemovableHiddenEffect(
                        MobEffects.REGENERATION,
                        100,
                        0
                ));
            }
        }
    }

    private static void applyWithdrawalEffects(Player player) {
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.HUNGER,
                HUNGER_DURATION,
                0
        ));
        player.addEffect(createNonRemovableHiddenEffect(
                MobEffects.WEAKNESS,
                WEAKNESS_DURATION,
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

    public static void cleanupBlissEffects(Player player) {
        if (player == null) return;
        player.getActiveEffects().removeIf(effect ->
                (effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN && effect.getAmplifier() == 2) ||
                        (effect.getEffect() == MobEffects.BLINDNESS) ||
                        (effect.getEffect() == MobEffects.REGENERATION) ||
                        (effect.getEffect() == MobEffects.DAMAGE_RESISTANCE && effect.getAmplifier() == 1) ||
                        (effect.getEffect() == MobEffects.HUNGER) ||
                        (effect.getEffect() == MobEffects.WEAKNESS && effect.getAmplifier() == 1) ||
                        (effect.getEffect() == MobEffects.ABSORPTION && effect.getAmplifier() == 1)
        );
    }

    public static int getTotalDuration() {
        return TOTAL_DURATION;
    }
    public static int getCooldownDuration() {
        return TOTAL_DURATION;
    }
}