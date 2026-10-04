package com.hbm.tileentity.network;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.network.PneumoTubePaintableBlock;
import com.hbm.tileentity.ModPneumaticBlockEntities;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;

/**
 * 1.7 {@code PneumoTubePaintableBlock.TileEntityPneumoTubePaintable}: a tube that remembers a camouflage block.
 * 1.7 stored block id + meta; that pair is one {@link BlockState} here.
 */
public class TileEntityPneumoTubePaintable extends TileEntityPneumoTube {

    private @Nullable BlockState camo;

    public TileEntityPneumoTubePaintable(BlockPos pos, BlockState state) {
        super(ModPneumaticBlockEntities.PNEUMATIC_TUBE_PAINTABLE.get(), pos, state);
    }

    public @Nullable BlockState getCamo() {
        return camo;
    }

    public void setCamo(@Nullable BlockState camo) {
        if (this.camo == camo) return;
        this.camo = camo;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(camo == null ? -1 : Block.getId(camo));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        BlockState old = camo;
        int id = buf.readInt();
        this.camo = id < 0 ? null : Block.stateById(id);
        if (old != camo && level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
        }
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = super.getSettings(level, pos);
        if (nbt == null) nbt = new CompoundTag();
        if (camo != null) com.hbm.util.SavedIds.putState(nbt, "paintblock", camo);
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        super.pasteSettings(nbt, index, level, player, pos);
        if (Tags.contains(nbt, "paintblock")) {
            setCamo(com.hbm.util.SavedIds.getState(nbt, "paintblock", camo));
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.camo = input.read("camo", BlockState.CODEC).orElse(null);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (camo != null) output.store("camo", BlockState.CODEC, camo);
    }
}
