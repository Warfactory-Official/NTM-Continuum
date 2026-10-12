package com.hbm.tileentity;

import dev.golem.core.data.Tags;

import com.hbm.lib.ForgeDirection;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.render.anim.BusAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.util.DoorRotation;

/**
 * Port of 1.7.10 {@code TileEntityDoorGeneric}: the open/close state machine of every
 * {@link BlockDoorGeneric}. The server steps {@code openTicks}, flags the cells the moving leaf has
 * cleared as open ({@code makeExtra}) or blocked again ({@code removeExtra}), and syncs the state byte;
 * the client turns state transitions into sounds and starts the keyframe animation the renderer plays.
 */
public class TileEntityDoorGeneric extends TileEntityLockableBase {

    public static final byte STATE_CLOSED = 0;
    public static final byte STATE_OPEN = 1;
    public static final byte STATE_CLOSING = 2;
    public static final byte STATE_OPENING = 3;

    // 0: closed, 1: open, 2: closing, 3: opening
    public byte state = STATE_CLOSED;
    protected @Nullable DoorDecl doorType;
    public int openTicks = 0;
    public long animStartTime = 0;
    public int redstonePower;
    public boolean shouldUseBB = false;
    private byte skinIndex = 0;

    public final Set<BlockPos> activatedBlocks = new HashSet<>(4);

    private @Nullable AudioWrapper audio;
    private @Nullable AudioWrapper audio2;

    /** The bus animation the current transition plays, started at {@link #animStartTime}; client only. */
    public @Nullable BusAnimation currentAnimation;

    /** A listener told that a door finished opening or closing; {@code core} is the door's core position. */
    @FunctionalInterface
    public interface Moved {
        void moved(net.minecraft.server.level.ServerLevel level, BlockPos core, TileEntityDoorGeneric door);
    }

    /**
     * Notified on the server when a door has finished opening ({@link #STATE_OPEN}) or closing ({@link #STATE_CLOSED}),
     * after its cells' {@code open} state is final. hbm-space told its sealed-room atmosphere here
     * ({@code IBlockSealable.updateSealedState}); plain HBM has no listener.
     */
    public static final dev.golem.core.hook.Hook<Moved> MOVED = dev.golem.core.hook.Hook.notifying();

