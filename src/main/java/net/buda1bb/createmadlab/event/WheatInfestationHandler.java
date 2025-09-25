package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "createmadlab", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WheatInfestationHandler {

    private static final float INFESTATION_CHANCE = 0.05f; // 5% chance per tick

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        Level level = event.level;
        if (level.isClientSide) return;
        if (event.phase != TickEvent.Phase.START) return;

        if (!(level instanceof ServerLevel serverLevel)) return;

        for (Player player : serverLevel.players()) {
            BlockPos playerPos = player.blockPosition(); // fixed for Parchment
            int dx = serverLevel.random.nextInt(16) - 8;
            int dz = serverLevel.random.nextInt(16) - 8;
            BlockPos pos = playerPos.offset(dx, 0, dz);

            BlockState state = level.getBlockState(pos);

            if (state.getBlock() == Blocks.WHEAT && state.hasProperty(CropBlock.AGE)) {
                int age = state.getValue(CropBlock.AGE);
                if (age >= 7 && serverLevel.random.nextFloat() < INFESTATION_CHANCE) {
                    level.setBlock(pos, ModBlocks.ERGOT_INFESTED_WHEAT.get().defaultBlockState(), 3);
                    System.out.println("Wheat at " + pos + " turned into ergot-infested wheat!");
                }
            }
        }
    }
}
