package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LSDPaperItem extends Item {
    private static final String DOSE_TAG = "dose";

    public LSDPaperItem(Properties properties) {
        super(properties.food(ModConsumables.LSD_PAPER));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        double dose = getDose(stack);
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player) {
            if (!level.isClientSide) {
                LSDEffectsManager.startLsdEffect(player, level, dose);
                applyCrossCooldowns(player);
            }

            if (!player.getAbilities().instabuild) {
                ItemStack remainder = new ItemStack(Items.PAPER);
                if (!player.getInventory().add(remainder)) {
                    player.drop(remainder, false);
                }
            }
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    private void applyCrossCooldowns(Player player) {
        int cooldownDuration = LSDEffectsManager.getCooldownDuration();
        player.getCooldowns().addCooldown(this, cooldownDuration);

        Item syringeItem = ModItems.SYRINGE.get();
        if (syringeItem != null) {
            player.getCooldowns().addCooldown(syringeItem, cooldownDuration);
        }
    }

    public static double getDose(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(DOSE_TAG)) {
            return stack.getTag().getDouble(DOSE_TAG);
        }
        return 1.0;
    }

    public static void setDose(ItemStack stack, double dose) {
        stack.getOrCreateTag().putDouble(DOSE_TAG, dose);
    }
}
