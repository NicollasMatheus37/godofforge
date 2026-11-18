package com.eris_mods.godofforge.knowledge;

import java.util.function.UnaryOperator;

import com.eris_mods.godofforge.registry.ModAttachments;

import net.minecraft.world.entity.player.Player;

/** Convenience access to a player's {@link SmithingKnowledge}. */
public final class KnowledgeHelper {
    private KnowledgeHelper() {
    }

    public static SmithingKnowledge get(Player player) {
        return player.getData(ModAttachments.SMITHING_KNOWLEDGE);
    }

    /** Stores the new value. On the server this also syncs it to the player's client. */
    public static void set(Player player, SmithingKnowledge knowledge) {
        player.setData(ModAttachments.SMITHING_KNOWLEDGE, knowledge);
    }

    public static SmithingKnowledge update(Player player, UnaryOperator<SmithingKnowledge> change) {
        SmithingKnowledge updated = change.apply(get(player));
        set(player, updated);
        return updated;
    }
}
