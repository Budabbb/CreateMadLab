package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ShaderFileSwapper {
    private static final AtomicBoolean reloadScheduled = new AtomicBoolean(false);
    private static boolean shadersActive = false;
    private static boolean playerWasDead = false;
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        // When player logs in, ensure shaders are deactivated
        if (!initialized) {
            System.out.println("[" + CreateMadLab.MOD_ID + "] Player logged in - ensuring shaders are deactivated");
            forceDeactivateShaders();
            initialized = true;
        }
    }

    public static void activateLSDShaders() {
        // Extract shader files
        ShaderpackExtractor.extractShaderFiles();
        System.out.println("[" + CreateMadLab.MOD_ID + "] LSD shader files activated");
        shadersActive = true;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    public static void deactivateLSDShaders() {
        // Delete shader files
        ShaderpackExtractor.deleteShaderFiles();
        System.out.println("[" + CreateMadLab.MOD_ID + "] LSD shader files deactivated");
        shadersActive = false;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    // Force deactivation without triggering reload (for startup)
    private static void forceDeactivateShaders() {
        // Delete shader files but don't set flags or trigger reload
        ShaderpackExtractor.deleteShaderFiles();
        shadersActive = false;
        playerWasDead = false;
        // Don't schedule reload to avoid unnecessary shader reload on login
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Handle shader reload
            if (reloadScheduled.getAndSet(false)) {
                reloadShaders();
            }

            // Check for player death every tick
            checkPlayerDeath();
        }
    }

    private static void checkPlayerDeath() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null && shadersActive) {
            if (player.isDeadOrDying()) {
                if (!playerWasDead) {
                    System.out.println("[" + CreateMadLab.MOD_ID + "] Player died - deactivating LSD shaders");
                    deactivateLSDShaders();
                    playerWasDead = true;
                }
            } else {
                playerWasDead = false;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // Backup death detection
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Minecraft mc = Minecraft.getInstance();
            if (player == mc.player && shadersActive) {
                System.out.println("[" + CreateMadLab.MOD_ID + "] Player died (LivingDeathEvent) - deactivating LSD shaders");
                deactivateLSDShaders();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // Clean up shaders when player leaves the world
        if (shadersActive) {
            System.out.println("[" + CreateMadLab.MOD_ID + "] Player logged out - deactivating LSD shaders");
            deactivateLSDShaders();
        }
        // Reset initialization for next login
        initialized = false;
    }

    private static void reloadShaders() {
        try {
            Thread.sleep(100); // Small delay
            triggerOculusReload();
        } catch (Exception e) {
            System.err.println("[" + CreateMadLab.MOD_ID + "] Failed to reload shaders: " + e.getMessage());
        }
    }

    private static void triggerOculusReload() {
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            java.lang.reflect.Method reloadMethod = irisClass.getMethod("reload");
            reloadMethod.invoke(null);
            System.out.println("[" + CreateMadLab.MOD_ID + "] Oculus shader reload triggered");
        } catch (Exception e) {
            System.err.println("[" + CreateMadLab.MOD_ID + "] Oculus reload failed: " + e.getMessage());
        }
    }

    // Helper method to check if shaders are currently active
    public static boolean areShadersActive() {
        return shadersActive;
    }
}