package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.LSDEffectS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID)
public class ServerEventHandler {
    private static final Set<UUID> PENDING_EFFECT_SYNC = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) {
            return;
        }

        if (MorphineEffectsManager.hasActiveMorphineWindow(player)) {
            long startTime = MorphineEffectsManager.getMorphineStartTime(player);
            long currentTime = player.level().getGameTime();
            long elapsedTicks = currentTime - startTime;

            MorphineEffectsManager.handleMorphineEffectTicks(player, player.level(), elapsedTicks);
        }

        MorphineEffectsManager.tickUnstableHp(player, player.level());

        boolean shouldSyncEffects = player instanceof ServerPlayer serverPlayer
                && PENDING_EFFECT_SYNC.remove(serverPlayer.getUUID());
        if (shouldSyncEffects && player instanceof ServerPlayer serverPlayer) {
            MorphineEffectsManager.syncActiveEffect(serverPlayer);
            HeroinEffectsManager.syncActiveEffect(serverPlayer);
            LSDEffectsManager.syncActiveEffect(serverPlayer);
            FentanylEffectsManager.syncActiveEffect(serverPlayer);
        } else {
            HeroinEffectsManager.tickActiveEffect(player);
            LSDEffectsManager.tickActiveEffect(player, player.level());
            FentanylEffectsManager.tickActiveEffect(player, player.level());
        }

        MorphineEffectsManager.updateOngoingGameplayEffects(player, player.level());
        HeroinEffectsManager.updateOngoingGameplayEffects(player, player.level());
        FentanylEffectsManager.updateOngoingGameplayEffects(player, player.level());

        if (!HeroinEffectsManager.isHeroinActive(player, player.level())) {
            HeroinEffectsManager.clearHeroinEffect(player);
        }

        if (!LSDEffectsManager.isLsdActive(player, player.level())) {
            LSDEffectsManager.clearLsdEffect(player);
        }

        if (FentanylEffectsManager.hasFentanylOverdoseState(player)
                && !FentanylEffectsManager.isFentanylOverdoseActive(player, player.level())) {
            FentanylEffectsManager.clearFentanylOverdose(player);
        }

        if (player.isDeadOrDying()) {
            if (MorphineEffectsManager.hasMorphineState(player)) {
                MorphineEffectsManager.cleanupMorphineEffects(player, player.level());
            }
            if (HeroinEffectsManager.isHeroinActive(player, player.level())) {
                HeroinEffectsManager.cleanupHeroinEffect(player, player.level());
            }
            if (FentanylEffectsManager.isFentanylOverdoseActive(player, player.level())) {
                FentanylEffectsManager.clearFentanylOverdose(player);
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
