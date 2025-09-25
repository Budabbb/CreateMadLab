package net.buda1bb.createmadlab.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ErgotInfestedWheatBlock extends Block {
    public ErgotInfestedWheatBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.WHEAT));
    }
}
