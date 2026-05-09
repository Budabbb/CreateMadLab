package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LSDEffectS2CPacket(int remainingTicks, int totalTicks, float strength) implements CustomPacketPayload {
    public static final Type<LSDEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "lsd_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LSDEffectS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LSDEffectS2CPacket::remainingTicks,
            ByteBufCodecs.VAR_INT, LSDEffectS2CPacket::totalTicks,
            ByteBufCodecs.FLOAT, LSDEffectS2CPacket::strength,
            LSDEffectS2CPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LSDEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.remainingTicks <= 0 || packet.totalTicks <= 0) {
                ShaderUtils.deactivateLSDShaders();
                return;
            }
            ShaderUtils.activateLSDShaders(packet.remainingTicks, packet.totalTicks, packet.strength);
        });
    }
}
