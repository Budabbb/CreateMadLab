package net.buda1bb.createmadlab;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.Registrate;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.buda1bb.createmadlab.client.ShaderpackExtractor;
import net.buda1bb.createmadlab.fluid.ModFluids;
import net.buda1bb.createmadlab.item.ModCreativeTabs;
import net.buda1bb.createmadlab.item.ModItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateMadLab.MOD_ID)
public class CreateMadLab {
    public static final String MOD_ID = "createmadlab";
    public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

    public CreateMadLab(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModFluids.register(REGISTRATE);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ShaderpackExtractor.extractShaderpackStructure();
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ERGOT_INFESTED_WHEAT.get(), RenderType.cutout());
        }
    }
}