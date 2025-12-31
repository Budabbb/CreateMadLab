package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.buda1bb.createmadlab.block.ErgotInfestedWheatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CropEventHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;

        // Every 5 ticks
        if (tickCounter >= 5) {
            tickCounter = 0;

            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                if (player.level() instanceof ServerLevel level) {
                    checkAreaAroundPlayer(level, player);
                }
            }
        }
    }

    private static void checkAreaAroundPlayer(ServerLevel level, ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();

        int radius = 8;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos surfacePos = level.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        playerPos.offset(dx, 0, dz)
                );

                for (int dy = 0; dy >= -3; dy--) {
                    BlockPos checkPos = surfacePos.offset(0, dy, 0);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.getBlock() == ModBlocks.ERGOT_INFESTED_WHEAT.get() &&
                            state.hasProperty(ErgotInfestedWheatBlock.AGE) &&
                            state.getValue(ErgotInfestedWheatBlock.AGE) == 0) {

                        level.setBlock(checkPos, Blocks.WHEAT.defaultBlockState()
                                .setValue(CropBlock.AGE, 0), 3);
                        break;
                    }
                }
            }
        }
    }
}