package com.hbm.deimport.steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Datapacks;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.target.ContinuumTarget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.IOUtilities;
import org.jetbrains.annotations.Nullable;

/**
 * DE reads its RBMK settings from the {@code hbm:machine_config} datapack registry, Continuum from world rules. Their
 * defaults are both 1.7's, so only a world datapack that overrides DE's {@code rbmk} entry needs carrying over; a
 * different column height would otherwise misalign every column's cells. Runs before the live phase rebuilds any.
 */
public final class RbmkDialsStep implements Step {
    private static final String CONFIGS = "data/hbm/hbm/machine_config/";

    @Override
    public String id() {
        return "rbmkdials@1";
    }

    @Override
    public Phase phase() {
        return Phase.LIVE;
    }

    @Override
    public boolean applies(Ctx ctx) {
        return Files.isDirectory(ctx.world.resolve("datapacks"));
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        JsonObject dials = ctx.rbmkDials();
        if (dials != null) {
            for (Map.Entry<String, JsonElement> e : dials.entrySet()) {
                try {
                    if (!ContinuumTarget.setRbmkDial(server, e.getKey(), e.getValue())) continue;
                } catch (RuntimeException bad) {
                    // a value DE's codec would have refused too; Continuum's default stays
                    ctx.report.line("RBMK setting " + e.getKey() + " = " + e.getValue() + " is not a value Continuum takes; left at its default");
                    continue;
                }
                ctx.report.line("RBMK setting " + e.getKey() + " = " + e.getValue() + " from the world's datapack");
                ctx.report.count("RBMK settings carried over");
            }
            // world rules live in the overworld's saved data, and this step is done before any autosave writes it
            if (server != null) {
                server.overworld().getDataStorage().save();
                IOUtilities.waitUntilIOWorkerComplete();
            }
        }
        TreeSet<String> others = new TreeSet<>();
        for (Path pack : Datapacks.ordered(ctx.world)) {
            try {
                for (String name : Datapacks.list(pack, CONFIGS)) if (name.endsWith(".json") && !name.equals("rbmk.json")) others.add(name);
            } catch (IOException | RuntimeException e) {
                // reported once by Ctx.rbmkDials, which read the same packs
            }
        }
        if (!others.isEmpty())
            ctx.report.line("the world's datapacks override DE's machine settings " + others
                    + "; Continuum keeps machine settings in config/hbmMachines.json, so these were not carried over");
        return true;
    }
}
