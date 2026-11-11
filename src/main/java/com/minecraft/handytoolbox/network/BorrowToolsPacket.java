package com.minecraft.handytoolbox.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.minecraft.handytoolbox.ToolLending;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -> server: "take these tools out of the toolbox and put them in these slots". Sent right before JEI's
 * transfer packet (same connection, so it arrives first), so JEI finds the tools in the inventory.
 */
public record BorrowToolsPacket(List<ToolLending.Move> moves) {
    private static final int MAX_MOVES = ToolLending.MAIN_SLOTS;

    public static void encode(BorrowToolsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.moves.size());
        for (ToolLending.Move move : packet.moves) {
            buf.writeVarInt(move.boxSlot());
            buf.writeVarInt(move.innerSlot());
            buf.writeVarInt(move.destSlot());
        }
    }

    public static BorrowToolsPacket decode(FriendlyByteBuf buf) {
        int size = Math.min(buf.readVarInt(), MAX_MOVES);
        List<ToolLending.Move> moves = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            moves.add(new ToolLending.Move(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new BorrowToolsPacket(moves);
    }

    public static void handle(BorrowToolsPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) {
            for (ToolLending.Move move : packet.moves) {
                ToolLending.apply(player, move);
                // the client predicted this change; after JEI's transfer the server sends the real state
                ToolLending.forceResync(player, move.boxSlot(), move.destSlot());
            }
        }
        context.get().setPacketHandled(true);
    }
}
