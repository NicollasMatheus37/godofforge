package com.eris_mods.godofforge.network;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.menu.MoldTableMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: "make this known mold" (Mold Table normal mode).
 *
 * @param containerId id of the open menu
 * @param pattern     the mold pattern to make; the server checks the player really knows it
 */
public record MoldTableCarvePayload(int containerId, Identifier pattern) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MoldTableCarvePayload> TYPE =
            new CustomPacketPayload.Type<>(GodOfForge.id("mold_table_carve"));

    public static final StreamCodec<ByteBuf, MoldTableCarvePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoldTableCarvePayload::containerId,
            Identifier.STREAM_CODEC, MoldTableCarvePayload::pattern,
            MoldTableCarvePayload::new);

    @Override
    public CustomPacketPayload.Type<MoldTableCarvePayload> type() {
        return TYPE;
    }

    /** Runs on the server main thread. */
    public static void handle(MoldTableCarvePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof MoldTableMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
            menu.carveKnown(player, payload.pattern());
        }
    }
}
