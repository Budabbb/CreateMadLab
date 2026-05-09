package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID)
public final class FentanylEventHandler {
    private FentanylEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        if (!FentanylEffectsManager.isFentanylOverdoseActive(player, player.level())) {
            return;
        }

        int elapsedTicks = FentanylEffectsManager.getElapsedTicks(player);
        double horizontalMultiplier = FentanylEffectsManager.getHorizontalJumpDistanceMultiplier(elapsedTicks);
        double jumpMultiplier = FentanylEffectsManager.getJumpVelocityMultiplier(elapsedTicks);
        if (jumpMultiplier >= 1.0D && horizontalMultiplier >= 0.999D) {
            return;
        }

        Vec3 deltaMovement = player.getDeltaMovement();
        player.setDeltaMovement(
                deltaMovement.x * horizontalMultiplier,
                deltaMovement.y * jumpMultiplier,
                deltaMovement.z * horizontalMultiplier
        );
        player.hasImpulse = true;
    }
}
