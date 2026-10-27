package com.hbm.deimport.engine;

import com.hbm.Constants;
import com.hbm.deimport.gate.ImportState;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/** Runs the steps of one phase against a world's state file. */
public final class Pipeline {
    private final List<Step> steps;
    private final Predicate<String> modLoaded;

    public Pipeline(List<Step> steps, Predicate<String> modLoaded) {
        this.steps = steps;
        this.modLoaded = modLoaded;
    }

    /** Runs every unfinished step of {@code phase}; deferred steps run once their mods are present. */
    public void run(Step.Phase phase, ImportState state, Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        run(phase, state, ctx, server, step -> true);
    }

    /** As {@link #run(Step.Phase, ImportState, Ctx, MinecraftServer)}, for the steps {@code only} accepts. */
    public void run(Step.Phase phase, ImportState state, Ctx ctx, @Nullable MinecraftServer server, Predicate<Step> only) throws IOException {
        for (Step step : steps) {
            if (!only.test(step)) continue;
            if (step.phase() != phase || state.stepDone(step.id()) || "converter".equals(state.deferredOn(step.id()))) continue;
            if (state.ranEarlier(step.id())) {
                // the stores it converted no longer look like DE's, so running it again would convert them twice
                state.markDone(step.id());
                state.save();
                continue;
            }
            String missing = step.requiresMods().stream().filter(m -> !modLoaded.test(m)).findFirst().orElse(null);
            if (missing != null) {
                if (state.deferredOn(step.id()) == null && step.applies(ctx)) {
                    step.defer(ctx);
                    state.markDeferred(step.id(), missing);
                    ctx.report.line(step.id() + " deferred until " + missing + " is installed");
                } else if (state.deferredOn(step.id()) == null) {
                    state.markDone(step.id());
                }
                state.save();
                continue;
            }
            if (!step.applies(ctx)) {
                state.markDone(step.id());
            } else {
                Constants.LOG.info("DE import: {}", step.id());
                if (!step.run(ctx, server)) {
                    state.markDeferred(step.id(), "converter");
                } else {
                    String waits = step.waitsFor(ctx);
                    if (waits == null) state.markDone(step.id());
                    else state.markDeferred(step.id(), waits);
                }
            }
            ctx.flushGlobal();
            state.save();
        }
    }

    /**
     * Done once every step is done or waits for a converter only a later importer can bring; a step waiting for a mod
     * keeps the world {@code partial}, so later starts check for that mod.
     */
    public void finish(ImportState state, Ctx ctx, Path world) throws IOException {
        boolean waitsForMod = steps.stream().anyMatch(s -> {
            String on = state.deferredOn(s.id());
            return on != null && !on.equals("converter");
        });
        boolean all = steps.stream().allMatch(s -> state.stepDone(s.id()) || state.deferredOn(s.id()) != null);
        state.status(waitsForMod ? "partial" : all ? "done" : "running");
        state.save();
        ctx.report.append(world, "DE import " + state.status());
        Constants.LOG.info("DE import {}: {}", state.status(), ctx.report.summary());
    }
}
