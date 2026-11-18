package com.eris_mods.godofforge.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.eris_mods.godofforge.casting.MoldPattern;
import com.eris_mods.godofforge.knowledge.KnowledgeHelper;
import com.eris_mods.godofforge.knowledge.SmithingKnowledge;
import com.eris_mods.godofforge.mastery.MasteryTier;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Debug/admin commands (require cheats / operator level 2):
 * <pre>
 * /godofforge knowledge list
 * /godofforge knowledge learn &lt;pattern&gt;
 * /godofforge knowledge forget &lt;pattern&gt;
 * /godofforge knowledge clear
 * /godofforge mastery get
 * /godofforge mastery set &lt;apprentice|journeyman|master&gt;
 * /godofforge patterns list
 * /godofforge patterns show &lt;pattern&gt;
 * </pre>
 */
public final class ModCommands {
    private ModCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("godofforge")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("knowledge")
                        .then(Commands.literal("list").executes(ModCommands::listKnowledge))
                        .then(Commands.literal("learn")
                                .then(Commands.argument("pattern", IdentifierArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(patterns(ctx).keySet(), builder))
                                        .executes(ModCommands::learn)))
                        .then(Commands.literal("forget")
                                .then(Commands.argument("pattern", IdentifierArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                KnowledgeHelper.get(ctx.getSource().getPlayerOrException()).knownPatterns(), builder))
                                        .executes(ModCommands::forget)))
                        .then(Commands.literal("clear").executes(ModCommands::clear)))
                .then(Commands.literal("mastery")
                        .then(Commands.literal("get").executes(ModCommands::getMastery))
                        .then(Commands.literal("set")
                                .then(Commands.argument("tier", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(MasteryTier.values()).map(MasteryTier::getSerializedName), builder))
                                        .executes(ModCommands::setMastery))))
                .then(Commands.literal("patterns")
                        .then(Commands.literal("list").executes(ModCommands::listPatterns))
                        .then(Commands.literal("show")
                                .then(Commands.argument("pattern", IdentifierArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(patterns(ctx).keySet(), builder))
                                        .executes(ModCommands::showPattern)))));
    }

    // ---- knowledge ----

    private static int listKnowledge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SmithingKnowledge knowledge = KnowledgeHelper.get(player);
        String known = knowledge.knownPatterns().isEmpty()
                ? "(none)"
                : knowledge.knownPatterns().stream().map(Identifier::toString).sorted().collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.literal("Mastery: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(knowledge.tier().getSerializedName()).withStyle(ChatFormatting.WHITE)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Known patterns (" + knowledge.knownPatterns().size() + "): ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(known).withStyle(ChatFormatting.WHITE)), false);
        return knowledge.knownPatterns().size();
    }

    private static int learn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Identifier pattern = IdentifierArgument.getId(ctx, "pattern");
        if (patterns(ctx).getValue(pattern) == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown mold pattern: " + pattern));
            return 0;
        }
        KnowledgeHelper.update(player, k -> k.learn(pattern));
        ctx.getSource().sendSuccess(() -> Component.literal("Learned " + pattern), false);
        return 1;
    }

    private static int forget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Identifier pattern = IdentifierArgument.getId(ctx, "pattern");
        KnowledgeHelper.update(player, k -> k.forget(pattern));
        ctx.getSource().sendSuccess(() -> Component.literal("Forgot " + pattern), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        KnowledgeHelper.set(player, SmithingKnowledge.EMPTY);
        ctx.getSource().sendSuccess(() -> Component.literal("Smithing knowledge reset (apprentice, no patterns)"), false);
        return 1;
    }

    // ---- mastery ----

    private static int getMastery(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MasteryTier tier = KnowledgeHelper.get(player).tier();
        ctx.getSource().sendSuccess(() -> Component.literal("Mastery: " + tier.getSerializedName()), false);
        return tier.ordinal() + 1;
    }

    private static int setMastery(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "tier");
        var result = MasteryTier.byName(name);
        if (result.result().isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown tier: " + name + " (use apprentice, journeyman or master)"));
            return 0;
        }
        MasteryTier tier = result.result().get();
        KnowledgeHelper.update(player, k -> k.withTier(tier));
        ctx.getSource().sendSuccess(() -> Component.literal("Mastery set to " + tier.getSerializedName()), false);
        return 1;
    }

    // ---- patterns ----

    private static int listPatterns(CommandContext<CommandSourceStack> ctx) {
        Registry<MoldPattern> registry = patterns(ctx);
        List<Identifier> ids = registry.keySet().stream().sorted().toList();
        ctx.getSource().sendSuccess(() -> Component.literal("Mold patterns loaded: " + ids.size()).withStyle(ChatFormatting.GOLD), false);
        for (Identifier id : ids) {
            MoldPattern pattern = registry.getValue(id);
            ctx.getSource().sendSuccess(() -> Component.literal(" - " + id + " [" + pattern.material().getSerializedName()
                    + ", " + pattern.tier().getSerializedName() + "] -> " + pattern.part()), false);
        }
        return ids.size();
    }

    private static int showPattern(CommandContext<CommandSourceStack> ctx) {
        Identifier id = IdentifierArgument.getId(ctx, "pattern");
        MoldPattern pattern = patterns(ctx).getValue(id);
        if (pattern == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown mold pattern: " + id));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(id + " (" + pattern.material().getSerializedName() + ") -> " + pattern.part())
                .withStyle(ChatFormatting.GOLD), false);
        for (String row : pattern.shape()) {
            // '#' = material kept, '.' = carved (spaces are invisible in chat)
            String visible = row.replace(MoldPattern.CARVED, '.').replace("#", "█");
            ctx.getSource().sendSuccess(() -> Component.literal(visible), false);
        }
        return 1;
    }

    private static Registry<MoldPattern> patterns(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer().registryAccess().lookupOrThrow(MoldPattern.REGISTRY_KEY);
    }
}
