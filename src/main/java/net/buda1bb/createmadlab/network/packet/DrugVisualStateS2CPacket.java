package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DrugVisualStateS2CPacket {
    private final boolean morphineActive;
    private final boolean heroinActive;
    private final boolean fentanylActive;
    private final boolean lsdActive;
    private final boolean opioidOverdoseActive;
    private final boolean withdrawalActive;

    public DrugVisualStateS2CPacket(boolean morphineActive, boolean heroinActive, boolean fentanylActive, boolean lsdActive,
                                    boolean opioidOverdoseActive, boolean withdrawalActive) {
        this.morphineActive = morphineActive;
        this.heroinActive = heroinActive;
        this.fentanylActive = fentanylActive;
        this.lsdActive = lsdActive;
        this.opioidOverdoseActive = opioidOverdoseActive;
        this.withdrawalActive = withdrawalActive;
    }

    public static void encode(DrugVisualStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.morphineActive);
        buffer.writeBoolean(packet.heroinActive);
        buffer.writeBoolean(packet.fentanylActive);
        buffer.writeBoolean(packet.lsdActive);
        buffer.writeBoolean(packet.opioidOverdoseActive);
        buffer.writeBoolean(packet.withdrawalActive);
    }

    public static DrugVisualStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new DrugVisualStateS2CPacket(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(DrugVisualStateS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
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
        context.setPacketHandled(true);
    }
}
