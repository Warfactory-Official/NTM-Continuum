package com.hbm.tileentity.network.pneumatic;

import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.StackCache;
import com.hbm.uninos.PneumaticGraph;
import com.hbm.uninos.networkproviders.PneumaticNetwork;

/**
 * 1.7 {@code TileEntityPneumaticMachineBase}: an automation endpoint reading the network through its own
 * {@link StackCache}. Also the trunk of the access terminal, which 1.7 wrote against {@code TileEntityLoadedBase}
 * with the same body.
 */
public abstract class TileEntityPneumaticMachineBase extends TileEntityMachineBase implements MenuProvider {

    public @Nullable StackCache cache;
    /** The net this cache was built against; 1.7 dissolved the cache whenever its node's net was destroyed. */
    private @Nullable PneumaticNetwork lastNet;

    protected TileEntityPneumaticMachineBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;

        PneumaticNetwork net = PneumaticNetwork.at(sl, worldPosition);

        // 1.7: a rebuilt net (a tube placed or broken anywhere on it) dissolves every cache so it re-gathers
        if (net != lastNet) {
            if (this.cache != null) this.cache.dissolveCache();
            lastNet = net;
        }

        if (this.cache == null || this.cache.hasExpired) {
            this.cache = new StackCache(worldPosition);
        }

        if (net != null) net.addStackCache(cache);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) PneumaticGraph.addNode(server, worldPosition);
    }

    /** 1.7 splits this across invalidate() and onChunkUnload(); setRemoved fires for both. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.cache != null) this.cache.dissolveCache();
        PneumaticGraph.dropCell(this, worldPosition);
    }
}
