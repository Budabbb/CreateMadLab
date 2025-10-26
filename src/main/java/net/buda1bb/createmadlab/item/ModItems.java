package net.buda1bb.createmadlab.item;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static net.buda1bb.createmadlab.CreateMadLab.REGISTRATE;

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

    public static final ItemEntry<LSDPaperItem> LSD_PAPER =
            REGISTRATE.item("lsd_paper", LSDPaperItem::new)
                    .properties(p -> p.stacksTo(16))
                    .register();

    public static final ItemEntry<SyringeItem> SYRINGE =
            REGISTRATE.item("syringe", SyringeItem::new)
                    .properties(p -> p.stacksTo(1))
                    .register();

    public static final RegistryObject<Item> CALCIUM_HYDROXIDE =
            ITEMS.register("calcium_hydroxide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> AMMONIUM_CHLORIDE =
            ITEMS.register("ammonium_chloride", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SODIUM_CARBONATE =
            ITEMS.register("sodium_carbonate", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MORPHINE_BASE =
            ITEMS.register("morphine_base", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DIAMORPHINE =
            ITEMS.register("diamorphine", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HEROIN =
            ITEMS.register("heroin", () -> new Item(new Item.Properties()));
}
