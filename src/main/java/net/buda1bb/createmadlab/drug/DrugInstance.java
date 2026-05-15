package net.buda1bb.createmadlab.drug;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class DrugInstance {
    private static final String TYPE_TAG = "Type";
    private static final String DOSE_TAG = "Dose";
    private static final String AGE_TICKS_TAG = "AgeTicks";
    private static final String DURATION_TICKS_TAG = "DurationTicks";

    public final DrugType type;
    public final DrugClass drugClass;
    public final float dose;
    public int ageTicks;
    public final int durationTicks;
    public float intensity;
    public TripPhase phase;

    public DrugInstance(DrugType type, float dose, int ageTicks, int durationTicks) {
        this.type = type;
        this.drugClass = type.getDrugClass();
        this.dose = Math.max(0.0F, dose);
        this.ageTicks = Math.max(0, ageTicks);
        this.durationTicks = Math.max(1, durationTicks);
        updateDerivedValues();
    }

    public DrugInstance copy() {
        return new DrugInstance(type, dose, ageTicks, durationTicks);
    }

    public void tick() {
        ageTicks++;
        updateDerivedValues();
    }

    public boolean isExpired() {
        return ageTicks >= durationTicks;
    }

    public float getScaledBase(float baseValue) {
        return baseValue * dose * intensity;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE_TAG, type.name());
        tag.putFloat(DOSE_TAG, dose);
        tag.putInt(AGE_TICKS_TAG, ageTicks);
        tag.putInt(DURATION_TICKS_TAG, durationTicks);
        return tag;
    }

    public static DrugInstance load(CompoundTag tag) {
        if (tag == null || !tag.contains(TYPE_TAG)) {
            return null;
        }

        try {
            DrugType type = DrugType.valueOf(tag.getString(TYPE_TAG));
            float dose = tag.contains(DOSE_TAG) ? tag.getFloat(DOSE_TAG) : 1.0F;
            int ageTicks = tag.getInt(AGE_TICKS_TAG);
            int durationTicks = tag.contains(DURATION_TICKS_TAG) ? tag.getInt(DURATION_TICKS_TAG) : type.getDurationTicks();
            return new DrugInstance(type, dose, ageTicks, durationTicks);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void updateDerivedValues() {
        float progress = Mth.clamp(ageTicks / (float) durationTicks, 0.0F, 1.0F);
        float comeUpEnd = Math.min(0.10F, 120.0F / durationTicks);
        float comedownStart = 0.85F;

        if (progress < comeUpEnd) {
            phase = TripPhase.COME_UP;
        } else if (progress >= comedownStart) {
            phase = TripPhase.COMEDOWN;
        } else {
            phase = TripPhase.PEAK;
        }

        if (progress >= comedownStart) {
            intensity = 1.0F - smoothstep(comedownStart, 1.0F, progress);
        } else {
            intensity = 1.0F;
        }
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / Math.max(edge1 - edge0, 0.0001F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
