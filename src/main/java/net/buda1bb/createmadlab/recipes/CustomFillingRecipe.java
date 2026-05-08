package net.buda1bb.createmadlab.recipes;

import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import net.buda1bb.createmadlab.item.SyringeItem;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;

import com.google.gson.JsonObject;

import java.util.List;

public class CustomFillingRecipe extends FillingRecipe {
    private static final String CONTENT_TAG = "content";
    private static final String DOSE_TAG = "dose";
    private static final String DYNAMIC_LACING_CONTENT = "dynamic_lacing_content";
    private static final String DYNAMIC_LSD_PAPER_DOSING = "dynamic_lsd_paper_dosing";
    private static final String LSD_CONTENT = "lsd";
    private static final double MAX_LSD_DOSE = 4.0D;

    private String dynamicLacingContent = "";
    private boolean dynamicLsdPaperDosing = false;
    private ItemStack dynamicResult = ItemStack.EMPTY;

    public CustomFillingRecipe(ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public boolean matches(RecipeWrapper inv, Level level) {
        ItemStack container = inv.getItem(0);
        dynamicResult = ItemStack.EMPTY;

        if (isDynamicLacingRecipe()) {
            if (!isValidDynamicLacingInput(container)) {
                return false;
            }

            dynamicResult = createDynamicLacedCopy(container);
            return true;
        }

        if (isDynamicLsdPaperDosingRecipe()) {
            if (!super.matches(inv, level) || !isValidDynamicLsdPaperInput(container)) {
                return false;
            }

            dynamicResult = createDynamicLsdPaperCopy(container);
            return true;
        }

        if (!super.matches(inv, level)) {
            return false;
        }
        return isValidInput(container, level);
    }

    @Override
    public List<ItemStack> rollResults() {
        if (!dynamicResult.isEmpty()) {
            return List.of(dynamicResult.copy());
        }

        return super.rollResults();
    }

    @Override
    public void readAdditional(JsonObject json) {
        dynamicLacingContent = GsonHelper.getAsString(json, DYNAMIC_LACING_CONTENT, "");
        dynamicLsdPaperDosing = GsonHelper.getAsBoolean(json, DYNAMIC_LSD_PAPER_DOSING, false);
    }

    @Override
    public void readAdditional(FriendlyByteBuf buffer) {
        dynamicLacingContent = buffer.readUtf();
        dynamicLsdPaperDosing = buffer.readBoolean();
    }

    @Override
    public void writeAdditional(JsonObject json) {
        if (isDynamicLacingRecipe()) {
            json.addProperty(DYNAMIC_LACING_CONTENT, dynamicLacingContent);
        }
        if (isDynamicLsdPaperDosingRecipe()) {
            json.addProperty(DYNAMIC_LSD_PAPER_DOSING, true);
        }
    }

    @Override
    public void writeAdditional(FriendlyByteBuf buffer) {
        buffer.writeUtf(dynamicLacingContent);
        buffer.writeBoolean(dynamicLsdPaperDosing);
    }

    private boolean isValidInput(ItemStack container, Level level) {
        if (container.getItem() instanceof PotionItem) {
            return false;
        }

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

    private boolean isDynamicLacingRecipe() {
        return !dynamicLacingContent.isBlank();
    }

    private boolean isDynamicLsdPaperDosingRecipe() {
        return dynamicLsdPaperDosing;
    }

    private boolean isValidDynamicLacingInput(ItemStack container) {
        if (container.isEmpty() || !container.isEdible()) {
            return false;
        }

        if (container.getItem() instanceof PotionItem
                || container.getItem() instanceof LSDPaperItem
                || container.getItem() instanceof SyringeItem) {
            return false;
        }

        if (LSD_CONTENT.equals(dynamicLacingContent)) {
            String currentContent = getContent(container);
            if (currentContent == null || "empty".equals(currentContent)) {
                return !hasNonEmptyContentNBT(container);
            }

            return LSD_CONTENT.equals(currentContent) && getDose(container, 1.0D) < MAX_LSD_DOSE;
        }

        return !hasNonEmptyContentNBT(container);
    }

    private boolean isValidDynamicLsdPaperInput(ItemStack container) {
        return container.getItem() instanceof LSDPaperItem && getDose(container, 1.0D) < MAX_LSD_DOSE;
    }

    private ItemStack createDynamicLacedCopy(ItemStack input) {
        ItemStack result = input.copy();
        result.setCount(1);
        CompoundTag tag = result.getOrCreateTag();
        tag.putString(CONTENT_TAG, dynamicLacingContent);

        if (LSD_CONTENT.equals(dynamicLacingContent)) {
            double currentDose = LSD_CONTENT.equals(getContent(input)) ? getDose(input, 1.0D) : 0.0D;
            tag.putDouble(DOSE_TAG, Math.min(MAX_LSD_DOSE, currentDose + 1.0D));
        }

        return result;
    }

    private ItemStack createDynamicLsdPaperCopy(ItemStack input) {
        ItemStack result = input.copy();
        result.setCount(1);
        result.getOrCreateTag().putDouble(DOSE_TAG, Math.min(MAX_LSD_DOSE, getDose(input, 1.0D) + 1.0D));
        return result;
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

    private String getContent(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(CONTENT_TAG)) {
            return stack.getTag().getString(CONTENT_TAG);
        }
        return null;
    }

    private double getDose(ItemStack stack, double defaultDose) {
        if (stack.hasTag() && stack.getTag().contains(DOSE_TAG)) {
            return stack.getTag().getDouble(DOSE_TAG);
        }
        return defaultDose;
    }

    private boolean hasNonEmptyContentNBT(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }

        CompoundTag nbt = stack.getTag();
        if (nbt.contains(CONTENT_TAG)) {
            String content = nbt.getString(CONTENT_TAG);
            if (!content.equals("empty")) {
                return true;
            }
        }

        if (nbt.contains("display")) {
            CompoundTag displayTag = nbt.getCompound("display");
            if (displayTag.contains(CONTENT_TAG)) {
                String content = displayTag.getString(CONTENT_TAG);
                if (!content.equals("empty")) {
                    return true;
                }
            }
        }

        return false;
    }
}
