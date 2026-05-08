package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FentanylEffectS2CPacket {
    private final boolean active;
    private final int remainingTicks;
    private final int totalTicks;

    public FentanylEffectS2CPacket(boolean active, int remainingTicks, int totalTicks) {
        this.active = active;
        this.remainingTicks = remainingTicks;
        this.totalTicks = totalTicks;
    }

    public static void encode(FentanylEffectS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
    }

    public static FentanylEffectS2CPacket decode(FriendlyByteBuf buffer) {
        return new FentanylEffectS2CPacket(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(FentanylEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateFentanylOverdoseShaders(packet.remainingTicks, packet.totalTicks);
            } else {
                ShaderUtils.deactivateFentanylOverdoseShaders();
            }
        });
        context.setPacketHandled(true);
    }
}
