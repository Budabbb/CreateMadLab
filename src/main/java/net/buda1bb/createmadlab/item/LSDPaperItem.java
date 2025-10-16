package net.buda1bb.createmadlab.item;

import net.buda1bb.createmadlab.client.ShaderFileSwapper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class LSDPaperItem extends Item {
    // Timer duration in ticks (20 ticks = 1 second)
    private static final int LSD_DURATION_TICKS = (5 * 60 + 20) * 20; // 5 minutes 20 seconds

    public LSDPaperItem(Properties properties) {
        super(properties.food(ModConsumables.LSD_PAPER));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            // Consume the LSD paper
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            // Give back plain paper
            ItemStack remainder = new ItemStack(Items.PAPER);
            if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }

            // Activate shader effect
            if (level.isClientSide) {
                activateShaderEffect(player);
            } else {
                // Server-side: send packet to client
                if (player instanceof ServerPlayer serverPlayer) {
                    // You'll need to create a packet for this
                    System.out.println("LSD consumed by: " + player.getName().getString());
                }
            }

            // Add cooldown to prevent spam
            player.getCooldowns().addCooldown(this, 20);
        }
        return stack;
    }

    private void activateShaderEffect(Player player) {
        // Activate the LSD shaders
        ShaderFileSwapper.activateLSDShaders();

        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);

        System.out.println("LSD effect activated for player: " + player.getName().getString());

        // Schedule deactivation after timer
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        ShaderFileSwapper.deactivateLSDShaders();
                    }
                },
                LSD_DURATION_TICKS * 50 // Convert ticks to milliseconds
        );
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }
}