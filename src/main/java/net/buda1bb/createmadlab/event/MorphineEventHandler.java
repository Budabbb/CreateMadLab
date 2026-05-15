package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID)
public final class MorphineEventHandler {
    private MorphineEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int immediateDamageHp = MorphineEffectsManager.convertIncomingDamage(serverPlayer, event.getSource(), event.getAmount());
        if (immediateDamageHp >= 0) {
            event.setAmount(immediateDamageHp);
        }
    }
}
