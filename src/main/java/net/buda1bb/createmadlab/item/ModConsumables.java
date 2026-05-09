package net.buda1bb.createmadlab.item;

import net.minecraft.world.food.FoodProperties;

public class ModConsumables {
    public static final FoodProperties LSD_PAPER = new FoodProperties.Builder()
            .nutrition(0)
            .saturationModifier(0.0F)
            .alwaysEdible()
            .build();
    public static final FoodProperties SYRINGE = new FoodProperties.Builder()
            .nutrition(0)
            .saturationModifier(0.0F)
            .alwaysEdible()
            .build();
    public static final FoodProperties SODIUM_CHLORIDE = new FoodProperties.Builder()
            .nutrition(0)
            .saturationModifier(0.1F)
            .alwaysEdible()
            .build();
}
