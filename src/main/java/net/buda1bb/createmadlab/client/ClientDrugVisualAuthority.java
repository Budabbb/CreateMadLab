package net.buda1bb.createmadlab.client;

public final class ClientDrugVisualAuthority {
    private static boolean morphineAllowed = true;
    private static boolean heroinAllowed = true;
    private static boolean fentanylAllowed = true;
    private static boolean lsdAllowed = true;
    private static boolean opioidOverdoseAllowed = true;
    private static boolean withdrawalAllowed = true;

    private ClientDrugVisualAuthority() {
    }

    public static void sync(boolean morphineActive, boolean heroinActive, boolean fentanylActive, boolean lsdActive,
                            boolean opioidOverdoseActive, boolean withdrawalActive) {
        morphineAllowed = morphineActive;
        heroinAllowed = heroinActive;
        fentanylAllowed = fentanylActive;
        lsdAllowed = lsdActive;
        opioidOverdoseAllowed = opioidOverdoseActive;
        withdrawalAllowed = withdrawalActive;
    }

    public static void clearAll() {
        sync(false, false, false, false, false, false);
    }

    public static boolean isMorphineAllowed() {
        return morphineAllowed;
    }

    public static boolean isHeroinAllowed() {
        return heroinAllowed;
    }

    public static boolean isFentanylAllowed() {
        return fentanylAllowed;
    }

    public static boolean isLsdAllowed() {
        return lsdAllowed;
    }

    public static boolean isOpioidOverdoseAllowed() {
        return opioidOverdoseAllowed;
    }

    public static boolean isWithdrawalAllowed() {
        return withdrawalAllowed;
    }
}
