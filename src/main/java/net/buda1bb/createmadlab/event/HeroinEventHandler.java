package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.nbt.CompoundTag;
import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID)
public final class HeroinEventHandler {
    private static final String HEROIN_PROVOKED_PLAYER_TAG = CreateMadLab.MOD_ID + ".heroinProvokedPlayer";
    private static final String HEROIN_PROVOKED_UNTIL_TAG = CreateMadLab.MOD_ID + ".heroinProvokedUntil";

    private HeroinEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }

        if (event.getEntity() instanceof Player player && HeroinEffectsManager.isHeroinActive(player, player.level())) {
            HeroinEffectsManager.recordDamageTaken(player, player.level());
        }

        if (!(event.getSource().getEntity() instanceof Player player)
                || !HeroinEffectsManager.isHeroinActive(player, player.level())) {
            return;
        }

        event.setAmount(event.getAmount() * HeroinEffectsManager.getDamageMultiplier(player));

        if (event.getEntity() instanceof Mob mob && mob instanceof Enemy && HeroinEffectsManager.shouldPacifyMobs(player)) {
            provokeNearbyEnemies(mob, player);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player == null || !isHeroinActiveForCurrentSide(player)) {
            return;
        }

        event.setNewSpeed(Math.max(0.0F, event.getNewSpeed() * HeroinEffectsManager.getMiningSpeedMultiplier(player)));
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isHeroinActiveForCurrentSide(player)) {
            return;
        }

        Vec3 deltaMovement = player.getDeltaMovement();
        if (deltaMovement.x == 0.0D && deltaMovement.z == 0.0D) {
            return;
        }

        player.setDeltaMovement(deltaMovement.x * HeroinEffectsManager.getJumpDistanceMultiplier(player),
                deltaMovement.y,
                deltaMovement.z * HeroinEffectsManager.getJumpDistanceMultiplier(player));
    }

    @SubscribeEvent
    public static void onMobTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) {
            return;
        }

        LivingEntity target = mob.getTarget();
        if (!(target instanceof Player player)
                || !HeroinEffectsManager.isHeroinActive(player, player.level())
                || !HeroinEffectsManager.shouldPacifyMobs(player)) {
            return;
        }

        if (!wasProvokedByPlayer(mob, player)) {
            mob.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) {
            return;
        }

        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (!(newTarget instanceof Player player)
                || !HeroinEffectsManager.isHeroinActive(player, player.level())
                || !HeroinEffectsManager.shouldPacifyMobs(player)) {
            return;
        }

        if (!wasProvokedByPlayer(mob, player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    private static boolean wasProvokedByPlayer(Mob mob, Player player) {
        if (mob.getLastHurtByMob() == player) {
            return true;
        }

        CompoundTag data = mob.getPersistentData();
        if (!data.hasUUID(HEROIN_PROVOKED_PLAYER_TAG) || !data.contains(HEROIN_PROVOKED_UNTIL_TAG)) {
            return false;
        }

        long provokedUntil = data.getLong(HEROIN_PROVOKED_UNTIL_TAG);
        if (mob.level().getGameTime() > provokedUntil) {
            clearProvocation(mob);
            return false;
        }

        return player.getUUID().equals(data.getUUID(HEROIN_PROVOKED_PLAYER_TAG));
    }

    private static void provokeNearbyEnemies(Mob originMob, Player player) {
        double radius = HeroinEffectsManager.getGroupAggroRadius();
        long provokedUntil = originMob.level().getGameTime() + HeroinEffectsManager.getGroupAggroDurationTicks();

        for (Mob nearbyMob : originMob.level().getEntitiesOfClass(Mob.class, originMob.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof Enemy && candidate.isAlive())) {
            markProvokedByPlayer(nearbyMob, player, provokedUntil);
            nearbyMob.setTarget(player);
        }
    }

    private static void markProvokedByPlayer(Mob mob, Player player, long provokedUntil) {
        CompoundTag data = mob.getPersistentData();
        data.putUUID(HEROIN_PROVOKED_PLAYER_TAG, player.getUUID());
        data.putLong(HEROIN_PROVOKED_UNTIL_TAG, provokedUntil);
    }

    private static void clearProvocation(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(HEROIN_PROVOKED_PLAYER_TAG);
        data.remove(HEROIN_PROVOKED_UNTIL_TAG);
    }

    private static boolean isHeroinActiveForCurrentSide(Player player) {
        return player.level().isClientSide
                ? HeroinEffectsManager.isHeroinActive(player)
                : HeroinEffectsManager.isHeroinActive(player, player.level());
    }
}
