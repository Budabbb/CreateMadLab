package net.buda1bb.createmadlab;

import com.tterrag.registrate.Registrate;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.buda1bb.createmadlab.fluid.ModFluids;
import net.buda1bb.createmadlab.item.ModCreativeTabs;
import net.buda1bb.createmadlab.item.ModItems;
import net.buda1bb.createmadlab.item.SyringeItem;
import net.buda1bb.createmadlab.recipes.ModRecipeSerializers;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(CreateMadLab.MOD_ID)
public class CreateMadLab {
    public static final String MOD_ID = "createmadlab";
    public static final Registrate REGISTRATE = Registrate.create(MOD_ID);
    private static final Logger LOGGER = LogUtils.getLogger();

    public CreateMadLab(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModFluids.register(REGISTRATE);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        ModRecipeSerializers.SERIALIZERS.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Server-safe initialization
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            try {
                Class<?> extractorClass = Class.forName("net.buda1bb.createmadlab.client.ShaderpackExtractor");
                java.lang.reflect.Method method = extractorClass.getMethod("extractShaderpackStructure");
                method.invoke(null);
            } catch (Exception e) {
            }

            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.ERGOT_INFESTED_WHEAT.get(), RenderType.cutout());

                ItemProperties.register(ModItems.SYRINGE.get(),
                        new ResourceLocation(CreateMadLab.MOD_ID, "content"),
                        (stack, level, entity, seed) -> {
                            if (SyringeItem.hasContent(stack)) {
                                String content = SyringeItem.getContent(stack);
                                if ("morphine".equals(content) || "bliss".equals(content)) {
                                    return 1.0F;
                                }
                            }
                            return 0.0F;
                        });
            });
        }
    }
}