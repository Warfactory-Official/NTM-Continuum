package com.hbm.tileentity.network;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModBlockEntities;

public class TileEntityRadioTorchReceiver extends TileEntityRadioTorchBase {
    public TileEntityRadioTorchReceiver(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_RECEIVER.get(), pos, state);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (lastUpdate == Long.MIN_VALUE) lastUpdate = level.getGameTime();
        if ((channel != null && !channel.isEmpty()) || polling) {
            RTTYSystem.RTTYChannel received = channel == null || channel.isEmpty()
                    ? null : RTTYSystem.listen(level, channel);
            if (received != null
                    && (polling || (received.timeStamp > lastUpdate - 1 && received.timeStamp != -1))) {
                String message = String.valueOf(received.signal);
                lastUpdate = level.getGameTime();
                int nextState = decode(message);
                if (received.timeStamp < lastUpdate - 1 && polling) nextState = 0;
                applyState(nextState);
            } else if (polling) {
                applyState(0);
            }
        }
        syncRadio();
    }

    public int decode(String message) {
        if (customMap) {
            for (int i = MAPPING_SIZE - 1; i >= 0; i--) {
                if (message.equals(mapping[i])) return i;
            }
            return 0;
        }
        try {
            return Mth.clamp(Integer.parseInt(message), 0, 15);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void applyState(int nextState) {
        if (lastState == nextState) return;
        lastState = nextState;
        setLit(nextState > 0);
        if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        setChanged();
        dataChanged();
    }

    @Override
    protected String titleKey() {
        return "container.rttyReceiver";
    }
}
