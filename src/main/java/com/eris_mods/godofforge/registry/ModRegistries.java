package com.eris_mods.godofforge.registry;

import com.eris_mods.godofforge.casting.MoldPattern;
import com.eris_mods.godofforge.material.ForgeMaterial;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;

/**
 * Data-pack registries: content defined in JSON so modpacks can add or change it.
 * <p>
 * These are loaded when a world is opened (a world restart is needed to apply changes,
 * {@code /reload} does not reload them).
 */
public final class ModRegistries {
    private ModRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModRegistries::onNewDatapackRegistries);
    }

    private static void onNewDatapackRegistries(NewDatapackRegistryEvent event) {
        // Synced to clients (second codec) so the Mold Table can list and preview known patterns
        // and the guide book can draw them. All crafting decisions are still validated on the server.
        event.worldRegistry(MoldPattern.REGISTRY_KEY, MoldPattern.CODEC, MoldPattern.CODEC);
        event.worldRegistry(ForgeMaterial.REGISTRY_KEY, ForgeMaterial.CODEC, ForgeMaterial.CODEC);
    }
}
