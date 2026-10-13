package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.network.RTTYSystem;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityRBMKLever}: two levers that take half a second to throw. A non-polling lever sends
 * its ON command once on reaching the bottom and its OFF command once on returning; a polling lever sends
 * the matching command every tick it rests at either end.
 *
 * <p>Making or breaking the contact plays the spark sound and two small "tau" bursts (five line sparks and a
 * half-size hadron flash each) at the lever's two contacts, sent to players within 25 blocks as in 1.7.
 */
public class BlockEntityRBMKLever extends TileEntityLoadedBase implements IControlReceiver, IRBMKTicking {

    public static final int LEVERS = 2;
    public final LeverUnit[] levers = new LeverUnit[LEVERS];

    public BlockEntityRBMKLever(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_LEVER.get(), pos, state);
        for (int i = 0; i < LEVERS; i++) levers[i] = new LeverUnit(i);
    }

    @Override public void tickServer() { for (LeverUnit lever : levers) lever.update(); networkPackNT(); }
    @Override public void tickClient() { for (LeverUnit lever : levers) lever.updateClient(); }

    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); for (LeverUnit lever : levers) lever.serialize(buf); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); for (LeverUnit lever : levers) lever.deserialize(buf); }
    @Override protected void saveData(DataOut output) { super.saveData(output); for (int i = 0; i < LEVERS; i++) levers[i].save(output, i); }
    @Override protected void loadData(DataIn input) { super.loadData(input); for (int i = 0; i < LEVERS; i++) levers[i].load(input, i); }

    @Override public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) < 15 * 15;
    }

    @Override public void receiveControl(CompoundTag data) {
        int active = Tags.getByteOr(data, "active", (byte) 0), polling = Tags.getByteOr(data, "polling", (byte) 0);
        for (int i = 0; i < LEVERS; i++) {
            levers[i].active = (active & 1 << i) != 0;
            levers[i].polling = (polling & 1 << i) != 0;
        }
        for (int i = 0; i < LEVERS; i++) {
            LeverUnit lever = levers[i];
            lever.label = Tags.getStringOr(data, "label" + i, "");
            lever.rtty = Tags.getStringOr(data, "rtty" + i, "");
            lever.commandOn = Tags.getStringOr(data, "cmdOn" + i, "");
            lever.commandOff = Tags.getStringOr(data, "cmdOff" + i, "");
        }
        setChanged();
    }

    private void sound(SoundEvent sound, float volume) {
        level.playSound(null, worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D,
                sound, SoundSource.BLOCKS, volume, 1F);
    }

    public final class LeverUnit {
        public static final float FLIP_SPEED = 1F / 10F; // 0.5s
        public final int index;
        public boolean polling, active;
        public String label = "", rtty = "", commandOn = "", commandOff = "";
        /** If true, the lever tries to move down to the ON state, otherwise it tries to return to OFF. */
        public boolean isTurningOn = false;
        public float flipProgress, prevFlipProgress, flipSync;
        public int turnProgress;

        LeverUnit(int index) {
            this.index = index;
            label = "Lever " + (index + 1);
        }

        public void click() {
            if (!active) return;
            if (flipProgress <= 0 || flipProgress >= 1) sound(ModSounds.LEVER_START.get(), 1F);
            isTurningOn = !isTurningOn;
            setChanged();
        }

        void update() {
            prevFlipProgress = flipProgress;
            if (!active) return;
            boolean arcFlash = false;
            if (polling) {
                if (flipProgress >= 1F && canSend(commandOn)) RTTYSystem.broadcast(level, rtty, commandOn);
                if (flipProgress <= 0F && canSend(commandOff)) RTTYSystem.broadcast(level, rtty, commandOff);
            }
            if (isTurningOn && flipProgress < 1F) {
                flipProgress += FLIP_SPEED;
                if (flipProgress >= 1F) {
                    flipProgress = 1F;
                    if (!polling && canSend(commandOn)) RTTYSystem.broadcast(level, rtty, commandOn);
                    sound(ModSounds.LEVER_STOP.get(), .5F);
                    arcFlash = true;
                }
            } else if (!isTurningOn && flipProgress > 0F) {
                if (prevFlipProgress >= 1) arcFlash = true;
                flipProgress -= FLIP_SPEED;
                if (flipProgress <= 0F) {
                    flipProgress = 0F;
                    if (!polling && canSend(commandOff)) RTTYSystem.broadcast(level, rtty, commandOff);
                    sound(ModSounds.LEVER_STOP.get(), .5F);
                }
            }
            if (arcFlash) {
                sound(ModSounds.SPARK.get(), 1F);
                // 1.7: dir is the panel's facing, rot = dir.getRotation(UP), i.e. its clockwise neighbour
                Direction dir = getBlockState().getValue(RBMKMiniPanelBase.FACING);
                Direction rot = dir.getClockWise();
                for (int i = 0; i < 2; i++) {
                    double spread = (index - 0.5) * (i == 0 ? 0.375 : 0.625);
                    double x = worldPosition.getX() + 0.5 + dir.getStepX() * 0.4 - rot.getStepX() * spread;
                    double y = worldPosition.getY() + 0.4375 - 0.03125;
                    double z = worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.4 - rot.getStepZ() * spread;
                    CompoundTag data = new CompoundTag();
                    data.putByte("count", (byte) 5);
                    data.putBoolean("small", true);
                    HbmEffectNT.Tau.send(level, x, y, z, data, 25);
                }
            }
        }

        void updateClient() {
            prevFlipProgress = flipProgress;
            // approach-based interp
            if (turnProgress > 0) {
                flipProgress = flipProgress + (flipSync - flipProgress) / (float) turnProgress;
                --turnProgress;
            } else {
                flipProgress = flipSync;
            }
        }

        public boolean canSend(String command) { return rtty != null && !rtty.isEmpty() && command != null && !command.isEmpty(); }

        void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(active); buf.writeBoolean(polling); buf.writeFloat(flipProgress);
            buf.writeUtf(label); buf.writeUtf(rtty); buf.writeUtf(commandOn); buf.writeUtf(commandOff);
        }
        void deserialize(RegistryFriendlyByteBuf buf) {
            active = buf.readBoolean(); polling = buf.readBoolean(); flipSync = buf.readFloat();
            label = buf.readUtf(); rtty = buf.readUtf(); commandOn = buf.readUtf(); commandOff = buf.readUtf();
            turnProgress = 3; // three-ply for extra smoothness
        }
        void save(DataOut out, int i) {
            out.putBoolean("active" + i, active); out.putBoolean("polling" + i, polling); out.putBoolean("isTurningOn" + i, isTurningOn);
            out.putFloat("flipProgress" + i, flipProgress); out.putString("label" + i, label); out.putString("rtty" + i, rtty);
            out.putString("commandOn" + i, commandOn); out.putString("commandOff" + i, commandOff);
        }
        void load(DataIn in, int i) {
            active = in.getBooleanOr("active" + i, false); polling = in.getBooleanOr("polling" + i, false); isTurningOn = in.getBooleanOr("isTurningOn" + i, false);
            flipProgress = in.getFloatOr("flipProgress" + i, 0F); label = in.getStringOr("label" + i, ""); rtty = in.getStringOr("rtty" + i, "");
            commandOn = in.getStringOr("commandOn" + i, ""); commandOff = in.getStringOr("commandOff" + i, "");
        }
    }
}
