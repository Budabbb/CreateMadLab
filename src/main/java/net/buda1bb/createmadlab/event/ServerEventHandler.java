package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.drug.DrugStateManager;
import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.effect.OpiateWithdrawalEffectsManager;
import net.buda1bb.createmadlab.effect.UniversalOverdoseHandler;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.network.packet.DrugVisualStateS2CPacket;
import net.buda1bb.createmadlab.network.packet.LSDEffectS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
        if (event.phase != TickEvent.Phase.END || event.player == null) {
            return;
        }

        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        if (MorphineEffectsManager.hasActiveMorphineWindow(player)) {
            long startTime = MorphineEffectsManager.getMorphineStartTime(player);
            long currentTime = level.getGameTime();
            long elapsedTicks = currentTime - startTime;

            MorphineEffectsManager.handleMorphineEffectTicks(player, level, elapsedTicks);
        }

        MorphineEffectsManager.tickNaloxoneFade(player, level);
        MorphineEffectsManager.tickUnstableHp(player, level);
        DrugStateManager.tickActiveDrugs(player, level);

        ServerPlayer serverPlayer = player instanceof ServerPlayer playerOnServer ? playerOnServer : null;
        boolean shouldSyncEffects = serverPlayer != null
                && PENDING_EFFECT_SYNC.remove(serverPlayer.getUUID());
        if (shouldSyncEffects) {
            MorphineEffectsManager.syncActiveEffect(serverPlayer);
            HeroinEffectsManager.syncActiveEffect(serverPlayer);
            FentanylEffectsManager.syncActiveEffect(serverPlayer);
            LSDEffectsManager.syncActiveEffect(serverPlayer);
            UniversalOverdoseHandler.syncActiveEffect(serverPlayer);
            OpiateWithdrawalEffectsManager.syncActiveEffect(serverPlayer);
        } else {
            HeroinEffectsManager.tickActiveEffect(player);
            FentanylEffectsManager.tickActiveEffect(player);
            LSDEffectsManager.tickActiveEffect(player, level);
            UniversalOverdoseHandler.tickActiveEffect(player, level);
            OpiateWithdrawalEffectsManager.tickActiveEffect(player, level);
        }

        MorphineEffectsManager.updateOngoingGameplayEffects(player, level);
        HeroinEffectsManager.updateOngoingGameplayEffects(player, level);
        FentanylEffectsManager.updateOngoingGameplayEffects(player, level);
        UniversalOverdoseHandler.updateOngoingGameplayEffects(player, level);
        OpiateWithdrawalEffectsManager.updateOngoingGameplayEffects(player, level);

        if (!HeroinEffectsManager.isHeroinActive(player, level)) {
            HeroinEffectsManager.clearHeroinEffect(player);
        }

        if (!FentanylEffectsManager.isFentanylActive(player, level)
                && FentanylEffectsManager.getRemainingDurationTicks(player) > 0) {
            FentanylEffectsManager.cleanupFentanylEffect(player, level);
        }

        if (!LSDEffectsManager.isLsdActive(player, level)) {
            LSDEffectsManager.clearLsdEffect(player);
        }

        if (UniversalOverdoseHandler.hasOpioidOverdoseState(player)
                && !UniversalOverdoseHandler.isOpioidOverdoseActive(player, level)) {
            UniversalOverdoseHandler.clearOpioidOverdose(player);
        }

        if (player.isDeadOrDying()) {
            if (serverPlayer != null) {
                clearAllDrugEffects(serverPlayer);
            } else {
                DrugStateManager.clearAll(player);
            }
            return;
        }

        DrugStateManager.syncIndividualEffectsWithActiveDrugs(player, level);
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
            DrugStateManager.forgetSyncedVisualState(serverPlayer);
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
        FentanylEffectsManager.cleanupFentanylEffect(event.getOriginal(), event.getOriginal().level());
        FentanylEffectsManager.cleanupFentanylEffect(event.getEntity(), event.getEntity().level());
        LSDEffectsManager.clearLsdEffect(event.getOriginal());
        LSDEffectsManager.clearLsdEffect(event.getEntity());
        UniversalOverdoseHandler.clearOpioidOverdose(event.getOriginal());
        UniversalOverdoseHandler.clearOpioidOverdose(event.getEntity());
        DrugStateManager.clearAll(event.getOriginal());
        DrugStateManager.clearAll(event.getEntity());
        DrugStateManager.forgetSyncedVisualState(event.getOriginal());
        DrugStateManager.forgetSyncedVisualState(event.getEntity());
        OpiateWithdrawalEffectsManager.clearWithdrawalEffect(event.getOriginal(), event.getOriginal().level());
        OpiateWithdrawalEffectsManager.clearWithdrawalEffect(event.getEntity(), event.getEntity().level());
        PENDING_EFFECT_SYNC.remove(event.getOriginal().getUUID());
        PENDING_EFFECT_SYNC.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            clearAllDrugEffects(serverPlayer);
            PENDING_EFFECT_SYNC.remove(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            clearAllDrugEffects(serverPlayer);
            PENDING_EFFECT_SYNC.remove(serverPlayer.getUUID());
        }
    }

    private static void clearAllDrugEffects(ServerPlayer serverPlayer) {
        MorphineEffectsManager.cleanupMorphineEffects(serverPlayer, serverPlayer.level());
        HeroinEffectsManager.cleanupHeroinEffect(serverPlayer, serverPlayer.level());
        FentanylEffectsManager.cleanupFentanylEffect(serverPlayer, serverPlayer.level());
        LSDEffectsManager.clearLsdEffect(serverPlayer);
        ModMessages.sendToPlayer(new LSDEffectS2CPacket(0, 0, 0.0F), serverPlayer);
        UniversalOverdoseHandler.clearOpioidOverdose(serverPlayer);
        OpiateWithdrawalEffectsManager.clearWithdrawalEffect(serverPlayer, serverPlayer.level());
        DrugStateManager.clearAll(serverPlayer);
        ModMessages.sendToPlayer(new DrugVisualStateS2CPacket(false, false, false, false, false, false), serverPlayer);
    }
}
