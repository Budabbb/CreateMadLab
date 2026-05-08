package net.buda1bb.createmadlab.event;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.item.FlaskItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlaskBasinEventHandler {
    private FlaskBasinEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBasin(PlayerInteractEvent.RightClickBlock event) {
        ItemStack heldItem = event.getItemStack();
        if (!(heldItem.getItem() instanceof FlaskItem)) {
            return;
        }

        Level level = event.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof BasinBlockEntity basin)) {
            return;
        }

        List<TankTarget> targets = getBasinFluidTargets(basin);
        if (targets.isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        if (!tryPourFlaskIntoBasin(level, player, event.getHand(), heldItem, targets, basin)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static List<TankTarget> getBasinFluidTargets(BasinBlockEntity basin) {
        List<TankTarget> targets = new ArrayList<>(2);
        addTarget(targets, basin.getBehaviour(SmartFluidTankBehaviour.INPUT), false);
        addTarget(targets, basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT), true);
        return targets;
    }

    private static void addTarget(List<TankTarget> targets, SmartFluidTankBehaviour behaviour, boolean forceFill) {
        if (behaviour == null) {
            return;
        }

        LazyOptional<? extends IFluidHandler> capability = behaviour.getCapability();
        IFluidHandler handler = capability.orElse(null);
        if (handler != null) {
            targets.add(new TankTarget(handler, forceFill));
        }
    }

    private static boolean tryPourFlaskIntoBasin(Level level, Player player, InteractionHand hand,
                                                ItemStack flaskStack, List<TankTarget> targets,
                                                BasinBlockEntity basin) {
        LazyOptional<IFluidHandlerItem> flaskCapability =
                flaskStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        IFluidHandlerItem flaskHandler = flaskCapability.orElse(null);
        if (flaskHandler == null) {
            return false;
        }

        FluidStack fluidInFlask = FlaskItem.normalizeFluid(
                flaskHandler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE));
        if (fluidInFlask.isEmpty()) {
            return false;
        }

        BasinTransfer transfer = findTransferTarget(targets, fluidInFlask, true);
        if (transfer == null) {
            transfer = findTransferTarget(targets, fluidInFlask, false);
        }
        if (transfer == null) {
            return false;
        }

        if (level.isClientSide) {
            return true;
        }

        int filled = transfer.target().fill(transfer.fluid().copy(), FluidAction.EXECUTE);
        if (filled <= 0) {
            return false;
        }

        if (!player.isCreative()) {
            FluidStack drained = flaskHandler.drain(filled, FluidAction.EXECUTE);
            if (drained.getAmount() != filled) {
                FluidStack rollback = transfer.fluid().copy();
                rollback.setAmount(filled);
                transfer.target().handler().drain(rollback, FluidAction.EXECUTE);
                return false;
            }
            player.setItemInHand(hand, flaskHandler.getContainer());
        }

        basin.notifyChangeOfContents();
        basin.notifyUpdate();
        return true;
    }

    private static BasinTransfer findTransferTarget(List<TankTarget> targets, FluidStack fluidInFlask,
                                                    boolean requireExistingFluid) {
        for (TankTarget target : targets) {
            for (int tankIndex = 0; tankIndex < target.handler().getTanks(); tankIndex++) {
                FluidStack existingFluid = target.handler().getFluidInTank(tankIndex);
                if (requireExistingFluid) {
                    if (existingFluid.isEmpty()
                            || existingFluid.getFluid() != fluidInFlask.getFluid()
                            || !FlaskItem.fluidTagsMatchIgnoringEmpty(existingFluid, fluidInFlask)) {
                        continue;
                    }
                } else if (!existingFluid.isEmpty()) {
                    continue;
                }

                FluidStack transferFluid = fluidInFlask.copy();
                if (requireExistingFluid) {
                    FlaskItem.copyFluidTag(existingFluid, transferFluid);
                }

                int fillable = target.fill(transferFluid.copy(), FluidAction.SIMULATE);
                if (fillable <= 0) {
                    continue;
                }

                transferFluid.setAmount(fillable);
                return new BasinTransfer(target, transferFluid);
            }
        }

        return null;
    }

    private record BasinTransfer(TankTarget target, FluidStack fluid) {
    }

    private record TankTarget(IFluidHandler handler, boolean forceFill) {
        private int fill(FluidStack fluid, FluidAction action) {
            if (forceFill && handler instanceof SmartFluidTankBehaviour.InternalFluidHandler internalHandler) {
                return internalHandler.forceFill(fluid, action);
            }
            return handler.fill(fluid, action);
        }
    }
}
