package com.minecraft.handytoolbox.network;

import com.minecraft.handytoolbox.HandyToolbox;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HandyToolbox.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Network() {
    }

    public static void register() {
        CHANNEL.messageBuilder(BorrowToolsPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BorrowToolsPacket::encode)
                .decoder(BorrowToolsPacket::decode)
                .consumerMainThread(BorrowToolsPacket::handle)
                .add();
    }
}
