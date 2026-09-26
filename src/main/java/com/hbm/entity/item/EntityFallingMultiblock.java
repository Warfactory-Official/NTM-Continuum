package com.hbm.entity.item;

import dev.golem.core.data.Tags;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.multiblock.MultiblockCore;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.special.CarriedBlockItems;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A folded multiblock falling AS ONE RIGID BODY; the port deliberately diverges from 1.7:201 - it falls as a whole, or not at all when any column is still supported.
 */
public class EntityFallingMultiblock extends CompatEntity implements StoredItems {

    private static final Logger LOGGER = LoggerFactory.getLogger("NTM");
    private static final double GRAVITY = 0.04D;
    private static final double DRAG = 0.98D;
    private static final int RESEND_INTERVAL = 20;
    /**
     * Absolute member positions at the ORIGIN drop, for the self-occupancy half of the step test.
     */
    private final LongOpenHashSet memberPosSet = new LongOpenHashSet();
    private BlockPos originCore = BlockPos.ZERO;
    /**
     * {@code (Block.BLOCK_STATE_REGISTRY id) << 32 | packOffset(dx, dy, dz)}; offset {@code (0,0,0)} is the core.
     */
    private long[] members = new long[0];
    private @Nullable CompoundTag carriedBe;
    private @Nullable BlockEntity carriedInventory;
    private @Nullable BlockEntity carriedView;
    private int committedDrop;
    private double fallProgress;
    private double fallVelocity;
    public EntityFallingMultiblock(EntityType<? extends EntityFallingMultiblock> type, Level level) {
        super(type, level);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (carriedBe == null || !(level() instanceof ServerLevel server)) return;
        if (carriedInventory == null) {
            carriedInventory = CarriedBlockItems.load(coreState(), originCore, carriedBe, server.registryAccess());
        }
        if (carriedInventory != null && visitor.visit(carriedInventory)) {
            carriedBe = dev.golem.core.registry.BlockEntities.saveWithoutMetadata(carriedInventory, server.registryAccess());
        }
    }

    /**
     * dx 6 bits, dy 10 bits, dz 6 bits, biased: a member offset packs into the low 22 bits of its long.
     */
    private static int packOffset(int dx, int dy, int dz) {
        return (dx & 0x3F) | ((dy & 0x3FF) << 6) | ((dz & 0x3F) << 16);
    }

    private static int offsetX(int packed) {
        return (packed << 26) >> 26;
    }

    private static int offsetY(int packed) {
        return (packed << 16) >> 22;
    }

    private static int offsetZ(int packed) {
        return (packed << 10) >> 26;
    }

