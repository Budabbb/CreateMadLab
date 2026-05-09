package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HeroinEffectS2CPacket(boolean active, int durationTicks) implements CustomPacketPayload {
    public static final Type<HeroinEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "heroin_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeroinEffectS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HeroinEffectS2CPacket::active,
            ByteBufCodecs.VAR_INT, HeroinEffectS2CPacket::durationTicks,
            HeroinEffectS2CPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HeroinEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateHeroinShaders(packet.durationTicks);
            } else {
                ShaderUtils.deactivateHeroinShaders();
            }
        });
    }
}
