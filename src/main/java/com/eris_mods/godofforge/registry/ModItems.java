package com.eris_mods.godofforge.registry;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.item.MoldItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All items of the mod. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GodOfForge.MOD_ID);

    /** Phase 0 "hello world" item. Will be removed in step 1.10. */
    public static final DeferredItem<Item> TEST_INGOT = ITEMS.registerSimpleItem("test_ingot");

    public static final DeferredItem<BlockItem> MOLD_TABLE = ITEMS.registerSimpleBlockItem("mold_table", ModBlocks.MOLD_TABLE);

    /** A wooden mold. Which shape it has is stored in the {@link ModDataComponents#MOLD_PATTERN} component. */
    public static final DeferredItem<MoldItem> WOODEN_MOLD = ITEMS.registerItem("wooden_mold", MoldItem::new,
            properties -> properties.stacksTo(1));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
