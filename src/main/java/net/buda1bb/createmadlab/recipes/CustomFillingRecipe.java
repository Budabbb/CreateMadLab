package net.buda1bb.createmadlab.recipes;

import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.buda1bb.createmadlab.item.SyringeItem;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.buda1bb.createmadlab.util.ItemDataUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

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

    public CustomFillingRecipe(ProcessingRecipeParams params) {
        this(params, "", false);
    }

    public CustomFillingRecipe(ProcessingRecipeParams params, String dynamicLacingContent, boolean dynamicLsdPaperDosing) {
        super(params);
        this.dynamicLacingContent = dynamicLacingContent == null ? "" : dynamicLacingContent;
        this.dynamicLsdPaperDosing = dynamicLsdPaperDosing;
    }

    @Override
    public boolean matches(SingleRecipeInput inv, Level level) {
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
    public List<ItemStack> rollResults(RandomSource randomSource) {
        if (!dynamicResult.isEmpty()) {
            return List.of(dynamicResult.copy());
        }

        return super.rollResults(randomSource);
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
        if (container.isEmpty() || !container.has(DataComponents.FOOD)) {
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
        ItemDataUtils.update(result, tag -> {
            tag.putString(CONTENT_TAG, dynamicLacingContent);

            if (LSD_CONTENT.equals(dynamicLacingContent)) {
                double currentDose = LSD_CONTENT.equals(getContent(input)) ? getDose(input, 1.0D) : 0.0D;
                tag.putDouble(DOSE_TAG, Math.min(MAX_LSD_DOSE, currentDose + 1.0D));
            }
        });

        return result;
    }

    private ItemStack createDynamicLsdPaperCopy(ItemStack input) {
        ItemStack result = input.copy();
        result.setCount(1);
        ItemDataUtils.update(result, tag -> tag.putDouble(DOSE_TAG, Math.min(MAX_LSD_DOSE, getDose(input, 1.0D) + 1.0D)));
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
        if (ItemDataUtils.contains(stack, CONTENT_TAG)) {
            return ItemDataUtils.getTagCopy(stack).getString(CONTENT_TAG);
        }
        return null;
    }

    private double getDose(ItemStack stack, double defaultDose) {
        if (ItemDataUtils.contains(stack, DOSE_TAG)) {
            return ItemDataUtils.getTagCopy(stack).getDouble(DOSE_TAG);
        }
        return defaultDose;
    }

    private boolean hasNonEmptyContentNBT(ItemStack stack) {
        if (stack.isEmpty() || !ItemDataUtils.hasCustomData(stack)) {
            return false;
        }

        CompoundTag nbt = ItemDataUtils.getTagCopy(stack);
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

    public static class Serializer implements RecipeSerializer<CustomFillingRecipe> {
        private final MapCodec<CustomFillingRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ProcessingRecipeParams.CODEC.forGetter(CustomFillingRecipe::getParams),
                Codec.STRING.optionalFieldOf(DYNAMIC_LACING_CONTENT, "").forGetter(recipe -> recipe.dynamicLacingContent),
                Codec.BOOL.optionalFieldOf(DYNAMIC_LSD_PAPER_DOSING, false).forGetter(recipe -> recipe.dynamicLsdPaperDosing)
        ).apply(instance, CustomFillingRecipe::new));

        private final StreamCodec<RegistryFriendlyByteBuf, CustomFillingRecipe> streamCodec = StreamCodec.of(
                (buffer, recipe) -> {
                    ProcessingRecipeParams.STREAM_CODEC.encode(buffer, recipe.getParams());
                    buffer.writeUtf(recipe.dynamicLacingContent);
                    buffer.writeBoolean(recipe.dynamicLsdPaperDosing);
                },
                buffer -> new CustomFillingRecipe(
                        ProcessingRecipeParams.STREAM_CODEC.decode(buffer),
                        buffer.readUtf(),
                        buffer.readBoolean()
                )
        );

        @Override
        public MapCodec<CustomFillingRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CustomFillingRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
