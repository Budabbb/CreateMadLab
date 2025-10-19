package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.item.LSDPaperItem;
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
        if (!initialized) {
            ShaderpackExtractor.extractShaderpackStructure();
            initialized = true;
        }
    }

    public static void activateShaders(double dose) {
        ShaderpackExtractor.activateShaders(dose);
        shadersActive = true;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    public static void deactivateShaders() {
        ShaderpackExtractor.deactivateShaders();
        shadersActive = false;
        playerWasDead = false;
        reloadScheduled.set(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            LSDPaperItem.handleLSDEffects(event.player, event.player.level());
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

        if (player != null && shadersActive) {
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
            if (player == mc.player && shadersActive) {
                deactivateShaders();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (shadersActive) {
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
        return shadersActive;
    }
}