    /**
     * Evaluate the structure at {@code core}; remove it and spawn the falling entity when every member can take one step down - all-or-nothing. Null when the core no longer resolves or a first step is obstructed.
     */
    public static @Nullable EntityFallingMultiblock tryFall(ServerLevel level, BlockPos core) {
        BlockState coreState = level.getBlockState(core);
        if (MultiblockSurface.foldedCore(coreState) == null) return null;
        // teardown needs entity ticking: else removed and frozen
        if (!level.isPositionEntityTicking(core)) return null;

        List<BlockPos> blocks = MultiblockSurface.rigidStructureBlocks(level, core);
        if (blocks.isEmpty()) return null;

        EntityFallingMultiblock entity = new EntityFallingMultiblock(ModNtmEntities.FALLING_MULTIBLOCK.get(), level);
        entity.originCore = core.immutable();
        for (BlockPos member : blocks) entity.memberPosSet.add(member.asLong());

        long[] packed = new long[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos member = blocks.get(i);
            BlockState state = level.getBlockState(member);
            int offset = packOffset(member.getX() - core.getX(), member.getY() - core.getY(),
                    member.getZ() - core.getZ());
            packed[i] = (long) Block.BLOCK_STATE_REGISTRY.getId(state) << 32 | (offset & 0x3FFFFFL);
        }
        entity.members = packed;

        if (!entity.canStep(level, 1)) return null;

        BlockEntity be = level.getBlockEntity(core);
        if (be != null) entity.carriedBe = dev.golem.core.registry.BlockEntities.saveWithoutMetadata(be, level.registryAccess());

        // relocation, not a break: teardown suppressed, BE effects off
        MultiblockCore.withoutTeardown(() -> {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (BlockPos member : blocks) {
                level.setBlock(member, air, Block.UPDATE_ALL | dev.golem.core.world.Compat.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
        });

        entity.setPos(core.getX() + 0.5, core.getY(), core.getZ() + 0.5);
        level.addFreshEntity(entity);
        entity.sendMembers();
        return entity;
    }

    public static int offsetXOf(long packed) {
        return offsetX((int) (packed & 0x3FFFFFL));
    }

    public static int offsetYOf(long packed) {
        return offsetY((int) (packed & 0x3FFFFFL));
    }

    public static int offsetZOf(long packed) {
        return offsetZ((int) (packed & 0x3FFFFFL));
    }

    public static BlockState stateOf(long packed) {
        return Block.BLOCK_STATE_REGISTRY.byId((int) (packed >>> 32));
    }

    private @Nullable BlockState coreState() {
        for (long member : members) {
            if ((member & 0x3FFFFFL) == 0) return stateOf(member);
        }
        return null;
    }

    /**
     * The step test, applied to EVERY member: free when a structure member vacates the target this same step (gapped columns), or when the world there is air or a replaceable non-liquid.
     */
    private boolean canStep(ServerLevel level, int drop) {
        for (long packed : members) {
            BlockPos target = memberAt(packed, drop);
            if (memberPosSet.contains(target.above(drop - 1).asLong())) continue;
            if (target.getY() < level.getMinY()) return false;
            BlockState state = level.getBlockState(target);
            if (!(state.isAir() || (state.canBeReplaced() && !state.liquid()))) return false;
        }
        return true;
    }

    private BlockPos memberAt(long packed, int drop) {
        int offset = (int) (packed & 0x3FFFFFL);
        return originCore.offset(offsetX(offset), offsetY(offset) - drop, offsetZ(offset));
    }

    @Override
    public void tick() {
        super.tick();

        // both sides, like vanilla FallingBlockEntity: server-only lagged
        fallVelocity += GRAVITY;
        fallProgress += fallVelocity;
        fallVelocity *= DRAG;

        if (level() instanceof ServerLevel server) {
            while (fallProgress >= 1.0D) {
                if (!canStep(server, committedDrop + 1)) {
                    land(server);
                    discard();
                    return;
                }
                committedDrop++;
                fallProgress -= 1.0D;
            }
        }

        setPos(originCore.getX() + 0.5, originCore.getY() - committedDrop - fallProgress,
                originCore.getZ() + 0.5);

        // re-sent on a slow cadence for late trackers
        if (tickCount % RESEND_INTERVAL == 0) sendMembers();
    }

    private void land(ServerLevel level) {
        BlockState[] states = new BlockState[members.length];
        for (int i = 0; i < members.length; i++) {
            states[i] = Block.BLOCK_STATE_REGISTRY.byId((int) (members[i] >>> 32));
        }

        MultiblockCore.withoutTeardown(() -> {
            for (int i = 0; i < members.length; i++) {
                level.setBlock(memberAt(members[i], committedDrop), states[i], Block.UPDATE_ALL);
            }
        });

        BlockPos newCore = originCore.below(committedDrop);
        BlockEntity be = level.getBlockEntity(newCore);
        if (carriedBe != null && be != null) {
            dev.golem.core.registry.BlockEntities.loadCustomOnly(be, carriedBe, level.registryAccess());
            be.setChanged();
        }

        MultiblockCore block = MultiblockSurface.foldedCore(level.getBlockState(newCore));
        if (block != null) block.reindexLoadedCells(level, newCore);
    }

    private void sendMembers() {
        if (level() instanceof ServerLevel) {
            com.hbm.packet.FallingMultiblockPayload.send(this, originCore.asLong(), members, carriedBe);
        }
    }

    /**
     * Client-side, from the payload: the member list and the anchor it renders against.
     */
    public void setMembersFromNetwork(long[] packedMembers, long corePos, @Nullable CompoundTag carried) {
        this.members = packedMembers;
        this.originCore = BlockPos.of(corePos);
        if (Objects.equals(carriedBe, carried)) return;
        carriedBe = carried;
        carriedView = null;
    }

    /**
     * Client-side: the carried core block entity, levelled and standing at {@code at}; never ticked, so it holds
     * the inert saved state. Rebuilt when {@code at} moves, so light and position reads follow the fall.
     */
    public @Nullable BlockEntity carriedBlockEntity(BlockPos at) {
        if (carriedBe == null) return null;
        if (carriedView == null || !carriedView.getBlockPos().equals(at)) {
            carriedView = CarriedBlockItems.load(coreState(), at, carriedBe, level().registryAccess());
            if (carriedView != null) carriedView.setLevel(level());
        }
        return carriedView;
    }

    /**
     * The packed member list, for the renderer. Offset {@code (0,0,0)} is the core.
     */
    public long[] membersPacked() {
        return members;
    }

    public BlockPos originCore() {
        return originCore;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    /**
     * Offsets and states persist SEPARATELY, the states by codec: the packed long carries a registry id rebuilt every launch - stable within a session, meaningless across one.
     */
    @Override
    protected void loadData(DataIn input) {
        originCore = input.read("core", BlockPos.CODEC).orElse(BlockPos.ZERO);
        carriedBe = input.read("be", CompoundTag.CODEC).orElse(null);
        carriedInventory = null;

        int[] offsets = input.read("offsets", Codec.INT_STREAM)
                             .map(IntStream::toArray).orElse(new int[0]);
        List<BlockState> states = input.read("states", BlockState.CODEC.listOf())
                                                 .orElse(List.of());
        int n = Math.min(offsets.length, states.size());
        members = new long[n];
        for (int i = 0; i < n; i++) {
            members[i] = (long) Block.BLOCK_STATE_REGISTRY.getId(states.get(i)) << 32 | (offsets[i] & 0x3FFFFFL);
        }

        memberPosSet.clear();
        for (long packed : members) memberPosSet.add(memberAt(packed, 0).asLong());
        committedDrop = input.getIntOr("drop", 0);
        fallProgress = input.getDoubleOr("progress", 0.0D);
        fallVelocity = input.getDoubleOr("velocity", 0.0D);
    }

    @Override
    protected void saveData(DataOut output) {
        output.store("core", BlockPos.CODEC, originCore);
        int[] offsets = new int[members.length];
        List<BlockState> states = new ArrayList<>(members.length);
        for (int i = 0; i < members.length; i++) {
            offsets[i] = (int) (members[i] & 0x3FFFFFL);
            states.add(Block.BLOCK_STATE_REGISTRY.byId((int) (members[i] >>> 32)));
        }
        output.store("offsets", Codec.INT_STREAM,
                IntStream.of(offsets));
        output.store("states", BlockState.CODEC.listOf(), states);
        if (carriedBe != null) output.store("be", CompoundTag.CODEC, carriedBe);
        output.putInt("drop", committedDrop);
        output.putDouble("progress", fallProgress);
        output.putDouble("velocity", fallVelocity);
    }
}
