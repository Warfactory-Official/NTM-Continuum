package com.hbm.tileentity.network;

import dev.golem.core.data.Tags;

import com.hbm.interfaces.ICopiable;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.network.BlockCablePaintable;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/**
 * The paintable cable's marker: a cable that remembers which block it is dressed as. The 1.7 tile carried
 * {@code Block + meta}; the port carries one block state, saved by name (an old save's bare block id reads as its
 * default state).
 */
public class TileEntityCablePaintable extends TileEntityCableClassic implements ICopiable {

    private @Nullable BlockState paint;

    public TileEntityCablePaintable(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.RED_CABLE_PAINTABLE.get(), pos, state);
    }

    public @Nullable BlockState getPaint() {
        return paint;
    }

    public void setPaint(@Nullable BlockState paint) {
        this.paint = paint;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState().setValue(BlockCablePaintable.PAINTED, paint != null);
            if (state != getBlockState()) level.setBlock(getBlockPos(), state, 3);
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putString("block", paint == null ? "" : SavedIds.stateName(paint));
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        BlockState old = paint;
        paint = SavedIds.getState(input, "block", null);
        if (old != paint) remesh(); // the paint is drawn by the block's model
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        if (paint != null) SavedIds.putState(nbt, "paintblock", paint);
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (Tags.contains(nbt, "paintblock")) setPaint(SavedIds.getState(nbt, "paintblock", paint));
    }
}
