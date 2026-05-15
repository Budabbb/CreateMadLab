package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DrugVisualStateS2CPacket(boolean morphineActive, boolean heroinActive, boolean fentanylActive,
                                       boolean lsdActive, boolean opioidOverdoseActive,
                                       boolean withdrawalActive) implements CustomPacketPayload {
    public static final Type<DrugVisualStateS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "drug_visual_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DrugVisualStateS2CPacket> STREAM_CODEC =
            StreamCodec.of(DrugVisualStateS2CPacket::encode, DrugVisualStateS2CPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, DrugVisualStateS2CPacket packet) {
        buffer.writeBoolean(packet.morphineActive);
        buffer.writeBoolean(packet.heroinActive);
        buffer.writeBoolean(packet.fentanylActive);
        buffer.writeBoolean(packet.lsdActive);
        buffer.writeBoolean(packet.opioidOverdoseActive);
        buffer.writeBoolean(packet.withdrawalActive);
    }

    private static DrugVisualStateS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new DrugVisualStateS2CPacket(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(DrugVisualStateS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ShaderUtils.syncDrugVisualAuthority(
                    packet.morphineActive,
                    packet.heroinActive,
                    packet.fentanylActive,
                    packet.lsdActive,
                    packet.opioidOverdoseActive,
                    packet.withdrawalActive
            );
            if (!packet.morphineActive) {
                ShaderUtils.deactivateMorphineShaders();
            }
            if (!packet.heroinActive) {
                ShaderUtils.deactivateHeroinShaders();
            }
            if (!packet.fentanylActive) {
                ShaderUtils.deactivateFentanylShaders();
            }
            if (!packet.lsdActive) {
                ShaderUtils.deactivateLSDShaders();
            }
            if (!packet.opioidOverdoseActive) {
                ShaderUtils.deactivateOpioidOverdoseShaders();
            }
            if (!packet.withdrawalActive) {
                ShaderUtils.deactivateOpiateWithdrawalShaders();
            }
        });
    }
}
