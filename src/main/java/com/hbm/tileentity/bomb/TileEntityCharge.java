package com.hbm.tileentity.bomb;

import dev.golem.core.data.Tags;

import com.hbm.sound.ModSounds;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.tileentity.ModExplosivesBlockEntities;

/** 1.7 {@code TileEntityCharge}: the timer behind every {@link BlockChargeBase}; pings once a second while armed. */
public class TileEntityCharge extends SyncedBlockEntity {

    public boolean started;
    public int timer;

    public TileEntityCharge(BlockPos pos, BlockState state) {
        super(ModExplosivesBlockEntities.CHARGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCharge charge) {
        if (level.isClientSide()) return;

        if (charge.started) {
            charge.timer--;

            if (charge.timer % 20 == 0 && charge.timer > 0) {
                level.playSound(null, pos, ModSounds.FSTBMB_PING.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            if (charge.timer <= 0 && state.getBlock() instanceof BlockChargeBase block) {
                block.explode(level, pos, null);
                return;
            }
        }

        charge.syncState();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeInt(timer);
        buf.writeBoolean(started);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        timer = buf.readInt();
        started = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("started", started);
        output.putInt("timer", timer);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        started = input.getBooleanOr("started", false);
        timer = input.getIntOr("timer", 0);
    }

    public String getMinutes() {
        String mins = "" + (timer / 1200);
        if (mins.length() == 1) mins = "0" + mins;
        return mins;
    }

    public String getSeconds() {
        String secs = "" + ((timer / 20) % 60);
        if (secs.length() == 1) secs = "0" + secs;
        return secs;
    }
}
