package com.eris_mods.godofforge.item;

import com.eris_mods.godofforge.registry.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A mold carved on the Mold Table. Its name shows the shape, e.g. "Wooden Mold (Pickaxe Head)". */
public class MoldItem extends Item {
    public MoldItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier pattern = stack.get(ModDataComponents.MOLD_PATTERN.get());
        if (pattern == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.godofforge.wooden_mold.with_pattern", patternName(pattern));
    }

    /** Translated name of a pattern, e.g. {@code godofforge:pickaxe_head} -> "Pickaxe Head". */
    public static Component patternName(Identifier pattern) {
        return Component.translatable("mold_pattern." + pattern.getNamespace() + "." + pattern.getPath());
    }
}
