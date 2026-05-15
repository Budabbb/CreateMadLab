package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpiateWithdrawalEffectS2CPacket(boolean active, int remainingTicks, int totalTicks,
                                              float intensity) implements CustomPacketPayload {
    public static final Type<OpiateWithdrawalEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "opiate_withdrawal_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpiateWithdrawalEffectS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpiateWithdrawalEffectS2CPacket::active,
            ByteBufCodecs.VAR_INT, OpiateWithdrawalEffectS2CPacket::remainingTicks,
            ByteBufCodecs.VAR_INT, OpiateWithdrawalEffectS2CPacket::totalTicks,
            ByteBufCodecs.FLOAT, OpiateWithdrawalEffectS2CPacket::intensity,
            OpiateWithdrawalEffectS2CPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpiateWithdrawalEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateOpiateWithdrawalShaders(packet.remainingTicks, packet.totalTicks, packet.intensity);
            } else {
                ShaderUtils.deactivateOpiateWithdrawalShaders();
            }
        });
    }
}
