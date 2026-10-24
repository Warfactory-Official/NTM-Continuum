package com.hbm.world.gen.nbt;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.BlockCable;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.data.NbtFiles;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.BlockEntities;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.zip.GZIPOutputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Exports the native 1.7 palette and tile migrations as a vanilla structure template. */
public final class LegacyStructureTemplate {

    private LegacyStructureTemplate() {}

    public static Result convert(String name, CompoundTag source, HolderLookup.Provider registries,
                                 boolean intactSpotlights) {
        return convert(name, source, registries, intactSpotlights, LegacyStructurePalette::resolve);
    }

    /** An addon may relink its old palette names while retaining the native metadata rules. */
    public static Result convert(String name, CompoundTag source, HolderLookup.Provider registries,
                                 boolean intactSpotlights,
                                 BiFunction<String, Integer, LegacyStructurePalette.Resolved> resolver) {
        var palette = NbtFiles.list(source, "palette");
        var blocks = NbtFiles.list(source, "blocks");
        var extent = NbtFiles.list(source, "size");
        var size = new Vec3i(NbtFiles.intAt(extent, 0), NbtFiles.intAt(extent, 1), NbtFiles.intAt(extent, 2));
        var itemPalette = new HashMap<Integer, String>();
        for (var element : NbtFiles.list(source, "itemPalette")) {
            var item = (CompoundTag) element;
            var id = Tags.getIntOr(item, "ID", -1);
            if (itemPalette.put(id, Tags.getStringOr(item, "Name", "")) != null) {
                throw new IllegalStateException(name + ": duplicate item palette id " + id);
            }
        }

        var authored = new HashMap<BlockPos, CompoundTag>();
        for (var element : blocks) {
            var block = (CompoundTag) element;
            var pos = position(block);
            if (authored.put(pos, block) != null) throw new IllegalStateException(name + ": duplicate block at " + pos);
        }
        var recording = new Recording(name, registries);
        var gaps = new TreeMap<String, Integer>();
        var cores = new int[1];
        var cells = new int[1];
        Runnable conversion = () -> {
            for (var element : blocks) {
                var block = (CompoundTag) element;
                var pos = position(block);
                var entry = Tags.getCompoundOrEmpty(palette, Tags.getIntOr(block, "state", -1));
                var legacyName = Tags.getStringOr(entry, "Name", "");
                var meta = metadata(entry);
                var transformed = LegacyStructurePalette.transform(legacyName, meta, 0);
                var resolved = resolver.apply(transformed.name(), transformed.meta());
                if (resolved.kind() == LegacyStructurePalette.Kind.UNKNOWN) {
                    gaps.merge(legacyName + "@" + meta, 1, Integer::sum);
                    continue;
                }
                if (resolved.kind() == LegacyStructurePalette.Kind.CELL) {
                    cells[0]++;
                    continue;
                }
                if (resolved.kind() == LegacyStructurePalette.Kind.CORE) cores[0]++;
                var state = resolved.state();
                if (resolved.kind() == LegacyStructurePalette.Kind.DOOR) {
                    var sibling = authored.get((meta & 8) == 0 ? pos.above() : pos.below());
                    var siblingEntry = sibling == null ? null
                            : Tags.getCompoundOrEmpty(palette, Tags.getIntOr(sibling, "state", -1));
                    if (siblingEntry == null || !legacyName.equals(Tags.getStringOr(siblingEntry, "Name", ""))) {
                        gaps.merge(legacyName + " (missing other half)@" + meta, 1, Integer::sum);
                        continue;
                    }
                    state = LegacyStructurePalette.door(state, meta, metadata(siblingEntry));
                }

                CompoundTag converted = null;
                var raw = NbtFiles.compoundOrNull(block, "nbt");
                if (raw != null) {
                    try {
                        if (Tags.getBooleanOr(raw, "tilted", false)) {
                            throw new IllegalStateException("untranslated tilted block entity");
                        }
                        var id = Tags.getStringOr(raw, "id", "");
                        if (id.equals("FlowerPot") || id.equals("Skull")) {
                            state = LegacyStructureTiles.upgradeState(state, raw, 0);
                        } else if (state.getBlock() instanceof EntityBlock entityBlock) {
                            var entity = entityBlock.newBlockEntity(pos, state);
                            if (entity instanceof ILegacyStructureTile legacyTile) {
                                legacyTile.readLegacyStructureNBT(raw.copy());
                            } else if (!LegacyStructureTiles.applyData(entity, raw, itemPalette, registries)) {
                                throw new IllegalStateException("untranslated block entity " + Tags.getStringOr(raw, "id", ""));
                            }
                            if (entity != null) {
                                converted = tileData(entity, registries);
                            }
                        } else if (!LegacyStructureTiles.applyData(null, raw, itemPalette, registries)) {
                            throw new IllegalStateException("tile payload needs a state migration: " + Tags.getStringOr(raw, "id", ""));
                        }
                    } catch (RuntimeException failure) {
                        throw new IllegalStateException(name + " at " + pos + " " + legacyName + ": " + failure.getMessage(), failure);
                    }
                }
                recording.setBlockState(pos, state, converted);
            }
        };
        if (intactSpotlights) LegacyStructurePalette.withSpotlightsWorking(conversion);
        else conversion.run();
        return new Result(gaps.isEmpty() ? recording : null, size, Map.copyOf(gaps), cores[0], cells[0]);
    }

