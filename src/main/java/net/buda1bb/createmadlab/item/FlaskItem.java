package net.buda1bb.createmadlab.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.buda1bb.createmadlab.util.ItemDataUtils;
import net.buda1bb.createmadlab.util.ModDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FlaskItem extends Item {
    private static final int CAPACITY = 100;
    private static final int BAR_WIDTH = 13;
    private static final String DAMAGE_TAG = "Damage";

    public FlaskItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .setNoRepair()
        );
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !getFluid(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        int amount = Mth.clamp(fluid.getAmount(), 0, CAPACITY);
        return Math.round((float) BAR_WIDTH * amount / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00BFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        FluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty()) {
            tooltip.add(Component.literal("Fluid: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(fluid.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
            tooltip.add(Component.literal("Amount: " + fluid.getAmount() + " / " + CAPACITY + " mB")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FlaskFluidHandler(stack), ModItems.FLASK.get());
    }

    private FluidStack getFluid(ItemStack stack) {
        return normalizeFluid(stack.getOrDefault(ModDataComponents.FLASK_FLUID.get(), SimpleFluidContent.EMPTY).copy());
    }

    public static FluidStack normalizeFluid(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }

        return fluid.copy();
    }

    public static boolean fluidTagsMatchIgnoringEmpty(FluidStack first, FluidStack second) {
        return FluidStack.isSameFluidSameComponents(first, second);
    }

    public static void copyFluidTag(FluidStack from, FluidStack to) {
        to.applyComponents(from.getComponentsPatch());
    }

    private static class FlaskFluidHandler extends FluidHandlerItemStack {
        private FlaskFluidHandler(ItemStack container) {
            super(ModDataComponents.FLASK_FLUID, container, CAPACITY);
        }

        @NotNull
        @Override
        public FluidStack getFluid() {
            return normalizeFluid(super.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (container.getCount() != 1 || resource.isEmpty() || !canFillFluidType(resource)) {
                return 0;
            }

            FluidStack contained = getFluid();
            if (contained.isEmpty()) {
                int fillAmount = Math.min(CAPACITY, resource.getAmount());
                if (action.execute() && fillAmount > 0) {
                    FluidStack filled = resource.copy();
                    filled.setAmount(fillAmount);
                    setFluid(filled);
                }
                return fillAmount;
            }

            if (contained.getFluid() != resource.getFluid() || !fluidTagsMatchIgnoringEmpty(contained, resource)) {
                return 0;
            }

            int fillAmount = Math.min(CAPACITY - contained.getAmount(), resource.getAmount());
            if (action.execute() && fillAmount > 0) {
                contained.grow(fillAmount);
                setFluid(contained);
            }

            return fillAmount;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack contained = getFluid();
            if (container.getCount() != 1
                    || resource.isEmpty()
                    || contained.isEmpty()
                    || resource.getFluid() != contained.getFluid()
                    || !fluidTagsMatchIgnoringEmpty(contained, resource)) {
                return FluidStack.EMPTY;
            }

            return drain(resource.getAmount(), action);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (container.getCount() != 1 || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }

            FluidStack contained = getFluid();
            if (contained.isEmpty() || !canDrainFluidType(contained)) {
                return FluidStack.EMPTY;
            }

            int drainAmount = Math.min(contained.getAmount(), maxDrain);
            FluidStack drained = contained.copy();
            drained.setAmount(drainAmount);

            if (action.execute()) {
                contained.shrink(drainAmount);
                if (contained.isEmpty()) {
                    setContainerToEmpty();
                } else {
                    setFluid(contained);
                }
            }

            return drained;
        }

        @Override
        protected void setFluid(FluidStack fluid) {
            if (fluid.isEmpty()) {
                setContainerToEmpty();
                return;
            }

            FluidStack stored = fluid.copy();
            stored.setAmount(Mth.clamp(stored.getAmount(), 0, CAPACITY));
            if (stored.isEmpty()) {
                setContainerToEmpty();
                return;
            }
            super.setFluid(stored);
            cleanupLegacyDamageTag();
        }

        @Override
        protected void setContainerToEmpty() {
            super.setContainerToEmpty();
            cleanupLegacyDamageTag();
        }

        private void cleanupLegacyDamageTag() {
            CompoundTag tag = ItemDataUtils.getTagCopy(container);
            if (tag == null) {
                return;
            }

            tag.remove(DAMAGE_TAG);
            container.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (!tag.isEmpty()) {
                ItemDataUtils.update(container, customData -> customData.merge(tag));
            }
        }
    }
}
