package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateMadLab.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CREATEMADLAB_TAB = CREATIVE_MODE_TABS.register("createmadlab_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(Items.BOOK))
                    .title(Component.translatable("creativetab.createmadlab_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.ERGOT_FUNGUS.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
