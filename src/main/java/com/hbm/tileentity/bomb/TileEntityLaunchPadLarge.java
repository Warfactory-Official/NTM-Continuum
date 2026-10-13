package com.hbm.tileentity.bomb;

import com.hbm.inventory.container.ContainerLaunchPad;
import com.hbm.items.weapon.ItemMissileStandard;
import com.hbm.items.weapon.ItemMissileStandard.MissileFormFactor;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.MachineAudio;
import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code TileEntityLaunchPadLarge}: the 9x9 erector pad for every standard missile. The missile is lifted
 * and swung upright by an erector whose parts are chosen by the loaded missile's form factor; the pad is only
 * ready once the erector has put it upright ({@code erected}), and a launch knocks that back down.
 *
 * <p>Timing is upstream's: the erector turns 1.5 degrees a tick and the lift moves 0.025 a tick, both halved for
 * the huge and Atlas airframes, with a 20-tick pause between stages. Everything moves only while the pad holds
 * 75 kHE. Two oddities are kept on purpose: the "lift fully extended" branch tests {@code erector == 1F}, which
 * never holds at that point, and {@code scheduleErect} lands inside the delay window rather than immediately so
 * the erector animation catches up first.
 *
 * <p>Uses the shared launch pad menu, as upstream's base class did. Deferred, as for the small pad: the tower
 * steam and launch smoke particles.
 */
public class TileEntityLaunchPadLarge extends TileEntityLaunchPadBase implements MenuProvider {

    public int formFactor = -1;
    public boolean erected = false;
    public boolean readyToLoad = false;
    public boolean scheduleErect = false;
    public float lift = 1F;
    public float erector = 90F;
    public float prevLift = 1F;
    public float prevErector = 90F;
    public float syncLift = 1F;
    public float syncErector = 90F;
    private int sync;
    public int delay = 20;

    protected boolean liftMoving = false;
    protected boolean erectorMoving = false;

    private @Nullable AudioWrapper audioLift;
    private @Nullable AudioWrapper audioErector;

    public TileEntityLaunchPadLarge(BlockPos pos, BlockState state) {
        super(ModCustomMissileBlockEntities.LAUNCH_PAD_LARGE.get(), pos, state, 7);
    }

