package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.LSDEffectS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class LSDEffectsManager {
    private static final String LSD_END_TIME_TAG = "LsdEndTime";
    private static final String LSD_REMAINING_TICKS_TAG = "LsdRemainingTicks";
    private static final String LSD_TOTAL_TICKS_TAG = "LsdTotalTicks";
    private static final String LSD_STRENGTH_TAG = "LsdStrength";
    private static final int EFFECT_DURATION_TICKS = 20 * 60 * 5;

    private LSDEffectsManager() {
    }

    public static void startLsdEffect(Player player, Level level, double dose) {
        if (player == null || level == null) {
            return;
        }

        float strength = getStrengthForDose(dose);
        storeActiveTrip(player, strength, EFFECT_DURATION_TICKS, EFFECT_DURATION_TICKS);
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new LSDEffectS2CPacket(EFFECT_DURATION_TICKS, EFFECT_DURATION_TICKS, strength), serverPlayer);
        }
    }

    public static void syncActiveEffect(ServerPlayer player) {
        if (player == null) {
            return;
        }

        int remainingTicks = getRemainingDurationTicks(player, player.level());
        if (remainingTicks <= 0) {
            clearLsdEffect(player);
            return;
        }

        ModMessages.sendToPlayer(new LSDEffectS2CPacket(remainingTicks, getStoredTotalDuration(player), getStoredStrength(player)), player);
    }

    public static void tickActiveEffect(Player player, Level level) {
        int remainingTicks = getRemainingDurationTicks(player, level);
        if (remainingTicks <= 0) {
            clearLsdEffect(player);
            return;
        }

        setStoredRemainingDuration(player, remainingTicks - 1);
    }

    public static boolean isLsdActive(Player player, Level level) {
        return getRemainingDurationTicks(player, level) > 0;
    }

    public static int getRemainingDurationTicks(Player player, Level level) {
        if (player == null || level == null) {
            return 0;
        }

        CompoundTag persistedData = getPersistedData(player);
        if (persistedData.contains(LSD_REMAINING_TICKS_TAG)) {
            return Math.max(0, persistedData.getInt(LSD_REMAINING_TICKS_TAG));
        }

        if (!persistedData.contains(LSD_END_TIME_TAG)) {
            return 0;
        }

        long legacyRemainingTicks = persistedData.getLong(LSD_END_TIME_TAG) - level.getGameTime();
        int remainingTicks = legacyRemainingTicks > 0L ? (int) Math.min(Integer.MAX_VALUE, legacyRemainingTicks) : 0;
        if (remainingTicks > 0) {
            storeActiveTrip(player, getStoredStrength(player), getStoredTotalDuration(player), remainingTicks);
        } else {
            clearLsdEffect(player);
        }
        return remainingTicks;
    }

    public static void clearLsdEffect(Player player) {
        if (player == null) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedData.remove(LSD_END_TIME_TAG);
        persistedData.remove(LSD_REMAINING_TICKS_TAG);
        persistedData.remove(LSD_TOTAL_TICKS_TAG);
        persistedData.remove(LSD_STRENGTH_TAG);
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    public static int getCooldownDuration() {
        return EFFECT_DURATION_TICKS;
    }

    public static int getEffectDurationTicks() {
        return EFFECT_DURATION_TICKS;
    }

    public static float getStrengthForDose(double dose) {
        float normalizedDose = Math.max(0.0F, (float) dose);
        if (normalizedDose >= 4.0F) {
            return 3.45F;
        }
        if (normalizedDose >= 3.0F) {
            return 2.55F;
        }
        if (normalizedDose >= 2.0F) {
            return 1.75F;
        }
        return clamp(0.58F + normalizedDose * 0.22F, 0.58F, 1.0F);
    }

    private static void storeActiveTrip(Player player, float strength, int totalDurationTicks, int remainingDurationTicks) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedData.remove(LSD_END_TIME_TAG);
        persistedData.putInt(LSD_REMAINING_TICKS_TAG, clampInt(remainingDurationTicks, 0, Math.max(1, totalDurationTicks)));
        persistedData.putInt(LSD_TOTAL_TICKS_TAG, Math.max(1, totalDurationTicks));
        persistedData.putFloat(LSD_STRENGTH_TAG, strength);
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static float getStoredStrength(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(LSD_STRENGTH_TAG) ? persistedData.getFloat(LSD_STRENGTH_TAG) : getStrengthForDose(1.0D);
    }

    private static int getStoredTotalDuration(Player player) {
        CompoundTag persistedData = getPersistedData(player);
        return persistedData.contains(LSD_TOTAL_TICKS_TAG)
                ? Math.max(1, persistedData.getInt(LSD_TOTAL_TICKS_TAG))
                : EFFECT_DURATION_TICKS;
    }

    private static void setStoredRemainingDuration(Player player, int remainingTicks) {
        if (remainingTicks <= 0) {
            clearLsdEffect(player);
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedData.putInt(LSD_REMAINING_TICKS_TAG, remainingTicks);
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static CompoundTag getPersistedData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            persistentData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistentData.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
