package com.hbm.blocks.multiblock;

import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
//? if >=1.21.1 {
import net.minecraft.world.level.chunk.status.ChunkStatus;
//?} else {
/*import net.minecraft.world.level.chunk.ChunkStatus;
*///?}
import org.jspecify.annotations.Nullable;

/** Escape through only the freshly assembled structure which contains its placing player. */
public final class MultiblockPlacementEscape {
    public static final long NO_CORE = Long.MIN_VALUE;
    public static final long UNRESOLVED = Long.MIN_VALUE + 1;

    private MultiblockPlacementEscape() {}

    public record Payload(String dimension, long core) implements GolemPayload {
        @Override public Identifier id() { return CODEC.id(); }
    }

    public static final GolemCodec<Payload> CODEC = GolemCodec.of(Payload.class,
            Ids.of("hbm", "placement_escape"),
            (payload, buffer) -> { buffer.writeUtf(payload.dimension()); buffer.writeLong(payload.core()); },
            buffer -> new Payload(buffer.readUtf(), buffer.readLong()));

    public static void init() { GolemNetwork.declarePlayToClient(CODEC); }

    public static void initClient() {
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> {
            if (Ids.keyId(context.level().dimension()).toString().equals(payload.dimension()))
                grant(context.player(), context.level(), payload.core());
        });
    }

    /** State stored directly on the placing player; a grant never lives on the block type. */
    public interface State {
        void hbm$grantPlacementEscape(Level level, long core);
        void hbm$refreshPlacementEscape();
        boolean hbm$hasPlacementEscape();
        boolean hbm$ignoresPlacementCollision(Level level, BlockPos pos);
    }

    private static void grant(Player player, Level level, long core) {
        ((State) player).hbm$grantPlacementEscape(level, core);
    }

    private static @Nullable LevelChunk loadedChunk(Level level, BlockPos pos) {
        return level instanceof ServerLevel server
                ? server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4)
                : (LevelChunk) level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
    }

    public static long ownerAt(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return NO_CORE;
        var chunk = loadedChunk(level, pos);
        if (chunk == null) return UNRESOLVED;
        var state = chunk.getBlockState(pos);
        if (MultiblockSurface.foldedCore(state) != null) return pos.asLong();
        if (!MultiblockSurface.isFoldedCell(state)) return NO_CORE;
        if (level instanceof ServerLevel server) return MultiblockCoreIndex.lookup(server, pos);
        var core = MultiblockSurface.coreOfCell(level, pos);
        return core == null ? UNRESOLVED : core.asLong();
    }

    public static boolean hasGrant(Player player) {
        return BlockMultiblockCore.walkOutAfterPlacing && ((State) player).hbm$hasPlacementEscape();
    }

    public static boolean ignores(Player player, Level level, BlockPos pos) {
        return BlockMultiblockCore.walkOutAfterPlacing && ((State) player).hbm$ignoresPlacementCollision(level, pos);
    }

    public static boolean isCurrentState(Level level, BlockPos pos, BlockState state) {
        var chunk = loadedChunk(level, pos);
        return chunk != null && chunk.getBlockState(pos) == state;
    }

    /** Initial geometry precedes the grant so client retirement cannot mistake delayed cells for empty space. */
    public static void placed(Level level, BlockPos core, BlockMultiblockCore block, Player player) {
        if (!BlockMultiblockCore.walkOutAfterPlacing || !(level instanceof ServerLevel)
                || !level.getBlockState(core).is(block)) return;
        var feet = BlockPos.containing(player.getX(), player.getY() + 0.1D, player.getZ());
        var packed = core.asLong();
        if (ownerAt(level, feet) != packed && ownerAt(level, feet.above()) != packed) return;
        grant(player, level, packed);
        if (!(player instanceof ServerPlayer recipient)) return;
        var changes = new Long2ObjectOpenHashMap<ShortOpenHashSet>();
        for (var pos : MultiblockSurface.rigidStructureBlocks(level, core)) {
            changes.computeIfAbsent(SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4),
                    ignored -> new ShortOpenHashSet()).add(SectionPos.sectionRelativePos(pos));
        }
        for (var entry : changes.long2ObjectEntrySet()) {
            var section = SectionPos.of(entry.getLongKey());
            var chunk = loadedChunk(level, section.origin());
            if (chunk == null) continue;
            recipient.connection.send(new ClientboundSectionBlocksUpdatePacket(section, entry.getValue(),
                    chunk.getSection(level.getSectionIndex(section.minBlockY()))));
        }
        var entity = level.getBlockEntity(core);
        if (entity != null) {
            var initial = entity.getUpdatePacket();
            if (initial != null) recipient.connection.send(initial);
        }
        GolemNetwork.sendToPlayer(recipient, CODEC,
                new Payload(Ids.keyId(level.dimension()).toString(), packed));
    }
}
