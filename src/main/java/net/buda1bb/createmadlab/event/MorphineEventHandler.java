package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MorphineEventHandler {

    // MORPHINE DAMAGE HANDLER - Server side
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (MorphineEffectsManager.hasDamageProtection(player)) {
                if (MorphineEffectsManager.handleDamage(player, event.getAmount())) {
                    if (player.getHealth() <= 1.0f) {
                        event.setCanceled(true);
                    } else {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    // MORPHINE TICK HANDLER - Server side
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            if (MorphineEffectsManager.isMorphineActive(event.player)) {
                long startTime = MorphineEffectsManager.getMorphineStartTime(event.player);
                long currentTime = event.player.level().getGameTime();
                long elapsedTicks = currentTime - startTime;

                MorphineEffectsManager.handleMorphineEffectTicks(event.player, event.player.level(), elapsedTicks);
            }

            //Clean up morphine effects if player is dead but still has data
            if (event.player.isDeadOrDying() && MorphineEffectsManager.isMorphineActive(event.player)) {
                MorphineEffectsManager.cleanupMorphineEffects(event.player, event.player.level());
            }
        }
    }
}