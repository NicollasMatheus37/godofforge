package com.eris_mods.godofforge.material;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** Mining tier of a head material, mirroring vanilla tool tiers. Applied to tools in step 1.8. */
public enum HarvestLevel {
    WOOD,
    STONE,
    IRON,
    DIAMOND,
    NETHERITE;

    public static final Codec<HarvestLevel> CODEC = Codec.STRING.comapFlatMap(HarvestLevel::byName, HarvestLevel::getSerializedName);

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DataResult<HarvestLevel> byName(String name) {
        for (HarvestLevel level : values()) {
            if (level.getSerializedName().equals(name)) {
                return DataResult.success(level);
            }
        }
        return DataResult.error(() -> "Unknown harvest level: " + name);
    }
}
