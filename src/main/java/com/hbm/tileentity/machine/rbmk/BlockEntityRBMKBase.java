package com.hbm.tileentity.machine.rbmk;

import dev.golem.core.data.Tags;

import com.hbm.advancement.AwardRegions;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.BlockFluidDuct;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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

    @Override public void setRemoved() { super.setRemoved(); if (level != null) NeutronNodeWorld.removeNode(level, worldPosition); }
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
        double x = random.nextGaussian() * .25D, y = .5D + random.nextDouble() * 1.5D, z = random.nextGaussian() * .25D;
        if (type == EntityRBMKDebris.DebrisType.LID) { x *= .5D; y += .5D; z *= .5D; }
        debris.setDeltaMovement(x, y, z); level.addFreshEntity(debris);
    }
    protected void standardMelt(int reduce) {
        int height = RBMKDials.getColumnHeight(level); reduce = Mth.clamp(reduce, 1, height);
        if (level.getRandom().nextInt(3) == 0) reduce++;
        for (int i = height; i >= 0; i--) level.setBlock(worldPosition.above(i), i <= height + 1 - reduce
                ? (reduce > 1 && i == height + 1 - reduce ? ModRBMKBlocks.rbmk_debris_burning : ModRBMKBlocks.rbmk_debris).get().defaultBlockState()
                : Blocks.AIR.defaultBlockState(), 3);
    }
    public void meltdown() {
        if (com.hbm.scripting.NTMServerEvents.postMeltdown(level, worldPosition, "rbmk")) return;
        columns.clear(); floodFill();
        int minX = worldPosition.getX(), maxX = minX, minZ = worldPosition.getZ(), maxZ = minZ;
        for (BlockEntityRBMKBase column : columns) { minX = Math.min(minX, column.worldPosition.getX()); maxX = Math.max(maxX, column.worldPosition.getX()); minZ = Math.min(minZ, column.worldPosition.getZ()); maxZ = Math.max(maxZ, column.worldPosition.getZ()); }
        final int x0 = minX, x1 = maxX, z0 = minZ, z1 = maxZ;
        BlockMultiblockCore.withoutTeardown(() -> { for (BlockEntityRBMKBase column : columns) { BlockPos p = column.worldPosition; column.onMelt(Math.min(Math.min(p.getX() - x0, x1 - p.getX()), Math.min(p.getZ() - z0, z1 - p.getZ())) + 1); } });
        boolean digamma = RBMKBase.digamma;
        BlockState hot = (digamma ? ModRBMKBlocks.rbmk_debris_digamma : ModRBMKBlocks.rbmk_debris_radiating).get().defaultBlockState();
        for (BlockEntityRBMKBase column : columns) if (column instanceof BlockEntityRBMKRod && level.getBlockState(column.worldPosition).is(ModHazardBlocks.block_corium.get())) for (BlockPos p : BlockPos.betweenClosed(column.worldPosition.offset(-1, -1, -1), column.worldPosition.offset(1, 1, 1))) if (level.getRandom().nextInt(3) == 0 && (level.getBlockState(p).is(ModRBMKBlocks.rbmk_debris.get()) || level.getBlockState(p).is(ModRBMKBlocks.rbmk_debris_burning.get()))) level.setBlock(p, hot, 3);
        double x = minX + (maxX - minX) / 2D + .5D, z = minZ + (maxZ - minZ) / 2D + .5D;
        ExplosionCreator.composeEffectRBMKMush(level, x, worldPosition.getY() + 1D, z);
        level.playSound(null, x, worldPosition.getY() + 1D, z, com.hbm.sound.ModSounds.RBMK_EXPLOSION.get(), SoundSource.BLOCKS, 50F, 1F);
        if (level instanceof ServerLevel server) {
            AwardRegions.within(server, worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D,
                    50D, player -> Advancements.award(player, "hbm:rbmk_meltdown", "meltdown"));
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
    /** Ruptures the duct component touching the melted core and destroys every attached receiver. */
    private void overpressure(ServerLevel server) {
        Set<Long> pipes = new java.util.HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        for (BlockEntityRBMKBase column : columns) for (Direction direction : Direction.values()) {
            BlockPos pipe = column.worldPosition.relative(direction);
            if (server.getBlockState(pipe).getBlock() instanceof BlockFluidDuct && pipes.add(pipe.asLong())) queue.add(pipe);
        }
        if (pipes.isEmpty()) return;
        Set<BlockPos> receivers = new java.util.HashSet<>();
        for (int safety = 10_000; !queue.isEmpty() && safety-- > 0;) {
            BlockPos pipe = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pipe.relative(direction);
                if (server.getBlockState(neighbor).getBlock() instanceof BlockFluidDuct) {
                    if (pipes.add(neighbor.asLong())) queue.add(neighbor);
                } else if (server.getBlockEntity(neighbor) instanceof IFluidReceiverMK2) receivers.add(neighbor.immutable());
                else {
                    BlockPos core = MultiblockSurface.coreOfCell(server, neighbor);
                    if (core != null && server.getBlockEntity(core) instanceof IFluidReceiverMK2) receivers.add(core);
                }
            }
        }
        int destroyed = 0, max = Math.min(pipes.size() / 5, 100);
        for (long pipe : pipes) { if (destroyed++ >= max) break; server.setBlock(BlockPos.of(pipe), Blocks.AIR.defaultBlockState(), 3); }
        for (BlockPos receiver : receivers) {
            BlockEntity blockEntity = server.getBlockEntity(receiver);
            if (blockEntity instanceof IOverpressurable overpressurable) overpressurable.explode(server, receiver);
            else {
                server.setBlock(receiver, Blocks.AIR.defaultBlockState(), 3);
                server.explode(null, receiver.getX() + .5D, receiver.getY() + .5D, receiver.getZ() + .5D, 5F,
                        net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
        }
    }
    private void floodFill() {
        Queue<BlockPos> queue = new ArrayDeque<>(); queue.add(worldPosition);
        for (int safety = 50000; !queue.isEmpty() && safety-- > 0;) { BlockPos p = queue.remove(); if (level.getBlockEntity(p) instanceof BlockEntityRBMKBase column && columns.add(column)) for (Direction direction : NEIGHBORS) queue.add(p.relative(direction)); }
    }
    @Override protected void loadData(DataIn input) { super.loadData(input); heat = input.getDoubleOr("heat", heat); reasimWater = input.getIntOr("reasimWater", reasimWater); reasimSteam = input.getIntOr("reasimSteam", reasimSteam); }
    @Override protected void saveData(DataOut output) { super.saveData(output); output.putDouble("heat", heat); output.putInt("reasimWater", reasimWater); output.putInt("reasimSteam", reasimSteam); }
    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); buf.writeDouble(heat); buf.writeInt(reasimWater); buf.writeInt(reasimSteam); buf.writeByte(craneIndicator); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); heat = buf.readDouble(); reasimWater = buf.readInt(); reasimSteam = buf.readInt(); craneIndicator = buf.readByte(); }
}
