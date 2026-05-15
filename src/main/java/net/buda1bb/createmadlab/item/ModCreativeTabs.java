package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.buda1bb.createmadlab.fluid.ModFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateMadLab.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATEMADLAB_TAB = CREATIVE_MODE_TABS.register("createmadlab_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.SYRINGE.get()))
                    .title(Component.translatable("creativetab.createmadlab_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.ERGOT_FUNGUS.get());
                        pOutput.accept(ModItems.ERGOT_POWDER.get());
                        pOutput.accept(ModItems.ERGOT_PAPER.get());
                        pOutput.accept(ModItems.ERGOTAMINE_TARTRATE.get());
                        pOutput.accept(ModItems.LYSERGIC_ACID.get());
                        pOutput.accept(ModItems.LSD_CRYSTAL.get());
                        pOutput.accept(ModItems.PURPLE_TRAY_ITEM.get());

                        ItemStack lsdPaper = new ItemStack(ModItems.LSD_PAPER.get());
                        LSDPaperItem.setDose(lsdPaper, 1.0D);
                        pOutput.accept(lsdPaper);

                        pOutput.accept(ModItems.SYRINGE.get());
                        pOutput.accept(ModItems.FLASK.get());
                        pOutput.accept(ModItems.CALCIUM_HYDROXIDE.get());
                        pOutput.accept(ModItems.AMMONIUM_CHLORIDE.get());
                        pOutput.accept(ModItems.CALCIUM_CHLORIDE.get());
                        pOutput.accept(ModItems.CALCIUM_CARBONATE.get());
                        pOutput.accept(ModItems.CALCIUM_SULFATE.get());
                        pOutput.accept(ModItems.SODIUM_CHLORIDE.get());
                        pOutput.accept(ModItems.SODIUM_CARBONATE.get());
                        pOutput.accept(ModItems.SODIUM_SULFATE.get());
                        pOutput.accept(ModItems.SODIUM_HYDROXIDE.get());
                        pOutput.accept(ModItems.HYDROXILAMINE.get());
                        pOutput.accept(ModItems.NPP_TRAY_ITEM.get());
                        pOutput.accept(ModItems.NPP_HYDROCHLORIDE.get());
                        pOutput.accept(ModItems.MORPHINE_BASE.get());
                        pOutput.accept(ModItems.OXYMORPHONE.get());
                        pOutput.accept(ModItems.NALOXONE_BASE.get());
                        pOutput.accept(ModItems.DIAMORPHINE.get());
                        pOutput.accept(ModItems.HEROIN.get());
                        pOutput.accept(ModItems.FENTANYL_BASE.get());
                        pOutput.accept(ModItems.FENTANYL_CITRATE.get());
                        pOutput.accept(ModItems.MORPHINE_SULFATE.get());
                        pOutput.accept(ModItems.VANADIUM_PENTOXIDE.get());
                        pOutput.accept(ModItems.VANADIUM_PENTOXIDE_CATALYST.get());
                        pOutput.accept(ModItems.IRON_SULFATE.get());
                        pOutput.accept(ModItems.IRON_SULFATE_CATALYST.get());

                        ItemStack morphineSyringe = new ItemStack(ModItems.SYRINGE.get());
                        SyringeItem.setContent(morphineSyringe, "morphine");
                        pOutput.accept(morphineSyringe);
                        ItemStack blissSyringe = new ItemStack(ModItems.SYRINGE.get());
                        SyringeItem.setContent(blissSyringe, "bliss");
                        pOutput.accept(blissSyringe);
                        ItemStack voidSyringe = new ItemStack(ModItems.SYRINGE.get());
                        SyringeItem.setContent(voidSyringe, "void");
                        pOutput.accept(voidSyringe);
                        ItemStack naloxoneSyringe = new ItemStack(ModItems.SYRINGE.get());
                        SyringeItem.setContent(naloxoneSyringe, "naloxone");
                        pOutput.accept(naloxoneSyringe);

                        pOutput.accept(ModFluids.PURIFIED_ERGOT_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.LYSERGIC_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.LIQUID_LSD_BUCKET.get());
                        pOutput.accept(ModFluids.LSD_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.CHLOROFORM_BUCKET.get());
                        pOutput.accept(ModFluids.ACETONE_BUCKET.get());
                        pOutput.accept(ModFluids.HYDROGEN_PEROXIDE_BUCKET.get());
                        pOutput.accept(ModFluids.ANHYDROUS_HYDRAZINE_BUCKET.get());
                        pOutput.accept(ModFluids.ERGOT_EXTRACT_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.CHLORINE_BUCKET.get());
                        pOutput.accept(ModFluids.SLUDGE_BUCKET.get());
                        pOutput.accept(ModFluids.RESIDUE_BUCKET.get());
                        pOutput.accept(ModFluids.DIETHYL_ETHER_BUCKET.get());
                        pOutput.accept(ModFluids.HYDROCHLORIC_ACID_BUCKET.get());
                        pOutput.accept(ModFluids.CITRIC_ACID_BUCKET.get());
                        pOutput.accept(ModFluids.OPIUM_LATEX_BUCKET.get());
                        pOutput.accept(ModFluids.HEROIN_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.MORPHINE_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.FENTANYL_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.SULFUR_TRIOXIDE_BUCKET.get());
                        pOutput.accept(ModFluids.SULFURIC_ACID_BUCKET.get());
                        pOutput.accept(ModFluids.ETHANOL_BUCKET.get());
                        pOutput.accept(ModFluids.ETHYL_CHLORIDE_BUCKET.get());
                        pOutput.accept(ModFluids.ALLYL_CHLORIDE_BUCKET.get());
                        pOutput.accept(ModFluids.DIETHYLAMINE_BUCKET.get());
                        pOutput.accept(ModFluids.PHENETHYLAMINE_BUCKET.get());
                        pOutput.accept(ModFluids.FOUR_PIPERIDONE_BUCKET.get());
                        pOutput.accept(ModFluids.NPP_HYDROCHLORIDE_SOLUTION_BUCKET.get());
                        pOutput.accept(ModFluids.NALOXONE_SOLUTION_BUCKET.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
