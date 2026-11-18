package com.eris_mods.godofforge.knowledge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.eris_mods.godofforge.mastery.MasteryTier;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Everything a player knows about smithing: their mastery tier and the molds/parts they have learned.
 * <p>
 * Immutable: every change returns a new instance, which must be stored back with
 * {@link KnowledgeHelper#set} so NeoForge saves it and syncs it to the client.
 */
public record SmithingKnowledge(MasteryTier tier, Set<Identifier> knownPatterns) {
    public static final SmithingKnowledge EMPTY = new SmithingKnowledge(MasteryTier.APPRENTICE, Set.of());

    /** Saved inside the player data (survives logout and, thanks to copyOnDeath, death). */
    public static final MapCodec<SmithingKnowledge> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MasteryTier.CODEC.optionalFieldOf("tier", MasteryTier.APPRENTICE).forGetter(SmithingKnowledge::tier),
            Identifier.CODEC.listOf().optionalFieldOf("known_patterns", List.of()).forGetter(k -> List.copyOf(k.knownPatterns()))
    ).apply(instance, (tier, patterns) -> new SmithingKnowledge(tier, copyOf(patterns))));

    private static final StreamCodec<ByteBuf, HashSet<Identifier>> PATTERN_SET_STREAM_CODEC =
            ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC);

    /** Sent to the owning player's client. */
    public static final StreamCodec<ByteBuf, SmithingKnowledge> STREAM_CODEC = StreamCodec.composite(
            MasteryTier.STREAM_CODEC, SmithingKnowledge::tier,
            PATTERN_SET_STREAM_CODEC.map(SmithingKnowledge::copyOf, HashSet::new), SmithingKnowledge::knownPatterns,
            SmithingKnowledge::new);

    public boolean knows(Identifier pattern) {
        return knownPatterns.contains(pattern);
    }

    public SmithingKnowledge learn(Identifier pattern) {
        if (knows(pattern)) {
            return this;
        }
        Set<Identifier> next = new LinkedHashSet<>(knownPatterns);
        next.add(pattern);
        return new SmithingKnowledge(tier, Collections.unmodifiableSet(next));
    }

    public SmithingKnowledge forget(Identifier pattern) {
        if (!knows(pattern)) {
            return this;
        }
        Set<Identifier> next = new LinkedHashSet<>(knownPatterns);
        next.remove(pattern);
        return new SmithingKnowledge(tier, Collections.unmodifiableSet(next));
    }

    public SmithingKnowledge withTier(MasteryTier newTier) {
        return new SmithingKnowledge(newTier, knownPatterns);
    }

    private static Set<Identifier> copyOf(Collection<Identifier> patterns) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(patterns));
    }
}
