package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.client.ShaderFileSwapper;
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
    private static final String BLISS_START_TIME_TAG = "BlissStartTime";
    private static final String BLISS_ACTIVE_TAG = "BlissActive";
    private static final int HEROIN_EFFECT_DURATION = 185 * 20;
    public static final int COOLDOWN_DURATION = HEROIN_EFFECT_DURATION;

    public SyringeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!hasContent(stack) || !"bliss".equals(getContent(stack))) {
                return stack;
            }

            if (level.isClientSide && !isShaderpackEnabled()) {
                player.displayClientMessage(Component.literal("§cPlease enable 'createmadlab_shaders' shaderpack for the effect to work!"), true);
                return stack;
            }

            startBlissEffect(player, level);

            applyCrossCooldowns(player);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);

                ItemStack emptySyringe = new ItemStack(this);
                setContent(emptySyringe, "empty");

                if (!player.getInventory().add(emptySyringe)) {
                    player.drop(emptySyringe, false);
                }
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

        if (!hasContent(stack) || !"bliss".equals(getContent(stack))) {
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
            tooltip.add(Component.literal("§dFull of Liquid Bliss"));

            if (level != null && level.isClientSide && !isShaderpackEnabled()) {
                tooltip.add(Component.literal("§cWarning: Shaderpack not enabled!"));
                tooltip.add(Component.literal("§7Enable 'createmadlab_shaders' for effects"));
            }
        } else if ("empty".equals(content)) {
            tooltip.add(Component.literal("§7Empty"));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return hasContent(stack) && "bliss".equals(getContent(stack)) ? 8 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return hasContent(stack) && "bliss".equals(getContent(stack)) ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public boolean isEdible() {
        return false;
    }

    private void startBlissEffect(Player player, Level level) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        compoundtag.putLong(BLISS_START_TIME_TAG, level.getGameTime());
        compoundtag.putBoolean(BLISS_ACTIVE_TAG, true);
        persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);

        if (level.isClientSide) {
            ShaderFileSwapper.activateHeroinShaders();
        }
    }

    public static void handleBlissEffects(Player player, Level level) {
        if (!isBlissActive(player) || player.isDeadOrDying()) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        long startTime = compoundtag.getLong(BLISS_START_TIME_TAG);
        long currentTime = level.getGameTime();
        long elapsedTicks = currentTime - startTime;

        if (elapsedTicks >= HEROIN_EFFECT_DURATION) {
            endBlissEffect(player, level);
        }
    }

    public static void cleanupBlissEffect(Player player, Level level) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);

        compoundtag.remove(BLISS_START_TIME_TAG);
        compoundtag.remove(BLISS_ACTIVE_TAG);
        persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);

        if (level.isClientSide && ShaderFileSwapper.areHeroinShadersActive()) {
            ShaderFileSwapper.deactivateShaders();
        }
    }

    public static void handlePlayerLogin(Player player, Level level) {
        if (hasBlissData(player)) {
            cleanupBlissEffect(player, level);
        }
    }

    private static void endBlissEffect(Player player, Level level) {
        if (level.isClientSide && ShaderFileSwapper.areHeroinShadersActive()) {
            ShaderFileSwapper.deactivateShaders();
        }
        cleanupBlissEffect(player, level);
    }

    public static boolean isBlissActive(Player player) {
        if (player == null || player.isDeadOrDying()) return false;

        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            return false;
        }

        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);

        if (!compoundtag.contains(BLISS_START_TIME_TAG) || !compoundtag.contains(BLISS_ACTIVE_TAG)) {
            return false;
        }

        return compoundtag.getBoolean(BLISS_ACTIVE_TAG);
    }

    private static boolean hasBlissData(Player player) {
        if (player == null) return false;

        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            return false;
        }

        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        return compoundtag.contains(BLISS_START_TIME_TAG) || compoundtag.contains(BLISS_ACTIVE_TAG);
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

    private void applyCrossCooldowns(Player player) {
        player.getCooldowns().addCooldown(this, COOLDOWN_DURATION);

        Item LSDPaperItem = ModItems.LSD_PAPER.get();
        if (LSDPaperItem != null) {
            player.getCooldowns().addCooldown(LSDPaperItem, COOLDOWN_DURATION);
        }
    }

    public static boolean hasContent(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(CONTENT_TAG) &&
                !stack.getTag().getString(CONTENT_TAG).equals("empty");
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