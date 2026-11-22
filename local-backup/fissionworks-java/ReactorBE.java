package fissionworks;

import fissionworks.sim.Reactor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/** Lives on the panel. Owns the reactor, steps it on the server, and keeps the blocks and clients in sync. */
public class ReactorBE extends BlockEntity implements Console {
    public static final int OP_ROD = 0, OP_GROUP = 1, OP_CYCLE = 2, OP_SCRAM = 3;

    private Reactor reactor;
    private Direction facing = Direction.NORTH;
    private int ticks;
    private int lastHash;

    public ReactorBE(BlockPos pos, BlockState state) {
        super(FissionWorks.REACTOR_BE.get(), pos, state);
    }

    /** Null until the hall is formed. */
    public Reactor reactor() {
        return reactor;
    }

    public void start(Reactor r, Direction f) {
        reactor = r;
        facing = f;
        sync();
    }

    public void stop() {
        reactor = null;
        sync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReactorBE be) {
        if (level.isClientSide || be.reactor == null || ++be.ticks % 2 != 0) {
            return;
        }
        be.reactor.step();
        be.applyLooks(level);
        if (be.ticks % 4 == 0 && Arrays.hashCode(be.reactor.toInts()) != be.lastHash) {
            be.sync();
        }
    }

    /** A console command: op, and two numbers whose meaning depends on the op. */
    @Override
    public void command(Player player, int op, int a, int b) {
        if (reactor == null) {
            return;
        }
        int x = Math.floorMod(a, reactor.w), z = Math.floorDiv(a, reactor.w);
        switch (op) {
            case OP_ROD -> reactor.setTarget(x, Math.min(z, reactor.h - 1), b);
            case OP_GROUP -> reactor.setGroupTarget(Math.floorMod(a, Reactor.GROUPS), b);
            case OP_CYCLE -> reactor.cycleGroup(x, Math.min(z, reactor.h - 1));
            case OP_SCRAM -> reactor.scram();
            default -> { }
        }
        sync();
    }

    /** Wrench on a rod: next colour group. Returns the new group, or -1 if pos is not a rod cell. */
    public int cycleGroupAt(BlockPos pos) {
        int[] cell = reactor == null ? null : Hall.locate(worldPosition, facing, pos);
        if (cell == null || cell[0] < 1 || cell[1] < 1 || cell[0] > Hall.INNER || cell[1] > Hall.INNER
                || reactor.kind(cell[0] - 1, cell[1] - 1) != Reactor.Kind.ROD) {
            return -1;
        }
        reactor.cycleGroup(cell[0] - 1, cell[1] - 1);
        applyLooks(level);
        sync();
        return reactor.group(cell[0] - 1, cell[1] - 1);
    }

    private void applyLooks(Level world) {
        for (int z = 0; z < reactor.h; z++) {
            for (int x = 0; x < reactor.w; x++) {
                BlockPos p = Hall.at(worldPosition, facing, x + 1, z + 1);
                BlockState now = world.getBlockState(p);
                BlockState next = now;
                if (now.getBlock() instanceof RodBlock) {
                    next = now.setValue(RodBlock.LEVEL, Math.round(reactor.level(x, z) * 8 / 100f))
                            .setValue(RodBlock.GROUP, reactor.group(x, z));
                } else if (now.getBlock() instanceof FuelBlock) {
                    next = now.setValue(FuelBlock.GLOW, Math.round((float) (reactor.flux(x, z) / Reactor.CAP * 4)));
                }
                if (next != now) {
                    world.setBlock(p, next, 2);
                }
            }
        }
    }

    private void sync() {
        setChanged();
        lastHash = reactor == null ? 0 : Arrays.hashCode(reactor.toInts());
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (reactor != null) {
            tag.putIntArray("reactor", reactor.toInts());
            tag.putString("facing", facing.getName());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reactor = Reactor.fromInts(tag.getIntArray("reactor"));
        Direction f = Direction.byName(tag.getString("facing"));
        facing = f == null || f.getAxis().isVertical() ? Direction.NORTH : f;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
