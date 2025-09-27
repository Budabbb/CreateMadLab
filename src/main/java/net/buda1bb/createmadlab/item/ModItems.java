package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreateMadLab.MOD_ID);


    public static final RegistryObject<Item> ERGOT_FUNGUS =
            ITEMS.register("ergot_fungus", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ERGOT_POWDER =
            ITEMS.register("ergot_powder", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PHOSPHORUS_PENTOXIDE =
            ITEMS.register("phosphorus_pentoxide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ERGOTAMINE_TARTRATE =
            ITEMS.register("ergotamine_tartrate", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LYSERGIC_ACID =
            ITEMS.register("lysergic_acid", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LSD_CRYSTAL =
            ITEMS.register("lsd_crystal", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ERGOT_PAPER =
            ITEMS.register("ergot_paper", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LSD_PAPER =
            ITEMS.register("lsd_paper", () -> new Item(new Item.Properties()));
}
