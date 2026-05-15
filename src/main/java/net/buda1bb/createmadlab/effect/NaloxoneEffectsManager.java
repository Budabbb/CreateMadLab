package net.buda1bb.createmadlab.effect;

import net.buda1bb.createmadlab.drug.DrugClass;
import net.buda1bb.createmadlab.drug.DrugStateManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class NaloxoneEffectsManager {
    private NaloxoneEffectsManager() {
    }

    public static void applyNaloxone(Player player, Level level) {
        if (player == null || level == null) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        float opioidWithdrawalLoad = DrugStateManager.getOpioidWithdrawalLoad(player);
        MorphineEffectsManager.counterWithNaloxone(player, level);
        HeroinEffectsManager.counterWithNaloxone(player, level);
        FentanylEffectsManager.counterWithNaloxone(player, level);
        UniversalOverdoseHandler.counterWithNaloxone(player, level);
        DrugStateManager.clearDrugClass(player, DrugClass.OPIOID);
        OpiateWithdrawalEffectsManager.startWithdrawalForOpioidDangerLoad(player, level, opioidWithdrawalLoad);
        DrugStateManager.syncIndividualEffectsWithActiveDrugs(player, level);
    }
}
