package net.buda1bb.createmadlab.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

final class ClientRenderTime {
    private ClientRenderTime() {
    }

    static float partialTick(DeltaTracker tracker) {
        return tracker.getGameTimeDeltaPartialTick(false);
    }

    static float partialTick(Minecraft minecraft) {
        if (minecraft == null) {
            return 1.0F;
        }
        return minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }
}
