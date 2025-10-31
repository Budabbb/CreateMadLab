package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.BlissEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            // Handle all drug effects on server side
            LSDPaperItem.handleLSDEffects(event.player, event.player.level());
            BlissEffectsManager.handleBlissEffects(event.player, event.player.level());

            // Handle morphine effects
            if (MorphineEffectsManager.isMorphineActive(event.player)) {
                long startTime = MorphineEffectsManager.getMorphineStartTime(event.player);
                long currentTime = event.player.level().getGameTime();
                long elapsedTicks = currentTime - startTime;

                MorphineEffectsManager.handleMorphineEffectTicks(event.player, event.player.level(), elapsedTicks);
            }

            // Clean up effects if player is dead but still has data
            if (event.player.isDeadOrDying()) {
                if (BlissEffectsManager.isBlissActive(event.player)) {
                    BlissEffectsManager.cleanupBlissEffect(event.player, event.player.level());
                }
                if (MorphineEffectsManager.isMorphineActive(event.player)) {
                    MorphineEffectsManager.cleanupMorphineEffects(event.player, event.player.level());
                }
            }
        }
    }
}