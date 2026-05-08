package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.block.ErgotInfestedWheatBlock;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CropEventHandler {

    @SubscribeEvent
    public static void onErgotPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        if (state.getBlock() == ModBlocks.ERGOT_INFESTED_WHEAT.get()
                && state.hasProperty(ErgotInfestedWheatBlock.AGE)
                && state.getValue(ErgotInfestedWheatBlock.AGE) == 0) {
            serverLevel.setBlock(pos, Blocks.WHEAT.defaultBlockState()
                    .setValue(CropBlock.AGE, 0), 3);
        }
    }
}
