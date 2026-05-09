package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FentanylEffectS2CPacket(boolean active, int remainingTicks, int totalTicks) implements CustomPacketPayload {
    public static final Type<FentanylEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "fentanyl_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FentanylEffectS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FentanylEffectS2CPacket::active,
            ByteBufCodecs.VAR_INT, FentanylEffectS2CPacket::remainingTicks,
            ByteBufCodecs.VAR_INT, FentanylEffectS2CPacket::totalTicks,
            FentanylEffectS2CPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FentanylEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateFentanylOverdoseShaders(packet.remainingTicks, packet.totalTicks);
            } else {
                ShaderUtils.deactivateFentanylOverdoseShaders();
            }
        });
    }
}
