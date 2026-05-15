package net.buda1bb.createmadlab.client;

import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;

public final class DrugSmoothCameraManager {
    private static final Set<String> ENABLED_OWNERS = new HashSet<>();
    private static boolean captured;
    private static boolean previousSmoothCamera;

    private DrugSmoothCameraManager() {
    }

    public static void enable(Minecraft minecraft, String owner) {
        if (minecraft == null || owner == null || owner.isEmpty()) {
            return;
        }

        if (!captured) {
            previousSmoothCamera = minecraft.options.smoothCamera;
            captured = true;
        }

        ENABLED_OWNERS.add(owner);
        minecraft.options.smoothCamera = true;
    }

    public static void disable(Minecraft minecraft, String owner) {
        if (owner == null || owner.isEmpty()) {
            return;
        }

        ENABLED_OWNERS.remove(owner);
        if (ENABLED_OWNERS.isEmpty()) {
            restore(minecraft);
        }
    }

    public static void disableAll(Minecraft minecraft) {
        ENABLED_OWNERS.clear();
        restore(minecraft);
    }

    private static void restore(Minecraft minecraft) {
        if (minecraft != null && captured) {
            minecraft.options.smoothCamera = previousSmoothCamera;
        }
        captured = false;
    }
}
