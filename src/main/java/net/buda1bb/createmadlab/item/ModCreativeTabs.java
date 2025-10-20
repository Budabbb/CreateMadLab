package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.fluid.ModFluids;
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
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.LSD_PAPER.get()))
                    .title(Component.translatable("creativetab.createmadlab_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.ERGOT_FUNGUS.get());
                        pOutput.accept(ModItems.ERGOT_POWDER.get());

                        pOutput.accept(ModItems.PHOSPHORUS_PENTOXIDE.get());
                        pOutput.accept(ModItems.ERGOTAMINE_TARTRATE.get());

                        pOutput.accept(ModItems.LYSERGIC_ACID.get());
                        pOutput.accept(ModItems.LSD_CRYSTAL.get());

                        pOutput.accept(ModItems.ERGOT_PAPER.get());
                        pOutput.accept(ModItems.LSD_PAPER.get());

                        pOutput.accept(ModFluids.CHLOROFORM_BUCKET.get());
                        pOutput.accept(ModFluids.LYSERGIC_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.LIQUID_LSD_BUCKET.get());
                        pOutput.accept(ModFluids.PURIFIED_ERGOT_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.ACETONE_BUCKET.get());
                        pOutput.accept(ModFluids.ANHYDROUS_HYDRAZINE_BUCKET.get());
                        pOutput.accept(ModFluids.ERGOT_EXTRACT_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.CHLORINE_BUCKET.get());
                        pOutput.accept(ModFluids.SLUDGE_BUCKET.get());
                        pOutput.accept(ModFluids.RESIDUE_BUCKET.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
