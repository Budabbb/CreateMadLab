package net.buda1bb.createmadlab.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FlaskItem extends Item {
    private static final int CAPACITY = 100;
    private static final int BAR_WIDTH = 13;
    private static final String FLUID_TAG = FluidHandlerItemStack.FLUID_NBT_KEY;
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
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        FluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty()) {
            tooltip.add(Component.literal("Fluid: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(fluid.getDisplayName().copy().withStyle(ChatFormatting.WHITE)));
            tooltip.add(Component.literal("Amount: " + fluid.getAmount() + " / " + CAPACITY + " mB")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.GRAY));
        }
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FlaskFluidHandler(stack);
    }

    private FluidStack getFluid(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(FLUID_TAG)) {
            return normalizeFluid(FluidStack.loadFluidStackFromNBT(stack.getTag().getCompound(FLUID_TAG)));
        }
        return FluidStack.EMPTY;
    }

    public static FluidStack normalizeFluid(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }

        FluidStack normalized = fluid.copy();
        normalizeFluidTag(normalized);
        return normalized;
    }

    public static boolean fluidTagsMatchIgnoringEmpty(FluidStack first, FluidStack second) {
        CompoundTag firstTag = normalizeFluidTag(first.getTag());
        CompoundTag secondTag = normalizeFluidTag(second.getTag());
        return firstTag == null ? secondTag == null : firstTag.equals(secondTag);
    }

    public static void copyFluidTag(FluidStack from, FluidStack to) {
        CompoundTag tag = from.getTag();
        to.setTag(tag == null ? null : tag.copy());
    }

    private static void normalizeFluidTag(FluidStack fluid) {
        if (fluid.hasTag() && fluid.getTag().isEmpty()) {
            fluid.setTag(null);
        }
    }

    private static CompoundTag normalizeFluidTag(@Nullable CompoundTag tag) {
        return tag == null || tag.isEmpty() ? null : tag;
    }

    private static class FlaskFluidHandler extends FluidHandlerItemStack {
        private FlaskFluidHandler(ItemStack container) {
            super(container, CAPACITY);
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
            normalizeFluidTag(drained);

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
            normalizeFluidTag(stored);
            super.setFluid(stored);
            cleanupLegacyDamageTag();
        }

        @Override
        protected void setContainerToEmpty() {
            super.setContainerToEmpty();
            cleanupLegacyDamageTag();
        }

        private void cleanupLegacyDamageTag() {
            CompoundTag tag = container.getTag();
            if (tag == null) {
                return;
            }

            tag.remove(DAMAGE_TAG);
            if (tag.isEmpty()) {
                container.setTag(null);
            }
        }
    }
}
