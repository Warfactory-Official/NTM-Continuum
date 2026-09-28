package com.hbm.commands;

import com.hbm.config.ClientConfig;
import com.mojang.brigadier.CommandDispatcher;
import dev.golem.core.command.CompatCommands;

/**
 * 1.7's client-side commands ({@code ClientCommandHandler}): {@code /ntmclient}, {@code /ntmwikirender} and
 * {@code /dumpthreadsandcrashgame}. They run on the client without asking the server, so they work on any server.
 * Registered once from client initialisation through Golem Core's {@link CompatCommands#client}.
 */
public final class HbmClientCommands {

    private HbmClientCommands() {}

    public static void register() {
        ClientConfig.initConfig();
        com.hbm.client.AdvancementHarnessQueries.register();
        CompatCommands.client(new CompatCommands.ClientTree() {
            @Override
            public <S> void register(CommandDispatcher<S> dispatcher, CompatCommands.ClientFeedback<S> feedback) {
                dispatcher.register(CommandReloadConfig.build(
                        new CommandReloadConfig.Target("ntmclient", "client", () -> ClientConfig.configMap,
                                ClientConfig::refresh, ClientConfig::reload),
                        source -> true, feedback));
                dispatcher.register(CommandWikiRender.build(feedback));
                dispatcher.register(SuicideThreadDump.build(feedback));
            }
        });
    }
}
