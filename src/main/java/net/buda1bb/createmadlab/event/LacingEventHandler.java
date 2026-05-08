package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.buda1bb.createmadlab.item.ModItems;
import net.buda1bb.createmadlab.item.SyringeItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LacingEventHandler {
    private static final String CONTENT_TAG = "content";
    private static final String DOSE_TAG = "dose";

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        ItemStack stack = event.getResultStack();
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (!(entity instanceof Player player)) {
            return;
        }

        if (stack.getItem() instanceof SyringeItem || stack.getItem() instanceof LSDPaperItem) {
            return;
        }

        String content = getContent(event.getItem());
        if (content == null || "empty".equals(content)) {
            return;
        }

        switch (content) {
            case "bliss" -> {
                handleBlissConsumption(player, level);
                applyBlissCooldowns(player);
            }
            case "morphine" -> {
                handleMorphineConsumption(player, level);
                applyMorphineCooldowns(player);
            }
            case "lsd" -> {
                handleLSDConsumption(player, level, event.getItem());
                applyLSDCooldowns(player);
            }
            case "void" -> {
                handleVoidConsumption(player, level);
                applyVoidCooldowns(player);
            }
            default -> {
            }
        }
    }

    private static void handleBlissConsumption(Player player, Level level) {
        HeroinEffectsManager.startHeroinEffect(player, level);
    }

    private static void handleMorphineConsumption(Player player, Level level) {
        MorphineEffectsManager.startMorphineEffect(player, level);
    }

    private static void handleLSDConsumption(Player player, Level level, ItemStack stack) {
        LSDEffectsManager.startLsdEffect(player, level, getDose(stack));
    }

    private static void handleVoidConsumption(Player player, Level level) {
        FentanylEffectsManager.startFentanylOverdose(player, level);
    }

    private static void applyBlissCooldowns(Player player) {
        player.getCooldowns().addCooldown(ModItems.SYRINGE.get(), HeroinEffectsManager.getCooldownDuration());
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), HeroinEffectsManager.getCooldownDuration());
    }

    private static void applyMorphineCooldowns(Player player) {
        player.getCooldowns().addCooldown(ModItems.SYRINGE.get(), MorphineEffectsManager.getCooldownDuration());
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), MorphineEffectsManager.getCooldownDuration());
    }

    private static void applyLSDCooldowns(Player player) {
        int cooldownDuration = LSDEffectsManager.getCooldownDuration();
        player.getCooldowns().addCooldown(ModItems.SYRINGE.get(), cooldownDuration);
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), cooldownDuration);
    }

    private static void applyVoidCooldowns(Player player) {
        int cooldownDuration = FentanylEffectsManager.getCooldownDuration();
        player.getCooldowns().addCooldown(ModItems.SYRINGE.get(), cooldownDuration);
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), cooldownDuration);
    }

    private static String getContent(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(CONTENT_TAG)) {
            return stack.getTag().getString(CONTENT_TAG);
        }
        return null;
    }

    private static double getDose(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(DOSE_TAG)) {
            return stack.getTag().getDouble(DOSE_TAG);
        }
        return 1.0D;
    }
}
