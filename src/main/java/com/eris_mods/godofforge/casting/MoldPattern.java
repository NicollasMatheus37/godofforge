package com.eris_mods.godofforge.casting;

import java.util.List;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.mastery.MasteryTier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A shape that can be carved on the Mold Table (research mode) to produce a mold.
 * <p>
 * Loaded from data packs at {@code data/<namespace>/godofforge/mold_pattern/<name>.json}.
 * The shape is a {@value #SIZE}x{@value #SIZE} grid where {@code '#'} means "material stays"
 * and {@code ' '} (space) means "carved away" (the cavity where the metal will flow).
 *
 * @param material what the mold is made of
 * @param tier     minimum mastery tier for this pattern
 * @param part     id of the part this mold casts (the part items are added in step 1.6)
 * @param shape    {@value #SIZE} rows of {@value #SIZE} characters
 */
public record MoldPattern(MoldMaterial material, MasteryTier tier, Identifier part, List<String> shape) {
    public static final int SIZE = 5;
    public static final char SOLID = '#';
    public static final char CARVED = ' ';

    public static final ResourceKey<Registry<MoldPattern>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(GodOfForge.id("mold_pattern"));

    public static final Codec<MoldPattern> CODEC = RecordCodecBuilder.<MoldPattern>create(instance -> instance.group(
            MoldMaterial.CODEC.fieldOf("material").forGetter(MoldPattern::material),
            MasteryTier.CODEC.optionalFieldOf("tier", MasteryTier.APPRENTICE).forGetter(MoldPattern::tier),
            Identifier.CODEC.fieldOf("part").forGetter(MoldPattern::part),
            Codec.STRING.listOf().fieldOf("shape").forGetter(MoldPattern::shape)
    ).apply(instance, MoldPattern::new)).validate(MoldPattern::validate);

    private static DataResult<MoldPattern> validate(MoldPattern pattern) {
        if (pattern.shape().size() != SIZE) {
            return DataResult.error(() -> "Mold shape must have " + SIZE + " rows, found " + pattern.shape().size());
        }
        boolean anyCarved = false;
        for (String row : pattern.shape()) {
            if (row.length() != SIZE) {
                return DataResult.error(() -> "Every mold shape row must have " + SIZE + " characters: \"" + row + "\"");
            }
            for (char c : row.toCharArray()) {
                if (c != SOLID && c != CARVED) {
                    return DataResult.error(() -> "Mold shape may only contain '#' and ' ': \"" + row + "\"");
                }
                anyCarved |= c == CARVED;
            }
        }
        if (!anyCarved) {
            return DataResult.error(() -> "Mold shape must carve at least one cell");
        }
        return DataResult.success(pattern);
    }

    /** True if the cell at (column, row) is carved away. */
    public boolean isCarved(int column, int row) {
        return shape.get(row).charAt(column) == CARVED;
    }

    /**
     * Compares a carved grid from the Mold Table with this pattern.
     *
     * @param carved {@code carved[row * SIZE + column]} is true when that cell was removed
     */
    public boolean matches(boolean[] carved) {
        if (carved.length != SIZE * SIZE) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (carved[row * SIZE + column] != isCarved(column, row)) {
                    return false;
                }
            }
        }
        return true;
    }
}
