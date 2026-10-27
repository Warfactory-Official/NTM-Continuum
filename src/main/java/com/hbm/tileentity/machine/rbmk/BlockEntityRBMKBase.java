package com.hbm.tileentity.machine.rbmk;

import dev.golem.core.world.Explosions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.Tags;

import com.hbm.advancement.AwardRegions;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKLoader;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.entity.effect.EntitySpear;
import com.hbm.entity.ModRBMKEntities;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import com.hbm.interfaces.IOverpressurable;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.world.Advancements;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import io.netty.buffer.Unpooled;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TilePort.PortDef;

/** Shared RBMK heat, ReaSim boiler, sync, and connected-core meltdown implementation. */
public abstract class BlockEntityRBMKBase extends TileEntityMachineBase implements IRBMKTicking {
    public static final int maxWater = 16000, maxSteam = 16000;
    public static final Set<BlockEntityRBMKBase> columns = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Direction[] NEIGHBORS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public static boolean explodeOnBroken = true;
    protected final BlockEntityRBMKBase[] neighborCache = new BlockEntityRBMKBase[4];
    public double heat = 20D;
    public int reasimWater, reasimSteam, craneIndicator;
    private boolean topExtraChecked;

    protected BlockEntityRBMKBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) { super(type, pos, state, slots); }
    @Override public String getDefaultName() { return "container.rbmk"; }
    public boolean hasLid() { return !isLidRemovable() || RBMKBase.lidOf(getBlockState()).present(); }
    public boolean isLidRemovable() { return true; }
    public double maxHeat() { return 1500D; }
    public int trackingRange() { return 15; }
    public double passiveCooling(int neighbors) {
        double min = RBMKDials.getPassiveCoolingInner(level), max = RBMKDials.getPassiveCooling(level);
        return min + (max - min) * ((4 - Mth.clamp(neighbors, 0, 4)) / 4D);
    }

    public void tickServer() {
        ensureTopExtra();
        if (craneIndicator > 0) craneIndicator--;
        moveHeat();
        if (RBMKDials.getReasimBoilers(level)) boilWater();
        networkPackNT();
    }

    /** Repair a top segment missed while its chunk was unavailable during column placement. */
    private void ensureTopExtra() {
        if (topExtraChecked) return;

        BlockPos top = worldPosition.above(RBMKDials.getColumnHeight(level));
        if (!level.hasChunkAt(top)) return;

        topExtraChecked = true;
        BlockState state = level.getBlockState(top);
        if (state.getBlock() != getBlockState().getBlock()) return;
        if (state.getValue(RBMKBase.PART) == RBMKBase.Part.TOP) return;
        level.setBlock(top, state.setValue(RBMKBase.PART, RBMKBase.Part.TOP), 3);
    }

    private void boilWater() {
        if (heat < 100D) return;
        double consumption = RBMKDials.getBoilerHeatConsumption(level);
        int processed = (int) Math.floor(Math.min((heat - 100D) / consumption,
                Math.min(reasimWater, maxSteam - reasimSteam)) * Mth.clamp(RBMKDials.getReaSimBoilerSpeed(level), 0D, 1D));
        if (processed > 0) { reasimWater -= processed; reasimSteam += processed; heat -= processed * consumption; }
    }

    private void moveHeat() {
        boolean reasim = RBMKDials.getReasimBoilers(level);
        double totalHeat = heat; int totalWater = reasimWater, totalSteam = reasimSteam, members = 1;
        for (int i = 0; i < 4; i++) {
            if (neighborCache[i] != null && neighborCache[i].isRemoved()) neighborCache[i] = null;
            if (neighborCache[i] == null && level.getBlockEntity(worldPosition.relative(NEIGHBORS[i])) instanceof BlockEntityRBMKBase other) neighborCache[i] = other;
            if (neighborCache[i] != null) { members++; totalHeat += neighborCache[i].heat; if (reasim) { totalWater += neighborCache[i].reasimWater; totalSteam += neighborCache[i].reasimSteam; } }
        }
        if (members > 1) {
            double target = totalHeat / members, flow = RBMKDials.getColumnHeatFlow(level);
            int water = totalWater / members, steam = totalSteam / members;
            for (BlockEntityRBMKBase other : neighborCache) if (other != null) { other.heat += (target - other.heat) * flow; if (reasim) { other.reasimWater = water; other.reasimSteam = steam; } }
            heat += (target - heat) * flow;
            if (reasim) { reasimWater = water + totalWater % members; reasimSteam = steam + totalSteam % members; }
            markChanged();
        }
        heat = Math.max(20D, heat - passiveCooling(members - 1));
    }

    // port: 1.7 never destroys these ports when a column goes, which leaves a passthrough node behind
    @Override public void setRemoved() { super.setRemoved(); if (level != null) { NeutronNodeWorld.removeNode(level, worldPosition); if (ports != null) ports.destroy(); } }

    /** The fluid ports of a column with tanks; null for the rest. */
    protected @Nullable ModulePortManFluidAdaptive ports;

    protected PortDef[] portShapeNoLoader;
    public PortDef[] getPortsNoLoader() {
        if (portShapeNoLoader == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            portShapeNoLoader = new PortDef[] {
                    PortDef.make(x, y + RBMKDials.getColumnHeight(level), z, Library.POS_Y),
                    PortDef.make(x, y, z, Library.NEG_Y),
            };
        }
        return portShapeNoLoader;
    }

    protected PortDef[] portShapeCloseLoader;
    public PortDef[] getPortsCloseLoader() {
        if (portShapeCloseLoader == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            portShapeCloseLoader = new PortDef[] {
                    PortDef.make(x, y + RBMKDials.getColumnHeight(level), z, Library.POS_Y),
                    PortDef.make(x, y - 1, z, ForgeDirection.VALID_DIRECTIONS),
            };
        }
        return portShapeCloseLoader;
    }

    protected PortDef[] portShapeClassicLoader;
    public PortDef[] getPortsClassicLoader() {
        if (portShapeClassicLoader == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            portShapeClassicLoader = new PortDef[] {
                    PortDef.make(x, y + RBMKDials.getColumnHeight(level), z, Library.POS_Y),
                    PortDef.make(x, y, z, Library.NEG_Y),
                    PortDef.make(x, y - 2, z, ForgeDirection.VALID_DIRECTIONS),
            };
        }
        return portShapeClassicLoader;
    }

    /** 1.7 checks for a loader two below first, then one below. */
    public PortDef[] getLoaderPorts() {
        if (level.getBlockState(worldPosition.below(2)).getBlock() instanceof RBMKLoader) return getPortsClassicLoader();
        if (level.getBlockState(worldPosition.below()).getBlock() instanceof RBMKLoader) return getPortsCloseLoader();
        return getPortsNoLoader();
    }
    public RBMKType getRBMKType() { return RBMKType.OTHER; }
    public boolean isModerated() { return false; }
    public abstract RBMKColumnType getConsoleType();
    public RBMKColumn getConsoleData() {
        RBMKColumn result = RBMKColumn.createForType(getConsoleType());
        result.heat = heat; result.maxHeat = maxHeat(); result.moderated = isModerated();
        result.reasimWater = reasimWater; result.reasimSteam = reasimSteam; result.indicator = craneIndicator;
        return result;
    }
    public void onOverheat() { for (int i = 0; i < 4; i++) level.setBlockAndUpdate(worldPosition.above(i), Blocks.LAVA.defaultBlockState()); }
    public void onMelt(int reduce) { standardMelt(reduce); if (RBMKBase.lidOf(getBlockState()) == RBMKBase.Lid.CONCRETE) spawnDebris(EntityRBMKDebris.DebrisType.LID); }
    protected void spawnDebris(EntityRBMKDebris.DebrisType type) {
        RandomSource random = level.getRandom();
        EntityRBMKDebris debris = new EntityRBMKDebris(level, worldPosition.getX() + .5D, worldPosition.getY() + 4D, worldPosition.getZ() + .5D, type);
        double x = random.nextGaussian() * .25D, y = .25D + random.nextDouble() * 1.25D, z = random.nextGaussian() * .25D;
        if (type == EntityRBMKDebris.DebrisType.LID) { x *= .5D; y += .5D; z *= .5D; }
        debris.setDeltaMovement(x, y, z); level.addFreshEntity(debris);
    }
    protected void spawnDebris(EntityRBMKDebris.DebrisType type, int count) { for (int i = 0; i < count; i++) spawnDebris(type); }
    protected void standardMelt(int reduce) {
        int height = RBMKDials.getColumnHeight(level); reduce = Mth.clamp(reduce, 1, height);
        if (level.getRandom().nextInt(3) == 0) reduce++;
        for (int i = height; i >= 0; i--) level.setBlock(worldPosition.above(i), i <= height + 1 - reduce
                ? (reduce > 1 && i == height + 1 - reduce ? ModRBMKBlocks.rbmk_debris_burning : ModRBMKBlocks.rbmk_debris).get().defaultBlockState()
                : Blocks.AIR.defaultBlockState(), 3);
    }
    public void meltdown() {
        columns.clear(); columns.add(this); floodFill();
        int minX = worldPosition.getX(), maxX = minX, minZ = worldPosition.getZ(), maxZ = minZ;
        for (BlockEntityRBMKBase column : columns) { minX = Math.min(minX, column.worldPosition.getX()); maxX = Math.max(maxX, column.worldPosition.getX()); minZ = Math.min(minZ, column.worldPosition.getZ()); maxZ = Math.max(maxZ, column.worldPosition.getZ()); }
        final int x0 = minX, x1 = maxX, z0 = minZ, z1 = maxZ;
        RBMKBase.dropLids = false;
        boolean digamma;
        try {
            BlockMultiblockCore.withoutTeardown(() -> { for (BlockEntityRBMKBase column : columns) { BlockPos p = column.worldPosition; column.onMelt(Math.min(Math.min(p.getX() - x0, x1 - p.getX()), Math.min(p.getZ() - z0, z1 - p.getZ())) + 1); } });
            digamma = RBMKBase.digamma;
            BlockState hot = (digamma ? ModRBMKBlocks.rbmk_debris_digamma : ModRBMKBlocks.rbmk_debris_radiating).get().defaultBlockState();
            for (BlockEntityRBMKBase column : columns) if (column instanceof BlockEntityRBMKRod && level.getBlockState(column.worldPosition).is(ModFluidNetBlocks.corium_block.get())) for (BlockPos p : BlockPos.betweenClosed(column.worldPosition.offset(-1, -1, -1), column.worldPosition.offset(1, 1, 1))) if (level.getRandom().nextInt(3) == 0 && (level.getBlockState(p).is(ModRBMKBlocks.rbmk_debris.get()) || level.getBlockState(p).is(ModRBMKBlocks.rbmk_debris_burning.get()))) level.setBlock(p, hot, 3);
        } finally { RBMKBase.dropLids = true; }
        double x = minX + (maxX - minX) / 2D + .5D, z = minZ + (maxZ - minZ) / 2D + .5D;
        ExplosionCreator.composeEffectRBMKMush(level, x, worldPosition.getY() + 1D, z);
        level.playSound(null, x, worldPosition.getY() + 1D, z, ModSounds.RBMK_EXPLOSION.get(), SoundSource.BLOCKS, 50F, 1F);
        if (level instanceof ServerLevel server) {
            AwardRegions.within(server, worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D,
                    50D, player -> HbmCriteria.detonation(player, DetonationTrigger.Kind.RBMK));
            if (RBMKDials.getOverpressure(level)) overpressure(server);
        }
        if (digamma) {
            RBMKBase.digamma = false;
            EntitySpear spear = new EntitySpear(ModRBMKEntities.SPEAR.get(), level);
            spear.setPos(x, worldPosition.getY() + 100D, z);
            level.addFreshEntity(spear);
        }
        columns.clear();
    }
    /** Ruptures the fluid components connected to the columns' actual output contacts. */
    private void overpressure(ServerLevel server) {
        var graph = FluidGraph.get(server);
        var seeds = new IdentityHashMap<FluidType, Set<NodeNetwork<DuctData>>>();
        for (var column : columns) {
            if (column.ports == null || column.ports.outTanks == null) continue;
            for (var contact : PortDef.connections(column.getLoaderPorts())) {
                var pos = contact.getPos();
                if (!pressureResident(server, pos)) continue;
                var data = graph.dataAt(pos.asLong());
                if (data == null || !graph.isOpenAt(pos.asLong(), contact.getDir().getOpposite().toDirection())) continue;
                for (var tank : column.ports.outTanks) {
                    var type = tank.getTankType();
                    if (!data.carries(type)) continue;
                    var net = graph.networkAt(pos.asLong());
                    if (net != null) seeds.computeIfAbsent(type,
                            fluid -> Collections.newSetFromMap(new IdentityHashMap<>())).add(net);
                }
            }
        }

        var cells = new LongLinkedOpenHashSet();
        Set<BlockEntity> receivers = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var entry : seeds.entrySet()) {
            var type = entry.getKey();
            for (var joined : PortNodes.nets(server, graph, graph.networks(), FluidNetProvider.of(type),
                    data -> data.carries(type))) {
                if (Collections.disjoint(joined.nets, entry.getValue())) continue;
                for (var net : joined.nets) {
                    for (var node : net.nodes) collectPressure(server, graph, node.posKey, type, cells, receivers);
                    for (var run : net.segments) {
                        for (int i = 0; i < run.cellCount(); i++) {
                            collectPressure(server, graph, run.cellAt(i), type, cells, receivers);
                        }
                    }
                }
                for (var port : joined.ports) {
                    for (var owner : port.receivers) {
                        if (owner instanceof BlockEntity be && owner instanceof IFluidReceiverMK2 receiver
                                && pressureSubscriber(receiver, type)) receivers.add(be);
                    }
                }
            }
        }
        int destroyed = 0, max = Math.min(cells.size() / 5, 100);
        for (long cell : cells) {
            if (destroyed++ >= max) break;
            server.setBlock(BlockPos.of(cell), Blocks.AIR.defaultBlockState(), 3);
        }
        for (var blockEntity : receivers) {
            var pos = blockEntity.getBlockPos();
            if (blockEntity instanceof IOverpressurable overpressurable) overpressurable.explode(server, pos);
            else {
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                Explosions.explode(server, null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, 5F,
                        Level.ExplosionInteraction.NONE);
            }
        }
    }

    private static void collectPressure(ServerLevel server, LevelNodeGraph<DuctData> graph, long cell,
            FluidType type, LongLinkedOpenHashSet cells, Set<BlockEntity> receivers) {
        var pos = BlockPos.of(cell);
        if (!pressureResident(server, pos)) return;
        cells.add(cell);
        for (var direction : Direction.values()) {
            if (!graph.isOpenAt(cell, direction)) continue;
            var contact = pos.relative(direction);
            if (!pressureResident(server, contact)) continue;
            var be = FluidNetwork.userAtFace(server, contact, direction.getOpposite(), type);
            if (be instanceof IFluidReceiverMK2 receiver && !PortNodes.isPorted(server, receiver, false)
                    && pressureSubscriber(receiver, type)) receivers.add(be);
        }
    }

    private static boolean pressureSubscriber(IFluidReceiverMK2 receiver, FluidType type) {
        if (receiver instanceof IFluidStandardReceiverMK2 standard) {
            for (var tank : standard.getReceivingTanks()) {
                if (tank.getTankType() == type && receiver.getReceiverSpeed(type, tank.getPressure()) > 0) return true;
            }
            return false;
        }
        var range = receiver.getReceivingPressureRange(type);
        for (int pressure = range[0]; pressure <= range[1]; pressure++) {
            if (receiver.getReceiverSpeed(type, pressure) > 0) return true;
        }
        return false;
    }

    private static boolean pressureResident(ServerLevel server, BlockPos pos) {
        return server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }
    private void floodFill() {
        Queue<BlockPos> queue = new ArrayDeque<>(); Set<BlockPos> seen = new HashSet<>(); queue.add(worldPosition); seen.add(worldPosition);
        for (int safety = 50000; !queue.isEmpty() && safety-- > 0;) {
            BlockPos p = queue.remove();
            BlockEntity at = p.equals(worldPosition) ? this : level.getBlockEntity(p);
            if (!(at instanceof BlockEntityRBMKBase column)) continue;
            columns.add(column);
            for (Direction direction : NEIGHBORS) { BlockPos next = p.relative(direction); if (seen.add(next)) queue.add(next); }
        }
    }
    @Override protected void loadData(DataIn input) { super.loadData(input); heat = input.getDoubleOr("heat", heat); reasimWater = input.getIntOr("reasimWater", reasimWater); reasimSteam = input.getIntOr("reasimSteam", reasimSteam); }
    @Override protected void saveData(DataOut output) { super.saveData(output); output.putDouble("heat", heat); output.putInt("reasimWater", reasimWater); output.putInt("reasimSteam", reasimSteam); }
    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); buf.writeDouble(heat); buf.writeInt(reasimWater); buf.writeInt(reasimSteam); buf.writeByte(craneIndicator); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); heat = buf.readDouble(); reasimWater = buf.readInt(); reasimSteam = buf.readInt(); craneIndicator = buf.readByte(); }
}
