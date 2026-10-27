package com.hbm.gametest.deimport;

import com.hbm.Constants;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /hbm deimport_fixture <world> <pos>}: saves the current world, copies it to {@code <world>} beside it and turns
 * the copy into a DE save with the standard content at {@code pos}, for the harness. Dev builds only.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeFixtureCommand {
    private DeFixtureCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hbm").then(Commands.literal("deimport_fixture")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("world", StringArgumentType.word())
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                            Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                            Path target = world.resolveSibling(StringArgumentType.getString(context, "world"));
                            server.saveEverything(true, true, true);
                            try {
                                if (Files.exists(target)) throw new IOException(target + " exists");
                                DeFixtures.copyWorld(world, target);
                                DeFixtures.makeDeWorld(target, pos);
                            } catch (IOException e) {
                                context.getSource().sendFailure(Component.literal(e.toString()));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> Component.literal("DE fixture written to " + target), false);
                            return 1;
                        })))));
    }
}
