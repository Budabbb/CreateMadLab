package net.buda1bb.createmadlab.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID, value = Dist.CLIENT)
public final class MorphineHudOverlay {
    private static final ResourceLocation UNSTABLE_HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateMadLab.MOD_ID,
            "textures/gui/hud/unstable_heart.png"
    );
    private static final ResourceLocation UNSTABLE_HALF_HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateMadLab.MOD_ID,
            "textures/gui/hud/unstable_heart_half_left.png"
    );
    private static final ResourceLocation UNSTABLE_HALF_RIGHT_HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateMadLab.MOD_ID,
            "textures/gui/hud/unstable_heart_half_right.png"
    );
    private static final int HEART_SIZE = 9;
    private static final int HEART_SPACING = 8;

    private MorphineHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null
                || minecraft.player == null
                || minecraft.options.hideGui
                || minecraft.gameMode == null
                || !minecraft.gameMode.canHurtPlayer()
                || !(minecraft.getCameraEntity() instanceof Player)) {
            return;
        }

        Player player = minecraft.player;
        int unstableHp = MorphineTripState.getUnstableHp();
        if (unstableHp <= 0) {
            return;
        }

        Gui gui = minecraft.gui;
        if (gui == null) {
            return;
        }

        int screenWidth = event.getGuiGraphics().guiWidth();
        int screenHeight = event.getGuiGraphics().guiHeight();
        int healthBaseY = screenHeight - gui.leftHeight;
        renderMorphineHealthBar(
                event.getGuiGraphics(),
                player,
                screenWidth,
                healthBaseY,
                unstableHp
        );
        reserveHealthOverlayHeight(gui, player);
        event.setCanceled(true);
    }

    private static void renderMorphineHealthBar(GuiGraphics guiGraphics, Player player, int screenWidth, int healthBaseY, int unstableHp) {
        int left = screenWidth / 2 - 91;
        int currentHealth = Mth.ceil(player.getHealth());
        if (currentHealth <= 0) {
            return;
        }

        int visualDebtHp = Math.min(unstableHp, currentHealth);
        if (visualDebtHp <= 0) {
            return;
        }

        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), (float) currentHealth);
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int healthRows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);
        int maxHealthHearts = Mth.ceil(maxHealth / 2.0F);
        int absorptionHearts = Mth.ceil(absorption / 2.0F);
        boolean hardcore = player.level().getLevelData().isHardcore();
        Gui.HeartType normalHeartType = getHeartTypeForPlayer(player);
        int unstableStartHp = currentHealth - visualDebtHp;

        for (int heartIndex = maxHealthHearts + absorptionHearts - 1; heartIndex >= 0; --heartIndex) {
            int row = heartIndex / 10;
            int column = heartIndex % 10;
            int x = left + column * HEART_SPACING;
            int y = healthBaseY - row * rowHeight;

            drawVanillaHeart(guiGraphics, Gui.HeartType.CONTAINER, x, y, hardcore, false, false);
            if (heartIndex >= maxHealthHearts) {
                int absorptionStartHp = (heartIndex - maxHealthHearts) * 2;
                if (absorptionStartHp < absorption) {
                    boolean halfAbsorption = absorptionStartHp + 1 == absorption;
                    Gui.HeartType absorptionHeartType = normalHeartType == Gui.HeartType.WITHERED
                            ? normalHeartType
                            : Gui.HeartType.ABSORBING;
                    drawVanillaHeart(guiGraphics, absorptionHeartType, x, y, hardcore, false, halfAbsorption);
                }
                continue;
            }

            int heartStartHp = heartIndex * 2;
            if (heartStartHp >= currentHealth) {
                continue;
            }

            HeartOverlayVariant overlayVariant = getOverlayVariantForHeart(heartIndex, currentHealth, unstableStartHp);
            if (overlayVariant == HeartOverlayVariant.NONE) {
                boolean halfHeart = heartStartHp + 1 == currentHealth;
                drawVanillaHeart(guiGraphics, normalHeartType, x, y, hardcore, false, halfHeart);
                continue;
            }

            renderUnstableHeart(guiGraphics, x, y, hardcore, normalHeartType, overlayVariant);
        }
    }

    private static void reserveHealthOverlayHeight(Gui gui, Player player) {
        int currentHealth = Mth.ceil(player.getHealth());
        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), (float) currentHealth);
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int healthRows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        gui.leftHeight += healthRows * rowHeight;
        if (rowHeight != 10) {
            gui.leftHeight += 10 - rowHeight;
        }
    }

    private static void renderUnstableHeart(GuiGraphics guiGraphics, int x, int y, boolean hardcore, Gui.HeartType normalHeartType, HeartOverlayVariant overlayVariant) {
        if (overlayVariant == HeartOverlayVariant.HALF_RIGHT) {
            drawVanillaHeart(guiGraphics, normalHeartType, x, y, hardcore, false, true);
        }

        ResourceLocation texture = switch (overlayVariant) {
            case FULL -> UNSTABLE_HEART_TEXTURE;
            case HALF_LEFT -> UNSTABLE_HALF_HEART_TEXTURE;
            case HALF_RIGHT -> UNSTABLE_HALF_RIGHT_HEART_TEXTURE;
            case NONE -> null;
        };
        if (texture != null) {
            guiGraphics.blit(texture, x, y, 0.0F, 0.0F, HEART_SIZE, HEART_SIZE, HEART_SIZE, HEART_SIZE);
        }
    }

    private static void drawVanillaHeart(GuiGraphics guiGraphics, Gui.HeartType heartType, int x, int y, boolean hardcore, boolean renderHighlight, boolean halfHeart) {
        RenderSystem.enableBlend();
        guiGraphics.blitSprite(heartType.getSprite(hardcore, halfHeart, renderHighlight), x, y, HEART_SIZE, HEART_SIZE);
        RenderSystem.disableBlend();
    }

    private static Gui.HeartType getHeartTypeForPlayer(Player player) {
        if (player.hasEffect(MobEffects.POISON)) {
            return Gui.HeartType.POISIONED;
        }
        if (player.hasEffect(MobEffects.WITHER)) {
            return Gui.HeartType.WITHERED;
        }
        if (player.isFullyFrozen()) {
            return Gui.HeartType.FROZEN;
        }
        return Gui.HeartType.NORMAL;
    }

    private static HeartOverlayVariant getOverlayVariantForHeart(int heartIndex, int currentHealth, int unstableStartHp) {
        int leftHalfIndex = heartIndex * 2;
        int rightHalfIndex = leftHalfIndex + 1;
        boolean leftHalfOccupied = leftHalfIndex < currentHealth;
        boolean rightHalfOccupied = rightHalfIndex < currentHealth;
        boolean leftHalfUnstable = leftHalfOccupied && leftHalfIndex >= unstableStartHp;
        boolean rightHalfUnstable = rightHalfOccupied && rightHalfIndex >= unstableStartHp;

        if (leftHalfUnstable && rightHalfUnstable) {
            return HeartOverlayVariant.FULL;
        }
        if (leftHalfUnstable) {
            return HeartOverlayVariant.HALF_LEFT;
        }
        if (rightHalfUnstable) {
            return HeartOverlayVariant.HALF_RIGHT;
        }

        return HeartOverlayVariant.NONE;
    }

    private enum HeartOverlayVariant {
        NONE,
        FULL,
        HALF_LEFT,
        HALF_RIGHT
    }

}
