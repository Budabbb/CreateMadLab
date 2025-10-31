package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.client.ShaderFileSwapper;
import net.buda1bb.createmadlab.effect.BlissEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    private static final int EFFECT_DELAY_TICKS = 2 * 60 * 20;
    private static final int EFFECT_DURATION_TICKS = 6 * 60 * 20;
    public static final int TOTAL_EFFECT_DURATION = EFFECT_DELAY_TICKS + EFFECT_DURATION_TICKS;
    private static final String DOSE_TAG = "dose";
    private static final String LSD_START_TIME_TAG = "LsdStartTime";
    private static final String LSD_ACTIVE_TAG = "LsdActive";

    public LSDPaperItem(Properties properties) {
        super(properties.food(ModConsumables.LSD_PAPER));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return stack;
            }

            if (!CreateMadLab.isShaderpackEnabled()) {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cPlease enable 'createmadlab_shaders' shaderpack for the effect to work!"), true);
                }
                return stack;
            }

            double dose = getDose(stack);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);

                ItemStack remainder = new ItemStack(Items.PAPER);
                if (!player.getInventory().add(remainder)) {
                    player.drop(remainder, false);
                }
            }

            CompoundTag persistentData = player.getPersistentData();
            CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
            compoundtag.putLong(LSD_START_TIME_TAG, level.getGameTime());
            compoundtag.putDouble(DOSE_TAG, dose);
            compoundtag.putBoolean(LSD_ACTIVE_TAG, false);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);

            applyCrossCooldowns(player);
        }
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!CreateMadLab.isShaderpackEnabled()) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.literal("§cPlease enable 'createmadlab_shaders' shaderpack for the effect to work!"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (level != null && level.isClientSide && !CreateMadLab.isShaderpackEnabled()) {
            tooltip.add(Component.literal("§cWarning: Shaderpack not enabled!"));
            tooltip.add(Component.literal("§7Enable 'createmadlab_shaders' for effects"));
        }
    }

    private void applyCrossCooldowns(Player player) {
        player.getCooldowns().addCooldown(this, TOTAL_EFFECT_DURATION);

        Item SyringeItem = ModItems.SYRINGE.get();
        if (SyringeItem != null) {
            player.getCooldowns().addCooldown(SyringeItem, TOTAL_EFFECT_DURATION);
        }
    }

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

        if (elapsedTicks >= TOTAL_EFFECT_DURATION) {
            if (isActive && level.isClientSide) {
                ShaderFileSwapper.deactivateShaders();
            }
            compoundtag.remove(LSD_START_TIME_TAG);
            compoundtag.remove(DOSE_TAG);
            compoundtag.remove(LSD_ACTIVE_TAG);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
        } else if (elapsedTicks >= EFFECT_DELAY_TICKS && !isActive) {
            if (level.isClientSide) {
                ShaderFileSwapper.activateLSDShaders(dose);
            }
            compoundtag.putBoolean(LSD_ACTIVE_TAG, true);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
        } else if (elapsedTicks < EFFECT_DELAY_TICKS && isActive) {
            if (level.isClientSide) {
                ShaderFileSwapper.deactivateShaders();
            }
            compoundtag.putBoolean(LSD_ACTIVE_TAG, false);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
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
}