package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.buda1bb.createmadlab.effect.UniversalOverdoseHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OpioidOverdoseEventHandler {
    private OpioidOverdoseEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        double horizontalMultiplier = 1.0D;
        double jumpMultiplier = 1.0D;

        if (FentanylEffectsManager.isFentanylActive(player, player.level())) {
            horizontalMultiplier *= FentanylEffectsManager.getHorizontalJumpDistanceMultiplier(player);
            jumpMultiplier *= FentanylEffectsManager.getJumpVelocityMultiplier(player);
        }

        if (UniversalOverdoseHandler.isOpioidOverdoseActive(player, player.level())) {
            horizontalMultiplier *= UniversalOverdoseHandler.getHorizontalJumpDistanceMultiplier(player);
            jumpMultiplier *= UniversalOverdoseHandler.getJumpVelocityMultiplier(player);
        }

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
