package com.hbm.commands;

import com.hbm.items.ICustomizable;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.golem.core.command.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code CommandCustomize}, {@code /ntmcustomize [args...]}: hands its arguments to the held {@link ICustomizable}
 * item. Permission level 0: every player may customise their own item.
 */
public final class CommandCustomize {

    private CommandCustomize() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("ntmcustomize").requires(Permissions.any())
                .executes(ctx -> run(ctx.getSource(), ""))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx.getSource(), StringArgumentType.getString(ctx, "args"))));
    }

    static int run(CommandSourceStack source, String raw) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendSystemMessage(Component.translatable("commands.hbm.ntmcustomize.player_only").withStyle(ChatFormatting.RED));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof ICustomizable item)) {
            source.sendSystemMessage(Component.translatable("commands.hbm.ntmcustomize.not_customizable").withStyle(ChatFormatting.RED));
            return 0;
        }
        String trimmed = raw.trim();
        item.customize(player, held, trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+"));
        return 1;
    }
}
