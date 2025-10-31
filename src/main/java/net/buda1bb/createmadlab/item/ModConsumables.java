package net.buda1bb.createmadlab.item;

import net.minecraft.world.food.FoodProperties;

public class ModConsumables {
    public static final FoodProperties LSD_PAPER = new FoodProperties.Builder()
            .nutrition(0)
            .saturationMod(0.0F)
            .alwaysEat()
            .build();
    public static final FoodProperties SYRINGE = new FoodProperties.Builder()
            .nutrition(0)
            .saturationMod(0.0F)
            .alwaysEat()
            .build();
    public static final FoodProperties SEA_SALT = new FoodProperties.Builder()
            .nutrition(0)
            .saturationMod(0.1F)
            .alwaysEat()
            .build();
}
