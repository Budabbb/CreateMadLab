package net.buda1bb.createmadlab.util;

import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class ShaderUtils {
    public static final int HEROIN_EFFECT_DURATION_TICKS = 185 * 20;
    public static final int LSD_EFFECT_DURATION_TICKS = 20 * 60 * 5;

    public static boolean isShaderpackEnabled() {
        return true;
    }

    public static void activateHeroinShaders() {
        activateHeroinShaders(0);
    }

    public static void activateHeroinShaders(int durationTicks) {
        deactivateLSDShaders();
        deactivateMorphineShaders();
        deactivateFentanylOverdoseShaders();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.HeroinClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("activate", int.class);
                method.invoke(null, durationTicks);
            } catch (Exception ignored) {
            }
        }
    }

    public static void activateMorphineShaders() {
        activateMorphineShaders(0, 0, 0, 0.0F, false, false);
    }

    public static void activateMorphineShaders(int remainingTicks, int totalTicks) {
        activateMorphineShaders(remainingTicks, totalTicks, 0, 0.0F, false, false);
    }

    public static void activateMorphineShaders(int remainingTicks, int totalTicks, int unstableHp) {
        activateMorphineShaders(remainingTicks, totalTicks, unstableHp, 0.0F, false, false);
    }

    public static void activateMorphineShaders(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay, boolean convertedDamage) {
        deactivateHeroinShaders();
        deactivateLSDShaders();
        deactivateFentanylOverdoseShaders();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.MorphineClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("activate", int.class, int.class, int.class, float.class, boolean.class, boolean.class);
                method.invoke(null, remainingTicks, totalTicks, unstableHp, health, silentDecay, convertedDamage);
            } catch (Exception ignored) {
            }
        }
    }

    public static void activateLSDShaders(double dose) {
        activateLSDShaders(LSD_EFFECT_DURATION_TICKS, LSD_EFFECT_DURATION_TICKS, LSDEffectsManager.getStrengthForDose(dose));
    }

    public static void activateLSDShaders(int durationTicks, float strength) {
        activateLSDShaders(durationTicks, durationTicks, strength);
    }

    public static void activateLSDShaders(int remainingTicks, int totalTicks, float strength) {
        deactivateHeroinShaders();
        deactivateMorphineShaders();
        deactivateFentanylOverdoseShaders();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.LSDClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("activate", int.class, int.class, float.class);
                method.invoke(null, remainingTicks, totalTicks, strength);
            } catch (Exception ignored) {
            }
        }
    }

    public static void activateFentanylOverdoseShaders(int remainingTicks, int totalTicks) {
        deactivateHeroinShaders();
        deactivateMorphineShaders();
        deactivateLSDShaders();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.FentanylClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("activate", int.class, int.class);
                method.invoke(null, remainingTicks, totalTicks);
            } catch (Exception ignored) {
            }
        }
    }

    public static void deactivateHeroinShaders() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.HeroinClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("deactivate");
                method.invoke(null);
            } catch (Exception ignored) {
            }
        }
    }

    public static void deactivateMorphineShaders() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.MorphineClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("deactivate");
                method.invoke(null);
            } catch (Exception ignored) {
            }
        }
    }

    public static void deactivateLSDShaders() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.LSDClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("deactivate");
                method.invoke(null);
            } catch (Exception ignored) {
            }
        }
    }

    public static void deactivateFentanylOverdoseShaders() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.FentanylClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("deactivate");
                method.invoke(null);
            } catch (Exception ignored) {
            }
        }
    }

    public static void deactivateShaders() {
        deactivateHeroinShaders();
        deactivateMorphineShaders();
        deactivateLSDShaders();
        deactivateFentanylOverdoseShaders();
    }

    public static boolean areHeroinShadersActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.HeroinClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("isActive");
                return (boolean) method.invoke(null);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    public static boolean areMorphineShadersActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.MorphineClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("isActive");
                return (boolean) method.invoke(null);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    public static boolean areLSDShadersActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.LSDClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("isActive");
                return (boolean) method.invoke(null);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    public static boolean areFentanylOverdoseShadersActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> managerClass = Class.forName("net.buda1bb.createmadlab.client.FentanylClientEffectManager");
                java.lang.reflect.Method method = managerClass.getMethod("isActive");
                return (boolean) method.invoke(null);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    public static boolean areShadersActive() {
        return areHeroinShadersActive()
                || areMorphineShadersActive()
                || areLSDShadersActive()
                || areFentanylOverdoseShadersActive();
    }
}
