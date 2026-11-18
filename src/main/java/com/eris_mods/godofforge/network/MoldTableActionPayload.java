package com.eris_mods.godofforge.network;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.menu.MoldTableMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the player clicked something in the Mold Table GUI.
 *
 * @param containerId id of the open menu (ignored if it doesn't match, e.g. after closing)
 * @param action      0..24 = carve that grid cell, or {@link MoldTableMenu#ACTION_CONFIRM} / {@link MoldTableMenu#ACTION_RESET}
 */
public record MoldTableActionPayload(int containerId, int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MoldTableActionPayload> TYPE =
            new CustomPacketPayload.Type<>(GodOfForge.id("mold_table_action"));

    public static final StreamCodec<ByteBuf, MoldTableActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoldTableActionPayload::containerId,
            ByteBufCodecs.VAR_INT, MoldTableActionPayload::action,
            MoldTableActionPayload::new);

    @Override
    public CustomPacketPayload.Type<MoldTableActionPayload> type() {
        return TYPE;
    }

    /** Runs on the server main thread. */
    public static void handle(MoldTableActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof MoldTableMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
            menu.handleAction(player, payload.action());
        }
    }
}
