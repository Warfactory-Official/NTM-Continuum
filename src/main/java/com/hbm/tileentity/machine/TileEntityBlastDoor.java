package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.blocks.machine.DummyBlockBlast;
import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.ModDoorBlockEntities;

/**
 * Port of 1.7.10 {@code TileEntityBlastDoor}: the old six-high blast door. The leaf is six dummy
 * blocks above the core that the door removes one at a time as it opens (0/20/40/60/80 ticks) and puts
 * back as it closes; adjacent blast doors open, close and lock together.
 *
 * <p>The original pushed a {@code TEVaultPacket} every tick carrying the state and a "reset the
 * animation clock" flag; here the state rides the ordinary buf sync and the flag is a toggle counter
 * the client compares against its last, which restarts {@link #sysTime} on the same edge.
 */
public class TileEntityBlastDoor extends TileEntityLockableBase {

    public boolean isOpening = false;
    //0: closed, 1: opening/closing, 2:open
    public int state = 0;
    public long sysTime;
    private int timer = 0;
    public boolean redstoned = false;
    /** Bumped on every open/close start; the client restarts its clock when it changes. */
    private int toggleSerial = 0;
    private int lastToggleSerial = 0;

    public TileEntityBlastDoor(BlockPos pos, BlockState state) {
        super(ModDoorBlockEntities.BLAST_DOOR.get(), pos, state);
    }

    public void serverTick() {
        if (level == null) return;
        BlockPos p = worldPosition;

        if (!isLocked() && level.hasNeighborSignal(p) || level.hasNeighborSignal(p.above(6))) {

            if (!redstoned) {
                this.tryToggle();
            }
            redstoned = true;

        } else {
            redstoned = false;
        }

        if (state != 1) {
            timer = 0;
        } else {
            timer++;

            if (isOpening) {
                if (timer >= 0) {
                    removeDummy(p.above(1));
                }
                if (timer >= 20) {
                    removeDummy(p.above(2));
                }
                if (timer >= 40) {
                    removeDummy(p.above(3));
                }
                if (timer >= 60) {
                    removeDummy(p.above(4));
                }
                if (timer >= 80) {
                    removeDummy(p.above(5));
                }
            } else {
                if (timer >= 20) {
                    placeDummy(p.above(5));
                }
                if (timer >= 40) {
                    placeDummy(p.above(4));
                }
                if (timer >= 60) {
                    placeDummy(p.above(3));
                }
                if (timer >= 80) {
                    placeDummy(p.above(2));
                }
                if (timer >= 100) {
                    placeDummy(p.above(1));
                }
            }

            if (timer >= 100) {

                if (isOpening)
                    finishOpen();
                else
                    finishClose();
            }
        }

        this.networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(isOpening);
        buf.writeByte(state);
        buf.writeInt(toggleSerial);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        isOpening = buf.readBoolean();
        state = buf.readByte();
        int serial = buf.readInt();
        if (serial != lastToggleSerial) {
            lastToggleSerial = serial;
            sysTime = System.currentTimeMillis();
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level == null) return;
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), sound, SoundSource.BLOCKS, volume, pitch);
    }

    public void open() {
        if (state == 0) {
            toggleSerial++;
            isOpening = true;
            state = 1;
            playSound(com.hbm.sound.ModSounds.REACTOR_START.get(), 0.5F, 0.75F);
        }
    }

    public void finishOpen() {
        state = 2;
        playSound(ModDoorSounds.REACTOR_STOP.get(), 0.5F, 1.0F);
    }

    public void close() {
        if (state == 2) {
            toggleSerial++;
            isOpening = false;
            state = 1;
            playSound(com.hbm.sound.ModSounds.REACTOR_START.get(), 0.5F, 0.75F);
        }
    }

    public void finishClose() {
        state = 0;
        playSound(ModDoorSounds.REACTOR_STOP.get(), 0.5F, 1.0F);
    }

    private static final BlockPos[] SIDES = { new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1) };

    public void openNeigh() {
        if (level == null) return;
        for (BlockPos side : SIDES) {
            BlockEntity te = level.getBlockEntity(worldPosition.offset(side));
            if (te instanceof TileEntityBlastDoor door) {
                if (door.canOpen() && (!door.isLocked() || door.lock == lock)) {
                    door.open();
                    door.openNeigh();
                }
            }
        }
    }

    @Override
    public void lock() {
        super.lock();
        lockNeigh();
    }

    public void closeNeigh() {
        if (level == null) return;
        for (BlockPos side : SIDES) {
            BlockEntity te = level.getBlockEntity(worldPosition.offset(side));
            if (te instanceof TileEntityBlastDoor door) {
                if (door.canClose() && (!door.isLocked() || door.lock == lock)) {
                    door.close();
                    door.closeNeigh();
                }
            }
        }
    }

    public void lockNeigh() {
        if (level == null) return;
        for (BlockPos side : SIDES) {
            BlockEntity te = level.getBlockEntity(worldPosition.offset(side));
            if (te instanceof TileEntityBlastDoor door) {
                if (!door.isLocked()) {
                    door.setPins(this.lock);
                    door.lock();
                    door.setMod(lockMod);
                }
            }
        }
    }

    public boolean canOpen() {
        return state == 0;
    }

    public boolean canClose() {
        return state == 2;
    }

    public void tryToggle() {

        if (canOpen()) {
            open();
            openNeigh();
        } else if (canClose()) {
            close();
            closeNeigh();
        }
    }

    public boolean placeDummy(BlockPos pos) {
        if (level == null) return false;
        BlockState present = level.getBlockState(pos);
        if (!present.canBeReplaced() && present.getBlock() != ModDoorBlocks.DUMMY_BLOCK_BLAST.get()) level.destroyBlock(pos, false);

        level.setBlock(pos, ModDoorBlocks.DUMMY_BLOCK_BLAST.get().defaultBlockState(), 3);

        if (level.getBlockEntity(pos) instanceof TileEntityBlastDoorDummy dummy) {
            dummy.target = worldPosition.immutable();
            dummy.setChanged();
        }

        return true;
    }

    public void removeDummy(BlockPos pos) {
        if (level == null) return;
        if (level.getBlockState(pos).getBlock() == ModDoorBlocks.DUMMY_BLOCK_BLAST.get()) {
            DummyBlockBlast.safeBreak = true;
            try {
                level.removeBlock(pos, false);
            } finally {
                DummyBlockBlast.safeBreak = false;
            }
        }
    }

    @Override
    protected void loadData(DataIn nbt) {
        super.loadData(nbt);
        isOpening = nbt.getBooleanOr("isOpening", false);
        state = nbt.getIntOr("state", 0);
        sysTime = nbt.getLongOr("sysTime", 0L);
        timer = nbt.getIntOr("timer", 0);
        redstoned = nbt.getBooleanOr("redstoned", false);
    }

    @Override
    protected void saveData(DataOut nbt) {
        super.saveData(nbt);
        nbt.putBoolean("isOpening", isOpening);
        nbt.putInt("state", state);
        nbt.putLong("sysTime", sysTime);
        nbt.putInt("timer", timer);
        nbt.putBoolean("redstoned", redstoned);
    }
}
