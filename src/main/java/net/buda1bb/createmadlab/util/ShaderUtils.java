package net.buda1bb.createmadlab.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class ShaderUtils {

    public static boolean isShaderpackEnabled() {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            return false;
        }

        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            java.lang.reflect.Method getCurrentPackNameMethod = irisClass.getMethod("getCurrentPackName");
            String currentPack = (String) getCurrentPackNameMethod.invoke(null);
            return currentPack != null && currentPack.equals("createmadlab_shaders");
        } catch (Exception e) {
            return false;
        }
    }

    public static void activateHeroinShaders() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> shaderSwapperClass = Class.forName("net.buda1bb.createmadlab.client.ShaderFileSwapper");
                java.lang.reflect.Method method = shaderSwapperClass.getMethod("activateHeroinShaders");
                method.invoke(null);
            } catch (Exception e) {
            }
        }
    }

    public static void activateMorphineShaders() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> shaderSwapperClass = Class.forName("net.buda1bb.createmadlab.client.ShaderFileSwapper");
                java.lang.reflect.Method method = shaderSwapperClass.getMethod("activateMorphineShaders");
                method.invoke(null);
            } catch (Exception e) {
            }
        }
    }

    public static void activateLSDShaders(double dose) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> shaderSwapperClass = Class.forName("net.buda1bb.createmadlab.client.ShaderFileSwapper");
                java.lang.reflect.Method method = shaderSwapperClass.getMethod("activateLSDShaders", double.class);
                method.invoke(null, dose);
            } catch (Exception e) {
            }
        }
    }

    public static void deactivateShaders() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> shaderSwapperClass = Class.forName("net.buda1bb.createmadlab.client.ShaderFileSwapper");
                java.lang.reflect.Method method = shaderSwapperClass.getMethod("deactivateShaders");
                method.invoke(null);
            } catch (Exception e) {
            }
        }
    }

    public static boolean areHeroinShadersActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> shaderSwapperClass = Class.forName("net.buda1bb.createmadlab.client.ShaderFileSwapper");
                java.lang.reflect.Method method = shaderSwapperClass.getMethod("areHeroinShadersActive");
                return (boolean) method.invoke(null);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static boolean areMorphineShadersActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> shaderSwapperClass = Class.forName("net.buda1bb.createmadlab.client.ShaderFileSwapper");
                java.lang.reflect.Method method = shaderSwapperClass.getMethod("areMorphineShadersActive");
                return (boolean) method.invoke(null);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static boolean areLSDShadersActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> shaderSwapperClass = Class.forName("net.buda1bb.createmadlab.client.ShaderFileSwapper");
                java.lang.reflect.Method method = shaderSwapperClass.getMethod("areLSDShadersActive");
                return (boolean) method.invoke(null);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static boolean areShadersActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> shaderSwapperClass = Class.forName("net.buda1bb.createmadlab.client.ShaderFileSwapper");
                java.lang.reflect.Method method = shaderSwapperClass.getMethod("areShadersActive");
                return (boolean) method.invoke(null);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}