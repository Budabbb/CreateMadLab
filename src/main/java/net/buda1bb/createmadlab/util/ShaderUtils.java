package net.buda1bb.createmadlab.util;

import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaderUtils {
    public static final int HEROIN_EFFECT_DURATION_TICKS = 185 * 20;
    public static final int LSD_EFFECT_DURATION_TICKS = 20 * 60 * 5;
    private static final String HEROIN_MANAGER_CLASS = "net.buda1bb.createmadlab.client.HeroinClientEffectManager";
    private static final String MORPHINE_MANAGER_CLASS = "net.buda1bb.createmadlab.client.MorphineClientEffectManager";
    private static final String LSD_MANAGER_CLASS = "net.buda1bb.createmadlab.client.LSDClientEffectManager";
    private static final String FENTANYL_MANAGER_CLASS = "net.buda1bb.createmadlab.client.FentanylClientEffectManager";
    private static final String WITHDRAWAL_MANAGER_CLASS = "net.buda1bb.createmadlab.client.OpiateWithdrawalClientEffectManager";
    private static final String VISUAL_AUTHORITY_CLASS = "net.buda1bb.createmadlab.client.ClientDrugVisualAuthority";
    private static final String SMOOTH_CAMERA_MANAGER_CLASS = "net.buda1bb.createmadlab.client.DrugSmoothCameraManager";
    private static final String MINECRAFT_CLASS = "net.minecraft.client.Minecraft";
    private static final Map<ClientMethodKey, Method> CLIENT_METHOD_CACHE = new HashMap<>();

    private static final ClientMethodKey HEROIN_ACTIVATE = ClientMethodKey.of(
            HEROIN_MANAGER_CLASS, "activate", int.class, int.class, float.class, boolean.class, boolean.class
    );
    private static final ClientMethodKey HEROIN_DEACTIVATE = ClientMethodKey.of(HEROIN_MANAGER_CLASS, "deactivate");
    private static final ClientMethodKey HEROIN_IS_ACTIVE = ClientMethodKey.of(HEROIN_MANAGER_CLASS, "isActive");
    private static final ClientMethodKey MORPHINE_ACTIVATE = ClientMethodKey.of(
            MORPHINE_MANAGER_CLASS, "activate", int.class, int.class, int.class, float.class,
            boolean.class, boolean.class, boolean.class, float.class, float.class, float.class
    );
    private static final ClientMethodKey MORPHINE_DEACTIVATE = ClientMethodKey.of(MORPHINE_MANAGER_CLASS, "deactivate");
    private static final ClientMethodKey MORPHINE_IS_ACTIVE = ClientMethodKey.of(MORPHINE_MANAGER_CLASS, "isActive");
    private static final ClientMethodKey LSD_ACTIVATE = ClientMethodKey.of(LSD_MANAGER_CLASS, "activate", int.class, int.class, float.class);
    private static final ClientMethodKey LSD_DEACTIVATE = ClientMethodKey.of(LSD_MANAGER_CLASS, "deactivate");
    private static final ClientMethodKey LSD_IS_ACTIVE = ClientMethodKey.of(LSD_MANAGER_CLASS, "isActive");
    private static final ClientMethodKey FENTANYL_ACTIVATE = ClientMethodKey.of(
            FENTANYL_MANAGER_CLASS, "activateFentanyl", int.class, int.class, boolean.class,
            float.class, float.class, float.class
    );
    private static final ClientMethodKey FENTANYL_ACTIVATE_OVERDOSE = ClientMethodKey.of(
            FENTANYL_MANAGER_CLASS, "activateOverdose", int.class, int.class, boolean.class, float.class, float.class
    );
    private static final ClientMethodKey FENTANYL_DEACTIVATE = ClientMethodKey.of(FENTANYL_MANAGER_CLASS, "deactivateFentanyl");
    private static final ClientMethodKey FENTANYL_DEACTIVATE_OVERDOSE = ClientMethodKey.of(FENTANYL_MANAGER_CLASS, "deactivateOverdose");
    private static final ClientMethodKey FENTANYL_IS_ACTIVE = ClientMethodKey.of(FENTANYL_MANAGER_CLASS, "isFentanylActive");
    private static final ClientMethodKey FENTANYL_IS_OVERDOSE_ACTIVE = ClientMethodKey.of(FENTANYL_MANAGER_CLASS, "isOverdoseActive");
    private static final ClientMethodKey WITHDRAWAL_ACTIVATE = ClientMethodKey.of(
            WITHDRAWAL_MANAGER_CLASS, "activate", int.class, int.class, float.class
    );
    private static final ClientMethodKey WITHDRAWAL_DEACTIVATE = ClientMethodKey.of(WITHDRAWAL_MANAGER_CLASS, "deactivate");
    private static final ClientMethodKey WITHDRAWAL_IS_ACTIVE = ClientMethodKey.of(WITHDRAWAL_MANAGER_CLASS, "isActive");
    private static final ClientMethodKey VISUAL_AUTHORITY_SYNC = ClientMethodKey.of(
            VISUAL_AUTHORITY_CLASS, "sync", boolean.class, boolean.class, boolean.class,
            boolean.class, boolean.class, boolean.class
    );
    private static final ClientMethodKey MINECRAFT_GET_INSTANCE = ClientMethodKey.of(MINECRAFT_CLASS, "getInstance");
    private static final ClientMethodKey SMOOTH_CAMERA_DISABLE_ALL = ClientMethodKey.ofTypeNames(
            SMOOTH_CAMERA_MANAGER_CLASS, "disableAll", MINECRAFT_CLASS
    );

    public static boolean isShaderpackEnabled() {
        return true;
    }

    public static void activateHeroinShaders() {
        activateHeroinShaders(0);
    }

    public static void activateHeroinShaders(int durationTicks) {
        activateHeroinShaders(durationTicks, HEROIN_EFFECT_DURATION_TICKS, 1.0F, true);
    }

    public static void activateHeroinShaders(int durationTicks, int totalTicks, float visualStrength, boolean cinematicCamera) {
        activateHeroinShaders(durationTicks, totalTicks, visualStrength, cinematicCamera, false);
    }

    public static void activateHeroinShaders(int durationTicks, int totalTicks, float visualStrength, boolean cinematicCamera, boolean fadeOut) {
        invokeClientStatic(HEROIN_ACTIVATE, durationTicks, totalTicks, visualStrength, cinematicCamera, fadeOut);
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
        activateMorphineShaders(remainingTicks, totalTicks, unstableHp, health, silentDecay, convertedDamage, 1.0F);
    }

    public static void activateMorphineShaders(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay,
                                               boolean convertedDamage, float visualStrength) {
        activateMorphineShaders(remainingTicks, totalTicks, unstableHp, health, silentDecay, convertedDamage, false, 0.0F, 0.0F, visualStrength);
    }

    public static void activateMorphineShaders(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay,
                                               boolean convertedDamage, boolean fadeOut, float startIntensity, float startDebtIntensity) {
        activateMorphineShaders(remainingTicks, totalTicks, unstableHp, health, silentDecay, convertedDamage, fadeOut,
                startIntensity, startDebtIntensity, 1.0F);
    }

    public static void activateMorphineShaders(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay,
                                               boolean convertedDamage, boolean fadeOut, float startIntensity, float startDebtIntensity,
                                               float visualStrength) {
        invokeClientStatic(MORPHINE_ACTIVATE, remainingTicks, totalTicks, unstableHp, health, silentDecay,
                convertedDamage, fadeOut, startIntensity, startDebtIntensity, visualStrength);
    }

    public static void activateLSDShaders(double dose) {
        activateLSDShaders(LSD_EFFECT_DURATION_TICKS, LSD_EFFECT_DURATION_TICKS, LSDEffectsManager.getStrengthForDose(dose));
    }

    public static void activateLSDShaders(int durationTicks, float strength) {
        activateLSDShaders(durationTicks, durationTicks, strength);
    }

    public static void activateLSDShaders(int remainingTicks, int totalTicks, float strength) {
        invokeClientStatic(LSD_ACTIVATE, remainingTicks, totalTicks, strength);
    }

    public static void activateFentanylShaders(int remainingTicks, int totalTicks, boolean fadeOut,
                                               float startIntensity, float startBlackoutAlpha, float visualStrength) {
        invokeClientStatic(FENTANYL_ACTIVATE, remainingTicks, totalTicks, fadeOut, startIntensity,
                startBlackoutAlpha, visualStrength);
    }

    public static void activateFentanylOverdoseShaders(int remainingTicks, int totalTicks) {
        activateFentanylShaders(remainingTicks, totalTicks, false, 0.0F, 0.0F, 1.0F);
    }

    public static void activateFentanylOverdoseShaders(int remainingTicks, int totalTicks, boolean fadeOut, float startIntensity, float startBlackoutAlpha) {
        activateFentanylShaders(remainingTicks, totalTicks, fadeOut, startIntensity, startBlackoutAlpha, 1.0F);
    }

    public static void activateOpioidOverdoseShaders(int remainingTicks, int totalTicks) {
        activateOpioidOverdoseShaders(remainingTicks, totalTicks, false, 0.0F, 0.0F);
    }

    public static void activateOpioidOverdoseShaders(int remainingTicks, int totalTicks, boolean fadeOut, float startIntensity, float startBlackoutAlpha) {
        invokeClientStatic(FENTANYL_ACTIVATE_OVERDOSE, remainingTicks, totalTicks, fadeOut, startIntensity, startBlackoutAlpha);
    }

    public static void activateOpiateWithdrawalShaders(int remainingTicks, int totalTicks, float intensity) {
        invokeClientStatic(WITHDRAWAL_ACTIVATE, remainingTicks, totalTicks, intensity);
    }

    public static void syncDrugVisualAuthority(boolean morphineActive, boolean heroinActive, boolean fentanylActive, boolean lsdActive,
                                               boolean opioidOverdoseActive, boolean withdrawalActive) {
        invokeClientStatic(VISUAL_AUTHORITY_SYNC, morphineActive, heroinActive, fentanylActive, lsdActive,
                opioidOverdoseActive, withdrawalActive);
    }

    public static void deactivateHeroinShaders() {
        invokeClientStatic(HEROIN_DEACTIVATE);
    }

    public static void deactivateMorphineShaders() {
        invokeClientStatic(MORPHINE_DEACTIVATE);
    }

    public static void deactivateLSDShaders() {
        invokeClientStatic(LSD_DEACTIVATE);
    }

    public static void deactivateFentanylShaders() {
        invokeClientStatic(FENTANYL_DEACTIVATE);
    }

    public static void deactivateFentanylOverdoseShaders() {
        deactivateFentanylShaders();
    }

    public static void deactivateOpioidOverdoseShaders() {
        invokeClientStatic(FENTANYL_DEACTIVATE_OVERDOSE);
    }

    public static void deactivateOpiateWithdrawalShaders() {
        invokeClientStatic(WITHDRAWAL_DEACTIVATE);
    }

    public static void deactivateShaders() {
        syncDrugVisualAuthority(false, false, false, false, false, false);
        deactivateHeroinShaders();
        deactivateMorphineShaders();
        deactivateFentanylShaders();
        deactivateLSDShaders();
        deactivateOpioidOverdoseShaders();
        deactivateOpiateWithdrawalShaders();
        forceRestoreDrugSmoothCamera();
    }

    public static boolean areHeroinShadersActive() {
        return invokeClientBoolean(HEROIN_IS_ACTIVE);
    }

    public static boolean areMorphineShadersActive() {
        return invokeClientBoolean(MORPHINE_IS_ACTIVE);
    }

    public static boolean areLSDShadersActive() {
        return invokeClientBoolean(LSD_IS_ACTIVE);
    }

    public static boolean areFentanylShadersActive() {
        return invokeClientBoolean(FENTANYL_IS_ACTIVE);
    }

    public static boolean areFentanylOverdoseShadersActive() {
        return areFentanylShadersActive();
    }

    public static boolean areOpioidOverdoseShadersActive() {
        return invokeClientBoolean(FENTANYL_IS_OVERDOSE_ACTIVE);
    }

    public static boolean areOpiateWithdrawalShadersActive() {
        return invokeClientBoolean(WITHDRAWAL_IS_ACTIVE);
    }

    public static boolean areShadersActive() {
        return areHeroinShadersActive()
                || areMorphineShadersActive()
                || areFentanylShadersActive()
                || areLSDShadersActive()
                || areOpioidOverdoseShadersActive()
                || areOpiateWithdrawalShadersActive();
    }

    private static void forceRestoreDrugSmoothCamera() {
        Object minecraft = invokeClientStatic(MINECRAFT_GET_INSTANCE);
        if (minecraft != null) {
            invokeClientStatic(SMOOTH_CAMERA_DISABLE_ALL, minecraft);
        }
    }

    private static boolean invokeClientBoolean(ClientMethodKey methodKey) {
        Object result = invokeClientStatic(methodKey);
        return result instanceof Boolean active && active;
    }

    private static Object invokeClientStatic(ClientMethodKey methodKey, Object... arguments) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return null;
        }

        try {
            Method method = getClientMethod(methodKey);
            return method.invoke(null, arguments);
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static synchronized Method getClientMethod(ClientMethodKey methodKey) throws ReflectiveOperationException {
        Method cachedMethod = CLIENT_METHOD_CACHE.get(methodKey);
        if (cachedMethod != null) {
            return cachedMethod;
        }

        Class<?> ownerClass = Class.forName(methodKey.ownerClassName());
        Class<?>[] parameterTypes = resolveParameterTypes(methodKey.parameterTypeNames());
        Method method = ownerClass.getMethod(methodKey.methodName(), parameterTypes);
        CLIENT_METHOD_CACHE.put(methodKey, method);
        return method;
    }

    private static Class<?>[] resolveParameterTypes(List<String> parameterTypeNames) throws ClassNotFoundException {
        Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.size()];
        for (int index = 0; index < parameterTypeNames.size(); index++) {
            parameterTypes[index] = resolveParameterType(parameterTypeNames.get(index));
        }
        return parameterTypes;
    }

    private static Class<?> resolveParameterType(String parameterTypeName) throws ClassNotFoundException {
        return switch (parameterTypeName) {
            case "boolean" -> boolean.class;
            case "float" -> float.class;
            case "int" -> int.class;
            default -> Class.forName(parameterTypeName);
        };
    }

    private record ClientMethodKey(String ownerClassName, String methodName, List<String> parameterTypeNames) {
        private static ClientMethodKey of(String ownerClassName, String methodName, Class<?>... parameterTypes) {
            String[] parameterTypeNames = new String[parameterTypes.length];
            for (int index = 0; index < parameterTypes.length; index++) {
                parameterTypeNames[index] = parameterTypes[index].getName();
            }
            return ofTypeNames(ownerClassName, methodName, parameterTypeNames);
        }

        private static ClientMethodKey ofTypeNames(String ownerClassName, String methodName, String... parameterTypeNames) {
            return new ClientMethodKey(ownerClassName, methodName, List.of(parameterTypeNames));
        }
    }
}
