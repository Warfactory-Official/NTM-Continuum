package com.hbm.delegacy;

import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** Files written whole or not at all, and compressed NBT read back without letting a damaged file stop a world. */
public final class AtomicNbt {
    private static final Logger LOGGER = LogUtils.getLogger();

    private AtomicNbt() {}

    /**
     * Writes a temporary file next to {@code file} and flushes it to disk, then moves it over; a crash or a power cut
     * leaves the old or the whole new file.
     */
    public static void write(CompoundTag tag, Path file) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, bytes);
        write(bytes.toByteArray(), file);
    }

    /** As {@link #write(CompoundTag, Path)}, for any bytes. */
    public static void write(byte[] bytes, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // a file system without atomic moves still gets the whole file, only without the crash guarantee
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * The file's content, or null when it is missing. A file that cannot be read is renamed {@code .damaged-<time>}
     * and read as missing, so it is kept and the caller starts empty instead of failing every start.
     */
    public static @Nullable CompoundTag read(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException e) {
            setAside(file, e);
            return null;
        }
    }

    /** Renames a file that cannot be read to {@code .damaged-<time>}, so it is kept and no longer read. */
    public static void setAside(Path file, Exception why) {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        LOGGER.error("Could not read {}; it is kept as {}.damaged-{}", file, file.getFileName(), stamp, why);
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".damaged-" + stamp));
        } catch (IOException moveFailed) {
            // left where it is; the next start meets it again and tries the rename again
        }
    }
}
