package com.eris_mods.godofforge;

import com.eris_mods.godofforge.client.screen.CrucibleScreen;
import com.eris_mods.godofforge.client.screen.MoldTableScreen;
import com.eris_mods.godofforge.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only entry point. This class is never loaded on a dedicated server,
 * so it is safe to reference client classes (screens, renderers) from here.
 */
@Mod(value = GodOfForge.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GodOfForge.MOD_ID, value = Dist.CLIENT)
public class GodOfForgeClient {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        GodOfForge.LOGGER.info("God of Forge: client setup done.");
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MOLD_TABLE.get(), MoldTableScreen::new);
        event.register(ModMenus.CRUCIBLE.get(), CrucibleScreen::new);
    }
}