    private void moved() {
        if (MOVED.isEmpty() || !(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        for (Moved listener : MOVED) listener.moved(server, worldPosition, this);
    }

    public TileEntityDoorGeneric(BlockPos pos, BlockState state) {
        super(ModDoorBlockEntities.DOOR_GENERIC.get(), pos, state);
    }

    public void serverTick() {
        if (level == null) return;
        DoorDecl decl = getDoorType();
        if (decl == null) {
            return;
        }

        if (decl.onDoorUpdate() != null) {
            decl.onDoorUpdate().accept(this);
        }

        if (state == STATE_OPENING) {
            openTicks++;
            if (openTicks >= decl.timeToOpen()) openTicks = decl.timeToOpen();

        } else if (state == STATE_CLOSING) {
            openTicks--;
            if (openTicks <= 0) openTicks = 0;
        }

        BlockPos pos = worldPosition;
        int[][] ranges = decl.getDoorOpenRanges();
        ForgeDirection dir = MachineCore.facing(getBlockState());
        BlockDoorGeneric block = getBlockState().getBlock() instanceof BlockDoorGeneric door ? door : null;

        if (state == STATE_OPENING) {

            for (int i = 0; i < ranges.length; i++) {

                int[] range = ranges[i];
                float time = decl.getDoorRangeOpenTime(openTicks, i);

                for (int j = 0; j < Math.abs(range[3]); j++) {

                    if ((float) j / (Math.abs(range[3] - 1)) > time)
                        break;

                    for (int k = 0; k < range[4]; k++) {
                        BlockPos finalPos = rangePos(range, j, k, dir, pos);

                        if (finalPos.equals(pos)) {
                            this.shouldUseBB = false;
                        } else if (block != null) {
                            block.makeExtra(level, finalPos);
                        }
                    }
                }
            }

        } else if (state == STATE_CLOSING) {

            for (int i = 0; i < ranges.length; i++) {

                int[] range = ranges[i];
                float time = decl.getDoorRangeOpenTime(openTicks, i);

                for (int j = Math.abs(range[3]) - 1; j >= 0; j--) {

                    if ((float) j / (Math.abs(range[3] - 1)) < time)
                        break;

                    for (int k = 0; k < range[4]; k++) {
                        BlockPos finalPos = rangePos(range, j, k, dir, pos);

                        if (finalPos.equals(pos)) {
                            this.shouldUseBB = false;
                        } else if (block != null) {
                            block.removeExtra(level, finalPos);
                        }
                    }
                }
            }
        }
        if (state == STATE_OPENING && openTicks == decl.timeToOpen()) {
            state = STATE_OPEN;
            moved();
        }
        if (state == STATE_CLOSING && openTicks == 0) {
            state = STATE_CLOSED;
            moved();
        }

        this.networkPackNT();

        if (redstonePower == -1 && state == STATE_OPEN) {
            tryToggle(-1);
        } else if (redstonePower > 0 && state == STATE_CLOSED) {
            tryToggle(-1);
        }
        if (redstonePower == -1) {
            redstonePower = 0;
        }
    }

    /**
     * One cell of an open range: the range's start, plus the {@code j}-th step along its axis and the
     * {@code k}-th along its width, turned into the door's frame. The original adds a half turn for
     * the X-facing doors on top of the block rotation, and that is kept as written.
     */
    private static BlockPos rangePos(int[] range, int j, int k, ForgeDirection dir, BlockPos core) {
        int ax = 0, ay = 0, az = 0;
        switch (range[5]) {
            case 0 -> { ay = k; az = (int) Math.signum(range[3]) * j; }
            case 1 -> { ax = k; ay = (int) Math.signum(range[3]) * j; }
            case 2 -> { ax = (int) Math.signum(range[3]) * j; ay = k; }
            default -> { }
        }

        DoorRotation r = DoorRotation.getBlockRotation(dir);
        if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST)
            r = r.add(DoorRotation.CLOCKWISE_180);

        BlockPos rotated = r.rotate(range[0] + ax, range[1] + ay, range[2] + az);
        return rotated.offset(core);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeByte(state);
        buf.writeByte(skinIndex);
        buf.writeBoolean(shouldUseBB);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        handleNewState(buf.readByte());
        skinIndex = buf.readByte();
        shouldUseBB = buf.readBoolean();
    }

    private void stopAudio() {
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
        if (audio2 != null) {
            audio2.stopSound();
            audio2 = null;
        }
    }

    public @Nullable DoorDecl getDoorType() {
        if (this.doorType == null && this.getBlockState().getBlock() instanceof BlockDoorGeneric door)
            this.doorType = door.type;
        return this.doorType;
    }

    public boolean tryToggle(@Nullable Player player) {

        if (this.isLocked() && player == null) return false;

        if (state == STATE_CLOSED && redstonePower > 0) {
            //Redstone "power locks" doors, just like minecraft iron doors
            return false;
        }

        boolean server = level != null && !level.isClientSide();
        if (this.state == STATE_CLOSED) {
            if (server && canAccess(player)) this.state = STATE_OPENING;
            return true;

        } else if (this.state == STATE_OPEN) {
            if (server && canAccess(player)) this.state = STATE_CLOSING;
            return true;
        }
        return false;
    }

    public boolean tryToggle(int passcode) {
        if (this.isLocked() && passcode != this.lock) return false;

        boolean server = level != null && !level.isClientSide();
        if (this.state == STATE_CLOSED) {
            if (server) this.state = STATE_OPENING;
            return true;

        } else if (this.state == STATE_OPEN) {
            if (server) this.state = STATE_CLOSING;
            return true;
        }
        return false;
    }

    /** Client side: a state byte arrived; play the transition's sounds and start its animation. */
    public void handleNewState(byte state) {

        if (this.state != state) {
            DoorDecl doorType = getDoorType();
            if (doorType == null) {
                this.state = state;
                return;
            }

            if (this.state == STATE_CLOSED && state == STATE_OPENING) { // Door transitioning to open
                if (audio != null) {
                    audio.stopSound();
                    audio.setKeepAlive(0);
                }

                if (doorType.getOpenSoundLoop() != null) {
                    audio = loop(doorType.getOpenSoundLoop());
                    if (audio != null) audio.startSound();
                }

                if (doorType.getOpenSoundStart() != null) {
                    playLocal(doorType.getOpenSoundStart());
                }

                if (doorType.getSoundLoop2() != null) {
                    if (audio2 != null) audio2.stopSound();

                    audio2 = loop(doorType.getSoundLoop2());
                    if (audio2 != null) audio2.startSound();
                }
            }

            if (this.state == STATE_OPEN && state == STATE_CLOSING) { // Door transitioning to closed
                if (audio != null) {
                    audio.stopSound();
                }

                if (doorType.getCloseSoundLoop() != null) {
                    audio = loop(doorType.getCloseSoundLoop());
                    if (audio != null) audio.startSound();
                }

                if (doorType.getCloseSoundStart() != null) {
                    playLocal(doorType.getCloseSoundStart());
                }

                if (doorType.getSoundLoop2() != null) {
                    if (audio2 != null) audio2.stopSound();

                    audio2 = loop(doorType.getSoundLoop2());
                    if (audio2 != null) audio2.startSound();
                }
            }

            if (state == STATE_OPEN || state == STATE_CLOSED) { // Door finished any transition
                stopAudio();
            }

            if (this.state == STATE_OPENING && state == STATE_OPEN) { // Door finished transitioning to open
                if (doorType.getOpenSoundEnd() != null) {
                    playLocal(doorType.getOpenSoundEnd());
                }
            }

            if (this.state == STATE_CLOSING && state == STATE_CLOSED) { // Door finished transitioning to closed
                if (doorType.getCloseSoundEnd() != null) {
                    playLocal(doorType.getCloseSoundEnd());
                }
            }

            this.state = state;

            if (state == STATE_OPENING || state == STATE_CLOSING) {
                animStartTime = System.currentTimeMillis();
                currentAnimation = doorType.getBusAnimation(state, this.skinIndex);
            }
        }
    }

    private @Nullable AudioWrapper loop(RegistryObject<SoundEvent> sound) {
        return AudioSystem.getLoopedSound(sound.get(), getDoorType().getSoundSource(),
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getVolume(), 10F, 1F);
    }

    private void playLocal(RegistryObject<SoundEvent> sound) {
        if (level == null) return;
        level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), sound.get(),
                getDoorType().getSoundSource(), getVolume(), 1F, false);
    }

    /** A server-side one-shot at the door, heard by everyone nearby (old {@code playSoundEffect}). */
    public void playServerSound(SoundEvent sound, float volume, float pitch) {
        if (level == null) return;
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), sound,
                getDoorType() == null ? net.minecraft.sounds.SoundSource.BLOCKS : getDoorType().getSoundSource(), volume, pitch);
    }

    public float getVolume() {
        DoorDecl decl = getDoorType();
        return getVolume(decl == null ? 1F : decl.getSoundVolume());
    }

    public int getSkinIndex() {
        return skinIndex;
    }

    public boolean cycleSkinIndex() {
        DoorDecl decl = getDoorType();
        if (decl == null || !decl.hasSkins()) return false;
        this.skinIndex++;
        this.skinIndex %= decl.getSkinCount();
        this.setChanged();
        return true;
    }

    /** Useful for logic block interactions, as a way to close/open doors. */
    public void open() {
        if (state == STATE_CLOSED) state = STATE_OPENING;
    }

    public void close() {
        if (state == STATE_OPEN) state = STATE_CLOSING;
    }

    @Override
    protected void loadData(DataIn tag) {
        this.state = tag.getByteOr("state", (byte) 0);
        this.openTicks = tag.getIntOr("openTicks", 0);
        this.animStartTime = tag.getLongOr("animStartTime", 0L);
        this.redstonePower = tag.getIntOr("redstoned", 0);
        this.shouldUseBB = tag.getBooleanOr("shouldUseBB", false);
        this.skinIndex = tag.getByteOr("skin", (byte) 0);
        int[] activated = tag.getIntArrayOr("activatedBlocks", new int[0]);
        this.activatedBlocks.clear();
        for (int i = 0; i + 2 < activated.length; i += 3) {
            this.activatedBlocks.add(new BlockPos(activated[i], activated[i + 1], activated[i + 2]));
        }
        super.loadData(tag);
    }

    @Override
    protected void saveData(DataOut tag) {
        super.saveData(tag);

        tag.putByte("state", state);
        tag.putInt("openTicks", openTicks);
        tag.putLong("animStartTime", animStartTime);
        tag.putInt("redstoned", redstonePower);
        tag.putBoolean("shouldUseBB", shouldUseBB);
        DoorDecl decl = getDoorType();
        if (decl != null && decl.hasSkins())
            tag.putByte("skin", skinIndex);
        int[] activated = new int[this.activatedBlocks.size() * 3];
        int i = 0;
        for (BlockPos p : this.activatedBlocks) {
            activated[i++] = p.getX();
            activated[i++] = p.getY();
            activated[i++] = p.getZ();
        }
        tag.putIntArray("activatedBlocks", activated);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        stopAudio();
    }

    public void updateRedstonePower(BlockPos pos) {
        //Drillgon200: Best I could come up with without having to use dummy tile entities
        if (level == null) return;
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos);
        if (!contained && powered) {
            activatedBlocks.add(pos.immutable());
            if (redstonePower == -1) {
                redstonePower = 0;
            }
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos);
            redstonePower--;
            if (redstonePower == 0) {
                redstonePower = -1;
            }
        }
    }
}