    private static CompoundTag tileData(BlockEntity entity, HolderLookup.Provider registries) {
        var data = BlockEntities.save(entity, registries);
        data.remove("x");
        data.remove("y");
        data.remove("z");
        return data;
    }

    private static int metadata(CompoundTag entry) {
        var properties = NbtFiles.compoundOrNull(entry, "Properties");
        return properties == null ? 0 : Integer.parseInt(Tags.getStringOr(properties, "meta", "0"));
    }

    private static BlockPos position(CompoundTag entry) {
        var xyz = NbtFiles.list(entry, "pos");
        return new BlockPos(NbtFiles.intAt(xyz, 0), NbtFiles.intAt(xyz, 1), NbtFiles.intAt(xyz, 2));
    }

    public record Result(@Nullable Recording builder, Vec3i size, Map<String, Integer> gaps,
                         int multiblockCores, int multiblockCells) {
        public boolean gated() { return !gaps.isEmpty(); }
    }

    /** A deterministic recording; declared size keeps each authored coordinate rather than shrink-wrapping it. */
    public static final class Recording implements BlockGetter {
        private final String name;
        private final Map<BlockPos, Cell> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, BlockEntity> entities = new HashMap<>();
        private final HolderLookup.Provider registries;
        private int minY = Integer.MAX_VALUE;
        private int maxY = Integer.MIN_VALUE;

        public Recording(String name) {
            this(name, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        }
        public Recording(String name, HolderLookup.Provider registries) {
            this.name = name;
            this.registries = registries;
        }

        @Override public int getHeight() { return blocks.isEmpty() ? 0 : maxY - minY + 1; }
        //? if >=26.1 {
        @Override public int getMinY() { return blocks.isEmpty() ? 0 : minY; }
        //?} else {
        /*@Override public int getMinBuildHeight() { return blocks.isEmpty() ? 0 : minY; }
        *///?}
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            var cached = entities.get(pos);
            if (cached != null) return cached;
            var cell = blocks.get(pos);
            if (cell == null || !(cell.state().getBlock() instanceof EntityBlock factory)) return null;
            var entity = factory.newBlockEntity(pos, cell.state());
            if (entity != null) {
                if (cell.data() != null) BlockEntities.loadWithComponents(entity, cell.data(), registries);
                entities.put(pos.immutable(), entity);
            }
            return entity;
        }

        public BlockState getBlockState(int x, int y, int z) { return getBlockState(new BlockPos(x, y, z)); }
        public BlockState getBlockState(BlockPos pos) {
            var cell = blocks.get(pos);
            return cell == null ? Blocks.AIR.defaultBlockState() : cell.state();
        }
        public void setBlockState(BlockPos pos, BlockState state) { setBlockState(pos, state, null); }
        public void setBlockState(BlockPos pos, BlockState state, @Nullable CompoundTag data) {
            blocks.put(pos.immutable(), new Cell(state, data == null ? null : data.copy()));
            entities.remove(pos);
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }
        public void setBlockState(int x, int y, int z, BlockState state) {
            setBlockState(new BlockPos(x, y, z), state);
        }
        public void setBlockState(int x, int y, int z, BlockState state, CompoundTag data) {
            setBlockState(new BlockPos(x, y, z), state, data);
        }

