package net.buda1bb.createmadlab.block;

import com.jetpacker06.CreateBrokenBad.block.TrayBlock;
import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CreateMadLab.MOD_ID);

    public static final RegistryObject<Block> ERGOT_INFESTED_WHEAT =
            BLOCKS.register("ergot_infested_wheat", () -> new ErgotInfestedWheatBlock());

    public static final RegistryObject<Block> PURPLE_TRAY = BLOCKS.register("purple_tray",
            () -> new TrayBlock.White(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> blockSupplier) {
        return BLOCKS.register(name, blockSupplier);
    }
}