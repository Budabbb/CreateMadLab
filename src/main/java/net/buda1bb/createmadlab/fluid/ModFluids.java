package net.buda1bb.createmadlab.fluid;

import com.simibubi.create.AllFluids;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.ForgeFlowingFluid;

public class ModFluids {
    public static Registrate REGISTRATE;

    public static ResourceLocation still = new ResourceLocation("block/water_still");
    public static ResourceLocation flow = new ResourceLocation("block/water_flow");

    // chloroform
    public static FluidEntry<ForgeFlowingFluid.Flowing> CHLOROFORM;
    public static ItemEntry<BucketItem> CHLOROFORM_BUCKET;

    // new fluids
    public static FluidEntry<ForgeFlowingFluid.Flowing> ANHYDROUS_HYDRAZINE;
    public static ItemEntry<BucketItem> ANHYDROUS_HYDRAZINE_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> ACETONE;
    public static ItemEntry<BucketItem> ACETONE_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> PURIFIED_ERGOT_SOLUTION;
    public static ItemEntry<BucketItem> PURIFIED_ERGOT_SOLUTION_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> LYSERGIC_SOLUTION;
    public static ItemEntry<BucketItem> LYSERGIC_SOLUTION_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> LIQUID_LSD;
    public static ItemEntry<BucketItem> LIQUID_LSD_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> LSD_SOLUTION;
    public static ItemEntry<BucketItem> LSD_SOLUTION_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> ERGOT_EXTRACT_SOLUTION;
    public static ItemEntry<BucketItem> ERGOT_EXTRACT_SOLUTION_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> SLUDGE;
    public static ItemEntry<BucketItem> SLUDGE_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> RESIDUE;
    public static ItemEntry<BucketItem> RESIDUE_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> CHLORINE;
    public static ItemEntry<BucketItem> CHLORINE_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> DIETHYL_ETHER;
    public static ItemEntry<BucketItem> DIETHYL_ETHER_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> HYDROCHLORIC_ACID;
    public static ItemEntry<BucketItem> HYDROCHLORIC_ACID_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> OPIUM_LATEX;
    public static ItemEntry<BucketItem> OPIUM_LATEX_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> HEROIN_SOLUTION;
    public static ItemEntry<BucketItem> HEROIN_SOLUTION_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> MORPHINE_SOLUTION;
    public static ItemEntry<BucketItem> MORPHINE_SOLUTION_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> SULFUR_TRIOXIDE;
    public static ItemEntry<BucketItem> SULFUR_TRIOXIDE_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> SULFURIC_ACID;
    public static ItemEntry<BucketItem> SULFURIC_ACID_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> ETHANOL;
    public static ItemEntry<BucketItem> ETHANOL_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> ETHYL_CHLORIDE;
    public static ItemEntry<BucketItem> ETHYL_CHLORIDE_BUCKET;

    public static FluidEntry<ForgeFlowingFluid.Flowing> DIETHYLAMINE;
    public static ItemEntry<BucketItem> DIETHYLAMINE_BUCKET;

    public static FluidBuilder<ForgeFlowingFluid.Flowing, Registrate> basicFluid(String name) {
        return basicFluid(name, 0xffffffff);
    }

    public static FluidBuilder<ForgeFlowingFluid.Flowing, Registrate> basicFluid(String name, int color) {
        return REGISTRATE.fluid(name, still, flow, (p, r1, r2) -> new NoColorFluidAttributes(p, color))
                .properties(p -> p.viscosity(500).density(500))
                .fluidProperties(p -> p.tickRate(5).slopeFindDistance(6).explosionResistance(100f))
                .source(ForgeFlowingFluid.Source::new);
    }

    public static ItemEntry<BucketItem> getBucket(FluidBuilder<ForgeFlowingFluid.Flowing, Registrate> fluid) {
        return fluid.bucket().properties(p -> p.stacksTo(1)).register();
    }

