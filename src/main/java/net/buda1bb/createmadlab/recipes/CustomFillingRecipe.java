package net.buda1bb.createmadlab.recipes;

import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import net.buda1bb.createmadlab.item.SyringeItem;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class CustomFillingRecipe extends FillingRecipe {

    public CustomFillingRecipe(ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public boolean matches(RecipeWrapper inv, Level level) {
        if (!super.matches(inv, level)) {
            return false;
        }
        ItemStack container = inv.getItem(0);
        return isValidInput(container, level);
    }

    private boolean isValidInput(ItemStack container, Level level) {
        if (container.getItem() instanceof LSDPaperItem) {
            return isValidLsdPaperInput(container, level);
        }

        if (hasNonEmptyContentNBT(container)) {
            return false;
        }

        if (container.getItem() instanceof SyringeItem) {
            return !SyringeItem.hasContent(container);
        }
        return true;
    }

    private boolean isValidLsdPaperInput(ItemStack container, Level level) {
        ItemStack result = getResultItem(level.registryAccess());
        if (!(result.getItem() instanceof LSDPaperItem)) {
            return false;
        }

        double targetDose = LSDPaperItem.getDose(result);
        double currentDose = LSDPaperItem.getDose(container);

        if (targetDose <= 1.0D) {
            return currentDose <= 1.0D;
        }

        return Math.abs(currentDose - (targetDose - 1.0D)) < 0.001D;
    }

    private boolean hasNonEmptyContentNBT(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }

        CompoundTag nbt = stack.getTag();
        if (nbt.contains("content")) {
            String content = nbt.getString("content");
            if (!content.equals("empty")) {
                return true;
            }
        }

        if (nbt.contains("display")) {
            CompoundTag displayTag = nbt.getCompound("display");
            if (displayTag.contains("content")) {
                String content = displayTag.getString("content");
                if (!content.equals("empty")) {
                    return true;
                }
            }
        }

        return false;
    }
}
