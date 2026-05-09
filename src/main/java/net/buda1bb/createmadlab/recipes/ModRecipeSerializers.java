package net.buda1bb.createmadlab.recipes;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateMadLab.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CustomFillingRecipe>> CUSTOM_FILLING =
            SERIALIZERS.register("custom_filling",
                    CustomFillingRecipe.Serializer::new);
}
