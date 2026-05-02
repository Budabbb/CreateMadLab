package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MorphineHudOverlay {
    private static final ResourceLocation GUI_ICONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minecraft",
            "textures/gui/icons.png"
    );
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
    private static final int HEART_ICON_BASE_X = 16;
    private static final int HARDCORE_HEART_Y_OFFSET = 45;

    private MorphineHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.type().id())) {
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

        renderMorphineHealthBar(
                event.getGuiGraphics(),
                player,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),
                unstableHp
        );
        event.setCanceled(true);
    }

    private static void renderMorphineHealthBar(GuiGraphics guiGraphics, Player player, int screenWidth, int screenHeight, int unstableHp) {
        int left = screenWidth / 2 - 91;
        int healthBaseY = screenHeight - 39;
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
        int heartYOffset = player.level().getLevelData().isHardcore() ? HARDCORE_HEART_Y_OFFSET : 0;
        VanillaHeartType normalHeartType = VanillaHeartType.forPlayer(player);
        int unstableStartHp = currentHealth - visualDebtHp;

        for (int heartIndex = maxHealthHearts + absorptionHearts - 1; heartIndex >= 0; --heartIndex) {
            int row = heartIndex / 10;
            int column = heartIndex % 10;
            int x = left + column * HEART_SPACING;
            int y = healthBaseY - row * rowHeight;

            drawVanillaHeart(guiGraphics, VanillaHeartType.CONTAINER, x, y, heartYOffset, false, false);
            if (heartIndex >= maxHealthHearts) {
                int absorptionStartHp = (heartIndex - maxHealthHearts) * 2;
                if (absorptionStartHp < absorption) {
                    boolean halfAbsorption = absorptionStartHp + 1 == absorption;
                    drawVanillaHeart(guiGraphics, VanillaHeartType.ABSORBING, x, y, heartYOffset, false, halfAbsorption);
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
                drawVanillaHeart(guiGraphics, normalHeartType, x, y, heartYOffset, false, halfHeart);
                continue;
            }

            renderUnstableHeart(guiGraphics, x, y, heartYOffset, normalHeartType, overlayVariant);
        }
    }

    private static void renderUnstableHeart(GuiGraphics guiGraphics, int x, int y, int heartYOffset, VanillaHeartType normalHeartType, HeartOverlayVariant overlayVariant) {
        if (overlayVariant == HeartOverlayVariant.HALF_RIGHT) {
            drawVanillaHeart(guiGraphics, normalHeartType, x, y, heartYOffset, false, true);
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

    private static void drawVanillaHeart(GuiGraphics guiGraphics, VanillaHeartType heartType, int x, int y, int heartYOffset, boolean renderHighlight, boolean halfHeart) {
        guiGraphics.blit(
                GUI_ICONS_TEXTURE,
                x,
                y,
                heartType.getX(halfHeart, renderHighlight),
                heartYOffset,
                HEART_SIZE,
                HEART_SIZE
        );
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

    private enum VanillaHeartType {
        CONTAINER(0, false),
        NORMAL(2, true),
        POISONED(4, true),
        WITHERED(6, true),
        ABSORBING(8, false),
        FROZEN(9, false);

        private final int index;
        private final boolean canBlink;

        VanillaHeartType(int index, boolean canBlink) {
            this.index = index;
            this.canBlink = canBlink;
        }

        private static VanillaHeartType forPlayer(Player player) {
            if (player.hasEffect(MobEffects.POISON)) {
                return POISONED;
            }
            if (player.hasEffect(MobEffects.WITHER)) {
                return WITHERED;
            }
            if (player.isFullyFrozen()) {
                return FROZEN;
            }
            return NORMAL;
        }

        private int getX(boolean halfHeart, boolean renderHighlight) {
            int iconOffset;
            if (this == CONTAINER) {
                iconOffset = renderHighlight ? 1 : 0;
            } else {
                int halfOffset = halfHeart ? 1 : 0;
                int blinkOffset = canBlink && renderHighlight ? 2 : 0;
                iconOffset = halfOffset + blinkOffset;
            }

            return HEART_ICON_BASE_X + (index * 2 + iconOffset) * HEART_SIZE;
        }
    }
}
