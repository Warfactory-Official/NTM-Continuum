package com.hbm.tileentity.machine;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.NoteBuilder;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityRadioRec}: tuned to an RTTY channel and switched on, it plays every note of a beat the
 * channel carried last tick as a note block would (harp, bass drum, snare, hat or bass, volume 3, the note's
 * pitch over two octaves). The GUI sends the channel and the on switch; only a player within 16 blocks may.
 */
public class TileEntityRadioRec extends TileEntityLoadedBase implements IControlReceiver {

    public String channel = "";
    public boolean isOn = false;

    public TileEntityRadioRec(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.RADIOREC.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityRadioRec radio) {
        if (radio.isOn && !radio.channel.isEmpty()) {
            RTTYSystem.RTTYChannel chan = RTTYSystem.listen(level, radio.channel);

            if (chan != null && chan.timeStamp == level.getGameTime() - 1) {
                for (NoteBuilder.Hit note : NoteBuilder.translate(chan.signal + "")) {
                    int noteId = note.note().ordinal() + note.octave().ordinal() * 12;
                    Holder<SoundEvent> s = switch (note.instrument()) {
                        case BASSDRUM -> SoundEvents.NOTE_BLOCK_BASEDRUM;
                        case SNARE -> SoundEvents.NOTE_BLOCK_SNARE;
                        case CLICKS -> SoundEvents.NOTE_BLOCK_HAT;
                        case BASSGUITAR -> SoundEvents.NOTE_BLOCK_BASS;
                        default -> SoundEvents.NOTE_BLOCK_HARP;
                    };
                    float f = (float) Math.pow(2.0D, (double) (noteId - 12) / 12.0D);
                    level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, s.value(),
                            SoundSource.RECORDS, 3.0F, f);
                }
            }
        }
        radio.networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.channel);
        buf.writeBoolean(this.isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.channel = buf.readUtf();
        this.isOn = buf.readBoolean();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        channel = input.getStringOr("channel", "");
        isOn = input.getBooleanOr("isOn", false);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putString("channel", channel);
        output.putBoolean("isOn", isOn);
    }

    @Override
    public boolean hasPermission(Player player) {
        return Math.sqrt(player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5)) < 16D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "channel")) this.channel = Tags.getStringOr(data, "channel", "");
        if (Tags.contains(data, "isOn")) this.isOn = Tags.getBooleanOr(data, "isOn", false);
        this.setChanged();
    }
}
