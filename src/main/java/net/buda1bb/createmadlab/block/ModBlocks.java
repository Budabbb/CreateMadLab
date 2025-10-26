package net.buda1bb.createmadlab.block;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CreateMadLab.MOD_ID);

    public static final RegistryObject<Block> ERGOT_INFESTED_WHEAT =
            BLOCKS.register("ergot_infested_wheat", () -> new ErgotInfestedWheatBlock());


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> blockSupplier) {
        return BLOCKS.register(name, blockSupplier);
    }
}