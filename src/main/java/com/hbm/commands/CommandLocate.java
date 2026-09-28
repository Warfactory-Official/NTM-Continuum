package com.hbm.commands;

import com.hbm.Constants;
import com.hbm.world.gen.nbt.NBTStructure;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.golem.core.Ids;
import dev.golem.core.command.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.TreeSet;

/**
 * 1.7 {@code CommandLocate}, {@code /ntmlocate structure <name>} and {@code /ntmlocate list}.
 *
 * <p>1.7 walked its own chunk-hash spawn scheme ring by ring out to 256 chunks. The port's structures are real
 * vanilla structures ({@code hbm:<name>}, owned by the worldgen family), whose placement vanilla already knows how
 * to search, so {@code structure} is a thin alias that runs {@code /locate structure hbm:<name>} for the sender and
 * vanilla reports the result. {@code list} names the NBT structures and the registered {@code hbm} structures.
 */
public final class CommandLocate {

    private CommandLocate() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        SuggestionProvider<CommandSourceStack> names = (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String name : names(ctx.getSource())) {
                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(name);
            }
            return builder.buildFuture();
        };
        return Commands.literal("ntmlocate").requires(Permissions.level(4))
                .executes(ctx -> usage(ctx.getSource()))
                .then(Commands.literal("list").executes(ctx -> {
                    String list = String.join(", ", names(ctx.getSource()));
                    ctx.getSource().sendSuccess(() -> Component.literal(list), false);
                    return 1;
                }))
                .then(Commands.literal("structure")
                        .executes(ctx -> usage(ctx.getSource()))
                        .then(Commands.argument("name", StringArgumentType.word()).suggests(names).executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.getPlayerOrException(); // 1.7: PlayerNotFoundException for anyone else
                            String name = StringArgumentType.getString(ctx, "name");
                            if (registered(source).contains(name)) {
                                source.getServer().getCommands().performPrefixedCommand(source,
                                        "locate structure " + Constants.MOD_ID + ":" + name);
                                return 1;
                            }
                            String key = NBTStructure.getStructure(name) != null
                                    ? "commands.locate.none_found" : "commands.locate.no_match";
                            source.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
                            return 0;
                        })));
    }

    private static int usage(CommandSourceStack source) {
        source.sendSystemMessage(Component.translatable("commands.hbm.ntmlocate.usage").withStyle(ChatFormatting.GREEN));
        return 0;
    }

    private static TreeSet<String> names(CommandSourceStack source) {
        TreeSet<String> names = new TreeSet<>(NBTStructure.listStructures());
        names.addAll(registered(source));
        return names;
    }

    /** Paths of every {@code hbm:} structure in the server's structure registry. */
    private static TreeSet<String> registered(CommandSourceStack source) {
        TreeSet<String> names = new TreeSet<>();
        source.getServer().registryAccess().lookupOrThrow(Registries.STRUCTURE).listElementIds()
                .map(Ids::keyId)
                .filter(id -> Constants.MOD_ID.equals(id.getNamespace()))
                .forEach(id -> names.add(id.getPath()));
        return names;
    }
}
