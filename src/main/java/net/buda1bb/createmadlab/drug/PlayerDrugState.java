package net.buda1bb.createmadlab.drug;

import java.util.ArrayList;
import java.util.List;

public class PlayerDrugState {
    public final List<DrugInstance> activeDrugs = new ArrayList<>();
    public float sedation;
    public float hallucination;
    public float opioidVisualIntensity;
    public float opioidDangerLoad;
    public float movementImpairment;
    public float respiratorySuppression;
    public float overdoseProgress;
    public boolean isOverdosing;
}
