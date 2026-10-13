package com.hbm.tileentity.network;

import dev.golem.core.Ids;
import dev.golem.core.data.Tags;

import com.hbm.interfaces.ICopiable;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.network.BlockCablePaintable;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/**
 * The paintable cable's marker: a cable that remembers which block it is dressed as. The 1.7 tile carried
 * {@code Block + meta}; the port carries the block's registry id (its default state is the paint).
 */
public class TileEntityCablePaintable extends TileEntityCableClassic implements ICopiable {

    private @Nullable Block block;

    public TileEntityCablePaintable(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.RED_CABLE_PAINTABLE.get(), pos, state);
    }

    public @Nullable Block getPaint() {
        return block;
    }

    public void setPaint(@Nullable Block paint) {
        this.block = paint;
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
        output.putString("block", block == null ? "" : BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        block = resolve(input.getStringOr("block", ""));
    }

    private static @Nullable Block resolve(String id) {
        if (id.isEmpty()) return null;
        Block found = BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElse(null);
        return found == null || found == Blocks.AIR ? null : found;
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        if (block != null) nbt.putString("paintblock", BuiltInRegistries.BLOCK.getKey(block).toString());
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (Tags.contains(nbt, "paintblock")) {
            setPaint(resolve(Tags.getStringOr(nbt, "paintblock", "")));
        }
    }
}
