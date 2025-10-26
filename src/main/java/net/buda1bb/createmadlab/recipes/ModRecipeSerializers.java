package net.buda1bb.createmadlab.recipes;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CreateMadLab.MOD_ID);

    public static final RegistryObject<RecipeSerializer<CustomFillingRecipe>> CUSTOM_FILLING =
            SERIALIZERS.register("custom_filling",
                    () -> new ProcessingRecipeSerializer<>(CustomFillingRecipe::new));
}