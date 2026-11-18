package com.eris_mods.godofforge.part;

import java.util.Optional;
import java.util.function.Consumer;

import com.eris_mods.godofforge.material.ForgeMaterial;
import com.eris_mods.godofforge.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A tool part (pickaxe head, axe head...). The material is stored in the {@link ModDataComponents#MATERIAL} component,
 * so one item covers every material: "Copper Pickaxe Head", "Gold Pickaxe Head"...
 * The texture per material is chosen in {@code assets/godofforge/items/<part>.json}.
 */
public class PartItem extends Item {
    public PartItem(Properties properties) {
        super(properties);
    }

    public static ItemStack withMaterial(Item part, Identifier material) {
        ItemStack stack = new ItemStack(part);
        stack.set(ModDataComponents.MATERIAL.get(), material);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier material = stack.get(ModDataComponents.MATERIAL.get());
        Component base = super.getName(stack);
        if (material == null) {
            return base;
        }
        // e.g. "item.godofforge.pickaxe_head" + ".with_material" -> "%s Pickaxe Head" / "Cabeça de Picareta de %s"
        return Component.translatable("item.godofforge.part.with_material", base, Component.translatable(ForgeMaterial.translationKey(material)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        Identifier materialId = stack.get(ModDataComponents.MATERIAL.get());
        if (materialId == null) {
            return;
        }
        HolderLookup.Provider registries = context.registries();
        if (registries == null) {
            return;
        }
        Optional<ForgeMaterial.HeadStats> head = registries.lookupOrThrow(ForgeMaterial.REGISTRY_KEY)
                .get(ResourceKey.create(ForgeMaterial.REGISTRY_KEY, materialId))
                .flatMap(holder -> holder.value().head());
        if (head.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.godofforge.part.unknown_material", materialId.toString()).withStyle(ChatFormatting.RED));
            return;
        }
        ForgeMaterial.HeadStats stats = head.get();
        tooltip.accept(Component.translatable("tooltip.godofforge.part.durability", stats.durability()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.godofforge.part.mining_speed", format(stats.miningSpeed())).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.godofforge.part.attack_damage", format(stats.attackDamage())).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.godofforge.part.harvest_level",
                Component.translatable("harvest_level.godofforge." + stats.harvestLevel().getSerializedName())).withStyle(ChatFormatting.GRAY));
    }

    private static String format(float value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
