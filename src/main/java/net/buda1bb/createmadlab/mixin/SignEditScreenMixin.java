package net.buda1bb.createmadlab.mixin;

import net.buda1bb.createmadlab.client.TextScrambler;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditScreenMixin {
    @ModifyArg(
            method = "removed()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundSignUpdatePacket;<init>(Lnet/minecraft/core/BlockPos;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
            ),
            index = 2
    )
    private String createmadlab$scrambleFirstLine(String line) {
        return TextScrambler.scrambleTypedText(line);
    }

    @ModifyArg(
            method = "removed()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundSignUpdatePacket;<init>(Lnet/minecraft/core/BlockPos;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
            ),
            index = 3
    )
    private String createmadlab$scrambleSecondLine(String line) {
        return TextScrambler.scrambleTypedText(line);
    }

    @ModifyArg(
            method = "removed()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundSignUpdatePacket;<init>(Lnet/minecraft/core/BlockPos;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
            ),
            index = 4
    )
    private String createmadlab$scrambleThirdLine(String line) {
        return TextScrambler.scrambleTypedText(line);
    }

    @ModifyArg(
            method = "removed()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundSignUpdatePacket;<init>(Lnet/minecraft/core/BlockPos;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
            ),
            index = 5
    )
    private String createmadlab$scrambleFourthLine(String line) {
        return TextScrambler.scrambleTypedText(line);
    }
}
