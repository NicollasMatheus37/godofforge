package com.eris_mods.godofforge.registry;

import com.eris_mods.godofforge.GodOfForge;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data stored on item stacks. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GodOfForge.MOD_ID);

    /** Which mold pattern a mold item was carved with, e.g. {@code godofforge:pickaxe_head}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> MOLD_PATTERN =
            COMPONENTS.registerComponentType("mold_pattern", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    /** Which {@code ForgeMaterial} a part is made of, e.g. {@code godofforge:copper}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> MATERIAL =
            COMPONENTS.registerComponentType("material", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    private ModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
