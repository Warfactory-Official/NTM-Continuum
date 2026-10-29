package com.hbm.blocks.multiblock;

import com.hbm.packet.PlacementEscapePayload;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.Chunks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Owner-scoped collision escape, confirmed only after successful server assembly.
 */
public final class MultiblockPlacementEscape {
    public static final long NO_CORE = Long.MIN_VALUE;
    public static final long UNRESOLVED = Long.MIN_VALUE + 1;

    private MultiblockPlacementEscape() {
    }

    public static long ownerAt(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return NO_CORE;
        if (Chunks.loaded(level, pos.getX() >> 4, pos.getZ() >> 4) == null) return UNRESOLVED;
        var state = level.getBlockState(pos);
        if (MultiblockSurface.foldedCore(state) != null) return pos.asLong();
        if (!MultiblockSurface.isFoldedCell(state)) return NO_CORE;
        var core = MultiblockSurface.coreOfCell(level, pos);
        return core == null ? UNRESOLVED : core.asLong();
    }

    public static boolean isCurrentState(Level level, BlockPos pos, BlockState state) {
        return Chunks.loaded(level, pos.getX() >> 4, pos.getZ() >> 4) != null && level.getBlockState(pos) == state;
    }

    public static void receive(Player player, PlacementEscapePayload payload) {
        if (Ids.keyId(player.level().dimension()).toString().equals(payload.dimension()))
            ((PlacementEscape) player).hbm$grantPlacementEscape(player.level().dimension(), payload.core());
    }

    public static void placed(Level level, BlockPos core, BlockMultiblockCore block, Player player) {
        if (!(level instanceof ServerLevel server)) return;
        var feet = BlockPos.containing(player.getX(), player.getY() + 0.1D, player.getZ());
        long packed = core.asLong();
        if (ownerAt(server, feet) != packed && ownerAt(server, feet.above()) != packed
                && ownerAt(server, BlockPos.containing(player.getEyePosition())) != packed) return;
        ((PlacementEscape) player).hbm$grantPlacementEscape(level.dimension(), packed);
        if (!(player instanceof ServerPlayer recipient)) return;

        var changes = new Long2ObjectOpenHashMap<ShortOpenHashSet>();
        add(changes, core);
        block.visitPlacedCells(level, core, level.getBlockState(core).getValue(BlockMultiblockCore.FACING),
                (pos, ignored) -> add(changes, pos));
        for (var entry : changes.long2ObjectEntrySet()) {
            var section = SectionPos.of(entry.getLongKey());
            var chunk = server.getChunk(section.x(), section.z());
            recipient.connection.send(new ClientboundSectionBlocksUpdatePacket(section, entry.getValue(),
                    chunk.getSection(level.getSectionIndex(section.minBlockY()))));
        }
        var entity = level.getBlockEntity(core);
        if (entity instanceof TileEntityLoadedBase synced) synced.syncTo(recipient);
        else if (entity != null) {
            var update = entity.getUpdatePacket();
            if (update != null) recipient.connection.send(update);
        }
        GolemNetwork.sendToPlayer(recipient, PlacementEscapePayload.CODEC,
                new PlacementEscapePayload(Ids.keyId(level.dimension()).toString(), packed));
    }

    private static void add(Long2ObjectOpenHashMap<ShortOpenHashSet> changes, BlockPos pos) {
        changes.computeIfAbsent(SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4),
                ignored -> new ShortOpenHashSet()).add(SectionPos.sectionRelativePos(pos));
    }
}
