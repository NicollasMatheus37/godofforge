package com.eris_mods.godofforge.mastery;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The three ranks of smithing mastery. Order matters: higher ordinal = higher rank. */
public enum MasteryTier {
    APPRENTICE,
    JOURNEYMAN,
    MASTER;

    /** Reads/writes the tier as a lowercase string, e.g. "journeyman". */
    public static final Codec<MasteryTier> CODEC = Codec.STRING.comapFlatMap(MasteryTier::byName, MasteryTier::getSerializedName);

    public static final StreamCodec<ByteBuf, MasteryTier> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> values()[i], MasteryTier::ordinal);

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isAtLeast(MasteryTier other) {
        return ordinal() >= other.ordinal();
    }

    public static DataResult<MasteryTier> byName(String name) {
        for (MasteryTier tier : values()) {
            if (tier.getSerializedName().equals(name)) {
                return DataResult.success(tier);
            }
        }
        return DataResult.error(() -> "Unknown mastery tier: " + name);
    }
}
