package com.hbm.tileentity.machine.rbmk;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.network.RTTYSystem;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityRBMKKeyPad}: four Redstone-over-Radio buttons. A non-polling button broadcasts its
 * command once per press and springs back after seven ticks; a polling button latches and broadcasts
 * every tick while held down. The OpenComputers callbacks of the original are not ported (no OC here).
 */
public class BlockEntityRBMKKeyPad extends TileEntityLoadedBase implements IControlReceiver, IRBMKTicking {

    public static final int KEYS = 4;
    public final KeyUnit[] keys = new KeyUnit[KEYS];

    public BlockEntityRBMKKeyPad(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_KEY_PAD.get(), pos, state);
        for (int i = 0; i < KEYS; i++) keys[i] = new KeyUnit(i);
    }

    @Override public void tickServer() {
        for (KeyUnit key : keys) key.update();
        networkPackNT();
    }

    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); for (KeyUnit key : keys) key.serialize(buf); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); for (KeyUnit key : keys) key.deserialize(buf); }
    @Override protected void saveData(DataOut output) { super.saveData(output); for (int i = 0; i < KEYS; i++) keys[i].save(output, i); }
    @Override protected void loadData(DataIn input) { super.loadData(input); for (int i = 0; i < KEYS; i++) keys[i].load(input, i); }

    @Override public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) < 15 * 15;
    }

    @Override public void receiveControl(CompoundTag data) {
        int active = Tags.getByteOr(data, "active", (byte) 0), polling = Tags.getByteOr(data, "polling", (byte) 0);
        for (int i = 0; i < KEYS; i++) {
            keys[i].active = (active & 1 << i) != 0;
            keys[i].polling = (polling & 1 << i) != 0;
        }
        for (int i = 0; i < KEYS; i++) {
            KeyUnit key = keys[i];
            key.color = Mth.clamp(Tags.getIntOr(data, "color" + i, 0), 0, 0xffffff);
            key.label = Tags.getStringOr(data, "label" + i, "");
            key.rtty = Tags.getStringOr(data, "rtty" + i, "");
            key.command = Tags.getStringOr(data, "cmd" + i, "");
        }
        // The original never marked the tile dirty here; persisting the configuration is the intent.
        setChanged();
    }

    public final class KeyUnit {
        /** If the output should be per tick, allows the "is pressed" state. */
        public boolean polling;
        /** If the button is toggled, assuming polling is enabled. */
        public boolean isPressed;
        public int color;
        public String label = "", rtty = "", command = "";
        public boolean active;
        /** For non-polling buttons, the time until the button visually unpresses. */
        public int clickTimer;

        KeyUnit(int index) {
            if (index == 0) color = 0xff0000;
            if (index == 1) color = 0xffff00;
            if (index == 2) color = 0x0080ff;
            if (index == 3) color = 0x00ff00;
            label = "Button " + (index + 1);
        }

        public void click() {
            if (!active) return;
            if (!polling) {
                if (canSend()) RTTYSystem.broadcast(level, rtty, command);
                isPressed = true;
                clickTimer = 7;
            } else {
                isPressed = !isPressed;
                setChanged();
            }
            level.playSound(null, worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D,
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1F, isPressed ? 1F : .75F);
        }

        void update() {
            if (!active) return;
            if (polling && isPressed && canSend()) RTTYSystem.broadcast(level, rtty, command);
            if (!polling && isPressed && clickTimer-- <= 0) isPressed = false;
        }

        public boolean canSend() { return rtty != null && !rtty.isEmpty() && command != null && !command.isEmpty(); }

        void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(active); buf.writeBoolean(polling); buf.writeBoolean(isPressed); buf.writeInt(color);
            buf.writeUtf(label); buf.writeUtf(rtty); buf.writeUtf(command);
        }
        void deserialize(RegistryFriendlyByteBuf buf) {
            active = buf.readBoolean(); polling = buf.readBoolean(); isPressed = buf.readBoolean(); color = buf.readInt();
            label = buf.readUtf(); rtty = buf.readUtf(); command = buf.readUtf();
        }
        void save(DataOut out, int i) {
            out.putBoolean("active" + i, active); out.putBoolean("polling" + i, polling); out.putBoolean("isPressed" + i, isPressed);
            out.putInt("color" + i, color); out.putString("label" + i, label); out.putString("rtty" + i, rtty); out.putString("command" + i, command);
        }
        void load(DataIn in, int i) {
            active = in.getBooleanOr("active" + i, false); polling = in.getBooleanOr("polling" + i, false); isPressed = in.getBooleanOr("isPressed" + i, false);
            color = in.getIntOr("color" + i, 0); label = in.getStringOr("label" + i, ""); rtty = in.getStringOr("rtty" + i, ""); command = in.getStringOr("command" + i, "");
        }
    }
}
