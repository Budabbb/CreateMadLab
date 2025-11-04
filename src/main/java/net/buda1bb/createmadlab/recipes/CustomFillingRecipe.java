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
        return isValidInput(container);
    }

    private boolean isValidInput(ItemStack container) {
        if (hasNonEmptyContentNBT(container)) {
            System.out.println("Item already has non-empty content NBT tag - recipe blocked");
            return false;
        }

        if (container.getItem() instanceof SyringeItem) {
            boolean isEmpty = !SyringeItem.hasContent(container);
            System.out.println("Syringe validation - isEmpty: " + isEmpty);
            return isEmpty;
        }
        if (container.getItem() instanceof LSDPaperItem) {
            boolean isValidDose = LSDPaperItem.getDose(container) <= 1.0;
            System.out.println("LSD Paper validation - isValidDose: " + isValidDose);
            return isValidDose;
        }
        return true;
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