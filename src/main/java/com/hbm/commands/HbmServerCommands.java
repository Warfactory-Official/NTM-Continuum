package com.hbm.commands;

import com.hbm.config.ServerConfig;
import com.mojang.brigadier.CommandDispatcher;
import dev.golem.core.command.CompatCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7 {@code MainRegistry.serverStart}'s command list, registered through Golem Core's {@link CompatCommands} on
 * every loader, plus the port's {@code /hbm} debug tree. {@code /ntmsatellites} is the satellites family's.
 *
 * <p>Permission levels are 1.7's: {@code CommandBase} required level 4 unless a command said otherwise;
 * {@code /ntmcustomize} allowed everyone and {@code /ntmserver} every player.
 *
 * <p>Not ported: {@code /ntmpackets}. It inspects and toggles 1.7's {@code PacketThreading} pool; the port sends
 * packets on the server thread through Golem Core's network layer, so there is no pool, lock or queue to report.
 */
public final class HbmServerCommands {

    private HbmServerCommands() {}

    /** Hooks every loader's server command registration; called once from mod construction. */
    public static void init() {
        CompatCommands.server(HbmServerCommands::register);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        HbmCommands.register(dispatcher);
        dispatcher.register(CommandReloadRecipes.build());
        dispatcher.register(CommandDebugChunkLoad.build());
        dispatcher.register(CommandRadiation.build());
        dispatcher.register(CommandReloadConfig.build(
                new CommandReloadConfig.Target("ntmserver", "server", () -> ServerConfig.configMap,
                        ServerConfig::refresh, ServerConfig::reload),
                source -> source.getEntity() instanceof Player, SERVER_FEEDBACK));
        dispatcher.register(CommandLocate.build());
        dispatcher.register(CommandCustomize.build());
        dispatcher.register(CommandReapNetworks.build());
    }

    /** Server-side {@link CompatCommands.ClientFeedback}, so the shared config command talks the same way. */
    static final CompatCommands.ClientFeedback<CommandSourceStack> SERVER_FEEDBACK = new CompatCommands.ClientFeedback<>() {
        @Override public void success(CommandSourceStack source, Component message) {
            source.sendSuccess(() -> message, false);
        }

        @Override public void error(CommandSourceStack source, Component message) {
            source.sendFailure(message);
        }
    };
}
