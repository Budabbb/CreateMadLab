package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.client.ShaderFileSwapper;
import net.buda1bb.createmadlab.effect.BlissEffectsManager;
import net.buda1bb.createmadlab.effect.MorphineEffectsManager;
import net.buda1bb.createmadlab.item.LSDPaperItem;
import net.buda1bb.createmadlab.item.ModItems;
import net.buda1bb.createmadlab.item.SyringeItem;
import net.minecraft.nbt.CompoundTag;
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
    private static final int LSD_EFFECT_DELAY_TICKS = 2 * 60 * 20;
    private static final int LSD_EFFECT_DURATION_TICKS = 6 * 60 * 20;
    public static final int LSD_TOTAL_EFFECT_DURATION = LSD_EFFECT_DELAY_TICKS + LSD_EFFECT_DURATION_TICKS;

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        ItemStack stack = event.getResultStack();
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (!(entity instanceof Player player)) {
            return;
        }

        if (!CreateMadLab.isShaderpackEnabled()) {
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
            case "bliss":
                handleBlissConsumption(player, level);
                applyBlissCooldowns(player);
                break;
            case "morphine":
                handleMorphineConsumption(player, level);
                applyMorphineCooldowns(player);
                break;
            case "lsd":
                handleLSDConsumption(player, level);
                applyLSDCooldowns(player);
                break;
        }
    }

    private static void handleBlissConsumption(Player player, Level level) {
        if (level.isClientSide) {
            BlissEffectsManager.startBlissEffect(player, level);
        }

        if (!level.isClientSide) {
            BlissEffectsManager.applyBlissEffects(player, level);
        }
    }

    private static void handleMorphineConsumption(Player player, Level level) {
        MorphineEffectsManager.startMorphineEffect(player, level);
    }

    private static void handleLSDConsumption(Player player, Level level) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        compoundtag.putLong("LsdStartTime", level.getGameTime());
        compoundtag.putDouble("LsdDose", 2.0);
        compoundtag.putBoolean("LsdActive", false);
        persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
    }

    public static void handleLacedLSDEffects(Player player, Level level) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag compoundtag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);

        if (!compoundtag.contains("LsdStartTime")) {
            return;
        }

        long startTime = compoundtag.getLong("LsdStartTime");
        long currentTime = level.getGameTime();
        long elapsedTicks = currentTime - startTime;
        double dose = compoundtag.getDouble("LsdDose");
        boolean isActive = compoundtag.getBoolean("LsdActive");

        if (elapsedTicks >= LSD_TOTAL_EFFECT_DURATION) {
            if (isActive && level.isClientSide) {
                ShaderFileSwapper.deactivateShaders();
            }
            compoundtag.remove("LsdStartTime");
            compoundtag.remove("LsdDose");
            compoundtag.remove("LsdActive");
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
        } else if (elapsedTicks >= LSD_EFFECT_DELAY_TICKS && !isActive) {
            if (level.isClientSide) {
                ShaderFileSwapper.activateLSDShaders(dose);
            }
            compoundtag.putBoolean("LsdActive", true);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
        } else if (elapsedTicks < LSD_EFFECT_DELAY_TICKS && isActive) {
            if (level.isClientSide) {
                ShaderFileSwapper.deactivateShaders();
            }
            compoundtag.putBoolean("LsdActive", false);
            persistentData.put(Player.PERSISTED_NBT_TAG, compoundtag);
        }
    }

    private static void applyBlissCooldowns(Player player) {
        player.getCooldowns().addCooldown(ModItems.SYRINGE.get(), BlissEffectsManager.getCooldownDuration());
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), BlissEffectsManager.getCooldownDuration());
    }

    private static void applyMorphineCooldowns(Player player) {
        player.getCooldowns().addCooldown(ModItems.SYRINGE.get(), MorphineEffectsManager.getCooldownDuration());
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), MorphineEffectsManager.getCooldownDuration());
    }

    private static void applyLSDCooldowns(Player player) {
        int cooldownDuration = LSD_TOTAL_EFFECT_DURATION;
        player.getCooldowns().addCooldown(ModItems.SYRINGE.get(), cooldownDuration);
        player.getCooldowns().addCooldown(ModItems.LSD_PAPER.get(), cooldownDuration);
    }

    private static String getContent(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(CONTENT_TAG)) {
            return stack.getTag().getString(CONTENT_TAG);
        }
        return null;
    }
}