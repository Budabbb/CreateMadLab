package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.client.ShaderFileSwapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LSDPaperItem extends Item {
    private static final int EFFECT_DELAY_TICKS = 2 * 60 * 20; // 2 minutes = 2400 ticks
    private static final int EFFECT_DURATION_TICKS = 6 * 60 * 20; // 6 minutes = 7200 ticks
    private static final String DOSE_TAG = "dose";
    private static final String LSD_START_TIME_TAG = "LsdStartTime";
    private static final String LSD_ACTIVE_TAG = "LsdActive";
    private static final String LSD_WARNING_SHOWN_TAG = "LsdWarningShown";

    public LSDPaperItem(Properties properties) {
        super(properties.food(ModConsumables.LSD_PAPER));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return stack;
            }

            // Check if shaderpack is enabled before consumption
            if (level.isClientSide && !isShaderpackEnabled()) {
                player.displayClientMessage(Component.literal("§cPlease enable 'createmadlab_shaders' shaderpack for the effect to work!"), true);
                return stack; // Stop consumption
            }

            double dose = getDose(stack);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            ItemStack remainder = new ItemStack(Items.PAPER);
            if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }

            // Store the start time and dose in player persistence
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
            compoundtag.putLong(LSD_START_TIME_TAG, level.getGameTime());
            compoundtag.putDouble(DOSE_TAG, dose);
            compoundtag.putBoolean(LSD_ACTIVE_TAG, false); // Not active yet, waiting for delay
            compoundtag.putBoolean(LSD_WARNING_SHOWN_TAG, false); // Reset warning flag
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);

            player.getCooldowns().addCooldown(this, EFFECT_DELAY_TICKS + EFFECT_DURATION_TICKS);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (level != null && level.isClientSide && !isShaderpackEnabled()) {
            tooltip.add(Component.literal("§cWarning: Shaderpack not enabled!"));
            tooltip.add(Component.literal("§7Enable 'createmadlab_shaders' for effects"));
        }
    }

    private boolean isShaderpackEnabled() {
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            java.lang.reflect.Method getCurrentPackNameMethod = irisClass.getMethod("getCurrentPackName");
            String currentPack = (String) getCurrentPackNameMethod.invoke(null);

            return currentPack != null && currentPack.equals("createmadlab_shaders");
        } catch (Exception e) {
            return false;
        }
    }

    // Call this method from a tick event handler
    public static void handleLSDEffects(Player player, Level level) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);

        if (!compoundtag.contains(LSD_START_TIME_TAG)) {
            return;
        }

        long startTime = compoundtag.getLong(LSD_START_TIME_TAG);
        long currentTime = level.getGameTime();
        long elapsedTicks = currentTime - startTime;
        double dose = compoundtag.getDouble(DOSE_TAG);
        boolean isActive = compoundtag.getBoolean(LSD_ACTIVE_TAG);
        boolean warningShown = compoundtag.getBoolean(LSD_WARNING_SHOWN_TAG);

        // Show persistent warning if effect should be active but shaderpack is disabled
        if (level.isClientSide && isActive && !isShaderpackEnabledStatic()) {
            if (!warningShown || currentTime % 100 == 0) { // Show warning initially and every 5 seconds
                player.displayClientMessage(Component.literal("§cWarning: 'createmadlab_shaders' shaderpack is disabled! Enable it for LSD effects."), true);
                compoundtag.putBoolean(LSD_WARNING_SHOWN_TAG, true);
                persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
            }
        }

        if (elapsedTicks >= EFFECT_DELAY_TICKS + EFFECT_DURATION_TICKS) {
            // Effect should end
            if (isActive && level.isClientSide) {
                ShaderFileSwapper.deactivateShaders();
            }
            // Clear the data
            compoundtag.remove(LSD_START_TIME_TAG);
            compoundtag.remove(DOSE_TAG);
            compoundtag.remove(LSD_ACTIVE_TAG);
            compoundtag.remove(LSD_WARNING_SHOWN_TAG);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
        } else if (elapsedTicks >= EFFECT_DELAY_TICKS && !isActive) {
            // Effect should start (delay elapsed)
            if (level.isClientSide) {
                if (isShaderpackEnabledStatic()) {
                    ShaderFileSwapper.activateShaders(dose);
                    compoundtag.putBoolean(LSD_WARNING_SHOWN_TAG, false); // Reset warning when enabled
                } else {
                    // Show immediate warning when effect should start but shaderpack is disabled
                    player.displayClientMessage(Component.literal("§cWarning: 'createmadlab_shaders' shaderpack is disabled! Enable it for LSD effects."), true);
                    compoundtag.putBoolean(LSD_WARNING_SHOWN_TAG, true);
                }
            }
            compoundtag.putBoolean(LSD_ACTIVE_TAG, true);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
        } else if (elapsedTicks < EFFECT_DELAY_TICKS && isActive) {
            // Effect became inactive (player changed worlds, etc.)
            if (level.isClientSide) {
                ShaderFileSwapper.deactivateShaders();
            }
            compoundtag.putBoolean(LSD_ACTIVE_TAG, false);
            compoundtag.putBoolean(LSD_WARNING_SHOWN_TAG, false); // Reset warning
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
        }
    }

    private static boolean isShaderpackEnabledStatic() {
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            java.lang.reflect.Method getCurrentPackNameMethod = irisClass.getMethod("getCurrentPackName");
            String currentPack = (String) getCurrentPackNameMethod.invoke(null);

            return currentPack != null && currentPack.equals("createmadlab_shaders");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    public static double getDose(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(DOSE_TAG)) {
            return stack.getTag().getDouble(DOSE_TAG);
        }
        return 1.0;
    }

    public static void setDose(ItemStack stack, double dose) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putDouble(DOSE_TAG, dose);
    }
}