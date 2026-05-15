package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.drug.DrugStateManager;
import net.buda1bb.createmadlab.drug.DrugType;
import net.buda1bb.createmadlab.effect.HeroinEffectsManager;
import net.buda1bb.createmadlab.effect.LSDEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.effect.UniversalOverdoseHandler;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.buda1bb.createmadlab.item.ModItems;
import net.buda1bb.createmadlab.item.SyringeItem;
import net.buda1bb.createmadlab.util.ItemDataUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID)
public class LacingEventHandler {
    private static final String CONTENT_TAG = "content";
    private static final String DOSE_TAG = "dose";

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        ItemStack usedStack = event.getItem();
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (!(entity instanceof Player player)) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        if (usedStack.getItem() instanceof SyringeItem || usedStack.getItem() instanceof LSDPaperItem) {
            return;
        }

        String content = getContent(usedStack);
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
                handleLSDConsumption(player, level, usedStack);
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
        DrugStateManager.addDrug(player, DrugType.HEROIN, 1.0F);
    }

    private static void handleMorphineConsumption(Player player, Level level) {
        DrugStateManager.addDrug(player, DrugType.MORPHINE, 1.0F);
    }

    private static void handleLSDConsumption(Player player, Level level, ItemStack stack) {
        DrugStateManager.addDrug(player, DrugType.LSD, (float) getDose(stack));
    }

    private static void handleVoidConsumption(Player player, Level level) {
        DrugStateManager.addDrug(player, DrugType.FENTANYL, 1.0F);
    }

    private static void applyBlissCooldowns(Player player) {
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), HeroinEffectsManager.getCooldownDuration());
    }

    private static void applyMorphineCooldowns(Player player) {
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), MorphineEffectsManager.getCooldownDuration());
    }

    private static void applyLSDCooldowns(Player player) {
        int cooldownDuration = LSDEffectsManager.getCooldownDuration();
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), cooldownDuration);
    }

    private static void applyVoidCooldowns(Player player) {
        int cooldownDuration = UniversalOverdoseHandler.getCooldownDuration();
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), cooldownDuration);
    }

    private static String getContent(ItemStack stack) {
        if (ItemDataUtils.contains(stack, CONTENT_TAG)) {
            return ItemDataUtils.getTagCopy(stack).getString(CONTENT_TAG);
        }
        return null;
    }

    private static double getDose(ItemStack stack) {
        if (ItemDataUtils.contains(stack, DOSE_TAG)) {
            return ItemDataUtils.getTagCopy(stack).getDouble(DOSE_TAG);
        }
        return 1.0D;
    }
}
