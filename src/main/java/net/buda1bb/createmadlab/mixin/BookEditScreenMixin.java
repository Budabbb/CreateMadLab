package net.buda1bb.createmadlab.mixin;

import net.buda1bb.createmadlab.client.TextScrambler;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin {
    @ModifyArg(
            method = "saveChanges(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundEditBookPacket;<init>(ILjava/util/List;Ljava/util/Optional;)V"
            ),
            index = 1
    )
    private List<String> createmadlab$scramblePages(List<String> pages) {
        return pages.stream()
                .map(TextScrambler::scrambleTypedText)
                .collect(Collectors.toList());
    }

    @ModifyArg(
            method = "saveChanges(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundEditBookPacket;<init>(ILjava/util/List;Ljava/util/Optional;)V"
            ),
            index = 2
    )
    private Optional<String> createmadlab$scrambleTitle(Optional<String> title) {
        return title.map(TextScrambler::scrambleTypedText);
    }
}
