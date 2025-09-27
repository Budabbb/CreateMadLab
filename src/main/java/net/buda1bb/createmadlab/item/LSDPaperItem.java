package net.buda1bb.createmadlab.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class LSDPaperItem extends Item {

    public LSDPaperItem(Properties properties) {
        super(properties.food(ModConsumables.LSD_PAPER));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            stack.shrink(1);
            ItemStack remainder = new ItemStack(Items.PAPER);
            if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }
        return stack;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }
}

