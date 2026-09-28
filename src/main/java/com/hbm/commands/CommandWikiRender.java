package com.hbm.commands;

import com.hbm.Constants;
import com.hbm.inventory.gui.GUIScreenWikiRender;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.golem.core.command.CompatCommands.ClientFeedback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * 1.7 {@code CommandWikiRender}, the client command {@code /ntmwikirender <type>}: renders a 256px icon of every
 * {@code hbm} item whose class (or whose block's class) is named {@code type}, e.g. {@code ItemGunBaseNT}, into
 * {@code wiki-block-renders-256/}. For developers and wiki editors. The class names are suggested.
 */
public final class CommandWikiRender {

    private CommandWikiRender() {}

    public static <S> LiteralArgumentBuilder<S> build(ClientFeedback<S> feedback) {
        return LiteralArgumentBuilder.<S>literal("ntmwikirender")
                .executes(ctx -> {
                    feedback.error(ctx.getSource(), Component.translatable("commands.hbm.ntmwikirender.usage")
                            .withStyle(ChatFormatting.GREEN));
                    return 0;
                })
                .then(RequiredArgumentBuilder.<S, String>argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                            for (String name : classNames()) {
                                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(name);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String type = StringArgumentType.getString(ctx, "type");
                            Constants.LOG.info("Taking a screenshot of " + type);
                            List<ItemStack> stacks = stacks(type);
                            // The chat screen that ran this closes itself right after; open ours once it has.
                            Minecraft mc = Minecraft.getInstance();
                            GUIScreenWikiRender screen = new GUIScreenWikiRender(stacks.toArray(new ItemStack[0]),
                                    type, "wiki-block-renders-256", 16);
                            java.util.concurrent.CompletableFuture.runAsync(() -> mc.execute(() -> mc.setScreen(screen)),
                                    java.util.concurrent.CompletableFuture.delayedExecutor(100,
                                            java.util.concurrent.TimeUnit.MILLISECONDS));
                            return stacks.size();
                        }));
    }

    private static boolean eligible(Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        if (!id.startsWith(Constants.MOD_ID + ":")) return false;
        // 1.7 ignoredItems: achievement_icon, spawn eggs, the mob spawner
        return !id.startsWith(Constants.MOD_ID + ":achievement_icon") && !(item instanceof SpawnEggItem);
    }

    private static List<ItemStack> stacks(String type) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!eligible(item)) continue;
            boolean match = item.getClass().getSimpleName().equalsIgnoreCase(type)
                    || item instanceof BlockItem block && block.getBlock().getClass().getSimpleName().equalsIgnoreCase(type);
            if (match) stacks.add(new ItemStack(item));
        }
        return stacks;
    }

    private static TreeSet<String> classNames() {
        TreeSet<String> names = new TreeSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!eligible(item)) continue;
            names.add(item.getClass().getSimpleName());
            if (item instanceof BlockItem block) names.add(block.getBlock().getClass().getSimpleName());
        }
        return names;
    }
}
