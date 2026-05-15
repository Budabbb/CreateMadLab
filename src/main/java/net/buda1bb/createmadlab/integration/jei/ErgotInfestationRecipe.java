package net.buda1bb.createmadlab.integration.jei;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.item.ModItems;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public record ErgotInfestationRecipe(List<ItemStack> wheatInputs, ItemStack output) {
    public static final RecipeType<ErgotInfestationRecipe> TYPE = RecipeType.create(
            CreateMadLab.MOD_ID,
            "ergot_infestation",
            ErgotInfestationRecipe.class
    );

    public static ErgotInfestationRecipe create() {
        return new ErgotInfestationRecipe(
                List.of(new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.WHEAT)),
                new ItemStack(ModItems.ERGOT_FUNGUS.get())
        );
    }
}
