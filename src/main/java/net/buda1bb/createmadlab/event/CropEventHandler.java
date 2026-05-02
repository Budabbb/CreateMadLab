package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CropEventHandler {

    @SubscribeEvent
    public static void onAnyBlockEvent(BlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BlockState state = serverLevel.getBlockState(pos);
        ResourceLocation ergotWheatId = new ResourceLocation("createmadlab", "ergot_infested_wheat");
        var ergotWheatBlock = ForgeRegistries.BLOCKS.getValue(ergotWheatId);

        if (ergotWheatBlock == null) return;

        if (state.getBlock() == ergotWheatBlock) {
            try {
                var agePropertyField = ergotWheatBlock.getClass().getDeclaredField("AGE");
                if (agePropertyField != null) {
                    var ageProperty = (net.minecraft.world.level.block.state.properties.IntegerProperty) agePropertyField.get(null);

                    if (state.hasProperty(ageProperty) && state.getValue(ageProperty) == 0) {
                        serverLevel.setBlock(pos, Blocks.WHEAT.defaultBlockState()
                                .setValue(CropBlock.AGE, 0), 3);
                    }
                }
            } catch (Exception e) {
                if (state.getBlock() == ergotWheatBlock) {
                    serverLevel.setBlock(pos, Blocks.WHEAT.defaultBlockState()
                            .setValue(CropBlock.AGE, 0), 3);
                }
            }
        }
    }
}