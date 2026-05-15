package net.buda1bb.createmadlab.integration.jei;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ErgotInfestationCategory implements IRecipeCategory<ErgotInfestationRecipe> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 128;

    private static final int INPUT_X = 14;
    private static final int SLOT_Y = 18;
    private static final int FIRST_ARROW_X = 44;
    private static final int CROP_X = 78;
    private static final int CROP_Y = 12;
    private static final int SECOND_ARROW_X = 118;
    private static final int OUTPUT_X = 150;
    private static final int DESCRIPTION_Y = 78;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final ResourceLocation INFESTED_WHEAT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "textures/block/ergot_infested_wheat.png");
    private static final ResourceLocation RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "ergot_infestation");

    private final IDrawable icon;
    private final IDrawable arrow;

    public ErgotInfestationCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.ERGOT_FUNGUS.get()));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<ErgotInfestationRecipe> getRecipeType() {
        return ErgotInfestationRecipe.TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createmadlab.ergot_infestation");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ErgotInfestationRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(recipe.wheatInputs())
                .addRichTooltipCallback((slotView, tooltip) ->
                        tooltip.add(Component.translatable("jei.createmadlab.ergot_infestation.input.tooltip")
                                .withStyle(ChatFormatting.GRAY)));

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.output())
                .addRichTooltipCallback((slotView, tooltip) ->
                        tooltip.add(Component.translatable("jei.createmadlab.ergot_infestation.output.tooltip")
                                .withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void draw(ErgotInfestationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        arrow.draw(guiGraphics, FIRST_ARROW_X, 20);
        arrow.draw(guiGraphics, SECOND_ARROW_X, 20);

        guiGraphics.fill(CROP_X - 4, CROP_Y - 4, CROP_X + 36, CROP_Y + 36, 0x55304A28);
        guiGraphics.fill(CROP_X - 3, CROP_Y - 3, CROP_X + 35, CROP_Y + 35, 0x44B0A46A);
        guiGraphics.blit(INFESTED_WHEAT_TEXTURE, CROP_X, CROP_Y, 32, 32, 0, 0, 16, 16, 16, 16);

        drawCenteredWithShadow(guiGraphics, font, Component.translatable("jei.createmadlab.ergot_infestation.input"), INPUT_X + 9, 41, 42);
        drawCenteredWithShadow(guiGraphics, font, Component.translatable("jei.createmadlab.ergot_infestation.crop"), CROP_X + 16, 48, 60);
        drawCenteredWithShadow(guiGraphics, font, Component.translatable("jei.createmadlab.ergot_infestation.output"), OUTPUT_X + 9, 41, 42);

        drawWrappedCentered(guiGraphics, font, Component.translatable("jei.createmadlab.ergot_infestation.description"), WIDTH / 2, DESCRIPTION_Y, 160);
    }

    @Override
    public ResourceLocation getRegistryName(ErgotInfestationRecipe recipe) {
        return RECIPE_ID;
    }

    private static void drawCenteredWithShadow(GuiGraphics guiGraphics, Font font, Component text, int centerX, int y, int maxWidth) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        int lineY = y;

        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            FormattedCharSequence line = lines.get(i);
            guiGraphics.drawString(font, line, centerX - font.width(line) / 2, lineY, TEXT_COLOR, true);
            lineY += font.lineHeight + 1;
        }
    }

    private static void drawWrappedCentered(GuiGraphics guiGraphics, Font font, Component text, int centerX, int y, int width) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int lineY = y;

        for (FormattedCharSequence line : lines) {
            guiGraphics.drawCenteredString(font, line, centerX, lineY, TEXT_COLOR);
            lineY += font.lineHeight + 1;
        }
    }
}
