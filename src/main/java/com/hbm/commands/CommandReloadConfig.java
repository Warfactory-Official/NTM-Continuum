package com.hbm.commands;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.golem.core.command.CompatCommands.ClientFeedback;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 1.7 {@code CommandReloadConfig}, the shared body of {@code /ntmserver} and {@code /ntmclient}: {@code help [command]},
 * {@code list}, {@code reload}, {@code get <name>}, {@code set <name> <value>}. Written against Brigadier's source
 * type so the server command and the client-only command share it; 1.7 let any <i>player</i> use both.
 */
public final class CommandReloadConfig {

    private CommandReloadConfig() {}

    /** What distinguishes the two commands. */
    public record Target(String name, String side, Supplier<Map<String, ConfigWrapper<?>>> map, Runnable refresh,
                         Runnable reload) {}

    public static <S> LiteralArgumentBuilder<S> build(Target target, Predicate<S> requirement, ClientFeedback<S> feedback) {
        SuggestionProvider<S> keys = (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String key : target.map().get().keySet()) {
                if (key.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(key);
            }
            return builder.buildFuture();
        };
        SuggestionProvider<S> operators = (ctx, builder) -> {
            for (String op : new String[] {"help", "list", "reload", "get", "set"}) {
                if (op.startsWith(builder.getRemaining())) builder.suggest(op);
            }
            return builder.buildFuture();
        };
        return LiteralArgumentBuilder.<S>literal(target.name())
                .requires(requirement)
                // 1.7: no arguments is a CommandException carrying the usage
                .executes(ctx -> fail(ctx, feedback, Component.translatable("commands.hbm.config.usage", target.name())))
                .then(LiteralArgumentBuilder.<S>literal("help")
                        .executes(ctx -> help(ctx, target, feedback, null))
                        .then(RequiredArgumentBuilder.<S, String>argument("command", StringArgumentType.word())
                                .suggests(operators)
                                .executes(ctx -> help(ctx, target, feedback, StringArgumentType.getString(ctx, "command")))))
                .then(LiteralArgumentBuilder.<S>literal("list").executes(ctx -> {
                    feedback.success(ctx.getSource(), Component.translatable("commands.hbm.config.title." + target.side())
                            .withStyle(ChatFormatting.RED));
                    for (Map.Entry<String, ConfigWrapper<?>> line : target.map().get().entrySet()) {
                        feedback.success(ctx.getSource(), Component.literal("  ").append(line(line.getKey(), line.getValue())));
                    }
                    return target.map().get().size();
                }))
                .then(LiteralArgumentBuilder.<S>literal("reload").executes(ctx -> {
                    target.reload().run();
                    feedback.success(ctx.getSource(), Component.translatable("commands.hbm.config.reloaded")
                            .withStyle(ChatFormatting.YELLOW));
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<S>literal("get")
                        .executes(ctx -> fail(ctx, feedback, Component.translatable("commands.hbm.config.usage", target.name())))
                        .then(RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.word()).suggests(keys)
                                .executes(ctx -> {
                                    String key = StringArgumentType.getString(ctx, "name");
                                    ConfigWrapper<?> wrapper = target.map().get().get(key);
                                    if (wrapper == null) return fail(ctx, feedback, Component.translatable("commands.hbm.config.no_key"));
                                    feedback.success(ctx.getSource(), line(key, wrapper));
                                    return 1;
                                })))
                .then(LiteralArgumentBuilder.<S>literal("set")
                        .executes(ctx -> fail(ctx, feedback, Component.translatable("commands.hbm.config.usage", target.name())))
                        .then(RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.word()).suggests(keys)
                                .executes(ctx -> fail(ctx, feedback, Component.translatable("commands.hbm.config.usage", target.name())))
                                .then(RequiredArgumentBuilder.<S, String>argument("value", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String key = StringArgumentType.getString(ctx, "name");
                                            ConfigWrapper<?> wrapper = target.map().get().get(key);
                                            if (wrapper == null) return fail(ctx, feedback, Component.translatable("commands.hbm.config.no_key"));
                                            try {
                                                wrapper.update(StringArgumentType.getString(ctx, "value"));
                                                target.refresh().run();
                                            } catch (Exception ex) {
                                                return fail(ctx, feedback, Component.translatable("commands.hbm.config.parse_error",
                                                        wrapper.get().getClass().getSimpleName(), String.valueOf(ex.getLocalizedMessage())));
                                            }
                                            feedback.success(ctx.getSource(), Component.translatable("commands.hbm.config.updated")
                                                    .withStyle(ChatFormatting.YELLOW));
                                            return 1;
                                        }))));
    }

    private static MutableComponent line(String key, ConfigWrapper<?> wrapper) {
        return Component.literal(key + ": ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.valueOf(wrapper.get())).withStyle(ChatFormatting.YELLOW));
    }

    /** 1.7 {@code help}: one line for a named subcommand, else the usage of all five. */
    private static <S> int help(CommandContext<S> ctx, Target target, ClientFeedback<S> feedback, String command) {
        if (command != null) {
            switch (command) {
                case "help", "list", "reload", "get", "set" -> feedback.success(ctx.getSource(), Component.translatable(
                        "commands.hbm.config.help." + command, target.name(), target.side()).withStyle(ChatFormatting.YELLOW));
                default -> { }
            }
            return 1;
        }
        String[][] usage = {{"help ", "<command>"}, {"list", ""}, {"reload", ""}, {"get ", "<name>"}, {"set ", "<name> <value>"}};
        for (String[] line : usage) {
            feedback.success(ctx.getSource(), Component.literal("/" + target.name() + " ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(line[0]).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(line[1]).withStyle(ChatFormatting.RED)));
        }
        return 1;
    }

    private static <S> int fail(CommandContext<S> ctx, ClientFeedback<S> feedback, Component message) {
        feedback.error(ctx.getSource(), message);
        return 0;
    }
}
