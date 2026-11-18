package com.eris_mods.godofforge.casting;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** What a mold is shaped from on the Mold Table. */
public enum MoldMaterial {
    /** Simple metals (copper, tin, lead, gold). */
    WOOD,
    /** Complex metals and alloys (bronze...). Must be fired in a kiln. */
    CLAY;

    public static final Codec<MoldMaterial> CODEC = Codec.STRING.comapFlatMap(MoldMaterial::byName, MoldMaterial::getSerializedName);

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DataResult<MoldMaterial> byName(String name) {
        for (MoldMaterial material : values()) {
            if (material.getSerializedName().equals(name)) {
                return DataResult.success(material);
            }
        }
        return DataResult.error(() -> "Unknown mold material: " + name);
    }
}
