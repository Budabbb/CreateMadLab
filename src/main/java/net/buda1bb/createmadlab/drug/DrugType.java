package net.buda1bb.createmadlab.drug;

public enum DrugType {
    MORPHINE(DrugClass.OPIOID, 0.7F, 0.6F, 0.0F, 0.35F, 0.55F, 120 * 20),
    HEROIN(DrugClass.OPIOID, 1.0F, 0.85F, 0.0F, 0.45F, 0.8F, 185 * 20),
    FENTANYL(DrugClass.OPIOID, 2.8F, 1.0F, 0.0F, 0.65F, 1.2F, 95 * 20),
    LSD(DrugClass.PSYCHEDELIC, 0.0F, -0.15F, 1.0F, 0.1F, 0.0F, 5 * 60 * 20);

    private final DrugClass drugClass;
    private final float opioidLoad;
    private final float sedation;
    private final float hallucination;
    private final float movementImpairment;
    private final float respiratorySuppression;
    private final int durationTicks;

    DrugType(DrugClass drugClass, float opioidLoad, float sedation, float hallucination,
             float movementImpairment, float respiratorySuppression, int durationTicks) {
        this.drugClass = drugClass;
        this.opioidLoad = opioidLoad;
        this.sedation = sedation;
        this.hallucination = hallucination;
        this.movementImpairment = movementImpairment;
        this.respiratorySuppression = respiratorySuppression;
        this.durationTicks = durationTicks;
    }

    public DrugClass getDrugClass() {
        return drugClass;
    }

    public float getOpioidLoad() {
        return opioidLoad;
    }

    public float getSedation() {
        return sedation;
    }

    public float getHallucination() {
        return hallucination;
    }

    public float getMovementImpairment() {
        return movementImpairment;
    }

    public float getRespiratorySuppression() {
        return respiratorySuppression;
    }

    public int getDurationTicks() {
        return durationTicks;
    }
}
