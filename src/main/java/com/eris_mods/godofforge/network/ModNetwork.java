package com.eris_mods.godofforge.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the mod's custom network packets. Bump the version when packet formats change. */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";

    private ModNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToServer(MoldTableActionPayload.TYPE, MoldTableActionPayload.STREAM_CODEC, MoldTableActionPayload::handle)
                .playToServer(MoldTableCarvePayload.TYPE, MoldTableCarvePayload.STREAM_CODEC, MoldTableCarvePayload::handle);
    }
}
