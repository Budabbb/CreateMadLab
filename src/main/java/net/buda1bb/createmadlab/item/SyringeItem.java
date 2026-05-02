package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SyringeItem extends Item {
    private static final String CONTENT_TAG = "content";

    public SyringeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            String content = getContent(stack);

            if ("bliss".equals(content) || "morphine".equals(content)) {
                if ("bliss".equals(content)) {
                    HeroinEffectsManager.startHeroinEffect(player, level);
                    if (!level.isClientSide) {
                        applyBlissCooldowns(player);
                    }
                } else if ("morphine".equals(content)) {
                    MorphineEffectsManager.startMorphineEffect(player, level);
                    if (!level.isClientSide) {
                        applyMorphineCooldowns(player);
                    }
                }

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);

                    ItemStack emptySyringe = new ItemStack(this);
                    setContent(emptySyringe, "empty");

                    if (!player.getInventory().add(emptySyringe)) {
                        player.drop(emptySyringe, false);
                    }
                }
            } else {
                return stack;
            }
        }
        return stack;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        setContent(stack, "empty");
        return stack;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (!stack.hasTag()) {
            setContent(stack, "empty");
        }
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String content = getContent(stack);

        if ("empty".equals(content) || content == null) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        String content = getContent(stack);
        if ("bliss".equals(content)) {
            tooltip.add(Component.literal("Full of Liquid Bliss"));
        } else if ("morphine".equals(content)) {
            tooltip.add(Component.literal("Full of Morphine"));
        } else if ("empty".equals(content)) {
            tooltip.add(Component.literal("Empty"));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        String content = getContent(stack);
        return ("bliss".equals(content) || "morphine".equals(content)) ? 8 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        String content = getContent(stack);
        return ("bliss".equals(content) || "morphine".equals(content)) ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public boolean isEdible() {
        return false;
    }

    private void applyBlissCooldowns(Player player) {
        player.getCooldowns().addCooldown(this, HeroinEffectsManager.getCooldownDuration());

        Item lsdPaperItem = ModItems.LSD_PAPER.get();
        if (lsdPaperItem != null) {
            player.getCooldowns().addCooldown(lsdPaperItem, HeroinEffectsManager.getCooldownDuration());
        }

        Item syringeItem = ModItems.SYRINGE.get();
        if (syringeItem != null) {
            player.getCooldowns().addCooldown(syringeItem, HeroinEffectsManager.getCooldownDuration());
        }
    }

    private void applyMorphineCooldowns(Player player) {
        player.getCooldowns().addCooldown(this, MorphineEffectsManager.getCooldownDuration());

        Item lsdPaperItem = ModItems.LSD_PAPER.get();
        if (lsdPaperItem != null) {
            player.getCooldowns().addCooldown(lsdPaperItem, MorphineEffectsManager.getCooldownDuration());
        }

        Item syringeItem = ModItems.SYRINGE.get();
        if (syringeItem != null) {
            player.getCooldowns().addCooldown(syringeItem, MorphineEffectsManager.getCooldownDuration());
        }
    }

    public static boolean hasContent(ItemStack stack) {
        String content = getContent(stack);
        return content != null && !"empty".equals(content);
    }

    public static String getContent(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(CONTENT_TAG)) {
            return stack.getTag().getString(CONTENT_TAG);
        }
        return "empty";
    }

    public static void setContent(ItemStack stack, String content) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(CONTENT_TAG, content);
    }
}
