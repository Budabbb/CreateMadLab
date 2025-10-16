package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ShaderFileSwapper {
    private static final AtomicBoolean reloadScheduled = new AtomicBoolean(false);

    public static void activateLSDShaders() {
        // Extract shader files
        ShaderpackExtractor.extractShaderFiles();
        System.out.println("[" + CreateMadLab.MOD_ID + "] LSD shader files activated");
        reloadScheduled.set(true);
    }

    public static void deactivateLSDShaders() {
        // Delete shader files
        ShaderpackExtractor.deleteShaderFiles();
        System.out.println("[" + CreateMadLab.MOD_ID + "] LSD shader files deactivated");
        reloadScheduled.set(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && reloadScheduled.getAndSet(false)) {
            reloadShaders();
        }
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
}