package com.hbm.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.golem.core.command.Permissions;
import dev.golem.core.data.Tags;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

/**
 * 1.7 {@code CommandDebugChunkLoad}, {@code /ntmloadchunk <x> <z>}: reads the saved (not loaded) chunk holding block
 * {@code x, z} straight from region storage and lists its block entities by id and chunk-relative position, the ones
 * whose stored position lies outside that chunk in red. It exists to find the corrupt block-entity entries that make
 * a chunk crash on load, which is why it refuses a chunk that is already loaded.
 *
 * <p>1.7's {@code Level.TileEntities} list is the modern {@code block_entities}; the height check is the level's
 * build range instead of 0-255. 1.7 left the actual repair (renaming the bad id) commented out, so this only reports.
 */
public final class CommandDebugChunkLoad {

    private CommandDebugChunkLoad() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("ntmloadchunk").requires(Permissions.level(4))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "x"),
                                        IntegerArgumentType.getInteger(ctx, "z")))));
    }

    private static int run(CommandSourceStack source, int x, int z) {
        ServerLevel level = source.getLevel();
        int cX = x >> 4;
        int cZ = z >> 4;
        if (level.getChunkSource().getChunkNow(cX, cZ) != null) {
            source.sendFailure(Component.translatable("commands.hbm.ntmloadchunk.loaded"));
            return 0;
        }
        try {
            Optional<CompoundTag> stored = level.getChunkSource().chunkMap.read(new ChunkPos(cX, cZ)).join();
            if (stored.isEmpty()) {
                source.sendFailure(Component.translatable("commands.hbm.ntmloadchunk.null"));
                return 0;
            }
            ListTag tagList = Tags.getListOrEmpty(stored.get(), "block_entities");
            if (tagList.isEmpty()) source.sendFailure(Component.translatable("commands.hbm.ntmloadchunk.empty"));
            for (int n = 0; n < tagList.size(); n++) {
                CompoundTag tile = Tags.getCompoundOrEmpty(tagList, n);
                int i = Tags.getIntOr(tile, "x", 0) - cX * 16;
                int j = Tags.getIntOr(tile, "y", 0);
                int k = Tags.getIntOr(tile, "z", 0) - cZ * 16;
                String name = Tags.getStringOr(tile, "id", "");
                boolean outside = i < 0 || i > 15 || level.isOutsideBuildHeight(j) || k < 0 || k > 15;
                Component line = Component.literal(name + " " + i + " " + j + " " + k)
                        .withStyle(outside ? ChatFormatting.RED : ChatFormatting.GREEN);
                source.sendSuccess(() -> line, false);
            }
            return tagList.size();
        } catch (Exception e) {
            source.sendFailure(Component.literal(String.valueOf(e.getLocalizedMessage())));
            return 0;
        }
    }
}
