package com.eris_mods.godofforge;

import org.slf4j.Logger;

import com.eris_mods.godofforge.command.ModCommands;
import com.eris_mods.godofforge.network.ModNetwork;
import com.eris_mods.godofforge.registry.ModAttachments;
import com.eris_mods.godofforge.registry.ModBlockEntities;
import com.eris_mods.godofforge.registry.ModBlocks;
import com.eris_mods.godofforge.registry.ModCreativeTabs;
import com.eris_mods.godofforge.registry.ModDataComponents;
import com.eris_mods.godofforge.registry.ModItems;
import com.eris_mods.godofforge.registry.ModMenus;
import com.eris_mods.godofforge.registry.ModRegistries;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

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
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModRegistries.register(modEventBus);
        ModNetwork.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(ModCommands::register);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("God of Forge: the forge is lit.");
    }

    /** Builds an identifier in the mod's namespace, e.g. {@code id("mold_pattern")} -> {@code godofforge:mold_pattern}. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
