package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientDrugCleanupHandler {
    private ClientDrugCleanupHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientDrugEffects();
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && event.getEntity() == minecraft.player) {
            clearClientDrugEffects();
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && event.getEntity() == minecraft.player) {
            clearClientDrugEffects();
        }
    }

    private static void clearClientDrugEffects() {
        ClientDrugVisualAuthority.clearAll();
        ShaderUtils.deactivateShaders();
    }
}