        public CompoundTag tag(Vec3i size) {
            if (blocks.isEmpty()) throw new IllegalStateException(name + ": recorded zero blocks");
            var owners = foldedOwners();
            bakePowerConnections(owners);
            var ordered = new ArrayList<>(blocks.entrySet());
            ordered.sort(Comparator.<Map.Entry<BlockPos, Cell>>comparingInt(entry -> entry.getKey().getY())
                    .thenComparingInt(entry -> entry.getKey().getX()).thenComparingInt(entry -> entry.getKey().getZ()));
            var indexes = new LinkedHashMap<BlockState, Integer>();
            var palette = new ListTag();
            var written = new ListTag();
            for (var entry : ordered) {
                var pos = entry.getKey();
                var owner = owners.get(pos);
                if (owner != null && !owner.core().equals(pos)) continue;
                if (pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0
                        || pos.getX() >= size.getX() || pos.getY() >= size.getY() || pos.getZ() >= size.getZ()) {
                    throw new IllegalStateException(name + ": block " + pos + " outside declared size " + size);
                }
                var cell = entry.getValue();
                var index = indexes.computeIfAbsent(cell.state(), state -> {
                    palette.add(LegacyStructurePalette.writeState(state));
                    return indexes.size();
                });
                var block = new CompoundTag();
                block.put("pos", xyz(pos.getX(), pos.getY(), pos.getZ()));
                block.putInt("state", index);
                if (cell.data() != null) block.put("nbt", cell.data().copy());
                written.add(block);
            }
            var result = new CompoundTag();
            result.put("size", xyz(size.getX(), size.getY(), size.getZ()));
            result.put("palette", palette);
            result.put("blocks", written);
            result.put("entities", new ListTag());
            NbtUtils.addCurrentDataVersion(result);
            return result;
        }

        /** Native assembly owns these cells; literal template writes there could erase an assembled machine. */
        private Map<BlockPos, Owner> foldedOwners() {
            var owners = new HashMap<BlockPos, Owner>();
            for (var entry : blocks.entrySet()) {
                var pos = entry.getKey();
                var state = entry.getValue().state();
                var block = MultiblockSurface.foldedCore(state);
                if (block == null || block.isCellState(state)) continue;
                var owner = new Owner(pos, state, block);
                owners.put(pos, owner);
                block.visitPlacedCells(this, pos, state.getValue(BlockMultiblockCore.FACING),
                        (cell, mask) -> owners.put(cell.immutable(), owner));
            }
            return owners;
        }

        /** Uses the same face declarations and tile connector interfaces that native cable placement queries. */
        private void bakePowerConnections(Map<BlockPos, Owner> owners) {
            for (var entry : blocks.entrySet()) {
                var cell = entry.getValue();
                var state = cell.state();
                var cable = state.getBlock() instanceof BlockCable;
                if (!cable && !(state.getBlock() instanceof IEnergyConnectorBlock)) continue;
                var entity = getBlockEntity(entry.getKey());
                if (!cable && !(entity instanceof PowerConnectionLayout)) continue;
                var mask = powerConnectionMask(entry.getKey(), owners);
                if (cable) {
                    for (var side : Direction.values()) {
                        state = state.setValue(BlockCable.PROPERTY_BY_DIRECTION.get(side), (mask & 1 << side.ordinal()) != 0);
                    }
                }
                var data = cell.data();
                if (entity instanceof PowerConnectionLayout layout) {
                    layout.setPowerConnectionMask(mask);
                    data = tileData(entity, registries);
                }
                entry.setValue(new Cell(state, data));
            }
        }

        private int powerConnectionMask(BlockPos pos, Map<BlockPos, Owner> owners) {
            var mask = 0;
            for (var side : Direction.values()) {
                var neighbour = pos.relative(side);
                var face = side.getOpposite();
                var owner = owners.get(neighbour);
                boolean connected;
                if (owner != null && owner.block() instanceof IEnergyConnectorBlock) {
                    connected = owner.block().isOpenForAny(owner.state().getValue(BlockMultiblockCore.FACING),
                            neighbour.getX() - owner.core().getX(), neighbour.getY() - owner.core().getY(),
                            neighbour.getZ() - owner.core().getZ(), face, BlockMultiblockCore.ROLE_POWER);
                } else {
                    var block = getBlockState(neighbour).getBlock();
                    var direction = ForgeDirection.getOrientation(face);
                    connected = block instanceof IEnergyConnectorBlock connector && connector.canConnect(this, neighbour, direction);
                }
                if (!connected && getBlockEntity(neighbour) instanceof IEnergyConnectorMK2 connector) {
                    connected = connector.canConnect(ForgeDirection.getOrientation(face));
                }
                if (connected) mask |= 1 << side.ordinal();
            }
            return mask;
        }

        public byte[] gzip(Vec3i size) {
            var bytes = new ByteArrayOutputStream();
            try (var gzip = new GZIPOutputStream(bytes); var output = new DataOutputStream(gzip)) {
                NbtIo.write(tag(size), output);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
            return bytes.toByteArray();
        }
    }

    private static ListTag xyz(int x, int y, int z) {
        var result = new ListTag();
        result.add(IntTag.valueOf(x));
        result.add(IntTag.valueOf(y));
        result.add(IntTag.valueOf(z));
        return result;
    }

    private record Owner(BlockPos core, BlockState state, BlockMultiblockCore block) {}
    private record Cell(BlockState state, @Nullable CompoundTag data) {}
}
