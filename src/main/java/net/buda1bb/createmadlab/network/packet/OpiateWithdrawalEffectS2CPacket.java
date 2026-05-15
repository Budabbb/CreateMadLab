package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpiateWithdrawalEffectS2CPacket {
    private final boolean active;
    private final int remainingTicks;
    private final int totalTicks;
    private final float intensity;

    public OpiateWithdrawalEffectS2CPacket(boolean active, int remainingTicks, int totalTicks, float intensity) {
        this.active = active;
        this.remainingTicks = remainingTicks;
        this.totalTicks = totalTicks;
        this.intensity = intensity;
    }

    public static void encode(OpiateWithdrawalEffectS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeFloat(packet.intensity);
    }

    public static OpiateWithdrawalEffectS2CPacket decode(FriendlyByteBuf buffer) {
        return new OpiateWithdrawalEffectS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat()
        );
    }

    public static void handle(OpiateWithdrawalEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateOpiateWithdrawalShaders(packet.remainingTicks, packet.totalTicks, packet.intensity);
            } else {
                ShaderUtils.deactivateOpiateWithdrawalShaders();
            }
        });
        context.setPacketHandled(true);
    }
}
