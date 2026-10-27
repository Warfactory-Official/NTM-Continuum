package com.hbm.deimport;

import com.hbm.Constants;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Journal;
import com.hbm.deimport.engine.Parking;
import com.hbm.deimport.engine.Pipeline;
import com.hbm.deimport.engine.Report;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.gate.Signature;
import com.hbm.deimport.steps.LiveStep;
import com.hbm.deimport.steps.Steps;
import com.hbm.deimport.target.ContinuumTarget;
import com.hbm.delegacy.AtomicNbt;
import com.hbm.delegacy.legacy.LegacyStacks;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModMismatchEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The importer's only NeoForge listeners. A world with a finished state file is left alone; any other is checked for
 * DE's own files, and converted when it has them.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeImport {
    /** Previous {@code hbm} versions NeoForge reported, by world folder. */
    private static final Map<Path, String> MISMATCHES = new ConcurrentHashMap<>();
    /** The live phase's share of each server tick: keeps the dedicated server's watchdog fed while it runs. */
    private static final long LIVE_BUDGET_NANOS = 25_000_000L;
    private static @Nullable Run run;
    private static @Nullable String announcement;

    /** One import between its files phase and its end. */
    public static final class Run {
        final Path world;
        final ImportState state;
        final Ctx ctx;
        final Pipeline pipeline;
        private final LiveStep live;
        private boolean prepared;

        public Run(Path world, ImportState state, Ctx ctx, Pipeline pipeline, LiveStep live) {
            this.world = world;
            this.state = state;
            this.ctx = ctx;
            this.pipeline = pipeline;
            this.live = live;
        }

        /**
         * One tick's share of the live phase; true once the import is finished. The other live steps run first, in
         * the first tick: the RBMK settings decide how tall the rebuilt columns are.
         */
        public boolean advance(MinecraftServer server, long deadline) throws IOException {
            if (!prepared) {
                pipeline.run(Step.Phase.LIVE, state, ctx, server, step -> step != live);
                prepared = true;
            }
            if (!live.slice(ctx, server, deadline)) return false;
            pipeline.run(Step.Phase.LIVE, state, ctx, server);
            pipeline.finish(state, ctx, world);
            live.reset();
            return true;
        }
    }

    private DeImport() {}

    /** A mod-bus event; NeoForge routes it here by its type. */
    @SubscribeEvent
    public static void mismatch(ModMismatchEvent event) {
        event.getVersionDifference(Constants.MOD_ID).ifPresent(diff -> MISMATCHES.put(
                event.getLevelDirectory().path().toAbsolutePath().normalize(), diff.oldVersion().toString()));
    }

    @SubscribeEvent
    public static void aboutToStart(ServerAboutToStartEvent event) {
        if ("off".equals(System.getProperty("hbm.deimport"))) return;
        MinecraftServer server = event.getServer();
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        String previous = MISMATCHES.remove(world);
        try {
            ImportState existing = ImportState.exists(world) ? ImportState.read(world) : null;
            ImportState state = existing;
            if (state == null) {
                // checked on every start without a state file, not only on a version change: a DE world may have been
                // opened once by a Continuum without the importer, or with the importer turned off
                Signature.Result signature = Signature.check(world, previous);
                if (!signature.de()) return;
                if (signature.dataVersion() > Signature.NEWEST_DATA_VERSION)
                    throw new IllegalStateException("This world was saved by a newer NTM: Decentralized Edition (data version "
                            + signature.dataVersion() + ") than this importer converts (" + Signature.NEWEST_DATA_VERSION
                            + "). Nothing was changed. Update NTM - Continuum or go back to DE.");
                Constants.LOG.info("DE import: converting {} ({})", world, signature.why());
                state = ImportState.create(world, previous == null ? "unknown" : previous, signature.dataVersion(),
                        ModList.get().getModFileById(Constants.MOD_ID).versionString());
                state.save();
            } else if (hasNewSteps(existing)) {
                Constants.LOG.info("DE import: running the steps added since this world was imported");
                state.status("running");
                state.save();
            } else if (state.finished()) {
                String warning = reopenedByDe(state, previous);
                if (warning != null) {
                    Constants.LOG.warn("DE import: {}", warning);
                    announcement = warning;
                }
                return;
            }
            Ctx ctx = new Ctx(world, new ContinuumTarget(server), new Parking(world), new Report(), new Journal(world));
            Pipeline pipeline = new Pipeline(Steps.ALL, id -> ModList.get().isLoaded(id));
            pipeline.run(Step.Phase.FILES, state, ctx, server);
            run = new Run(world, state, ctx, pipeline, Steps.LIVE);
        } catch (IOException e) {
            throw new UncheckedIOException("DE import failed; the world was not loaded. The backup is next to it.", e);
        }
    }

    /** The warning for an imported world that DE opened again since, whose new saves are not converted; else null. */
    public static @Nullable String reopenedByDe(ImportState state, @Nullable String previous) {
        if (!"done".equals(state.status()) || !Signature.deVersion(previous)) return null;
        return "NTM: Decentralized Edition " + previous + " opened this world after it was converted. What DE saved since then "
                + "is not converted; the backup made by the import is next to the world.";
    }

    /** A world imported by an earlier importer, with steps this one added since. */
    public static boolean hasNewSteps(ImportState state) {
        return "done".equals(state.status()) && Steps.ALL.stream().anyMatch(s -> !state.knows(s.id()) && !state.ranEarlier(s.id()));
    }

    /**
     * The live phase runs in slices of server ticks, never in one go: both server events come after the dedicated
     * server's watchdog starts, and a long phase there would halt the server on every start.
     */
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        Run current = run;
        if (current == null) return;
        try {
            if (!current.advance(event.getServer(), System.nanoTime() + LIVE_BUDGET_NANOS)) return;
            run = null;
            announcement = "This world was converted from NTM: Decentralized Edition: " + current.ctx.report.summary()
                    + ". Details in " + Report.FILE + ".";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** An operator, or the owner of a singleplayer world, who is no operator without cheats. */
    private static boolean mayRun(ServerPlayer player) {
        return player.hasPermissions(2) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        run = null;
        Steps.LIVE.reset();
        announcement = null;
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        String text = announcement;
        if (text != null && event.getEntity() instanceof ServerPlayer player && mayRun(player)) {
            player.sendSystemMessage(Component.literal(text));
            announcement = null;
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hbm").then(Commands.literal("deimport")
                .requires(source -> source.hasPermission(2) || source.getPlayer() != null && mayRun(source.getPlayer()))
                .then(Commands.literal("status").executes(DeImport::status))
                .then(Commands.literal("decline").executes(DeImport::decline))));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        Path world = context.getSource().getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        try {
            ImportState state = ImportState.read(world);
            if (state == null) {
                context.getSource().sendSuccess(() -> Component.literal("No DE import in this world."), false);
                return 0;
            }
            Map<String, Integer> parked = new Parking(world).countsFromRecords();
            CompoundTag legacy = AtomicNbt.read(world.resolve(LegacyStacks.LEFTOVERS));
            int later = legacy == null ? 0 : legacy.getList("records", Tag.TAG_COMPOUND).size();
            context.getSource().sendSuccess(() -> Component.literal("DE import " + state.status() + " (from "
                    + state.sourceVersion() + "), parked: " + parked + ", parked from stacks loaded later: " + later), false);
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int decline(CommandContext<CommandSourceStack> context) {
        Path world = context.getSource().getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (run != null) {
            // the files are converted already; the running import would write its own status over this one
            context.getSource().sendFailure(Component.literal("The DE import is running; decline after it finishes."));
            return 0;
        }
        try {
            ImportState state = ImportState.read(world);
            if (state == null) state = ImportState.create(world, "none", 0, "declined");
            state.status("declined");
            state.save();
            context.getSource().sendSuccess(() -> Component.literal("The DE importer will never touch this world."), true);
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }
}
