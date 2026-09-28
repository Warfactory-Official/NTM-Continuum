package com.hbm.commands;

import com.hbm.Constants;
import com.hbm.uninos.graph.EndpointRegistry;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.golem.core.command.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * 1.7 {@code CommandReapNetworks}, {@code /ntmreapnetworks}: throws away the in-memory network bookkeeping so it is
 * rebuilt from the world, the fix for a network that has wedged.
 *
 * <p>1.7's {@code UniNodespace} nets were transient and rebuilt themselves from their nodes. The port's conductor
 * graphs are saved with the level and are the network, not a cache of it, so clearing them would delete every cable
 * run; what the port keeps only in memory is each level's endpoint table, the "which machine sits on which face"
 * lookup the nets resolve through. That is what this clears: it is an optimisation, never a gate, and block entities
 * re-declare themselves as their chunks come back. The error report is 1.7's.
 */
public final class CommandReapNetworks {

    private CommandReapNetworks() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("ntmreapnetworks").requires(Permissions.level(4)).executes(ctx -> {
            CommandSourceStack source = ctx.getSource();
            try {
                for (ServerLevel level : source.getServer().getAllLevels()) EndpointRegistry.clearLevel(level);
                source.sendSuccess(() -> Component.translatable("commands.hbm.ntmreapnetworks.success")
                        .withStyle(ChatFormatting.YELLOW), false);
                return 1;
            } catch (Exception ex) {
                Constants.LOG.error("/ntmreapnetworks failed", ex);
                CommandReloadRecipes.reportError(source, "commands.hbm.ntmreapnetworks.error", ex);
                throw ex;
            }
        });
    }
}
