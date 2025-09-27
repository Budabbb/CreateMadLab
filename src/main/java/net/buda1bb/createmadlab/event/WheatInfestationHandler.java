package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "createmadlab", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WheatInfestationHandler {

    private static final float INFESTATION_CHANCE = 0.01f; // 1%

    @SubscribeEvent
    public static void onBlockGrow(BlockEvent.CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (state.getBlock() == Blocks.WHEAT && state.hasProperty(CropBlock.AGE)) {
            int age = state.getValue(CropBlock.AGE);

            if (age == 7) {
                if (serverLevel.random.nextFloat() < INFESTATION_CHANCE) {
                    serverLevel.setBlock(pos, ModBlocks.ERGOT_INFESTED_WHEAT.get().defaultBlockState(), 3);
                }
            }
        }
    }
}