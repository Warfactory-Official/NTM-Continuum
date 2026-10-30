package com.hbm.deimport.target;

import com.google.gson.JsonElement;
import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.ImportedCells;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.BlockCable;
import com.hbm.blocks.network.BlockCablePaintable;
import com.hbm.blocks.network.WireCoated;
import com.hbm.capability.MachineCaps;
import com.hbm.deimport.engine.NbtDiff;
import com.hbm.deimport.engine.Target;
import com.hbm.delegacy.ContinuumStacks;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.world.WorldRules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.jetbrains.annotations.Nullable;

/** The running Continuum, read through its registries: the converter's only view of Continuum's blocks and items. */
public final class ContinuumTarget implements Target {
    private final Map<String, Optional<String>> beTypes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<String>>> props = new ConcurrentHashMap<>();
    private final @Nullable MinecraftServer server;
    private static final Set<String> BLOCK_ENTITY_KEYS = Set.of("id", "x", "y", "z", "keepPacked");
    /** Passengers are separate entities, checked on their own. */
    private static final Set<String> ENTITY_KEYS = Set.of("id", "Passengers");

    public ContinuumTarget(@Nullable MinecraftServer server) {
        this.server = server;
    }

    @Override
    public boolean hasBlock(String id) {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(id));
    }

    @Override
    public boolean hasItem(String id) {
        return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id));
    }

    @Override
    public boolean hasEntity(String id) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(id));
    }

    @Override
    public boolean hasComponent(String id) {
        return ContinuumStacks.hasComponent(id);
    }

    @Override
    public @Nullable List<String> advancementCriteria(String id) {
        if (server == null) return null;
        AdvancementHolder holder = server.getAdvancements().get(ResourceLocation.parse(id));
        return holder == null ? null : List.copyOf(holder.value().criteria().keySet());
    }

    @Override
    public Map<String, List<String>> properties(String id) {
        return props.computeIfAbsent(id, k -> {
            Map<String, List<String>> out = new LinkedHashMap<>();
            for (Property<?> p : block(k).getStateDefinition().getProperties()) out.put(p.getName(), values(p));
            return out;
        });
    }

    private static <T extends Comparable<T>> List<String> values(Property<T> property) {
        return property.getPossibleValues().stream().map(property::getName).toList();
    }

    @Override
    public @Nullable String blockEntityType(String id, CompoundTag properties) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Name", id);
        if (!properties.isEmpty()) entry.put("Properties", properties);
        return beTypes.computeIfAbsent(entry.toString(), k -> {
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry);
            if (!(state.getBlock() instanceof EntityBlock entityBlock)) return Optional.empty();
            BlockEntity be = entityBlock.newBlockEntity(BlockPos.ZERO, state);
            return be == null ? Optional.empty()
                    : Optional.of(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString());
        }).orElse(null);
    }

    @Override
    public boolean isMultiblockCore(String id) {
        return block(id) instanceof BlockMultiblockCore;
    }

    @Override
    public boolean isCoreState(String id, CompoundTag properties) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Name", id);
        if (!properties.isEmpty()) entry.put("Properties", properties);
        return MultiblockSurface.foldedCore(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry)) != null;
    }

    @Override
    public @Nullable CompoundTag filledComponents(String item, String fluid, int amount) {
        return ContinuumStacks.filledComponents(server == null ? null : server.registryAccess(), item, fluid, amount);
    }

    @Override
    public Map<Long, CompoundTag> footprint(String block, CompoundTag properties, long core, @Nullable Integer columnHeight) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Name", block);
        if (!properties.isEmpty()) entry.put("Properties", properties);
        BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry);
        Map<Long, CompoundTag> out = new LinkedHashMap<>();
        if (!(state.getBlock() instanceof BlockMultiblockCore machine)) return out;
        // the rule's own bounds; without a value the default, never the height of the world loaded last
        int height = columnHeight == null ? RBMKDials.COLUMN_HEIGHT.defaultValue() : Math.clamp(columnHeight, 2, 16);
        ImportedCells.footprint(machine, BlockPos.of(core), state.getValue(BlockMultiblockCore.FACING), height)
                .forEach((pos, cell) -> out.put(pos.asLong(), NbtUtils.writeBlockState(cell)));
        return out;
    }

    @Override
    public @Nullable String weaponModName(int id) {
        return ContinuumStacks.weaponModName(id);
    }

    @Override
    public boolean isPlacedConductor(String id) {
        Block block = block(id);
        return block instanceof BlockCable || block instanceof BlockCablePaintable || block instanceof WireCoated;
    }

    /**
     * Adds an airship's conductor to its level's power graph without loading its plot, as its placement hook would:
     * the graph keeps it while the ship is not loaded.
     */
    public static void joinPlotConductor(ServerLevel level, BlockPos pos) {
        PowerGraph.get(level).addNode(pos.asLong(), CableData.INSTANCE, MachineCaps.FACE_MASK_ALL);
    }

    /** Runs a conductor's own placement hook, which adds it to its level's network graph. */
    public static void announceConductor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockCable || state.getBlock() instanceof BlockCablePaintable
                || state.getBlock() instanceof WireCoated) state.onPlace(level, pos, Blocks.AIR.defaultBlockState(), false);
    }

    @Override
    public @Nullable String itemFor(String blockId) {
        Item item = block(blockId).asItem();
        return item == Items.AIR ? null : BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /**
     * An imported launch pad's redstone, counted the way its own neighbour updates count it, so a signal that was
     * already on is not a new edge: the pad fires when the signal next goes off and on, never on its first update.
     */
    @Override
    public boolean hasStructure(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (server == null || id.equals("INVALID")) return true;
        return rl != null && server.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(rl);
    }

    @Override
    public boolean loadsPiece(CompoundTag piece) {
        ResourceLocation rl = ResourceLocation.tryParse(piece.getString("id"));
        if (rl == null || !BuiltInRegistries.STRUCTURE_PIECE.containsKey(rl)) return false;
        if (server == null || !(piece.get("pool_element") instanceof CompoundTag element)) return true;
        // vanilla takes a partial element too, and a template it cannot find as an empty one
        return StructurePoolElement.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), element)
                .resultOrPartial(error -> {}).isPresent();
    }

    @Override
    public @Nullable String mixerRecipe(String fluid, int index) {
        FluidType type = Fluids.byRegistryName(fluid);
        MixerRecipes.MixerRecipe[] recipes = type == null ? null : MixerRecipes.getOutput(type);
        return recipes == null || recipes.length == 0 ? null : recipes[Math.floorMod(index, recipes.length)].name;
    }

    @Override
    public @Nullable String packRecipe(String fluid, boolean unpack) {
        FluidType type = Fluids.byRegistryName(fluid);
        if (type == null) return null;
        String name = (unpack ? "ass.unpackage" : "ass.package") + "hbmfluid." + type.getName().toLowerCase(Locale.ROOT);
        return AssemblyMachineRecipes.INSTANCE.getRecipe(name) == null ? null : name;
    }

    @Override
    public boolean hasSatelliteType(String type) {
        return SatelliteType.REGISTRY.get(type) != null;
    }

    /** Points every pile block inside DE's member box at its core, which Continuum keeps on the members. */
    @Override
    public int maxStackSize(String item) {
        ResourceLocation id = ResourceLocation.tryParse(item);
        return id == null || !BuiltInRegistries.ITEM.containsKey(id) ? 0 : BuiltInRegistries.ITEM.get(id).getDefaultMaxStackSize();
    }

    @Override
    public boolean canHoldRefund(CompoundTag state) {
        ResourceLocation id = ResourceLocation.tryParse(state.getString("Name"));
        // a block of a missing mod reads as air, and must not be written over
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return false;
        BlockState block = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), state);
        return block.isAir() || block.canBeReplaced();
    }

    @Override
    public CompoundTag refundChest() {
        return NbtUtils.writeBlockState(Blocks.CHEST.defaultBlockState());
    }

    @Override
    public boolean linkPileMember(CompoundTag be, long core) {
        if (!be.getString("id").equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ModBlockEntities.PILE_BLOCK.get()).toString())) return false;
        BlockPos pos = BlockPos.of(core);
        be.putInt("cX", pos.getX());
        be.putInt("cY", pos.getY());
        be.putInt("cZ", pos.getZ());
        return true;
    }

    /** Points cells at their core in the level's core index, without loading them; returns how many. */
    public static int indexCells(ServerLevel level, BlockPos core, long[] cells) {
        for (long cell : cells) MultiblockCoreIndex.put(level, BlockPos.of(cell), core);
        return cells.length;
    }

    /**
     * Takes out the core index entries and the cable node an older importer put at an airship's place in its plot, near
     * the level's origin; returns how many. A real machine or cable standing there keeps its own.
     */
    public static int dropStale(ServerLevel level, BlockPos pos, CompoundTag record) {
        List<BlockPos> cells = new ArrayList<>();
        for (long cell : record.getLongArray("cells"))
            if (MultiblockCoreIndex.lookup(level, BlockPos.of(cell)) == pos.asLong()) cells.add(BlockPos.of(cell));
        boolean node = record.getBoolean("conductor") && PowerGraph.get(level).containsCell(pos.asLong());
        // the block is read only where something was put, so no chunk loads for nothing
        if (cells.isEmpty() && !node) return 0;
        BlockState state = level.getBlockState(pos);
        int removed = 0;
        if (!(state.getBlock() instanceof BlockMultiblockCore)) {
            for (BlockPos cell : cells) MultiblockCoreIndex.remove(level, cell);
            removed += cells.size();
        }
        boolean owned = state.getBlock() instanceof BlockCable || state.getBlock() instanceof BlockCablePaintable
                || state.getBlock() instanceof WireCoated || level.getBlockEntity(pos) != null;
        if (node && !owned) {
            PowerGraph.get(level).removeNode(pos.asLong());
            removed++;
        }
        return removed;
    }

    public static int linkPile(ServerLevel level, BlockPos core, int[] box) {
        int linked = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box[0], box[1], box[2], box[3], box[4], box[5])) {
            if (!(level.getBlockEntity(pos) instanceof TileEntityPileBaseMK2 member)) continue;
            member.coreX = core.getX();
            member.coreY = core.getY();
            member.coreZ = core.getZ();
            member.setChanged();
            linked++;
        }
        return linked;
    }

    @Override
    public int minSection(String dim) {
        if (server == null) return -4;
        LevelStem stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(ResourceLocation.parse(dim));
        return stem == null ? -4 : stem.type().value().minY() >> 4;
    }

    @Override
    public @Nullable List<String> undecodable(CompoundTag stack) {
        return ContinuumStacks.undecodable(server == null ? null : server.registryAccess(), stack);
    }

    @Override
    public @Nullable Map<String, Tag> blockEntityLost(String block, CompoundTag properties, CompoundTag tag) {
        if (server == null) return null;
        CompoundTag entry = new CompoundTag();
        entry.putString("Name", block);
        if (!properties.isEmpty()) entry.put("Properties", properties);
        BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry);
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        try {
            if (!(state.getBlock() instanceof EntityBlock entityBlock)) return null;
            BlockEntity fresh = entityBlock.newBlockEntity(pos, state);
            CompoundTag shape = fresh == null ? new CompoundTag() : fresh.saveWithoutMetadata(server.registryAccess());
            Map<String, Tag> narrowed = NbtDiff.conformNumbers(tag, shape);
            // vanilla's loadStatic swallows a failed load and returns null; the chunk load would then drop it too
            BlockEntity be = BlockEntity.loadStatic(pos, state, tag.copy(), server.registryAccess());
            if (be == null) return NbtDiff.everything(tag, BLOCK_ENTITY_KEYS);
            Map<String, Tag> lost = NbtDiff.withoutDefaults(NbtDiff.lost(tag, be.saveWithoutMetadata(server.registryAccess()), BLOCK_ENTITY_KEYS), shape);
            lost.putAll(narrowed);
            return lost;
        } catch (RuntimeException e) {
            // a block entity whose load or save needs a level; counted as unchecked
            return null;
        }
    }

    /** Re-saves a converted entity through Continuum and returns what it did not keep; null if it cannot be created. */
    public static @Nullable Map<String, Tag> entityLost(ServerLevel level, CompoundTag tag, boolean player) {
        CompoundTag probe = tag;
        if (player) {
            probe = new CompoundTag();
            probe.putString("id", "minecraft:pig");
            probe.put("neoforge:attachments", tag.getCompound("neoforge:attachments"));
        }
        Entity entity;
        try {
            entity = EntityType.create(probe.copy(), level).orElse(null);
        } catch (RuntimeException e) {
            // vanilla's chunk load catches the same failure and drops the entity
            entity = null;
        }
        if (entity == null) return NbtDiff.everything(player ? probe : tag, ENTITY_KEYS);
        try {
            return NbtDiff.lost(player ? probe : tag, entity.saveWithoutId(new CompoundTag()), ENTITY_KEYS);
        } catch (RuntimeException e) {
            // an entity whose save needs more world than a fresh probe has; counted as unchecked
            return null;
        }
    }

    public static boolean settleRedstone(ServerLevel level, BlockPos core) {
        BlockState state = level.getBlockState(core);
        if (!(level.getBlockEntity(core) instanceof TileEntityLaunchPadBase pad)
                || !(state.getBlock() instanceof BlockMultiblockCore machine)) return false;
        List<BlockPos> parts = new ArrayList<>(ImportedCells.footprint(machine, core, state.getValue(BlockMultiblockCore.FACING)).keySet());
        parts.add(core);
        List<Integer> powered = new ArrayList<>();
        for (BlockPos pos : parts) {
            if (!level.hasNeighborSignal(pos)) continue;
            powered.add(pos.getX());
            powered.add(pos.getY());
            powered.add(pos.getZ());
        }
        int count = powered.size() / 3;
        CompoundTag tag = pad.saveWithoutMetadata(level.registryAccess());
        tag.putIntArray("activatedBlocks", powered);
        tag.putInt("redstonePower", count);
        tag.putInt("prevRedstonePower", count);
        pad.loadWithComponents(tag, level.registryAccess());
        pad.setChanged();
        return count > 0;
    }

    /** Sets the radiation of the pocket holding {@code pos}, loading its chunk. */
    public static void setRadiation(ServerLevel level, BlockPos pos, double density) {
        level.getChunk(pos);
        RadiationSystemNT.setRadForCoord(level, pos, density);
    }

    /** DE's {@code rbmk} machine config keys as Continuum's RBMK world rules. */
    private static final Map<String, WorldRules.Rule<?>> RBMK = Map.ofEntries(
            Map.entry("passive_cooling", RBMKDials.PASSIVE_COOLING), Map.entry("passive_cooling_inner", RBMKDials.PASSIVE_COOLING_INNER),
            Map.entry("column_heat_flow", RBMKDials.COLUMN_HEAT_FLOW), Map.entry("diffusion_mod", RBMKDials.FUEL_DIFFUSION_MOD),
            Map.entry("heat_provision", RBMKDials.HEAT_PROVISION), Map.entry("column_height", RBMKDials.COLUMN_HEIGHT),
            Map.entry("permanent_scrap", RBMKDials.PERMANENT_SCRAP), Map.entry("boiler_heat_consumption", RBMKDials.BOILER_HEAT_CONSUMPTION),
            Map.entry("control_speed", RBMKDials.CONTROL_SPEED), Map.entry("reactivity_mod", RBMKDials.REACTIVITY_MOD),
            Map.entry("outgasser_speed_mod", RBMKDials.OUTGASSER_MOD), Map.entry("control_surge_mod", RBMKDials.SURGE_MOD),
            Map.entry("flux_range", RBMKDials.FLUX_RANGE), Map.entry("reasim_boilers", RBMKDials.REASIM_BOILERS),
            Map.entry("reasim_boiler_speed", RBMKDials.REASIM_BOILER_SPEED), Map.entry("disable_meltdowns", RBMKDials.DISABLE_MELTDOWNS),
            Map.entry("enable_meltdown_overpressure", RBMKDials.MELTDOWN_OVERPRESSURE),
            Map.entry("moderator_efficiency", RBMKDials.MODERATOR_EFFICIENCY), Map.entry("absorber_efficiency", RBMKDials.ABSORBER_EFFICIENCY),
            Map.entry("reflector_efficiency", RBMKDials.REFLECTOR_EFFICIENCY), Map.entry("disable_depletion", RBMKDials.DISABLE_DEPLETION),
            Map.entry("disable_xenon", RBMKDials.DISABLE_XENON), Map.entry("absorber_heat_conversion", RBMKDials.ABSORBER_HEAT_CONVERSION));

    /** Sets the RBMK rule DE's {@code key} names; false when Continuum has none. */
    public static boolean setRbmkDial(MinecraftServer server, String key, JsonElement value) {
        WorldRules.Rule<?> rule = RBMK.get(key);
        if (rule instanceof WorldRules.BoolRule bool) bool.set(server, value.getAsBoolean());
        else if (rule instanceof WorldRules.IntRule integer) integer.set(server, value.getAsInt());
        else if (rule instanceof WorldRules.DoubleRule decimal) decimal.set(server, value.getAsDouble());
        else return false;
        return true;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }
}
