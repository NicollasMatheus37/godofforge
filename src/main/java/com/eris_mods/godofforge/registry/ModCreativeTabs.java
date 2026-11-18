package com.eris_mods.godofforge.registry;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.material.ForgeMaterial;
import com.eris_mods.godofforge.part.PartItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creative mode tabs of the mod. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GodOfForge.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.godofforge.main"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.MOLD_TABLE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.MOLD_TABLE.get());
                // every part in every head material (materials come from data packs)
                parameters.holders().lookupOrThrow(ForgeMaterial.REGISTRY_KEY).listElements()
                        .filter(material -> material.value().head().isPresent())
                        .forEach(material -> ModItems.PARTS.forEach(part ->
                                output.accept(PartItem.withMaterial(part.get(), material.key().identifier()))));
                output.accept(ModItems.TEST_INGOT.get());
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
