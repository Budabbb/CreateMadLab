package net.buda1bb.createmadlab.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BasinBlockEntity.class)
public abstract class BasinBlockEntityMixin {
    @Unique
    private static final int CREATEMADLAB_BASIN_FLUID_TANKS = 4;

    @ModifyArg(
            method = "addBehaviours",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/fluid/SmartFluidTankBehaviour;<init>(Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;IIZ)V"
            ),
            index = 2,
            require = 2
    )
    private int createmadlab$expandBasinFluidTankCount(int tanks) {
        return Math.max(tanks, CREATEMADLAB_BASIN_FLUID_TANKS);
    }
}