    @Override public boolean isReadyForLaunch() { return this.erected && this.readyToLoad; }
    @Override public double getLaunchOffset() { return 2D; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchPadLarge te) {
        te.prevLift = te.lift;
        te.prevErector = te.erector;

        float erectorSpeed = 1.5F;
        float liftSpeed = 0.025F;

        if (te.isMissileValid()) {
            if (te.inventory.getStackInSlot(0).getItem() instanceof ItemMissileStandard missile) {
                te.formFactor = missile.formFactor.ordinal();
                if (missile.formFactor == MissileFormFactor.ATLAS || missile.formFactor == MissileFormFactor.HUGE) {
                    erectorSpeed /= 2F;
                    liftSpeed /= 2F;
                }
            }
            if (te.erector == 90F && te.lift == 1F) te.readyToLoad = true;
        } else {
            te.readyToLoad = false;
            te.erected = false;
            te.delay = 20;
        }

        if (te.power >= 75_000) {
            if (te.delay > 0) {
                te.delay--;

                if (te.delay < 10 && te.scheduleErect) {
                    te.erected = true;
                    te.scheduleErect = false;
                }

                if (te.inventory.getStackInSlot(0).isEmpty() || !te.readyToLoad) {
                    if (te.erector < 90F) {
                        te.erector = Math.min(te.erector + erectorSpeed, 90F);
                        if (te.erector == 90F) te.delay = 20;
                    } else if (te.lift < 1F) {
                        te.lift = Math.min(te.lift + liftSpeed, 1F);
                        if (te.erector == 1F) { // upstream tests the erector here, so this never fires
                            te.readyToLoad = true;
                            te.delay = 20;
                        }
                    }
                }
            } else {
                if (!te.erected && te.readyToLoad) {
                    te.state = STATE_LOADING;
                    if (te.erector != 0F) {
                        te.erector = Math.max(te.erector - erectorSpeed, 0F);
                        if (te.erector == 0F) te.delay = 20;
                    } else if (te.lift > 0) {
                        te.lift = Math.max(te.lift - liftSpeed, 0F);
                        if (te.lift == 0F) {
                            te.scheduleErect = true;
                            te.delay = 20;
                        }
                    }
                } else {
                    if (te.erector < 90F) {
                        te.erector = Math.min(te.erector + erectorSpeed, 90F);
                        if (te.erector == 90F) te.delay = 20;
                    } else if (te.lift < 1F) {
                        te.lift = Math.min(te.lift + liftSpeed, 1F);
                        if (te.erector == 1F) {
                            te.readyToLoad = true;
                            te.delay = 20;
                        }
                    }
                }
            }
        }

        if (!te.hasFuel() || !te.isMissileValid()) te.state = STATE_MISSING;
        if (te.erected && te.canLaunch()) te.state = STATE_READY;

        boolean prevLiftMoving = te.liftMoving;
        boolean prevErectorMoving = te.erectorMoving;
        te.liftMoving = te.prevLift != te.lift;
        te.erectorMoving = te.prevErector != te.erector;

        if (prevLiftMoving && !te.liftMoving) {
            level.playSound(null, pos, ModDoorSounds.WGH_STOP.get(), SoundSource.BLOCKS, 2F, 1F);
        }
        if (prevErectorMoving && !te.erectorMoving) {
            level.playSound(null, pos, ModDoorSounds.GARAGE_STOP.get(), SoundSource.BLOCKS, 2F, 1F);
        }

        te.serverTickBase(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchPadLarge te) {
        te.prevLift = te.lift;
        te.prevErector = te.erector;

        if (te.sync > 0) {
            te.lift = te.lift + ((te.syncLift - te.lift) / (float) te.sync);
            te.erector = te.erector + ((te.syncErector - te.erector) / (float) te.sync);
            --te.sync;
        } else {
            te.lift = te.syncLift;
            te.erector = te.syncErector;
        }

        te.audioLift = MachineAudio.tick(te.audioLift, te.liftMoving, ModDoorSounds.WGH_START.get(), SoundSource.BLOCKS,
                pos.getX(), pos.getY(), pos.getZ(), 0.75F, 25F, 1.0F, 5);
        te.audioErector = MachineAudio.tick(te.audioErector, te.erectorMoving, ModDoorSounds.GARAGE_MOVE.get(),
                SoundSource.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 1.5F, 25F, 1.0F, 5);
    }

    @Override
    public void setRemoved() {
        if (audioLift != null) { audioLift.stopSound(); audioLift = null; }
        if (audioErector != null) { audioErector.stopSound(); audioErector = null; }
        super.setRemoved();
    }

    @Override
    public void finalizeLaunch(Entity missile) {
        super.finalizeLaunch(missile);
        this.erected = false;
    }

    /** The tier-4 contrail's facing byte: 1.7 read {@code getBlockMetadata() - 10}. */
    @Override
    protected byte getPadRotation() {
        return (byte) MachineCore.facingOrdinal(getBlockState(), 5);
    }

    @Override
    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 5, y, z - 2, Library.POS_X),
                new DirPos(x + 5, y, z + 2, Library.POS_X),
                new DirPos(x - 5, y, z - 2, Library.NEG_X),
                new DirPos(x - 5, y, z + 2, Library.NEG_X),
                new DirPos(x - 2, y, z + 5, Library.POS_Z),
                new DirPos(x + 2, y, z + 5, Library.POS_Z),
                new DirPos(x - 2, y, z - 5, Library.NEG_Z),
                new DirPos(x + 2, y, z - 5, Library.NEG_Z)
        };
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.liftMoving);
        buf.writeBoolean(this.erectorMoving);
        buf.writeBoolean(this.erected);
        buf.writeBoolean(this.readyToLoad);
        buf.writeByte((byte) this.formFactor);
        buf.writeFloat(this.lift);
        buf.writeFloat(this.erector);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.liftMoving = buf.readBoolean();
        this.erectorMoving = buf.readBoolean();
        this.erected = buf.readBoolean();
        this.readyToLoad = buf.readBoolean();
        this.formFactor = buf.readByte();
        this.syncLift = buf.readFloat();
        this.syncErector = buf.readFloat();
        if (this.lift != this.syncLift || this.erector != this.syncErector) this.sync = 3;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("erected", erected);
        output.putBoolean("readyToLoad", readyToLoad);
        output.putFloat("lift", lift);
        output.putFloat("erector", erector);
        if (formFactor >= 0) SavedIds.putEnum(output, "formFactor", MissileFormFactor.values()[formFactor]); // by name
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        erected = input.getBooleanOr("erected", false);
        readyToLoad = input.getBooleanOr("readyToLoad", false);
        lift = input.getFloatOr("lift", 1F);
        erector = input.getFloatOr("erector", 90F);
        MissileFormFactor savedForm = SavedIds.getEnumOrNull(input, "formFactor", MissileFormFactor.class);
        formFactor = savedForm == null ? -1 : savedForm.ordinal();
        syncLift = prevLift = lift;
        syncErector = prevErector = erector;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.launchPad");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerLaunchPad(containerId, playerInventory, this);
    }
}
