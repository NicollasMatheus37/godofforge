package com.eris_mods.godofforge.registry;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.menu.MoldTableMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Container menus (the server-side half of a GUI). */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GodOfForge.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MoldTableMenu>> MOLD_TABLE =
            MENUS.register("mold_table", () -> IMenuTypeExtension.create(MoldTableMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
