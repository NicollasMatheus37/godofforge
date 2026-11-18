package com.eris_mods.godofforge.registry;

import com.eris_mods.godofforge.GodOfForge;
import com.eris_mods.godofforge.knowledge.SmithingKnowledge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attached to game objects (players, block entities...). */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GodOfForge.MOD_ID);

    /**
     * Per-player smithing knowledge.
     * <ul>
     *   <li>serialize: saved with the player</li>
     *   <li>copyOnDeath: kept after respawning</li>
     *   <li>sync: sent only to the player who owns it (others don't need to know)</li>
     * </ul>
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SmithingKnowledge>> SMITHING_KNOWLEDGE =
            ATTACHMENT_TYPES.register("smithing_knowledge", () -> AttachmentType.builder(() -> SmithingKnowledge.EMPTY)
                    .serialize(SmithingKnowledge.MAP_CODEC)
                    .copyOnDeath()
                    .sync((holder, player) -> holder == player, SmithingKnowledge.STREAM_CODEC)
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
