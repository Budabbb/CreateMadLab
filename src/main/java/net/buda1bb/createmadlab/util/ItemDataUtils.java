package net.buda1bb.createmadlab.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class ItemDataUtils {
    private ItemDataUtils() {
    }

    public static boolean hasCustomData(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public static boolean contains(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(key);
    }

    @Nullable
    public static CompoundTag getTagCopy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> updater) {
        CompoundTag tag = getOrCreateTagCopy(stack);
        updater.accept(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static CompoundTag getOrCreateTagCopy(ItemStack stack) {
        CompoundTag tag = getTagCopy(stack);
        return tag == null ? new CompoundTag() : tag;
    }
}
