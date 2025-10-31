package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.BlissEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ShaderFileSwapper {
    private static final AtomicBoolean reloadScheduled = new AtomicBoolean(false);
    private static boolean lsdShadersActive = false;
    private static boolean heroinShadersActive = false;
    private static boolean morphineShadersActive = false;
    private static boolean playerWasDead = false;
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!initialized) {
            ShaderpackExtractor.extractShaderpackStructure();
            initialized = true;
        }
        Player player = event.getPlayer();
        if (player != null) {
            BlissEffectsManager.handlePlayerLogin(player, player.level());
        }
    }

    // LSD Methods
    public static void activateLSDShaders(double dose) {
        ShaderpackExtractor.activateLSDShaders(dose);
        lsdShadersActive = true;
        heroinShadersActive = false;
        morphineShadersActive = false;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    // Heroin Methods
    public static void activateHeroinShaders() {
        ShaderpackExtractor.activateHeroinShaders();
        heroinShadersActive = true;
        lsdShadersActive = false;
        morphineShadersActive = false;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    // Morphine Methods
    public static void activateMorphineShaders() {
        ShaderpackExtractor.activateMorphineShaders();
        morphineShadersActive = true;
        lsdShadersActive = false;
        heroinShadersActive = false;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    // Single deactivation method for all shaders
    public static void deactivateShaders() {
        ShaderpackExtractor.deactivateShaders();
        lsdShadersActive = false;
        heroinShadersActive = false;
        morphineShadersActive = false;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            // Handle both LSD and heroin effects
            LSDPaperItem.handleLSDEffects(event.player, event.player.level());
            BlissEffectsManager.handleBlissEffects(event.player, event.player.level());

            // Handle morphine effects
            if (MorphineEffectsManager.isMorphineActive(event.player)) {
                long startTime = MorphineEffectsManager.getMorphineStartTime(event.player);
                long currentTime = event.player.level().getGameTime();
                long elapsedTicks = currentTime - startTime;

                MorphineEffectsManager.handleMorphineEffectTicks(event.player, event.player.level(), elapsedTicks);
            }

            // Extra safety: Clean up effects if player is dead but still has data
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (reloadScheduled.getAndSet(false)) {
                reloadShaders();
            }
            checkPlayerDeath();
        }
    }

    private static void checkPlayerDeath() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null && (lsdShadersActive || heroinShadersActive || morphineShadersActive)) {
            if (player.isDeadOrDying()) {
                if (!playerWasDead) {
                    deactivateShaders();
                    playerWasDead = true;
                }
            } else {
                playerWasDead = false;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Minecraft mc = Minecraft.getInstance();
            if (player == mc.player) {
                // Force cleanup of effects on death
                if (BlissEffectsManager.isBlissActive(player)) {
                    BlissEffectsManager.cleanupBlissEffect(player, player.level());
                }
                if (MorphineEffectsManager.isMorphineActive(player)) {
                    MorphineEffectsManager.cleanupMorphineEffects(player, player.level());
                }

                if (lsdShadersActive || heroinShadersActive || morphineShadersActive) {
                    deactivateShaders();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player) {
            // Force cleanup of effects on respawn
            if (BlissEffectsManager.isBlissActive(player)) {
                BlissEffectsManager.cleanupBlissEffect(player, player.level());
            }
            if (MorphineEffectsManager.isMorphineActive(player)) {
                MorphineEffectsManager.cleanupMorphineEffects(player, player.level());
            }

            // Ensure shaders are deactivated
            if (lsdShadersActive || heroinShadersActive || morphineShadersActive) {
                deactivateShaders();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Player player = event.getPlayer();
        if (player != null) {
            BlissEffectsManager.cleanupBlissEffect(player, player.level());
            MorphineEffectsManager.cleanupMorphineEffects(player, player.level());
        }

        if (lsdShadersActive || heroinShadersActive || morphineShadersActive) {
            deactivateShaders();
        }
        initialized = false;
    }

    private static void reloadShaders() {
        try {
            Thread.sleep(100);
            triggerOculusReload();
        } catch (Exception e) {
            // Silent fail
        }
    }

    private static void triggerOculusReload() {
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            java.lang.reflect.Method reloadMethod = irisClass.getMethod("reload");
            reloadMethod.invoke(null);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public static boolean areShadersActive() {
        return lsdShadersActive || heroinShadersActive || morphineShadersActive;
    }

    public static boolean areLSDShadersActive() {
        return lsdShadersActive;
    }

    public static boolean areHeroinShadersActive() {
        return heroinShadersActive;
    }

    public static boolean areMorphineShadersActive() {
        return morphineShadersActive;
    }
}