    public static void register(Registrate registrate) {
        REGISTRATE = registrate;

        var fChl = basicFluid("chloroform", 0xffefffff);
        CHLOROFORM_BUCKET = getBucket(fChl);
        CHLOROFORM = fChl.register();

        var fHyd = basicFluid("anhydrous_hydrazine", 0xfff4fff4);
        ANHYDROUS_HYDRAZINE_BUCKET = getBucket(fHyd);
        ANHYDROUS_HYDRAZINE = fHyd.register();

        var fAcetone = basicFluid("acetone", 0xffefffff);
        ACETONE_BUCKET = getBucket(fAcetone);
        ACETONE = fAcetone.register();

        var fPurErgot = basicFluid("purified_ergot_solution", 0xffbeb4ce);
        PURIFIED_ERGOT_SOLUTION_BUCKET = getBucket(fPurErgot);
        PURIFIED_ERGOT_SOLUTION = fPurErgot.register();

        var fLysergicSol = basicFluid("lysergic_solution", 0xffcdbbe2);
        LYSERGIC_SOLUTION_BUCKET = getBucket(fLysergicSol);
        LYSERGIC_SOLUTION = fLysergicSol.register();

        var fLSD = basicFluid("liquid_lsd", 0xff905a80);
        LIQUID_LSD_BUCKET = getBucket(fLSD);
        LIQUID_LSD = fLSD.register();

        var fLSDSol = basicFluid("lsd_solution", 0xff946686);
        LSD_SOLUTION_BUCKET = getBucket(fLSDSol);
        LSD_SOLUTION = fLSDSol.register();

        var fErgotExtract = basicFluid("ergot_extract_solution", 0xff2c2506);
        ERGOT_EXTRACT_SOLUTION_BUCKET = getBucket(fErgotExtract);
        ERGOT_EXTRACT_SOLUTION = fErgotExtract.register();

        var fChlorine = basicFluid("chlorine", 0xffd8e95b);
        CHLORINE_BUCKET = getBucket(fChlorine);
        CHLORINE = fChlorine.register();

        var fSludge = basicFluid("sludge", 0xff574226);
        SLUDGE_BUCKET = getBucket(fSludge);
        SLUDGE = fSludge.register();

        var fResidue = basicFluid("residue", 0xffffffff);
        RESIDUE_BUCKET = getBucket(fResidue);
        RESIDUE = fResidue.register();

        var fDietyhlEther = basicFluid("diethyl_ether", 0xffefffff);
        DIETHYL_ETHER_BUCKET = getBucket(fDietyhlEther);
        DIETHYL_ETHER = fDietyhlEther.register();

        var fHydA = basicFluid("hydrochloric_acid", 0xfff6f6f6);
        HYDROCHLORIC_ACID_BUCKET = getBucket(fHydA);
        HYDROCHLORIC_ACID = fHydA.register();

        var fOpLatex = basicFluid("opium_latex", 0xff6b4a2b);
        OPIUM_LATEX_BUCKET = getBucket(fOpLatex);
        OPIUM_LATEX = fOpLatex.register();

        var fHerSol = basicFluid("heroin_solution", 0xffffffff);
        HEROIN_SOLUTION_BUCKET = getBucket(fHerSol);
        HEROIN_SOLUTION = fHerSol.register();

        var fMorSol = basicFluid("morphine_solution", 0xffffffff);
        MORPHINE_SOLUTION_BUCKET = getBucket(fMorSol);
        MORPHINE_SOLUTION = fMorSol.register();

        var fSulfTri = basicFluid("sulfur_trioxide", 0xfff2db5a);
        SULFUR_TRIOXIDE_BUCKET = getBucket(fSulfTri);
        SULFUR_TRIOXIDE = fSulfTri.register();

        var fSulfA = basicFluid("sulfuric_acid", 0xfff8f8e0);
        SULFURIC_ACID_BUCKET = getBucket(fSulfA);
        SULFURIC_ACID = fSulfA.register();

        var fEth = basicFluid("ethanol", 0xfff8f8e0);
        ETHANOL_BUCKET = getBucket(fEth);
        ETHANOL = fEth.register();

        var fEthChl = basicFluid("ethyl_chloride", 0xfff8f8e0);
        ETHYL_CHLORIDE_BUCKET = getBucket(fEthChl);
        ETHYL_CHLORIDE = fEthChl.register();

        var fDieth = basicFluid("diethylamine", 0xfff8f8e0);
        DIETHYLAMINE_BUCKET = getBucket(fDieth);
        DIETHYLAMINE = fDieth.register();
    }

    public static class NoColorFluidAttributes extends AllFluids.TintedFluidType {
        private final int color;
        public NoColorFluidAttributes(Properties properties, int color) {
            super(properties, still, flow);
            this.color = color;
        }

        @Override
        protected int getTintColor(FluidStack stack) {
            return color;
        }

        @Override
        public int getTintColor(FluidState state, BlockAndTintGetter world, BlockPos pos) {
            return color;
        }

    }
}