package net.buda1bb.createmadlab.mixin;

import net.buda1bb.createmadlab.client.TextScrambler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @ModifyVariable(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true)
    private String createmadlab$scrambleChatMessage(String message) {
        return TextScrambler.scrambleTypedText(message);
    }
}
