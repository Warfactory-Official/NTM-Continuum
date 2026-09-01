package com.hbm.command;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Debug/testing commands (op-only). Registered per loader (NeoForge {@code RegisterCommandsEvent} /
 * Fabric {@code CommandRegistrationCallback}). Currently exposes the chunk radiation engine so it can be
 * exercised without a placed radioactive source:
 *
 * <ul>
 * <li>{@code /hbm rad set <pos> <val>} — set the pocket density at pos</li>
 * <li>{@code /hbm rad add <pos> <val>} — add to it</li>
 * <li>{@code /hbm rad get <pos>} — read it</li>
 * <li>{@code /hbm rad here} — read at the caller's feet</li>
 * </ul>
 */
public final class HbmCommands {

    private HbmCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hbm").requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("rad")
                        .then(Commands.literal("set")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("val", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> {
                                                    ServerLevel level = ctx.getSource().getLevel();
                                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                                    double val = DoubleArgumentType.getDouble(ctx, "val");
                                                    ChunkRadiationManager.proxy.setRadiation(level, pos, val);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Set radiation at " + pos + " to " + val), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("val", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> {
                                                    ServerLevel level = ctx.getSource().getLevel();
                                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                                    double val = DoubleArgumentType.getDouble(ctx, "val");
                                                    ChunkRadiationManager.proxy.incrementRad(level, pos, val, Double.MAX_VALUE);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Added " + val + " radiation at " + pos), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            ServerLevel level = ctx.getSource().getLevel();
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            double val = ChunkRadiationManager.proxy.getRadiation(level, pos);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Radiation at " + pos + " = " + val + " RAD/s"), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("here")
                                .executes(ctx -> {
                                    ServerLevel level = ctx.getSource().getLevel();
                                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
                                    double val = ChunkRadiationManager.proxy.getRadiation(level, pos);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Radiation here (" + pos + ") = " + val + " RAD/s"), false);
                                    return 1;
                                }))));
    }
}
