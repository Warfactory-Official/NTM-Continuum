package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A power cable: a pure conductor. It owns a {@link Nodespace.PowerNode} in UNINOS, which the engine
 * links into a network with adjacent conductors. Carries no power itself — providers and receivers
 * subscribe to the network the cables form.
 */
public class TileEntityCableBaseNT extends BlockEntity implements IEnergyConductorMK2, ILoadedTile {

    protected Nodespace.PowerNode node;

    public TileEntityCableBaseNT(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_CABLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCableBaseNT cable) {
        if (cable.node == null || cable.node.expired) {
            cable.node = UniNodespace.getNode(level, pos, Nodespace.THE_POWER_PROVIDER);
            if (cable.node == null || cable.node.expired) {
                cable.node = cable.createNode();
                UniNodespace.createNode(level, cable.node);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            UniNodespace.destroyNode(level, getBlockPos(), Nodespace.THE_POWER_PROVIDER);
        }
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
