package net.buda1bb.createmadlab.item;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

import java.util.List;

import static net.buda1bb.createmadlab.CreateMadLab.REGISTRATE;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreateMadLab.MOD_ID);


    public static final RegistryObject<Item> ERGOT_FUNGUS =
            ITEMS.register("ergot_fungus", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ERGOT_POWDER =
            ITEMS.register("ergot_powder", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ERGOTAMINE_TARTRATE =
            ITEMS.register("ergotamine_tartrate", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LYSERGIC_ACID =
            ITEMS.register("lysergic_acid", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LSD_CRYSTAL =
            ITEMS.register("lsd_crystal", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PURPLE_TRAY_ITEM = ITEMS.register("purple_tray",
            () -> new BlockItem(ModBlocks.PURPLE_TRAY.get(), new Item.Properties()));

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

    public static final RegistryObject<Item> SEA_SALT =
            ITEMS.register("sea_salt", () -> new Item(new Item.Properties().food(ModConsumables.SEA_SALT)));

    public static final RegistryObject<Item> CALCIUM_HYDROXIDE =
            ITEMS.register("calcium_hydroxide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> AMMONIUM_CHLORIDE =
            ITEMS.register("ammonium_chloride", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CALCIUM_CHLORIDE =
            ITEMS.register("calcium_chloride", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SODIUM_CARBONATE =
            ITEMS.register("sodium_carbonate", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SODIUM_SULFATE =
            ITEMS.register("sodium_sulfate", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MORPHINE_BASE =
            ITEMS.register("morphine_base", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DIAMORPHINE =
            ITEMS.register("diamorphine", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HEROIN =
            ITEMS.register("heroin", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MORPHINE_SULFATE =
            ITEMS.register("morphine_sulfate", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> VANADIUM_PENTOXIDE =
            ITEMS.register("vanadium_pentoxide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> VANADIUM_PENTOXIDE_CATALYST =
            ITEMS.register("vanadium_pentoxide_catalyst", () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.literal("Not consumed on use.").withStyle(ChatFormatting.DARK_GRAY));
                    super.appendHoverText(stack, level, tooltip, flag);
                }
            });
}
