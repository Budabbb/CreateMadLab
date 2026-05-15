package net.buda1bb.createmadlab.block;

import com.jetpacker06.CreateBrokenBad.block.TrayBlock;
import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CreateMadLab.MOD_ID);

    public static final DeferredHolder<Block, Block> ERGOT_INFESTED_WHEAT =
            BLOCKS.register("ergot_infested_wheat", () -> new ErgotInfestedWheatBlock());

    public static final DeferredHolder<Block, Block> PURPLE_TRAY = BLOCKS.register("purple_tray",
            () -> new TrayBlock.White(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).destroyTime(1).noOcclusion()));

    public static final DeferredHolder<Block, Block> NPP_TRAY = BLOCKS.register("npp_tray",
            () -> new TrayBlock.White(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).destroyTime(1).noOcclusion()));


    private static <T extends Block> DeferredHolder<Block, T> registerBlock(String name, Supplier<T> blockSupplier) {
        return BLOCKS.register(name, blockSupplier);
    }
}