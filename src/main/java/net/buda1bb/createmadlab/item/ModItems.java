package net.buda1bb.createmadlab.item;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.core.registries.Registries;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;


import java.util.List;

import static net.buda1bb.createmadlab.CreateMadLab.REGISTRATE;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, CreateMadLab.MOD_ID);


    public static final DeferredHolder<Item, Item> ERGOT_FUNGUS =
            ITEMS.register("ergot_fungus", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ERGOT_POWDER =
            ITEMS.register("ergot_powder", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ERGOTAMINE_TARTRATE =
            ITEMS.register("ergotamine_tartrate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LYSERGIC_ACID =
            ITEMS.register("lysergic_acid", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LSD_CRYSTAL =
            ITEMS.register("lsd_crystal", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> PURPLE_TRAY_ITEM = ITEMS.register("purple_tray",
            () -> new BlockItem(ModBlocks.PURPLE_TRAY.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> NPP_TRAY_ITEM = ITEMS.register("npp_tray",
            () -> new BlockItem(ModBlocks.NPP_TRAY.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> ERGOT_PAPER =
            ITEMS.register("ergot_paper", () -> new Item(new Item.Properties()));

    public static final ItemEntry<LSDPaperItem> LSD_PAPER =
            REGISTRATE.item("lsd_paper", LSDPaperItem::new)
                    .properties(p -> p.stacksTo(16))
                    .register();

    public static final ItemEntry<SyringeItem> SYRINGE =
            REGISTRATE.item("syringe", SyringeItem::new)
                    .properties(p -> p.stacksTo(1))
                    .register();


    public static final ItemEntry<FlaskItem> FLASK =
            REGISTRATE.item("flask", FlaskItem::new)
                    .properties(p -> p.stacksTo(1))
                    .register();

    public static final DeferredHolder<Item, Item> CALCIUM_HYDROXIDE =
            ITEMS.register("calcium_hydroxide", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> AMMONIUM_CHLORIDE =
            ITEMS.register("ammonium_chloride", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> CALCIUM_CHLORIDE =
            ITEMS.register("calcium_chloride", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> CALCIUM_CARBONATE =
            ITEMS.register("calcium_carbonate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> CALCIUM_SULFATE =
            ITEMS.register("calcium_sulfate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SODIUM_CHLORIDE =
            ITEMS.register("sodium_chloride", () -> new Item(new Item.Properties().food(ModConsumables.SODIUM_CHLORIDE)));

    public static final DeferredHolder<Item, Item> SODIUM_CARBONATE =
            ITEMS.register("sodium_carbonate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SODIUM_SULFATE =
            ITEMS.register("sodium_sulfate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SODIUM_HYDROXIDE =
            ITEMS.register("sodium_hydroxide", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> HYDROXILAMINE =
            ITEMS.register("hydroxilamine", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> NPP_HYDROCHLORIDE =
            ITEMS.register("npp_hydrochloride", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> MORPHINE_BASE =
            ITEMS.register("morphine_base", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> OXYMORPHONE =
            ITEMS.register("oxymorphone", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> NALOXONE_BASE =
            ITEMS.register("naloxone_base", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> DIAMORPHINE =
            ITEMS.register("diamorphine", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> HEROIN =
            ITEMS.register("heroin", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> FENTANYL_BASE =
            ITEMS.register("fentanyl_base", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> FENTANYL_CITRATE =
            ITEMS.register("fentanyl_citrate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> MORPHINE_SULFATE =
            ITEMS.register("morphine_sulfate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> VANADIUM_PENTOXIDE =
            ITEMS.register("vanadium_pentoxide", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> VANADIUM_PENTOXIDE_CATALYST =
            ITEMS.register("vanadium_pentoxide_catalyst", () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.literal("Not consumed on use.").withStyle(ChatFormatting.DARK_GRAY));
                    super.appendHoverText(stack, context, tooltip, flag);
                }
            });

    public static final DeferredHolder<Item, Item> IRON_SULFATE =
            ITEMS.register("iron_sulfate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> IRON_SULFATE_CATALYST =
            ITEMS.register("iron_sulfate_catalyst", () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.literal("Not consumed on use.").withStyle(ChatFormatting.DARK_GRAY));
                    super.appendHoverText(stack, context, tooltip, flag);
                }
            });
}
