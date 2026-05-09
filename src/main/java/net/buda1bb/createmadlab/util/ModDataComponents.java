package net.buda1bb.createmadlab.util;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateMadLab.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLASK_FLUID =
            DATA_COMPONENTS.registerComponentType("flask_fluid", builder -> builder
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    private ModDataComponents() {
    }
}
