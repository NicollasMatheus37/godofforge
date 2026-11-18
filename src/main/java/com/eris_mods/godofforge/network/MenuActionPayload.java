package com.eris_mods.godofforge.network;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.menu.ActionMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the player pressed a button in one of our GUIs.
 *
 * @param containerId id of the open menu (ignored if it doesn't match, e.g. after closing)
 * @param action      button id, interpreted by the menu ({@link ActionMenu#handleAction})
 */
public record MenuActionPayload(int containerId, int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MenuActionPayload> TYPE =
            new CustomPacketPayload.Type<>(GodOfForge.id("menu_action"));

    public static final StreamCodec<ByteBuf, MenuActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MenuActionPayload::containerId,
            ByteBufCodecs.VAR_INT, MenuActionPayload::action,
            MenuActionPayload::new);

    @Override
    public CustomPacketPayload.Type<MenuActionPayload> type() {
        return TYPE;
    }

    /** Runs on the server main thread. */
    public static void handle(MenuActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof ActionMenu menu
                && player.containerMenu.containerId == payload.containerId()
                && player.containerMenu.stillValid(player)) {
            menu.handleAction(player, payload.action());
        }
    }
}
