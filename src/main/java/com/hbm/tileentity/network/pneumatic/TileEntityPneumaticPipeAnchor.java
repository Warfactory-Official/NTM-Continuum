package com.hbm.tileentity.network.pneumatic;

import com.hbm.api.tile.ILoadedTile;
import com.hbm.blocks.network.BasePipeAnchor;
import com.hbm.tileentity.ModPneumaticBlockEntities;
import com.hbm.tileentity.network.TileEntityPipelineBase;
import com.hbm.uninos.PneumaticData;
import com.hbm.uninos.PneumaticGraph;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** 1.7 {@code TileEntityPneumaticPipeAnchor}: a tube node open on its mounted face, joined to its peers by pipelines. */
public class TileEntityPneumaticPipeAnchor extends CompatBlockEntity implements TileEntityPipelineBase, ILoadedTile {

    protected final List<BlockPos> connected = new ArrayList<>();

    public TileEntityPneumaticPipeAnchor(BlockPos pos, BlockState state) {
        super(ModPneumaticBlockEntities.PNEUMATIC_PIPE_ANCHOR.get(), pos, state);
    }

    @Override
    public NetworkType getNetworkType() {
        return NetworkType.PNEUMATIC;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<PneumaticData> graph = PneumaticGraph.get(server);
        long key = getBlockPos().asLong();
        int mask = BasePipeAnchor.faceMask(getBlockState());
        if (graph.containsCell(key)) graph.updateConnections(key, mask);
        else graph.addNode(key, PneumaticData.INSTANCE, mask);
        for (BlockPos peer : connected) graph.addRemoteLink(key, peer.asLong());
    }

    @Override
    public List<BlockPos> getConnected() {
        return connected;
    }

    @Override
    public void linkNode(BlockPos peer) {
        if (level instanceof ServerLevel server) PneumaticGraph.get(server).addRemoteLink(getBlockPos().asLong(), peer.asLong());
    }

    @Override
    public void unlinkNode(BlockPos peer) {
        if (level instanceof ServerLevel server) PneumaticGraph.get(server).removeRemoteLink(getBlockPos().asLong(), peer.asLong());
    }

    @Override
    public void setRemoved() {
        boolean broken = level instanceof ServerLevel && LevelNodeGraph.blockWentWithIt(this);
        if (broken) disconnectAll();
        super.setRemoved();
        PneumaticGraph.dropCell(this, getBlockPos());
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
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
        TileEntityPipelineBase.save(connected, output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        TileEntityPipelineBase.load(connected, input);
    }
}
