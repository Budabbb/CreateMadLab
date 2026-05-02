package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LSDEffectS2CPacket {
    private final int remainingTicks;
    private final int totalTicks;
    private final float strength;

    public LSDEffectS2CPacket(int remainingTicks, int totalTicks, float strength) {
        this.remainingTicks = remainingTicks;
        this.totalTicks = totalTicks;
        this.strength = strength;
    }

    public static void encode(LSDEffectS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeFloat(packet.strength);
    }

    public static LSDEffectS2CPacket decode(FriendlyByteBuf buffer) {
        return new LSDEffectS2CPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(LSDEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.remainingTicks <= 0 || packet.totalTicks <= 0) {
                ShaderUtils.deactivateLSDShaders();
                return;
            }
            ShaderUtils.activateLSDShaders(packet.remainingTicks, packet.totalTicks, packet.strength);
        });
        context.setPacketHandled(true);
    }
}
