package com.eris_mods.godofforge.menu;

import net.minecraft.server.level.ServerPlayer;

/** A menu that reacts to simple numbered button presses sent by its screen (see {@code MenuActionPayload}). */
public interface ActionMenu {
    /** Called on the server main thread. The action id meaning is defined by each menu. */
    void handleAction(ServerPlayer player, int action);
}
