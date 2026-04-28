package dev.propulsionteam.propulsionsimulated.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

//Better than PacketHandler
public class PropulsionPackets {
    public static void register() {
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
    }

    public static <MSG> void sendToAll(MSG message) {
    }

    public static <MSG> void sendToTracking(MSG message, LevelChunk chunk) {
    }


    public static <MSG> void sendToServer(MSG message) {
    }
}
