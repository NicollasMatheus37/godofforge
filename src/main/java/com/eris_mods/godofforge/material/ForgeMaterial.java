package com.eris_mods.godofforge.material;

import java.util.Optional;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.casting.MoldMaterial;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A material parts can be made of. Loaded from data packs at
 * {@code data/<namespace>/godofforge/material/<name>.json} and synced to clients (tooltips).
 * <p>
 * A material can be used as a tool head ({@link #head}), as a handle ({@link #handle}), or both.
 */
public record ForgeMaterial(Optional<HeadStats> head, Optional<HandleStats> handle) {
    public static final ResourceKey<Registry<ForgeMaterial>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(GodOfForge.id("material"));

    public static final Codec<ForgeMaterial> CODEC = RecordCodecBuilder.<ForgeMaterial>create(instance -> instance.group(
            HeadStats.CODEC.optionalFieldOf("head").forGetter(ForgeMaterial::head),
            HandleStats.CODEC.optionalFieldOf("handle").forGetter(ForgeMaterial::handle)
    ).apply(instance, ForgeMaterial::new)).validate(material -> material.head().isPresent() || material.handle().isPresent()
            ? DataResult.success(material)
            : DataResult.error(() -> "A material needs a \"head\" and/or a \"handle\" section"));

    /** Translation key of a material name, e.g. {@code material.godofforge.copper}. */
    public static String translationKey(Identifier id) {
        return "material." + id.getNamespace() + "." + id.getPath();
    }

    /**
     * Stats when used as a tool head / blade.
     *
     * @param durability    base durability of a tool with this head
     * @param miningSpeed   mining speed (vanilla reference: stone 4, iron 6, gold 12)
     * @param attackDamage  bonus attack damage
     * @param harvestLevel  which blocks it can mine, like vanilla tiers: wood, stone, iron, diamond, netherite
     * @param meltingPoint  in degrees Celsius; used by the crucible (step 1.7)
     * @param mold          which mold material can hold this metal when cast
     */
    public record HeadStats(int durability, float miningSpeed, float attackDamage, HarvestLevel harvestLevel,
                            int meltingPoint, MoldMaterial mold) {
        public static final Codec<HeadStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 100_000).fieldOf("durability").forGetter(HeadStats::durability),
                Codec.floatRange(0f, 100f).fieldOf("mining_speed").forGetter(HeadStats::miningSpeed),
                Codec.floatRange(0f, 100f).fieldOf("attack_damage").forGetter(HeadStats::attackDamage),
                HarvestLevel.CODEC.fieldOf("harvest_level").forGetter(HeadStats::harvestLevel),
                Codec.intRange(0, 5000).fieldOf("melting_point").forGetter(HeadStats::meltingPoint),
                MoldMaterial.CODEC.fieldOf("mold").forGetter(HeadStats::mold)
        ).apply(instance, HeadStats::new));
    }

    /**
     * Stats when used as a handle. Handles are plain vanilla items (stick, bone...).
     *
     * @param item                 the vanilla item used as this handle
     * @param durabilityMultiplier multiplies the head durability
     * @param speedMultiplier      multiplies the head mining speed
     */
    public record HandleStats(Identifier item, float durabilityMultiplier, float speedMultiplier) {
        public static final Codec<HandleStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("item").forGetter(HandleStats::item),
                Codec.floatRange(0.1f, 10f).optionalFieldOf("durability_multiplier", 1f).forGetter(HandleStats::durabilityMultiplier),
                Codec.floatRange(0.1f, 10f).optionalFieldOf("speed_multiplier", 1f).forGetter(HandleStats::speedMultiplier)
        ).apply(instance, HandleStats::new));
    }
}
