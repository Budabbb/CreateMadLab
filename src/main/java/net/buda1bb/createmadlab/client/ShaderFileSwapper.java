package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.buda1bb.createmadlab.item.SyringeItem;
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
            SyringeItem.handlePlayerLogin(player, player.level());
        }
    }

    // LSD Methods - unchanged
    public static void activateLSDShaders(double dose) {
        ShaderpackExtractor.activateLSDShaders(dose);
        lsdShadersActive = true;
        heroinShadersActive = false; // Ensure only one effect is active at a time
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    // Heroin Methods - simplified, timing handled in SyringeItem
    public static void activateHeroinShaders() {
        ShaderpackExtractor.activateHeroinShaders();
        heroinShadersActive = true;
        lsdShadersActive = false; // Ensure only one effect is active at a time
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    // Single deactivation method for both LSD and heroin shaders
    public static void deactivateShaders() {
        ShaderpackExtractor.deactivateShaders();
        lsdShadersActive = false;
        heroinShadersActive = false;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            // Handle both LSD and heroin effects
            LSDPaperItem.handleLSDEffects(event.player, event.player.level());
            SyringeItem.handleBlissEffects(event.player, event.player.level());

            // Extra safety: Clean up bliss effect if player is dead but still has data
            if (event.player.isDeadOrDying() && SyringeItem.isBlissActive(event.player)) {
                SyringeItem.cleanupBlissEffect(event.player, event.player.level());
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

        if (player != null && (lsdShadersActive || heroinShadersActive)) {
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
                // Force cleanup of bliss effect on death
                if (SyringeItem.isBlissActive(player)) {
                    SyringeItem.cleanupBlissEffect(player, player.level());
                }

                if (lsdShadersActive || heroinShadersActive) {
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
            // Force cleanup of bliss effect on respawn
            if (SyringeItem.isBlissActive(player)) {
                SyringeItem.cleanupBlissEffect(player, player.level());
            }

            // Ensure shaders are deactivated
            if (lsdShadersActive || heroinShadersActive) {
                deactivateShaders();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Player player = event.getPlayer();
        if (player != null) {
            SyringeItem.cleanupBlissEffect(player, player.level());
        }

        if (lsdShadersActive || heroinShadersActive) {
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
        return lsdShadersActive || heroinShadersActive;
    }

    public static boolean areLSDShadersActive() {
        return lsdShadersActive;
    }

    public static boolean areHeroinShadersActive() {
        return heroinShadersActive;
    }
}