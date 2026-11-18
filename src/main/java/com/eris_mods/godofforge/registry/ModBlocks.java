package com.eris_mods.godofforge.registry;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.block.CrucibleBlock;
import com.eris_mods.godofforge.block.MoldTableBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All blocks of the mod. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GodOfForge.MOD_ID);

    public static final DeferredBlock<MoldTableBlock> MOLD_TABLE = BLOCKS.registerBlock("mold_table", MoldTableBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));

    /** Not a full cube (cauldron-like model), hence noOcclusion. */
    public static final DeferredBlock<CrucibleBlock> CRUCIBLE = BLOCKS.registerBlock("crucible", CrucibleBlock::new,
            properties -> properties.mapColor(MapColor.TERRACOTTA_ORANGE).strength(2.0f).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().noOcclusion());

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
