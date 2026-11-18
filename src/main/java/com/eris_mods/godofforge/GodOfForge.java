package com.eris_mods.godofforge;

import org.slf4j.Logger;

import com.eris_mods.godofforge.registry.ModCreativeTabs;
import com.eris_mods.godofforge.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Entry point of God of Forge.
 * <p>
 * The value of {@link #MOD_ID} must match the {@code mod_id} in gradle.properties
 * and the entry in META-INF/neoforge.mods.toml.
 */
@Mod(GodOfForge.MOD_ID)
public class GodOfForge {
    public static final String MOD_ID = "godofforge";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * FML calls this constructor when the mod is loaded.
     * Every DeferredRegister must be attached to the mod event bus here.
     */
    public GodOfForge(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("God of Forge: the forge is lit.");
    }
}
