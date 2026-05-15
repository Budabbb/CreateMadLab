package net.buda1bb.createmadlab.drug;

import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.effect.OpiateWithdrawalEffectsManager;
import net.buda1bb.createmadlab.effect.UniversalOverdoseHandler;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.DrugVisualStateS2CPacket;
import net.buda1bb.createmadlab.network.packet.LSDEffectS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DrugStateManager {
    private static final String ACTIVE_DRUGS_TAG = "CreateMadLabActiveDrugs";
    private static final int FULL_VISUAL_SYNC_INTERVAL_TICKS = 20;
    private static final Map<UUID, VisualState> LAST_SYNCED_VISUAL_STATES = new HashMap<>();

    private DrugStateManager() {
    }

    public static void addDrug(Player player, DrugType type, float dose) {
        if (player == null || type == null || dose <= 0.0F) {
            return;
        }

        Level level = player.level();
        if (level == null || level.isClientSide) {
            return;
        }

        List<DrugInstance> activeDrugs = loadActiveDrugs(player);
        boolean alreadyActiveType = hasActiveDrug(activeDrugs, type);
        activeDrugs.add(new DrugInstance(type, dose, 0, type.getDurationTicks()));
        saveActiveDrugs(player, activeDrugs);

        clearInactiveIndividualVisuals(player, level, activeDrugs, true);
        startOrExtendIndividualEffect(player, level, activeDrugs, type, dose, alreadyActiveType);
        UniversalOverdoseHandler.evaluateAndSync(player, level);
        syncIndividualEffectsWithActiveDrugs(player, level);
    }

    public static PlayerDrugState getDrugState(Player player) {
        return calculateMixedDrugState(player);
    }

    public static PlayerDrugState calculateMixedDrugState(Player player) {
        PlayerDrugState state = new PlayerDrugState();
        if (player == null) {
            return state;
        }

        for (DrugInstance instance : loadActiveDrugs(player)) {
            if (!instance.isExpired()) {
                state.activeDrugs.add(instance.copy());
            }
        }

        for (DrugType type : DrugType.values()) {
            addExactDrugTypeContributions(state, type);
        }

        state.opioidDangerLoad = calculateRawOpioidDangerLoad(state.activeDrugs);
        state.overdoseProgress = UniversalOverdoseHandler.getOverdoseProgress(player);
        state.isOverdosing = UniversalOverdoseHandler.isOverdosing(player)
                || state.opioidDangerLoad >= UniversalOverdoseHandler.ACTIVE_THRESHOLD;
        return state;
    }

    public static void tickActiveDrugs(Player player, Level level) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }

        List<DrugInstance> activeDrugs = loadActiveDrugs(player);
        float expiringOpioidWithdrawalLoad = calculateRawOpioidDangerLoad(activeDrugs);
        boolean hadActiveOpioids = hasActiveDrugClass(activeDrugs, DrugClass.OPIOID);
        boolean removedOpioid = false;
        boolean changed = false;
        Iterator<DrugInstance> iterator = activeDrugs.iterator();
        while (iterator.hasNext()) {
            DrugInstance instance = iterator.next();
            instance.tick();
            changed = true;
            if (instance.isExpired()) {
                if (instance.drugClass == DrugClass.OPIOID) {
                    removedOpioid = true;
                }
                iterator.remove();
            }
        }

        if (changed) {
            saveActiveDrugs(player, activeDrugs);
        }

        if (hadActiveOpioids
                && removedOpioid
                && !hasActiveDrugClass(activeDrugs, DrugClass.OPIOID)
                && !player.isDeadOrDying()
                && !OpiateWithdrawalEffectsManager.isWithdrawalActive(player, level)) {
            OpiateWithdrawalEffectsManager.startWithdrawalForOpioidDangerLoad(player, level, expiringOpioidWithdrawalLoad);
        }
    }

    public static void syncIndividualEffectsWithActiveDrugs(Player player, Level level) {
        if (player == null || level == null || level.isClientSide) {
            return;
        }

        List<DrugInstance> activeDrugs = loadActiveDrugs(player);
        clearInactiveIndividualVisuals(player, level, activeDrugs, player.tickCount % 20 == 0);
        if (player instanceof ServerPlayer serverPlayer && player.tickCount % 20 == 0) {
            syncActiveIndividualEffectStrengths(serverPlayer, level, activeDrugs);
        }
        syncAuthoritativeVisualState(player, level, activeDrugs);
    }

    public static boolean isOverdosing(Player player) {
        return calculateMixedDrugState(player).isOverdosing;
    }

    public static float getOpioidDangerLoad(Player player) {
        return calculateMixedDrugState(player).opioidDangerLoad;
    }

    public static float getOpioidWithdrawalLoad(Player player) {
        if (player == null) {
            return 0.0F;
        }
        return calculateRawOpioidDangerLoad(loadActiveDrugs(player));
    }

    public static boolean hasActiveDrug(Player player, DrugType type) {
        if (player == null || type == null) {
            return false;
        }
        return hasActiveDrug(loadActiveDrugs(player), type);
    }

    public static boolean hasActiveDrugClass(Player player, DrugClass drugClass) {
        if (player == null || drugClass == null) {
            return false;
        }

        for (DrugInstance instance : loadActiveDrugs(player)) {
            if (instance.drugClass == drugClass && !instance.isExpired()) {
                return true;
            }
        }
        return false;
    }

    public static int getMaxRemainingDurationTicks(Player player, DrugType type) {
        if (player == null || type == null) {
            return 0;
        }

        return getMaxRemainingDurationTicks(loadActiveDrugs(player), type);
    }

    public static float getStrongestDose(Player player, DrugType type) {
        if (player == null || type == null) {
            return 0.0F;
        }

        float strongestDose = 0.0F;
        for (DrugInstance instance : loadActiveDrugs(player)) {
            if (instance.type == type && !instance.isExpired()) {
                strongestDose = Math.max(strongestDose, instance.dose);
            }
        }
        return strongestDose;
    }

    public static float getDrugVisualStrength(Player player, DrugType type) {
        if (player == null || type == null) {
            return 0.0F;
        }

        return getDrugVisualStrength(loadActiveDrugs(player), type);
    }

    public static void clearDrugClass(Player player, DrugClass drugClass) {
        if (player == null || drugClass == null) {
            return;
        }

        List<DrugInstance> activeDrugs = loadActiveDrugs(player);
        activeDrugs.removeIf(instance -> instance.drugClass == drugClass);
        saveActiveDrugs(player, activeDrugs);
    }

    public static void clearAll(Player player) {
        if (player == null) {
            return;
        }

        CompoundTag persistedData = getPersistedData(player);
        persistedData.remove(ACTIVE_DRUGS_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
        forgetSyncedVisualState(player);
    }

    public static void forgetSyncedVisualState(Player player) {
        if (player != null) {
            LAST_SYNCED_VISUAL_STATES.remove(player.getUUID());
        }
    }

    private static void startOrExtendIndividualEffect(Player player, Level level, List<DrugInstance> activeDrugs,
                                                      DrugType type, float dose, boolean alreadyActiveType) {
        if (alreadyActiveType) {
            extendIndividualEffect(player, level, activeDrugs, type);
            return;
        }

        switch (type) {
            case MORPHINE -> MorphineEffectsManager.startMorphineEffect(player, level);
            case HEROIN -> HeroinEffectsManager.startHeroinEffect(player, level);
            case FENTANYL -> FentanylEffectsManager.startFentanylEffect(player, level);
            case LSD -> LSDEffectsManager.startLsdEffect(player, level, dose);
        }
    }

    private static void extendIndividualEffect(Player player, Level level, List<DrugInstance> activeDrugs, DrugType type) {
        int remainingTicks = getMaxRemainingDurationTicks(activeDrugs, type);
        switch (type) {
            case MORPHINE -> MorphineEffectsManager.extendMorphineEffect(player, level, remainingTicks);
            case HEROIN -> HeroinEffectsManager.extendHeroinEffect(player, remainingTicks);
            case FENTANYL -> FentanylEffectsManager.extendFentanylEffect(player, remainingTicks);
            case LSD -> LSDEffectsManager.extendLsdEffect(player, remainingTicks, getDrugVisualStrength(activeDrugs, DrugType.LSD));
        }
    }

    private static void clearInactiveIndividualVisuals(Player player, Level level, List<DrugInstance> activeDrugs, boolean forceClientClear) {
        if (!(player instanceof ServerPlayer serverPlayer) || level == null || level.isClientSide) {
            return;
        }

        if (!hasActiveDrug(activeDrugs, DrugType.HEROIN)
                && !HeroinEffectsManager.isCounteredByNaloxone(serverPlayer)
                && (forceClientClear || HeroinEffectsManager.getRemainingDurationTicks(serverPlayer) > 0)) {
            HeroinEffectsManager.cleanupHeroinEffect(serverPlayer, level);
        }

        if (!hasActiveDrug(activeDrugs, DrugType.MORPHINE)
                && !MorphineEffectsManager.hasMorphineState(serverPlayer)
                && MorphineEffectsManager.getUnstableHp(serverPlayer) <= 0
                && (forceClientClear || MorphineEffectsManager.isMorphineActive(serverPlayer, level))) {
            MorphineEffectsManager.cleanupMorphineEffects(serverPlayer, level);
        }

        if (!hasActiveDrug(activeDrugs, DrugType.LSD)
                && (forceClientClear || LSDEffectsManager.isLsdActive(serverPlayer, level))) {
            LSDEffectsManager.clearLsdEffect(serverPlayer);
            ModMessages.sendToPlayer(new LSDEffectS2CPacket(0, 0, 0.0F), serverPlayer);
        }

        if (!hasActiveDrug(activeDrugs, DrugType.FENTANYL)
                && !FentanylEffectsManager.isCounteredByNaloxone(serverPlayer)
                && (forceClientClear || FentanylEffectsManager.getRemainingDurationTicks(serverPlayer) > 0)) {
            FentanylEffectsManager.cleanupFentanylEffect(serverPlayer, level);
        }
    }

    private static void syncAuthoritativeVisualState(Player player, Level level, List<DrugInstance> activeDrugs) {
        if (!(player instanceof ServerPlayer serverPlayer) || level == null || level.isClientSide) {
            return;
        }

        float opioidDangerLoad = calculateRawOpioidDangerLoad(activeDrugs);
        boolean morphineActive = hasActiveDrug(activeDrugs, DrugType.MORPHINE)
                || MorphineEffectsManager.hasMorphineState(player);
        boolean heroinActive = hasActiveDrug(activeDrugs, DrugType.HEROIN)
                || HeroinEffectsManager.isCounteredByNaloxone(player);
        boolean fentanylActive = (hasActiveDrug(activeDrugs, DrugType.FENTANYL)
                || FentanylEffectsManager.isCounteredByNaloxone(player))
                && FentanylEffectsManager.getRemainingDurationTicks(player) > 0;
        boolean lsdActive = hasActiveDrug(activeDrugs, DrugType.LSD);
        boolean opioidOverdoseActive = UniversalOverdoseHandler.isOpioidOverdoseActive(player, level)
                && (opioidDangerLoad >= UniversalOverdoseHandler.WARNING_THRESHOLD
                || UniversalOverdoseHandler.getElapsedTicks(player) > 0
                || UniversalOverdoseHandler.isCounteredByNaloxone(player));
        boolean withdrawalActive = OpiateWithdrawalEffectsManager.isWithdrawalActive(player, level);

        VisualState visualState = new VisualState(
                morphineActive, heroinActive, fentanylActive, lsdActive, opioidOverdoseActive, withdrawalActive
        );
        UUID playerId = serverPlayer.getUUID();
        boolean forceSync = player.tickCount % FULL_VISUAL_SYNC_INTERVAL_TICKS == 0;
        if (!forceSync && visualState.equals(LAST_SYNCED_VISUAL_STATES.get(playerId))) {
            return;
        }

        LAST_SYNCED_VISUAL_STATES.put(playerId, visualState);
        ModMessages.sendToPlayer(visualState.toPacket(), serverPlayer);
    }

    private static void syncActiveIndividualEffectStrengths(ServerPlayer player, Level level, List<DrugInstance> activeDrugs) {
        if (hasActiveDrug(activeDrugs, DrugType.MORPHINE) || MorphineEffectsManager.hasMorphineState(player)) {
            MorphineEffectsManager.syncActiveEffect(player);
        }
        if (hasActiveDrug(activeDrugs, DrugType.HEROIN) || HeroinEffectsManager.isCounteredByNaloxone(player)) {
            HeroinEffectsManager.syncActiveEffect(player);
        }
        if (hasActiveDrug(activeDrugs, DrugType.FENTANYL) || FentanylEffectsManager.isCounteredByNaloxone(player)) {
            FentanylEffectsManager.syncActiveEffect(player);
        }
        if (hasActiveDrug(activeDrugs, DrugType.LSD)) {
            LSDEffectsManager.syncActiveEffect(player);
        }
        if (UniversalOverdoseHandler.isOpioidOverdoseActive(player, level)) {
            UniversalOverdoseHandler.syncActiveEffect(player);
        }
        if (OpiateWithdrawalEffectsManager.isWithdrawalActive(player, level)) {
            OpiateWithdrawalEffectsManager.syncActiveEffect(player);
        }
    }

    private static void addExactDrugTypeContributions(PlayerDrugState state, DrugType type) {
        List<DrugInstance> instances = getActiveInstancesOfType(state.activeDrugs, type);
        for (int index = 0; index < instances.size(); index++) {
            DrugInstance instance = instances.get(index);
            float stackMultiplier = getSameDrugEffectMultiplier(index);
            if (stackMultiplier <= 0.0F) {
                continue;
            }

            float scaledDose = instance.dose * instance.intensity * stackMultiplier;
            state.sedation += type.getSedation() * scaledDose;
            state.hallucination += type.getHallucination() * scaledDose;
            state.movementImpairment += type.getMovementImpairment() * scaledDose;
            state.respiratorySuppression += type.getRespiratorySuppression() * scaledDose;
            if (type.getDrugClass() == DrugClass.OPIOID) {
                state.opioidVisualIntensity += type.getOpioidLoad() * scaledDose;
            }
        }
    }

    private static float getSameDrugEffectMultiplier(int index) {
        if (index == 0) {
            return 1.0F;
        }
        if (index == 1) {
            return 0.75F;
        }
        if (index == 2) {
            return 0.50F;
        }
        if (index == 3) {
            return 0.25F;
        }
        return 0.0F;
    }

    private static float getOpioidDangerMultiplier(int index) {
        if (index == 0) {
            return 1.0F;
        }
        if (index == 1) {
            return 0.75F;
        }
        if (index == 2) {
            return 0.50F;
        }
        return 0.25F;
    }

    private static float calculateRawOpioidDangerLoad(List<DrugInstance> activeDrugs) {
        List<DrugInstance> opioids = new ArrayList<>();
        for (DrugInstance instance : activeDrugs) {
            if (instance.drugClass == DrugClass.OPIOID && !instance.isExpired()) {
                opioids.add(instance);
            }
        }

        opioids.sort(Comparator.<DrugInstance>comparingDouble(instance ->
                instance.getScaledBase(instance.type.getOpioidLoad())
        ).reversed());
        float opioidDangerLoad = 0.0F;
        for (int index = 0; index < opioids.size(); index++) {
            DrugInstance instance = opioids.get(index);
            opioidDangerLoad += instance.getScaledBase(instance.type.getOpioidLoad()) * getOpioidDangerMultiplier(index);
        }
        return opioidDangerLoad;
    }

    private static int getMaxRemainingDurationTicks(List<DrugInstance> activeDrugs, DrugType type) {
        int maxRemainingTicks = 0;
        for (DrugInstance instance : activeDrugs) {
            if (instance.type == type && !instance.isExpired()) {
                maxRemainingTicks = Math.max(maxRemainingTicks, instance.durationTicks - instance.ageTicks);
            }
        }
        return maxRemainingTicks;
    }

    private static float getDrugVisualStrength(List<DrugInstance> activeDrugs, DrugType type) {
        float visualStrength = 0.0F;
        List<DrugInstance> instances = getActiveInstancesOfType(activeDrugs, type);
        for (int index = 0; index < instances.size(); index++) {
            DrugInstance instance = instances.get(index);
            visualStrength += instance.dose * instance.intensity * getSameDrugEffectMultiplier(index);
        }
        return visualStrength;
    }

    private static List<DrugInstance> getActiveInstancesOfType(List<DrugInstance> activeDrugs, DrugType type) {
        List<DrugInstance> instances = new ArrayList<>();
        for (DrugInstance instance : activeDrugs) {
            if (instance.type == type && !instance.isExpired()) {
                instances.add(instance);
            }
        }
        instances.sort(Comparator.comparingDouble(DrugStateManager::getExactDrugSortValue).reversed());
        return instances;
    }

    private static float getExactDrugSortValue(DrugInstance instance) {
        return instance.dose * instance.intensity;
    }

    private static List<DrugInstance> loadActiveDrugs(Player player) {
        List<DrugInstance> activeDrugs = new ArrayList<>();
        CompoundTag persistedData = getPersistedData(player);
        if (!persistedData.contains(ACTIVE_DRUGS_TAG, Tag.TAG_LIST)) {
            return activeDrugs;
        }

        ListTag list = persistedData.getList(ACTIVE_DRUGS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            DrugInstance instance = DrugInstance.load(list.getCompound(index));
            if (instance != null && !instance.isExpired()) {
                activeDrugs.add(instance);
            }
        }
        return activeDrugs;
    }

    private static boolean hasActiveDrug(List<DrugInstance> activeDrugs, DrugType type) {
        for (DrugInstance instance : activeDrugs) {
            if (instance.type == type && !instance.isExpired()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasActiveDrugClass(List<DrugInstance> activeDrugs, DrugClass drugClass) {
        for (DrugInstance instance : activeDrugs) {
            if (instance.drugClass == drugClass && !instance.isExpired()) {
                return true;
            }
        }
        return false;
    }

    private static void saveActiveDrugs(Player player, List<DrugInstance> activeDrugs) {
        CompoundTag persistedData = getPersistedData(player);
        if (activeDrugs.isEmpty()) {
            persistedData.remove(ACTIVE_DRUGS_TAG);
        } else {
            ListTag list = new ListTag();
            for (DrugInstance instance : activeDrugs) {
                if (!instance.isExpired()) {
                    list.add(instance.save());
                }
            }
            persistedData.put(ACTIVE_DRUGS_TAG, list);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
    }

    private static CompoundTag getPersistedData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            persistentData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistentData.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private record VisualState(boolean morphineActive, boolean heroinActive, boolean fentanylActive, boolean lsdActive,
                               boolean opioidOverdoseActive, boolean withdrawalActive) {
        private DrugVisualStateS2CPacket toPacket() {
            return new DrugVisualStateS2CPacket(
                    morphineActive,
                    heroinActive,
                    fentanylActive,
                    lsdActive,
                    opioidOverdoseActive,
                    withdrawalActive
            );
        }
    }
}
