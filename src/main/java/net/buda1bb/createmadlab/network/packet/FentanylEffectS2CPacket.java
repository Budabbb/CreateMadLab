package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FentanylEffectS2CPacket {
    private final boolean active;
    private final int remainingTicks;
    private final int totalTicks;
    private final boolean fadeOut;
    private final float startIntensity;
    private final float startBlackoutAlpha;
    private final float visualStrength;

    public FentanylEffectS2CPacket(boolean active, int remainingTicks, int totalTicks) {
        this(active, remainingTicks, totalTicks, false, 0.0F, 0.0F, 1.0F);
    }

    public FentanylEffectS2CPacket(boolean active, int remainingTicks, int totalTicks, boolean fadeOut, float startIntensity, float startBlackoutAlpha) {
        this(active, remainingTicks, totalTicks, fadeOut, startIntensity, startBlackoutAlpha, 1.0F);
    }

    public FentanylEffectS2CPacket(boolean active, int remainingTicks, int totalTicks, boolean fadeOut,
                                   float startIntensity, float startBlackoutAlpha, float visualStrength) {
        this.active = active;
        this.remainingTicks = remainingTicks;
        this.totalTicks = totalTicks;
        this.fadeOut = fadeOut;
        this.startIntensity = startIntensity;
        this.startBlackoutAlpha = startBlackoutAlpha;
        this.visualStrength = visualStrength;
    }

    public static void encode(FentanylEffectS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeBoolean(packet.fadeOut);
        buffer.writeFloat(packet.startIntensity);
        buffer.writeFloat(packet.startBlackoutAlpha);
        buffer.writeFloat(packet.visualStrength);
    }

    public static FentanylEffectS2CPacket decode(FriendlyByteBuf buffer) {
        return new FentanylEffectS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(FentanylEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateFentanylShaders(packet.remainingTicks, packet.totalTicks,
                        packet.fadeOut, packet.startIntensity, packet.startBlackoutAlpha, packet.visualStrength);
            } else {
                ShaderUtils.deactivateFentanylShaders();
            }
        });
        context.setPacketHandled(true);
    }
}
