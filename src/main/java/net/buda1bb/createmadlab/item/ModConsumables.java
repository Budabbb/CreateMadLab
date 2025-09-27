package net.buda1bb.createmadlab.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModConsumables {
    public static final FoodProperties LSD_PAPER = new FoodProperties.Builder()
            .nutrition(0) // doesn’t restore hunger
            .saturationMod(0.0F)
            .alwaysEat() // can eat even if full
            .build();
}
