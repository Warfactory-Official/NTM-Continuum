package com.hbm.commands;

import com.hbm.Constants;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.handler.JetpackHandler;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.items.ICustomizable;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.golem.core.command.Permissions;
import dev.golem.core.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Debug/testing commands (op-only), registered with the 1.7 ntm* commands through
 * {@link com.hbm.commands.HbmServerCommands} (Golem Core's command facade). They expose the chunk radiation engine
 * so it can be exercised without a placed radioactive source:
 *
 * <ul>
 * <li>{@code /hbm rad set <pos> <val>}: set the pocket density at pos</li>
 * <li>{@code /hbm rad add <pos> <val>}: add to it</li>
 * <li>{@code /hbm rad get <pos>}: read it</li>
 * <li>{@code /hbm rad here}: read at the caller's feet</li>
 * <li>{@code /hbm jetpack_smoke}: development-only disposable client smoke setup</li>
 * <li>{@code /hbm entity_smoke}: development-only; spawns the ported entity roster for the render smoke</li>
 * </ul>
 */
public final class HbmCommands {

    private HbmCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hbm").requires(Permissions.gamemasters())
                .then(Commands.literal("jetpack_smoke")
                        .requires(source -> Platform.isDevelopment())
                        .then(Commands.literal("stop")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean stopped = JetpackHandler.stopSmoke(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(stopped
                                            ? "Development jetpack smoke stopped"
                                            : "No development jetpack smoke session was active"), false);
                                    return stopped ? 1 : 0;
                                }))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            JetpackHandler.startSmoke(player);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Development jetpack smoke setup active for 10 seconds"), false);
                            return 1;
                        }))
                .then(Commands.literal("entity_smoke")
                        .requires(source -> Platform.isDevelopment())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int placed = EntityRenderSmoke.spawn(player);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Entity render smoke placed " + placed + " of " + EntityRenderSmoke.ROSTER.size()), false);
                            return placed;
                        }))
                .then(Commands.literal("torex_benchmark")
                        .requires(source -> Platform.isDevelopment())
                        .then(Commands.literal("stop")
                                .executes(ctx -> stopTorexBenchmark(ctx.getSource())))
                        .then(Commands.argument("age", IntegerArgumentType.integer(0, 4_000))
                                .executes(ctx -> startTorexBenchmark(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "age"))))
                         .executes(ctx -> startTorexBenchmark(ctx.getSource(), 1_200)))
                .then(Commands.literal("customize")
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .executes(ctx -> customize(ctx.getSource(), StringArgumentType.getString(ctx, "args"))))
                        .executes(ctx -> customize(ctx.getSource(), "")))
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

    private static int customize(CommandSourceStack source, String raw) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        if (!(player.getMainHandItem().getItem() instanceof ICustomizable item)) return 0;
        String trimmed = raw.trim();
        item.customize(player, player.getMainHandItem(), trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+"));
        return 1;
    }

    private static int startTorexBenchmark(CommandSourceStack source, int age)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        stopTorexBenchmark(source);
        EntityNukeTorex torex = EntityNukeTorex.benchmarkFac(source.getLevel(),
                player.getX(), player.getY(), player.getZ() + 240D, age);
        source.getLevel().addFreshEntity(torex);
        Constants.LOG.info("[TOREX-BENCH] fixture dimension={} player={} origin={} age={} seed=0x48424D544F524558",
                source.getLevel().dimension(), player.blockPosition(), torex.blockPosition(), age);
        source.sendSuccess(() -> Component.literal("Development Torex benchmark started"), false);
        return 1;
    }

    private static int stopTorexBenchmark(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int removed = 0;
        for (EntityNukeTorex torex : level.getEntitiesOfClass(EntityNukeTorex.class,
                new AABB(source.getPosition(), source.getPosition()).inflate(4_096D),
                EntityNukeTorex::isBenchmarkFixture)) {
            torex.discard();
            removed++;
        }
        int count = removed;
        source.sendSuccess(() -> Component.literal("Stopped " + count + " Torex benchmark fixture(s)"), false);
        return removed;
    }
}
