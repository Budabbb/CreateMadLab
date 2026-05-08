package net.buda1bb.createmadlab.mixin;

import net.buda1bb.createmadlab.client.TextScrambler;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AnvilScreen.class)
public abstract class AnvilEditScreenMixin {
    @ModifyArg(
            method = "onNameChanged(Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundRenameItemPacket;<init>(Ljava/lang/String;)V"
            ),
            index = 0
    )
    private String createmadlab$scrambleName(String name) {
        return TextScrambler.scrambleTypedText(name);
    }
}
