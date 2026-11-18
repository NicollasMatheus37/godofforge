package com.eris_mods.godofforge.registry;

import com.eris_mods.godofforge.GodOfForge;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All items of the mod. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GodOfForge.MOD_ID);

    /** Phase 0 "hello world" item. Will be removed once real content exists. */
    public static final DeferredItem<Item> TEST_INGOT = ITEMS.registerSimpleItem("test_ingot");

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
