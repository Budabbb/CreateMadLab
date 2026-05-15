package net.buda1bb.createmadlab;

import com.tterrag.registrate.Registrate;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.buda1bb.createmadlab.fluid.ModFluids;
import net.buda1bb.createmadlab.item.FlaskItem;
import net.buda1bb.createmadlab.item.ModCreativeTabs;
import net.buda1bb.createmadlab.item.ModItems;
import net.buda1bb.createmadlab.item.SyringeItem;
import net.buda1bb.createmadlab.network.ModMessages;
import net.buda1bb.createmadlab.recipes.ModRecipeSerializers;
import net.buda1bb.createmadlab.util.ModDataComponents;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(CreateMadLab.MOD_ID)
public class CreateMadLab {
    public static final String MOD_ID = "createmadlab";
    public static final Registrate REGISTRATE = Registrate.create(MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    private static final Logger LOGGER = LogUtils.getLogger();

    public CreateMadLab(IEventBus modEventBus) {
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModFluids.register(REGISTRATE);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModMessages::register);
        modEventBus.addListener(FlaskItem::registerCapabilities);
        ModRecipeSerializers.SERIALIZERS.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.ERGOT_INFESTED_WHEAT.get(), RenderType.cutout());

                ItemProperties.register(ModItems.SYRINGE.get(),
                        ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "content"),
                        (stack, level, entity, seed) -> {
                            if (SyringeItem.usesFilledTexture(stack)) {
                                return 1.0F;
                            }
                            return 0.0F;
                        });
            });
        }
    }
}

// ngl at this point this entire mod is mostly vibecoded
