package net.buda1bb.createmadlab.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
        float fillRatio = (float) fluid.getAmount() / CAPACITY;
        return Math.round(13.0F * fillRatio);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Always return blue color for the durability bar
        return 0xFF00BFFF; // Light blue color
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        FluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty()) {
            // Show what fluid is in the flask
            tooltip.add(Component.literal("§7Fluid: §f" + fluid.getDisplayName().getString()));
            tooltip.add(Component.literal("§8Amount: §7" + fluid.getAmount() + " / 100 mB"));
        } else {
            tooltip.add(Component.literal("§7Empty"));
        }
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, CAPACITY) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return true;
            }

            @Override
            protected void setFluid(FluidStack fluid) {
                super.setFluid(fluid);
                updateDamage();
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                int filled = super.fill(resource, action);
                if (action.execute()) {
                    updateDamage();
                }
                return filled;
            }

            @NotNull
            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                FluidStack drained = super.drain(maxDrain, action);
                if (action.execute()) {
                    updateDamage();
                }
                return drained;
            }

            private void updateDamage() {
                ItemStack container = getContainer();
                int amount = getFluid().getAmount();
                int damage = 100 - (100 * amount / CAPACITY);
                container.setDamageValue(damage);
            }
        };
    }

    private FluidStack getFluid(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("Fluid")) {
            return FluidStack.loadFluidStackFromNBT(stack.getTag().getCompound("Fluid"));
        }
        return FluidStack.EMPTY;
    }
}