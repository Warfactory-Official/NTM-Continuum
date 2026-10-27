package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.source.de7.Players;
import com.hbm.delegacy.de7.Walker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * {@code playerdata}'s player files and their {@code .dat_old} backups, which vanilla loads when a file is damaged, and
 * the singleplayer host in {@code level.dat}, which the server has already read and which is written back converted.
 */
public final class PlayersStep implements Step {
    /** Vanilla drops unknown keys from a player file when it saves it, so the mark lasts only until the player plays. */
    static final String CONVERTED = "hbm_de_converted";
    @Override
    public String id() {
        return "players@1";
    }

    @Override
    public boolean applies(Ctx ctx) {
        return true;
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        Walker walker = new Walker(ctx);
        Path folder = ctx.world.resolve("playerdata");
        if (Files.isDirectory(folder)) {
            List<Path> files;
            try (Stream<Path> list = Files.list(folder)) {
                files = list.filter(p -> p.getFileName().toString().endsWith(".dat") || p.getFileName().toString().endsWith(".dat_old")).toList();
            }
            for (Path file : files) {
                if (ctx.journal.done(id(), ctx.world, file)) continue;
                CompoundTag player;
                try {
                    player = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                } catch (IOException | RuntimeException e) {
                    // vanilla would start this player afresh; the file stays as it is and a copy is parked
                    ctx.parking.park("player_file", "player " + file.getFileName(), "unreadable: " + e, new ByteArrayTag(Files.readAllBytes(file)));
                    ctx.report.line("player file " + file.getFileName() + " cannot be read and was left as it is");
                    ctx.journal.mark(id(), ctx.world, file);
                    continue;
                }
                // a crash after the swap and before the journal line finds the mark and does not convert twice
                if (!player.getBoolean(CONVERTED)) {
                    Unit unit = ctx.begin("player " + file.getFileName());
                    try {
                        Players.convert(player, walker, ctx, "player " + file.getFileName());
                    } catch (RuntimeException e) {
                        // left as it is with a copy kept, as an unreadable file is
                        ctx.abandon(unit);
                        ctx.parking.park("player_file", "player " + file.getFileName(), "not converted: " + e, new ByteArrayTag(Files.readAllBytes(file)));
                        ctx.report.line("player file " + file.getFileName() + " was not converted (" + e + ") and was left as it is");
                        ctx.journal.mark(id(), ctx.world, file);
                        continue;
                    }
                    player.putBoolean(CONVERTED, true);
                    Path tmp = file.resolveSibling(file.getFileName() + ".deimport");
                    NbtIo.writeCompressed(player, tmp);
                    ctx.commit(unit);
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                    ctx.report.count("players converted");
                }
                ctx.journal.mark(id(), ctx.world, file);
            }
        }
        CompoundTag host = server == null ? null : server.getWorldData().getLoadedPlayerTag();
        if (host != null && !host.getBoolean(CONVERTED)) host(ctx, walker, host);
        return true;
    }

    /**
     * The host's tag is the one the server keeps in memory and loads the owner from, so it is converted in place; it is
     * also written into {@code level.dat} before the world loads, or a crash before the first save would load DE's.
     */
    public static void host(Ctx ctx, Walker walker, CompoundTag host) throws IOException {
        Unit unit = ctx.begin("player level.dat");
        CompoundTag converted = host.copy();
        try {
            Players.convert(converted, walker, ctx, "level.dat player");
        } catch (RuntimeException e) {
            // left as it is with a copy kept, as a player file is
            ctx.abandon(unit);
            ctx.parking.park("player_file", "level.dat player", "not converted: " + e, host);
            ctx.report.line("the level.dat player was not converted (" + e + ") and was left as it is");
            return;
        }
        converted.putBoolean(CONVERTED, true);
        for (String key : List.copyOf(host.getAllKeys())) host.remove(key);
        host.merge(converted);
        Path file = ctx.world.resolve("level.dat");
        CompoundTag root;
        try {
            root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException e) {
            // the server read it a moment ago; if it cannot now, the converted host is written at the first save
            ctx.commit(unit);
            ctx.report.line("level.dat could not be read again, so its converted player is written at the first save");
            ctx.report.count("players converted");
            return;
        }
        root.getCompound("Data").put("Player", host.copy());
        Path tmp = file.resolveSibling("level.dat.deimport");
        NbtIo.writeCompressed(root, tmp);
        ctx.commit(unit);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        ctx.report.count("players converted");
    }
}
