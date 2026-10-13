package com.hbm.commands;

import com.hbm.Constants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.golem.core.command.CompatCommands.ClientFeedback;
import net.minecraft.network.chat.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/**
 * 1.7 {@code SuicideThreadDump}, the client command {@code /dumpthreadsandcrashgame <dump|crash>}: writes every JVM
 * thread's state, held locks and stack to the log at FATAL, and with {@code crash} then halts the game exactly as
 * 1.7's {@code FMLCommonHandler.exitJava(0, true)} did (no shutdown hooks, no world save: it is for a client that is
 * already deadlocked). {@code dump} alone is safe to run at any time; {@code crash} is not exercised by
 * the automated harness. Any other argument is 1.7's error.
 */
public final class SuicideThreadDump {

    private SuicideThreadDump() {}

    public static <S> LiteralArgumentBuilder<S> build(ClientFeedback<S> feedback) {
        return LiteralArgumentBuilder.<S>literal("dumpthreadsandcrashgame")
                .executes(ctx -> {
                    feedback.error(ctx.getSource(), Component.translatable("commands.hbm.dumpthreads.usage"));
                    return 0;
                })
                .then(RequiredArgumentBuilder.<S, String>argument("mode", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            if ("dump".startsWith(builder.getRemaining())) builder.suggest("dump");
                            if ("crash".startsWith(builder.getRemaining())) builder.suggest("crash");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String mode = StringArgumentType.getString(ctx, "mode");
                            if (!mode.equals("dump") && !mode.equals("crash")) {
                                feedback.error(ctx.getSource(), Component.translatable("commands.hbm.dumpthreads.usage"));
                                return 0;
                            }
                            ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                            for (ThreadInfo thread : threads) dumpThread(thread);
                            if (mode.equals("crash")) Runtime.getRuntime().halt(0);
                            return threads.length;
                        }));
    }

    private static void dumpThread(ThreadInfo info) {
        Constants.LOG.error("===========================================");
        Constants.LOG.error("Thread: " + info.getThreadName() + " PID: " + info.getThreadId());
        Constants.LOG.error("Suspended: " + info.isSuspended());
        Constants.LOG.error("Blocked: " + info.getBlockedTime() + "ms, " + info.getBlockedCount() + "x");
        Constants.LOG.error("Runs Native: " + info.isInNative());
        Constants.LOG.error("State: " + info.getThreadState().name());
        Constants.LOG.error("-------------------------------------------");
        if (info.getLockedMonitors().length != 0) {
            Constants.LOG.error("Following locks found:");
            for (MonitorInfo monitor : info.getLockedMonitors()) Constants.LOG.error("- " + monitor.getLockedStackFrame());
            Constants.LOG.error("-------------------------------------------");
        }
        Constants.LOG.error("Stacktrace:");
        for (StackTraceElement line : info.getStackTrace()) Constants.LOG.error("- " + line);
        Constants.LOG.error("===========================================");
    }
}
