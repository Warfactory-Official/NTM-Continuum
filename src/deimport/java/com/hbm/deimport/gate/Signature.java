package com.hbm.deimport.gate;

import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.source.de7.ChunkConverter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.jetbrains.annotations.Nullable;

/**
 * Gate 3: is this world a DE save? Decided by DE's own files (its locked-contents key, its radiation folder, its chunk
 * stamps), never by the version alone, so a later DE release or a world some Continuum opened first is still found.
 */
public final class Signature {
    /** DE releases are {@code 1.0.0-alpha.N} and {@code 1.0.0-beta.N}; Continuum never used that scheme. */
    private static final Pattern DE_VERSION = Pattern.compile("\\d+\\.\\d+\\.\\d+-(alpha|beta)\\.\\d+");
    public static final String STAMP = "hbm_data_version";
    public static final int NEWEST_DATA_VERSION = 7;
    private static final int FILES = 64, PER_FILE = 4;
    /** DE's saved data whose names Continuum shares; only DE's copies are wrapped twice. */
    private static final List<String> DE_SAVED_DATA = List.of("hbm_power_graph", "hbm_pollution", "hbm_satellites", "hbm_impact_data");

    public record Result(boolean de, int dataVersion, String why) {}

    private Signature() {}

    public static boolean deVersion(@Nullable String version) {
        return version != null && DE_VERSION.matcher(version).matches();
    }

    /**
     * DE's own files decide it whatever version last opened the world: a DE world opened once by a Continuum without
     * the importer names Continuum in its {@code level.dat} and has lost the stamps of the chunks that session saved.
     */
    public static Result check(Path world, @Nullable String previousVersion) throws IOException {
        String from = previousVersion == null ? "" : "DE version " + previousVersion + " and ";
        int stamp = sampledStamp(world);
        if (Files.isRegularFile(world.resolve("data/hbm_locked_contents_key.dat")))
            return new Result(true, Math.max(stamp, 0), from + "its locked-contents key");
        if (Files.isDirectory(world.resolve("hbm/radiation")))
            return new Result(true, Math.max(stamp, 0), from + "its radiation folder");
        if (stamp >= 0) return new Result(true, stamp, from + "DE chunk stamps");
        if (wrappedSavedData(world)) return new Result(true, 0, from + "its saved data");
        // DE opened it last but saved none of its own data where it was looked for: every step converts only what is
        // in DE's shape, so importing is safe, and loading it unconverted would save over DE's data
        if (deVersion(previousVersion)) return new Result(true, 0, "DE version " + previousVersion);
        return new Result(false, -1, "no DE data");
    }

    /**
     * The highest DE data version among chunks sampled in every dimension (the spawn chunk, which DE always saves, and
     * up to {@link #FILES} region files, {@link #PER_FILE} chunks each), or -1. A file or chunk that cannot be read is
     * passed over.
     */
    private static int sampledStamp(Path world) throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path dim : Dimensions.all(world).values()) files.addAll(RegionIo.regionFiles(dim.resolve("region")));
        int highest = spawnStamp(world);
        int step = Math.max(1, files.size() / FILES);
        for (int f = 0; f < files.size(); f += step) {
            Path file = files.get(f);
            try (RegionFile region = RegionIo.open(file)) {
                for (ChunkPos pos : RegionIo.chunks(region, file).stream().limit(PER_FILE).toList())
                    highest = Math.max(highest, stamp(region, pos));
            } catch (IOException | RuntimeException e) {
                // a region file vanilla cannot open either says nothing about DE
            }
        }
        return highest;
    }

    private static int spawnStamp(Path world) {
        try {
            CompoundTag data = NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.unlimitedHeap()).getCompound("Data");
            ChunkPos spawn = new ChunkPos(data.getInt("SpawnX") >> 4, data.getInt("SpawnZ") >> 4);
            Path file = world.resolve("region/r." + spawn.getRegionX() + "." + spawn.getRegionZ() + ".mca");
            if (!Files.isRegularFile(file)) return -1;
            try (RegionFile region = RegionIo.open(file)) {
                return stamp(region, spawn);
            }
        } catch (IOException | RuntimeException e) {
            // no readable level.dat or spawn region says nothing either way
            return -1;
        }
    }

    /** One chunk's DE data version, 0 for DE's unstamped chunks, -1 for none or a chunk that cannot be read. */
    private static int stamp(RegionFile region, ChunkPos pos) {
        try {
            CompoundTag tag = region.hasChunk(pos) ? RegionIo.read(region, pos) : null;
            if (tag != null && tag.contains(STAMP)) return tag.getInt(STAMP);
            if (tag != null && tag.contains(ChunkConverter.OLD_INDEX)) return 0;
        } catch (IOException | RuntimeException e) {
            // a damaged chunk says nothing either way
        }
        return -1;
    }

    /** The DE addons among {@code known} whose data the world holds: in its saved mod list, or as a dimension folder. */
    public static List<String> addons(Path world, Set<String> known) {
        List<String> out = new ArrayList<>();
        try {
            CompoundTag root = NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.unlimitedHeap());
            for (Tag t : root.getCompound("fml").getList("LoadingModList", Tag.TAG_COMPOUND)) {
                String id = ((CompoundTag) t).getString("ModId");
                if (known.contains(id)) out.add(id);
            }
        } catch (IOException | RuntimeException e) {
            // no readable level.dat: the dimension folders still tell
        }
        for (String id : known) if (!out.contains(id) && Files.isDirectory(world.resolve("dimensions").resolve(id))) out.add(id);
        return out;
    }

    /** DE wraps its saved data in a second {@code data} level, which Continuum never writes. */
    private static boolean wrappedSavedData(Path world) throws IOException {
        for (Path dim : Dimensions.all(world).values()) {
            for (String name : DE_SAVED_DATA) {
                Path file = dim.resolve("data/" + name + ".dat");
                if (!Files.isRegularFile(file)) continue;
                try {
                    if (NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getCompound("data").contains("data", Tag.TAG_COMPOUND)) return true;
                } catch (IOException | RuntimeException e) {
                    // an unreadable file says nothing either way
                }
            }
        }
        return false;
    }
}
