package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.LSDEffectS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    private static final Set<UUID> PENDING_EFFECT_SYNC = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null || event.player.level().isClientSide) {
            return;
        }

        if (MorphineEffectsManager.hasActiveMorphineWindow(event.player)) {
            long startTime = MorphineEffectsManager.getMorphineStartTime(event.player);
            long currentTime = event.player.level().getGameTime();
            long elapsedTicks = currentTime - startTime;

            MorphineEffectsManager.handleMorphineEffectTicks(event.player, event.player.level(), elapsedTicks);
        }

        MorphineEffectsManager.tickUnstableHp(event.player, event.player.level());

        boolean shouldSyncEffects = event.player instanceof ServerPlayer serverPlayer
                && PENDING_EFFECT_SYNC.remove(serverPlayer.getUUID());
        if (shouldSyncEffects && event.player instanceof ServerPlayer serverPlayer) {
            MorphineEffectsManager.syncActiveEffect(serverPlayer);
            HeroinEffectsManager.syncActiveEffect(serverPlayer);
            LSDEffectsManager.syncActiveEffect(serverPlayer);
            FentanylEffectsManager.syncActiveEffect(serverPlayer);
        } else {
            HeroinEffectsManager.tickActiveEffect(event.player);
            LSDEffectsManager.tickActiveEffect(event.player, event.player.level());
            FentanylEffectsManager.tickActiveEffect(event.player, event.player.level());
        }

        MorphineEffectsManager.updateOngoingGameplayEffects(event.player, event.player.level());
        HeroinEffectsManager.updateOngoingGameplayEffects(event.player, event.player.level());
        FentanylEffectsManager.updateOngoingGameplayEffects(event.player, event.player.level());

        if (!HeroinEffectsManager.isHeroinActive(event.player, event.player.level())) {
            HeroinEffectsManager.clearHeroinEffect(event.player);
        }

        if (!LSDEffectsManager.isLsdActive(event.player, event.player.level())) {
            LSDEffectsManager.clearLsdEffect(event.player);
        }

        if (FentanylEffectsManager.hasFentanylOverdoseState(event.player)
                && !FentanylEffectsManager.isFentanylOverdoseActive(event.player, event.player.level())) {
            FentanylEffectsManager.clearFentanylOverdose(event.player);
        }

        if (event.player.isDeadOrDying()) {
            if (MorphineEffectsManager.hasMorphineState(event.player)) {
                MorphineEffectsManager.cleanupMorphineEffects(event.player, event.player.level());
            }
            if (HeroinEffectsManager.isHeroinActive(event.player, event.player.level())) {
                HeroinEffectsManager.cleanupHeroinEffect(event.player, event.player.level());
            }
            if (FentanylEffectsManager.isFentanylOverdoseActive(event.player, event.player.level())) {
                FentanylEffectsManager.clearFentanylOverdose(event.player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PENDING_EFFECT_SYNC.add(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PENDING_EFFECT_SYNC.remove(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        MorphineEffectsManager.cleanupMorphineEffects(event.getOriginal(), event.getOriginal().level());
        MorphineEffectsManager.cleanupMorphineEffects(event.getEntity(), event.getEntity().level());
        HeroinEffectsManager.clearHeroinEffect(event.getOriginal());
        HeroinEffectsManager.clearHeroinEffect(event.getEntity());
        LSDEffectsManager.clearLsdEffect(event.getOriginal());
        LSDEffectsManager.clearLsdEffect(event.getEntity());
        FentanylEffectsManager.clearFentanylOverdose(event.getOriginal());
        FentanylEffectsManager.clearFentanylOverdose(event.getEntity());
        PENDING_EFFECT_SYNC.remove(event.getOriginal().getUUID());
        PENDING_EFFECT_SYNC.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MorphineEffectsManager.cleanupMorphineEffects(serverPlayer, serverPlayer.level());
            HeroinEffectsManager.clearHeroinEffect(serverPlayer);
            LSDEffectsManager.clearLsdEffect(serverPlayer);
            FentanylEffectsManager.clearFentanylOverdose(serverPlayer);
            PENDING_EFFECT_SYNC.remove(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (MorphineEffectsManager.hasMorphineState(serverPlayer)) {
                MorphineEffectsManager.cleanupMorphineEffects(serverPlayer, serverPlayer.level());
            }
            HeroinEffectsManager.cleanupHeroinEffect(serverPlayer, serverPlayer.level());
            LSDEffectsManager.clearLsdEffect(serverPlayer);
            FentanylEffectsManager.clearFentanylOverdose(serverPlayer);
            PENDING_EFFECT_SYNC.remove(serverPlayer.getUUID());
            ModMessages.sendToPlayer(new LSDEffectS2CPacket(0, 0, 0.0F), serverPlayer);
        }
    }
}
